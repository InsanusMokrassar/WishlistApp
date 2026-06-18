package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
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

    /**
     * One Calm `.scrim` confirmation modal.
     *
     * @param title Question-style heading.
     * @param body Plain-language consequence line.
     * @param confirmLabel Label restating the destructive/confirming verb.
     * @param onCancel Invoked when the user dismisses the modal.
     * @param onConfirm Invoked when the user confirms the action.
     */
    @Composable
    private fun ConfirmModal(title: String, body: String, confirmLabel: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
        Div({
            classes(CalmStudioStyleSheet.scrim)
            onClick { onCancel() }
        }) {
            Div({
                classes(CalmStudioStyleSheet.modal)
                onClick { it.stopPropagation() }
            }) {
                Div({ classes(CalmStudioStyleSheet.mhead) }) {
                    H2 { Text(title) }
                    P { Text(body) }
                }
                Div({ classes(CalmStudioStyleSheet.mfoot) }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
                        onClick { onCancel() }
                    }) { Text(WishlistStrings.cancelButton.translation()) }
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.danger)
                        onClick { onConfirm() }
                    }) { Text(confirmLabel) }
                }
            }
        }
    }

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
                onCancel = { viewModel.onCancelDelete() },
                onConfirm = { viewModel.onConfirmDelete() }
            )
        }
        if (showDialog) {
            ConfirmModal(
                title = WishlistStrings.confirmDiscardTitle.translation(),
                body = WishlistStrings.confirmDiscardMessage.translation(),
                confirmLabel = WishlistStrings.confirmButton.translation(),
                onCancel = { viewModel.onCancelBack() },
                onConfirm = { viewModel.onConfirmBack() }
            )
        }

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 {
                        Text(
                            if (viewModel.isCreating) WishlistStrings.createWishlistButton.translation()
                            else WishlistStrings.editWishlistTitle.translation()
                        )
                    }
                }
            }

            Div({ classes(CalmStudioStyleSheet.form) }) {
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("wl-title") { Text(WishlistStrings.titleLabel.translation()) }
                    Input(InputType.Text) {
                        id("wl-title")
                        classes(CalmStudioStyleSheet.input)
                        value(title)
                        placeholder(WishlistStrings.titleLabel.translation())
                        onInput { viewModel.onTitleChanged(it.value) }
                        if (loading) disabled()
                    }
                }

                PriceUnitsSelector(
                    label = WishlistStrings.defaultCurrencyLabel.translation(),
                    value = defaultPriceUnits,
                    enabled = !loading,
                    onValueChange = { viewModel.onDefaultPriceUnitsChanged(it) },
                    id = "wl-default-units"
                )

                Div({
                    style {
                        property("display", "flex")
                        property("gap", "9px")
                        property("margin-top", "24px")
                    }
                }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                        onClick { viewModel.onSave() }
                        if (loading || title.isBlank()) disabled()
                    }) { Text(WishlistStrings.saveButton.translation()) }
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost)
                        onClick { viewModel.onBack() }
                    }) { Text(WishlistStrings.cancelButton.translation()) }
                    Div({ style { property("flex", "1") } })
                    if (viewModel.canDelete) {
                        Button({
                            classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.danger)
                            onClick { viewModel.onDelete() }
                            if (loading) disabled()
                        }) {
                            CalmIcon(CalmIcons.trash)
                            Text(WishlistStrings.deleteButton.translation())
                        }
                    }
                }
            }
        }
    }
}
