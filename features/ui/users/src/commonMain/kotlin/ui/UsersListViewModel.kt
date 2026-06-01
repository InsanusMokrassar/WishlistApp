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
 * Loads the full users list and the caller identity on init and on node resume; delegates row
 * selection and the "My profile" action to [interactor]. User deletion no longer lives here — it
 * was moved to the profile edit screen (root only).
 *
 * @param node Navigation node hosting this ViewModel.
 * @param model Users data source.
 * @param interactor Navigation delegate.
 */
class UsersListViewModel(
    private val node: NavigationNode<UsersListViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UsersListViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _usersState = MutableRedeliverStateFlow<List<RegisteredUser>>(emptyList())

    /** Current list of registered users. */
    val usersState = _usersState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)

    /** Authenticated caller id, or `null` when anonymous; gates the "My profile" button. */
    val currentUserIdState = _currentUserIdState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUsers()
        }
    }

    private suspend fun loadUsers() {
        _loadingState.value = true
        try {
            _currentUserIdState.value = model.getCurrentUserId()
            _usersState.value = model.getAllUsers()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Opens the authenticated caller's own profile ("My profile").
     * No-op when anonymous (no current user id resolved yet).
     */
    fun onMyProfile() {
        val myId = _currentUserIdState.value ?: return
        scope.launchLoggingDropExceptions { interactor.onOpenProfile(node, myId) }
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
