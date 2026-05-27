package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the admin wishlist item create/edit screen. */
class AdminWishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistItemEditViewConfig,
) : ComposeView<AdminWishlistItemEditViewConfig, ViewConfig, AdminWishlistItemEditViewModel>(config, chain) {
    override val viewModel: AdminWishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistItemEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val title by viewModel.titleState.collectAsState()
        val price by viewModel.priceState.collectAsState()
        val priceUnits by viewModel.priceUnitsState.collectAsState()
        val description by viewModel.descriptionState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(AdminPanelStrings.confirmDiscardTitle.translation(resources)) },
                text = { Text(AdminPanelStrings.confirmDiscardMessage.translation(resources)) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) { Text(AdminPanelStrings.confirmButton.translation(resources)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) { Text(AdminPanelStrings.cancelButton.translation(resources)) }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.onBack() }) { Text(AdminPanelStrings.backButton.translation(resources)) }
                Text(
                    text = if (viewModel.isCreating) AdminPanelStrings.newItemTitle.translation(resources)
                    else AdminPanelStrings.editItemTitle.translation(resources),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(AdminPanelStrings.itemTitleLabel.translation(resources)) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { viewModel.onPriceChanged(it) },
                    label = { Text(AdminPanelStrings.itemPriceLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = priceUnits,
                    onValueChange = { viewModel.onPriceUnitsChanged(it) },
                    label = { Text(AdminPanelStrings.itemPriceUnitsLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text(AdminPanelStrings.itemDescriptionLabel.translation(resources)) },
                enabled = !loading,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AdminPanelStrings.saveButton.translation(resources))
            }
        }
    }
}
