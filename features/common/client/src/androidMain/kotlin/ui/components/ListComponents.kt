package dev.inmo.wishlist.features.common.client.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Screen heading rendered with the Material3 headline typography.
 *
 * @param text Already-translated heading text.
 * @param modifier Modifier applied to the headline (e.g. `Modifier.weight(1f)` inside a row).
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.headlineSmall)
}

/**
 * Back-navigation button.
 *
 * @param text Already-translated button label.
 * @param onClick Invoked when the user activates the button.
 */
@Composable
fun BackButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}

/**
 * Single clickable list row inside a `LazyColumn`, carrying a primary label and optional leading/trailing content.
 *
 * @param text Already-translated primary label of the row.
 * @param onSelect Invoked when the user clicks the row label; `null` makes the label non-interactive.
 * @param leading Optional leading content (e.g. an avatar/thumbnail) rendered at the row's start.
 * @param trailing Optional trailing content (e.g. action buttons) rendered at the row's end.
 */
@Composable
fun ListRow(
    text: String,
    onSelect: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListRow(
        onSelect = onSelect,
        leading = leading,
        trailing = trailing,
        content = { Text(text) },
    )
}

/**
 * Single clickable list row inside a `LazyColumn`, with a caller-provided primary content slot and optional leading/trailing content.
 *
 * Use this overload when the primary cell needs more than a single label (secondary text, prices, etc.).
 *
 * @param onSelect Invoked when the user clicks the primary content; `null` makes the content non-interactive.
 * @param leading Optional leading content (e.g. an avatar/thumbnail) rendered at the row's start.
 * @param trailing Optional trailing content (e.g. action buttons) rendered at the row's end.
 * @param content Primary content of the row.
 */
@Composable
fun ListRow(
    onSelect: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        if (leading == null && trailing == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (onSelect != null) it.clickable { onSelect() } else it }
                    .padding(16.dp)
            ) { content() }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .let { if (onSelect != null) it.clickable { onSelect() } else it }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leading?.invoke()
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
                trailing?.invoke()
            }
        }
    }
}
