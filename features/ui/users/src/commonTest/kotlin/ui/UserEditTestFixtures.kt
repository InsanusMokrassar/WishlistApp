package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Mutable, hook-driven model for deterministic owner-email and root-save ViewModel tests. */
internal class UserEditTestUsersModel(
    initialUserId: UserId?,
    initialProfile: AuthFeatureUser?,
    initiallyAuthorised: Boolean = initialUserId != null,
) : UsersModel {
    val currentUserIdState = MutableStateFlow(initialUserId)
    val authorisedState = MutableStateFlow(initiallyAuthorised)
    val rootState = MutableStateFlow(false)
    val avatarForOthersState = MutableStateFlow(false)
    val profileState = MutableStateFlow(initialProfile)

    var emailFeatureEnabled = true
    var saveEmailResult = true
    var requestResult = EmailVerificationRequestResult.Sent
    var updateUsernameResult = true
    var setPasswordResult = true

    var probeHandler: suspend () -> Boolean = { emailFeatureEnabled }
    var profileHandler: suspend () -> AuthFeatureUser? = { profileState.value }
    var saveEmailHandler: suspend (Email?) -> Boolean = { email ->
        if (saveEmailResult) {
            profileState.value = profileState.value?.copy(email = email, emailApproved = false)
        }
        saveEmailResult
    }
    var requestHandler: suspend (Email) -> EmailVerificationRequestResult = { requestResult }
    var updateUsernameHandler: suspend (UserId, Username) -> Boolean = { _, _ -> updateUsernameResult }
    var setPasswordHandler: suspend (UserId, Password) -> Boolean = { _, _ -> setPasswordResult }

    val savedEmails = mutableListOf<Email?>()
    val requestedEmails = mutableListOf<Email>()
    val usernameUpdates = mutableListOf<Pair<UserId, Username>>()
    val passwordUpdates = mutableListOf<Pair<UserId, Password>>()
    var profileReads = 0
    var probeReads = 0

    override val userAuthorisedState: StateFlow<Boolean> = authorisedState
    override val currentUserIdFlow: StateFlow<UserId?> = currentUserIdState
    override val isCurrentUserRootFlow: StateFlow<Boolean> = rootState
    override val canChangeAvatarForOthersFlow: StateFlow<Boolean> = avatarForOthersState

    override suspend fun getAllUsers(): List<UsersFeatureUser> = emptyList()
    override suspend fun getUser(id: UserId): UsersFeatureUser? = null

    override suspend fun getMyProfile(): AuthFeatureUser? {
        profileReads += 1
        return profileHandler()
    }

    override suspend fun isEmailFeatureEnabled(): Boolean {
        probeReads += 1
        return probeHandler()
    }

    override suspend fun setMyEmail(email: Email?): Boolean {
        savedEmails += email
        return saveEmailHandler(email)
    }

    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult {
        requestedEmails += expectedEmail
        return requestHandler(expectedEmail)
    }

    override suspend fun updateUsername(id: UserId, username: Username): Boolean {
        usernameUpdates += id to username
        return updateUsernameHandler(id, username)
    }

    override suspend fun setPassword(id: UserId, password: Password): Boolean {
        passwordUpdates += id to password
        return setPasswordHandler(id, password)
    }

    override suspend fun deleteUser(id: UserId): Boolean = false
    override suspend fun getAvatar(userId: UserId): FileId? = null
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = null
    override fun imageUrl(id: FileId): String = ""
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
}

/** Recording navigation delegate used to prove ViewModel save and logout outcomes. */
internal class RecordingUserEditInteractor : UserEditViewInteractor {
    var navigateBackCalls = 0
    var savedCalls = 0
    var deletedCalls = 0

    override suspend fun onNavigateBack(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
        navigateBackCalls += 1
    }

    override suspend fun onSaved(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
        savedCalls += 1
    }

    override suspend fun onDeleted(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
        deletedCalls += 1
    }
}

/** Creates a navigation node without starting platform navigation infrastructure. */
internal fun userEditTestNode(userId: UserId): NavigationNode<UserEditViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, UserEditViewConfig(userId))
}
