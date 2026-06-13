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
import dev.inmo.micro_utils.strings.translation
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.labelResource

/**
 * Reusable Material view-mode selector row: a "View" caption followed by one button per
 * [WishlistViewMode]. The currently [selected] mode is rendered filled, the rest outlined.
 *
 * @param selected Currently active view mode.
 * @param onViewModeSelected Invoked with the mode the user picked.
 */
@Composable
fun ViewModeSelector(
    selected: WishlistViewMode,
    onViewModeSelected: (WishlistViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(WishlistStrings.viewModeLabel.translation(), style = MaterialTheme.typography.caption)
        WishlistViewMode.entries.forEach { mode ->
            val active = mode == selected
            Button(
                onClick = { onViewModeSelected(mode) },
                colors = if (active) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(mode.labelResource().translation())
            }
        }
    }
}
