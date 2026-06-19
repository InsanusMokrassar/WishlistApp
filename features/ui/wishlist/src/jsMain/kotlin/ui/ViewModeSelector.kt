package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.common.client.ui.components.SegmentedControl
import dev.inmo.wishlist.features.ui.wishlist.labelResource

/**
 * Calm Studio view-mode toggle rendered through the shared [SegmentedControl]: one segment per
 * [WishlistViewMode] (Grid then List), the active mode carrying the `on` highlight. Meant to sit in the
 * right side of a screen toolbar; carries no caption of its own.
 *
 * @param selected Currently active view mode.
 * @param onViewModeSelected Invoked with the mode the user picked.
 */
@Composable
fun ViewModeSelector(
    selected: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
) {
    SegmentedControl(
        options = listOf(WishlistViewMode.Grid, WishlistViewMode.List),
        selected = selected,
        label = { it.labelResource().translation() },
        onSelect = onViewModeSelected,
    )
}
