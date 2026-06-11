package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.onResumeFlow
import dev.inmo.navigation.mvvm.ViewModel
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/**
 * ViewModel for scenario view B — the list of all presents the caller plans to make.
 *
 * Loads every item the authenticated caller has booked. Per issue #29 point #6 nothing navigates
 * here yet; the screen is fully implemented but unreachable.
 *
 * @param node Navigation node this ViewModel is bound to.
 * @param model Booking data source.
 * @param interactor Navigation delegate for this screen.
 */
class MyPresentsBooksViewModel(
    private val node: NavigationNode<MyPresentsBooksViewConfig, ViewConfig>,
    private val model: BookingModel,
    private val interactor: MyPresentsBooksViewInteractor
) : ViewModel<ViewConfig>(node) {
    private val _presentsState = MutableRedeliverStateFlow<List<RegisteredWishlistItem>>(emptyList())

    /** Items the caller has booked; empty while loading or when nothing booked. */
    val presentsState = _presentsState.asStateFlow()

    private val _loadingState = MutableRedeliverStateFlow(false)

    /** `true` while the presents list is being loaded. */
    val loadingState = _loadingState.asStateFlow()

    init {
        merge(flowOf(Unit), node.onResumeFlow).subscribeLoggingDropExceptions(scope) {
            _loadingState.value = true
            try {
                _presentsState.value = model.myPresentsBooks()
            } finally {
                _loadingState.value = false
            }
        }
    }

    /** Delegates to [MyPresentsBooksViewInteractor.onBack]. */
    fun onBack() {
        scope.launchLoggingDropExceptions { interactor.onBack(node) }
    }
}
