package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.admin.client.AdminFeature
import dev.inmo.wishlist.features.admin.common.Constants as AdminConstants
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.email.client.EmailFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.common.Constants as FilesConstants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.roles.client.RolesFeature
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Default outside-world implementation shared by the users list, profile, and editor screens.
 *
 * @param feature Public users capability.
 * @param authFeature Private current-user authentication capability.
 * @param emailFeature Current-user email management capability.
 * @param meState Reactive private current-user record.
 * @param adminFeature Administrative user mutation capability.
 * @param filesService Avatar storage and download service.
 * @param scope Lifetime used for derived state flows.
 * @param credentialsStorage Persistent authentication state exposed to editor ViewModels.
 * @param rolesFeature Functionality-availability capability.
 */
class DefaultUsersModel(
    private val feature: UsersFeature,
    private val authFeature: ClientAuthFeature,
    private val emailFeature: EmailFeature,
    private val meState: StateFlow<AuthFeatureUser?>,
    private val adminFeature: AdminFeature,
    private val filesService: FilesClientService,
    private val scope: CoroutineScope,
    private val credentialsStorage: AuthCredentialsStorage,
    private val rolesFeature: RolesFeature,
) : UsersModel {
    /** Authentication state shared with user editor ViewModels. */
    override val userAuthorisedState = credentialsStorage.userAuthorised

    /** @return Every publicly visible user. */
    override suspend fun getAllUsers(): List<UsersFeatureUser> = feature.getAll()

    /** @return The first public user matching [id], or `null` when absent. */
    override suspend fun getUser(id: UserId): UsersFeatureUser? =
        feature.getAll().find { it.id == id }

    /** Caller id derived from the reactive private-user record. */
    override val currentUserIdFlow: StateFlow<UserId?> =
        meState.map { it?.id }.stateIn(scope, SharingStarted.Eagerly, meState.value?.id)

    /** Reactive permission to access the administrative panel. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val isCurrentUserRootFlow: StateFlow<Boolean> =
        meState
            .mapLatest { me ->
                me != null && rolesFeature.isFunctionalityAvailable(AdminConstants.adminPanelFunctionalityId)
            }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** Reactive permission to change another user's avatar. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val canChangeAvatarForOthersFlow: StateFlow<Boolean> =
        meState
            .mapLatest { me ->
                me != null && rolesFeature.isFunctionalityAvailable(FilesConstants.avatarChangeForOthersFunctionalityId)
            }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /** @return The authenticated caller's private profile, or `null` when unavailable. */
    override suspend fun getMyProfile() = authFeature.getMe()

    /** @return `true` when SMTP-backed email operations are enabled. */
    override suspend fun isEmailFeatureEnabled(): Boolean = emailFeature.isFeatureEnabled()

    /** @return `true` when [email] is stored for the authenticated caller. */
    override suspend fun setMyEmail(email: Email?): Boolean = emailFeature.setMyEmail(email)

    /** @return Verification-request result for the caller's [expectedEmail]. */
    override suspend fun requestMyEmailVerification(
        expectedEmail: Email,
    ): EmailVerificationRequestResult = emailFeature.requestMyEmailVerification(expectedEmail)

    /** @return `true` when user [id] is renamed to [username]. */
    override suspend fun updateUsername(id: UserId, username: Username): Boolean =
        adminFeature.usersManagement.updateUsername(id, username)

    /** @return `true` when [password] replaces the password for user [id]. */
    override suspend fun setPassword(id: UserId, password: Password): Boolean =
        adminFeature.usersManagement.setPassword(id, password)

    /** @return `true` when user [id] is deleted. */
    override suspend fun deleteUser(id: UserId): Boolean =
        adminFeature.usersManagement.delete(id)

    /** @return Avatar identifier for [userId], or `null` when unset. */
    override suspend fun getAvatar(userId: UserId): FileId? =
        filesService.getAvatar(userId)

    /** @return Stored avatar identifier after uploading [file] for [userId], or `null` on failure. */
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? =
        filesService.uploadAvatar(userId, file)

    /** @return Download URL for file [id]. */
    override fun imageUrl(id: FileId): String = filesService.apiFileUrl(id)

    /** @return Downloaded bytes for file [id], or `null` on failure. */
    override suspend fun loadImageBytes(id: FileId): ByteArray? =
        filesService.downloadBytes(id)
}
