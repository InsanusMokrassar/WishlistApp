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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val wishlist: RegisteredWishlist,
    val items: List<RegisteredWishlistItem>
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

    /** Sort by [RegisteredWishlistItem.approximatePrice] ascending; items without a price go last. */
    Cost,

    /** Sort by [dev.inmo.wishlist.features.wishlist.common.models.Priority.weight] descending (most important first). */
    Priority,

    /** Sort by [RegisteredWishlistItem.title] case-insensitively, ascending. */
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
    val item: RegisteredWishlistItem,
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

    private val _sortModeState = MutableRedeliverStateFlow(WishlistSortMode.None)

    /**
     * Currently selected ordering. [WishlistSortMode.None] keeps the grouped presentation; any other
     * value switches the view to the flat [sortedItemsState] list.
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
     * items last; it falls back to grouped order when cost sorting is unavailable.
     */
    val sortedItemsState =
        combine(_sectionsState, _sortModeState, _ratesState, _currencyEnabledState) { sections, mode, rates, enabled ->
            val allItems = sections.flatMap { it.items }
            val pricedUnits = allItems.filter { it.approximatePrice != null }.map { it.priceUnits }
            val effectiveMode =
                if (mode == WishlistSortMode.Cost && !isCostSortAvailable(pricedUnits, enabled)) {
                    WishlistSortMode.None
                } else {
                    mode
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

    private val _isOwnerState = MutableRedeliverStateFlow(false)

    /**
     * `true` when the authenticated caller is the user whose items are displayed. The view shows the
     * "New Wishlist" button only when this is `true`, mirroring the ownership gating of the
     * wishlists list screen.
     */
    val isOwnerState = _isOwnerState.asStateFlow()

    private val _viewModeState = MutableRedeliverStateFlow(WishlistViewMode.List)

    /**
     * Currently selected presentation of the items: [WishlistViewMode.List] (rows) or
     * [WishlistViewMode.Grid] (cards). Defaults to [WishlistViewMode.List].
     */
    val viewModeState = _viewModeState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                _isOwnerState.value = model.getCurrentUserId() == node.config.userId
                _userNameState.value = model.getUserName(node.config.userId)
                _sectionsState.value = model.getUserWishlists(node.config.userId)
                    .map { wishlist -> UserWishlistsSection(wishlist, model.getWishlistItems(wishlist.id)) }
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
    fun onItemSelected(item: RegisteredWishlistItem) {
        scope.launchLoggingDropExceptions { interactor.onItemSelected(node, item.id, item.wishlistId) }
    }

    /**
     * Delegates to [UserWishlistsViewInteractor.onWishlistSelected].
     *
     * @param wishlist Wishlist whose detail screen should be opened.
     */
    fun onWishlistSelected(wishlist: RegisteredWishlist) {
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

    /** Opens the wishlist create form via [UserWishlistsViewInteractor.onCreateWishlist]. */
    fun onCreateWishlist() {
        scope.launchLoggingDropExceptions { interactor.onCreateWishlist(node) }
    }

    /**
     * Opens the item create form for [wishlist] via [UserWishlistsViewInteractor.onCreateItem].
     *
     * @param wishlist Wishlist the new item should be created in.
     */
    fun onCreateItem(wishlist: RegisteredWishlist) {
        scope.launchLoggingDropExceptions { interactor.onCreateItem(node, wishlist.id) }
    }

    /** Download URL of image [id], for platforms that render directly from a URL (JS). */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /** Raw bytes of image [id], for platforms that decode images locally (JVM/Android). */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}
