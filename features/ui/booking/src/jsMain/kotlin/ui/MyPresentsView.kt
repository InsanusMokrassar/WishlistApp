package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the my-presents screen (scenario view B). Uses Bootstrap classes.
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

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(BookingStrings.backButton.translation()) { viewModel.onBack() }
            }
            ScreenTitle(BookingStrings.myPresentsTitle.translation())

            when {
                loading -> P { Text(BookingStrings.loading.translation()) }
                presents.isEmpty() -> P({ classes("text-muted") }) { Text(BookingStrings.emptyPresents.translation()) }
                else -> Ul({ classes("list-group") }) {
                    presents.forEach { item ->
                        ListRow(text = item.title)
                    }
                }
            }
        }
    }
}
