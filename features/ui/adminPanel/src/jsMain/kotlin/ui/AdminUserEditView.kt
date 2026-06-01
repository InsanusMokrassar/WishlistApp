package dev.inmo.wishlist.features.ui.adminPanel.ui

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
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin user create/edit screen. Uses Bootstrap classes. */
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
            Div({ classes("modal-backdrop", "fade", "show") })
            Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
                Div({ classes("modal-dialog") }) {
                    Div({ classes("modal-content") }) {
                        Div({ classes("modal-header") }) {
                            Div({ classes("modal-title", "h5") }) {
                                Text(AdminPanelStrings.confirmDiscardTitle.translation())
                            }
                        }
                        Div({ classes("modal-body") }) {
                            P { Text(AdminPanelStrings.confirmDiscardMessage.translation()) }
                        }
                        Div({ classes("modal-footer") }) {
                            Button({
                                classes("btn", "btn-secondary")
                                onClick { viewModel.onCancelBack() }
                            }) { Text(AdminPanelStrings.cancelButton.translation()) }
                            Button({
                                classes("btn", "btn-danger")
                                onClick { viewModel.onConfirmBack() }
                            }) { Text(AdminPanelStrings.confirmButton.translation()) }
                        }
                    }
                }
            }
        }

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
            }
            Div({ classes("mb-3") }) {
                Label("user-username") { Text(AdminPanelStrings.usernameLabel.translation()) }
                Input(InputType.Text) {
                    id("user-username")
                    classes("form-control")
                    value(username)
                    placeholder(AdminPanelStrings.usernameLabel.translation())
                    onInput { viewModel.onUsernameChanged(it.value) }
                    if (loading) disabled()
                }
            }
            if (viewModel.isCreating) {
                Div({ classes("mb-3") }) {
                    Label("user-password") { Text(AdminPanelStrings.passwordLabel.translation()) }
                    Input(InputType.Password) {
                        id("user-password")
                        classes("form-control")
                        value(password)
                        placeholder(AdminPanelStrings.passwordLabel.translation())
                        onInput { viewModel.onPasswordChanged(it.value) }
                        if (loading) disabled()
                    }
                }
            }
            Button({
                classes("btn", "btn-primary")
                onClick { viewModel.onSave() }
                if (loading || username.isBlank() || (viewModel.isCreating && password.isBlank())) disabled()
            }) {
                Text(AdminPanelStrings.saveButton.translation())
            }
        }
    }
}
