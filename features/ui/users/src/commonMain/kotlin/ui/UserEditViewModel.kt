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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

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
 * @param dispatcher UI dispatcher for lifecycle and owner-email transitions. Production uses
 *   [Dispatchers.Main.immediate]; deterministic tests inject a shared serial test dispatcher.
 */
class UserEditViewModel(
    private val node: NavigationNode<UserEditViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UserEditViewInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel<ViewConfig>(node) {
    /**
     * UI-confined child scope with the ViewModel lifecycle [Job] inherited from [scope].
     *
     * All UI callbacks enter on the UI dispatcher, and collectors plus continuations use this
     * scope. That keeps owner generations, request versions, tokens, jobs, checks, and state
     * publications serial while preserving cancellation when the navigation node is destroyed.
     */
    private val workScope = CoroutineScope(scope.coroutineContext + dispatcher)

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
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    private val _showDeleteDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the delete confirmation dialog should be visible. */
    val showDeleteDialogState = _showDeleteDialogState.asStateFlow()

    private val _profileSaveErrorState = MutableRedeliverStateFlow<ProfileSaveError?>(null)

    /** Failure of the root-only username/password save sequence, kept separate from email feedback. */
    val profileSaveErrorState: StateFlow<ProfileSaveError?> = _profileSaveErrorState.asStateFlow()

    private val _emailCapabilityState = MutableRedeliverStateFlow(EmailCapabilityState.Unknown)

    /** Current owner-email capability probe state, including recoverable loading and failure states. */
    val emailCapabilityState: StateFlow<EmailCapabilityState> = _emailCapabilityState.asStateFlow()

    /** Whether the server has positively confirmed SMTP-backed self-service email verification. */
    val emailFeatureEnabledState: StateFlow<Boolean> = _emailCapabilityState
        .map { it == EmailCapabilityState.Enabled }
        .stateIn(workScope, SharingStarted.Eagerly, false)

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

    private val _emailLoadFailedState = MutableRedeliverStateFlow(false)

    /** `true` when a capability/profile reconciliation failed without replacing a primary operation error. */
    val emailLoadFailedState: StateFlow<Boolean> = _emailLoadFailedState.asStateFlow()

    private val _emailVerificationResultState = MutableRedeliverStateFlow<EmailVerificationRequestResult?>(null)

    /** Most recent verification-request outcome returned by the server. */
    val emailVerificationResultState: StateFlow<EmailVerificationRequestResult?> =
        _emailVerificationResultState.asStateFlow()

    /** `true` only for the current authenticated profile owner. */
    private val isCurrentAuthenticatedOwnerFlow =
        combine(model.currentUserIdFlow, model.userAuthorisedState) { currentUserId, authorised ->
            authorised && currentUserId == userId
        }

    /**
     * `true` while an authenticated owner may inspect email capability/loading/failure feedback.
     * A confirmed disabled capability hides the section; unknown and failed probes remain visible so
     * the owner can retry without exposing private feedback to another profile.
     */
    val canManageOwnEmailState: StateFlow<Boolean> =
        combine(isCurrentAuthenticatedOwnerFlow, _emailCapabilityState) { isOwner, capability ->
            isOwner && capability != EmailCapabilityState.Disabled
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /** `true` only after capability and the matching private owner profile are both confirmed. */
    val canMutateOwnEmailState: StateFlow<Boolean> =
        combine(
            isCurrentAuthenticatedOwnerFlow,
            _emailCapabilityState,
            _ownEmailProfileState,
            _emailLoadingState,
            _emailBusyState,
        ) { isOwner, capability, profile, loading, busy ->
            isOwner &&
                capability == EmailCapabilityState.Enabled &&
                profile?.id == userId &&
                !loading &&
                !busy
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    private var emailRefreshVersion = 0L

    private var ownerGeneration = 0L

    private var observedOwnerSession: OwnerSession? = null

    private var nextEmailMutationId = 0L

    private var activeEmailMutation: OwnerEmailMutation? = null

    private var emailRefreshJob: Job? = null

    private var pendingEmailRefresh = false

    /**
     * `true` when the typed password and confirmation differ while at least one is non-blank.
     * Used by the view to show an inline mismatch error.
     */
    val passwordMismatchState: StateFlow<Boolean> =
        combine(_passwordState, _confirmPasswordState) { password, confirm ->
            password != confirm && (password.isNotBlank() || confirm.isNotBlank())
        }.stateIn(workScope, SharingStarted.Eagerly, false)

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
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /**
     * `true` when the avatar uploader should be available: the caller is the profile owner, or holds
     * the `files.avatarChangeForOthers` functionality. Mirrors the server-side avatar-change guard.
     */
    val canUploadAvatarState: StateFlow<Boolean> =
        combine(model.currentUserIdFlow, model.canChangeAvatarForOthersFlow) { currentUserId, canForOthers ->
            currentUserId == userId || canForOthers
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { !inited }.subscribeLoggingDropExceptions(workScope) {
            _loadingState.value = true
            try {
                model.getUser(userId)?.let { _usernameState.value = it.username.string }
                _avatarIdState.value = model.getAvatar(userId)
            } finally {
                _loadingState.value = false
            }
            inited = true
        }
        model.userAuthorisedState.subscribeOnLoggedOut(workScope) {
            interactor.onNavigateBack(node)
        }
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(workScope) {
            requestOwnedEmailRefresh()
        }
        combine(model.currentUserIdFlow, model.userAuthorisedState) { callerId, authorised ->
            OwnerSession(callerId, authorised)
        }.subscribeLoggingDropExceptions(workScope) { session ->
            if (observedOwnerSession != session) {
                observedOwnerSession = session
                invalidateOwnedEmailSession()
                if (session.authorised && session.callerId == userId) {
                    requestOwnedEmailRefresh()
                }
            }
        }
    }

    private fun clearPrivateEmailState() {
        _ownEmailProfileState.value = null
        _emailInputState.value = ""
        _emailDraftDirtyState.value = false
        _emailErrorState.value = null
        _emailLoadFailedState.value = false
        _emailVerificationResultState.value = null
    }

    /** Invalidates all private email work when the observed caller or authorization state changes. */
    private fun invalidateOwnedEmailSession() {
        ownerGeneration += 1
        emailRefreshVersion += 1
        emailRefreshJob?.cancel()
        emailRefreshJob = null
        val mutation = activeEmailMutation
        activeEmailMutation = null
        mutation?.job?.cancel()
        pendingEmailRefresh = false
        _emailCapabilityState.value = EmailCapabilityState.Unknown
        _emailLoadingState.value = false
        _emailBusyState.value = false
        clearPrivateEmailState()
    }

    /** Returns whether [callerId] is still the authenticated owner of this editor. */
    private fun isCurrentRawOwner(callerId: UserId): Boolean =
        callerId == userId &&
            model.userAuthorisedState.value &&
            model.currentUserIdFlow.value == callerId

    /** Returns whether a private refresh still belongs to its initiating owner generation. */
    private fun isCurrentOwnerRequest(
        requestVersion: Long,
        generation: Long,
        callerId: UserId,
    ): Boolean =
        requestVersion == emailRefreshVersion &&
            generation == ownerGeneration &&
            isCurrentRawOwner(callerId)

    /** Returns whether [mutation] is still the active owner-bound email operation. */
    private fun isCurrentEmailMutation(mutation: OwnerEmailMutation): Boolean =
        activeEmailMutation === mutation &&
            mutation.generation == ownerGeneration &&
            isCurrentRawOwner(mutation.callerId)

    /** Rechecks cancellation and owner identity before an owner-scoped request or state publication. */
    private suspend fun canContinueEmailMutation(mutation: OwnerEmailMutation): Boolean {
        currentCoroutineContext().ensureActive()
        return isCurrentEmailMutation(mutation)
    }

    /** Starts or defers an owner-private capability/profile refresh without transferring an active mutation. */
    private fun requestOwnedEmailRefresh() {
        val callerId = model.currentUserIdFlow.value ?: return
        if (!isCurrentRawOwner(callerId)) return
        if (_emailBusyState.value) {
            pendingEmailRefresh = true
            return
        }

        emailRefreshJob?.cancel()
        val requestVersion = ++emailRefreshVersion
        val generation = ownerGeneration
        _emailCapabilityState.value = EmailCapabilityState.Loading
        _emailLoadingState.value = true
        _ownEmailProfileState.value = null
        _emailLoadFailedState.value = false
        emailRefreshJob = workScope.launch {
            try {
                val emailFeatureEnabled = model.isEmailFeatureEnabled()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                if (!emailFeatureEnabled) {
                    _emailCapabilityState.value = EmailCapabilityState.Disabled
                    _ownEmailProfileState.value = null
                    if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                        _emailErrorState.value = null
                    }
                    return@launch
                }

                _emailCapabilityState.value = EmailCapabilityState.Enabled
                val profile = model.getMyProfile()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                if (profile?.id != userId) {
                    _ownEmailProfileState.value = null
                    _emailCapabilityState.value = EmailCapabilityState.Failed
                    _emailLoadFailedState.value = true
                    if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                        _emailErrorState.value = EmailEditorError.LoadFailed
                    }
                    return@launch
                }

                _ownEmailProfileState.value = profile
                if (!_emailDraftDirtyState.value) {
                    _emailInputState.value = profile.email?.string.orEmpty()
                }
                _emailLoadFailedState.value = false
                if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (isCurrentOwnerRequest(requestVersion, generation, callerId)) {
                    _ownEmailProfileState.value = null
                    _emailCapabilityState.value = EmailCapabilityState.Failed
                    _emailLoadFailedState.value = true
                    if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                        _emailErrorState.value = EmailEditorError.LoadFailed
                    }
                }
            } finally {
                if (isCurrentOwnerRequest(requestVersion, generation, callerId)) {
                    _emailLoadingState.value = false
                }
            }
        }
    }

    /** Admits a single owner-bound email mutation only after capability and private profile confirmation. */
    private fun beginEmailMutation(): OwnerEmailMutation? {
        val callerId = model.currentUserIdFlow.value ?: return null
        if (!isCurrentRawOwner(callerId)) return null
        if (_emailCapabilityState.value != EmailCapabilityState.Enabled) return null
        if (_ownEmailProfileState.value?.id != userId) return null
        if (_emailLoadingState.value || _emailBusyState.value) return null

        val mutation = OwnerEmailMutation(callerId, ownerGeneration, ++nextEmailMutationId)
        activeEmailMutation = mutation
        _emailBusyState.value = true
        _emailErrorState.value = null
        _emailVerificationResultState.value = null
        return mutation
    }

    /** Starts [block] lazily so its cancellation handle is registered before work can run. */
    private fun launchEmailMutation(
        mutation: OwnerEmailMutation,
        block: suspend () -> Unit,
    ) {
        val job = workScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } finally {
                finishEmailMutation(mutation)
            }
        }
        mutation.job = job
        job.start()
    }

    /** Clears only the matching mutation's busy state and honors one deferred resume/manual refresh. */
    private fun finishEmailMutation(mutation: OwnerEmailMutation) {
        if (!isCurrentEmailMutation(mutation)) return
        activeEmailMutation = null
        _emailBusyState.value = false
        if (pendingEmailRefresh) {
            pendingEmailRefresh = false
            requestOwnedEmailRefresh()
        }
    }

    /** Reconciles only a still-current mutation with the server's private profile without probing capability again. */
    private suspend fun reconcileOwnedEmailProfile(mutation: OwnerEmailMutation) {
        if (!canContinueEmailMutation(mutation)) return
        try {
            val profile = model.getMyProfile()
            if (!canContinueEmailMutation(mutation)) return
            if (profile?.id != userId) {
                _ownEmailProfileState.value = null
                _emailLoadFailedState.value = true
                if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = EmailEditorError.LoadFailed
                }
                return
            }

            _ownEmailProfileState.value = profile
            val storedEmail = profile.email
            if (storedEmail != null) {
                _emailInputState.value = storedEmail.string
                _emailDraftDirtyState.value = false
            }
            _emailLoadFailedState.value = false
            if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                _emailErrorState.value = null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            if (isCurrentEmailMutation(mutation)) {
                _emailLoadFailedState.value = true
                if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = EmailEditorError.LoadFailed
                }
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
        workScope.launchLoggingDropExceptions {
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
            workScope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and pops. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        workScope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
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
        if (!canSaveState.value) return
        val username = Username(_usernameState.value.trim())
        val password = _passwordState.value
        _loadingState.value = true
        _profileSaveErrorState.value = null
        workScope.launchLoggingDropExceptions {
            try {
                val usernameSaved = try {
                    model.updateUsername(userId, username)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    false
                }
                if (!usernameSaved) {
                    _profileSaveErrorState.value = ProfileSaveError.UsernameSaveFailed
                    return@launchLoggingDropExceptions
                }
                if (password.isNotBlank()) {
                    val passwordSaved = try {
                        model.setPassword(userId, Password(password))
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        false
                    }
                    if (!passwordSaved) {
                        _profileSaveErrorState.value = ProfileSaveError.PasswordSaveFailed
                        return@launchLoggingDropExceptions
                    }
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Updates the private owner-email draft. No-op for other profiles or while a request is active. */
    fun onEmailChanged(email: String) {
        if (!canMutateOwnEmailState.value) return
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
        if (!canMutateOwnEmailState.value || _ownEmailProfileState.value?.email != null) return
        val email = Email.parse(_emailInputState.value).getOrNull()
        if (email == null) {
            _emailErrorState.value = EmailEditorError.InvalidEmail
            return
        }

        val mutation = beginEmailMutation() ?: return
        launchEmailMutation(mutation) {
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            val saved = try {
                model.setMyEmail(email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (isCurrentEmailMutation(mutation)) {
                    _emailErrorState.value = EmailEditorError.SaveFailed
                }
                reconcileOwnedEmailProfile(mutation)
                return@launchEmailMutation
            }
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            if (!saved) {
                _emailErrorState.value = EmailEditorError.SaveFailed
                reconcileOwnedEmailProfile(mutation)
                return@launchEmailMutation
            }

            _emailInputState.value = email.string
            _emailDraftDirtyState.value = false
            val result = try {
                if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
                model.requestMyEmailVerification(email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                EmailVerificationRequestResult.DeliveryFailed
            }
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            _emailVerificationResultState.value = result
            reconcileOwnedEmailProfile(mutation)
        }
    }

    /** Retries verification for the current saved, unapproved owner email without rewriting it. */
    fun onResendEmailVerification() {
        if (!canMutateOwnEmailState.value) return
        val profile = _ownEmailProfileState.value ?: return
        val email = profile.email ?: return
        if (profile.emailApproved) return

        val mutation = beginEmailMutation() ?: return
        launchEmailMutation(mutation) {
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            val result = try {
                model.requestMyEmailVerification(email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                EmailVerificationRequestResult.DeliveryFailed
            }
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            _emailVerificationResultState.value = result
            reconcileOwnedEmailProfile(mutation)
        }
    }

    /** Refreshes the private owner-email state after an external verification deeplink returns. */
    fun onRefreshEmail() {
        requestOwnedEmailRefresh()
    }

    /** Opens the delete confirmation dialog. No-op unless the caller is root. */
    fun onDeleteRequest() {
        if (!isRootState.value) return
        _showDeleteDialogState.value = true
    }

    /** Confirms deletion: removes the user (server cascade) then delegates to [UserEditViewInteractor.onDeleted]. */
    fun onConfirmDelete() {
        workScope.launchLoggingDropExceptions {
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

/** Root profile-save outcomes that keep the editor open for correction or retry. */
enum class ProfileSaveError {
    /** Username persistence was false or could not be confirmed; no password request was sent. */
    UsernameSaveFailed,

    /** Username persisted, but the optional password request was false or could not be confirmed. */
    PasswordSaveFailed,
}

/** Capability lifecycle for the owner-only email section. */
enum class EmailCapabilityState {
    /** No owner-private probe has completed for the current observed identity. */
    Unknown,

    /** A capability/profile refresh is in flight and mutations are fail-closed. */
    Loading,

    /** SMTP-backed owner verification and a matching private profile are available. */
    Enabled,

    /** The server confirmed that owner verification delivery is unavailable. */
    Disabled,

    /** Capability or private-profile loading failed; retry remains available but mutations are blocked. */
    Failed,
}

/** Observed authenticated-caller state used to invalidate owner-private work across identity changes. */
private data class OwnerSession(
    val callerId: UserId?,
    val authorised: Boolean,
)

/** One active owner-bound mutation with an explicit cancellation handle. */
private class OwnerEmailMutation(
    val callerId: UserId,
    val generation: Long,
    val mutationId: Long,
) {
    /** Job launched after this token becomes the active mutation. */
    var job: Job? = null
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
