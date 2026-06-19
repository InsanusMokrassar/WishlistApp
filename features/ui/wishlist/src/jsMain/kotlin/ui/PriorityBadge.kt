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
 * Calm Studio stylesheet class setting the dot fill for a [Priority] (applied alongside `.dot`).
 *
 * @return One of the `.dot-pri-*` classes; custom priorities reuse the high-priority fill.
 */
fun Priority.dotClass(): String = when (this) {
    Priority.Small -> CalmStudioStyleSheet.`dot-pri-low`
    Priority.Medium -> CalmStudioStyleSheet.`dot-pri-med`
    Priority.High -> CalmStudioStyleSheet.`dot-pri-high`
    is Priority.Custom -> CalmStudioStyleSheet.`dot-pri-high`
}

/**
 * Calm Studio priority pill (`.pill`) — a neutral rounded chip with a colored leading dot and the
 * localized priority label. The dot color encodes the [Priority] via [dotClass]; the chip itself
 * stays deliberately neutral (metadata, not a status alarm). Shared by every place that surfaces an
 * item's priority (detail and list rows).
 *
 * @param priority Priority to display.
 */
@Composable
fun PriorityBadge(priority: Priority) {
    Span({ classes(CalmStudioStyleSheet.pill) }) {
        Span({ classes(CalmStudioStyleSheet.dot, priority.dotClass()) })
        Text("${priority.labelResource().translation()}${priority.weightSuffix()}")
    }
}
