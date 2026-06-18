package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import dev.inmo.wishlist.features.ui.wishlist.weightSuffix
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio CSS custom property holding the dot color for a [Priority].
 *
 * @return One of the `--pri-*` design tokens; custom priorities reuse the high-priority accent.
 */
fun Priority.dotColorVar(): String = when (this) {
    Priority.Small -> "var(--pri-low)"
    Priority.Medium -> "var(--pri-med)"
    Priority.High -> "var(--pri-high)"
    is Priority.Custom -> "var(--pri-high)"
}

/**
 * Calm Studio priority pill (`.pill`) — a neutral rounded chip with a colored leading dot and the
 * localized priority label. The dot color encodes the [Priority] via [dotColorVar]; the chip itself
 * stays deliberately neutral (metadata, not a status alarm). Shared by every place that surfaces an
 * item's priority (detail and list rows).
 *
 * @param priority Priority to display.
 */
@Composable
fun PriorityBadge(priority: Priority) {
    Span({ classes(CalmStudioStyleSheet.pill) }) {
        Span({
            classes(CalmStudioStyleSheet.dot)
            style { property("background", priority.dotColorVar()) }
        })
        Text("${priority.labelResource().translation()}${priority.weightSuffix()}")
    }
}
