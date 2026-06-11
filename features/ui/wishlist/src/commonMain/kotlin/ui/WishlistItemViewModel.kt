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
 * ViewModel for the wishlist item read-only view screen.
 *
 * Loads the item identified by [WishlistItemViewConfig.wishlistItemId] and the parent wishlist.
 * Exposes [isOwnerState] so the view can conditionally render an "Edit" button.
 *
 * Booking and any other item-scoped extra views are no longer hard-coded here: each registered
 * [WishlistAdditionalConfigsProvider] contributes one compact view drawn INLINE by the item view
 * (via `InjectNavigationChain` / `InjectNavigationNode`); see [additionalConfigsProviders].
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 * @param additionalConfigsProviders All registered providers of item-scoped extra views, injected
 *   via Koin `getAllDistinct`; each drawn inline in the item view.
 */
class WishlistItemViewModel(
    private val node: NavigationNode<WishlistItemViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistItemViewInteractor,
    val additionalConfigsProviders: List<WishlistAdditionalConfigsProvider>
) : ViewModel<ViewConfig>(node) {
    private val _itemState = MutableRedeliverStateFlow<RegisteredWishlistItem?>(null)

    /** The loaded item, `null` while loading or when not found. */
    val itemState = _itemState.asStateFlow()

    private val _parentWishlistState = MutableRedeliverStateFlow<RegisteredWishlist?>(null)

    /**
     * `true` when the authenticated caller is the parent wishlist owner. Controls visibility of the
     * Edit button. Derived reactively from the loaded parent wishlist and the auth "me" flow
     * ([WishlistsModel.currentUserIdFlow]), so it self-corrects once the cold-start `getMe()`
     * round-trip completes and on later login/logout (PR #31 F2). A missing (`null`) parent wishlist
     * counts as not-owned.
     */
    val isOwnerState: StateFlow<Boolean> =
        combine(_parentWishlistState, model.currentUserIdFlow) { wishlist, currentUserId ->
            wishlist != null && model.isOwner(wishlist.userId, currentUserId)
        }.stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * `true` when an authenticated caller is viewing an item they do NOT own — the only case in which
     * copying into one of their own wishlists is offered. Controls visibility of the Copy button.
     */
    val canCopyState: StateFlow<Boolean> = combine(_parentWishlistState, model.currentUserIdFlow) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId != userId
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    private val _currencyEnabledState = MutableRedeliverStateFlow(false)

    /** `true` when the currency-conversion feature is enabled and the selector should be shown. */
    val currencyEnabledState = _currencyEnabledState.asStateFlow()

    private val _currenciesState = MutableRedeliverStateFlow<List<CurrencyInfo>>(emptyList())

    /** Currencies available in the conversion dropdown; empty when the feature is disabled. */
    val currenciesState = _currenciesState.asStateFlow()

    private val _ratesState = MutableRedeliverStateFlow<CurrencyRates?>(null)

    /** Latest exchange-rate snapshot used to convert the displayed price; `null` when unavailable. */
    val ratesState = _ratesState.asStateFlow()

    /** Shared selected conversion target; `null` means original price. */
    val selectedCurrencyState: StateFlow<CurrencyCode?> = model.selectedCurrency

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            val item = try {
                _parentWishlistState.value = model.getWishlist(node.config.wishlistId)
                model.getWishlistItems(node.config.wishlistId)
                    .find { it.id == node.config.wishlistItemId }
                    .also { _itemState.value = it }
            } finally {
                _loadingState.value = false
            }
            // Item may have been deleted (here or from the edit screen) — leave the screen
            // automatically when it no longer exists, matching a plain back navigation.
            if (item == null) {
                interactor.onBack(node)
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
     * @param code Target currency, or `null` to display the original price.
     */
    fun onCurrencySelected(code: CurrencyCode?) {
        model.selectCurrency(code)
    }

    /**
     * Builds the download URL for an image attached to the item so the view can render it.
     *
     * @param id Image identifier (one of [RegisteredWishlistItem.imageIds]).
     * @return Relative download URL.
     */
    fun imageUrl(id: FileId): String = model.imageUrl(id)

    /**
     * Downloads the raw bytes of an attached image (for platforms that decode images locally).
     *
     * @param id Image identifier.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray? = model.loadImageBytes(id)

    /** Delegates to [WishlistItemViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [WishlistItemViewInteractor.onEditItem]. Only meaningful when [isOwnerState] is `true`. */
    fun onEditItem() {
        scope.launchLoggingDropExceptions { interactor.onEditItem(node) }
    }

    /** Delegates to [WishlistItemViewInteractor.onCopyItem]. Only meaningful when [canCopyState] is `true`. */
    fun onCopyItem() {
        scope.launchLoggingDropExceptions { interactor.onCopyItem(node) }
    }
}
