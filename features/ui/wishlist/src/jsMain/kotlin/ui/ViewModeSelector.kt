package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Reusable Bootstrap view-mode selector: a "View" caption plus a Bootstrap dropdown whose toggle shows
 * the currently [selected] mode label and whose menu lists one `dropdown-item` per [WishlistViewMode].
 * The active mode item carries the `active` class. The menu's open/close state is driven by Compose
 * local state (the `show` class is toggled on click), so no Bootstrap JavaScript is required.
 *
 * @param selected Currently active view mode.
 * @param onViewModeSelected Invoked with the mode the user picked.
 */
@Composable
fun ViewModeSelector(
    selected: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
        Span({ classes("text-muted", "small") }) { Text(WishlistStrings.viewModeLabel.translation()) }
        Div({ classes("dropdown") }) {
            Button({
                classes("btn", "btn-outline-primary", "btn-sm", "dropdown-toggle")
                onClick { expanded = !expanded }
            }) {
                Text(selected.labelResource().translation())
            }
            Div({
                classes("dropdown-menu")
                if (expanded) classes("show")
            }) {
                WishlistViewMode.entries.forEach { mode ->
                    Button({
                        classes("dropdown-item")
                        if (mode == selected) classes("active")
                        onClick {
                            onViewModeSelected(mode)
                            expanded = false
                        }
                    }) {
                        Text(mode.labelResource().translation())
                    }
                }
            }
        }
    }
}
