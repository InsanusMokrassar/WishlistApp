package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcon
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.utils.pickImageFile
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.displayText
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlist item create/edit screen (Calm Studio form).
 *
 * Renders a `.form` of `.fieldset`s — title, description, price + amount + units, a `.priopts`
 * segmented priority control, links and images — with Save / Cancel / Delete actions, plus the
 * "Delete item?" and "Discard changes?" confirmations as Calm `.scrim` modals. Class names mirror the
 * design skill's `app.jsx` so the Calm Studio shell CSS styles the screen directly.
 */
class WishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemEditViewConfig,
) : ComposeView<WishlistItemEditViewConfig, ViewConfig, WishlistItemEditViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemEditView)
    }

    override val title: String
        @Composable get() = if (viewModel.isCreating) WishlistStrings.newItemTitle.translation()
            else WishlistStrings.editItemTitle.translation()

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
        val description by viewModel.descriptionState.collectAsState()
        val amount by viewModel.amountState.collectAsState()
        val price by viewModel.priceState.collectAsState()
        val priceUnits by viewModel.priceUnitsState.collectAsState()
        val priority by viewModel.priorityState.collectAsState()
        val links by viewModel.linksState.collectAsState()
        val newLink by viewModel.newLinkState.collectAsState()
        val newLinkTitle by viewModel.newLinkTitleState.collectAsState()
        val imageIds by viewModel.imageIdsState.collectAsState()
        val uploadingImage by viewModel.uploadingImageState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()
        val scope = rememberCoroutineScope()

        if (showDeleteDialog) {
            ConfirmModal(
                title = WishlistStrings.confirmDeleteItemTitle.translation(),
                body = WishlistStrings.confirmDeleteItemMessage.translation(),
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
                            if (viewModel.isCreating) WishlistStrings.newItemTitle.translation()
                            else WishlistStrings.editItemTitle.translation()
                        )
                    }
                }
            }

            Div({ classes(CalmStudioStyleSheet.form) }) {
                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("item-title") { Text(WishlistStrings.titleLabel.translation()) }
                    Input(InputType.Text) {
                        id("item-title")
                        classes(CalmStudioStyleSheet.input)
                        value(title)
                        placeholder(WishlistStrings.titleLabel.translation())
                        onInput { viewModel.onTitleChanged(it.value) }
                        if (loading) disabled()
                    }
                }

                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label("item-desc") { Text(WishlistStrings.descriptionLabel.translation()) }
                    TextArea {
                        id("item-desc")
                        classes(CalmStudioStyleSheet.textarea)
                        value(description)
                        onInput { viewModel.onDescriptionChanged(it.value) }
                        if (loading) disabled()
                    }
                }

                Div({ classes(CalmStudioStyleSheet.`form-row`) }) {
                    Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                        Label("item-price") { Text(WishlistStrings.priceLabel.translation()) }
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
                        Label("item-amount") { Text(WishlistStrings.amountLabel.translation()) }
                        Input(InputType.Text) {
                            id("item-amount")
                            classes(CalmStudioStyleSheet.input)
                            value(amount)
                            attr("type", "number")
                            attr("inputmode", "numeric")
                            attr("min", "1")
                            attr("step", "1")
                            placeholder("1")
                            onInput { viewModel.onAmountChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                }

                PriceUnitsSelector(
                    label = WishlistStrings.priceUnitsLabel.translation(),
                    value = priceUnits,
                    enabled = !loading,
                    onValueChange = { viewModel.onPriceUnitsChanged(it) },
                    id = "item-units"
                )

                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label { Text(WishlistStrings.priorityLabel.translation()) }
                    Div({ classes(CalmStudioStyleSheet.priopts) }) {
                        PriorityOption(WishlistStrings.prioritySmall.translation(), priority == Priority.Small, loading) {
                            viewModel.onPrioritySelected(Priority.Small)
                        }
                        PriorityOption(WishlistStrings.priorityMedium.translation(), priority == Priority.Medium, loading) {
                            viewModel.onPrioritySelected(Priority.Medium)
                        }
                        PriorityOption(WishlistStrings.priorityHigh.translation(), priority == Priority.High, loading) {
                            viewModel.onPrioritySelected(Priority.High)
                        }
                        PriorityOption(WishlistStrings.priorityCustom.translation(), priority is Priority.Custom, loading) {
                            viewModel.onPrioritySelected(Priority.Custom((priority as? Priority.Custom)?.weight ?: 0u))
                        }
                    }
                    if (priority is Priority.Custom) {
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input)
                            style { property("margin-top", "8px") }
                            value((priority as Priority.Custom).weight.toString())
                            placeholder(WishlistStrings.priorityCustomWeightLabel.translation())
                            onInput { viewModel.onCustomWeightChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                    P({ classes(CalmStudioStyleSheet.hint) }) { Text(WishlistStrings.priorityHelp.translation()) }
                }

                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label { Text(WishlistStrings.linksLabel.translation()) }
                    links.forEachIndexed { index, link ->
                        Div({
                            style {
                                property("display", "flex")
                                property("align-items", "center")
                                property("gap", "8px")
                                property("margin-bottom", "6px")
                            }
                        }) {
                            Span({ style { property("flex", "1"); property("min-width", "0") } }) { Text(link.displayText) }
                            Button({
                                classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.ghost, CalmStudioStyleSheet.sm)
                                onClick { viewModel.onRemoveLink(index) }
                            }) { Text("×") }
                        }
                    }
                    Div({
                        style {
                            property("display", "flex")
                            property("gap", "8px")
                        }
                    }) {
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input)
                            value(newLink)
                            placeholder(WishlistStrings.newLinkPlaceholder.translation())
                            onInput { viewModel.onNewLinkChanged(it.value) }
                            if (loading) disabled()
                        }
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input)
                            value(newLinkTitle)
                            placeholder(WishlistStrings.linkTitlePlaceholder.translation())
                            onInput { viewModel.onNewLinkTitleChanged(it.value) }
                            if (loading) disabled()
                        }
                        Button({
                            classes(CalmStudioStyleSheet.btn)
                            onClick { viewModel.onAddLink() }
                            if (newLink.isBlank()) disabled()
                        }) { Text(WishlistStrings.addLinkButton.translation()) }
                    }
                }

                Div({ classes(CalmStudioStyleSheet.fieldset) }) {
                    Label { Text(WishlistStrings.imagesLabel.translation()) }
                    if (imageIds.isNotEmpty()) {
                        Div({
                            style {
                                property("display", "flex")
                                property("flex-wrap", "wrap")
                                property("gap", "8px")
                                property("margin-bottom", "8px")
                            }
                        }) {
                            imageIds.forEachIndexed { index, id ->
                                Div({ style { property("position", "relative") } }) {
                                    Img(src = viewModel.imageUrl(id), alt = "") {
                                        style {
                                            property("width", "96px")
                                            property("height", "96px")
                                            property("object-fit", "cover")
                                            property("border-radius", "10px")
                                        }
                                    }
                                    Button({
                                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.danger, CalmStudioStyleSheet.sm)
                                        style {
                                            property("position", "absolute")
                                            property("top", "4px")
                                            property("right", "4px")
                                        }
                                        onClick { viewModel.onRemoveImage(index) }
                                        if (loading) disabled()
                                    }) { Text("×") }
                                }
                            }
                        }
                    }
                    Button({
                        classes(CalmStudioStyleSheet.btn)
                        onClick { scope.launch { pickImageFile()?.let { viewModel.onAddImage(it) } } }
                        if (loading || uploadingImage) disabled()
                    }) {
                        Text(
                            if (uploadingImage) WishlistStrings.uploadingImage.translation()
                            else WishlistStrings.addImageButton.translation()
                        )
                    }
                }

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

    /**
     * Renders one priority option as a Calm Studio `.priopt` segment.
     *
     * @param label Localized option text.
     * @param selected `true` when this option is the active priority (adds the `on` class).
     * @param loading Ignores clicks while a request is in flight.
     * @param action Invoked when the option is clicked.
     */
    @Composable
    private fun PriorityOption(label: String, selected: Boolean, loading: Boolean, action: () -> Unit) {
        Div({
            if (selected) classes(CalmStudioStyleSheet.priopt, CalmStudioStyleSheet.on) else classes(CalmStudioStyleSheet.priopt)
            if (!loading) onClick { action() }
        }) {
            Text(label)
        }
    }
}
