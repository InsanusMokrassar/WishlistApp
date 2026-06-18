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
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
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

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 {
                        Text(
                            if (viewModel.isCreating) AdminPanelStrings.newUserTitle.translation()
                            else AdminPanelStrings.editUserTitle.translation()
                        )
                    }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                }
            }

            Div({ classes(CalmStudioStyleSheet.form) }) {
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("user-username") { Text(AdminPanelStrings.usernameLabel.translation()) }
                    Input(InputType.Text) {
                        id("user-username")
                        classes(CalmStudioStyleSheet.input)
                        value(username)
                        placeholder(AdminPanelStrings.usernameLabel.translation())
                        onInput { viewModel.onUsernameChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                if (viewModel.isCreating) {
                    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                        Label("user-password") { Text(AdminPanelStrings.passwordLabel.translation()) }
                        Input(InputType.Password) {
                            id("user-password")
                            classes(CalmStudioStyleSheet.input)
                            value(password)
                            placeholder(AdminPanelStrings.passwordLabel.translation())
                            onInput { viewModel.onPasswordChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                }
                Div({ style { property("margin-top", "24px") } }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                        onClick { viewModel.onSave() }
                        if (loading || username.isBlank() || (viewModel.isCreating && password.isBlank())) disabled()
                    }) {
                        Text(AdminPanelStrings.saveButton.translation())
                    }
                }
            }
        }
    }
}
