package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Neutral background fill of the gift-box placeholder. */
private val GiftPlaceholderBackground = Color(0xFFEEF1F4)

/** Body (lower box) fill of the gift. */
private val GiftBoxBody = Color(0xFFC0C6CF)

/** Lid fill of the gift. */
private val GiftBoxLid = Color(0xFFA8B0BB)

/** Ribbon / bow fill of the gift. */
private val GiftBoxRibbon = Color(0xFF8B94A1)

/**
 * Renders the default gift-box placeholder shown whenever a wishlist item has no attached image: a
 * box body, a lid, a vertical ribbon and a bow, drawn with Compose [Canvas] so it scales to whatever
 * size the [modifier] enforces.
 *
 * @param modifier Layout modifier supplying size and any clipping; mirrors `RemoteImage` call shape.
 * @param contentDescription Already-translated accessibility text, or `null` when decorative.
 */
@Composable
fun WishlistItemImagePlaceholder(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription == null) Modifier
            else Modifier.semantics { this.contentDescription = contentDescription }
        )
    ) {
        drawRect(color = GiftPlaceholderBackground, size = size)
        // Draw the gift inside a centered square so it keeps its aspect ratio (never stretches) in
        // non-square containers like the full-width card media.
        val side = minOf(size.width, size.height)
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        fun x(f: Float) = ox + side * f
        fun y(f: Float) = oy + side * f
        // Box body.
        drawRect(
            color = GiftBoxBody,
            topLeft = Offset(x(0.24f), y(0.44f)),
            size = Size(side * 0.52f, side * 0.38f)
        )
        // Lid.
        drawRect(
            color = GiftBoxLid,
            topLeft = Offset(x(0.20f), y(0.36f)),
            size = Size(side * 0.60f, side * 0.14f)
        )
        // Vertical ribbon.
        drawRect(
            color = GiftBoxRibbon,
            topLeft = Offset(x(0.46f), y(0.36f)),
            size = Size(side * 0.08f, side * 0.46f)
        )
        // Bow (two rounded loops meeting at the ribbon top), mirroring the JS bezier shape.
        val bow = Path().apply {
            moveTo(x(0.50f), y(0.36f))
            cubicTo(x(0.40f), y(0.24f), x(0.26f), y(0.30f), x(0.50f), y(0.36f))
            cubicTo(x(0.74f), y(0.30f), x(0.60f), y(0.24f), x(0.50f), y(0.36f))
            close()
        }
        drawPath(path = bow, color = GiftBoxRibbon)
    }
}
