package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Img

/**
 * Inline SVG markup (URL-encoded, ready for a `data:` URI) of the default user silhouette: a neutral
 * gray field with a lighter head circle and shoulders arc. Drawn on a 100x100 viewBox so it scales to
 * any requested pixel size.
 */
private val userSilhouetteSvg: String =
    (
        "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
            "<rect width='100' height='100' fill='%23d0d3d8'/>" +
            "<circle cx='50' cy='38' r='18' fill='%23f1f3f5'/>" +
            "<path d='M20 86 a30 30 0 0 1 60 0 Z' fill='%23f1f3f5'/>" +
            "</svg>"
        )

/** `data:` URI of [userSilhouetteSvg] usable directly as an `<img src>`. */
private val userSilhouetteDataUri: String = "data:image/svg+xml;charset=UTF-8,$userSilhouetteSvg"

/**
 * Renders the default gray user-avatar placeholder shown whenever a user has no uploaded photo.
 *
 * @param sizePx Width and height of the rendered square in CSS pixels.
 * @param circle When `true` the image is clipped to a circle (list thumbnails); otherwise it keeps
 * rounded corners (large profile previews).
 * @param alt Already-translated accessibility text for the image.
 */
@Composable
fun UserAvatarPlaceholder(sizePx: Int, circle: Boolean, alt: String) {
    Img(src = userSilhouetteDataUri, alt = alt) {
        classes(if (circle) "rounded-circle" else "rounded", "flex-shrink-0")
        attr("width", sizePx.toString())
        attr("height", sizePx.toString())
        attr("style", "object-fit: cover;")
    }
}
