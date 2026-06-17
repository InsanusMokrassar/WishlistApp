package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import dev.inmo.wishlist.features.ui.users.utils.pickImageFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

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

        Div({ classes("content-inner") }) {
            Div({ classes("pagehead") }) {
                Div {
                    H1 { Text(UsersListStrings.editProfileTitle.translation()) }
                    P({ classes("subline") }) { Text("#${viewModel.userId.long}") }
                }
            }

            Div({ classes("form") }) {
                // Avatar section — available to owner and root.
                Div({ classes("fieldset") }) {
                    Label(null) { Text(UsersListStrings.avatarLabel.translation()) }
                    Div({ style { property("margin-bottom", "8px") } }) {
                        val id = avatarId
                        if (id != null) {
                            Img(src = viewModel.imageUrl(id), alt = UsersListStrings.avatarLabel.translation()) {
                                style {
                                    property("width", "160px")
                                    property("height", "160px")
                                    property("object-fit", "cover")
                                    property("border-radius", "12px")
                                    property("display", "block")
                                }
                            }
                        } else {
                            UserAvatarPlaceholder(
                                sizePx = 160,
                                circle = false,
                                alt = UsersListStrings.avatarPlaceholderAlt.translation()
                            )
                        }
                    }
                    Button({
                        classes("btn")
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
                    Div({ classes("fieldset") }) {
                        Label("settings-username") { Text(UsersListStrings.usernameLabel.translation()) }
                        Input(InputType.Text) {
                            id("settings-username")
                            classes("input")
                            value(username)
                            placeholder(UsersListStrings.usernameLabel.translation())
                            onInput { viewModel.onUsernameChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                    Div({ classes("fieldset") }) {
                        Label("settings-password") { Text(UsersListStrings.newPasswordLabel.translation()) }
                        Input(InputType.Password) {
                            id("settings-password")
                            classes("input")
                            value(password)
                            placeholder(UsersListStrings.newPasswordLabel.translation())
                            onInput { viewModel.onPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                    Div({ classes("fieldset") }) {
                        Label("settings-confirm-password") { Text(UsersListStrings.confirmPasswordLabel.translation()) }
                        Input(InputType.Password) {
                            id("settings-confirm-password")
                            classes("input")
                            value(confirmPassword)
                            placeholder(UsersListStrings.confirmPasswordLabel.translation())
                            onInput { viewModel.onConfirmPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                        if (mismatch) {
                            P({
                                classes("hint")
                                style { property("color", "var(--cs-danger)") }
                            }) { Text(UsersListStrings.passwordMismatch.translation()) }
                        }
                    }
                    Div({
                        style {
                            property("display", "flex")
                            property("gap", "9px")
                            property("margin-top", "24px")
                        }
                    }) {
                        Button({
                            classes("btn", "primary")
                            onClick { viewModel.onSave() }
                            if (!canSave) disabled()
                        }) { Text(UsersListStrings.saveButton.translation()) }
                        Button({
                            classes("btn", "ghost")
                            onClick { viewModel.onBack() }
                        }) { Text(UsersListStrings.backButton.translation()) }
                        Div({ style { property("flex", "1") } })
                        Button({
                            classes("btn", "danger")
                            onClick { viewModel.onDeleteRequest() }
                            if (loading) disabled()
                        }) { Text(UsersListStrings.deleteButton.translation()) }
                    }
                } else {
                    Div({ classes("fieldset") }) {
                        Label("settings-username") { Text(UsersListStrings.usernameLabel.translation()) }
                        Input(InputType.Text) {
                            id("settings-username")
                            classes("input")
                            value(username)
                            attr("readonly", "true")
                            disabled()
                        }
                    }
                    P({ classes("hint") }) { Text(UsersListStrings.noEditableFields.translation()) }
                    Div({ style { property("margin-top", "24px") } }) {
                        Button({
                            classes("btn", "ghost")
                            onClick { viewModel.onBack() }
                        }) { Text(UsersListStrings.backButton.translation()) }
                    }
                }
            }
        }
    }

    /**
     * One Calm `.scrim` confirmation modal (question title + consequence line, ghost Cancel +
     * danger confirm).
     */
    @Composable
    private fun ConfirmModal(
        title: String,
        message: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        Div({
            classes("scrim")
            onClick { onCancel() }
        }) {
            Div({
                classes("modal")
                onClick { it.stopPropagation() }
            }) {
                Div({ classes("mhead") }) {
                    H2 { Text(title) }
                    P { Text(message) }
                }
                Div({ classes("mfoot") }) {
                    Button({
                        classes("btn", "ghost")
                        onClick { onCancel() }
                    }) { Text(UsersListStrings.cancelButton.translation()) }
                    Button({
                        classes("btn", "danger")
                        onClick { onConfirm() }
                    }) { Text(confirmLabel) }
                }
            }
        }
    }
}
