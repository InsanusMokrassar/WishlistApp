package dev.inmo.wishlist.features.ui.adminPanel.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmForm
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Div
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin user create/edit screen (Calm Studio form). */
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
            DiscardModal(
                onCancel = { viewModel.onCancelBack() },
                onConfirm = { viewModel.onConfirmBack() },
            )
        }

        ContentColumn {
            PageHead(
                title = if (viewModel.isCreating) AdminPanelStrings.newUserTitle.translation()
                    else AdminPanelStrings.editUserTitle.translation(),
                actions = { BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() } },
            )

            CalmForm {
                CalmTextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    label = AdminPanelStrings.usernameLabel.translation(),
                    placeholder = AdminPanelStrings.usernameLabel.translation(),
                    disabled = loading,
                    id = "user-username",
                )
                if (viewModel.isCreating) {
                    CalmTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = AdminPanelStrings.passwordLabel.translation(),
                        placeholder = AdminPanelStrings.passwordLabel.translation(),
                        type = InputType.Password,
                        disabled = loading,
                        id = "user-password",
                    )
                }
                Div({ classes(CalmStudioStyleSheet.formactions) }) {
                    CalmButton(
                        text = AdminPanelStrings.saveButton.translation(),
                        onClick = { viewModel.onSave() },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading || username.isBlank() || (viewModel.isCreating && password.isBlank()),
                    )
                }
            }
        }
    }
}
