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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the user profile edit screen. */
class UserEditView(
    chain: NavigationChain<ViewConfig>,
    config: UserEditViewConfig,
) : ComposeView<UserEditViewConfig, ViewConfig, UserEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserEditView)
    }

    override val title: String
        @Composable get() = UsersListStrings.editProfileTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
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
                title = { Text(UsersListStrings.confirmDiscardTitle.translation()) },
                text = { Text(UsersListStrings.confirmDiscardMessage.translation()) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) { Text(UsersListStrings.confirmButton.translation()) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) { Text(UsersListStrings.cancelButton.translation()) }
                }
            )
        }
        if (showDelete) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelDelete() },
                title = { Text(UsersListStrings.confirmDeleteUserFinalTitle.translation()) },
                text = { Text("${UsersListStrings.confirmDeleteUserMessageSecond.translation()} $username") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) { Text(UsersListStrings.confirmDeleteButton.translation()) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelDelete() }) { Text(UsersListStrings.cancelButton.translation()) }
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
                BackButton(UsersListStrings.backButton.translation()) { viewModel.onBack() }
            }

            OutlinedTextField(
                value = "#${viewModel.userId.long}",
                onValueChange = {},
                label = { Text(UsersListStrings.userIdLabel.translation()) },
                singleLine = true,
                enabled = false,
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Avatar — owner and root.
            Text(UsersListStrings.avatarLabel.translation(), style = MaterialTheme.typography.subtitle1)
            avatarId?.let { id ->
                RemoteImage(
                    key = id.string,
                    loader = { viewModel.loadImageBytes(id) },
                    contentDescription = UsersListStrings.avatarLabel.translation(),
                    modifier = Modifier.size(160.dp)
                )
            }
            OutlinedButton(
                onClick = { scope.launch { pickImageFile()?.let { viewModel.onAvatarPicked(it) } } },
                enabled = !loading && !uploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uploading) UsersListStrings.uploadingPhoto.translation()
                    else UsersListStrings.uploadPhotoButton.translation()
                )
            }

            if (isRoot) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = { Text(UsersListStrings.usernameLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text(UsersListStrings.newPasswordLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                    label = { Text(UsersListStrings.confirmPasswordLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (mismatch) {
                    Text(
                        UsersListStrings.passwordMismatch.translation(),
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }
                Button(
                    onClick = { viewModel.onSave() },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(UsersListStrings.saveButton.translation())
                }
                Button(
                    onClick = { viewModel.onDeleteRequest() },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(UsersListStrings.deleteButton.translation())
                }
            } else {
                OutlinedTextField(
                    value = username,
                    onValueChange = {},
                    label = { Text(UsersListStrings.usernameLabel.translation()) },
                    singleLine = true,
                    enabled = false,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(UsersListStrings.noEditableFields.translation(), color = MaterialTheme.colors.onSurface)
            }
        }
    }
}
