package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmPill
import dev.inmo.wishlist.features.common.client.ui.components.Toaster
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML compact view for gift reservation (scenario view A), rendered in Calm Studio markup.
 *
 * Embedded inline inside the wishlist item screen's action bar, so it emits its [CalmButton] / [CalmPill]
 * controls as direct flex siblings (no wrapper). Shows nothing when [BookingViewModel.bookingState] is
 * `null` (owner / anonymous — the server hides the state, so a list owner never learns an item is
 * reserved through this control, and never who reserved it). States:
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
     * A green "reserved" status pill mirroring the design skill's reserved indicator, rendered through
     * the shared [CalmPill] with the `--cs-ok` success tokens.
     *
     * @param label Visible pill text.
     */
    @Composable
    private fun ReservedPill(label: String) {
        CalmPill(
            text = label,
            dotClass = CalmStudioStyleSheet.`dot-ok`,
            pillClass = CalmStudioStyleSheet.`pill-ok`,
        )
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
                    CalmButton(
                        text = BookingStrings.cancelReservationButton.translation(),
                        onClick = {
                            viewModel.onCancelBooking()
                            Toaster.show(BookingStrings.cancelReservationToast.translation())
                        },
                        disabled = loading,
                    )
                }
                BookingState.Booked -> {
                    ReservedPill(BookingStrings.reservedBySomeoneLabel.translation())
                }
                BookingState.Free -> {
                    CalmButton(
                        text = BookingStrings.reserveGiftButton.translation(),
                        onClick = {
                            viewModel.onBook()
                            Toaster.show(BookingStrings.reserveToast.translation())
                        },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading,
                    )
                }
            }
        }
    }
}
