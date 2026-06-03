package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the wishlist create/edit screen.
 *
 * When [WishlistEditViewConfig.wishlistId] is null, operates in create mode.
 * When non-null, loads the existing wishlist and pre-fills the title field.
 *
 * Back navigation shows a discard-changes confirmation modal when [isDirtyState] is `true`.
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistEditViewModel(
    private val node: NavigationNode<WishlistEditViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** `true` when this screen is in create mode (no existing wishlist id). */
    val isCreating: Boolean = node.config.wishlistId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

    private val _defaultPriceUnitsState = MutableRedeliverStateFlow("")

    /** Current default currency/units pre-filled into new items of this wishlist. */
    val defaultPriceUnitsState = _defaultPriceUnitsState.asStateFlow()

    private val _isDirtyState = MutableRedeliverStateFlow(false)

    /** `true` when any field has been modified since the screen was opened. */
    val isDirtyState = _isDirtyState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _showConfirmDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the discard-changes confirmation dialog should be visible. */
    val showConfirmDialogState = _showConfirmDialogState.asStateFlow()

    private val _showDeleteDialogState = MutableRedeliverStateFlow(false)

    /** `true` when the delete-wishlist confirmation dialog should be visible. */
    val showDeleteDialogState = _showDeleteDialogState.asStateFlow()

    /** `true` when this screen edits an existing wishlist and may therefore offer deletion. */
    val canDelete: Boolean = !isCreating

    init {
        var inited = false
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { inited == false }.subscribeLoggingDropExceptions(scope) {
            node.config.wishlistId?.let { id ->
                _loadingState.value = true
                try {
                    val wishlist = model.getWishlist(id)
                    if (wishlist != null) {
                        _titleState.value = wishlist.title
                        _defaultPriceUnitsState.value = wishlist.defaultPriceUnits
                    }
                } finally {
                    _loadingState.value = false
                }
            }
            inited = true
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
     * Updates the default currency/units field and marks the form as dirty.
     *
     * @param units New default currency/units label.
     */
    fun onDefaultPriceUnitsChanged(units: String) {
        _defaultPriceUnitsState.value = units
        _isDirtyState.value = true
    }

    /**
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`,
     * otherwise delegates to [WishlistEditViewInteractor.onNavigateBack].
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and delegates to [WishlistEditViewInteractor.onNavigateBack]. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the confirm dialog, returning the user to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /** Requests wishlist deletion: shows the delete confirmation dialog. No-op in create mode. */
    fun onDelete() {
        if (!canDelete) return
        _showDeleteDialogState.value = true
    }

    /**
     * Confirms deletion: removes the wishlist via the model, then navigates back exactly as a
     * plain "back" would (delegates to [WishlistEditViewInteractor.onNavigateBack]).
     */
    fun onConfirmDelete() {
        val id = node.config.wishlistId ?: run {
            _showDeleteDialogState.value = false
            return
        }
        scope.launchLoggingDropExceptions {
            _showDeleteDialogState.value = false
            _loadingState.value = true
            try {
                model.deleteWishlist(id)
            } finally {
                _loadingState.value = false
            }
            interactor.onNavigateBack(node)
        }
    }

    /** Cancels the delete dialog, returning the user to the form. */
    fun onCancelDelete() {
        _showDeleteDialogState.value = false
    }

    /**
     * Saves the wishlist (create or update) and delegates to [WishlistEditViewInteractor.onSaved] on success.
     * No-op when [titleState] is blank or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            if (title.isBlank()) return@launchLoggingDropExceptions
            val defaultPriceUnits = _defaultPriceUnitsState.value.trim()
            _loadingState.value = true
            try {
                val id = node.config.wishlistId
                if (id == null) {
                    model.createWishlist(title, defaultPriceUnits)
                } else {
                    model.updateWishlist(id, title, defaultPriceUnits)
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
