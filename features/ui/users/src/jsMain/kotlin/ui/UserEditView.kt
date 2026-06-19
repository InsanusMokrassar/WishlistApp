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
                // Avatar section — available to owner and root.
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
                    CalmButton(
                        text = if (uploading) UsersListStrings.uploadingPhoto.translation()
                            else UsersListStrings.uploadPhotoButton.translation(),
                        onClick = { scope.launch { pickImageFile()?.let { viewModel.onAvatarPicked(it) } } },
                        disabled = loading || uploading,
                    )
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
