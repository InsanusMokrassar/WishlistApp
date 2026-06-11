package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Android Compose-Material3 view for the my-presents screen (scenario view B).
 *
 * Lists every item the caller has booked. Per issue #29 point #6 nothing navigates here yet.
 */
class MyPresentsBooksView(
    chain: NavigationChain<ViewConfig>,
    config: MyPresentsBooksViewConfig,
) : ComposeView<MyPresentsBooksViewConfig, ViewConfig, MyPresentsBooksViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: MyPresentsBooksViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@MyPresentsBooksView)
    }

    override val title: String
        @Composable get() = BookingStrings.myPresentsBooksTitle.translation(LocalResources.current)

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val presents by viewModel.presentsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackButton(BookingStrings.backButton.translation(resources)) { viewModel.onBack() }
            ScreenTitle(BookingStrings.myPresentsBooksTitle.translation(resources))

            when {
                loading -> Text(BookingStrings.loading.translation(resources), style = MaterialTheme.typography.bodyMedium)
                presents.isEmpty() -> Text(BookingStrings.emptyPresents.translation(resources), style = MaterialTheme.typography.bodySmall)
                else -> presents.forEach { item ->
                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
