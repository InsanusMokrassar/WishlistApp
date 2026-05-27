package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the admin wishlist create/edit screen. Owner selection via dropdown. */
class AdminWishlistEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistEditViewConfig,
) : ComposeView<AdminWishlistEditViewConfig, ViewConfig, AdminWishlistEditViewModel>(config, chain) {
    override val viewModel: AdminWishlistEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val users by viewModel.usersState.collectAsState()
        val selectedUserId by viewModel.selectedUserIdState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        var dropdownExpanded by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(AdminPanelStrings.confirmDiscardTitle.translation()) },
                text = { Text(AdminPanelStrings.confirmDiscardMessage.translation()) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) { Text(AdminPanelStrings.confirmButton.translation()) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) { Text(AdminPanelStrings.cancelButton.translation()) }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.onBack() }) { Text(AdminPanelStrings.backButton.translation()) }
                Text(
                    text = if (viewModel.isCreating) AdminPanelStrings.newWishlistTitle.translation()
                    else AdminPanelStrings.editWishlistTitle.translation(),
                    style = MaterialTheme.typography.h5
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(AdminPanelStrings.wishlistTitleLabel.translation()) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            val selectedUser = users.firstOrNull { it.id == selectedUserId }
            Column {
                Text(AdminPanelStrings.ownerLabel.translation(), style = MaterialTheme.typography.caption)
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedUser?.let { "${it.username.string} (#${it.id.long})" }
                        ?: AdminPanelStrings.selectOwner.translation())
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    users.forEach { user ->
                        DropdownMenuItem(onClick = {
                            viewModel.onOwnerSelected(user.id)
                            dropdownExpanded = false
                        }) {
                            Text("${user.username.string} (#${user.id.long})")
                        }
                    }
                }
            }
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank() && selectedUserId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AdminPanelStrings.saveButton.translation())
            }
        }
    }
}
