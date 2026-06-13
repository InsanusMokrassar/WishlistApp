package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Reusable Bootstrap sort-mode selector: a "Sort" caption plus a Bootstrap dropdown whose toggle shows
 * the currently [selected] mode label and whose menu lists one `dropdown-item` per available
 * [WishlistSortMode]. The active mode item carries the `active` class. The menu's open/close state is
 * driven by Compose local state (the `show` class is toggled on click), so no Bootstrap JavaScript is
 * required.
 *
 * @param selected Currently active sort mode.
 * @param onSortModeSelected Invoked with the mode the user picked.
 * @param noneLabel Label resource used for [WishlistSortMode.None]; defaults to [WishlistStrings.sortNone]
 * ("Grouped"). The detail screen passes [WishlistStrings.sortDefault] instead.
 * @param availableModes Modes to offer; defaults to all. Callers drop [WishlistSortMode.Cost] when
 * price sorting is not meaningful (currency feature disabled and items use mixed currencies).
 */
@Composable
fun WishlistSortSelector(
    selected: WishlistSortMode,
    onSortModeSelected: (WishlistSortMode) -> Unit,
    noneLabel: StringResource = WishlistStrings.sortNone,
    availableModes: List<WishlistSortMode> = WishlistSortMode.entries,
) {
    var expanded by remember { mutableStateOf(false) }

    fun labelOf(mode: WishlistSortMode): String =
        if (mode == WishlistSortMode.None) noneLabel.translation() else mode.labelResource().translation()

    Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
        Span({ classes("text-muted", "small") }) { Text(WishlistStrings.sortLabel.translation()) }
        Div({ classes("dropdown") }) {
            Button({
                classes("btn", "btn-outline-primary", "btn-sm", "dropdown-toggle")
                onClick { expanded = !expanded }
            }) {
                Text(labelOf(selected))
            }
            Div({
                classes("dropdown-menu")
                if (expanded) classes("show")
            }) {
                availableModes.forEach { mode ->
                    Button({
                        classes("dropdown-item")
                        if (mode == selected) classes("active")
                        onClick {
                            onSortModeSelected(mode)
                            expanded = false
                        }
                    }) {
                        Text(labelOf(mode))
                    }
                }
            }
        }
    }
}
