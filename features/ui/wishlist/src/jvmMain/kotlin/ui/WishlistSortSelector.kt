package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource

/**
 * Reusable sort-mode selector row: a "Sort" caption followed by one button per [WishlistSortMode].
 * The currently [selected] mode is rendered filled, the rest outlined.
 *
 * @param selected Currently active sort mode.
 * @param onSortModeSelected Invoked with the mode the user picked.
 * @param noneLabel Label resource used for [WishlistSortMode.None]; defaults to [WishlistStrings.sortNone]
 * ("Grouped"). The detail screen passes [WishlistStrings.sortDefault] instead.
 */
@Composable
fun WishlistSortSelector(
    selected: WishlistSortMode,
    onSortModeSelected: (WishlistSortMode) -> Unit,
    noneLabel: StringResource = WishlistStrings.sortNone,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.sortLabel.translation(), style = MaterialTheme.typography.caption)
        WishlistSortMode.entries.forEach { mode ->
            val active = mode == selected
            val label = if (mode == WishlistSortMode.None) {
                noneLabel.translation()
            } else {
                mode.labelResource().translation()
            }
            Button(
                onClick = { onSortModeSelected(mode) },
                colors = if (active) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(label)
            }
        }
    }
}
