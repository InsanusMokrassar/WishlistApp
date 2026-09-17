package dev.inmo.wishlist.features.ui.users

import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistItemsFeature
import dev.inmo.wishlist.features.admin.client.AdminWishlistsFeature
import dev.inmo.wishlist.features.admin.client.UsersManagementFeature
import dev.inmo.wishlist.features.admin.common.Constants as AdminConstants
import dev.inmo.wishlist.features.admin.common.models.AdminUser
import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.client.meQualifier
import dev.inmo.wishlist.features.auth.common.models.AuthConfig
import dev.inmo.wishlist.features.auth.common.models.AuthCredentials
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RefreshToken
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.client.FilesFeature
import dev.inmo.wishlist.features.files.common.Constants as FilesConstants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FilesFeatureMetaInfo
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.roles.client.RolesFeature
import dev.inmo.wishlist.features.roles.common.models.FunctionalityId
import dev.inmo.wishlist.features.ui.users.ui.DefaultUsersModel
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies users-model behavior and its production singleton binding. */
class UsersModelTest {
    internal companion object {
        /** Builds the production model around a JVM-test file service. */
        fun modelForFileTests(
            filesService: FilesClientService,
            scope: CoroutineScope,
        ): DefaultUsersModel = DefaultUsersModel(
            feature = RecordingUsersFeature(),
            authFeature = RecordingAuthFeature(),
            emailFeature = RecordingEmailFeature(),
            meState = MutableStateFlow(null),
            adminFeature = object : AdminFeature {
                override val usersManagement: UsersManagementFeature = RecordingUsersManagementFeature()
                override val wishlists: AdminWishlistsFeature = UnusedWishlistsFeature
                override val wishlistItems: AdminWishlistItemsFeature = UnusedWishlistItemsFeature
            },
            filesService = filesService,
            scope = scope,
            credentialsStorage = TestCredentialsStorage(),
            rolesFeature = RecordingRolesFeature(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pluginModelPreservesEveryPlatformNeutralDelegationAndReactiveState() = runTest {
        val meState = MutableStateFlow<AuthFeatureUser?>(null)
        val users = RecordingUsersFeature()
        val auth = RecordingAuthFeature()
        val email = RecordingEmailFeature()
        val management = RecordingUsersManagementFeature()
        val roles = RecordingRolesFeature()
        val files = RecordingFilesFeature()
        val storage = TestCredentialsStorage()
        val client = HttpClient()
        val filesService = FilesClientService(client, files)
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<UsersFeature> { users }
                    single<ClientAuthFeature> { auth }
                    single<EmailFeature> { email }
                    single<AdminFeature> {
                        object : AdminFeature {
                            override val usersManagement: UsersManagementFeature = management
                            override val wishlists: AdminWishlistsFeature = UnusedWishlistsFeature
                            override val wishlistItems: AdminWishlistItemsFeature = UnusedWishlistItemsFeature
                        }
                    }
                    single<FilesClientService> { filesService }
                    single<CoroutineScope> { backgroundScope }
                    single<AuthCredentialsStorage> { storage }
                    single<RolesFeature> { roles }
                    single<StateFlow<AuthFeatureUser?>>(meQualifier) { meState }
                }
            )
        }
        try {
            val model = koin.koin.get<UsersModel>()
            assertIs<DefaultUsersModel>(model)
            assertSame(model, koin.koin.get<UsersModel>())
            assertSame(storage.userAuthorised, model.userAuthorisedState)

            runCurrent()
            assertNull(model.currentUserIdFlow.value)
            assertFalse(model.isCurrentUserRootFlow.value)
            assertFalse(model.canChangeAvatarForOthersFlow.value)
            assertEquals(emptyList(), roles.calls)

            val profile = AuthFeatureUser(UserId(7L), Username("owner"), Email("owner@example.com"), emailApproved = false)
            meState.value = profile
            runCurrent()
            assertEquals(profile.id, model.currentUserIdFlow.value)
            assertTrue(model.isCurrentUserRootFlow.value)
            assertTrue(model.canChangeAvatarForOthersFlow.value)
            assertEquals(
                setOf(
                    AdminConstants.adminPanelFunctionalityId,
                    FilesConstants.avatarChangeForOthersFunctionalityId,
                ),
                roles.calls.toSet(),
            )

            assertSame(users.values, model.getAllUsers())
            assertSame(users.firstDuplicate, model.getUser(users.firstDuplicate.id))
            assertNull(model.getUser(UserId(404L)))
            assertEquals(3, users.calls)

            auth.profile = profile
            assertEquals(profile, model.getMyProfile())
            assertEquals(1, auth.getMeCalls)

            val replacement = Email("replacement@example.com")
            assertTrue(model.isEmailFeatureEnabled())
            assertTrue(model.setMyEmail(replacement))
            assertEquals(EmailVerificationRequestResult.Sent, model.requestMyEmailVerification(replacement))
            assertEquals(1, email.enabledCalls)
            assertEquals(listOf<Email?>(replacement), email.setCalls)
            assertEquals(listOf(replacement), email.requestCalls)

            val username = Username("owner-renamed")
            val password = Password("replacement-secret")
            assertTrue(model.updateUsername(profile.id, username))
            assertTrue(model.setPassword(profile.id, password))
            assertTrue(model.deleteUser(profile.id))
            assertEquals(listOf(profile.id to username), management.usernameCalls)
            assertEquals(listOf(profile.id to password), management.passwordCalls)
            assertEquals(listOf(profile.id), management.deleteCalls)

            assertEquals(files.avatarId, model.getAvatar(profile.id))
            assertEquals(listOf(profile.id), files.avatarCalls)
            assertEquals("/api/files/${files.avatarId.string}", model.imageUrl(files.avatarId))

            meState.value = null
            runCurrent()
            assertNull(model.currentUserIdFlow.value)
            assertFalse(model.isCurrentUserRootFlow.value)
            assertFalse(model.canChangeAvatarForOthersFlow.value)
            assertEquals(2, roles.calls.size)
        } finally {
            stopKoin()
            client.close()
        }
    }

