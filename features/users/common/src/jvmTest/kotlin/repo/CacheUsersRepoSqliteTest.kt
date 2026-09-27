package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import korlibs.time.DateTime
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** Exercises the production full-cache wrapper over the real SQLite repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class CacheUsersRepoSqliteTest {
    /** A failed mutation or approval cannot issue even a redundant cache write. */
    @Test
    fun invalidClockFailurePreservesWarmedCacheAndWriteCount() = runTest {
        val clock = EmailTestClock()
        withFileBackedSqliteUsersRepos(firstNow = clock::sample) { url, backing, _ ->
            val current = Email("cache-invalid-current@example.com")
            val pending = Email("cache-invalid-pending@example.com")
            val user = backing.create(NewUser(Username("cache-invalid"), current)).single()
            checkNotNull(backing.approveEmail(user.id, current, cooldownMillis = 0L))
            checkNotNull(backing.setEmail(user.id, pending))
            val storage = CountingUsersCache()
            val cache = CacheUsersRepo(backing, backgroundScope, kvCache = storage)
            advanceUntilIdle()
            val before = rawUsersSnapshot(url)
            val cached = cache.getById(user.id)
            val writes = storage.setCalls
            val events = mutableListOf<RegisteredUser>()
            val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                backing.updatedObjectsFlow.collect(events::add)
            }
            try {
                clock.value = DateTime(Double.NaN)
                assertFailsWith<IllegalArgumentException> { cache.setEmail(user.id, null) }
                assertFailsWith<IllegalArgumentException> { cache.approveEmail(user.id, pending, cooldownMillis = 10L) }
                advanceUntilIdle()
                assertEquals(before, rawUsersSnapshot(url))
                assertEquals(cached, cache.getById(user.id))
                assertEquals(writes, storage.setCalls)
                assertEquals(emptyList(), events)
            } finally {
                collector.cancel()
            }
        }
    }

    /** A fresh read bypasses a warmed cache after another file-backed repository changes lifecycle state. */
    @Test
    fun freshReadBypassesCacheAfterIndependentRepositoryMutation() = runTest {
        var now = DateTime.fromUnixMillis(1_000L)
        withFileBackedSqliteUsersRepos(firstNow = { now }, secondNow = { now }) { _, first, second ->
            val current = Email("approved@example.com")
            val pending = Email("pending@example.com")
            val created = first.create(NewUser(Username("cached"), current)).single()
            val cache = CacheUsersRepo(first, backgroundScope)
            advanceUntilIdle()
            assertEquals(created, cache.getById(created.id))

            checkNotNull(second.approveEmail(created.id, current, cooldownMillis = 10L))
            now = DateTime.fromUnixMillis(1_010L)
            val newer = checkNotNull(second.update(created.id, NewUser(created.username, pending)))

            assertEquals(created, cache.getById(created.id))
            assertEquals(
                RegisteredUser(
                    id = created.id,
                    username = created.username,
                    email = current,
                    emailApproved = true,
                ),
                newer,
            )
            assertEquals(newer, cache.getByIdFresh(created.id))
            assertEquals(created, cache.getById(created.id))
            assertEquals(
                EmailProfile(
                    userId = created.id.long,
                    email = current,
                    emailApproved = true,
                    pendingEmail = pending,
                    emailChangeRequestedAt = DateTime.fromUnixMillis(1_010L),
                    emailChangeAllowedAt = DateTime.fromUnixMillis(1_010L),
                ),
                cache.getEmailProfileFresh(created.id),
            )
        }
    }

    /** A conditional approval and pending replacement immediately mirror complete lifecycle state into the cache. */
    @Test
    fun approvalImmediatelyMirrorsAndReplacementResetsCache() = runTest {
        withInMemorySqliteUsersRepo { backing ->
            val cache = CacheUsersRepo(backing, backgroundScope)
            advanceUntilIdle()
            val original = Email("cached@example.com")
            val replacement = Email("cached-replacement@example.com")
            val created = cache.create(NewUser(Username("cached"), original)).single()

            val approved = checkNotNull(cache.approveEmail(created.id, original))
            assertTrue(approved.emailApproved)
            assertEquals(approved, cache.getById(created.id))
            assertEquals(approved, cache.getAll()[created.id])
            assertEquals(approved, backing.getById(created.id))

            assertNull(cache.approveEmail(created.id, replacement))
            assertEquals(approved, cache.getById(created.id))

            val changed = checkNotNull(cache.update(created.id, NewUser(approved.username, replacement)))
            assertEquals(original, changed.email)
            assertTrue(changed.emailApproved)
            assertEquals(replacement, cache.getEmailProfileFresh(created.id)?.pendingEmail)
            assertEquals(changed, cache.getById(created.id))
            assertEquals(changed, backing.getById(created.id))
        }
    }
}
