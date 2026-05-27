package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the admin wishlist item create/edit screen.
 *
 * When [AdminWishlistItemEditViewConfig.itemId] is `null`, operates in create mode.
 * When non-null, loads the existing item and pre-fills all fields.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Admin data source.
 * @param interactor Navigation delegate for this screen.
 */
class AdminWishlistItemEditViewModel(
    private val node: NavigationNode<AdminWishlistItemEditViewConfig, ViewConfig>,
    private val model: AdminPanelModel,
    private val interactor: AdminWishlistItemEditViewInteractor
) : ViewModel<ViewConfig>(node) {
    /** `true` when operating in create mode. */
    val isCreating: Boolean = node.config.itemId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

    private val _priceState = MutableRedeliverStateFlow("")

    /** Current value of the approximate price input (as raw string). */
    val priceState = _priceState.asStateFlow()

    private val _priceUnitsState = MutableRedeliverStateFlow("")

    /** Current value of the price units/currency input field. */
    val priceUnitsState = _priceUnitsState.asStateFlow()

    private val _descriptionState = MutableRedeliverStateFlow("")

    /** Current value of the description input field. */
    val descriptionState = _descriptionState.asStateFlow()

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
            node.config.itemId?.let { itemId ->
                _loadingState.value = true
                try {
                    val items = model.getItemsByWishlist(node.config.wishlistId)
                    val item = items.firstOrNull { it.id == itemId }
                    if (item != null) {
                        _titleState.value = item.title
                        _priceState.value = item.approximatePrice?.value?.toString() ?: ""
                        _priceUnitsState.value = item.priceUnits
                        _descriptionState.value = item.description
                    }
                } finally {
                    _loadingState.value = false
                }
            }
            inited = true
        }
    }

    /** Updates [titleState] and marks the form dirty. */
    fun onTitleChanged(title: String) {
        _titleState.value = title
        _isDirtyState.value = true
    }

    /** Updates [priceState] and marks the form dirty. */
    fun onPriceChanged(price: String) {
        _priceState.value = price
        _isDirtyState.value = true
    }

    /** Updates [priceUnitsState] and marks the form dirty. */
    fun onPriceUnitsChanged(units: String) {
        _priceUnitsState.value = units
        _isDirtyState.value = true
    }

    /** Updates [descriptionState] and marks the form dirty. */
    fun onDescriptionChanged(description: String) {
        _descriptionState.value = description
        _isDirtyState.value = true
    }

    /**
     * Attempts to navigate back. Shows confirm dialog when [isDirtyState] is `true`,
     * otherwise delegates to [AdminWishlistItemEditViewInteractor.onNavigateBack].
     */
    fun onBack() {
        if (_isDirtyState.value) {
            _showConfirmDialogState.value = true
        } else {
            scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
        }
    }

    /** Confirms discarding changes and delegates to [AdminWishlistItemEditViewInteractor.onNavigateBack]. */
    fun onConfirmBack() {
        _showConfirmDialogState.value = false
        scope.launchLoggingDropExceptions { interactor.onNavigateBack(node) }
    }

    /** Cancels the confirm dialog, returning the admin to the form. */
    fun onCancelBack() {
        _showConfirmDialogState.value = false
    }

    /**
     * Saves the wishlist item (create or update) and delegates to
     * [AdminWishlistItemEditViewInteractor.onSaved] on success.
     * No-op when [titleState] is blank or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            if (title.isBlank()) return@launchLoggingDropExceptions
            val price = _priceState.value.trim().toDoubleOrNull()?.let { Amount(it) }
            val units = _priceUnitsState.value.trim()
            val description = _descriptionState.value.trim()
            val item = NewWishlistItem(
                wishlistId = node.config.wishlistId,
                title = title,
                approximatePrice = price,
                priceUnits = units,
                description = description
            )
            _loadingState.value = true
            try {
                val id = node.config.itemId
                if (id == null) {
                    model.createWishlistItem(item)
                } else {
                    model.updateWishlistItem(id, item)
                }
                interactor.onSaved(node)
            } finally {
                _loadingState.value = false
            }
        }
    }
}
