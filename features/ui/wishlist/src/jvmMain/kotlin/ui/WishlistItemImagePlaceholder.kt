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
        val w = size.width
        val h = size.height
        drawRect(color = GiftPlaceholderBackground, size = size)
        // Box body.
        drawRect(
            color = GiftBoxBody,
            topLeft = Offset(w * 0.24f, h * 0.44f),
            size = Size(w * 0.52f, h * 0.38f)
        )
        // Lid.
        drawRect(
            color = GiftBoxLid,
            topLeft = Offset(w * 0.20f, h * 0.36f),
            size = Size(w * 0.60f, h * 0.14f)
        )
        // Vertical ribbon.
        drawRect(
            color = GiftBoxRibbon,
            topLeft = Offset(w * 0.46f, h * 0.36f),
            size = Size(w * 0.08f, h * 0.46f)
        )
        // Bow (two rounded loops meeting at the ribbon top), mirroring the JS bezier shape.
        val bow = Path().apply {
            moveTo(w * 0.50f, h * 0.36f)
            cubicTo(w * 0.40f, h * 0.24f, w * 0.26f, h * 0.30f, w * 0.50f, h * 0.36f)
            cubicTo(w * 0.74f, h * 0.30f, w * 0.60f, h * 0.24f, w * 0.50f, h * 0.36f)
            close()
        }
        drawPath(path = bow, color = GiftBoxRibbon)
    }
}
