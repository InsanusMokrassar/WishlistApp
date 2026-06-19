package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.auth.common.models.Password
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
    private val _usernameState = MutableRedeliverStateFlow("")
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")
    val passwordState = _passwordState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)
    val loadingState = _loadingState.asStateFlow()

    private val _errorState = MutableRedeliverStateFlow(false)
    val errorState = _errorState.asStateFlow()

    private val _formExpandedState = MutableRedeliverStateFlow(false)

    /** `true` when the login/register form is currently expanded. */
    val formExpandedState = _formExpandedState.asStateFlow()

    private val _registerModeState = MutableRedeliverStateFlow(false)

    /** `true` when the expanded form is in registration mode rather than login mode. */
    val registerModeState = _registerModeState.asStateFlow()

    private val _registrationEnabledState = MutableRedeliverStateFlow(false)

    /** `true` when the server permits self-service account registration. */
    val registrationEnabledState = _registrationEnabledState.asStateFlow()

    /** Mirrors [AuthModel.userAuthorisedState]. */
    val loggedInState: StateFlow<Boolean> = model.userAuthorisedState

    /** `true` when the submit button is enabled (fields non-blank, no request in flight). */
    val loginEnabledState: StateFlow<Boolean> = combine(
        _usernameState,
        _passwordState,
        _loadingState
    ) { username, password, loading ->
        !loading && username.isNotBlank() && password.isNotBlank()
    }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launchLoggingDropExceptions {
            if (model.isAlreadyLoggedIn()) {
                interactor.onUserLoggedIn(node)
            }
        }
        scope.launchLoggingDropExceptions {
            _registrationEnabledState.value = model.isRegistrationEnabled()
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

    /** Expands the form in login mode. */
    fun onToggleForm() {
        _registerModeState.value = false
        _formExpandedState.value = !_formExpandedState.value
        if (!_formExpandedState.value) {
            _errorState.value = false
        }
    }

    /** Expands the form in registration mode. */
    fun onToggleRegisterForm() {
        _registerModeState.value = true
        _formExpandedState.value = true
        _errorState.value = false
    }

    /**
     * Switches the (open) form to login mode without collapsing it. Used by the tabbed
     * login/register modal so the "Log in" tab is idempotent when already expanded.
     */
    fun onShowLoginForm() {
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
            if (username.isBlank() || password.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            _errorState.value = false
            try {
                val success = model.register(Username(username), Password(password))
                if (success) {
                    _usernameState.value = ""
                    _passwordState.value = ""
                    _formExpandedState.value = false
                    _registerModeState.value = false
                    interactor.onUserLoggedIn(node)
                } else {
                    _errorState.value = true
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
}
