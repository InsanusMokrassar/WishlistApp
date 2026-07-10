package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.wishlist.features.currency.common.utils.formatItemPriceWithAmount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem

/**
 * Material card presenting a single wishlist [item].
 *
 * Mapping: media = first attached image (only when present); title = item title; subtitle = the
 * wishlist the item belongs to ([wishlistTitle], when non-null); content = description (when not
 * blank); footer = price + quantity (see [formatItemPriceWithAmount]). The item priority is rendered
 * as a [PriorityBadge] overlaid in the top-right corner of the card. When the item's `amount` exceeds
 * `1`, the footer shows `<price>x<amount> (<whole>)`; with no price it shows `×<amount>`; for
 * `amount == 1` the plain price is shown (and nothing when no price is set).
 *
 * @param item Item to display.
 * @param wishlistTitle Title of the wishlist [item] belongs to, shown as the card subtitle; `null`
 * hides the subtitle line.
 * @param loadImageBytes Suspending loader for the encoded bytes of the item's image.
 * @param onSelect Invoked when the user clicks the card.
 */
@Composable
fun WishlistItemCard(
    item: WishlistsFeatureItem,
    wishlistTitle: String?,
    loadImageBytes: suspend (FileId) -> ByteArray?,
    onSelect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().clickable { onSelect() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val firstImage = item.imageIds.firstOrNull()
                if (firstImage != null) {
                    RemoteImage(
                        key = firstImage.string,
                        loader = { loadImageBytes(firstImage) },
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                } else {
                    WishlistItemImagePlaceholder(
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(item.title, style = MaterialTheme.typography.subtitle1)
                    if (wishlistTitle != null) {
                        Text(
                            wishlistTitle,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (item.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(item.description, style = MaterialTheme.typography.body2)
                    }
                    val priceText = formatItemPriceWithAmount(item.approximatePrice, item.priceUnits, item.amount, null, null)
                    if (priceText.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(priceText, style = MaterialTheme.typography.caption)
                    }
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                PriorityBadge(item.priority)
            }
        }
    }
}
