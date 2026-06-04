package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for scenario view A — the gift-booking view.
 *
 * Loads the booking state of [BookingViewConfig.itemId] visible to the caller and drives the
 * book / cancel actions. When [bookingState] is `null` (caller is owner or anonymous — server
 * enforced) the view renders nothing, so the view itself self-gates booking availability.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Booking data source.
 * @param interactor Navigation delegate for this screen.
 */
class BookingViewModel(
    private val node: NavigationNode<BookingViewConfig, ViewConfig>,
    private val model: BookingModel,
    private val interactor: BookingViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _bookingState = MutableRedeliverStateFlow<BookingState?>(null)

    /**
     * Booking state visible to the caller, or `null` when hidden (owner / anonymous). The view
     * shows booking controls only when this is non-null.
     */
    val bookingState = _bookingState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while a booking network request is in flight. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            reloadBookingState()
        }
    }

    /**
     * Reloads [bookingState] for the configured item.
     *
     * Reflects the latest server-side state (including the `null`/hidden result for owners and
     * anonymous callers).
     */
    private suspend fun reloadBookingState() {
        _loadingState.value = true
        try {
            _bookingState.value = model.getBookingState(node.config.itemId)
        } finally {
            _loadingState.value = false
        }
    }

    /**
     * Reserves the configured item for gifting on behalf of the caller, then refreshes [bookingState].
     *
     * Guarded by [loadingState]. The server rejects any disallowed attempt.
     */
    fun onBook() {
        scope.launchLoggingDropExceptions {
            _loadingState.value = true
            try {
                model.bookItem(node.config.itemId)
            } finally {
                _loadingState.value = false
            }
            reloadBookingState()
        }
    }

    /**
     * Cancels the caller's own reservation of the configured item, then refreshes [bookingState].
     *
     * Guarded by [loadingState]. The server rejects any disallowed attempt.
     */
    fun onCancelBooking() {
        scope.launchLoggingDropExceptions {
            _loadingState.value = true
            try {
                model.cancelBooking(node.config.itemId)
            } finally {
                _loadingState.value = false
            }
            reloadBookingState()
        }
    }

    /** Delegates to [BookingViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }
}
