package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.common.client.utils.subscribeOnLoggedOut
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the user profile edit screen.
 *
 * Access rules mirror the requirement:
 * - The screen is reachable by the profile **owner** and an admin-panel caller; the avatar uploader
 *   is shown when [canUploadAvatarState] is `true` (the owner, or a caller holding the
 *   `files.avatarChangeForOthers` functionality).
 * - A non-privileged owner has no admin-managed text fields ([isRootState] is `false`), but may
 *   manage the owner's own email when SMTP-backed email verification is enabled.
 * - An admin-panel caller ([isRootState] is `true`) may edit the username and set a new password
 *   (with a confirmation field that must match) and delete the user. The user id is never editable.
 *
 * Username/password mutations go through the admin-panel endpoints; the avatar upload goes through
 * the files feature (allowed for the owner or the `files.avatarChangeForOthers` functionality).
 * Server-side authorization is the source of truth — the gating here is purely presentational.
 *
 * On logout this screen exits unconditionally to the underlying profile (read) view via
 * [UserEditViewInteractor.onNavigateBack], bypassing the dirty-changes confirm dialog.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Users data source.
 * @param interactor Navigation delegate for this screen.
 */
class UserEditViewModel(
    private val node: NavigationNode<UserEditViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UserEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** Identifier of the edited user; surfaced read-only to the view. */
    val userId: UserId = node.config.userId

    /**
     * `true` when the caller is `root`; gates the editable username/password fields and delete.
     * Sourced reactively from [UsersModel.isCurrentUserRootFlow], so it self-corrects once the
     * cold-start `getMe()` round-trip completes and on later login/logout (PR #31 F2).
     */
    val isRootState: StateFlow<Boolean> = model.isCurrentUserRootFlow

    private val _usernameState = MutableRedeliverStateFlow("")

    /** Current value of the username input (editable by root only). */
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")

    /** Current value of the new-password input (root only; blank = keep current password). */
    val passwordState = _passwordState.asStateFlow()

    private val _confirmPasswordState = MutableRedeliverStateFlow("")

    /** Current value of the password confirmation input (root only). */
    val confirmPasswordState = _confirmPasswordState.asStateFlow()

    private val _avatarIdState = MutableRedeliverStateFlow<FileId?>(null)

    /** Current avatar file id of the edited user, or `null` when none. */
    val avatarIdState = _avatarIdState.asStateFlow()

    private val _uploadingAvatarState = MutableRedeliverStateFlow(false)

    /** `true` while an avatar upload is in flight. */
    val uploadingAvatarState = _uploadingAvatarState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a load/save/delete request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _profileDirtyState = MutableRedeliverStateFlow(false)

    private val _emailDraftDirtyState = MutableRedeliverStateFlow(false)

    /** `true` when an admin field or unsaved owner-email draft changes (gates the discard dialog). */
    val isDirtyState: StateFlow<Boolean> =
        combine(_profileDirtyState, _emailDraftDirtyState) { profileDirty, emailDirty ->
            profileDirty || emailDirty
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    private val _showDeleteDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the delete confirmation dialog should be visible. */
    val showDeleteDialogState = _showDeleteDialogState.asStateFlow()

    private val _emailFeatureEnabledState = MutableRedeliverStateFlow(false)

    /** Whether the server currently permits SMTP-backed self-service email verification. */
    val emailFeatureEnabledState: StateFlow<Boolean> = _emailFeatureEnabledState.asStateFlow()

    private val _ownEmailProfileState = MutableRedeliverStateFlow<AuthFeatureUser?>(null)

    /** Private current-owner record used only by the owner-email controls. */
    val ownEmailProfileState: StateFlow<AuthFeatureUser?> = _ownEmailProfileState.asStateFlow()

    private val _emailInputState = MutableRedeliverStateFlow("")

    /** Current owner-email input; this draft is never populated from the public users surface. */
    val emailInputState: StateFlow<String> = _emailInputState.asStateFlow()

    private val _emailLoadingState = MutableRedeliverStateFlow(false)

    /** `true` while the private owner profile or email capability is refreshing. */
    val emailLoadingState: StateFlow<Boolean> = _emailLoadingState.asStateFlow()

    private val _emailBusyState = MutableRedeliverStateFlow(false)

    /** `true` while an owner-email save or verification request is in flight. */
    val emailBusyState: StateFlow<Boolean> = _emailBusyState.asStateFlow()

    private val _emailErrorState = MutableRedeliverStateFlow<EmailEditorError?>(null)

    /** Local validation, load, or persistence error for the owner-email controls. */
    val emailErrorState: StateFlow<EmailEditorError?> = _emailErrorState.asStateFlow()

    private val _emailVerificationResultState = MutableRedeliverStateFlow<EmailVerificationRequestResult?>(null)

    /** Most recent verification-request outcome returned by the server. */
    val emailVerificationResultState: StateFlow<EmailVerificationRequestResult?> =
        _emailVerificationResultState.asStateFlow()

    /** `true` only for the current authenticated profile owner while email delivery is enabled. */
    val canManageOwnEmailState: StateFlow<Boolean> =
        combine(model.currentUserIdFlow, _emailFeatureEnabledState) { currentUserId, emailFeatureEnabled ->
            currentUserId == userId && emailFeatureEnabled
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private var emailRefreshVersion = 0L

    /**
     * `true` when the typed password and confirmation differ while at least one is non-blank.
     * Used by the view to show an inline mismatch error.
     */
    val passwordMismatchState: StateFlow<Boolean> =
        combine(_passwordState, _confirmPasswordState) { password, confirm ->
            password != confirm && (password.isNotBlank() || confirm.isNotBlank())
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * `true` when the form may be saved: caller is root, username is non-blank, no request is in
     * flight, and the optional new password (when provided) matches its confirmation.
     */
    val canSaveState: StateFlow<Boolean> =
        combine(
            model.isCurrentUserRootFlow,
            _usernameState,
            _passwordState,
            _confirmPasswordState,
            _loadingState
        ) { isRoot, username, password, confirm, loading ->
            val passwordOk = (password.isBlank() && confirm.isBlank()) || password == confirm
            isRoot && username.isNotBlank() && !loading && passwordOk
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * `true` when the avatar uploader should be available: the caller is the profile owner, or holds
     * the `files.avatarChangeForOthers` functionality. Mirrors the server-side avatar-change guard.
     */
    val canUploadAvatarState: StateFlow<Boolean> =
        combine(model.currentUserIdFlow, model.canChangeAvatarForOthersFlow) { currentUserId, canForOthers ->
            currentUserId == userId || canForOthers
        }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { !inited }.subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                model.getUser(userId)?.let { _usernameState.value = it.username.string }
                _avatarIdState.value = model.getAvatar(userId)
            } finally {
                _loadingState.value = false
            }
            inited = true
        }
        model.userAuthorisedState.subscribeOnLoggedOut(scope) {
            interactor.onNavigateBack(node)
        }
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            refreshOwnedEmailProfile()
        }
        model.currentUserIdFlow.subscribeLoggingDropExceptions(scope) { currentUserId ->
            if (currentUserId == userId) {
                refreshOwnedEmailProfile()
            } else {
                clearOwnedEmailState()
            }
        }
    }

    private fun clearPrivateEmailState() {
        _ownEmailProfileState.value = null
        _emailInputState.value = ""
        _emailDraftDirtyState.value = false
        _emailErrorState.value = null
        _emailVerificationResultState.value = null
    }

    private fun clearOwnedEmailState() {
        emailRefreshVersion += 1
        _emailFeatureEnabledState.value = false
        _emailLoadingState.value = false
        _emailBusyState.value = false
        clearPrivateEmailState()
    }

    private fun isCurrentOwnerRequest(requestVersion: Long, callerId: UserId): Boolean =
        requestVersion == emailRefreshVersion && callerId == userId && model.currentUserIdFlow.value == callerId

    private suspend fun refreshOwnedEmailProfile() {
        val callerId = model.currentUserIdFlow.value
        if (callerId != userId) {
            clearOwnedEmailState()
            return
        }

        val requestVersion = ++emailRefreshVersion
        _emailLoadingState.value = true
        try {
            val emailFeatureEnabled = model.isEmailFeatureEnabled()
            if (!isCurrentOwnerRequest(requestVersion, callerId)) return
            _emailFeatureEnabledState.value = emailFeatureEnabled
            if (!emailFeatureEnabled) {
                clearPrivateEmailState()
                return
            }

            val profile = model.getMyProfile()
            if (!isCurrentOwnerRequest(requestVersion, callerId)) return
            if (profile?.id != userId) {
                clearOwnedEmailState()
                return
            }

            _ownEmailProfileState.value = profile
            if (!_emailDraftDirtyState.value) {
                _emailInputState.value = profile.email?.string.orEmpty()
            }
            if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                _emailErrorState.value = null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            if (isCurrentOwnerRequest(requestVersion, callerId)) {
                _ownEmailProfileState.value = null
                _emailErrorState.value = EmailEditorError.LoadFailed
            }
        } finally {
            if (isCurrentOwnerRequest(requestVersion, callerId)) {
                _emailLoadingState.value = false
            }
        }
    }

    /**
     * Updates the username field and marks the form dirty (root only).
     *
     * @param username New username value.
     */
    fun onUsernameChanged(username: String) {
        _usernameState.value = username
        _profileDirtyState.value = true
    }

    /**
     * Updates the new-password field and marks the form dirty (root only).
     *
     * @param password New password value.
     */
    fun onPasswordChanged(password: String) {
        _passwordState.value = password
        _profileDirtyState.value = true
    }

    /**
     * Updates the password confirmation field and marks the form dirty (root only).
     *
     * @param confirm Confirmation value; must match [passwordState] to save.
     */
    fun onConfirmPasswordChanged(confirm: String) {
        _confirmPasswordState.value = confirm
        _profileDirtyState.value = true
    }

    /**
     * Uploads [file] and sets it as this user's avatar (owner or `files.avatarChangeForOthers`).
     * No-op unless [canUploadAvatarState] is `true`. Refreshes [avatarIdState] on success. Avatar
     * changes persist immediately and do not affect [isDirtyState].
     *
     * @param file Image chosen by the user on the current platform.
     */
    fun onAvatarPicked(file: MPPFile) {
        if (!canUploadAvatarState.value) return
        scope.launchLoggingDropExceptions {
            _uploadingAvatarState.value = true
            try {
                val newId = model.uploadAvatar(userId, file)
                if (newId != null) _avatarIdState.value = newId
            } finally {
                _uploadingAvatarState.value = false
            }
        }
    }

    /**
     * Attempts to navigate back. Shows the discard dialog when the form is dirty, otherwise pops.
     */
    fun onBack() {
        if (isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and pops. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the discard dialog, returning to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves root-editable fields: updates the username and, when a new password was entered and
     * confirmed, sets it. No-op unless [canSaveState] is `true`.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            if (!canSaveState.value) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                model.updateUsername(userId, Username(_usernameState.value.trim()))
                val password = _passwordState.value
                if (password.isNotBlank()) {
                    model.setPassword(userId, Password(password))
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Updates the private owner-email draft. No-op for other profiles or while a request is active. */
    fun onEmailChanged(email: String) {
        if (!canManageOwnEmailState.value || _emailBusyState.value) return
        _emailInputState.value = email
        _emailDraftDirtyState.value = true
        _emailErrorState.value = null
        _emailVerificationResultState.value = null
    }

    /**
     * Persists a missing or replacement owner email, then requests verification for that exact value.
     *
     * A failed persistence attempt never sends a verification request. After a successful persistence
     * the server outcome is exposed verbatim, including a delivery failure, and the private profile
     * is refreshed so the latest approval state wins.
     */
    fun onSaveEmailAndRequestVerification() {
        if (!canManageOwnEmailState.value || _emailLoadingState.value || _emailBusyState.value) return
        val email = Email.parse(_emailInputState.value).getOrNull()
        if (email == null) {
            _emailErrorState.value = EmailEditorError.InvalidEmail
            return
        }

        _emailBusyState.value = true
        _emailErrorState.value = null
        _emailVerificationResultState.value = null
        scope.launchLoggingDropExceptions {
            try {
                if (!model.setMyEmail(email)) {
                    _emailErrorState.value = EmailEditorError.SaveFailed
                    return@launchLoggingDropExceptions
                }
                _emailInputState.value = email.string
                _emailDraftDirtyState.value = false
                _emailVerificationResultState.value = model.requestMyEmailVerification(email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _emailVerificationResultState.value = EmailVerificationRequestResult.DeliveryFailed
            } finally {
                _emailBusyState.value = false
                refreshOwnedEmailProfile()
            }
        }
    }

    /** Retries verification for the current saved, unapproved owner email without rewriting it. */
    fun onResendEmailVerification() {
        if (!canManageOwnEmailState.value || _emailLoadingState.value || _emailBusyState.value) return
        val profile = _ownEmailProfileState.value ?: return
        val email = profile.email ?: return
        if (profile.emailApproved) return

        _emailBusyState.value = true
        _emailErrorState.value = null
        _emailVerificationResultState.value = null
        scope.launchLoggingDropExceptions {
            try {
                _emailVerificationResultState.value = model.requestMyEmailVerification(email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _emailVerificationResultState.value = EmailVerificationRequestResult.DeliveryFailed
            } finally {
                _emailBusyState.value = false
                refreshOwnedEmailProfile()
            }
        }
    }

    /** Refreshes the private owner-email state after an external verification deeplink returns. */
    fun onRefreshEmail() {
        scope.launchLoggingDropExceptions { refreshOwnedEmailProfile() }
    }

    /** Opens the delete confirmation dialog. No-op unless the caller is root. */
    fun onDeleteRequest() {
        if (!isRootState.value) return
        _showDeleteDialogState.value = true
    }

    /** Confirms deletion: removes the user (server cascade) then delegates to [UserEditViewInteractor.onDeleted]. */
    fun onConfirmDelete() {
        scope.launchLoggingDropExceptions {
            _showDeleteDialogState.value = false
            _loadingState.value = true
            try {
                model.deleteUser(userId)
            } finally {
                _loadingState.value = false
            }
            interactor.onDeleted(node)
        }
    }

    /** Cancels the delete dialog. */
    fun onCancelDelete() {
        _showDeleteDialogState.value = false
    }

    /**
     * Builds the URL of the avatar image [id] for direct rendering (JS).
     *
     * @param id Avatar file id.
     * @return Relative URL resolved against the server base URL.
     */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /**
     * Downloads avatar bytes for platforms that decode images themselves (JVM/Android).
     *
     * @param id Avatar file id.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}

/** Local presentation errors that are distinct from server verification outcomes. */
enum class EmailEditorError {
    /** The typed draft is not a valid email address. */
    InvalidEmail,

    /** The capability/profile refresh could not obtain the private owner record. */
    LoadFailed,

    /** The server did not persist the requested owner email. */
    SaveFailed,
}
