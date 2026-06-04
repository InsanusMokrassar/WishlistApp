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
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.BookingState
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
 * Navigation side-effects are delegated to [interactor].
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Wishlist data source.
 * @param interactor Navigation delegate for this screen.
 */
class WishlistItemViewModel(
    private val node: NavigationNode<WishlistItemViewConfig, ViewConfig>,
    private val model: WishlistsModel,
    private val interactor: WishlistItemViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _itemState = MutableRedeliverStateFlow<RegisteredWishlistItem?>(null)

    /** The loaded item, `null` while loading or when not found. */
    val itemState = _itemState.asStateFlow()

    private val _wishlistState = MutableRedeliverStateFlow<RegisteredWishlist?>(null)
    private val _currentUserIdState = MutableRedeliverStateFlow<UserId?>(null)

    /**
     * `true` when the authenticated caller is the parent wishlist owner.
     * Controls visibility of the Edit button.
     */
    val isOwnerState: StateFlow<Boolean> = combine(_wishlistState, _currentUserIdState) { wishlist, userId ->
        wishlist != null && userId != null && wishlist.userId == userId
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

    private val _bookingState = MutableRedeliverStateFlow<BookingState?>(null)

    /**
     * Booking (gift-reservation) state visible to the current caller, or `null` when booking is
     * hidden from the caller — i.e. the caller is the item owner or is not authorized. The view
     * renders the booking section only when this is non-null, so owners and anonymous users never
     * see booking controls (the server is the authoritative gate; this is defense-in-depth).
     */
    val bookingState = _bookingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            val item = try {
                _currentUserIdState.value = model.getCurrentUserId()
                _wishlistState.value = model.getWishlist(node.config.wishlistId)
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
            } else {
                // Booking state is server-gated: returns null for the owner and for anonymous
                // callers, so the booking section stays hidden from them.
                _bookingState.value = model.getBookingState(item.id)
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

    /**
     * Reloads [bookingState] for the currently loaded item.
     *
     * No-op when the item is not loaded; reflects the latest server-side state (including the
     * `null`/hidden result for owners and anonymous callers).
     */
    private suspend fun reloadBookingState() {
        val item = _itemState.value ?: return
        _bookingState.value = model.getBookingState(item.id)
    }

    /**
     * Reserves the current item for gifting on behalf of the caller, then refreshes [bookingState].
     *
     * Guarded by [loadingState]. Meaningful only when [bookingState] is non-null and not already
     * booked; the server rejects any disallowed attempt.
     */
    fun onBook() {
        scope.launchLoggingDropExceptions {
            val item = _itemState.value ?: return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                model.bookItem(item.id)
                reloadBookingState()
            } finally {
                _loadingState.value = false
            }
        }
    }

    /**
     * Cancels the caller's own reservation of the current item, then refreshes [bookingState].
     *
     * Guarded by [loadingState]. Meaningful only when [bookingState] indicates the caller booked
     * the item; the server rejects any disallowed attempt.
     */
    fun onCancelBooking() {
        scope.launchLoggingDropExceptions {
            val item = _itemState.value ?: return@launchLoggingDropExceptions
            _loadingState.value = true
            try {
                model.cancelBooking(item.id)
                reloadBookingState()
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Delegates to [WishlistItemViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }

    /** Delegates to [WishlistItemViewInteractor.onEditItem]. Only meaningful when [isOwnerState] is `true`. */
    fun onEditItem() {
        scope.launchLoggingDropExceptions { interactor.onEditItem(node) }
    }
}
