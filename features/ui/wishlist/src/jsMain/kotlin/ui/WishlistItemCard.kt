package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.currency.common.utils.formatItemPriceWithAmount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio item card (`.card`) presenting a single wishlist [item].
 *
 * Layout mirrors the design skill's `ItemCard` reference: a fixed-height `.media` strip (the item's
 * first image cropped to cover, or a deterministic gradient tint when the item has no image) carrying
 * the priority `.badge` in the top-right corner, over a `.c` body holding the title, an optional
 * description / source-list line, and the price. The whole card is clickable.
 *
 * @param item Item to display.
 * @param wishlistTitle Title of the wishlist [item] belongs to, shown as the secondary line when the
 * item has no description (cross-list grids need it); `null` hides the line.
 * @param imageUrl Resolver turning a [FileId] into a fetchable image URL.
 * @param onSelect Invoked when the user clicks the card.
 */
@Composable
fun WishlistItemCard(
    item: RegisteredWishlistItem,
    wishlistTitle: String?,
    imageUrl: (FileId) -> String,
    onSelect: () -> Unit,
) {
    Div({
        classes("card")
        onClick { onSelect() }
    }) {
        val firstImage = item.imageIds.firstOrNull()
        Div({
            if (firstImage == null) classes("media", tintClass(item.id.long)) else classes("media")
        }) {
            if (firstImage != null) {
                Img(src = imageUrl(firstImage), alt = "") {
                    style {
                        property("width", "100%")
                        property("height", "100%")
                        property("object-fit", "cover")
                        property("display", "block")
                    }
                }
            }
            Span({ classes("badge") }) {
                Span({
                    classes("dot")
                    style { property("background", item.priority.dotColorVar()) }
                })
                Text(item.priority.labelResource().translation())
            }
        }
        Div({ classes("c") }) {
            H3 { Text(item.title) }
            val secondary = item.description.takeIf { it.isNotBlank() } ?: wishlistTitle
            if (secondary != null) {
                P({ classes("desc") }) { Text(secondary) }
            }
            val priceText = formatItemPriceWithAmount(item.approximatePrice, item.priceUnits, item.amount, null, null)
            if (priceText.isNotEmpty()) {
                Div({ classes("price") }) { Text(priceText) }
            }
        }
    }
}
