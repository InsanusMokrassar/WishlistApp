package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.auth.common.models.RegistrationResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the inline auth widget embedded in the top bar.
 *
 * Visible modes driven by [loggedInState], [formExpandedState], and [registerModeState]:
 * - logged-in: shows a "Log out" button
 * - logged-out + collapsed: shows "Log in" (and "Register" when [registrationEnabledState] is true)
 * - logged-out + expanded + login mode: shows username/password form with "Submit"
 * - logged-out + expanded + register mode: shows username/password form with "Register"
 *
 * @param node Navigation node hosting this ViewModel.
 * @param model Auth backend facade.
 * @param interactor Navigation/app-level delegate.
 */
class AuthViewModel(
    private val node: NavigationNode<AuthViewConfig, ViewConfig>,
    private val model: AuthModel,
    private val interactor: AuthViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** Input values used to derive submit availability. */
    private data class SubmitInput(
        val username: String,
        val password: String,
        val email: String,
        val loading: Boolean,
    )

    /** Registration mode and server email-policy flags used to derive submit availability. */
    private data class RegistrationPolicy(
        val registerMode: Boolean,
        val requireEmail: Boolean,
    )

    private val _usernameState = MutableRedeliverStateFlow("")
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")
    val passwordState = _passwordState.asStateFlow()

    private val _emailState = MutableRedeliverStateFlow("")
    val emailState = _emailState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)
    val loadingState = _loadingState.asStateFlow()

    private val _errorState = MutableRedeliverStateFlow(false)
    val errorState = _errorState.asStateFlow()

    private val _formExpandedState = MutableRedeliverStateFlow(false)

    /** `true` when the login/register form is currently expanded. */
    val formExpandedState = _formExpandedState.asStateFlow()

    private val _pendingEmailVerificationState = MutableRedeliverStateFlow(false)

    /** `true` while the separate check-email confirmation is visible. */
    val pendingEmailVerificationState = _pendingEmailVerificationState.asStateFlow()

    private val _registerModeState = MutableRedeliverStateFlow(false)

    /** `true` when the expanded form is in registration mode rather than login mode. */
    val registerModeState = _registerModeState.asStateFlow()

    private val _registrationEnabledState = MutableRedeliverStateFlow(false)

    /** `true` when the server permits self-service account registration. */
    val registrationEnabledState = _registrationEnabledState.asStateFlow()

    private val _requireEmailForRegistrationState = MutableRedeliverStateFlow(false)

    /** `true` when registration requires a validated email and a verification invite. */
    val requireEmailForRegistrationState = _requireEmailForRegistrationState.asStateFlow()

    /** Mirrors [AuthModel.userAuthorisedState]. */
    val loggedInState: StateFlow<Boolean> = model.userAuthorisedState

    /** `true` when the submit button is enabled (fields valid, no request in flight). */
    val loginEnabledState: StateFlow<Boolean> = combine(
        combine(_usernameState, _passwordState, _emailState, _loadingState) {
            username, password, email, loading -> SubmitInput(username, password, email, loading)
        },
        combine(_registerModeState, _requireEmailForRegistrationState) { registerMode, requireEmail ->
            RegistrationPolicy(registerMode, requireEmail)
        },
    ) { input, policy ->
        when {
            input.loading -> false
            input.username.isBlank() || input.password.isBlank() -> false
            !policy.registerMode -> true
            else -> isRegistrationEmailValid(input.email, policy.requireEmail)
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launchLoggingDropExceptions {
            if (model.isAlreadyLoggedIn()) {
                interactor.onUserLoggedIn(node)
            }
        }
        scope.launchLoggingDropExceptions {
            val config = model.getConfig()
            _registrationEnabledState.value = config.enableRegistration
            _requireEmailForRegistrationState.value = config.requireEmailForRegistration
        }
    }

    /** Handles username input edits and clears any previous error. */
    fun onUsernameChanged(input: String) {
        _usernameState.value = input
        _errorState.value = false
    }

    /** Handles password input edits and clears any previous error. */
    fun onPasswordChanged(input: String) {
        _passwordState.value = input
        _errorState.value = false
    }

    /** Handles email input edits and clears any previous error. */
    fun onEmailChanged(input: String) {
        _emailState.value = input
        _errorState.value = false
    }

    /** Expands the form in login mode. */
    fun onToggleForm() {
        _pendingEmailVerificationState.value = false
        _registerModeState.value = false
        _formExpandedState.value = !_formExpandedState.value
        if (!_formExpandedState.value) {
            _errorState.value = false
        }
    }

    /** Expands the form in registration mode. */
    fun onToggleRegisterForm() {
        _pendingEmailVerificationState.value = false
        _registerModeState.value = true
        _formExpandedState.value = true
        _errorState.value = false
    }

    /**
     * Switches the (open) form to login mode without collapsing it. Used by the tabbed
     * login/register modal so the "Log in" tab is idempotent when already expanded.
     */
    fun onShowLoginForm() {
        _pendingEmailVerificationState.value = false
        _registerModeState.value = false
        _formExpandedState.value = true
        _errorState.value = false
    }

    /** Collapses the form and resets error state. */
    fun onCancelForm() {
        _formExpandedState.value = false
        _registerModeState.value = false
        _errorState.value = false
    }

    /** Dismisses the check-email confirmation without changing credentials or form inputs. */
    fun onDismissPendingEmailVerification() {
        _pendingEmailVerificationState.value = false
    }

    /** Submits the entered credentials as a login request. */
    fun onAuthorize() {
        scope.launchLoggingDropExceptions {
            val username = _usernameState.value.trim()
            val password = _passwordState.value
            if (username.isBlank() || password.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            _errorState.value = false
            try {
                val success = model.login(Username(username), Password(password))
                if (success) {
                    _usernameState.value = ""
                    _passwordState.value = ""
                    _formExpandedState.value = false
                    interactor.onUserLoggedIn(node)
                } else {
                    _errorState.value = true
                }
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Submits the entered credentials as a registration request. */
    fun onRegister() {
        scope.launchLoggingDropExceptions {
            val username = _usernameState.value.trim()
            val password = _passwordState.value
            val emailText = _emailState.value
            if (
                username.isBlank() || password.isBlank() ||
                !isRegistrationEmailValid(emailText, _requireEmailForRegistrationState.value)
            ) return@launchLoggingDropExceptions
            val email = Email.parse(emailText).getOrNull()
            _loadingState.value = true
            _errorState.value = false
            try {
                when (val result = model.register(Username(username), Password(password), email)) {
                    is RegistrationResult.Authorized -> {
                        clearRegistrationForm()
                        _pendingEmailVerificationState.value = false
                        interactor.onUserLoggedIn(node)
                    }
                    RegistrationResult.PendingEmailVerification -> {
                        clearRegistrationForm()
                        _errorState.value = false
                        _pendingEmailVerificationState.value = true
                    }
                    null -> _errorState.value = true
                }
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Invalidates the current session via [AuthModel.logout]. */
    fun onLogout() {
        scope.launchLoggingDropExceptions {
            _loadingState.value = true
            try {
                model.logout()
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Clears inputs and closes registration mode after either successful registration outcome. */
    private fun clearRegistrationForm() {
        _usernameState.value = ""
        _passwordState.value = ""
        _emailState.value = ""
        _formExpandedState.value = false
        _registerModeState.value = false
    }
}

/**
 * Validates the registration email field according to the server policy.
 *
 * @param email Raw field value.
 * @param required Whether an empty value is forbidden.
 * @return `true` for a blank optional value or a syntactically valid non-blank address.
 */
internal fun isRegistrationEmailValid(email: String, required: Boolean): Boolean = when {
    email.isBlank() -> !required
    else -> Email.parse(email).isSuccess
}
