package dev.inmo.wishlist.features.ui.users

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistItemsFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistsFeature
import dev.inmo.wishlist.features.admin.client.UsersManagementFeature
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.client.PasswordChangeFeature
import dev.inmo.wishlist.features.auth.client.meQualifier
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.client.FilesFeature
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.roles.client.RolesFeature
import dev.inmo.wishlist.features.roles.common.models.FunctionalityId
import dev.inmo.wishlist.features.ui.users.ui.UsersModel
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Resolves the production users model and proves private email calls stay on their owning features. */
class UsersModelTest {
    @Test
    fun pluginModelDelegatesPrivateEmailAndUsernameOperationsWithoutChangingArguments() = runTest {
        val profile = AuthFeatureUser(UserId(7L), Username("owner"), Email("owner@example.com"), emailApproved = false)
        val auth = RecordingAuthFeature(profile)
        val email = RecordingEmailFeature()
        val passwordChange = RecordingPasswordChangeFeature()
        val usersManagement = RecordingUsersManagementFeature()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val filesClient = HttpClient()
        val koin = startModelKoin(auth, email, passwordChange, usersManagement, scope, filesClient)
        try {
            val model = koin.koin.get<UsersModel>()
            val replacement = Email("replacement@example.com")
            val username = Username("owner-renamed")

            assertEquals(profile, model.getMyProfile())
            assertTrue(model.isEmailFeatureEnabled())
            assertTrue(model.setMyEmail(replacement))
            assertEquals(
                EmailVerificationRequestResult.Sent,
                model.requestMyEmailVerification(replacement),
            )
            assertEquals(PasswordChangeEmailRequestResult.Sent, model.requestPasswordChangeEmail(replacement))
            assertEquals(
                PasswordChangeResult.Changed,
                model.completePasswordChange(
                    CompletePasswordChangeRequest(profile.id, dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId("approval"), Password("new-password")),
                ),
            )
            assertTrue(model.updateUsername(profile.id, username))
            assertEquals(listOf<Email?>(replacement), email.setCalls)
            assertEquals(listOf(replacement), email.requestCalls)
            assertEquals(listOf(replacement), passwordChange.requestedEmails)
            assertEquals(profile.id, passwordChange.completedRequests.single().userId)
            assertEquals(listOf(profile.id to username), usersManagement.usernameCalls)
        } finally {
            stopKoin()
            scope.cancel()
            filesClient.close()
        }
    }

    private fun startModelKoin(
        auth: RecordingAuthFeature,
        email: RecordingEmailFeature,
        passwordChange: RecordingPasswordChangeFeature,
        usersManagement: RecordingUsersManagementFeature,
        scope: CoroutineScope,
        client: HttpClient,
    ): KoinApplication = startKoin {
        modules(
            module {
                with(Plugin) { setupDI(JsonObject(emptyMap())) }
                single<UsersFeature> { EmptyUsersFeature }
                single<ClientAuthFeature> { auth }
                single<EmailFeature> { email }
                single<PasswordChangeFeature> { passwordChange }
                single<AdminFeature> {
                    object : AdminFeature {
                        override val usersManagement: UsersManagementFeature = usersManagement
                        override val wishlists: AdminWishlistsFeature = UnusedWishlistsFeature
                        override val wishlistItems: AdminWishlistItemsFeature = UnusedWishlistItemsFeature
                    }
                }
                single<FilesClientService> { FilesClientService(client, UnusedFilesFeature) }
                single<CoroutineScope> { scope }
                single<AuthCredentialsStorage> { TestCredentialsStorage }
                single<RolesFeature> { NoRolesFeature }
                single<StateFlow<AuthFeatureUser?>>(meQualifier) { MutableStateFlow(auth.profile) }
            }
        )
    }

