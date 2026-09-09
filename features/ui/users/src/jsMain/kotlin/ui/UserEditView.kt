package dev.inmo.wishlist.features.ui.users.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmForm
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ConfirmModal
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.FieldSet
import dev.inmo.wishlist.features.common.client.ui.components.FormHint
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.email.common.models.EmailVerificationRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.ui.users.utils.pickImageFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Per-view stylesheet for [UserEditView]: the avatar preview block (wrapper spacing + square image).
 * Self-registers into the [StyleSheetsAggregator].
 */
object UserEditViewStylesheet : StyleSheet() {
    /** Spacing under the avatar preview, above the change-photo button. */
    val avatarWrap by style { property("margin-bottom", "8px") }

    /** Square 160px avatar preview image (cropped to cover). */
    val avatarImg by style {
        property("width", "160px"); property("height", "160px"); property("object-fit", "cover")
        property("border-radius", "12px"); property("display", "block")
    }

    init { StyleSheetsAggregator.addStyleSheet(this) }
}

/** JS Compose-HTML view for the profile / account settings screen (Calm Studio form). */
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
            ConfirmModal(
                title = UsersListStrings.confirmDiscardTitle.translation(),
                body = UsersListStrings.confirmDiscardMessage.translation(),
                confirmLabel = UsersListStrings.confirmButton.translation(),
                cancelLabel = UsersListStrings.cancelButton.translation(),
                danger = true,
                onCancel = { viewModel.onCancelBack() },
                onConfirm = { viewModel.onConfirmBack() },
            )
        }
        if (showDelete) {
            ConfirmModal(
                title = UsersListStrings.confirmDeleteUserFinalTitle.translation(),
                body = "${UsersListStrings.confirmDeleteUserMessageSecond.translation()} $username",
                confirmLabel = UsersListStrings.confirmDeleteButton.translation(),
                cancelLabel = UsersListStrings.cancelButton.translation(),
                danger = true,
                onCancel = { viewModel.onCancelDelete() },
                onConfirm = { viewModel.onConfirmDelete() },
            )
        }

        ContentColumn {
            PageHead(
                title = UsersListStrings.editProfileTitle.translation(),
                subline = "#${viewModel.userId.long}",
            )

            CalmForm {
                // Avatar section — image shown to all; uploader gated by the avatar-change functionality.
                FieldSet(label = UsersListStrings.avatarLabel.translation()) {
                    Div({ classes(UserEditViewStylesheet.avatarWrap) }) {
                        val id = avatarId
                        if (id != null) {
                            Img(src = viewModel.imageUrl(id), alt = UsersListStrings.avatarLabel.translation()) {
                                classes(UserEditViewStylesheet.avatarImg)
                            }
                        } else {
                            UserAvatarPlaceholder(
                                sizePx = 160,
                                circle = false,
                                alt = UsersListStrings.avatarPlaceholderAlt.translation()
                            )
                        }
                    }
                if (canUploadAvatar) {
                        CalmButton(
                            text = if (uploading) UsersListStrings.uploadingPhoto.translation()
                                else UsersListStrings.uploadPhotoButton.translation(),
                            onClick = { scope.launch { pickImageFile()?.let { viewModel.onAvatarPicked(it) } } },
                            disabled = loading || uploading,
                        )
                    }
                }

                if (canManageOwnEmail) {
                    FieldSet(label = UsersListStrings.emailSectionTitle.translation()) {
                        when {
                            emailLoading -> FormHint(UsersListStrings.emailLoading.translation())
                            ownEmailProfile == null -> {
                                FormHint(
                                    if (emailLoadFailed) UsersListStrings.emailLoadFailed.translation()
                                    else UsersListStrings.emailLoading.translation(),
                                    error = true,
                                )
                            }
                            ownEmailProfile?.email == null -> {
                                CalmTextField(
                                    value = emailInput,
                                    onValueChange = { viewModel.onEmailChanged(it) },
                                    label = UsersListStrings.emailLabel.translation(),
                                    type = InputType.Email,
                                    disabled = !canMutateOwnEmail,
                                    hint = UsersListStrings.emailMissing.translation(),
                                    id = "settings-email",
                                )
                                CalmButton(
                                    text = UsersListStrings.saveEmailAndVerifyButton.translation(),
                                    onClick = { viewModel.onSaveEmailAndRequestVerification() },
                                    variant = CalmButtonVariant.Primary,
                                    disabled = !canMutateOwnEmail,
                                )
                            }
                            ownEmailProfile?.emailApproved == true -> {
                                CalmTextField(
                                    value = ownEmailProfile?.email?.string.orEmpty(),
                                    onValueChange = {},
                                    label = UsersListStrings.emailLabel.translation(),
                                    type = InputType.Email,
                                    disabled = true,
                                    hint = UsersListStrings.emailApproved.translation(),
                                    id = "settings-email",
                                )
                                CalmButton(
                                    text = UsersListStrings.requestPasswordChangeButton.translation(),
                                    onClick = { viewModel.onRequestPasswordChangeEmail() },
                                    variant = CalmButtonVariant.Primary,
                                    disabled = !canRequestPasswordChangeEmail,
                                )
                            }
                            else -> {
                                CalmTextField(
                                    value = ownEmailProfile?.email?.string.orEmpty(),
                                    onValueChange = {},
                                    label = UsersListStrings.emailLabel.translation(),
                                    type = InputType.Email,
                                    disabled = true,
                                    hint = UsersListStrings.emailPendingApproval.translation(),
                                    id = "settings-email",
                                )
                                CalmButton(
                                    text = UsersListStrings.resendEmailVerificationButton.translation(),
                                    onClick = { viewModel.onResendEmailVerification() },
                                    variant = CalmButtonVariant.Primary,
                                    disabled = !canMutateOwnEmail,
                                )
                            }
                        }
                        when (emailError) {
                            EmailEditorError.InvalidEmail -> FormHint(
                                UsersListStrings.emailInvalid.translation(),
                                error = true,
                            )
                            EmailEditorError.SaveFailed -> FormHint(
                                UsersListStrings.emailSaveFailed.translation(),
                                error = true,
                            )
                            EmailEditorError.PasswordChangeRequestFailed -> FormHint(
                                UsersListStrings.passwordChangeEmailDeliveryFailed.translation(),
                                error = true,
                            )
                            EmailEditorError.LoadFailed, null -> Unit
                        }
                        if (emailLoadFailed && ownEmailProfile != null) {
                            FormHint(UsersListStrings.emailLoadFailed.translation(), error = true)
                        }
                        CalmButton(
                            text = UsersListStrings.refreshEmailButton.translation(),
                            onClick = { viewModel.onRefreshEmail() },
                            variant = CalmButtonVariant.Ghost,
                            disabled = emailBusy || emailLoading,
                        )
                        when (emailVerificationResult) {
                            EmailVerificationRequestResult.Sent -> FormHint(
                                UsersListStrings.emailVerificationSent.translation()
                            )
                            EmailVerificationRequestResult.AlreadyApproved -> FormHint(
                                UsersListStrings.emailVerificationAlreadyApproved.translation()
                            )
                            EmailVerificationRequestResult.Unavailable -> FormHint(
                                UsersListStrings.emailVerificationUnavailable.translation(),
                                error = true,
                            )
                            EmailVerificationRequestResult.NoEmail -> FormHint(
                                UsersListStrings.emailVerificationNoEmail.translation(),
                                error = true,
                            )
                            EmailVerificationRequestResult.EmailChanged -> FormHint(
                                UsersListStrings.emailVerificationChanged.translation(),
                                error = true,
                            )
                            EmailVerificationRequestResult.DeliveryFailed -> FormHint(
                                UsersListStrings.emailVerificationDeliveryFailed.translation(),
                                error = true,
                            )
                            null -> Unit
                        }
                        when (passwordChangeEmailResult) {
                            PasswordChangeEmailRequestResult.Sent -> FormHint(
                                UsersListStrings.passwordChangeEmailSent.translation()
                            )
                            PasswordChangeEmailRequestResult.Unavailable,
                            PasswordChangeEmailRequestResult.Ineligible -> FormHint(
                                UsersListStrings.passwordChangeEmailUnavailable.translation(),
                                error = true,
                            )
                            PasswordChangeEmailRequestResult.DeliveryFailed -> FormHint(
                                UsersListStrings.passwordChangeEmailDeliveryFailed.translation(),
                                error = true,
                            )
                            null -> Unit
                        }
                    }
                }

                if (isRoot) {
                    CalmTextField(
                        value = username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        label = UsersListStrings.usernameLabel.translation(),
                        placeholder = UsersListStrings.usernameLabel.translation(),
                        disabled = loading,
                        id = "settings-username",
                    )
                    CalmTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = UsersListStrings.newPasswordLabel.translation(),
                        placeholder = UsersListStrings.newPasswordLabel.translation(),
                        type = InputType.Password,
                        disabled = loading,
                        id = "settings-password",
                    )
                    FieldSet(label = UsersListStrings.confirmPasswordLabel.translation(), forId = "settings-confirm-password") {
                        Input(InputType.Password) {
                            id("settings-confirm-password")
                            classes(CalmStudioStyleSheet.input)
                            value(confirmPassword)
                            onInput { viewModel.onConfirmPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                        if (mismatch) {
                            FormHint(UsersListStrings.passwordMismatch.translation(), error = true)
                        }
                    }
                    when (profileSaveError) {
                        ProfileSaveError.UsernameSaveFailed -> FormHint(
                            UsersListStrings.profileUsernameSaveFailed.translation(),
                            error = true,
                        )
                        ProfileSaveError.PasswordSaveFailed -> FormHint(
                            UsersListStrings.profilePasswordSaveFailed.translation(),
                            error = true,
                        )
                        null -> Unit
                    }
                    Div({ classes(CalmStudioStyleSheet.formactions) }) {
                        CalmButton(
                            text = UsersListStrings.saveButton.translation(),
                            onClick = { viewModel.onSave() },
                            variant = CalmButtonVariant.Primary,
                            disabled = !canSave,
                        )
                        CalmButton(
                            text = UsersListStrings.backButton.translation(),
                            onClick = { viewModel.onBack() },
                            variant = CalmButtonVariant.Ghost,
                        )
                        CalmButton(
                            text = UsersListStrings.deleteButton.translation(),
                            onClick = { viewModel.onDeleteRequest() },
                            variant = CalmButtonVariant.Danger,
                            disabled = loading,
                        )
                    }
                } else {
                    FieldSet(label = UsersListStrings.usernameLabel.translation(), forId = "settings-username") {
                        Input(InputType.Text) {
                            id("settings-username")
                            classes(CalmStudioStyleSheet.input)
                            value(username)
                            attr("readonly", "true")
                            disabled()
                        }
                    }
                    FormHint(UsersListStrings.noEditableFields.translation())
                    Div({ classes(CalmStudioStyleSheet.formactions) }) {
                        CalmButton(
                            text = UsersListStrings.backButton.translation(),
                            onClick = { viewModel.onBack() },
                            variant = CalmButtonVariant.Ghost,
                        )
                    }
                }
            }
        }
    }
}
