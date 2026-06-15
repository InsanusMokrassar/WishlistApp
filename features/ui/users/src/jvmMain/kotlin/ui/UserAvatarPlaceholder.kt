package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Neutral background fill of the user-avatar placeholder. */
private val UserPlaceholderBackground = Color(0xFFD0D3D8)

/** Lighter foreground fill used for the head and shoulders of the silhouette. */
private val UserPlaceholderForeground = Color(0xFFF1F3F5)

/**
 * Renders the default user-avatar placeholder shown whenever a user has no uploaded photo: a neutral
 * gray field with a lighter head circle and shoulders arc, drawn with Compose [Canvas] so it scales
 * to whatever size the [modifier] enforces.
 *
 * @param modifier Layout modifier; the caller supplies size and any clipping (e.g. circle for list
 * thumbnails). Mirrors the call shape of `RemoteImage` so it drops into the same slots.
 * @param contentDescription Already-translated accessibility text, or `null` when decorative.
 */
@Composable
fun UserAvatarPlaceholder(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription == null) Modifier
            else Modifier.semantics { this.contentDescription = contentDescription }
        )
    ) {
        val w = size.width
        val h = size.height
        drawRect(color = UserPlaceholderBackground, size = size)
        val headRadius = w * 0.18f
        drawCircle(
            color = UserPlaceholderForeground,
            radius = headRadius,
            center = Offset(w / 2f, h * 0.38f)
        )
        val shoulderTop = h * 0.62f
        val shoulderHalfWidth = w * 0.32f
        val shoulders = Path().apply {
            moveTo(w / 2f - shoulderHalfWidth, h)
            cubicTo(
                w / 2f - shoulderHalfWidth, shoulderTop,
                w / 2f + shoulderHalfWidth, shoulderTop,
                w / 2f + shoulderHalfWidth, h
            )
            close()
        }
        drawPath(path = shoulders, color = UserPlaceholderForeground)
    }
}
