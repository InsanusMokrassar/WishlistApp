package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.ui.users.utils.pickImageFile
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the user profile edit screen. */
class UserEditView(
    chain: NavigationChain<ViewConfig>,
    config: UserEditViewConfig,
) : ComposeView<UserEditViewConfig, ViewConfig, UserEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserEditView)
    }

    override val title: String
        @Composable get() = UsersListStrings.editProfileTitle.translation(LocalResources.current)

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val isRoot by viewModel.isRootState.collectAsState()
        val username by viewModel.usernameState.collectAsState()
        val password by viewModel.passwordState.collectAsState()
        val confirmPassword by viewModel.confirmPasswordState.collectAsState()
        val avatarId by viewModel.avatarIdState.collectAsState()
        val uploading by viewModel.uploadingAvatarState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val mismatch by viewModel.passwordMismatchState.collectAsState()
        val canSave by viewModel.canSaveState.collectAsState()
        val showDiscard by viewModel.showConfirmDialogState.collectAsState()
        val showDelete by viewModel.showDeleteDialogState.collectAsState()
        val scope = rememberCoroutineScope()

        if (showDiscard) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(UsersListStrings.confirmDiscardTitle.translation(resources)) },
                text = { Text(UsersListStrings.confirmDiscardMessage.translation(resources)) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) { Text(UsersListStrings.confirmButton.translation(resources)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) { Text(UsersListStrings.cancelButton.translation(resources)) }
                }
            )
        }
        if (showDelete) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelDelete() },
                title = { Text(UsersListStrings.confirmDeleteUserFinalTitle.translation(resources)) },
                text = { Text("${UsersListStrings.confirmDeleteUserMessageSecond.translation(resources)} $username") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text(UsersListStrings.confirmDeleteButton.translation(resources)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelDelete() }) { Text(UsersListStrings.cancelButton.translation(resources)) }
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
                BackButton(UsersListStrings.backButton.translation(resources)) { viewModel.onBack() }
            }

            OutlinedTextField(
                value = "#${viewModel.userId.long}",
                onValueChange = {},
                label = { Text(UsersListStrings.userIdLabel.translation(resources)) },
                singleLine = true,
                enabled = false,
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(UsersListStrings.avatarLabel.translation(resources), style = MaterialTheme.typography.titleSmall)
            val avatarFileId = avatarId
            if (avatarFileId != null) {
                RemoteImage(
                    key = avatarFileId.string,
                    loader = { viewModel.loadImageBytes(avatarFileId) },
                    contentDescription = UsersListStrings.avatarLabel.translation(resources),
                    modifier = Modifier.size(160.dp)
                )
            } else {
                UserAvatarPlaceholder(
                    modifier = Modifier.size(160.dp),
                    contentDescription = UsersListStrings.avatarPlaceholderAlt.translation(resources)
                )
            }
            OutlinedButton(
                onClick = { scope.launch { pickImageFile()?.let { viewModel.onAvatarPicked(it) } } },
                enabled = !loading && !uploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uploading) UsersListStrings.uploadingPhoto.translation(resources)
                    else UsersListStrings.uploadPhotoButton.translation(resources)
                )
            }

            if (isRoot) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text(UsersListStrings.usernameLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text(UsersListStrings.newPasswordLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                    label = { Text(UsersListStrings.confirmPasswordLabel.translation(resources)) },
                    singleLine = true,
                    enabled = !loading,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (mismatch) {
                    Text(
                        UsersListStrings.passwordMismatch.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { viewModel.onSave() },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(UsersListStrings.saveButton.translation(resources))
                }
                Button(
                    onClick = { viewModel.onDeleteRequest() },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(UsersListStrings.deleteButton.translation(resources))
                }
            } else {
                OutlinedTextField(
                    value = username,
                    onValueChange = {},
                    label = { Text(UsersListStrings.usernameLabel.translation(resources)) },
                    singleLine = true,
                    enabled = false,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(UsersListStrings.noEditableFields.translation(resources))
            }
        }
    }
}
