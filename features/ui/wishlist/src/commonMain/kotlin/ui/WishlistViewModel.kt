package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.currency.common.utils.costSortKey
import dev.inmo.wishlist.features.currency.common.utils.dominantCurrency
import dev.inmo.wishlist.features.currency.common.utils.isCostSortAvailable
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the wishlist detail screen.
 *
 * Loads the wishlist and its items on init. Exposes [isOwnerState] which is `true`
 * when the authenticated caller is the wishlist owner — used to show edit controls.
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistViewModel(
    private val node: NavigationNode<WishlistViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _wishlistState = MutableRedeliverStateFlow<RegisteredWishlist?>(null)

    /** The loaded wishlist, `null` while loading or on error. */
    val wishlistState = _wishlistState.asStateFlow()

    private val _itemsState = MutableRedeliverStateFlow<List<RegisteredWishlistItem>>(emptyList())

    /** Items belonging to the loaded wishlist. */
    val itemsState = _itemsState.asStateFlow()

    private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)

    /**
     * `true` when the authenticated caller is the wishlist owner.
     * Derived from [_wishlistState] and [_currentUserIdState].
     */
    val isOwnerState: StateFlow<Boolean> = combine(_wishlistState, _currentUserIdState) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId == userId
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _sortModeState = MutableRedeliverStateFlow(WishlistSortMode.None)

    /**
     * Currently selected ordering of the items. [WishlistSortMode.None] keeps the stored order;
     * any other value reorders [sortedItemsState] accordingly.
     */
    val sortModeState = _sortModeState.asStateFlow()

    private val _currencyEnabledState = MutableRedeliverStateFlow(false)

    /** `true` when the currency-conversion feature is enabled and the selector should be shown. */
    val currencyEnabledState = _currencyEnabledState.asStateFlow()

    private val _currenciesState = MutableRedeliverStateFlow<List<CurrencyInfo>>(emptyList())

    /** Currencies available in the conversion dropdown; empty when the feature is disabled. */
    val currenciesState = _currenciesState.asStateFlow()

    private val _ratesState = MutableRedeliverStateFlow<CurrencyRates?>(null)

    /** Latest exchange-rate snapshot used to convert displayed prices; `null` when unavailable. */
    val ratesState = _ratesState.asStateFlow()

    /**
     * `true` when sorting by price is meaningful: the currency feature is enabled (prices can be
     * converted to a common currency) or every priced item already shares one currency label. The
     * view hides the Cost sort option when this is `false`.
     */
    val costSortAvailableState: StateFlow<Boolean> =
        combine(_itemsState, _currencyEnabledState) { items, enabled ->
            isCostSortAvailable(items.filter { it.approximatePrice != null }.map { it.priceUnits }, enabled)
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Items of the loaded wishlist reordered according to [sortModeState]. For
     * [WishlistSortMode.None] the stored order from [itemsState] is preserved. [WishlistSortMode.Cost]
     * compares prices in the items' dominant currency (converted via [ratesState] when the feature is
     * enabled), with unpriced/unconvertible items last; it falls back to the stored order when cost
     * sorting is unavailable. Mirrors the sort orders used by the all-items screen.
     */
    val sortedItemsState: StateFlow<List<RegisteredWishlistItem>> =
        combine(_itemsState, _sortModeState, _ratesState, _currencyEnabledState) { items, mode, rates, enabled ->
            val pricedUnits = items.filter { it.approximatePrice != null }.map { it.priceUnits }
            val effectiveMode =
                if (mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled)) {
                    WishlistSortMode.None
                } else {
                    mode
                }
            when (effectiveMode) {
                WishlistSortMode.None -> items
                WishlistSortMode.Cost -> {
                    val common = dominantCurrency(items.map { it.priceUnits })
                    items.sortedWith(
                        compareBy(nullsLast()) { costSortKey(it.approximatePrice, it.priceUnits, common, rates) }
                    )
                }
                WishlistSortMode.Priority -> items.sortedByDescending { it.priority.weight }
                WishlistSortMode.Title -> items.sortedBy { it.title.lowercase() }
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Shared selected conversion target; `null` means original prices. */
    val selectedCurrencyState: StateFlow<CurrencyCode?> = model.selectedCurrency

    private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.List)

    /**
     * Currently selected presentation of the items: [WishlistViewMode.List] (rows) or
     * [WishlistViewMode.Grid] (cards). Defaults to [WishlistViewMode.List].
     */
    val viewModeState = _viewModeState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            loadWishlist()
        }
        scope.launchLoggingDropExceptions {
            if (model.isCurrencyEnabled()) {
                _currencyEnabledState.value = true
                _currenciesState.value = model.availableCurrencies()
                _ratesState.value = model.currencyRates()
            }
        }
    }

    /**
     * Updates the shared currency-conversion target for all wishlist screens.
     *
     * @param code Target currency, or `null` to display original prices.
     */
    fun onCurrencySelected(code: CurrencyCode?) {
        model.selectCurrency(code)
    }

    private suspend fun loadWishlist() {
        _loadingState.value = true
        val wishlist = try {
            _currentUserIdState.value = model.getCurrentUserId()
            val loaded = model.getWishlist(node.config.wishlistId)
            _wishlistState.value = loaded
            _itemsState.value = model.getWishlistItems(node.config.wishlistId)
            loaded
        } finally {
            _loadingState.value = false
        }
        // Wishlist may have been deleted (here or from the edit screen) — leave the screen
        // automatically when it no longer exists, matching a plain back navigation.
        if (wishlist == null) {
            interactor.onBack(node)
        }
    }

    /** Delegates to [WishlistViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [WishlistViewInteractor.onEditWishlist]. Requires ownership. */
    fun onEditWishlist() {
        scope.launchLoggingDropExceptions { interactor.onEditWishlist(node) }
    }

    /**
     * Delegates to [WishlistViewInteractor.onViewItem].
     *
     * Both owners and non-owners open the read-only view first; owners may proceed to edit from there.
     *
     * @param itemId Item to view.
     */
    fun onViewItem(itemId: WishlistItemId) {
        scope.launchLoggingDropExceptions { interactor.onViewItem(node, itemId) }
    }

    /**
     * Changes the active ordering of the items.
     *
     * @param mode New sort mode; [WishlistSortMode.None] restores the stored order.
     */
    fun onSortModeSelected(mode: WishlistSortMode) {
        _sortModeState.value = mode
    }

    /**
     * Changes the presentation of the items between rows and a card grid.
     *
     * @param mode New view mode.
     */
    fun onViewModeSelected(mode: WishlistViewMode) {
        _viewModeState.value = mode
    }

    /**
     * Download URL of image [id], for platforms that render images directly from a URL (JS).
     *
     * @param id Image file identifier.
     * @return Absolute URL the image can be fetched from.
     */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /**
     * Raw bytes of image [id], for platforms that decode images locally (JVM/Android).
     *
     * @param id Image file identifier.
     * @return Encoded image bytes, or `null` when the download fails.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)

    /** Delegates to [WishlistViewInteractor.onAddItem]. */
    fun onAddItem() {
        scope.launchLoggingDropExceptions { interactor.onAddItem(node) }
    }
}
