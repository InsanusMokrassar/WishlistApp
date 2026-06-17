package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio view-mode toggle rendered as a segmented control (`.seg`): one button per
 * [WishlistViewMode] (Grid then List), the active mode carrying the `on` class. Meant to sit in the
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
    Div({ classes("seg") }) {
        listOf(WishlistViewMode.Grid, WishlistViewMode.List).forEach { mode ->
            Button({
                if (mode == selected) classes("on")
                onClick { onViewModeSelected(mode) }
            }) {
                Text(mode.labelResource().translation())
            }
        }
    }
}
