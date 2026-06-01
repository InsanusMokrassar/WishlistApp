package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
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
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the admin user create/edit screen. */
class AdminUserEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUserEditViewConfig,
) : ComposeView<AdminUserEditViewConfig, ViewConfig, AdminUserEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminUserEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUserEditView)
    }

    override val title: String
        @Composable get() = if (viewModel.isCreating) AdminPanelStrings.newUserTitle.translation()
            else AdminPanelStrings.editUserTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val username by viewModel.usernameState.collectAsState()
        val password by viewModel.passwordState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()

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
                BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
            }
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = { Text(AdminPanelStrings.usernameLabel.translation()) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            if (viewModel.isCreating) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text(AdminPanelStrings.passwordLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && username.isNotBlank() && (!viewModel.isCreating || password.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AdminPanelStrings.saveButton.translation())
            }
        }
    }
}
