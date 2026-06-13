package dev.inmo.wishlist.features.ui.adminPanel.ui

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
 * ViewModel for the admin users list screen.
 *
 * Loads all users on init and on each resume. Delegates navigation side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminUsersListViewModel(
    private val node: NavigationNode<AdminUsersListViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminUsersListViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _usersState = MutableRedeliverStateFlow<List<RegisteredUser>>(emptyList())

    /** Current list of all registered users. */
    val usersState = _usersState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUsers()
        }
    }

    private suspend fun loadUsers() {
        _loadingState.value = true
        try {
            _usersState.value = model.getAllUsers()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Delegates to [AdminUsersListViewInteractor.onUserSelected].
     *
     * @param userId Identifier of the user the admin tapped.
     */
    fun onUserSelected(userId: UserId) {
        scope.launchLoggingDropExceptions { interactor.onUserSelected(node, userId) }
    }

    /** Delegates to [AdminUsersListViewInteractor.onCreateUser]. */
    fun onCreateUser() {
        scope.launchLoggingDropExceptions { interactor.onCreateUser(node) }
    }
}
