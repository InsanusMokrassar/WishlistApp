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
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/**
 * One section of the all-items screen: a wishlist together with the items it owns.
 *
 * Used to render a separator (the wishlist title) above the wishlist's items.
 *
 * @param wishlist Wishlist this section represents; its title is shown as the separator header.
 * @param items Items belonging to [wishlist], in their stored order.
 */
data class UserWishlistsSection(
    val wishlist: WishlistsFeatureWishlist,
    val items: List<WishlistsFeatureItem>
)

/**
 * Ordering applied to the flattened all-items list.
 *
 * [None] keeps the default grouping-by-wishlist presentation; every other mode flattens the items
 * across all wishlists into a single sorted list (see [UserWishlistsViewModel.sortedItemsState]).
 */
enum class WishlistSortMode {
    /** No custom sorting; items stay grouped under their wishlist headers. */
    None,

    /** Sort by [WishlistsFeatureItem.approximatePrice] ascending; items without a price go last. */
    Cost,

    /** Sort by [dev.inmo.wishlist.features.wishlist.common.models.Priority.weight] descending (most important first). */
    Priority,

    /** Sort by [WishlistsFeatureItem.title] case-insensitively, ascending. */
    Title
}

/**
 * Sort modes to offer in the selector: all modes, minus [WishlistSortMode.Cost] when price sorting
 * is unavailable (currency feature disabled and items use mixed currencies).
 *
 * @param costSortAvailable Whether sorting by price is meaningful for the current item set.
 * @return Modes to render in the sort selector.
 */
fun sortModesFor(costSortAvailable: Boolean): List<WishlistSortMode> =
    if (costSortAvailable) {
        WishlistSortMode.entries
    } else {
        WishlistSortMode.entries.filter { it != WishlistSortMode.Cost }
    }

/**
 * One item of the flattened, custom-sorted all-items list together with the title of the wishlist it
 * belongs to, so the view can append the wishlist title after the item title in brackets.
 *
 * @param item Item to display.
 * @param wishlistTitle Title of the wishlist [item] belongs to.
 */
data class SortedWishlistItem(
    val item: WishlistsFeatureItem,
    val wishlistTitle: String
)

