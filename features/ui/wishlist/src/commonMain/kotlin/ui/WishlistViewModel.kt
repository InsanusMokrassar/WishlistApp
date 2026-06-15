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
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    private val _backLabelState = MutableRedeliverStateFlow<String?>(null)

    /**
     * Label for the contextual Back button: the wishlist owner's display name. Back replaces this
     * screen with that owner's all-items screen, so the button names the destination. `null` until
     * the owner name resolves (or unknown) — the view then falls back to the generic back string.
     * Resolved from the loaded wishlist's `userId` via [WishlistsModel.getUserName] in [loadWishlist].
     */
    val backLabelState = _backLabelState.asStateFlow()

    /**
     * `true` when the authenticated caller is the wishlist owner. Derived reactively from the loaded
     * [wishlistState] and the auth "me" flow ([WishlistsModel.currentUserIdFlow]), so it self-corrects
     * once the cold-start `getMe()` round-trip completes and on later login/logout (PR #31 F2). A
     * missing (`null`) wishlist counts as not-owned.
     */
    val isOwnerState: StateFlow<Boolean> =
        merge(_wishlistState, model.currentUserIdFlow).map {
            val wishlist = wishlistState.value
            val currentUserId = model.currentUserIdFlow.value

            wishlist != null && model.isOwner(wishlist.userId, currentUserId)
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * `true` when an authenticated caller views a wishlist they do NOT own — the only case in which
     * copying the whole wishlist into their own profile is offered. Controls the Copy button.
     */
    val canCopyState: StateFlow<Boolean> = combine(_wishlistState, model.currentUserIdFlow) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId != userId
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _copyRequestedState = MutableRedeliverStateFlow(false)

    /** `true` once a whole-wishlist copy has been queued; the view shows a confirmation message. */
    val copyRequestedState = _copyRequestedState.asStateFlow()

    private val _copyFailedState = MutableRedeliverStateFlow(false)

    /** `true` when the most recent copy-enqueue attempt failed. */
    val copyFailedState = _copyFailedState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _sortModeState = MutableRedeliverStateFlow(WishlistSortMode.None)

    /**
     * Single source of the "sorting is meaningful" rule (the wishlist holds two or more items):
     * sorting fewer than two items is a no-op and a non-[WishlistSortMode.None] mode would clamp the
     * list (PR #31 T1). [sortModeState], [sortSelectorVisibleState] and [sortedItemsState] all derive
     * from this one flow so the threshold lives in exactly one place (PR #31 F7).
     */
    private val sortableState: StateFlow<Boolean> =
        _itemsState.map { items -> items.size >= 2 }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Effective ordering applied by the views. Mirrors the user selection from [onSortModeSelected]
     * but is clamped to [WishlistSortMode.None] while the wishlist is not [sortableState] —
     * sorting fewer than two items is meaningless (PR #31 T1). The raw selection is kept privately,
     * so it re-applies when the item count grows back to two or more.
     */
    val sortModeState: StateFlow<WishlistSortMode> =
        combine(_sortModeState, sortableState) { mode, sortable ->
            if (sortable) mode else WishlistSortMode.None
        }.stateIn(scope, SharingStarted.Eagerly, WishlistSortMode.None)

    /**
     * `true` when the sort selector should be rendered: the wishlist holds two or more items.
     * Hidden otherwise — with fewer than two items every mode is equivalent to
     * [WishlistSortMode.None] (PR #31 T1).
     */
    val sortSelectorVisibleState: StateFlow<Boolean> = sortableState

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
     * sorting is unavailable. Mirrors the sort orders used by the all-items screen. Also clamped to
     * [WishlistSortMode.None] while fewer than two items are loaded (PR #31 T1).
     */
    val sortedItemsState: StateFlow<List<RegisteredWishlistItem>> =
        combine(_itemsState, _sortModeState, _ratesState, _currencyEnabledState, sortableState) { items, mode, rates, enabled, sortable ->
            val pricedUnits = items.filter { it.approximatePrice != null }.map { it.priceUnits }
            val effectiveMode = when {
                !sortable -> WishlistSortMode.None
                mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled) -> WishlistSortMode.None
                else -> mode
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

    private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.Grid)

    /**
     * Currently selected presentation of the items: [WishlistViewMode.List] (rows) or
     * [WishlistViewMode.Grid] (cards). Defaults to [WishlistViewMode.Grid].
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
        scope.launchLoggingDropExceptions {
            _viewModeState.value = model.getSavedViewMode()
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
            val loaded = model.getWishlist(node.config.wishlistId)
            _wishlistState.value = loaded
            _itemsState.value = model.getWishlistItems(node.config.wishlistId)
            loaded
        } finally {
            _loadingState.value = false
        }
        // Resolve the owner's name for the contextual Back label, reusing the just-loaded wishlist's
        // userId (one cached UsersFeature lookup; no item round-trips repeated).
        _backLabelState.value = wishlist?.userId?.let { model.getUserName(it) }
        // Wishlist may have been deleted (here or from the edit screen) — leave the screen
        // automatically when it no longer exists, matching a plain back navigation.
        if (wishlist == null) {
            interactor.onBack(node, null)
        }
    }

    /**
     * Delegates to [WishlistViewInteractor.onBack], passing the loaded wishlist's owner id so Back
     * leads to that owner's all-items screen. A `null` owner (wishlist not loaded) falls back to pop.
     */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node, wishlistState.value?.userId) }
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
     * Changes the presentation of the items between rows and a card grid and persists the choice so it
     * is restored on the next screen open (page refresh / app reopen).
     *
     * @param mode New view mode.
     */
    fun onViewModeSelected(mode: WishlistViewMode) {
        _viewModeState.value = mode
        scope.launchLoggingDropExceptions { model.saveViewMode(mode) }
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

    /**
     * Enqueues a server-side background deep copy of the displayed wishlist into the caller's
     * profile and reflects the outcome in [copyRequestedState] / [copyFailedState].
     * Only meaningful when [canCopyState] is `true`.
     */
    fun onCopyWishlist() {
        scope.launchLoggingDropExceptions {
            _copyFailedState.value = false
            val queued = model.enqueueWishlistCopy(node.config.wishlistId)
            if (queued) {
                _copyRequestedState.value = true
            } else {
                _copyFailedState.value = true
            }
        }
    }
}
