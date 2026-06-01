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
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlist create/edit screen. Owner selection via dropdown. */
class AdminWishlistEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistEditViewConfig,
) : ComposeView<AdminWishlistEditViewConfig, ViewConfig, AdminWishlistEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminWishlistEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistEditView)
    }

    override val title: String
        @Composable get() = if (viewModel.isCreating) AdminPanelStrings.newWishlistTitle.translation()
            else AdminPanelStrings.editWishlistTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val users by viewModel.usersState.collectAsState()
        val selectedUserId by viewModel.selectedUserIdState.collectAsState()
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
                Label("wl-title") { Text(AdminPanelStrings.wishlistTitleLabel.translation()) }
                Input(InputType.Text) {
                    id("wl-title")
                    classes("form-control")
                    value(title)
                    placeholder(AdminPanelStrings.wishlistTitleLabel.translation())
                    onInput { viewModel.onTitleChanged(it.value) }
                    if (loading) disabled()
                }
            }
            Div({ classes("mb-3") }) {
                Label("wl-owner") { Text(AdminPanelStrings.ownerLabel.translation()) }
                Select({
                    id("wl-owner")
                    classes("form-select")
                    if (loading) disabled()
                    onChange { event ->
                        val value = event.value
                        val userId = value?.toLongOrNull()?.let {
                            dev.inmo.wishlist.features.users.common.models.UserId(it)
                        }
                        viewModel.onOwnerSelected(userId)
                    }
                }) {
                    Option("") {
                        Text(AdminPanelStrings.selectOwner.translation())
                    }
                    users.forEach { user ->
                        Option(
                            user.id.long.toString(),
                            {
                                if (selectedUserId == user.id) {
                                    attr("selected", "selected")
                                }
                            }
                        ) {
                            Text("${user.username.string} (#${user.id.long})")
                        }
                    }
                }
            }
            Button({
                classes("btn", "btn-primary")
                onClick { viewModel.onSave() }
                if (loading || title.isBlank() || selectedUserId == null) disabled()
            }) {
                Text(AdminPanelStrings.saveButton.translation())
            }
        }
    }
}