/**
 * ViewModel for the "all items" presentation of a user's wishlists.
 *
 * Loads every wishlist owned by [UserWishlistsViewConfig.userId] together with its items on init
 * and on node resume, grouping them into [UserWishlistsSection]s so the view can render each
 * wishlist's items under a separator, and delegates navigation side-effects to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class UserWishlistsViewModel(
    private val node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: UserWishlistsViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _sectionsState = MutableRedeliverStateFlow<List<UserWishlistsSection>>(emptyList())

    /**
     * Items of the target user grouped by their wishlist. Wishlists with no items are included with
     * an empty [UserWishlistsSection.items] list so the view can render their header with an
     * "empty" placeholder instead of hiding them.
     */
    val sectionsState = _sectionsState.asStateFlow()

    private val _userNameState = MutableRedeliverStateFlow<String?>(null)

    /**
     * Display name of the target user, used to build the personalized title. `null` until resolved
     * or when the user is unknown — the view then falls back to the generic title.
     */
    val userNameState = _userNameState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    /**
     * Label for the contextual Back button. Back replaces this screen with the global users list
     * ([UsersListViewConfig]), whose name is a static title rather than a per-entity name, so this
     * flow stays `null` and the view supplies the static users-list label as the fallback.
     */
    val backLabelState: StateFlow<String?> = MutableRedeliverStateFlow<String?>(null).asStateFlow()

    private val _sortModeState = MutableRedeliverStateFlow(WishlistSortMode.None)

    /**
     * Single source of the "sorting is meaningful" rule (two or more items exist across all loaded
     * sections): sorting fewer than two items is a no-op and a non-[WishlistSortMode.None] mode would
     * blank the grouped presentation (PR #31 T1). [sortModeState], [sortSelectorVisibleState] and
     * [sortedItemsState] all derive from this one flow so the threshold lives in exactly one place
     * (PR #31 F7).
     */
    private val sortableState: StateFlow<Boolean> =
        _sectionsState.map { sections -> sections.sumOf { it.items.size } >= 2 }
            .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Effective ordering applied by the views. Mirrors the user selection from [onSortModeSelected]
     * but is clamped to [WishlistSortMode.None] while the sections are not [sortableState] —
     * sorting fewer than two items is meaningless and a non-[WishlistSortMode.None] mode would blank
     * the grouped presentation (PR #31 T1). The raw selection is kept privately, so it re-applies
     * when the item count grows back to two or more.
     */
    val sortModeState: StateFlow<WishlistSortMode> =
        combine(_sortModeState, sortableState) { mode, sortable ->
            if (sortable) mode else WishlistSortMode.None
        }.stateIn(scope, SharingStarted.Eagerly, WishlistSortMode.None)

    /**
     * `true` when the sort selector should be rendered: two or more items exist across all loaded
     * sections. Hidden otherwise — with fewer than two items every mode is equivalent to
     * [WishlistSortMode.None], so the selector (including a meaningless Cost option for an empty
     * item set) would only mislead (PR #31 T1).
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
        combine(_sectionsState, _currencyEnabledState) { sections, enabled ->
            val pricedUnits = sections.flatMap { it.items }.filter { it.approximatePrice != null }.map { it.priceUnits }
            isCostSortAvailable(pricedUnits, enabled)
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Flattened, custom-sorted view of every item across the loaded sections, valid only when
     * [sortModeState] is not [WishlistSortMode.None]. Empty for [WishlistSortMode.None] (the view
     * renders [sectionsState] instead). Each entry keeps its wishlist title so the view can show it
     * after the item title in brackets. [WishlistSortMode.Cost] compares prices in the items' dominant
     * currency (converted via [ratesState] when the feature is enabled), with unpriced/unconvertible
     * items last; it falls back to grouped order when cost sorting is unavailable. Also clamped to
     * [WishlistSortMode.None] while fewer than two items are loaded (PR #31 T1).
     */
    val sortedItemsState =
        combine(_sectionsState, _sortModeState, _ratesState, _currencyEnabledState, sortableState) { sections, mode, rates, enabled, sortable ->
            val allItems = sections.flatMap { it.items }
            val pricedUnits = allItems.filter { it.approximatePrice != null }.map { it.priceUnits }
            val effectiveMode = when {
                !sortable -> WishlistSortMode.None
                mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled) -> WishlistSortMode.None
                else -> mode
            }
            if (effectiveMode == WishlistSortMode.None) {
                emptyList()
            } else {
                val flat = sections.flatMap { section ->
                    section.items.map { SortedWishlistItem(it, section.wishlist.title) }
                }
                when (effectiveMode) {
                    WishlistSortMode.Cost -> {
                        val common = dominantCurrency(allItems.map { it.priceUnits })
                        flat.sortedWith(
                            compareBy(nullsLast()) { costSortKey(it.item.approximatePrice, it.item.priceUnits, common, rates) }
                        )
                    }
                    WishlistSortMode.Priority -> flat.sortedByDescending { it.item.priority.weight }
                    WishlistSortMode.Title -> flat.sortedBy { it.item.title.lowercase() }
                    WishlistSortMode.None -> flat
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Shared selected conversion target; `null` means original prices. */
    val selectedCurrencyState: StateFlow<CurrencyCode?> = model.selectedCurrency

    /**
     * `true` when the authenticated caller is the user whose items are displayed. The view shows the
     * "New Wishlist" button only when this is `true`, mirroring the ownership gating of the
     * wishlists list screen. Derived reactively from [WishlistsModel.isOwnerFlow], so it self-corrects
     * once the cold-start `getMe()` round-trip completes and on later login/logout (PR #31 F2).
     */
    val isOwnerState: StateFlow<Boolean> =
        model.isOwnerFlow(node.config.userId).stateIn(scope, SharingStarted.Eagerly, false)

    private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.Grid)

    /**
     * Currently selected presentation of the items: [WishlistViewMode.List] (rows) or
     * [WishlistViewMode.Grid] (cards). Defaults to [WishlistViewMode.Grid].
     */
    val viewModeState = _viewModeState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                // Fan out the independent fetches instead of awaiting them one-by-one: the user name
                // runs concurrently with the wishlists load, and each wishlist's items are fetched in
                // parallel rather than W serial round-trips (PR #31 F8).
                coroutineScope {
                    val userNameDeferred = async { model.getUserName(node.config.userId) }
                    val sectionsDeferred = async {
                        model.getUserWishlists(node.config.userId).map { wishlist ->
                            async { UserWishlistsSection(wishlist, model.getWishlistItems(wishlist.id)) }
                        }.awaitAll()
                    }
                    _userNameState.value = userNameDeferred.await()
                    _sectionsState.value = sectionsDeferred.await()
                }
            } finally {
                _loadingState.value = false
            }
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

    /**
     * Delegates to [UserWishlistsViewInteractor.onItemSelected].
     *
     * @param item Item whose detail screen should be opened.
     */
    fun onItemSelected(item: WishlistsFeatureItem) {
        scope.launchLoggingDropExceptions { interactor.onItemSelected(node, item.id, item.wishlistId) }
    }

    /**
     * Delegates to [UserWishlistsViewInteractor.onWishlistSelected].
     *
     * @param wishlist Wishlist whose detail screen should be opened.
     */
    fun onWishlistSelected(wishlist: WishlistsFeatureWishlist) {
        scope.launchLoggingDropExceptions { interactor.onWishlistSelected(node, wishlist.id) }
    }

    /**
     * Changes the active ordering of the items.
     *
     * @param mode New sort mode; [WishlistSortMode.None] restores the grouped presentation.
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

    /** Delegates to [UserWishlistsViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Opens the target user's public profile via [UserWishlistsViewInteractor.onOpenProfile]. */
    fun onOpenProfile() {
        scope.launchLoggingDropExceptions { interactor.onOpenProfile(node, node.config.userId) }
    }

    /** Opens the wishlist create form via [UserWishlistsViewInteractor.onCreateWishlistClick]. */
    fun onCreateWishlist() {
        scope.launchLoggingDropExceptions { interactor.onCreateWishlistClick(node) }
    }

    /**
     * Opens the item create form for [wishlist] via [UserWishlistsViewInteractor.onCreateItemClick].
     *
     * @param wishlist Wishlist the new item should be created in.
     */
    fun onCreateItem(wishlist: WishlistsFeatureWishlist) {
        scope.launchLoggingDropExceptions { interactor.onCreateItemClick(node, wishlist.id) }
    }

    /** Download URL of image [id], for platforms that render directly from a URL (JS). */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /** Raw bytes of image [id], for platforms that decode images locally (JVM/Android). */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}
