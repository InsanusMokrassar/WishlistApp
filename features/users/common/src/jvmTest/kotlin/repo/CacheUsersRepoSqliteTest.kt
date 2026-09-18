package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the production full-cache wrapper over the real SQLite repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class CacheUsersRepoSqliteTest {
    /** A fresh read bypasses a warmed cache after another file-backed repository changes lifecycle state. */
    @Test
    fun freshReadBypassesCacheAfterIndependentRepositoryMutation() = runTest {
        var now = 1_000L
        withFileBackedSqliteUsersRepos(firstNowMillis = { now }, secondNowMillis = { now }) { _, first, second ->
            val current = Email("approved@example.com")
            val pending = Email("pending@example.com")
            val created = first.create(NewUser(Username("cached"), current)).single()
            val cache = CacheUsersRepo(first, backgroundScope)
            advanceUntilIdle()
            assertEquals(created, cache.getById(created.id))

            checkNotNull(second.approveEmail(created.id, current, cooldownMillis = 10L))
            now = 1_010L
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
                    emailChangeRequestedAt = 1_010L,
                    emailChangeAllowedAt = 1_010L,
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
