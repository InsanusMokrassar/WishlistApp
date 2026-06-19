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
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextArea
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.FormRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.dom.Div
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlist item create/edit screen (Calm Studio form). */
class AdminWishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistItemEditViewConfig,
) : ComposeView<AdminWishlistItemEditViewConfig, ViewConfig, AdminWishlistItemEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminWishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistItemEditView)
    }

    override val title: String
        @Composable get() = if (viewModel.isCreating) AdminPanelStrings.newItemTitle.translation()
            else AdminPanelStrings.editItemTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val price by viewModel.priceState.collectAsState()
        val priceUnits by viewModel.priceUnitsState.collectAsState()
        val description by viewModel.descriptionState.collectAsState()
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
                title = if (viewModel.isCreating) AdminPanelStrings.newItemTitle.translation()
                    else AdminPanelStrings.editItemTitle.translation(),
                actions = { BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() } },
            )

            CalmForm {
                CalmTextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = AdminPanelStrings.itemTitleLabel.translation(),
                    placeholder = AdminPanelStrings.itemTitleLabel.translation(),
                    disabled = loading,
                    id = "item-title",
                )
                FormRow {
                    CalmTextField(
                        value = price,
                        onValueChange = { viewModel.onPriceChanged(it) },
                        label = AdminPanelStrings.itemPriceLabel.translation(),
                        placeholder = "0.00",
                        disabled = loading,
                        id = "item-price",
                    )
                    CalmTextField(
                        value = priceUnits,
                        onValueChange = { viewModel.onPriceUnitsChanged(it) },
                        label = AdminPanelStrings.itemPriceUnitsLabel.translation(),
                        placeholder = "$, €, ...",
                        disabled = loading,
                        id = "item-units",
                    )
                }
                CalmTextArea(
                    value = description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = AdminPanelStrings.itemDescriptionLabel.translation(),
                    placeholder = AdminPanelStrings.itemDescriptionLabel.translation(),
                    disabled = loading,
                    id = "item-description",
                )
                Div({ classes(CalmStudioStyleSheet.formactions) }) {
                    CalmButton(
                        text = AdminPanelStrings.saveButton.translation(),
                        onClick = { viewModel.onSave() },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading || title.isBlank(),
                    )
                }
            }
        }
    }
}
