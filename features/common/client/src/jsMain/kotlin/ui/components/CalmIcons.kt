package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Span

/**
 * Inline [Lucide](https://lucide.dev)-style glyphs (2px stroke, round caps/joins, 24×24 viewBox) used
 * by the Calm Studio content screens. Each constant is the inner markup of one icon; [CalmIcon] wraps
 * it in the shared `<svg>` envelope and injects it as raw DOM. The Calm Studio shell CSS sizes/colors
 * the `<svg>` through descendant selectors (`.btn svg`, `.linkrow svg`, …), so no per-icon styling is
 * needed here. Mirrors the icon set in the design skill's `ui_kits/calm-studio` reference.
 */
object CalmIcons {
    /** "New" / add affordance. */
    val plus = """<path d="M12 5v14M5 12h14"/>"""

    /** Share action (copies the current link). */
    val share = """<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4"/>"""

    /** Edit action. */
    val edit = """<path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>"""

    /** Destructive delete action. */
    val trash = """<path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>"""

    /** External link indicator on link rows. */
    val ext = """<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3"/>"""

    /** Empty-state gift glyph (no items). */
    val gift = """<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8M12 8S10.5 3 8 3a2.5 2.5 0 0 0 0 5M12 8s1.5-5 4-5a2.5 2.5 0 0 1 0 5"/>"""

    /** Empty-state compass glyph (Discover). */
    val compass = """<circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2 5-5 2 2-5z"/>"""
}

/**
 * Renders a single Calm Studio glyph by injecting [inner] into a shared `<svg>` envelope.
 *
 * Compose-HTML has no SVG DOM builder, so the markup is written through a `ref` once on attach. Mirrors
 * the sidebar feature's `LucideIcon`, kept here so the wishlist/users content views share one icon set
 * without depending on the sidebar module.
 *
 * @param inner Inner SVG markup, e.g. one of the [CalmIcons] constants.
 */
@Composable
fun CalmIcon(inner: String) {
    Span(attrs = {
        ref { element ->
            element.innerHTML =
                """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">$inner</svg>"""
            onDispose { }
        }
    })
}

/**
 * Deterministic tint class (`t0`..`t7`) keyed by [seed] so a given list/item/user keeps the same
 * cover/media/avatar color across renders. The `t0`..`t7` gradients are defined in the Calm Studio
 * shell CSS.
 *
 * @param seed Stable numeric id (wishlist, item, or user).
 * @return One of the `t0`..`t7` media-tint classes.
 */
fun tintClass(seed: Long): String = "t${((seed % 8 + 8) % 8)}"
