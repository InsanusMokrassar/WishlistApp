package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.ui.users.utils.emailChangeDeadlineText
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

            OwnerEmailEditor(viewModel, this@UserEditView)

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

/**
 * Renders private owner-email controls only for the exact live navigation [node] bound to [viewModel].
 * Production and desktop tests invoke this same boundary so a lagging ViewModel owner collector cannot
 * expose a previously selected account's private email content.
 *
 * @param viewModel Editor state and guarded callbacks for one immutable bound user.
 * @param node Actual rendered navigation node whose collected and raw targets are both checked.
 */
@Composable
internal fun OwnerEmailEditor(
    viewModel: UserEditViewModel,
    node: NavigationNode<UserEditViewConfig, ViewConfig>,
) {
    val currentConfig by node.configState.collectAsState()
    val canManageOwnEmail by viewModel.canManageOwnEmailState.collectAsState()
    val canMutateOwnEmail by viewModel.canMutateOwnEmailState.collectAsState()
    val canSaveEmail by viewModel.canSaveEmailState.collectAsState()
    val canResendEmail by viewModel.canResendEmailVerificationState.collectAsState()
    val emailCapability by viewModel.emailCapabilityState.collectAsState()
    val ownEmailProfile by viewModel.ownEmailProfileState.collectAsState()
    val emailInput by viewModel.emailInputState.collectAsState()
    val emailLoading by viewModel.emailLoadingState.collectAsState()
    val emailBusy by viewModel.emailBusyState.collectAsState()
    val emailError by viewModel.emailErrorState.collectAsState()
    val emailLoadFailed by viewModel.emailLoadFailedState.collectAsState()
    val emailVerificationResult by viewModel.emailVerificationResultState.collectAsState()
    val emailSaved by viewModel.emailSavedState.collectAsState()
    val emailOperationInterrupted by viewModel.emailOperationInterruptedState.collectAsState()
    val emailChangeRestriction by viewModel.emailChangeRestrictionState.collectAsState()
    val canRenderOwnEmail =
        canManageOwnEmail &&
            currentConfig.userId == viewModel.userId &&
            node.config.userId == viewModel.userId

    if (canRenderOwnEmail) {
        when {
            emailLoading -> Text(UsersListStrings.emailLoading.translation())
            ownEmailProfile == null -> {
                Text(
                    if (emailLoadFailed) UsersListStrings.emailLoadFailed.translation()
                    else UsersListStrings.emailLoading.translation(),
                    color = MaterialTheme.colors.error,
                )
            }
            else -> {
                val currentEmail = ownEmailProfile?.email
                val pendingEmail = ownEmailProfile?.pendingEmail
                if (currentEmail == null) {
                    Text(UsersListStrings.emailMissing.translation())
                } else {
                    OutlinedTextField(
                        value = currentEmail.string,
                        onValueChange = {},
                        label = { Text(UsersListStrings.savedEmailLabel.translation()) },
                        singleLine = true,
                        enabled = false,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings-email-saved"),
                    )
                    Text(
                        if (ownEmailProfile?.emailApproved == true) {
                            UsersListStrings.emailApproved.translation()
                        } else {
                            UsersListStrings.emailPendingApproval.translation()
                        }
                    )
                }
                if (pendingEmail != null) {
                    OutlinedTextField(
                        value = pendingEmail.string,
                        onValueChange = {},
                        label = { Text(UsersListStrings.pendingEmailLabel.translation()) },
                        singleLine = true,
                        enabled = false,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings-email-pending"),
                    )
                    Text(UsersListStrings.emailPendingApproval.translation())
                    Text(UsersListStrings.emailReplacementNeedsVerification.translation())
                }
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text(UsersListStrings.emailLabel.translation()) },
                    singleLine = true,
                    enabled = canMutateOwnEmail,
                    isError = emailError == EmailEditorError.InvalidEmail ||
                        emailError == EmailEditorError.SaveFailed,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.onSaveEmail() }),
                    modifier = Modifier.fillMaxWidth().testTag("settings-email"),
                )
                Button(
                    onClick = { viewModel.onSaveEmail() },
                    enabled = canSaveEmail,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (emailCapability == EmailCapabilityState.Enabled) {
                            UsersListStrings.saveEmailAndVerifyButton.translation()
                        } else {
                            UsersListStrings.saveEmailButton.translation()
                        }
                    )
                }
                if (
                    (pendingEmail != null || (currentEmail != null && !ownEmailProfile!!.emailApproved)) &&
                    emailCapability == EmailCapabilityState.Enabled
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onResendEmailVerification() },
                        enabled = canResendEmail,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(UsersListStrings.resendEmailVerificationButton.translation())
                    }
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
            EmailEditorError.EmailChanged -> Text(
                UsersListStrings.emailVerificationChanged.translation(),
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
        emailChangeRestriction?.let { deadline ->
            Text(
                UsersListStrings.emailChangeCooldown.translation().replace("%s", emailChangeDeadlineText(deadline)),
                color = MaterialTheme.colors.error,
                modifier = Modifier.testTag("settings-email-cooldown"),
            )
        }
        emailSaved?.let {
            Text(UsersListStrings.emailSaved.translation())
        }
        OutlinedButton(
            onClick = { viewModel.onRefreshEmail() },
            enabled = !emailBusy && !emailLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(UsersListStrings.refreshEmailButton.translation())
        }
        if (
            emailVerificationResult == EmailVerificationRequestResult.Unavailable ||
            (ownEmailProfile != null && emailCapability == EmailCapabilityState.Disabled)
        ) {
            Text(
                UsersListStrings.emailVerificationUnavailable.translation(),
                color = MaterialTheme.colors.error,
            )
        }
        when (emailVerificationResult) {
            EmailVerificationRequestResult.Sent -> Text(UsersListStrings.emailVerificationSent.translation())
            EmailVerificationRequestResult.AlreadyApproved -> Text(
                UsersListStrings.emailVerificationAlreadyApproved.translation()
            )
            EmailVerificationRequestResult.Unavailable -> Unit
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
        if (emailOperationInterrupted) {
            Text(UsersListStrings.emailOperationInterrupted.translation(), color = MaterialTheme.colors.error)
        }
    }
}
