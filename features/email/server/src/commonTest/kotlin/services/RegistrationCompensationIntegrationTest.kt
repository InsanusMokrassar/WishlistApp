package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.auth.server.repo.PasswordsRepo
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.roles.server.RolesRegistrationRoleLifecycle
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Exercises failed registration across the real Auth, Roles, Email, and DeepLinks orchestration. */
class RegistrationCompensationIntegrationTest {
    /** In-memory password storage used by the real auth service. */
    private class FakePasswordsRepo : PasswordsRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo()

    /** In-memory deeplink persistence used by the real invite sender. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

    /** Password store that exposes a finalization failure after successful invite delivery.
     *
     * @param failure Failure raised when Auth stores the required-email password.
     */
    private class ThrowingPasswordsRepo(
        private val failure: Throwable,
    ) : PasswordsRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo() {
        /** Always fails required-email password persistence.
         *
         * @param toSet Passwords Auth requested to persist.
         */
        override suspend fun set(toSet: Map<UserId, Password>) {
            throw failure
        }
    }

    /** SMTP rejection compensates every store populated before delivery returned. */
    @Test
    fun smtpFailureRemovesUserPasswordLinkAndDirectRoles() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val rolesRepo = FakeRolesRepo()
        val deepLinksRepo = FakeDeepLinksRepo()
        val sender = EmailRegistrationInviteSender(
            emailsService = FakeEmailsService(result = false),
            deepLinksService = DeepLinksService(deepLinksRepo, emptyList()),
            publicHttpOrigin = "http://127.0.0.1:8196",
        )
        val service = AuthFeatureService(
            usersRepo = usersRepo,
            writeUsersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = sender,
            registrationRoleLifecycle = RolesRegistrationRoleLifecycle(rolesRepo),
        )

        assertNull(
            service.register(
                Username("alice"),
                Password("s3cret-pw"),
                Email("alice@example.com"),
            )
        )

        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(deepLinksRepo.getAll().isEmpty())
        assertTrue(
            rolesRepo.getDirectRoles(BaseRoleSubject.Direct("1")).isEmpty()
        )
    }

    /** A delivered real invite is removed when password finalization fails afterward. */
    @Test
    fun finalizationFailureRemovesDeliveredUserPasswordLinkAndDirectRoles() = runTest {
        val failure = IllegalStateException("password storage unavailable")
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = ThrowingPasswordsRepo(failure)
        val rolesRepo = FakeRolesRepo()
        val deepLinksRepo = FakeDeepLinksRepo()
        val service = AuthFeatureService(
            usersRepo = usersRepo,
            writeUsersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = EmailRegistrationInviteSender(
                emailsService = FakeEmailsService(result = true),
                deepLinksService = DeepLinksService(deepLinksRepo, emptyList()),
                publicHttpOrigin = "http://127.0.0.1:8196",
            ),
            registrationRoleLifecycle = RolesRegistrationRoleLifecycle(rolesRepo),
        )

        val thrown = assertFailsWith<IllegalStateException> {
            service.register(Username("alice"), Password("s3cret-pw"), Email("alice@example.com"))
        }

        assertSame(failure, thrown)
        assertTrue(usersRepo.getAll().isEmpty())
        assertTrue(passwordsRepo.getAll().isEmpty())
        assertTrue(deepLinksRepo.getAll().isEmpty())
        assertTrue(rolesRepo.getDirectRoles(BaseRoleSubject.Direct("1")).isEmpty())
    }

    /** A successful real invite retains its exact deeplink while Auth returns a pending result. */
    @Test
    fun successfulInviteRetainsDeepLinkAndReturnsPendingEmailVerification() = runTest {
        val usersRepo = FakeUsersRepo()
        val passwordsRepo = FakePasswordsRepo()
        val rolesRepo = FakeRolesRepo()
        val deepLinksRepo = FakeDeepLinksRepo()
        val service = AuthFeatureService(
            usersRepo = usersRepo,
            writeUsersRepo = usersRepo,
            passwordsRepo = passwordsRepo,
            enableRegistration = true,
            requireEmailForRegistration = true,
            registrationEmailSender = EmailRegistrationInviteSender(
                emailsService = FakeEmailsService(result = true),
                deepLinksService = DeepLinksService(deepLinksRepo, emptyList()),
                publicHttpOrigin = "http://127.0.0.1:8196",
            ),
            registrationRoleLifecycle = RolesRegistrationRoleLifecycle(rolesRepo),
        )

        assertEquals(
            RegistrationResult.PendingEmailVerification,
            service.register(Username("alice"), Password("s3cret-pw"), Email("alice@example.com")),
        )
        assertEquals(1, deepLinksRepo.getAll().size)
    }
}
