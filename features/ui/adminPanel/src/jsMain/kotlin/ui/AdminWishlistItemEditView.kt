package dev.inmo.wishlist.features.ui.adminPanel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the admin wishlist item create/edit screen. Uses Bootstrap classes. */
class AdminWishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: AdminWishlistItemEditViewConfig,
) : ComposeView<AdminWishlistItemEditViewConfig, ViewConfig, AdminWishlistItemEditViewModel>(config, chain) {
    override val viewModel: AdminWishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminWishlistItemEditView)
    }

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
                Button({
                    classes("btn", "btn-outline-secondary")
                    onClick { viewModel.onBack() }
                }) { Text(AdminPanelStrings.backButton.translation()) }
                H1({ classes("h3", "mb-0") }) {
                    Text(
                        if (viewModel.isCreating) AdminPanelStrings.newItemTitle.translation()
                        else AdminPanelStrings.editItemTitle.translation()
                    )
                }
            }
            Div({ classes("mb-3") }) {
                Label("item-title") { Text(AdminPanelStrings.itemTitleLabel.translation()) }
                Input(InputType.Text) {
                    id("item-title")
                    classes("form-control")
                    value(title)
                    placeholder(AdminPanelStrings.itemTitleLabel.translation())
                    onInput { viewModel.onTitleChanged(it.value) }
                    if (loading) disabled()
                }
            }
            Div({ classes("row", "mb-3") }) {
                Div({ classes("col") }) {
                    Label("item-price") { Text(AdminPanelStrings.itemPriceLabel.translation()) }
                    Input(InputType.Text) {
                        id("item-price")
                        classes("form-control")
                        value(price)
                        placeholder("0.00")
                        onInput { viewModel.onPriceChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ classes("col") }) {
                    Label("item-units") { Text(AdminPanelStrings.itemPriceUnitsLabel.translation()) }
                    Input(InputType.Text) {
                        id("item-units")
                        classes("form-control")
                        value(priceUnits)
                        placeholder("$, €, ...")
                        onInput { viewModel.onPriceUnitsChanged(it.value) }
                        if (loading) disabled()
                    }
                }
            }
            Div({ classes("mb-3") }) {
                Label("item-description") { Text(AdminPanelStrings.itemDescriptionLabel.translation()) }
                TextArea({
                    id("item-description")
                    classes("form-control")
                    value(description)
                    placeholder(AdminPanelStrings.itemDescriptionLabel.translation())
                    onInput { viewModel.onDescriptionChanged(it.value) }
                    if (loading) disabled()
                })
            }
            Button({
                classes("btn", "btn-primary")
                onClick { viewModel.onSave() }
                if (loading || title.isBlank()) disabled()
            }) {
                Text(AdminPanelStrings.saveButton.translation())
            }
        }
    }
}
