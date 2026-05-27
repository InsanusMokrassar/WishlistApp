package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** Android Compose-Material3 view for the admin wishlist create/edit screen. Owner selection via dropdown. */
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
        val resources = LocalResources.current
        val title by viewModel.titleState.collectAsState()
        val users by viewModel.usersState.collectAsState()
        val selectedUserId by viewModel.selectedUserIdState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        var dropdownExpanded by remember { mutableStateOf(false) }

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
                    text = if (viewModel.isCreating) AdminPanelStrings.newWishlistTitle.translation(resources)
                    else AdminPanelStrings.editWishlistTitle.translation(resources),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(AdminPanelStrings.wishlistTitleLabel.translation(resources)) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            val selectedUser = users.firstOrNull { it.id == selectedUserId }
            Column {
                Text(AdminPanelStrings.ownerLabel.translation(resources), style = MaterialTheme.typography.labelMedium)
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedUser?.let { "${it.username.string} (#${it.id.long})" }
                        ?: AdminPanelStrings.selectOwner.translation(resources))
                }
                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    users.forEach { user ->
                        DropdownMenuItem(
                            text = { Text("${user.username.string} (#${user.id.long})") },
                            onClick = {
                                viewModel.onOwnerSelected(user.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank() && selectedUserId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AdminPanelStrings.saveButton.translation(resources))
            }
        }
    }
}
