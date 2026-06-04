package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the gift-booking screen (scenario view A). Uses Bootstrap classes.
 *
 * Renders booking controls only when [BookingViewModel.bookingState] is non-null; for the item
 * owner and anonymous callers the server returns no state and the screen stays empty.
 */
class BookingView(
    chain: NavigationChain<ViewConfig>,
    config: BookingViewConfig,
) : ComposeView<BookingViewConfig, ViewConfig, BookingViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: BookingViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@BookingView)
    }

    override val title: String
        @Composable get() = BookingStrings.bookingLabel.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val booking by viewModel.bookingState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(BookingStrings.backButton.translation()) { viewModel.onBack() }
            }

            H6({ classes("text-muted") }) { Text(BookingStrings.bookingLabel.translation()) }

            booking?.let { state ->
                when {
                    state.bookedByMe -> {
                        P({ classes("text-success") }) { Text(BookingStrings.bookedByYou.translation()) }
                        Button({
                            classes("btn", "btn-outline-danger")
                            if (loading) disabled()
                            onClick { viewModel.onCancelBooking() }
                        }) {
                            Text(BookingStrings.cancelBookingButton.translation())
                        }
                    }
                    state.booked -> {
                        P({ classes("text-warning") }) { Text(BookingStrings.bookedByOther.translation()) }
                    }
                    else -> {
                        P({ classes("text-muted") }) { Text(BookingStrings.notBooked.translation()) }
                        Button({
                            classes("btn", "btn-primary")
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
