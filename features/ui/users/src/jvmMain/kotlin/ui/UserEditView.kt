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
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
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
        val canUploadAvatar by viewModel.canUploadAvatarState.collectAsState()
        val canManageOwnEmail by viewModel.canManageOwnEmailState.collectAsState()
        val canMutateOwnEmail by viewModel.canMutateOwnEmailState.collectAsState()
        val canRequestPasswordChangeEmail by viewModel.canRequestPasswordChangeEmailState.collectAsState()
        val ownEmailProfile by viewModel.ownEmailProfileState.collectAsState()
        val emailInput by viewModel.emailInputState.collectAsState()
        val emailLoading by viewModel.emailLoadingState.collectAsState()
        val emailBusy by viewModel.emailBusyState.collectAsState()
        val emailError by viewModel.emailErrorState.collectAsState()
        val emailLoadFailed by viewModel.emailLoadFailedState.collectAsState()
        val emailVerificationResult by viewModel.emailVerificationResultState.collectAsState()
        val passwordChangeEmailResult by viewModel.passwordChangeEmailResultState.collectAsState()
        val profileSaveError by viewModel.profileSaveErrorState.collectAsState()
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

            // Avatar — image shown to all; uploader gated by the avatar-change functionality.
            Text(UsersListStrings.avatarLabel.translation(), style = MaterialTheme.typography.subtitle1)
            val avatarFileId = avatarId
            if (avatarFileId != null) {
                RemoteImage(
                    key = avatarFileId.string,
                    loader = { viewModel.loadImageBytes(avatarFileId) },
                    contentDescription = UsersListStrings.avatarLabel.translation(),
                    modifier = Modifier.size(160.dp)
                )
            } else {
                UserAvatarPlaceholder(
                    modifier = Modifier.size(160.dp),
                    contentDescription = UsersListStrings.avatarPlaceholderAlt.translation()
                )
            }
            if (canUploadAvatar) {
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
            }

            if (canManageOwnEmail) {
                when {
                    emailLoading -> Text(UsersListStrings.emailLoading.translation())
                    ownEmailProfile == null -> {
                        Text(
                            if (emailLoadFailed) UsersListStrings.emailLoadFailed.translation()
                            else UsersListStrings.emailLoading.translation(),
                            color = MaterialTheme.colors.error,
                        )
                    }
                    ownEmailProfile?.email == null -> {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { viewModel.onEmailChanged(it) },
                            label = { Text(UsersListStrings.emailLabel.translation()) },
                            singleLine = true,
                            enabled = canMutateOwnEmail,
                            isError = emailError == EmailEditorError.InvalidEmail ||
                                emailError == EmailEditorError.SaveFailed,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailMissing.translation())
                        Button(
                            onClick = { viewModel.onSaveEmailAndRequestVerification() },
                            enabled = canMutateOwnEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(UsersListStrings.saveEmailAndVerifyButton.translation())
                        }
                    }
                    ownEmailProfile?.emailApproved == true -> {
                        OutlinedTextField(
                            value = ownEmailProfile?.email?.string.orEmpty(),
                            onValueChange = {},
                            label = { Text(UsersListStrings.emailLabel.translation()) },
                            singleLine = true,
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailApproved.translation())
                        Button(
                            onClick = { viewModel.onRequestPasswordChangeEmail() },
                            enabled = canRequestPasswordChangeEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(UsersListStrings.requestPasswordChangeButton.translation()) }
                    }
                    else -> {
                        OutlinedTextField(
                            value = ownEmailProfile?.email?.string.orEmpty(),
                            onValueChange = {},
                            label = { Text(UsersListStrings.emailLabel.translation()) },
                            singleLine = true,
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(UsersListStrings.emailPendingApproval.translation())
                        Button(
                            onClick = { viewModel.onResendEmailVerification() },
                            enabled = canMutateOwnEmail,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(UsersListStrings.resendEmailVerificationButton.translation())
                        }
                    }
                }
                when (emailError) {
                    EmailEditorError.InvalidEmail -> Text(
                        UsersListStrings.emailInvalid.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailEditorError.SaveFailed -> Text(
                        UsersListStrings.emailSaveFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailEditorError.PasswordChangeRequestFailed -> Text(
                        UsersListStrings.passwordChangeEmailDeliveryFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailEditorError.LoadFailed, null -> Unit
                }
                if (emailLoadFailed && ownEmailProfile != null) {
                    Text(
                        UsersListStrings.emailLoadFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.onRefreshEmail() },
                    enabled = !emailBusy && !emailLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(UsersListStrings.refreshEmailButton.translation())
                }
                when (emailVerificationResult) {
                    EmailVerificationRequestResult.Sent -> Text(UsersListStrings.emailVerificationSent.translation())
                    EmailVerificationRequestResult.AlreadyApproved -> Text(
                        UsersListStrings.emailVerificationAlreadyApproved.translation()
                    )
                    EmailVerificationRequestResult.Unavailable -> Text(
                        UsersListStrings.emailVerificationUnavailable.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailVerificationRequestResult.NoEmail -> Text(
                        UsersListStrings.emailVerificationNoEmail.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailVerificationRequestResult.EmailChanged -> Text(
                        UsersListStrings.emailVerificationChanged.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    EmailVerificationRequestResult.DeliveryFailed -> Text(
                        UsersListStrings.emailVerificationDeliveryFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    null -> Unit
                }
                when (passwordChangeEmailResult) {
                    PasswordChangeEmailRequestResult.Sent -> Text(UsersListStrings.passwordChangeEmailSent.translation())
                    PasswordChangeEmailRequestResult.Unavailable,
                    PasswordChangeEmailRequestResult.Ineligible -> Text(
                        UsersListStrings.passwordChangeEmailUnavailable.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    PasswordChangeEmailRequestResult.DeliveryFailed -> Text(
                        UsersListStrings.passwordChangeEmailDeliveryFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    null -> Unit
                }
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
                when (profileSaveError) {
                    ProfileSaveError.UsernameSaveFailed -> Text(
                        UsersListStrings.profileUsernameSaveFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    ProfileSaveError.PasswordSaveFailed -> Text(
                        UsersListStrings.profilePasswordSaveFailed.translation(),
                        color = MaterialTheme.colors.error,
                    )
                    null -> Unit
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
