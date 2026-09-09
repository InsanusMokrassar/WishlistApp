package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.common.utils.isAcceptablePasswordChangePassword
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * UI state and submission policy for one email-authorized password-change approval.
 *
 * Password fields live only in memory; completion submits the immutable route subject and approval
 * UUID verbatim and never observes or reacts to current login state.
 *
 * @param node Current pending or completed navigation node.
 * @param model Users feature facade carrying the Auth client transport.
 * @param interactor Navigation delegate implemented by the top-level client module.
 * @param dispatcher UI dispatcher used for form state and completion continuations.
 */
class PasswordChangeViewModel(
    private val node: NavigationNode<PasswordChangeViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: PasswordChangeViewInteractor,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel<ViewConfig>(node) {
    /** UI-confined child scope retaining this ViewModel's cancellation lifecycle. */
    private val workScope = CoroutineScope(scope.coroutineContext + dispatcher)

    /** Immutable navigation configuration for this ViewModel lifetime. */
    val config: PasswordChangeViewConfig = node.config

    private val _passwordState = MutableRedeliverStateFlow("")

    /** New plaintext password held only until submission/navigation destruction. */
    val passwordState: StateFlow<String> = _passwordState.asStateFlow()

    private val _confirmationState = MutableRedeliverStateFlow("")

    /** Matching confirmation held only in ViewModel memory. */
    val confirmationState: StateFlow<String> = _confirmationState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while one completion request is awaiting a response. */
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()

    private val _resultState = MutableRedeliverStateFlow<PasswordChangeSubmissionState?>(null)

    /** Most recent local or server-facing completion result. */
    val resultState: StateFlow<PasswordChangeSubmissionState?> = _resultState.asStateFlow()

    private val _terminalInvalidApprovalState = MutableRedeliverStateFlow(false)

    /** `true` once an approval is rejected, preventing unsafe replay from the same screen. */
    val terminalInvalidApprovalState: StateFlow<Boolean> = _terminalInvalidApprovalState.asStateFlow()

    /** Whether this node already represents a credential-free completed state. */
    val completedState: Boolean = config is PasswordChangeViewConfig.Completed

    /** Shows a mismatch only after either field has user input. */
    val passwordsMismatchState: StateFlow<Boolean> =
        combine(_passwordState, _confirmationState) { password, confirmation ->
            password != confirmation && (password.isNotEmpty() || confirmation.isNotEmpty())
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    /** Shows local policy guidance as soon as a non-empty entered password violates the policy. */
    val passwordInvalidState: StateFlow<Boolean> =
        _passwordState
            .combine(_confirmationState) { password, _ ->
                password.isNotEmpty() && !isAcceptablePasswordChangePassword(Password(password))
            }
            .stateIn(workScope, SharingStarted.Eagerly, false)

    /** Allows one submission only for a valid pending config and matching locally acceptable input. */
    val canSubmitState: StateFlow<Boolean> =
        combine(
            _passwordState,
            _confirmationState,
            _loadingState,
            _terminalInvalidApprovalState,
        ) { password, confirmation, loading, terminalInvalidApproval ->
            config is PasswordChangeViewConfig.Pending &&
                !loading &&
                !terminalInvalidApproval &&
                password == confirmation &&
                isAcceptablePasswordChangePassword(Password(password))
        }.stateIn(workScope, SharingStarted.Eagerly, false)

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion {
            _passwordState.value = ""
            _confirmationState.value = ""
        }
    }

    /** Replaces the in-memory new password and clears stale retryable feedback. */
    fun onPasswordChanged(password: String) {
        if (completedState || _terminalInvalidApprovalState.value) return
        _passwordState.value = password
        if (_resultState.value != PasswordChangeSubmissionState.InvalidApproval) {
            _resultState.value = null
        }
    }

    /** Replaces the in-memory confirmation and clears stale retryable feedback. */
    fun onConfirmationChanged(confirmation: String) {
        if (completedState || _terminalInvalidApprovalState.value) return
        _confirmationState.value = confirmation
        if (_resultState.value != PasswordChangeSubmissionState.InvalidApproval) {
            _resultState.value = null
        }
    }

    /**
     * Submits the immutable pending UUID and subject once matching input passes local policy.
     *
     * The server remains authoritative; local rejection avoids consuming a valid approval for an
     * obviously invalid password.
     */
    fun onSubmitPasswordChange() {
        val pending = config as? PasswordChangeViewConfig.Pending ?: return
        val password = _passwordState.value
        val confirmation = _confirmationState.value
        when {
            _loadingState.value || _terminalInvalidApprovalState.value -> return
            password != confirmation -> {
                _resultState.value = PasswordChangeSubmissionState.Mismatch
                return
            }
            !isAcceptablePasswordChangePassword(Password(password)) -> {
                _resultState.value = PasswordChangeSubmissionState.InvalidPassword
                return
            }
        }
        val request = CompletePasswordChangeRequest(pending.userId, pending.approvalId, Password(password))
        _loadingState.value = true
        _resultState.value = null
        workScope.launchLoggingDropExceptions {
            try {
                when (model.completePasswordChange(request)) {
                    PasswordChangeResult.Changed -> {
                        _passwordState.value = ""
                        _confirmationState.value = ""
                        interactor.onChanged(node)
                    }
                    PasswordChangeResult.InvalidApproval -> {
                        _terminalInvalidApprovalState.value = true
                        _resultState.value = PasswordChangeSubmissionState.InvalidApproval
                    }
                    PasswordChangeResult.InvalidPassword -> {
                        _resultState.value = PasswordChangeSubmissionState.InvalidPassword
                    }
                    null -> {
                        _resultState.value = PasswordChangeSubmissionState.Unconfirmed
                    }
                }
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Leaves this screen through the safe credential-free navigation path. */
    fun onContinue() {
        workScope.launchLoggingDropExceptions {
            _passwordState.value = ""
            _confirmationState.value = ""
            interactor.onContinue(node)
        }
    }
}

/** Presentation outcomes for a password-change submission. */
enum class PasswordChangeSubmissionState {
    /** Entered password and confirmation differ. */
    Mismatch,

    /** Password violates the bounded client policy or server repeats that rejection. */
    InvalidPassword,

    /** Persisted approval cannot be used again. */
    InvalidApproval,

    /** HTTP or transport failure makes the outcome unsafe to retry automatically. */
    Unconfirmed,
}
