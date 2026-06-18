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
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlist create/edit screen (Calm Studio form). Owner selection via dropdown. */
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
                            if (viewModel.isCreating) AdminPanelStrings.newWishlistTitle.translation()
                            else AdminPanelStrings.editWishlistTitle.translation()
                        )
                    }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                }
            }

            Div({ classes(CalmStudioStyleSheet.form) }) {
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("wl-title") { Text(AdminPanelStrings.wishlistTitleLabel.translation()) }
                    Input(InputType.Text) {
                        id("wl-title")
                        classes(CalmStudioStyleSheet.input)
                        value(title)
                        placeholder(AdminPanelStrings.wishlistTitleLabel.translation())
                        onInput { viewModel.onTitleChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("wl-owner") { Text(AdminPanelStrings.ownerLabel.translation()) }
                    Select({
                        id("wl-owner")
                        classes(CalmStudioStyleSheet.select)
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
                Div({ style { property("margin-top", "24px") } }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                        onClick { viewModel.onSave() }
                        if (loading || title.isBlank() || selectedUserId == null) disabled()
                    }) {
                        Text(AdminPanelStrings.saveButton.translation())
                    }
                }
            }
        }
    }
}