    private class RecordingUsersFeature : UsersFeature {
        val firstDuplicate = UsersFeatureUser(UserId(1L), Username("first"))
        val values = listOf(
            firstDuplicate,
            UsersFeatureUser(UserId(1L), Username("second")),
            UsersFeatureUser(UserId(2L), Username("other")),
        )
        var calls = 0
        override suspend fun getAll(): List<UsersFeatureUser> {
            calls += 1
            return values
        }
    }

    private class RecordingAuthFeature : ClientAuthFeature {
        var profile: AuthFeatureUser? = null
        var getMeCalls = 0
        override suspend fun getMe(): AuthFeatureUser? {
            getMeCalls += 1
            return profile
        }
        override suspend fun logout() = Unit
        override suspend fun login(username: Username, password: Password): AuthCredentials? = error("unused")
        override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? = error("unused")
        override suspend fun register(username: Username, password: Password): RegistrationResult? = error("unused")
        override suspend fun getConfig(): AuthConfig = error("unused")
        override suspend fun isRegistrationAvailable(): Boolean = error("unused")
    }

    private class RecordingEmailFeature : EmailFeature {
        var enabledCalls = 0
        val setCalls = mutableListOf<Email?>()
        val requestCalls = mutableListOf<Email>()
        override suspend fun isFeatureEnabled(): Boolean {
            enabledCalls += 1
            return true
        }
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

    private class RecordingUsersManagementFeature : UsersManagementFeature {
        val usernameCalls = mutableListOf<Pair<UserId, Username>>()
        val passwordCalls = mutableListOf<Pair<UserId, Password>>()
        val deleteCalls = mutableListOf<UserId>()
        override suspend fun getAll(): List<AdminUser> = error("unused")
        override suspend fun getById(id: UserId): AdminUser? = error("unused")
        override suspend fun create(newUser: NewUserWithPassword): AdminUser? = error("unused")
        override suspend fun update(id: UserId, newUser: NewUser): Boolean = error("unused")
        override suspend fun updateUsername(id: UserId, username: Username): Boolean {
            usernameCalls += id to username
            return true
        }
        override suspend fun setPassword(id: UserId, password: Password): Boolean {
            passwordCalls += id to password
            return true
        }
        override suspend fun delete(id: UserId): Boolean {
            deleteCalls += id
            return true
        }
    }

    private class RecordingRolesFeature : RolesFeature {
        val calls = mutableListOf<FunctionalityId>()
        override suspend fun isFunctionalityAvailable(functionalityId: FunctionalityId): Boolean {
            calls += functionalityId
            return true
        }
    }

    private class RecordingFilesFeature : FilesFeature {
        val avatarId = FileId("avatar-id")
        val avatarCalls = mutableListOf<UserId>()
        override suspend fun getAvatar(userId: UserId): FileId {
            avatarCalls += userId
            return avatarId
        }
        override suspend fun finalize(request: FinalizeFileRequest): FilesFeatureMetaInfo? = error("unused")
        override suspend fun getMeta(id: FileId): FilesFeatureMetaInfo? = error("unused")
        override suspend fun setAvatar(userId: UserId, fileId: FileId): Boolean = error("unused")
    }

    private class TestCredentialsStorage : AuthCredentialsStorage {
        override val userAuthorised = MutableStateFlow(true)
        override suspend fun get(): AuthCredentials? = null
        override suspend fun save(credentials: AuthCredentials?) = Unit
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
