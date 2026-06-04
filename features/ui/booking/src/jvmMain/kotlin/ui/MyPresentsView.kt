package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JVM (Desktop) Compose-Material view for the my-presents screen (scenario view B).
 *
 * Lists every item the caller has booked. Per issue #29 point #6 nothing navigates here yet.
 */
class MyPresentsView(
    chain: NavigationChain<ViewConfig>,
    config: MyPresentsViewConfig,
) : ComposeView<MyPresentsViewConfig, ViewConfig, MyPresentsViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: MyPresentsViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@MyPresentsView)
    }

    override val title: String
        @Composable get() = BookingStrings.myPresentsTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val presents by viewModel.presentsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackButton(BookingStrings.backButton.translation()) { viewModel.onBack() }
            ScreenTitle(BookingStrings.myPresentsTitle.translation())

            when {
                loading -> Text(BookingStrings.loading.translation(), style = MaterialTheme.typography.body2)
                presents.isEmpty() -> Text(BookingStrings.emptyPresents.translation(), style = MaterialTheme.typography.caption)
                else -> presents.forEach { item ->
                    Text(item.title, style = MaterialTheme.typography.body1)
                }
            }
        }
    }
}
