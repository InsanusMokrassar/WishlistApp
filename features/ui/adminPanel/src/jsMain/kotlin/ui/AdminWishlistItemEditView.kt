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
import org.jetbrains.compose.web.dom.TextArea
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

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 {
                        Text(
                            if (viewModel.isCreating) AdminPanelStrings.newItemTitle.translation()
                            else AdminPanelStrings.editItemTitle.translation()
                        )
                    }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                }
            }

            Div({ classes(CalmStudioStyleSheet.form) }) {
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("item-title") { Text(AdminPanelStrings.itemTitleLabel.translation()) }
                    Input(InputType.Text) {
                        id("item-title")
                        classes(CalmStudioStyleSheet.input)
                        value(title)
                        placeholder(AdminPanelStrings.itemTitleLabel.translation())
                        onInput { viewModel.onTitleChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ classes(CalmStudioStyleSheet.`form-row`) }) {
                    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                        Label("item-price") { Text(AdminPanelStrings.itemPriceLabel.translation()) }
                        Input(InputType.Text) {
                            id("item-price")
                            classes(CalmStudioStyleSheet.input)
                            value(price)
                            placeholder("0.00")
                            onInput { viewModel.onPriceChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                        Label("item-units") { Text(AdminPanelStrings.itemPriceUnitsLabel.translation()) }
                        Input(InputType.Text) {
                            id("item-units")
                            classes(CalmStudioStyleSheet.input)
                            value(priceUnits)
                            placeholder("$, €, ...")
                            onInput { viewModel.onPriceUnitsChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                }
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("item-description") { Text(AdminPanelStrings.itemDescriptionLabel.translation()) }
                    TextArea(description) {
                        id("item-description")
                        classes(CalmStudioStyleSheet.textarea)
                        value(description)
                        placeholder(AdminPanelStrings.itemDescriptionLabel.translation())
                        onInput { viewModel.onDescriptionChanged(it.value) }
                        if (loading) disabled()
                    }
                }
                Div({ style { property("margin-top", "24px") } }) {
                    Button({
                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.primary)
                        onClick { viewModel.onSave() }
                        if (loading || title.isBlank()) disabled()
                    }) {
                        Text(AdminPanelStrings.saveButton.translation())
                    }
                }
            }
        }
    }
}
