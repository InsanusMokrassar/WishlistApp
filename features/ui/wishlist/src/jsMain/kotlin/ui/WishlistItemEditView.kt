package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.utils.pickImageFile
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the wishlist item create/edit screen. Uses Bootstrap classes. */
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
        val imageIds by viewModel.imageIdsState.collectAsState()
        val uploadingImage by viewModel.uploadingImageState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()
        val scope = rememberCoroutineScope()

        if (showDeleteDialog) {
            Div({ classes("modal-backdrop", "fade", "show") })
            Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
                Div({ classes("modal-dialog") }) {
                    Div({ classes("modal-content") }) {
                        Div({ classes("modal-header") }) {
                            Div({ classes("modal-title", "h5") }) {
                                Text(WishlistStrings.confirmDeleteItemTitle.translation())
                            }
                        }
                        Div({ classes("modal-body") }) {
                            P { Text(WishlistStrings.confirmDeleteItemMessage.translation()) }
                        }
                        Div({ classes("modal-footer") }) {
                            Button({
                                classes("btn", "btn-secondary")
                                onClick { viewModel.onCancelDelete() }
                            }) { Text(WishlistStrings.cancelButton.translation()) }
                            Button({
                                classes("btn", "btn-danger")
                                onClick { viewModel.onConfirmDelete() }
                            }) { Text(WishlistStrings.confirmDeleteButton.translation()) }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            Div({ classes("modal-backdrop", "fade", "show") })
            Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
                Div({ classes("modal-dialog") }) {
                    Div({ classes("modal-content") }) {
                        Div({ classes("modal-header") }) {
                            Div({ classes("modal-title", "h5") }) {
                                Text(WishlistStrings.confirmDiscardTitle.translation())
                            }
                        }
                        Div({ classes("modal-body") }) {
                            P { Text(WishlistStrings.confirmDiscardMessage.translation()) }
                        }
                        Div({ classes("modal-footer") }) {
                            Button({
                                classes("btn", "btn-secondary")
                                onClick { viewModel.onCancelBack() }
                            }) { Text(WishlistStrings.cancelButton.translation()) }
                            Button({
                                classes("btn", "btn-danger")
                                onClick { viewModel.onConfirmBack() }
                            }) { Text(WishlistStrings.confirmButton.translation()) }
                        }
                    }
                }
            }
        }

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
            }

            Div({ classes("mb-3") }) {
                Label("item-title") { Text(WishlistStrings.titleLabel.translation()) }
                Input(InputType.Text) {
                    id("item-title")
                    classes("form-control")
                    value(title)
                    placeholder(WishlistStrings.titleLabel.translation())
                    onInput { viewModel.onTitleChanged(it.value) }
                    if (loading) disabled()
                }
            }

            Div({ classes("mb-3") }) {
                Label("item-desc") { Text(WishlistStrings.descriptionLabel.translation()) }
                TextArea {
                    id("item-desc")
                    classes("form-control")
                    value(description)
                    onInput { viewModel.onDescriptionChanged(it.value) }
                    if (loading) disabled()
                }
            }

            Div({ classes("mb-3") }) {
                Label("item-amount") { Text(WishlistStrings.amountLabel.translation()) }
                Input(InputType.Text) {
                    id("item-amount")
                    classes("form-control")
                    value(amount)
                    attr("inputmode", "numeric")
                    attr("min", "1")
                    placeholder("1")
                    onInput { viewModel.onAmountChanged(it.value) }
                    if (loading) disabled()
                }
            }

            Div({ classes("row", "mb-3") }) {
                Div({ classes("col") }) {
                    Label("item-price") { Text(WishlistStrings.priceLabel.translation()) }
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
                    PriceUnitsSelector(
                        label = WishlistStrings.priceUnitsLabel.translation(),
                        value = priceUnits,
                        enabled = !loading,
                        onValueChange = { viewModel.onPriceUnitsChanged(it) },
                        id = "item-units"
                    )
                }
            }

            Div({ classes("mb-3") }) {
                Label { Text(WishlistStrings.priorityLabel.translation()) }
                Div({ classes("btn-group", "d-flex", "mb-2"); attr("role", "group") }) {
                    PriorityButton(WishlistStrings.prioritySmall.translation(), priority == Priority.Small, loading) {
                        viewModel.onPrioritySelected(Priority.Small)
                    }
                    PriorityButton(WishlistStrings.priorityMedium.translation(), priority == Priority.Medium, loading) {
                        viewModel.onPrioritySelected(Priority.Medium)
                    }
                    PriorityButton(WishlistStrings.priorityHigh.translation(), priority == Priority.High, loading) {
                        viewModel.onPrioritySelected(Priority.High)
                    }
                    PriorityButton(WishlistStrings.priorityCustom.translation(), priority is Priority.Custom, loading) {
                        viewModel.onPrioritySelected(Priority.Custom((priority as? Priority.Custom)?.weight ?: 0u))
                    }
                }
                if (priority is Priority.Custom) {
                    Input(InputType.Text) {
                        classes("form-control")
                        value((priority as Priority.Custom).weight.toString())
                        placeholder(WishlistStrings.priorityCustomWeightLabel.translation())
                        onInput { viewModel.onCustomWeightChanged(it.value) }
                        if (loading) disabled()
                    }
                }
            }

            Div({ classes("mb-3") }) {
                Label { Text(WishlistStrings.linksLabel.translation()) }
                if (links.isNotEmpty()) {
                    Ul({ classes("list-group", "mb-2") }) {
                        links.forEachIndexed { index, link ->
                            ListRow(
                                trailing = {
                                    Button({
                                        classes("btn", "btn-sm", "btn-outline-danger")
                                        onClick { viewModel.onRemoveLink(index) }
                                    }) { Text("×") }
                                }
                            ) {
                                Span({ classes("text-truncate", "me-2") }) { Text(link) }
                            }
                        }
                    }
                }
                Div({ classes("input-group") }) {
                    Input(InputType.Text) {
                        classes("form-control")
                        value(newLink)
                        placeholder(WishlistStrings.newLinkPlaceholder.translation())
                        onInput { viewModel.onNewLinkChanged(it.value) }
                        if (loading) disabled()
                    }
                    Button({
                        classes("btn", "btn-outline-secondary")
                        onClick { viewModel.onAddLink() }
                        if (newLink.isBlank()) disabled()
                    }) {
                        Text(WishlistStrings.addLinkButton.translation())
                    }
                }
            }

            Div({ classes("mb-3") }) {
                Label { Text(WishlistStrings.imagesLabel.translation()) }
                if (imageIds.isNotEmpty()) {
                    Div({ classes("d-flex", "flex-wrap", "gap-2", "mb-2") }) {
                        imageIds.forEachIndexed { index, id ->
                            Div({ classes("position-relative") }) {
                                Img(src = viewModel.imageUrl(id), alt = "") {
                                    classes("rounded", "border")
                                    attr("width", "96")
                                    attr("height", "96")
                                    attr("style", "object-fit: cover;")
                                }
                                Button({
                                    classes("btn", "btn-sm", "btn-danger", "position-absolute", "top-0", "end-0")
                                    onClick { viewModel.onRemoveImage(index) }
                                    if (loading) disabled()
                                }) { Text("×") }
                            }
                        }
                    }
                }
                Button({
                    classes("btn", "btn-outline-secondary")
                    onClick { scope.launch { pickImageFile()?.let { viewModel.onAddImage(it) } } }
                    if (loading || uploadingImage) disabled()
                }) {
                    Text(
                        if (uploadingImage) WishlistStrings.uploadingImage.translation()
                        else WishlistStrings.addImageButton.translation()
                    )
                }
            }

            Div({ classes("d-flex", "gap-2") }) {
                Button({
                    classes("btn", "btn-primary")
                    onClick { viewModel.onSave() }
                    if (loading || title.isBlank()) disabled()
                }) {
                    Text(WishlistStrings.saveButton.translation())
                }
                if (viewModel.canDelete) {
                    Button({
                        classes("btn", "btn-danger")
                        onClick { viewModel.onDelete() }
                        if (loading) disabled()
                    }) {
                        Text(WishlistStrings.deleteButton.translation())
                    }
                }
            }
        }
    }

    /**
     * Renders one priority option as a Bootstrap toggle button.
     *
     * @param label Localized option text.
     * @param selected `true` when this option is the active priority.
     * @param loading Disables the button while a request is in flight.
     * @param action Invoked when the option is clicked.
     */
    @Composable
    private fun PriorityButton(label: String, selected: Boolean, loading: Boolean, action: () -> Unit) {
        Button({
            classes("btn", if (selected) "btn-primary" else "btn-outline-primary")
            onClick { action() }
            if (loading) disabled()
        }) {
            Text(label)
        }
    }
}
