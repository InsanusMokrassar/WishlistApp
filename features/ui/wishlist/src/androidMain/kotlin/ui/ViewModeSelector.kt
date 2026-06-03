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
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource

/**
 * Reusable Material3 view-mode selector row: a "View" caption followed by one button per
 * [WishlistViewMode]. The [selected] mode uses a filled [Button], the rest an [OutlinedButton].
 *
 * @param selected Currently active view mode.
 * @param onViewModeSelected Invoked with the mode the user picked.
 */
@Composable
fun ViewModeSelector(
    selected: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
) {
    val resources = LocalResources.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.viewModeLabel.translation(resources), style = MaterialTheme.typography.bodySmall)
        WishlistViewMode.entries.forEach { mode ->
            val label = mode.labelResource().translation(resources)
            if (mode == selected) {
                Button(onClick = { onViewModeSelected(mode) }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onViewModeSelected(mode) }) { Text(label) }
            }
        }
    }
}
