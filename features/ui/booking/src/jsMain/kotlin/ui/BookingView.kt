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
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML compact view for gift reservation (scenario view A), rendered in Calm Studio markup.
 *
 * Embedded inline inside the wishlist item screen's `.actbar`, so it emits its `.btn` / `.pill` controls
 * as direct flex siblings (no wrapper). Shows nothing when [BookingViewModel.bookingState] is `null`
 * (owner / anonymous — the server hides the state, so a list owner never learns an item is reserved
 * through this control, and never who reserved it). States:
 * - [BookingState.Free] → primary "Reserve this gift" button.
 * - [BookingState.BookedByMe] → "Reserved by you" pill + "Cancel reservation" button.
 * - [BookingState.Booked] → "Reserved by someone" pill only (the booker's identity is never exposed).
 */
class BookingView(
    chain: NavigationChain<ViewConfig>,
    config: BookingViewConfig,
) : ComposeView<BookingViewConfig, ViewConfig, BookingViewModel>(config, chain) {
    override val viewModel: BookingViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@BookingView)
    }

    /**
     * A green "reserved" status pill mirroring the design skill's reserved indicator.
     *
     * @param label Visible pill text.
     */
    @Composable
    private fun ReservedPill(label: String) {
        Span({
            classes("pill")
            style {
                property("background", "var(--cs-ok-soft)")
                property("color", "var(--cs-ok)")
            }
        }) {
            Span({
                classes("dot")
                style { property("background", "var(--cs-ok)") }
            })
            Text(label)
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val booking by viewModel.bookingState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        booking?.let { state ->
            when (state) {
                BookingState.BookedByMe -> {
                    ReservedPill(BookingStrings.reservedByYouLabel.translation())
                    Button({
                        classes("btn")
                        if (loading) disabled()
                        onClick { viewModel.onCancelBooking() }
                    }) {
                        Text(BookingStrings.cancelReservationButton.translation())
                    }
                }
                BookingState.Booked -> {
                    ReservedPill(BookingStrings.reservedBySomeoneLabel.translation())
                }
                BookingState.Free -> {
                    Button({
                        classes("btn", "primary")
                        if (loading) disabled()
                        onClick { viewModel.onBook() }
                    }) {
                        Text(BookingStrings.reserveGiftButton.translation())
                    }
                }
            }
        }
    }
}
