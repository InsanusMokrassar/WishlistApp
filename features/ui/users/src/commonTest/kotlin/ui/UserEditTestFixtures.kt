package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.EmailProfile
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import korlibs.time.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Mutable, hook-driven model for deterministic owner-email and root-save ViewModel tests. */
internal class UserEditTestUsersModel(
    initialUserId: UserId?,
    initialProfile: EmailProfile?,
    initiallyAuthorised: Boolean = initialUserId != null,
) : UsersModel {
    val currentUserIdState = MutableStateFlow(initialUserId)
    val authorisedState = MutableStateFlow(initiallyAuthorised)
    val rootState = MutableStateFlow(false)
    val avatarForOthersState = MutableStateFlow(false)
    val profileState = MutableStateFlow(initialProfile)

    var emailFeatureEnabled = true
    var nextEmailChangeRequestedAt = 10_000L
    var saveEmailResult = true
    var requestResult = EmailVerificationRequestResult.Sent
    var passwordChangeRequestResult: PasswordChangeEmailRequestResult? = PasswordChangeEmailRequestResult.Sent
    var passwordChangeResult: PasswordChangeResult? = PasswordChangeResult.Changed
    var updateUsernameResult = true
    var setPasswordResult = true

    var probeHandler: suspend () -> Boolean = { emailFeatureEnabled }
    var profileHandler: suspend () -> EmailProfile? = { profileState.value }
    var saveEmailHandler: suspend (Email?) -> Boolean = { email ->
        if (saveEmailResult) {
            profileState.value = profileState.value?.let { profile ->
                when {
                    email == null -> profile.copy(
                        email = null,
                        emailApproved = false,
                        pendingEmail = null,
                        emailChangeRequestedAt = null,
                        emailChangeAllowedAt = null,
                    )
                    email == profile.email || email == profile.pendingEmail -> profile
                    profile.emailApproved -> profile.copy(
                        pendingEmail = email,
                        emailChangeRequestedAt = DateTime.fromUnixMillis(nextEmailChangeRequestedAt++),
                    )
                    else -> profile.copy(
                        email = email,
                        emailApproved = false,
                        pendingEmail = null,
                        emailChangeRequestedAt = DateTime.fromUnixMillis(nextEmailChangeRequestedAt++),
                    )
                }
            }
        }
        saveEmailResult
    }
    var requestHandler: suspend (Email) -> EmailVerificationRequestResult = { requestResult }
    var passwordChangeRequestHandler: suspend (Email) -> PasswordChangeEmailRequestResult? = { passwordChangeRequestResult }
    var passwordChangeHandler: suspend (CompletePasswordChangeRequest) -> PasswordChangeResult? = { passwordChangeResult }
    var updateUsernameHandler: suspend (UserId, Username) -> Boolean = { _, _ -> updateUsernameResult }
    var setPasswordHandler: suspend (UserId, Password) -> Boolean = { _, _ -> setPasswordResult }

    val savedEmails = mutableListOf<Email?>()
    val requestedEmails = mutableListOf<Email>()
    val passwordChangeRequestedEmails = mutableListOf<Email>()
    val passwordChangeRequests = mutableListOf<CompletePasswordChangeRequest>()
    val emailEvents = mutableListOf<String>()
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

    override suspend fun getMyEmailProfile(): EmailProfile? {
        profileReads += 1
        emailEvents += "GET"
        return profileHandler()
    }

    override suspend fun isEmailFeatureEnabled(): Boolean {
        probeReads += 1
        emailEvents += "PROBE"
        return probeHandler()
    }

    override suspend fun setMyEmail(email: Email?): Boolean {
        savedEmails += email
        emailEvents += "PUT:${email?.string}"
        return saveEmailHandler(email)
    }

    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult {
        requestedEmails += expectedEmail
        emailEvents += "POST:${expectedEmail.string}"
        return requestHandler(expectedEmail)
    }

    override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult? {
        passwordChangeRequestedEmails += expectedEmail
        return passwordChangeRequestHandler(expectedEmail)
    }

    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult? {
        passwordChangeRequests += request
        return passwordChangeHandler(request)
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

/** Mutable navigation node exposing live target and lifecycle transitions to deterministic tests. */
internal class UserEditTestNode(
    initialUserId: UserId,
) : NavigationNode<UserEditViewConfig, ViewConfig>() {
    /** Isolated navigation chain required by the production ViewModel contract. */
    override val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )

    private val mutableConfigState = MutableStateFlow(UserEditViewConfig(initialUserId))

    /** Live editor target observed by owner-private state. */
    override val configState: StateFlow<UserEditViewConfig> = mutableConfigState

    /** Changes the live navigation target without replacing the bound ViewModel instance. */
    fun retarget(userId: UserId) {
        mutableConfigState.value = UserEditViewConfig(userId)
    }

    /** Emits a normal resume transition, pausing first when already resumed. */
    suspend fun resume() {
        if (state == NavigationNodeState.RESUMED) {
            changeState(NavigationNodeState.STARTED)
        }
        changeState(NavigationNodeState.RESUMED)
    }

    /** Emits destruction so the real ViewModel lifecycle cancels active work. */
    suspend fun destroy() {
        changeState(NavigationNodeState.NEW)
    }
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

/** Creates a live-config navigation node without starting platform navigation infrastructure. */
internal fun userEditTestNode(userId: UserId): UserEditTestNode = UserEditTestNode(userId)

/** Recording delegate for password-completion navigation. */
internal class RecordingPasswordChangeInteractor : PasswordChangeViewInteractor {
    var changedCalls = 0
    var continueCalls = 0

    override suspend fun onChanged(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        changedCalls += 1
    }

    override suspend fun onContinue(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        continueCalls += 1
    }
}

/** Creates an isolated password-change node without platform navigation infrastructure. */
internal fun passwordChangeTestNode(
    config: PasswordChangeViewConfig,
): NavigationNode<PasswordChangeViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, config)
}
