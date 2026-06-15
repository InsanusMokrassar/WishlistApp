package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the public user profile detail screen.
 *
 * Loads the user, their avatar and the caller's edit permission on init and on each resume.
 * Auto-navigates back when the displayed user no longer exists (e.g. deleted from the edit screen).
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Users data source.
 * @param interactor Navigation delegate for this screen.
 */
class UserViewModel(
    private val node: NavigationNode<UserViewConfig, ViewConfig>,
    private val model: UsersModel,
    private val interactor: UserViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _userState = MutableRedeliverStateFlow<RegisteredUser?>(null)

    /** The loaded user, `null` while loading or after the user was removed. */
    val userState = _userState.asStateFlow()

    /**
     * Label for the contextual Back button: this user's display name. Back replaces this screen with
     * the user's all-items screen, so the button names the destination. `null` until the user
     * resolves — the view then falls back to the generic back string. Derived from the already-loaded
     * [userState], so it adds no extra round-trip.
     */
    val backLabelState: StateFlow<String?> =
        _userState.map { it?.username?.string }.stateIn(scope, SharingStarted.Eagerly, null)

    private val _avatarIdState = MutableRedeliverStateFlow<FileId?>(null)

    /** Avatar file id of the loaded user, or `null` when no avatar is set. */
    val avatarIdState = _avatarIdState.asStateFlow()

    /**
     * `true` when the caller may edit this profile (the owner or `root`); gates the Edit button.
     * Derived reactively from [UsersModel.isCurrentUserRootFlow] and [UsersModel.currentUserIdFlow],
     * so it self-corrects once the cold-start `getMe()` round-trip completes and on later
     * login/logout (PR #31 F2).
     */
    val canEditState: StateFlow<Boolean> =
        combine(model.isCurrentUserRootFlow, model.currentUserIdFlow) { isRoot, currentUserId ->
            isRoot || currentUserId == node.config.userId
        }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadUser()
        }
    }

    private suspend fun loadUser() {
        _loadingState.value = true
        val user = try {
            val loaded = model.getUser(node.config.userId)
            _userState.value = loaded
            _avatarIdState.value = model.getAvatar(node.config.userId)
            loaded
        } finally {
            _loadingState.value = false
        }
        // User may have been deleted (here or elsewhere) — leave the screen when it no longer exists.
        if (user == null) {
            interactor.onBack(node)
        }
    }

    /** Delegates to [UserViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [UserViewInteractor.onEditUser]. Shown only when [canEditState] is `true`. */
    fun onEditUser() {
        scope.launchLoggingDropExceptions { interactor.onEditUser(node) }
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
