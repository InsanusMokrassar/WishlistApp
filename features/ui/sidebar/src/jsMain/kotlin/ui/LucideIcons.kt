package dev.inmo.wishlist.features.ui.sidebar.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Span

/**
 * Inline [Lucide](https://lucide.dev) glyphs used by the sidebar navigation.
 *
 * Each constant is the inner markup of a 24×24, 2px-stroke icon; [LucideIcon] wraps it in the shared
 * `<svg>` envelope and injects it as raw DOM. The Calm Studio shell CSS sizes/colors the `<svg>` via
 * descendant selectors (`.navitem svg`, `.logo .mk svg`, …), so no per-icon styling is needed here.
 */
object LucideIcons {
    /** Brand glyph shown in the sidebar word-mark. */
    val gift = """<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8M12 8S10.5 3 8 3a2.5 2.5 0 0 0 0 5M12 8s1.5-5 4-5a2.5 2.5 0 0 1 0 5"/>"""

    /** "My Lists" item. */
    val home = """<path d="M3 10.5 12 4l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>"""

    /** "Discover" item. */
    val compass = """<circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2 5-5 2 2-5z"/>"""

    /** "Reserved" item. */
    val bookmark = """<path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/>"""

    /** "Settings" item. */
    val settings = """<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 0 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 0 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/>"""

    /** "New list" affordance. */
    val plus = """<path d="M12 5v14M5 12h14"/>"""

    /** Profile / login glyph. */
    val user = """<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>"""
}

/**
 * Renders a single Lucide glyph by injecting [inner] into a shared `<svg>` envelope.
 *
 * Compose-HTML has no SVG DOM builder, so the markup is written through a `ref` once on attach.
 *
 * @param inner Inner SVG markup, e.g. one of the [LucideIcons] constants.
 */
@Composable
fun LucideIcon(inner: String) {
    Span(attrs = {
        ref { element ->
            element.innerHTML =
                """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">$inner</svg>"""
            onDispose { }
        }
    })
}
