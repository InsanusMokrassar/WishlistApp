package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Neutral background fill of the wishlist (stacked-items) placeholder. */
private val WishlistPlaceholderBackground = Color(0xFFEEF1F4)

/** Fill colours of the three stacked cards, from the rearmost (faintest) to the frontmost. */
private val WishlistStackColors = listOf(
    Color(0xFFC7CDD6),
    Color(0xFFAEB6C1),
    Color(0xFF8B94A1)
)

/**
 * Renders the default wishlist placeholder shown for a wishlist (which never carries its own image):
 * three small item cards stacked behind each other, each offset by a few units in x and y so the
 * stack reads as "several wishlist items, one behind another". Drawn with Compose [Canvas].
 *
 * @param modifier Layout modifier supplying size and any clipping; mirrors `RemoteImage` call shape.
 * @param contentDescription Already-translated accessibility text, or `null` when decorative.
 */
@Composable
fun WishlistImagePlaceholder(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription == null) Modifier
            else Modifier.semantics { this.contentDescription = contentDescription }
        )
    ) {
        val w = size.width
        val h = size.height
        drawRect(color = WishlistPlaceholderBackground, size = size)
        val cardSize = Size(w * 0.44f, h * 0.44f)
        val corner = CornerRadius(w * 0.05f, h * 0.05f)
        // Each card is shifted further down-right than the one behind it.
        val offsets = listOf(
            Offset(w * 0.34f, h * 0.22f),
            Offset(w * 0.27f, h * 0.29f),
            Offset(w * 0.20f, h * 0.36f)
        )
        offsets.forEachIndexed { index, topLeft ->
            drawRoundRect(
                color = WishlistStackColors[index],
                topLeft = topLeft,
                size = cardSize,
                cornerRadius = corner
            )
        }
    }
}
