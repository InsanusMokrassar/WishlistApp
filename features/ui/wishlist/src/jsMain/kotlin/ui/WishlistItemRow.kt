package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.currency.common.utils.formatItemPriceWithAmount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio list row (`.row`) for a single wishlist [item], used by the list (rows) presentation of
 * the detail and all-items screens. Mirrors the design skill's `ItemRow` reference: a leading `.thumb`
 * (the item's first image cropped to cover, or a deterministic gradient tint), a `.rmain` block with
 * the title, its priority pill and an optional description, and a trailing `.rprice`.
 *
 * @param item Item to display.
 * @param secondaryTitle Optional source-list title appended after the item title in brackets (custom
 * sort across lists); `null` shows the title alone.
 * @param selectedCurrency Shared conversion target, or `null` for original prices.
 * @param rates Latest rates snapshot used to convert the price, or `null` when unavailable.
 * @param imageUrl Resolver turning a [FileId] into a fetchable image URL.
 * @param onSelect Invoked when the user clicks the row.
 */
@Composable
fun WishlistItemRow(
    item: RegisteredWishlistItem,
    secondaryTitle: String?,
    selectedCurrency: CurrencyCode?,
    rates: CurrencyRates?,
    imageUrl: (FileId) -> String,
    onSelect: () -> Unit,
) {
    Div({
        classes("row")
        onClick { onSelect() }
    }) {
        val firstImage = item.imageIds.firstOrNull()
        if (firstImage != null) {
            Img(src = imageUrl(firstImage), alt = "") {
                classes("thumb")
                style { property("object-fit", "cover") }
            }
        } else {
            Span({ classes("thumb", tintClass(item.id.long)) })
        }
        Div({ classes("rmain") }) {
            Div({
                style {
                    property("display", "flex")
                    property("align-items", "center")
                    property("gap", "9px")
                }
            }) {
                H3 { Text(secondaryTitle?.let { "${item.title} ($it)" } ?: item.title) }
                PriorityBadge(item.priority)
            }
            if (item.description.isNotBlank()) {
                P({ classes("desc") }) { Text(item.description) }
            }
        }
        val priceText = formatItemPriceWithAmount(item.approximatePrice, item.priceUnits, item.amount, selectedCurrency, rates)
        if (priceText.isNotEmpty()) {
            Span({ classes("rprice") }) { Text(priceText) }
        }
    }
}
