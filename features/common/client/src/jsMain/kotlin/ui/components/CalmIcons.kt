@file:OptIn(ExperimentalComposeWebSvgApi::class)

package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.svg.Circle
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Rect
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.rx
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.svg.SVGElement

/**
 * Inner SVG content of a single Calm Studio glyph: a `@Composable` that emits the shape children
 * (`<path>`, `<circle>`, `<rect>`, …) into the surrounding `<svg>` envelope built by [CalmIcon]. Replaces
 * the previous raw-markup `String` so the glyphs are described with the Compose-HTML SVG DSL instead of
 * injected HTML.
 */
typealias CalmIconContent = ContentBuilder<SVGElement>

/**
 * Inline [Lucide](https://lucide.dev)-style glyphs (2px stroke, round caps/joins, 24×24 viewBox) used
 * by the Calm Studio content screens. Each constant is the inner shape content of one icon expressed via
 * the Compose-HTML SVG DSL; [CalmIcon] wraps it in the shared `<svg>` envelope. The Calm Studio shell CSS
 * sizes/colors the `<svg>` through descendant selectors (`.btn svg`, `.linkrow svg`, …), so no per-icon
 * styling is needed here. Mirrors the icon set in the design skill's `ui_kits/calm-studio` reference.
 */
object CalmIcons {
    /** "New" / add affordance. */
    val plus: CalmIconContent = { Path("M12 5v14M5 12h14") }

    /** Share action (copies the current link). */
    val share: CalmIconContent = {
        Circle(18, 5, 3)
        Circle(6, 12, 3)
        Circle(18, 19, 3)
        Path("m8.6 13.5 6.8 4M15.4 6.5l-6.8 4")
    }

    /** Edit action. */
    val edit: CalmIconContent = { Path("M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z") }

    /** Destructive delete action. */
    val trash: CalmIconContent = {
        Path("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6")
    }

    /** External link indicator on link rows. */
    val ext: CalmIconContent = {
        Path("M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3")
    }

    /** Empty-state gift glyph (no items). */
    val gift: CalmIconContent = {
        Rect(3, 8, 18, 4, attrs = { rx(1) })
        Path("M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8M12 8S10.5 3 8 3a2.5 2.5 0 0 0 0 5M12 8s1.5-5 4-5a2.5 2.5 0 0 1 0 5")
    }

    /** Empty-state compass glyph (Discover). */
    val compass: CalmIconContent = {
        Circle(12, 12, 9)
        Path("m15.5 8.5-2 5-5 2 2-5z")
    }

    /** Reservation / bookmark glyph (Reserved section, empty state). */
    val bookmark: CalmIconContent = { Path("M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z") }

    /** Success checkmark (toast confirmations). */
    val check: CalmIconContent = { Path("M20 6 9 17l-5-5") }

    /** "My Lists" / home navigation glyph. */
    val home: CalmIconContent = { Path("M3 10.5 12 4l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z") }

    /** Settings navigation glyph (gear). */
    val settings: CalmIconContent = {
        Circle(12, 12, 3)
        Path("M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 0 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 0 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z")
    }

    /** Magnifying-glass search glyph (top bar). */
    val search: CalmIconContent = {
        Circle(11, 11, 7)
        Path("m20 20-3-3")
    }

    /** Generic list glyph. */
    val list: CalmIconContent = { Rect(4, 4, 16, 16, attrs = { rx(3) }) }

    /** Back-navigation arrow. */
    val back: CalmIconContent = { Path("M19 12H5M12 19l-7-7 7-7") }

    /** User / profile glyph. */
    val user: CalmIconContent = {
        Circle(12, 8, 4)
        Path("M4 21a8 8 0 0 1 16 0")
    }

    /** Lock glyph (signed-out / private states). */
    val lock: CalmIconContent = {
        Rect(5, 11, 14, 10, attrs = { rx(2) })
        Path("M8 11V7a4 4 0 0 1 8 0v4")
    }
}

/**
 * Renders a single Calm Studio glyph by emitting the shared `<svg>` envelope — `24×24` viewBox, no fill,
 * `currentColor` stroke with round caps/joins — and invoking [inner] to add the glyph's shapes, all hosted
 * in a `.icon` span. Built entirely with the Compose-HTML SVG DSL (no raw HTML injection); the `.icon`
 * class makes the span an inline-flex box so the glyph stays vertically centred against adjacent text. The
 * Calm Studio shell CSS sizes/colors the `<svg>` through descendant selectors (`.btn svg`, `.linkrow svg`,
 * …).
 *
 * @param inner Inner SVG shape content, e.g. one of the [CalmIcons] constants.
 * @param attrs Extra attribute builder applied to the host span (added classes, sizing, data-attrs);
 * applied last so callers can extend or override the defaults.
 */
@Composable
fun CalmIcon(inner: CalmIconContent, attrs: AttrBuilderContext<HTMLSpanElement> = {}) {
    Span(attrs = {
        classes(CalmStudioStyleSheet.icon)
        attrs()
    }) {
        Svg(viewBox = "0 0 24 24", attrs = {
            attr("fill", "none")
            attr("stroke", "currentColor")
            attr("stroke-width", "2")
            attr("stroke-linecap", "round")
            attr("stroke-linejoin", "round")
        }) {
            inner()
        }
    }
}

/**
 * Deterministic tint class (`t0`..`t7`) keyed by [seed] so a given list/item/user keeps the same
 * cover/media/avatar color across renders. The `t0`..`t7` gradients are defined in the Calm Studio
 * shell CSS.
 *
 * @param seed Stable numeric id (wishlist, item, or user).
 * @return One of the `t0`..`t7` media-tint classes.
 */
fun tintClass(seed: Long): String = when (((seed % 8 + 8) % 8).toInt()) {
    0 -> CalmStudioStyleSheet.t0
    1 -> CalmStudioStyleSheet.t1
    2 -> CalmStudioStyleSheet.t2
    3 -> CalmStudioStyleSheet.t3
    4 -> CalmStudioStyleSheet.t4
    5 -> CalmStudioStyleSheet.t5
    6 -> CalmStudioStyleSheet.t6
    else -> CalmStudioStyleSheet.t7
}
