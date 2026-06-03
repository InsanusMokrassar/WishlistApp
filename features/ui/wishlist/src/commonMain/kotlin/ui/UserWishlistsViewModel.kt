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
     * Items of the target user grouped by their wishlist. Wishlists with no items are omitted so
     * the view never renders an empty separator.
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

    /**
     * Flattened, custom-sorted view of every item across the loaded sections, valid only when
     * [sortModeState] is not [WishlistSortMode.None]. Empty for [WishlistSortMode.None] (the view
     * renders [sectionsState] instead). Each entry keeps its wishlist title so the view can show it
     * after the item title in brackets.
     */
    val sortedItemsState = combine(_sectionsState, _sortModeState) { sections, mode ->
        if (mode == WishlistSortMode.None) {
            emptyList()
        } else {
            val flat = sections.flatMap { section ->
                section.items.map { SortedWishlistItem(it, section.wishlist.title) }
            }
            when (mode) {
                WishlistSortMode.Cost -> flat.sortedWith(
                    compareBy(nullsLast()) { it.item.approximatePrice }
                )
                WishlistSortMode.Priority -> flat.sortedByDescending { it.item.priority.weight }
                WishlistSortMode.Title -> flat.sortedBy { it.item.title.lowercase() }
                WishlistSortMode.None -> flat
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _currencyEnabledState = MutableRedeliverStateFlow(false)

    /** `true` when the currency-conversion feature is enabled and the selector should be shown. */
    val currencyEnabledState = _currencyEnabledState.asStateFlow()

    private val _currenciesState = MutableRedeliverStateFlow<List<CurrencyInfo>>(emptyList())

    /** Currencies available in the conversion dropdown; empty when the feature is disabled. */
    val currenciesState = _currenciesState.asStateFlow()

    private val _ratesState = MutableRedeliverStateFlow<CurrencyRates?>(null)

    /** Latest exchange-rate snapshot used to convert displayed prices; `null` when unavailable. */
    val ratesState = _ratesState.asStateFlow()

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
            _loadingState.value = true
            try {
                _userNameState.value = model.getUserName(node.config.userId)
                _sectionsState.value = model.getUserWishlists(node.config.userId)
                    .map { wishlist -> UserWishlistsSection(wishlist, model.getWishlistItems(wishlist.id)) }
                    .filter { it.items.isNotEmpty() }
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
     * Changes the presentation of the items between rows and a card grid.
     *
     * @param mode New view mode.
     */
    fun onViewModeSelected(mode: WishlistViewMode) {
        _viewModeState.value = mode
    }

    /** Delegates to [UserWishlistsViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Opens the target user's public profile via [UserWishlistsViewInteractor.onOpenProfile]. */
    fun onOpenProfile() {
        scope.launchLoggingDropExceptions { interactor.onOpenProfile(node, node.config.userId) }
    }

    /** Download URL of image [id], for platforms that render directly from a URL (JS). */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /** Raw bytes of image [id], for platforms that decode images locally (JVM/Android). */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)
}
