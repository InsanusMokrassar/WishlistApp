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
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
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
        val canUploadAvatar by viewModel.canUploadAvatarState.collectAsState()
        val canManageOwnEmail by viewModel.canManageOwnEmailState.collectAsState()
        val canMutateOwnEmail by viewModel.canMutateOwnEmailState.collectAsState()
        val ownEmailProfile by viewModel.ownEmailProfileState.collectAsState()
        val emailInput by viewModel.emailInputState.collectAsState()
        val emailLoading by viewModel.emailLoadingState.collectAsState()
        val emailBusy by viewModel.emailBusyState.collectAsState()
        val emailError by viewModel.emailErrorState.collectAsState()
        val emailLoadFailed by viewModel.emailLoadFailedState.collectAsState()
        val emailVerificationResult by viewModel.emailVerificationResultState.collectAsState()
        val profileSaveError by viewModel.profileSaveErrorState.collectAsState()
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
            if (canUploadAvatar) {
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
            }

            if (canManageOwnEmail) {
                Text(
                    UsersListStrings.emailSectionTitle.translation(resources),
                    style = MaterialTheme.typography.titleSmall,
                )
                when {
                    emailLoading -> Text(UsersListStrings.emailLoading.translation(resources))
                    ownEmailProfile == null -> {
                        Text(
                            if (emailLoadFailed) UsersListStrings.emailLoadFailed.translation(resources)
                            else UsersListStrings.emailLoading.translation(resources),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    ownEmailProfile?.email == null -> {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { viewModel.onEmailChanged(it) },
                            label = { Text(UsersListStrings.emailLabel.translation(resources)) },
                            singleLine = true,
                            enabled = canMutateOwnEmail,
                            isError = emailError == EmailEditorError.InvalidEmail ||
                                emailError == EmailEditorError.SaveFailed,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailMissing.translation(resources))
                        Button(
                            onClick = { viewModel.onSaveEmailAndRequestVerification() },
                            enabled = canMutateOwnEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(UsersListStrings.saveEmailAndVerifyButton.translation(resources))
                        }
                    }
                    ownEmailProfile?.emailApproved == true -> {
                        OutlinedTextField(
                            value = ownEmailProfile?.email?.string.orEmpty(),
                            onValueChange = {},
                            label = { Text(UsersListStrings.emailLabel.translation(resources)) },
                            singleLine = true,
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailApproved.translation(resources))
                    }
                    else -> {
                        OutlinedTextField(
                            value = ownEmailProfile?.email?.string.orEmpty(),
                            onValueChange = {},
                            label = { Text(UsersListStrings.emailLabel.translation(resources)) },
                            singleLine = true,
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailPendingApproval.translation(resources))
                        Button(
                            onClick = { viewModel.onResendEmailVerification() },
                            enabled = canMutateOwnEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(UsersListStrings.resendEmailVerificationButton.translation(resources))
                        }
                    }
                }
                when (emailError) {
                    EmailEditorError.InvalidEmail -> Text(
                        UsersListStrings.emailInvalid.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    EmailEditorError.SaveFailed -> Text(
                        UsersListStrings.emailSaveFailed.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    EmailEditorError.LoadFailed, null -> Unit
                }
                if (emailLoadFailed && ownEmailProfile != null) {
                    Text(
                        UsersListStrings.emailLoadFailed.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.onRefreshEmail() },
                    enabled = !emailBusy && !emailLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(UsersListStrings.refreshEmailButton.translation(resources))
                }
                when (emailVerificationResult) {
                    EmailVerificationRequestResult.Sent -> Text(UsersListStrings.emailVerificationSent.translation(resources))
                    EmailVerificationRequestResult.AlreadyApproved -> Text(
                        UsersListStrings.emailVerificationAlreadyApproved.translation(resources)
                    )
                    EmailVerificationRequestResult.Unavailable -> Text(
                        UsersListStrings.emailVerificationUnavailable.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    EmailVerificationRequestResult.NoEmail -> Text(
                        UsersListStrings.emailVerificationNoEmail.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    EmailVerificationRequestResult.EmailChanged -> Text(
                        UsersListStrings.emailVerificationChanged.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    EmailVerificationRequestResult.DeliveryFailed -> Text(
                        UsersListStrings.emailVerificationDeliveryFailed.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    null -> Unit
                }
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
                when (profileSaveError) {
                    ProfileSaveError.UsernameSaveFailed -> Text(
                        UsersListStrings.profileUsernameSaveFailed.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    ProfileSaveError.PasswordSaveFailed -> Text(
                        UsersListStrings.profilePasswordSaveFailed.translation(resources),
                        color = MaterialTheme.colorScheme.error,
                    )
                    null -> Unit
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
