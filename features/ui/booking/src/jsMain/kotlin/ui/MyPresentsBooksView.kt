package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the Calm Studio "Reserved" section (scenario view B).
 *
 * Reached as a primary section from the sidebar (no back button), so it renders the standard
 * `.content-inner` + `.pagehead` shell over a `.grid` of `.card`s — one per gift the caller has
 * reserved, each carrying the green `.reserved-flag`. This is the caller's OWN reservation list, so it
 * exposes no other user's identity; a list owner never sees this screen and never learns who reserved
 * their items (the server only returns the caller's own bookings). Class names mirror the design skill's
 * `app.jsx` so the shell CSS styles the screen directly.
 */
class MyPresentsBooksView(
    chain: NavigationChain<ViewConfig>,
    config: MyPresentsBooksViewConfig,
) : ComposeView<MyPresentsBooksViewConfig, ViewConfig, MyPresentsBooksViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: MyPresentsBooksViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@MyPresentsBooksView)
    }

    override val title: String
        @Composable get() = BookingStrings.reservedTitle.translation()

    /**
     * One reserved-gift card: a gradient `.media` strip carrying the green reserved flag over the item
     * title and approximate price.
     *
     * @param item Reserved item to render.
     */
    @Composable
    private fun ReservedCard(item: RegisteredWishlistItem) {
        Div({ classes(CalmStudioStyleSheet.card) }) {
            Div({ classes(CalmStudioStyleSheet.media, tintClass(item.id.long)) }) {
                Span({ classes(CalmStudioStyleSheet.`reserved-flag`) }) { Text(BookingStrings.reservedFlag.translation()) }
            }
            Div({ classes(CalmStudioStyleSheet.c) }) {
                H3 { Text(item.title) }
                if (item.description.isNotBlank()) {
                    P({ classes(CalmStudioStyleSheet.desc) }) { Text(item.description) }
                }
                val price = item.approximatePrice
                if (price != null) {
                    val units = item.priceUnits.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
                    val base = "≈ $price$units"
                    val priceText = if (item.amount > 1u) "$base · ×${item.amount}" else base
                    Div({ classes(CalmStudioStyleSheet.price) }) { Text(priceText) }
                }
            }
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val presents by viewModel.presentsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 { Text(BookingStrings.reservedTitle.translation()) }
                    P({ classes(CalmStudioStyleSheet.subline) }) { Text(BookingStrings.reservedSubline.translation()) }
                }
            }

            when {
                loading -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(BookingStrings.loading.translation()) }
                presents.isEmpty() -> Div({ classes("empty") }) {
                    Div({ classes(CalmStudioStyleSheet.ic) }) { CalmIcon(CalmIcons.bookmark) }
                    H3 { Text(BookingStrings.reservedEmptyTitle.translation()) }
                    P { Text(BookingStrings.reservedEmptyBody.translation()) }
                }
                else -> Div({ classes(CalmStudioStyleSheet.grid) }) {
                    presents.forEach { item -> ReservedCard(item) }
                }
            }
        }
    }
}
