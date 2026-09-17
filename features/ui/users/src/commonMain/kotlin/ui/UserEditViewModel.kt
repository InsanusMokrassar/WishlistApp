package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailChangeCooldownException
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
import korlibs.time.DateTime

/**
 * ViewModel for the user profile edit screen.
 *
 * Access rules mirror the requirement:
 * - The screen is reachable by the profile **owner** and an admin-panel caller; the avatar uploader
 *   is shown when [canUploadAvatarState] is `true` (the owner, or a caller holding the
 *   `files.avatarChangeForOthers` functionality).
 * - A non-privileged owner has no admin-managed text fields ([isRootState] is `false`), but may
 *   store or replace the owner's own email. SMTP availability controls verification delivery only.
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
 * @param nowMillis Epoch-millisecond clock used only for advisory cooldown presentation and immediate
 *   callback admission. The server remains the authorization source of truth.
 */
class UserEditViewModel(
    private val node: NavigationNode<UserEditViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UserEditViewInteractor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val nowMillis: () -> Long = DateTime::nowUnixMillisLong,
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

    /** Whether the server has positively confirmed SMTP-backed owner-email verification delivery. */
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

    /** Most recent verification-request outcome bound to the reconciled saved recipient. */
    val emailVerificationResultState: StateFlow<EmailVerificationRequestResult?> =
        _emailVerificationResultState.asStateFlow()

    private val _emailSavedState = MutableRedeliverStateFlow<Email?>(null)

    /** Acknowledged saved address confirmed by the mutation's final matching private read. */
    val emailSavedState: StateFlow<Email?> = _emailSavedState.asStateFlow()

    private val _emailOperationInterruptedState = MutableRedeliverStateFlow(false)

    /** `true` when active owner-email work was invalidated by an account or selected-target change. */
    val emailOperationInterruptedState: StateFlow<Boolean> =
        _emailOperationInterruptedState.asStateFlow()

    private val _emailChangeRestrictionState = MutableRedeliverStateFlow<Long?>(null)

    /** Authoritative cooldown deadline currently preventing a local save, or `null` when allowed. */
    val emailChangeRestrictionState: StateFlow<Long?> = _emailChangeRestrictionState.asStateFlow()

    private val _emailChangeAllowedState = MutableRedeliverStateFlow(true)

    /** `true` when the checked profile has no active durable email-change deadline. */
    val emailChangeAllowedState: StateFlow<Boolean> = _emailChangeAllowedState.asStateFlow()

    /** Checked email-and-approval snapshot supporting [emailSavedState], or `null` without a saved marker. */
    private var emailSavedSnapshot: EmailFeedbackSnapshot? = null

    /** Checked email-and-approval snapshot supporting [emailVerificationResultState], or `null` without a result. */
    private var emailVerificationSnapshot: EmailFeedbackSnapshot? = null

    /** Checked state supporting [emailChangeRestrictionState], or `null` for a transport-only rejection. */
    private var emailCooldownSnapshot: EmailFeedbackSnapshot? = null

    /** `true` only for the current authenticated profile owner. */
    private val isCurrentAuthenticatedOwnerFlow =
        combine(
            model.currentUserIdFlow,
            model.userAuthorisedState,
            node.configState,
        ) { currentUserId, authorised, config ->
            authorised && currentUserId == userId && config.userId == userId
        }

    /**
     * `true` while an authenticated owner may inspect email capability/loading/failure feedback.
     * Unknown, failed, enabled, and disabled capability states remain visible for recovery or storage.
     */
    val canManageOwnEmailState: StateFlow<Boolean> = isCurrentAuthenticatedOwnerFlow
        .stateIn(workScope, SharingStarted.Eagerly, false)

    /** `true` when profile loading, reconciliation failure, and email mutation are all absent. */
    private val emailMutationIdleState: StateFlow<Boolean> =
        combine(_emailLoadingState, _emailBusyState, _emailLoadFailedState) { loading, busy, loadFailed ->
            !loading && !busy && !loadFailed
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /** `true` when owner storage is available with a matching private profile and no active work. */
    val canMutateOwnEmailState: StateFlow<Boolean> =
        combine(
            isCurrentAuthenticatedOwnerFlow,
            _emailCapabilityState,
            _ownEmailProfileState,
            emailMutationIdleState,
            _emailChangeAllowedState,
        ) { isOwner, capability, profile, idle, changeAllowed ->
            isOwner &&
                capability.allowsStorage &&
                profile?.id == userId &&
                idle &&
                changeAllowed
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /** `true` when the current draft parses and differs from both persisted email slots. */
    val canSaveEmailState: StateFlow<Boolean> =
        combine(canMutateOwnEmailState, _emailInputState, _ownEmailProfileState) { canMutate, input, profile ->
            canMutate && Email.parse(input).getOrNull()?.let { email ->
                email != profile?.email && email != profile?.pendingEmail
            } == true
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /** `true` when enabled SMTP can verify the authoritative saved pending address. */
    val canResendEmailVerificationState: StateFlow<Boolean> =
        combine(canMutateOwnEmailState, _emailCapabilityState, _ownEmailProfileState) { canMutate, capability, profile ->
            canMutate &&
                capability == EmailCapabilityState.Enabled &&
            profile?.verificationCandidate() != null
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
        combine(
            model.currentUserIdFlow,
            model.userAuthorisedState,
            node.configState,
        ) { callerId, authorised, config ->
            OwnerSession(callerId, authorised, config.userId)
        }.subscribeLoggingDropExceptions(workScope) { session ->
            if (observedOwnerSession != session) {
                observedOwnerSession = session
                invalidateOwnedEmailSession()
                if (session.isOwnerOf(userId)) {
                    requestOwnedEmailRefresh()
                }
            }
        }
    }

    /** Clears every address-bearing private state value for the bound editor. */
    private fun clearPrivateEmailState() {
        _ownEmailProfileState.value = null
        _emailInputState.value = ""
        _emailDraftDirtyState.value = false
        _emailErrorState.value = null
        _emailLoadFailedState.value = false
        _emailChangeRestrictionState.value = null
        _emailChangeAllowedState.value = true
        emailCooldownSnapshot = null
        clearEmailSavedFeedback()
        clearEmailVerificationFeedback()
    }

    /** Invalidates all private email work when the caller, authorization, or selected target changes. */
    private fun invalidateOwnedEmailSession() {
        val interrupted = activeEmailMutation != null
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
        if (interrupted) {
            _emailOperationInterruptedState.value = true
        }
    }

    /** Returns the current raw caller, authorization, and selected-target snapshot. */
    private fun currentOwnerSession(): OwnerSession = OwnerSession(
        callerId = model.currentUserIdFlow.value,
        authorised = model.userAuthorisedState.value,
        selectedUserId = node.config.userId,
    )

    /** Synchronizes a raw session change before an imperative callback can use stale derived state. */
    private fun synchronizeOwnerSession(): OwnerSession {
        val session = currentOwnerSession()
        if (observedOwnerSession != session) {
            observedOwnerSession = session
            invalidateOwnedEmailSession()
        }
        return session
    }

    /** Returns whether [callerId] is still the authenticated owner and selected target of this editor. */
    private fun isCurrentRawOwner(callerId: UserId): Boolean =
        callerId == userId &&
            model.userAuthorisedState.value &&
            model.currentUserIdFlow.value == callerId &&
            node.config.userId == userId

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
            mutation.selectedUserId == userId &&
            (!mutation.lifecycleStarted || node.state != NavigationNodeState.NEW) &&
            isCurrentRawOwner(mutation.callerId)

    /** Rechecks cancellation and owner identity before an owner-scoped request or state publication. */
    private suspend fun canContinueEmailMutation(mutation: OwnerEmailMutation): Boolean {
        currentCoroutineContext().ensureActive()
        return isCurrentEmailMutation(mutation)
    }

    /** Starts or defers an owner-private capability/profile refresh without transferring an active mutation. */
    private fun requestOwnedEmailRefresh() {
        val session = synchronizeOwnerSession()
        val callerId = session.callerId ?: return
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
                currentCoroutineContext().ensureActive()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                val emailFeatureEnabled = model.isEmailFeatureEnabled()
                currentCoroutineContext().ensureActive()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                _emailCapabilityState.value = if (emailFeatureEnabled) {
                    EmailCapabilityState.Enabled
                } else {
                    EmailCapabilityState.Disabled
                }
                currentCoroutineContext().ensureActive()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                val profile = model.getMyProfile()
                currentCoroutineContext().ensureActive()
                if (!isCurrentOwnerRequest(requestVersion, generation, callerId)) return@launch
                if (profile?.id != userId) {
                    _ownEmailProfileState.value = null
                    clearCompletedEmailFeedback()
                    _emailCapabilityState.value = EmailCapabilityState.Failed
                    _emailLoadFailedState.value = true
                    if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                        _emailErrorState.value = EmailEditorError.LoadFailed
                    }
                    return@launch
                }

                applyOwnedEmailProfile(profile, preserveDraft = _emailDraftDirtyState.value)
                _emailLoadFailedState.value = false
                if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (isCurrentOwnerRequest(requestVersion, generation, callerId)) {
                    _ownEmailProfileState.value = null
                    clearCompletedEmailFeedback()
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

    /** Returns whether trimmed raw [input] differs from the authoritative saved address without parsing. */
    private fun isEmailDraftDirty(input: String, baselineEmail: Email?): Boolean =
        input.trim() != baselineEmail?.string.orEmpty()

    /** Returns the saved candidate the editor uses as its clean draft baseline. */
    private fun AuthFeatureUser.editableEmailBaseline(): Email? = pendingEmail ?: email

    /** Returns the only address eligible for owner verification delivery. */
    private fun AuthFeatureUser.verificationCandidate(): Email? =
        pendingEmail ?: email?.takeIf { !emailApproved }

    /** Returns whether [email] already occupies either persisted owner email slot. */
    private fun AuthFeatureUser.containsPersistedEmail(email: Email): Boolean =
        email == this.email || email == pendingEmail

    /** Returns whether [email] is the approved current address rather than a pending candidate. */
    private fun AuthFeatureUser.isApprovedCurrentEmail(email: Email): Boolean =
        pendingEmail == null && this.email == email && emailApproved

    /** Clears the completed-storage marker and its exact checked snapshot together. */
    private fun clearEmailSavedFeedback() {
        _emailSavedState.value = null
        emailSavedSnapshot = null
    }

    /** Clears the delivery result and its exact checked snapshot together. */
    private fun clearEmailVerificationFeedback() {
        _emailVerificationResultState.value = null
        emailVerificationSnapshot = null
    }

    /** Clears authoritative cooldown feedback and the snapshot that made it current. */
    private fun clearEmailCooldownFeedback() {
        _emailChangeRestrictionState.value = null
        emailCooldownSnapshot = null
    }

    /** Clears claims of completed storage or delivery while retaining a current local or negative failure. */
    private fun clearCompletedEmailFeedback() {
        clearEmailSavedFeedback()
        if (_emailVerificationResultState.value?.isSuccessResult() == true) {
            clearEmailVerificationFeedback()
        }
    }

    /** Clears address-bound feedback before an eligible edit or newly admitted operation. */
    private fun clearEmailOperationFeedback() {
        _emailErrorState.value = null
        clearEmailVerificationFeedback()
        clearEmailSavedFeedback()
        _emailOperationInterruptedState.value = false
    }

    /** Applies a checked private profile while retaining a dirty or explicitly preserved draft. */
    private fun applyOwnedEmailProfile(
        profile: AuthFeatureUser,
        preserveDraft: Boolean,
    ) {
        val snapshot = EmailFeedbackSnapshot(
            email = profile.email,
            emailApproved = profile.emailApproved,
            pendingEmail = profile.pendingEmail,
            emailChangeAllowedAt = profile.emailChangeAllowedAt,
        )
        if (emailSavedSnapshot != snapshot) {
            clearEmailSavedFeedback()
        }
        if (emailVerificationSnapshot != snapshot) {
            clearEmailVerificationFeedback()
        }
        val activeDeadline = profile.emailChangeAllowedAt?.takeIf { nowMillis() < it }
        _emailChangeAllowedState.value = activeDeadline == null
        if (_emailChangeRestrictionState.value != activeDeadline) {
            clearEmailCooldownFeedback()
        }
        if (activeDeadline != null) {
            _emailChangeRestrictionState.value = activeDeadline
            emailCooldownSnapshot = snapshot
        }
        _ownEmailProfileState.value = profile
        if (!preserveDraft && !_emailDraftDirtyState.value) {
            _emailInputState.value = profile.editableEmailBaseline()?.string.orEmpty()
        }
        _emailDraftDirtyState.value = isEmailDraftDirty(_emailInputState.value, profile.editableEmailBaseline())
    }

    /** Publishes a server-supplied cooldown deadline without guessing from a generic failed response. */
    private fun publishEmailCooldown(
        deadline: Long,
        profile: AuthFeatureUser? = _ownEmailProfileState.value,
    ) {
        _emailChangeRestrictionState.value = deadline
        _emailChangeAllowedState.value = nowMillis() >= deadline
        emailCooldownSnapshot = profile?.let {
            EmailFeedbackSnapshot(it.email, it.emailApproved, it.pendingEmail, it.emailChangeAllowedAt)
        }
    }

    /** Returns the checked private profile accepted by an imperative owner-email callback. */
    private fun currentProfileForEmailMutation(requireEnabledSmtp: Boolean): AuthFeatureUser? {
        val session = synchronizeOwnerSession()
        val callerId = session.callerId ?: return null
        if (!isCurrentRawOwner(callerId)) return null
        val capability = _emailCapabilityState.value
        if (!capability.allowsStorage) return null
        if (requireEnabledSmtp && capability != EmailCapabilityState.Enabled) return null
        if (_emailLoadFailedState.value || _emailLoadingState.value || _emailBusyState.value) return null
        val profile = _ownEmailProfileState.value?.takeIf { it.id == userId } ?: return null
        profile.emailChangeAllowedAt?.takeIf { nowMillis() < it }?.let {
            publishEmailCooldown(it, profile)
            return null
        }
        if (!_emailChangeAllowedState.value) {
            _emailChangeAllowedState.value = true
            clearEmailCooldownFeedback()
        }
        return profile
    }

    /** Admits one owner-bound mutation after repeating raw owner, target, capability, and busy checks. */
    private fun beginEmailMutation(requireEnabledSmtp: Boolean): OwnerEmailMutation? {
        val session = synchronizeOwnerSession()
        val callerId = session.callerId ?: return null
        if (!isCurrentRawOwner(callerId)) return null
        val capability = _emailCapabilityState.value
        if (!capability.allowsStorage) return null
        if (requireEnabledSmtp && capability != EmailCapabilityState.Enabled) return null
        if (_ownEmailProfileState.value?.id != userId) return null
        if (_emailLoadFailedState.value || _emailLoadingState.value || _emailBusyState.value) return null
        _ownEmailProfileState.value?.emailChangeAllowedAt?.takeIf { nowMillis() < it }?.let {
            publishEmailCooldown(it)
            return null
        }

        val mutation = OwnerEmailMutation(
            callerId = callerId,
            selectedUserId = userId,
            generation = ownerGeneration,
            mutationId = ++nextEmailMutationId,
            lifecycleStarted = node.state != NavigationNodeState.NEW,
        )
        activeEmailMutation = mutation
        _emailBusyState.value = true
        clearEmailOperationFeedback()
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

    /**
     * Reads and applies one matching private owner record without probing capability again.
     *
     * @param mutation Active owner/target token.
     * @param preserveDraft Keeps the caller's raw draft while recomputing dirtiness against storage.
     * @return Checked current-owner profile, or `null` when loading failed or returned another owner.
     */
    private suspend fun reconcileOwnedEmailProfile(
        mutation: OwnerEmailMutation,
        preserveDraft: Boolean,
    ): AuthFeatureUser? {
        if (!canContinueEmailMutation(mutation)) return null
        try {
            val profile = model.getMyProfile()
            if (!canContinueEmailMutation(mutation)) return null
            if (profile?.id != userId) {
                _ownEmailProfileState.value = null
                clearCompletedEmailFeedback()
                _emailLoadFailedState.value = true
                if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = EmailEditorError.LoadFailed
                }
                return null
            }

            applyOwnedEmailProfile(profile, preserveDraft)
            _emailLoadFailedState.value = false
            if (_emailErrorState.value == EmailEditorError.LoadFailed) {
                _emailErrorState.value = null
            }
            return profile
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            if (isCurrentEmailMutation(mutation)) {
                clearCompletedEmailFeedback()
                _emailLoadFailedState.value = true
                if (_emailErrorState.value == null || _emailErrorState.value == EmailEditorError.LoadFailed) {
                    _emailErrorState.value = EmailEditorError.LoadFailed
                }
            }
            return null
        }
    }

    /** Publishes [result] only with the exact checked [profile] that still owns [recipient]. */
    private fun publishVerificationResult(
        mutation: OwnerEmailMutation,
        recipient: Email,
        profile: AuthFeatureUser,
        result: EmailVerificationRequestResult,
    ) {
        if (profile.verificationCandidate() != recipient && !profile.isApprovedCurrentEmail(recipient)) return
        val snapshot = EmailFeedbackSnapshot(
            email = profile.email,
            emailApproved = profile.emailApproved,
            pendingEmail = profile.pendingEmail,
            emailChangeAllowedAt = profile.emailChangeAllowedAt,
        )
        if (!isCurrentEmailMutation(mutation)) return
        emailVerificationSnapshot = snapshot
        _emailVerificationResultState.value = result
    }

    /** Publishes the completed-storage marker only from an exact checked private [profile]. */
    private fun publishEmailSaved(
        mutation: OwnerEmailMutation,
        profile: AuthFeatureUser,
    ) {
        val email = profile.editableEmailBaseline() ?: return
        if (!isCurrentEmailMutation(mutation)) return
        emailSavedSnapshot = EmailFeedbackSnapshot(
            email = profile.email,
            emailApproved = profile.emailApproved,
            pendingEmail = profile.pendingEmail,
            emailChangeAllowedAt = profile.emailChangeAllowedAt,
        )
        _emailSavedState.value = email
    }

    /** Returns whether a verification result could falsely imply completed delivery or approval. */
    private fun EmailVerificationRequestResult.isSuccessResult(): Boolean = when (this) {
        EmailVerificationRequestResult.Sent,
        EmailVerificationRequestResult.AlreadyApproved -> true
        EmailVerificationRequestResult.Unavailable,
        EmailVerificationRequestResult.NoEmail,
        EmailVerificationRequestResult.EmailChanged,
        EmailVerificationRequestResult.DeliveryFailed -> false
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

    /** Attempts to navigate back using immediate admin and raw-email dirtiness, otherwise pops. */
    fun onBack() {
        if (_profileDirtyState.value || _emailDraftDirtyState.value) {
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

    /**
     * Updates an admitted private owner-email draft, validating malformed nonblank input immediately.
     * A materially changed trimmed draft begins a new edit; redundant draft events retain operation
     * failures except for obsolete validation feedback.
     */
    fun onEmailChanged(email: String) {
        val profile = currentProfileForEmailMutation(requireEnabledSmtp = false) ?: return
        val materiallyChanged = _emailInputState.value.trim() != email.trim()
        _emailInputState.value = email
        _emailDraftDirtyState.value = isEmailDraftDirty(email, profile.editableEmailBaseline())
        if (materiallyChanged) {
            clearEmailOperationFeedback()
        } else if (_emailErrorState.value == EmailEditorError.InvalidEmail) {
            _emailErrorState.value = null
        }
        if (email.isNotBlank() && Email.parse(email).isFailure) {
            _emailErrorState.value = EmailEditorError.InvalidEmail
        } else if (_emailErrorState.value == EmailEditorError.InvalidEmail) {
            _emailErrorState.value = null
        }
    }

    /**
     * Persists a missing or replacement owner email and conditionally requests verification.
     *
     * Enabled SMTP follows acknowledged PUT, checked private GET, optional exact-recipient POST, and
     * final checked private GET. Disabled SMTP follows acknowledged PUT and one checked private GET.
     * Invalid, unchanged, rejected, uncertain, stale-owner, and stale-target work never sends a POST
     * or publishes storage success.
     */
    fun onSaveEmail() {
        val profile = currentProfileForEmailMutation(requireEnabledSmtp = false) ?: return
        val rawDraft = _emailInputState.value
        val email = Email.parse(rawDraft).getOrNull()
        if (email == null) {
            _emailErrorState.value = EmailEditorError.InvalidEmail
            return
        }
        if (profile.containsPersistedEmail(email)) {
            _emailInputState.value = profile.editableEmailBaseline()?.string.orEmpty()
            _emailDraftDirtyState.value = false
            clearEmailOperationFeedback()
            return
        }

        val capability = _emailCapabilityState.value
        val mutation = beginEmailMutation(requireEnabledSmtp = false) ?: return
        launchEmailMutation(mutation) {
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            val saved = try {
                model.setMyEmail(email)
            } catch (error: CancellationException) {
                throw error
            } catch (error: EmailChangeCooldownException) {
                if (isCurrentEmailMutation(mutation)) {
                    publishEmailCooldown(error.cooldown.emailChangeAllowedAt)
                }
                reconcileOwnedEmailProfile(mutation, preserveDraft = true)
                return@launchEmailMutation
            } catch (_: Throwable) {
                if (isCurrentEmailMutation(mutation)) {
                    _emailErrorState.value = EmailEditorError.SaveFailed
                }
                reconcileOwnedEmailProfile(mutation, preserveDraft = true)
                return@launchEmailMutation
            }
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            if (!saved) {
                _emailErrorState.value = EmailEditorError.SaveFailed
                reconcileOwnedEmailProfile(mutation, preserveDraft = true)
                return@launchEmailMutation
            }

            val firstProfile = reconcileOwnedEmailProfile(mutation, preserveDraft = true)
                ?: return@launchEmailMutation
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            if (!firstProfile.containsPersistedEmail(email)) {
                _emailErrorState.value = EmailEditorError.EmailChanged
                return@launchEmailMutation
            }

            val completedProfile = when (capability) {
                EmailCapabilityState.Enabled -> {
                    val result = if (firstProfile.isApprovedCurrentEmail(email)) {
                        null
                    } else {
                        try {
                            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
                            model.requestMyEmailVerification(email)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Throwable) {
                            EmailVerificationRequestResult.DeliveryFailed
                        }
                    }
                    if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
                    val finalProfile = reconcileOwnedEmailProfile(mutation, preserveDraft = true)
                    if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
                    if (finalProfile == null) {
                        if (result != null && !result.isSuccessResult()) {
                            publishVerificationResult(mutation, email, firstProfile, result)
                        }
                        return@launchEmailMutation
                    }
                    if (!finalProfile.containsPersistedEmail(email)) {
                        _emailErrorState.value = EmailEditorError.EmailChanged
                        return@launchEmailMutation
                    }
                    if (result == EmailVerificationRequestResult.AlreadyApproved && !finalProfile.isApprovedCurrentEmail(email)) {
                        _emailErrorState.value = EmailEditorError.EmailChanged
                        return@launchEmailMutation
                    }
                    if (result != null) {
                        publishVerificationResult(mutation, email, finalProfile, result)
                    }
                    finalProfile
                }
                EmailCapabilityState.Disabled -> firstProfile
                EmailCapabilityState.Unknown,
                EmailCapabilityState.Loading,
                EmailCapabilityState.Failed -> return@launchEmailMutation
            }
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            _emailInputState.value = completedProfile.editableEmailBaseline()?.string.orEmpty()
            _emailDraftDirtyState.value = false
            publishEmailSaved(mutation, completedProfile)
        }
    }

    /** Retries enabled-SMTP verification for the saved pending address without rewriting or losing a draft. */
    fun onResendEmailVerification() {
        val profile = currentProfileForEmailMutation(requireEnabledSmtp = true) ?: return
        val email = profile.verificationCandidate() ?: return

        val mutation = beginEmailMutation(requireEnabledSmtp = true) ?: return
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
            val finalProfile = reconcileOwnedEmailProfile(mutation, preserveDraft = true)
            if (!canContinueEmailMutation(mutation)) return@launchEmailMutation
            if (finalProfile == null) {
                if (!result.isSuccessResult()) {
                    publishVerificationResult(mutation, email, profile, result)
                }
                return@launchEmailMutation
            }
            if ((finalProfile.verificationCandidate() != email && !finalProfile.isApprovedCurrentEmail(email)) ||
                (result == EmailVerificationRequestResult.AlreadyApproved && !finalProfile.isApprovedCurrentEmail(email))
            ) {
                _emailErrorState.value = EmailEditorError.EmailChanged
                return@launchEmailMutation
            }
            publishVerificationResult(mutation, email, finalProfile, result)
        }
    }

    /** Refreshes private state after an external change and clears a prior interruption notice. */
    fun onRefreshEmail() {
        val session = synchronizeOwnerSession()
        if (!session.isOwnerOf(userId)) return
        _emailOperationInterruptedState.value = false
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

    /** The server confirmed that SMTP-backed owner verification delivery is available. */
    Enabled,

    /** Verification delivery is unavailable, while owner email storage remains available. */
    Disabled,

    /** Capability or private-profile loading failed; retry remains available but mutations are blocked. */
    Failed,

    ;

    /** `true` when the confirmed server capability permits owner email storage. */
    val allowsStorage: Boolean
        get() = this == Enabled || this == Disabled
}

/**
 * Exact checked private-email state supporting one completed storage or delivery publication.
 *
 * @property email Current owner address, or `null` when absent.
 * @property emailApproved Approval value read with [email] from the same private owner record.
 * @property pendingEmail Replacement candidate awaiting approval, or `null` when absent.
 * @property emailChangeAllowedAt Persisted email-change deadline, or `null` when unrestricted.
 */
private data class EmailFeedbackSnapshot(
    val email: Email?,
    val emailApproved: Boolean,
    val pendingEmail: Email?,
    val emailChangeAllowedAt: Long?,
)

/**
 * Observed caller, authorization, and selected-target snapshot used to invalidate private work.
 *
 * @property callerId Current authenticated caller identifier, or `null` when absent.
 * @property authorised Current raw authorization flag.
 * @property selectedUserId Current live navigation target identifier.
 */
private data class OwnerSession(
    val callerId: UserId?,
    val authorised: Boolean,
    val selectedUserId: UserId,
) {
    /** Returns whether the snapshot authorizes private self-service for [boundUserId]. */
    fun isOwnerOf(boundUserId: UserId): Boolean =
        authorised && callerId == boundUserId && selectedUserId == boundUserId
}

/**
 * One active owner-bound mutation with an explicit cancellation handle.
 *
 * @param callerId Initiating authenticated owner.
 * @param selectedUserId Original editor target captured at admission.
 * @param generation Owner-session generation captured at admission.
 * @param mutationId Monotonic operation token used for deterministic identity.
 * @param lifecycleStarted Whether the node had entered its lifecycle before admission.
 */
private class OwnerEmailMutation(
    val callerId: UserId,
    val selectedUserId: UserId,
    val generation: Long,
    val mutationId: Long,
    val lifecycleStarted: Boolean,
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

    /** Persistence could not be confirmed; reconciliation must not convert uncertainty to success. */
    SaveFailed,

    /** A checked private read did not contain the exact address required by the active operation. */
    EmailChanged,
}
