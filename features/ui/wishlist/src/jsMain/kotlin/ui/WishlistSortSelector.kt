package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text

/**
 * Calm Studio sort-mode selector rendered as a native `.select` listing one option per available
 * [WishlistSortMode]. Picking an option forwards the chosen mode to [onSortModeSelected]. Meant to sit
 * in the right side of a screen toolbar; carries no caption of its own.
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
    fun labelOf(mode: WishlistSortMode): String =
        if (mode == WishlistSortMode.None) noneLabel.translation() else mode.labelResource().translation()

    Select({
        classes("select")
        onChange { event ->
            val picked = event.value?.let { value -> availableModes.firstOrNull { it.name == value } }
            if (picked != null) onSortModeSelected(picked)
        }
    }) {
        availableModes.forEach { mode ->
            Option(mode.name, { if (mode == selected) selected() }) {
                Text(labelOf(mode))
            }
        }
    }
}
