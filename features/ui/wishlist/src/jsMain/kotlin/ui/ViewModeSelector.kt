package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Reusable Bootstrap view-mode selector: a "View" caption plus a button group with one button per
 * [WishlistViewMode]. The [selected] mode is rendered with `btn-primary`, the rest `btn-outline-primary`.
 *
 * @param selected Currently active view mode.
 * @param onViewModeSelected Invoked with the mode the user picked.
 */
@Composable
fun ViewModeSelector(
    selected: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
) {
    Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
        Span({ classes("text-muted", "small") }) { Text(WishlistStrings.viewModeLabel.translation()) }
        Div({ classes("btn-group", "btn-group-sm") }) {
            WishlistViewMode.entries.forEach { mode ->
                val active = mode == selected
                Button({
                    classes("btn", if (active) "btn-primary" else "btn-outline-primary")
                    onClick { onViewModeSelected(mode) }
                }) {
                    Text(mode.labelResource().translation())
                }
            }
        }
    }
}
