package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
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
    /** Mutable caller identity observed by the editor. */
    val currentUserIdState = MutableStateFlow(initialUserId)
    /** Mutable authorization state observed by the editor. */
    val authorisedState = MutableStateFlow(initiallyAuthorised)
    /** Mutable root-capability state observed by the editor. */
    val rootState = MutableStateFlow(false)
    /** Mutable avatar capability state used by unrelated editor branches. */
    val avatarForOthersState = MutableStateFlow(false)
    /** Mutable private profile returned by reconciliation. */
    val profileState = MutableStateFlow(initialProfile)

    /** Whether the fake SMTP capability probe succeeds. */
    var emailFeatureEnabled = true
    /** Result returned by the fake private-email storage operation. */
    var saveEmailResult = true
    /** Result returned by the fake email verification request. */
    var requestResult = EmailVerificationRequestResult.Sent
    /** Result returned by the fake password-change email request. */
    var passwordChangeRequestResult: PasswordChangeEmailRequestResult? = PasswordChangeEmailRequestResult.Sent
    /** Result returned by the fake password-change completion. */
    var passwordChangeResult: PasswordChangeResult? = PasswordChangeResult.Changed
    /** Result returned by the fake username mutation. */
    var updateUsernameResult = true
    /** Result returned by the fake administrator password mutation. */
    var setPasswordResult = true

    /** Hook controlling capability-probe suspension and result. */
    var probeHandler: suspend () -> Boolean = { emailFeatureEnabled }
    /** Hook controlling private-profile suspension and result. */
    var profileHandler: suspend () -> AuthFeatureUser? = { profileState.value }
    /** Hook controlling private-email storage suspension and result. */
    var saveEmailHandler: suspend (Email?) -> Boolean = { email ->
        if (saveEmailResult) {
            profileState.value = profileState.value?.copy(email = email, emailApproved = false)
        }
        saveEmailResult
    }
    /** Hook controlling verification-request suspension and result. */
    var requestHandler: suspend (Email) -> EmailVerificationRequestResult = { requestResult }
    /** Hook controlling password-email request suspension and result. */
    var passwordChangeRequestHandler: suspend (Email) -> PasswordChangeEmailRequestResult? = {
        passwordChangeRequestResult
    }
    /** Hook controlling password completion suspension and result. */
    var passwordChangeHandler: suspend (CompletePasswordChangeRequest) -> PasswordChangeResult? = {
        passwordChangeResult
    }
    /** Hook controlling username mutation suspension and result. */
    var updateUsernameHandler: suspend (UserId, Username) -> Boolean = { _, _ -> updateUsernameResult }
    /** Hook controlling administrator password mutation suspension and result. */
    var setPasswordHandler: suspend (UserId, Password) -> Boolean = { _, _ -> setPasswordResult }

    /** Recorded private-email saves. */
    val savedEmails = mutableListOf<Email?>()
    /** Recorded email verification requests. */
    val requestedEmails = mutableListOf<Email>()
    /** Recorded password-change email requests. */
    val passwordChangeRequestedEmails = mutableListOf<Email>()
    /** Recorded password-change completion requests. */
    val passwordChangeRequests = mutableListOf<CompletePasswordChangeRequest>()
    /** Recorded username mutations. */
    val usernameUpdates = mutableListOf<Pair<UserId, Username>>()
    /** Recorded administrator password mutations. */
    val passwordUpdates = mutableListOf<Pair<UserId, Password>>()
    /** Number of private-profile reads. */
    var profileReads = 0
    /** Number of SMTP capability probes. */
    var probeReads = 0

    /** Exposes fake authorization state through the production interface. */
    override val userAuthorisedState: StateFlow<Boolean> = authorisedState
    /** Exposes fake caller identity through the production interface. */
    override val currentUserIdFlow: StateFlow<UserId?> = currentUserIdState
    /** Exposes fake root capability through the production interface. */
    override val isCurrentUserRootFlow: StateFlow<Boolean> = rootState
    /** Exposes fake avatar capability through the production interface. */
    override val canChangeAvatarForOthersFlow: StateFlow<Boolean> = avatarForOthersState

    /** Returns no public users. */
    override suspend fun getAllUsers(): List<UsersFeatureUser> = emptyList()
    /** Returns no public user detail. */
    override suspend fun getUser(id: UserId): UsersFeatureUser? = null

    /** Reads the configured private profile through the recording hook. */
    override suspend fun getMyProfile(): AuthFeatureUser? {
        profileReads += 1
        return profileHandler()
    }

    /** Probes the configured email capability through the recording hook. */
    override suspend fun isEmailFeatureEnabled(): Boolean {
        probeReads += 1
        return probeHandler()
    }

    /** Records and executes a private-email storage mutation. */
    override suspend fun setMyEmail(email: Email?): Boolean {
        savedEmails += email
        return saveEmailHandler(email)
    }

    /** Records and executes an email-verification request. */
    override suspend fun requestMyEmailVerification(expectedEmail: Email): EmailVerificationRequestResult {
        requestedEmails += expectedEmail
        return requestHandler(expectedEmail)
    }

    /** Records and executes a password-change email request. */
    override suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult? {
        passwordChangeRequestedEmails += expectedEmail
        return passwordChangeRequestHandler(expectedEmail)
    }

    /** Records and executes a password-change completion request. */
    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult? {
        passwordChangeRequests += request
        return passwordChangeHandler(request)
    }

    /** Records and executes a username mutation. */
    override suspend fun updateUsername(id: UserId, username: Username): Boolean {
        usernameUpdates += id to username
        return updateUsernameHandler(id, username)
    }

    /** Records and executes an administrator password mutation. */
    override suspend fun setPassword(id: UserId, password: Password): Boolean {
        passwordUpdates += id to password
        return setPasswordHandler(id, password)
    }

    /** Returns no deletion result for unrelated editor tests. */
    override suspend fun deleteUser(id: UserId): Boolean = false
    /** Returns no avatar metadata. */
    override suspend fun getAvatar(userId: UserId): FileId? = null
    /** Rejects avatar uploads. */
    override suspend fun uploadAvatar(userId: UserId, file: MPPFile): FileId? = null
    /** Returns no avatar URL. */
    override fun imageUrl(id: FileId): String = ""
    /** Returns no avatar bytes. */
    override suspend fun loadImageBytes(id: FileId): ByteArray? = null
}

