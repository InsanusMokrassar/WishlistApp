package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Android Compose-Material3 view for the gift-booking screen (scenario view A).
 *
 * Renders booking controls only when [BookingViewModel.bookingState] is non-null.
 */
class BookingView(
    chain: NavigationChain<ViewConfig>,
    config: BookingViewConfig,
) : ComposeView<BookingViewConfig, ViewConfig, BookingViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: BookingViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@BookingView)
    }

    override val title: String
        @Composable get() = BookingStrings.bookingLabel.translation(LocalResources.current)

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val booking by viewModel.bookingState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackButton(BookingStrings.backButton.translation(resources)) { viewModel.onBack() }

            Text(BookingStrings.bookingLabel.translation(resources), style = MaterialTheme.typography.titleSmall)

            booking?.let { state ->
                when {
                    state.bookedByMe -> {
                        Text(BookingStrings.bookedByYou.translation(resources), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.onCancelBooking() }, enabled = !loading) {
                            Text(BookingStrings.cancelBookingButton.translation(resources))
                        }
                    }
                    state.booked -> {
                        Text(BookingStrings.bookedByOther.translation(resources), style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        Text(BookingStrings.notBooked.translation(resources), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.onBook() }, enabled = !loading) {
                            Text(BookingStrings.bookButton.translation(resources))
                        }
                    }
                }
            }
        }
    }
}
