package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.ui.users.utils.pickImageFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the user profile edit screen. Uses Bootstrap classes. */
class UserEditView(
    chain: NavigationChain<ViewConfig>,
    config: UserEditViewConfig,
) : ComposeView<UserEditViewConfig, ViewConfig, UserEditViewModel>(config, chain) {
    override val viewModel: UserEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserEditView)
    }

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
            ConfirmModal(
                title = UsersListStrings.confirmDiscardTitle.translation(),
                message = UsersListStrings.confirmDiscardMessage.translation(),
                confirmLabel = UsersListStrings.confirmButton.translation(),
                onConfirm = { viewModel.onConfirmBack() },
                onCancel = { viewModel.onCancelBack() }
            )
        }
        if (showDelete) {
            ConfirmModal(
                title = UsersListStrings.confirmDeleteUserFinalTitle.translation(),
                message = "${UsersListStrings.confirmDeleteUserMessageSecond.translation()} $username",
                confirmLabel = UsersListStrings.confirmDeleteButton.translation(),
                onConfirm = { viewModel.onConfirmDelete() },
                onCancel = { viewModel.onCancelDelete() }
            )
        }

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(UsersListStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(UsersListStrings.editProfileTitle.translation())
            }

            // Read-only user id (never editable).
            Div({ classes("mb-3") }) {
                Label(null) { Text(UsersListStrings.userIdLabel.translation()) }
                Input(InputType.Text) {
                    classes("form-control")
                    value("#${viewModel.userId.long}")
                    attr("readonly", "true")
                    disabled()
                }
            }

            // Avatar section — available to owner and root.
            Div({ classes("mb-3") }) {
                Label(null) { Text(UsersListStrings.avatarLabel.translation()) }
                avatarId?.let { id ->
                    Div({ classes("mb-2") }) {
                        Img(src = viewModel.imageUrl(id), alt = UsersListStrings.avatarLabel.translation()) {
                            classes("rounded", "border", "d-block")
                            attr("width", "160")
                            attr("height", "160")
                            attr("style", "object-fit: cover;")
                        }
                    }
                }
                Button({
                    classes("btn", "btn-outline-secondary")
                    onClick { scope.launch { pickImageFile()?.let { viewModel.onAvatarPicked(it) } } }
                    if (loading || uploading) disabled()
                }) {
                    Text(
                        if (uploading) UsersListStrings.uploadingPhoto.translation()
                        else UsersListStrings.uploadPhotoButton.translation()
                    )
                }
            }

            if (isRoot) {
                Div({ classes("mb-3") }) {
                    Label(null) { Text(UsersListStrings.usernameLabel.translation()) }
                    Input(InputType.Text) {
                        classes("form-control")
                        value(username)
                        placeholder(UsersListStrings.usernameLabel.translation())
                        onInput { viewModel.onUsernameChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ classes("mb-3") }) {
                    Label(null) { Text(UsersListStrings.newPasswordLabel.translation()) }
                    Input(InputType.Password) {
                        classes("form-control")
                        value(password)
                        placeholder(UsersListStrings.newPasswordLabel.translation())
                        onInput { viewModel.onPasswordChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ classes("mb-3") }) {
                    Label(null) { Text(UsersListStrings.confirmPasswordLabel.translation()) }
                    Input(InputType.Password) {
                        classes("form-control")
                        value(confirmPassword)
                        placeholder(UsersListStrings.confirmPasswordLabel.translation())
                        onInput { viewModel.onConfirmPasswordChanged(it.value) }
                        if (loading) disabled()
                    }
                    if (mismatch) {
                        Div({ classes("form-text", "text-danger") }) {
                            Text(UsersListStrings.passwordMismatch.translation())
                        }
                    }
                }
                Div({ classes("d-flex", "gap-2") }) {
                    Button({
                        classes("btn", "btn-primary")
                        onClick { viewModel.onSave() }
                        if (!canSave) disabled()
                    }) { Text(UsersListStrings.saveButton.translation()) }
                    Button({
                        classes("btn", "btn-danger")
                        onClick { viewModel.onDeleteRequest() }
                        if (loading) disabled()
                    }) { Text(UsersListStrings.deleteButton.translation()) }
                }
            } else {
                Div({ classes("mb-3") }) {
                    Label(null) { Text(UsersListStrings.usernameLabel.translation()) }
                    Input(InputType.Text) {
                        classes("form-control")
                        value(username)
                        attr("readonly", "true")
                        disabled()
                    }
                }
                P({ classes("text-muted") }) { Text(UsersListStrings.noEditableFields.translation()) }
            }
        }
    }

    @Composable
    private fun ConfirmModal(
        title: String,
        message: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        Div({ classes("modal-backdrop", "fade", "show") })
        Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
            Div({ classes("modal-dialog") }) {
                Div({ classes("modal-content") }) {
                    Div({ classes("modal-header") }) {
                        Div({ classes("modal-title", "h5") }) { Text(title) }
                    }
                    Div({ classes("modal-body") }) {
                        P { Text(message) }
                    }
                    Div({ classes("modal-footer") }) {
                        Button({
                            classes("btn", "btn-secondary")
                            onClick { onCancel() }
                        }) { Text(UsersListStrings.cancelButton.translation()) }
                        Button({
                            classes("btn", "btn-danger")
                            onClick { onConfirm() }
                        }) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}