/** Recording navigation delegate used to prove ViewModel save and logout outcomes. */
internal class RecordingUserEditInteractor : UserEditViewInteractor {
    /** Number of logout/back navigation callbacks. */
    var navigateBackCalls = 0
    /** Number of successful editor-save callbacks. */
    var savedCalls = 0
    /** Number of editor-delete callbacks. */
    var deletedCalls = 0

    /** Records a back-navigation callback. */
    override suspend fun onNavigateBack(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
        navigateBackCalls += 1
    }

    /** Records a successful save callback. */
    override suspend fun onSaved(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
        savedCalls += 1
    }

    /** Records a successful delete callback. */
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

/** Recording navigation delegate used to prove completion never keeps an actionable route. */
internal class RecordingPasswordChangeInteractor : PasswordChangeViewInteractor {
    /** Number of accepted completion transitions. */
    var changedCalls = 0

    /** Number of explicit exits from a password-change screen. */
    var continueCalls = 0

    /** Records the credential-free replacement transition. */
    override suspend fun onChanged(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        changedCalls += 1
    }

    /** Records the safe exit transition. */
    override suspend fun onContinue(node: NavigationNode<PasswordChangeViewConfig, ViewConfig>) {
        continueCalls += 1
    }
}

/** Creates an isolated password-change node without starting platform navigation infrastructure. */
internal fun passwordChangeTestNode(
    config: PasswordChangeViewConfig,
): NavigationNode<PasswordChangeViewConfig, ViewConfig> {
    val chain = NavigationChain<ViewConfig>(
        parentNode = null,
        nodeFactory = NavigationNodeFactory { _, _ -> null },
    )
    return NavigationNode.Empty(chain, config)
}
