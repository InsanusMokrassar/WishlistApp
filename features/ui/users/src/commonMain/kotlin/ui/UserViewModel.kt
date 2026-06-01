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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

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

    private val _avatarIdState = MutableRedeliverStateFlow<FileId?>(null)

    /** Avatar file id of the loaded user, or `null` when no avatar is set. */
    val avatarIdState = _avatarIdState.asStateFlow()

    private val _canEditState = MutableRedeliverStateFlow(false)

    /** `true` when the caller may edit this profile (the owner or `root`); gates the Edit button. */
    val canEditState = _canEditState.asStateFlow()

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
            val currentUserId = model.getCurrentUserId()
            _canEditState.value = model.isCurrentUserRoot() || currentUserId == node.config.userId
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
