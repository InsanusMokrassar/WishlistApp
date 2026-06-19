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
import dev.inmo.wishlist.features.common.client.ui.components.FieldSet
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Div
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

        ContentColumn {
            PageHead(
                title = if (viewModel.isCreating) AdminPanelStrings.newWishlistTitle.translation()
                    else AdminPanelStrings.editWishlistTitle.translation(),
                actions = { BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() } },
            )

            CalmForm {
                CalmTextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = AdminPanelStrings.wishlistTitleLabel.translation(),
                    placeholder = AdminPanelStrings.wishlistTitleLabel.translation(),
                    disabled = loading,
                    id = "wl-title",
                )
                FieldSet(label = AdminPanelStrings.ownerLabel.translation()) {
                    Select({
                        id("wl-owner")
                        classes(CalmStudioStyleSheet.select)
                        if (loading) disabled()
                        onChange { event ->
                            val value = event.value
                            val userId = value?.toLongOrNull()?.let { UserId(it) }
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
                Div({ classes(CalmStudioStyleSheet.formactions) }) {
                    CalmButton(
                        text = AdminPanelStrings.saveButton.translation(),
                        onClick = { viewModel.onSave() },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading || title.isBlank() || selectedUserId == null,
                    )
                }
            }
        }
    }
}
