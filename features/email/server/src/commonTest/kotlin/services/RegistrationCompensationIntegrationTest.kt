package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.micro_utils.repos.MapKeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.Password
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercises failed registration across the real Auth, Roles, Email, and DeepLinks orchestration. */
class RegistrationCompensationIntegrationTest {
    /** In-memory password storage used by the real auth service. */
    private class FakePasswordsRepo : PasswordsRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<UserId, Password> by MapKeyValueRepo()

    /** In-memory deeplink persistence used by the real invite sender. */
    private class FakeDeepLinksRepo : DeepLinksRepo,
        dev.inmo.micro_utils.repos.KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo> by MapKeyValueRepo()

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
}
