package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Reusable Bootstrap sort-mode selector: a "Sort" caption plus a button group with one button per
 * [WishlistSortMode]. The [selected] mode is rendered with `btn-primary`, the rest `btn-outline-primary`.
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
    Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2", "flex-wrap") }) {
        Span({ classes("text-muted", "small") }) { Text(WishlistStrings.sortLabel.translation()) }
        Div({ classes("btn-group", "btn-group-sm") }) {
            availableModes.forEach { mode ->
                val active = mode == selected
                val label = if (mode == WishlistSortMode.None) {
                    noneLabel.translation()
                } else {
                    mode.labelResource().translation()
                }
                Button({
                    classes("btn", if (active) "btn-primary" else "btn-outline-primary")
                    onClick { onSortModeSelected(mode) }
                }) {
                    Text(label)
                }
            }
        }
    }
}
