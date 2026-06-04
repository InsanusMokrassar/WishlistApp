package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML compact view for gift booking (scenario view A). Uses Bootstrap classes.
 *
 * Embedded inline inside the wishlist item screen: renders a single book/cancel control (or a short
 * status text) in one flex row. Shows nothing when [BookingViewModel.bookingState] is `null` (owner /
 * anonymous — server hides the state).
 */
class BookingView(
    chain: NavigationChain<ViewConfig>,
    config: BookingViewConfig,
) : ComposeView<BookingViewConfig, ViewConfig, BookingViewModel>(config, chain) {
    override val viewModel: BookingViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@BookingView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val booking by viewModel.bookingState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        booking?.let { state ->
            Div({ classes("d-flex", "align-items-center", "gap-2") }) {
                when (state) {
                    BookingState.BookedByMe -> {
                        Span({ classes("text-success") }) { Text(BookingStrings.bookedByYou.translation()) }
                        Button({
                            classes("btn", "btn-outline-danger", "btn-sm")
                            if (loading) disabled()
                            onClick { viewModel.onCancelBooking() }
                        }) {
                            Text(BookingStrings.cancelBookingButton.translation())
                        }
                    }
                    BookingState.Booked -> {
                        Span({ classes("text-warning") }) { Text(BookingStrings.bookedByOther.translation()) }
                    }
                    BookingState.Free -> {
                        Button({
                            classes("btn", "btn-primary", "btn-sm")
                            if (loading) disabled()
                            onClick { viewModel.onBook() }
                        }) {
                            Text(BookingStrings.bookButton.translation())
                        }
                    }
                }
            }
        }
    }
}
