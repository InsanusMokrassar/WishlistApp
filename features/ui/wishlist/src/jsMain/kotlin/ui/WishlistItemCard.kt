package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Dedicated stylesheet for [WishlistItemCard] holding the custom CSS that Bootstrap utilities cannot
 * express (fixed media height + object-fit cover, clickable cursor).
 */
object WishlistItemCardStylesheet : StyleSheet() {
    /** Applied to the card root to signal the whole card is clickable. */
    val clickable by style {
        cursor("pointer")
    }

    /** Applied to the card media image: fixed height, full width, cropped to cover. */
    val media by style {
        width(100.percent)
        height(180.px)
        property("object-fit", "cover")
    }
}

/**
 * Bootstrap card presenting a single wishlist [item].
 *
 * Mapping: media = first attached image (only when present); title = item title; subtitle = the
 * wishlist the item belongs to ([wishlistTitle], when non-null); content = description (when not
 * blank); footer = price + currency/units (when a price is set). The item [priority] is rendered as a
 * [PriorityBadge] overlaid in the top-right corner of the card.
 *
 * @param item Item to display.
 * @param wishlistTitle Title of the wishlist [item] belongs to, shown as the card subtitle; `null`
 * hides the subtitle line.
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
    Style(WishlistItemCardStylesheet)
    Div({
        classes("card", "h-100", "position-relative", WishlistItemCardStylesheet.clickable)
        onClick { onSelect() }
    }) {
        Div({ classes("position-absolute", "top-0", "end-0", "m-2") }) {
            PriorityBadge(item.priority)
        }

        val firstImage = item.imageIds.firstOrNull()
        if (firstImage != null) {
            Img(src = imageUrl(firstImage), alt = "") {
                classes("card-img-top", WishlistItemCardStylesheet.media)
            }
        }

        Div({ classes("card-body") }) {
            H5({ classes("card-title") }) { Text(item.title) }
            if (wishlistTitle != null) {
                H6({ classes("card-subtitle", "mb-2", "text-muted") }) { Text(wishlistTitle) }
            }
            if (item.description.isNotBlank()) {
                P({ classes("card-text") }) { Text(item.description) }
            }
        }

        item.approximatePrice?.let { price ->
            Div({ classes("card-footer", "text-muted", "small") }) {
                Text("$price ${item.priceUnits}")
            }
        }
    }
}
