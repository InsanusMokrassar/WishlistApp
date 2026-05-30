package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for the users list screen (main page content slot).
 *
 * Loads the full users list on init and on node resume; delegates row selection
 * to [interactor].
 *
 * @param node Navigation node hosting this ViewModel.
 * @param model Users data source.
 * @param interactor Navigation delegate.
 */
class UsersListViewModel(
    private val node: NavigationNode<UsersListViewConfig, ViewConfig>,
    private val model: UsersListModel,
    private val interactor: UsersListViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _usersState = MutableRedeliverStateFlow<List<RegisteredUser>>(emptyList())

    /** Current list of registered users. */
    val usersState = _usersState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _isRootState = MutableRedeliverStateFlow(false)

    /** `true` when the caller is the `root` user; gates visibility of per-row delete controls. */
    val isRootState = _isRootState.asStateFlow()

    private val _deleteTargetState = MutableRedeliverStateFlow<RegisteredUser?>(null)

    /** User currently targeted for deletion, or `null` when no delete flow is active. */
    val deleteTargetState = _deleteTargetState.asStateFlow()

    private val _deleteStepState = MutableRedeliverStateFlow(0)

    /**
     * Stage of the two-step delete confirmation:
     * `0` = no dialog, `1` = first confirmation, `2` = second (final) confirmation.
     */
    val deleteStepState = _deleteStepState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUsers()
        }
    }

    private suspend fun loadUsers() {
        _loadingState.value = true
        try {
            _isRootState.value = model.isCurrentUserRoot()
            _usersState.value = model.getAllUsers()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Starts the delete flow for [user]: opens the first confirmation dialog.
     * No-op unless the caller is `root`.
     *
     * @param user Row the operator asked to delete.
     */
    fun onDeleteUserRequest(user: RegisteredUser) {
        if (!_isRootState.value) return
        _deleteTargetState.value = user
        _deleteStepState.value = 1
    }

    /** Advances from the first to the second (final) confirmation dialog. */
    fun onConfirmDeleteFirst() {
        if (_deleteStepState.value == 1) {
            _deleteStepState.value = 2
        }
    }

    /**
     * Final confirmation: deletes the targeted user (with all related data, server-side cascade),
     * closes the dialog and reloads the list.
     */
    fun onConfirmDeleteSecond() {
        val target = _deleteTargetState.value ?: return
        scope.launchLoggingDropExceptions {
            _deleteStepState.value = 0
            _deleteTargetState.value = null
            _loadingState.value = true
            try {
                model.deleteUser(target.id)
            } finally {
                _loadingState.value = false
            }
            loadUsers()
        }
    }

    /** Cancels the delete flow at any stage. */
    fun onCancelDelete() {
        _deleteStepState.value = 0
        _deleteTargetState.value = null
    }

    /**
     * Forwards the selection event to [UsersListViewInteractor.onUserSelected].
     *
     * @param userId Identifier of the selected row.
     */
    fun onUserSelected(userId: UserId) {
        scope.launchLoggingDropExceptions { interactor.onUserSelected(node, userId) }
    }
}
