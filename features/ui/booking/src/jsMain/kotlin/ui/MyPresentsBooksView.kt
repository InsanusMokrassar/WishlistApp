package dev.inmo.wishlist.features.ui.booking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.EmptyState
import dev.inmo.wishlist.features.common.client.ui.components.ItemCard
import dev.inmo.wishlist.features.common.client.ui.components.ItemGrid
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.booking.BookingStrings
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the Calm Studio "Reserved" section (scenario view B).
 *
 * Reached as a primary section from the sidebar (no back button), so it renders the standard
 * [ContentColumn] + [PageHead] shell over an [ItemGrid] of reserved-gift [ItemCard]s (each carrying the
 * green reserved flag). This is the caller's OWN reservation list, so it exposes no other user's
 * identity; a list owner never sees this screen and never learns who reserved their items (the server
 * only returns the caller's own bookings).
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
     * One reserved-gift card — an [ItemCard] with the green reserved flag over the item title and
     * approximate price.
     *
     * @param item Reserved item to render.
     */
    @Composable
    private fun ReservedCard(item: BookingFeatureItem) {
        val price = item.approximatePrice
        val priceText = if (price != null) {
            val units = item.priceUnits.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
            val base = "≈ $price$units"
            if (item.amount > 1u) "$base · ×${item.amount}" else base
        } else {
            null
        }
        ItemCard(
            title = item.title,
            tintClass = tintClass(item.id.long),
            description = item.description.takeIf { it.isNotBlank() },
            priceText = priceText,
            reservedFlag = BookingStrings.reservedFlag.translation(),
        )
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val presents by viewModel.presentsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        ContentColumn {
            PageHead(
                title = BookingStrings.reservedTitle.translation(),
                subline = BookingStrings.reservedSubline.translation(),
            )

            when {
                loading -> Subline(BookingStrings.loading.translation())
                presents.isEmpty() -> EmptyState(
                    icon = CalmIcons.bookmark,
                    title = BookingStrings.reservedEmptyTitle.translation(),
                    text = BookingStrings.reservedEmptyBody.translation(),
                )
                else -> ItemGrid {
                    presents.forEach { item -> ReservedCard(item) }
                }
            }
        }
    }
}
