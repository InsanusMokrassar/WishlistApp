package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the wishlist create/edit screen.
 *
 * When [WishlistEditViewConfig.wishlistId] is null, operates in create mode.
 * When non-null, loads the existing wishlist and pre-fills the title field.
 *
 * Back navigation shows a discard-changes confirmation modal when [isDirtyState] is `true`.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 */
class WishlistEditViewModel(
    private val node: NavigationNode<WishlistEditViewConfig, ViewConfig>,
    private val model: WishlistsModel
) : ViewModel<ViewConfig>(node) {
    /** `true` when this screen is in create mode (no existing wishlist id). */
    val isCreating: Boolean = node.config.wishlistId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

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
        scope.launchLoggingDropExceptions {
            node.config.wishlistId?.let { id ->
                _loadingState.value = true
                try {
                    val wishlist = model.getWishlist(id)
                    if (wishlist != null) {
                        _titleState.value = wishlist.title
                    }
                } finally {
                    _loadingState.value = false
                }
            }
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
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`,
     * otherwise pops the current node immediately.
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { node.chain.pop() }
        }
    }

    /** Confirms discarding changes and pops the current node. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { node.chain.pop() }
    }

    /** Cancels the confirm dialog, returning the user to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves the wishlist (create or update) and pops the current node on success.
     * No-op when [titleState] is blank or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            if (title.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                val id = node.config.wishlistId
                if (id == null) {
                    model.createWishlist(title)
                } else {
                    model.updateWishlist(id, title)
                }
                node.chain.pop()
            } finally {
                _loadingState.value = false
            }
        }
    }
}
