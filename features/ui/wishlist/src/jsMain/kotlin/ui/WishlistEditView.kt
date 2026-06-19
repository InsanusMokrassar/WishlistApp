package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmForm
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ConfirmModal
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Div
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the wishlist create/edit screen (Calm Studio form). */
class WishlistEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistEditViewConfig,
) : ComposeView<WishlistEditViewConfig, ViewConfig, WishlistEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistEditView)
    }

    override val title: String
        @Composable get() = if (viewModel.isCreating) WishlistStrings.createWishlistButton.translation()
            else WishlistStrings.editWishlistTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val defaultPriceUnits by viewModel.defaultPriceUnitsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()

        if (showDeleteDialog) {
            ConfirmModal(
                title = WishlistStrings.confirmDeleteWishlistTitle.translation(),
                body = WishlistStrings.confirmDeleteWishlistMessage.translation(),
                confirmLabel = WishlistStrings.confirmDeleteButton.translation(),
                cancelLabel = WishlistStrings.cancelButton.translation(),
                danger = true,
                onCancel = { viewModel.onCancelDelete() },
                onConfirm = { viewModel.onConfirmDelete() },
            )
        }
        if (showDialog) {
            ConfirmModal(
                title = WishlistStrings.confirmDiscardTitle.translation(),
                body = WishlistStrings.confirmDiscardMessage.translation(),
                confirmLabel = WishlistStrings.confirmButton.translation(),
                cancelLabel = WishlistStrings.cancelButton.translation(),
                danger = true,
                onCancel = { viewModel.onCancelBack() },
                onConfirm = { viewModel.onConfirmBack() },
            )
        }

        ContentColumn {
            PageHead(
                title = if (viewModel.isCreating) WishlistStrings.createWishlistButton.translation()
                    else WishlistStrings.editWishlistTitle.translation(),
            )

            CalmForm {
                CalmTextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = WishlistStrings.titleLabel.translation(),
                    placeholder = WishlistStrings.titleLabel.translation(),
                    disabled = loading,
                    id = "wl-title",
                )

                PriceUnitsSelector(
                    label = WishlistStrings.defaultCurrencyLabel.translation(),
                    value = defaultPriceUnits,
                    enabled = !loading,
                    onValueChange = { viewModel.onDefaultPriceUnitsChanged(it) },
                    id = "wl-default-units"
                )

                Div({ classes(CalmStudioStyleSheet.formactions) }) {
                    CalmButton(
                        text = WishlistStrings.saveButton.translation(),
                        onClick = { viewModel.onSave() },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading || title.isBlank(),
                    )
                    CalmButton(
                        text = WishlistStrings.cancelButton.translation(),
                        onClick = { viewModel.onBack() },
                        variant = CalmButtonVariant.Ghost,
                    )
                    if (viewModel.canDelete) {
                        CalmButton(
                            text = WishlistStrings.deleteButton.translation(),
                            onClick = { viewModel.onDelete() },
                            variant = CalmButtonVariant.Danger,
                            leadingIcon = CalmIcons.trash,
                            disabled = loading,
                        )
                    }
                }
            }
        }
    }
}
