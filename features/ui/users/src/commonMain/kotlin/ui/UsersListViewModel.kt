package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.UsersFeatureUser
import kotlinx.coroutines.flow.StateFlow
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
    private val _usersState = MutableRedeliverStateFlow<List<UsersFeatureUser>>(emptyList())

    /** Current list of registered users. */
    val usersState = _usersState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    /**
     * Authenticated caller id, or `null` when anonymous; gates the "My profile" button. Sourced
     * reactively from [UsersModel.currentUserIdFlow], so it self-corrects once the cold-start
     * `getMe()` round-trip completes and on later login/logout (PR #31 F2).
     */
    val currentUserIdState: StateFlow<UserId?> = model.currentUserIdFlow

    private val _avatarsState = MutableRedeliverStateFlow<Map<UserId, FileId>>(emptyMap())

    /** Avatar file id per user id; absent entries mean the user has no avatar set. */
    val avatarsState = _avatarsState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUsers()
        }
    }

    private suspend fun loadUsers() {
        _loadingState.value = true
        try {
            val users = model.getAllUsers()
            _usersState.value = users
            _avatarsState.value = users
                .mapNotNull { user -> model.getAvatar(user.id)?.let { user.id to it } }
                .toMap()
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Opens the authenticated caller's own profile ("My profile").
     * No-op when anonymous (no current user id resolved yet).
     */
    fun onMyProfile() {
        val myId = currentUserIdState.value ?: return
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
