package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.admin.common.models.NewUserWithPassword
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.utils.subscribeOnLoggedOut
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the admin user create/edit screen.
 *
 * When [AdminUserEditViewConfig.userId] is `null`, operates in create mode (requires password).
 * When non-null, loads the existing user and pre-fills fields. Password field only shown in create mode.
 *
 * On logout this screen exits unconditionally via [AdminUserEditViewInteractor.onNavigateBack],
 * bypassing the dirty-changes confirm dialog.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 * @param authCredentialsStorage Login-state source; on logout this screen exits to its non-edit view.
 */
class AdminUserEditViewModel(
    private val node: NavigationNode<AdminUserEditViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminUserEditViewInteractor,
    private val authCredentialsStorage: AuthCredentialsStorage
) : ViewModel<ViewConfig>(node) {
    /** `true` when operating in create mode (no existing user id). */
    val isCreating: Boolean = node.config.userId == null

    private val _usernameState = MutableRedeliverStateFlow("")

    /** Current value of the username input field. */
    val usernameState = _usernameState.asStateFlow()

    private val _passwordState = MutableRedeliverStateFlow("")

    /** Current value of the password input field. Only used in create mode. */
    val passwordState = _passwordState.asStateFlow()

    private val _isDirtyState = MutableRedeliverStateFlow(false)

    /** `true` when any field has been modified since the screen was opened. */
    val isDirtyState = _isDirtyState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { !inited }.subscribeLoggingDropExceptions(scope) {
            node.config.userId?.let { id ->
                _loadingState.value = true
                try {
                    val user = model.getUserById(id)
                    if (user != null) {
                        _usernameState.value = user.username.string
                    }
                } finally {
                    _loadingState.value = false
                }
            }
            inited = true
        }
        authCredentialsStorage.userAuthorised.subscribeOnLoggedOut(scope) {
            interactor.onNavigateBack(node)
        }
    }

    /**
     * Updates the username field and marks the form as dirty.
     *
     * @param username New username value.
     */
    fun onUsernameChanged(username: String) {
        _usernameState.value = username
        _isDirtyState.value = true
    }

    /**
     * Updates the password field and marks the form as dirty. Only used in create mode.
     *
     * @param password New password value.
     */
    fun onPasswordChanged(password: String) {
        _passwordState.value = password
        _isDirtyState.value = true
    }

    /**
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`,
     * otherwise delegates to [AdminUserEditViewInteractor.onNavigateBack].
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and delegates to [AdminUserEditViewInteractor.onNavigateBack]. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the confirm dialog, returning the admin to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves the user (create or update) and delegates to [AdminUserEditViewInteractor.onSaved] on success.
     * No-op when [usernameState] is blank, or password is blank in create mode, or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val username = _usernameState.value.trim()
            if (username.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                val id = node.config.userId
                if (id == null) {
                    val password = _passwordState.value.trim()
                    if (password.isBlank()) return@launchLoggingDropExceptions
                    model.createUser(NewUserWithPassword(Username(username), Password(password)))
                } else {
                    model.updateUser(id, NewUser(Username(username)))
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
