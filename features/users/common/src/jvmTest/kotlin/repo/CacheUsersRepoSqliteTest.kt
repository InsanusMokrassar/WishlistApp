package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises the production full-cache wrapper over the real SQLite repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class CacheUsersRepoSqliteTest {
    /** A conditional approval immediately mirrors into the cache and later replacements reset it. */
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
            assertFalse(changed.emailApproved)
            assertEquals(changed, cache.getById(created.id))
            assertEquals(changed, backing.getById(created.id))
        }
    }
}
