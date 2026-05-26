package dev.inmo.wishlist.features.ui.wishlist.ui

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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile

/**
 * ViewModel for the wishlist item create/edit screen.
 *
 * When [WishlistItemEditViewConfig.wishlistItemId] is null, operates in create mode.
 * When non-null, loads the existing item and pre-fills all fields.
 *
 * [priceState] holds the price as a decimal string (e.g. `"19.99"`). On save it is parsed
 * with [Amount.invoke(Double)] and stored as `null` when blank or not a valid number.
 *
 * Back navigation shows a discard-changes confirmation modal when [isDirtyState] is `true`.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 */
class WishlistItemEditViewModel(
    private val node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>,
    private val model: WishlistsModel
) : ViewModel<ViewConfig>(node) {
    /** `true` when this screen is in create mode (no existing item id). */
    val isCreating: Boolean = node.config.wishlistItemId == null

    private val _titleState = MutableRedeliverStateFlow("")

    /** Current value of the title input field. */
    val titleState = _titleState.asStateFlow()

    private val _descriptionState = MutableRedeliverStateFlow("")

    /** Current value of the description input field. */
    val descriptionState = _descriptionState.asStateFlow()

    private val _priceState = MutableRedeliverStateFlow("")

    /** Current price as decimal string; blank means no price. */
    val priceState = _priceState.asStateFlow()

    private val _priceUnitsState = MutableRedeliverStateFlow("")

    /** Current currency or units label. */
    val priceUnitsState = _priceUnitsState.asStateFlow()

    private val _linksState = MutableRedeliverStateFlow<List<String>>(emptyList())

    /** Current list of external links. */
    val linksState = _linksState.asStateFlow()

    private val _newLinkState = MutableRedeliverStateFlow("")

    /** Text currently typed in the "add link" input field. */
    val newLinkState = _newLinkState.asStateFlow()

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
        merge(flowOf(Unit), node.onResumeFlow).takeWhile { inited == false }.subscribeLoggingDropExceptions(scope) {
            node.config.wishlistItemId?.let { itemId ->
                _loadingState.value = true
                try {
                    val item = model.getWishlistItems(node.config.wishlistId).find { it.id == itemId }
                    if (item != null) {
                        _titleState.value = item.title
                        _descriptionState.value = item.description
                        _priceState.value = item.approximatePrice?.toString() ?: ""
                        _priceUnitsState.value = item.priceUnits
                        _linksState.value = item.links
                    }
                } finally {
                    _loadingState.value = false
                }
            }
            inited = true
        }
    }

    /** @param v New title value. */
    fun onTitleChanged(v: String) { _titleState.value = v; _isDirtyState.value = true }

    /** @param v New description value. */
    fun onDescriptionChanged(v: String) { _descriptionState.value = v; _isDirtyState.value = true }

    /** @param v New price string (decimal notation). */
    fun onPriceChanged(v: String) { _priceState.value = v; _isDirtyState.value = true }

    /** @param v New currency/units label. */
    fun onPriceUnitsChanged(v: String) { _priceUnitsState.value = v; _isDirtyState.value = true }

    /** @param v Text typed in the new-link input. */
    fun onNewLinkChanged(v: String) { _newLinkState.value = v }

    /** Appends [newLinkState] to the links list and clears the input. No-op when blank. */
    fun onAddLink() {
        val link = _newLinkState.value.trim()
        if (link.isNotBlank()) {
            _linksState.value = _linksState.value + link
            _newLinkState.value = ""
            _isDirtyState.value = true
        }
    }

    /**
     * Removes the link at [index] from the links list.
     *
     * @param index Zero-based index of the link to remove.
     */
    fun onRemoveLink(index: Int) {
        _linksState.value = _linksState.value.toMutableList().also { it.removeAt(index) }
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
     * Saves the item (create or update) and pops the current node on success.
     * No-op when [titleState] is blank or a request is already in flight.
     */
    fun onSave() {
        scope.launchLoggingDropExceptions {
            val title = _titleState.value.trim()
            if (title.isBlank()) return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                val price = _priceState.value.trim().toDoubleOrNull()?.let { Amount(it) }
                val item = NewWishlistItem(
                    wishlistId = node.config.wishlistId,
                    title = title,
                    description = _descriptionState.value.trim(),
                    priceUnits = _priceUnitsState.value.trim(),
                    approximatePrice = price,
                    links = _linksState.value
                )
                val itemId = node.config.wishlistItemId
                if (itemId == null) {
                    model.createWishlistItem(item)
                } else {
                    model.updateWishlistItem(itemId, item)
                }
                node.chain.pop()
            } finally {
                _loadingState.value = false
            }
        }
    }
}