    private class RecordingAuthFeature(val profile: AuthFeatureUser) : ClientAuthFeature {
        override suspend fun logout() = Unit
        override suspend fun getMe(): AuthFeatureUser = profile
        override suspend fun login(username: Username, password: Password): AuthCredentials? = error("unused")
        override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? = error("unused")
        override suspend fun register(username: Username, password: Password): RegistrationResult? = error("unused")
        override suspend fun getConfig(): AuthConfig = error("unused")
        override suspend fun isRegistrationAvailable(): Boolean = error("unused")
    }

    private class RecordingEmailFeature : EmailFeature {
        val setCalls = mutableListOf<Email?>()
        val requestCalls = mutableListOf<Email>()
        override suspend fun isFeatureEnabled(): Boolean = true
        override suspend fun sendTestEmail(recipient: Email): Boolean = error("unused")
        override suspend fun setMyEmail(email: Email?): Boolean {
            setCalls += email
            return true
        }
        override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult {
            requestCalls += expectedEmail
            return EmailVerificationRequestResult.Sent
        }
    }

    /** Records password-change transport calls delegated through the production users model. */
    private class RecordingPasswordChangeFeature : PasswordChangeFeature {
        /** Exact expected addresses sent by the owner-editor operation. */
        val requestedEmails = mutableListOf<Email>()

        /** Exact completion payloads sent by the token-authorized page. */
        val completedRequests = mutableListOf<CompletePasswordChangeRequest>()

        /** Records one expected email and reports a confirmed message delivery. */
        override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult {
            requestedEmails += expectedEmail
            return PasswordChangeEmailRequestResult.Sent
        }

        /** Records one immutable completion payload and reports a changed password. */
        override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult {
            completedRequests += request
            return PasswordChangeResult.Changed
        }
    }

    private class RecordingUsersManagementFeature : UsersManagementFeature {
        val usernameCalls = mutableListOf<Pair<UserId, Username>>()
        override suspend fun getAll(): List<AdminUser> = error("unused")
        override suspend fun getById(id: UserId): AdminUser? = error("unused")
        override suspend fun create(newUser: NewUserWithPassword): AdminUser? = error("unused")
        override suspend fun update(id: UserId, newUser: NewUser): Boolean = error("unused")
        override suspend fun updateUsername(id: UserId, username: Username): Boolean {
            usernameCalls += id to username
            return true
        }
        override suspend fun setPassword(id: UserId, password: Password): Boolean = error("unused")
        override suspend fun delete(id: UserId): Boolean = error("unused")
    }

    private object TestCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(true)
        override suspend fun get(): AuthCredentials? = null
        override suspend fun save(credentials: AuthCredentials?) = Unit
    }

    private object EmptyUsersFeature : UsersFeature {
        override suspend fun getAll(): List<UsersFeatureUser> = emptyList()
    }

    private object NoRolesFeature : RolesFeature {
        override suspend fun isFunctionalityAvailable(functionalityId: FunctionalityId): Boolean = false
    }

    private object UnusedFilesFeature : FilesFeature {
        override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo? = error("unused")
        override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? = error("unused")
        override suspend fun getAvatar(userId: UserId): FileId? = error("unused")
        override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean = error("unused")
    }

    private object UnusedWishlistsFeature : AdminWishlistsFeature {
        override suspend fun getAll(): List<AdminWishlist> = error("unused")
        override suspend fun getByUserId(userId: UserId): List<AdminWishlist> = error("unused")
        override suspend fun getById(id: WishlistId): AdminWishlist? = error("unused")
        override suspend fun create(newWishlist: NewWishlist): AdminWishlist? = error("unused")
        override suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean = error("unused")
        override suspend fun delete(id: WishlistId): Boolean = error("unused")
    }

    private object UnusedWishlistItemsFeature : AdminWishlistItemsFeature {
        override suspend fun getByWishlistId(wishlistId: WishlistId): List<AdminWishlistItem> = error("unused")
        override suspend fun create(item: NewWishlistItem): AdminWishlistItem? = error("unused")
        override suspend fun update(id: WishlistItemId, item: NewWishlistItem): Boolean = error("unused")
        override suspend fun delete(id: WishlistItemId): Boolean = error("unused")
    }
}
