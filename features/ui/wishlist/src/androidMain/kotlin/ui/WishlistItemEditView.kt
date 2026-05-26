package dev.inmo.wishlist.features.ui.wishlist.ui.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the wishlist item create/edit screen. */
class WishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemEditViewConfig,
) : ComposeView<WishlistItemEditViewConfig, ViewConfig, WishlistItemEditViewModel>(config, chain) {
    override val viewModel: WishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val title by viewModel.titleState.collectAsState()
        val description by viewModel.descriptionState.collectAsState()
        val price by viewModel.priceState.collectAsState()
        val priceUnits by viewModel.priceUnitsState.collectAsState()
        val links by viewModel.linksState.collectAsState()
        val newLink by viewModel.newLinkState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(WishlistStrings.confirmDiscardTitle.translation(resources)) },
                text = { Text(WishlistStrings.confirmDiscardMessage.translation(resources)) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) {
                        Text(WishlistStrings.confirmButton.translation(resources))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) {
                        Text(WishlistStrings.cancelButton.translation(resources))
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.onBack() }) {
                    Text(WishlistStrings.backButton.translation(resources))
                }
                Text(
                    text = if (viewModel.isCreating) WishlistStrings.newItemTitle.translation(resources)
                    else WishlistStrings.editItemTitle.translation(resources),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(WishlistStrings.titleLabel.translation(resources)) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text(WishlistStrings.descriptionLabel.translation(resources)) },
                minLines = 3,
                maxLines = 5,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { viewModel.onPriceChanged(it) },
                    label = { Text(WishlistStrings.priceLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priceUnits,
                    onValueChange = { viewModel.onPriceUnitsChanged(it) },
                    label = { Text(WishlistStrings.priceUnitsLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(WishlistStrings.linksLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
            links.forEachIndexed { index, link ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(link, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { viewModel.onRemoveLink(index) }) {
                            Text("×")
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newLink,
                    onValueChange = { viewModel.onNewLinkChanged(it) },
                    label = { Text(WishlistStrings.newLinkPlaceholder.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.onAddLink() },
                    enabled = newLink.isNotBlank()
                ) {
                    Text(WishlistStrings.addLinkButton.translation(resources))
                }
            }
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(WishlistStrings.saveButton.translation(resources))
            }
        }
    }
}
