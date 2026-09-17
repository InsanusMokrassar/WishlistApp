package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.create
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Verifies the email lifecycle against a disposable PostgreSQL schema. */
class PostgresUsersRepoTest {
    /** PostgreSQL writers retain approval, protect both slots, and atomically promote the candidate. */
    @Test
    fun retainedApprovalAndCrossSlotUniqueness() = runTest {
        withPostgresUsersRepo { repo ->
            val current = Email("postgres-current@example.com")
            val pending = Email("postgres-pending@example.com")
            val user = repo.create(NewUser(Username("postgres-owner"), current)).single()
            checkNotNull(repo.approveEmail(user.id, current, cooldownMillis = 0))

            val replacement = checkNotNull(repo.setEmail(user.id, pending))
            assertEquals(current, replacement.email)
            assertEquals(pending, replacement.pendingEmail)
            assertTrue(replacement.emailApproved)

            val duplicate = assertFailsWith<DuplicateUserFieldException> {
                repo.create(NewUser(Username("postgres-other"), pending))
            }
            assertNull(duplicate.cause)

            val promoted = checkNotNull(repo.approveEmail(user.id, pending, cooldownMillis = 1_000))
            assertEquals(pending, promoted.email)
            assertNull(promoted.pendingEmail)
            assertTrue(promoted.emailApproved)
            assertNotNull(promoted.emailChangeAllowedAt)
        }
    }
}
