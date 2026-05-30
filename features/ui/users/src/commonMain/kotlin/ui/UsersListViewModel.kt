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
     * Forwards the selection event to [UsersListViewInteractor.onUserSelected].
     *
     * @param userId Identifier of the selected row.
     */
    fun onUserSelected(userId: UserId) {
        scope.launchLoggingDropExceptions { interactor.onUserSelected(node, userId) }
    }
}
