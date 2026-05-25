package dev.inmo.wishlist.features.ui.auth.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.utils.defaultServerUrl
import dev.inmo.wishlist.features.users.common.models.Username

class AuthViewModel(
    private val node: NavigationNode<AuthViewConfig, ViewConfig>,
    private val model: AuthModel,
    private val interactor: AuthViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _usernameState = MutableRedeliverStateFlow("")
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")
    val passwordState = _passwordState.asStateFlow()

    private val _addressState = MutableRedeliverStateFlow("")
    val addressState = _addressState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)
    val loadingState = _loadingState.asStateFlow()

    private val _errorState = MutableRedeliverStateFlow<Boolean>(false)
    val errorState = _errorState.asStateFlow()

    val loginEnabledState: StateFlow<Boolean> = combine(
        _usernameState,
        _passwordState,
        _addressState,
        _loadingState
    ) { username, password, address, loading ->
        !loading && checkFields(username, password, address)
    }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launchLoggingDropExceptions {
            val saved = model.getServerAddress().orEmpty()
            _addressState.value = saved.ifBlank { defaultServerUrl() }
            if (model.isAlreadyLoggedIn()) {
                interactor.onUserLoggedIn(node)
            } else {
                println("User logged out")
            }
        }
    }

    fun checkFields(
        username: String = _usernameState.value,
        password: String = _passwordState.value,
        address: String = _addressState.value
    ): Boolean = username.isNotBlank() && password.isNotBlank() && address.isNotBlank()

    fun onUsernameChanged(input: String) {
        _usernameState.value = input
        _errorState.value = false
    }

    fun onPasswordChanged(input: String) {
        _passwordState.value = input
        _errorState.value = false
    }

    fun onAddressChanged(input: String) {
        _addressState.value = input
        _errorState.value = false
    }

    fun onAuthorize() {
        scope.launchLoggingDropExceptions {
            if (!checkFields()) return@launchLoggingDropExceptions
            _loadingState.value = true
            _errorState.value = false
            try {
                model.saveServerAddress(_addressState.value.trim())
                val success = model.login(
                    Username(_usernameState.value.trim()),
                    Password(_passwordState.value)
                )
                if (success) {
                    interactor.onUserLoggedIn(node)
                } else {
                    _errorState.value = true
                }
            } finally {
                _loadingState.value = false
            }
        }
    }
}
