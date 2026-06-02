package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import dev.inmo.wishlist.features.ui.wishlist.weightSuffix
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Bootstrap pill badge showing an item's [Priority].
 *
 * Renders the localized priority label followed by the [Priority.weightSuffix] for custom
 * priorities. Shared by every place that surfaces an item's priority (detail and all-items rows).
 *
 * @param priority Priority to display.
 */
@Composable
fun PriorityBadge(priority: Priority) {
    Span({ classes("badge", "rounded-pill", "bg-secondary-subtle", "text-secondary-emphasis") }) {
        Text("${priority.labelResource().translation()}${priority.weightSuffix()}")
    }
}
