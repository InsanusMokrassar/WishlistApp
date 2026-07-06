package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.utils.subscribeOnLoggedOut
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the admin wishlist create/edit screen.
 *
 * When [AdminWishlistEditViewConfig.wishlistId] is `null`, operates in create mode.
 * When non-null, loads the existing wishlist and pre-fills fields.
 * Loads all users to populate the owner dropdown.
 * If [AdminWishlistEditViewConfig.preselectedUserId] is non-null and the wishlistId is null,
 * the owner dropdown is pre-selected to that user.
 *
 * On logout this screen exits unconditionally via [AdminWishlistEditViewInteractor.onNavigateBack],
 * bypassing the dirty-changes confirm dialog.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminWishlistEditViewModel(
    private val node: NavigationNode<AdminWishlistEditViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminWishlistEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** `true` when operating in create mode. */
    val isCreating: Boolean = node.config.wishlistId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

    private val _usersState = MutableRedeliverStateFlow<List<RegisteredUser>>(emptyList())

    /** All users available for selection as owner. */
    val usersState = _usersState.asStateFlow()

    private val _selectedUserIdState = MutableRedeliverStateFlow<UserId?>(node.config.preselectedUserId)

    /** Currently selected owner user id, or `null` when none selected. */
    val selectedUserIdState = _selectedUserIdState.asStateFlow()

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
            _loadingState.value = true
            try {
                _usersState.value = model.getAllUsers()
                node.config.wishlistId?.let { id ->
                    val wishlist = model.getWishlistById(id)
                    if (wishlist != null) {
                        _titleState.value = wishlist.title
                        _selectedUserIdState.value = wishlist.userId
                    }
                }
            } finally {
                _loadingState.value = false
            }
            inited = true
        }
        model.userAuthorisedState.subscribeOnLoggedOut(scope) {
            interactor.onNavigateBack(node)
        }
    }

    /**
     * Updates the title field and marks the form as dirty.
     *
     * @param title New title value.
     */
    fun onTitleChanged(title: String) {
        _titleState.value = title
        _isDirtyState.value = true
    }

    /**
     * Updates the selected owner user id and marks the form as dirty.
     *
     * @param userId Selected user's identifier.
     */
    fun onOwnerSelected(userId: UserId?) {
        _selectedUserIdState.value = userId
        _isDirtyState.value = true
    }

    /**
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`,
     * otherwise delegates to [AdminWishlistEditViewInteractor.onNavigateBack].
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and delegates to [AdminWishlistEditViewInteractor.onNavigateBack]. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the confirm dialog, returning the admin to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves the wishlist (create or update) and delegates to [AdminWishlistEditViewInteractor.onSaved] on success.
     * No-op when [titleState] is blank, no owner is selected, or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            val ownerId = _selectedUserIdState.value
            if (title.isBlank() || ownerId == null) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                val id = node.config.wishlistId
                if (id == null) {
                    model.createWishlist(NewWishlist(ownerId, title))
                } else {
                    model.updateWishlist(id, ownerId, title)
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
