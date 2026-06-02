package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.StringResource
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource

/**
 * Reusable Material3 sort-mode selector row: a "Sort" caption followed by one button per
 * [WishlistSortMode]. The [selected] mode uses a filled [Button], the rest an [OutlinedButton].
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
    val resources = LocalResources.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.sortLabel.translation(resources), style = MaterialTheme.typography.bodySmall)
        WishlistSortMode.entries.forEach { mode ->
            val label = if (mode == WishlistSortMode.None) {
                noneLabel.translation(resources)
            } else {
                mode.labelResource().translation(resources)
            }
            if (mode == selected) {
                Button(onClick = { onSortModeSelected(mode) }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onSortModeSelected(mode) }) { Text(label) }
            }
        }
    }
}
