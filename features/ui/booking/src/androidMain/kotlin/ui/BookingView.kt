package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Android Compose-Material3 compact view for gift booking (scenario view A).
 *
 * Embedded inline inside the wishlist item screen: renders a single book/cancel control (or a short
 * status text) in one [Row]. Shows nothing when [BookingViewModel.bookingState] is `null` (owner /
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
        val resources = LocalResources.current
        val booking by viewModel.bookingState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        booking?.let { state ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    BookingState.BookedByMe -> {
                        Text(BookingStrings.bookedByYou.translation(resources), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.onCancelBooking() }, enabled = !loading) {
                            Text(BookingStrings.cancelBookingButton.translation(resources))
                        }
                    }
                    BookingState.Booked -> {
                        Text(BookingStrings.bookedByOther.translation(resources), style = MaterialTheme.typography.bodyMedium)
                    }
                    BookingState.Free -> {
                        Button(onClick = { viewModel.onBook() }, enabled = !loading) {
                            Text(BookingStrings.bookButton.translation(resources))
                        }
                    }
                }
            }
        }
    }
}
