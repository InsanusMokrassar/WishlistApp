package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the wishlist create/edit screen. */
class WishlistEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistEditViewConfig,
) : ComposeView<WishlistEditViewConfig, ViewConfig, WishlistEditViewModel>(config, chain) {
    override val viewModel: WishlistEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val title by viewModel.titleState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelDelete() },
                title = { Text(WishlistStrings.confirmDeleteWishlistTitle.translation(resources)) },
                text = { Text(WishlistStrings.confirmDeleteWishlistMessage.translation(resources)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(WishlistStrings.confirmDeleteButton.translation(resources))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelDelete() }) {
                        Text(WishlistStrings.cancelButton.translation(resources))
                    }
                }
            )
        }

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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(WishlistStrings.backButton.translation(resources)) { viewModel.onBack() }
                ScreenTitle(
                    if (viewModel.isCreating) WishlistStrings.newWishlistTitle.translation(resources)
                    else WishlistStrings.editWishlistTitle.translation(resources)
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
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(WishlistStrings.saveButton.translation(resources))
            }
            if (viewModel.canDelete) {
                Button(
                    onClick = { viewModel.onDelete() },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WishlistStrings.deleteButton.translation(resources))
                }
            }
        }
    }
}
