package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the wishlist create/edit screen. */
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
        val title by viewModel.titleState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelDelete() },
                title = { Text(WishlistStrings.confirmDeleteWishlistTitle.translation()) },
                text = { Text(WishlistStrings.confirmDeleteWishlistMessage.translation()) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text(WishlistStrings.confirmDeleteButton.translation())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelDelete() }) {
                        Text(WishlistStrings.cancelButton.translation())
                    }
                }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(WishlistStrings.confirmDiscardTitle.translation()) },
                text = { Text(WishlistStrings.confirmDiscardMessage.translation()) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) {
                        Text(WishlistStrings.confirmButton.translation())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) {
                        Text(WishlistStrings.cancelButton.translation())
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
                Button(onClick = { viewModel.onBack() }) {
                    Text(WishlistStrings.backButton.translation())
                }
                Text(
                    text = if (viewModel.isCreating) WishlistStrings.newWishlistTitle.translation()
                    else WishlistStrings.editWishlistTitle.translation(),
                    style = MaterialTheme.typography.h5
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(WishlistStrings.titleLabel.translation()) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(WishlistStrings.saveButton.translation())
            }
            if (viewModel.canDelete) {
                Button(
                    onClick = { viewModel.onDelete() },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WishlistStrings.deleteButton.translation())
                }
            }
        }
    }
}
