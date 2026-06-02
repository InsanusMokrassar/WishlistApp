package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import dev.inmo.wishlist.features.ui.wishlist.weightSuffix
import dev.inmo.wishlist.features.wishlist.common.models.Priority

/**
 * Material3 badge showing an item's [Priority].
 *
 * Renders the localized priority label followed by the [Priority.weightSuffix] for custom
 * priorities. Shared by every place that surfaces an item's priority (detail and all-items rows).
 *
 * @param priority Priority to display.
 */
@Composable
fun PriorityBadge(priority: Priority) {
    val resources = LocalResources.current
    Badge(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text("${priority.labelResource().translation(resources)}${priority.weightSuffix()}")
    }
}
