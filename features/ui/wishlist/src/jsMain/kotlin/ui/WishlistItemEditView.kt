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
import dev.inmo.wishlist.features.common.client.ui.components.CalmButton
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonSize
import dev.inmo.wishlist.features.common.client.ui.components.CalmButtonVariant
import dev.inmo.wishlist.features.common.client.ui.components.CalmForm
import dev.inmo.wishlist.features.common.client.ui.components.CalmIcons
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextArea
import dev.inmo.wishlist.features.common.client.ui.components.CalmTextField
import dev.inmo.wishlist.features.common.client.ui.components.ConfirmModal
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.FieldSet
import dev.inmo.wishlist.features.common.client.ui.components.FormHint
import dev.inmo.wishlist.features.common.client.ui.components.FormRow
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.PriorityOptions
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
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the wishlist item create/edit screen (Calm Studio form).
 *
 * Composed from the shared Calm Studio form components ([ContentColumn] + [PageHead] shell, [CalmForm]
 * with [CalmTextField] / [CalmTextArea] / [FieldSet] / [FormRow] / [PriorityOptions] / [FormHint]), plus
 * the "Delete item?" and "Discard changes?" confirmations via [ConfirmModal]. The amount (number),
 * custom-priority weight and link inputs, and the positioned image-remove button keep raw `.input` /
 * `.btn` classes as no component covers them.
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
        // Two links with the same url count as a repeat; saving is blocked until duplicates are removed.
        val hasDuplicateLinks = links.size != links.distinctBy { it.url.trim() }.size

        if (showDeleteDialog) {
            ConfirmModal(
                title = WishlistStrings.confirmDeleteItemTitle.translation(),
                body = WishlistStrings.confirmDeleteItemMessage.translation(),
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
                title = if (viewModel.isCreating) WishlistStrings.newItemTitle.translation()
                    else WishlistStrings.editItemTitle.translation(),
            )

            CalmForm {
                CalmTextField(
                    value = title,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = WishlistStrings.titleLabel.translation(),
                    placeholder = WishlistStrings.titleLabel.translation(),
                    disabled = loading,
                    id = "item-title",
                )

                CalmTextArea(
                    value = description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = WishlistStrings.descriptionLabel.translation(),
                    disabled = loading,
                    id = "item-desc",
                )

                FormRow {
                    CalmTextField(
                        value = price,
                        onValueChange = { viewModel.onPriceChanged(it) },
                        label = WishlistStrings.priceLabel.translation(),
                        placeholder = "0.00",
                        disabled = loading,
                        id = "item-price",
                    )
                    FieldSet(label = WishlistStrings.amountLabel.translation()) {
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

                val customWeight = (priority as? Priority.Custom)?.weight ?: 0u
                FieldSet(label = WishlistStrings.priorityLabel.translation()) {
                    PriorityOptions(
                        options = listOf(Priority.Small, Priority.Medium, Priority.High, Priority.Custom(customWeight)),
                        selected = priority,
                        label = { option ->
                            when (option) {
                                Priority.Small -> WishlistStrings.prioritySmall.translation()
                                Priority.Medium -> WishlistStrings.priorityMedium.translation()
                                Priority.High -> WishlistStrings.priorityHigh.translation()
                                is Priority.Custom -> WishlistStrings.priorityCustom.translation()
                            }
                        },
                        enabled = !loading,
                        onSelect = { viewModel.onPrioritySelected(it) },
                    )
                    if (priority is Priority.Custom) {
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input, WishlistItemEditStyleSheet.customWeight)
                            value((priority as Priority.Custom).weight.toString())
                            placeholder(WishlistStrings.priorityCustomWeightLabel.translation())
                            onInput { viewModel.onCustomWeightChanged(it.value) }
                            if (loading) disabled()
                        }
                    }
                    FormHint(WishlistStrings.priorityHelp.translation())
                }

                FieldSet(label = WishlistStrings.linksLabel.translation()) {
                    links.forEachIndexed { index, link ->
                        Div({ classes(WishlistItemEditStyleSheet.linkRow) }) {
                            Span({ classes(WishlistItemEditStyleSheet.linkText) }) { Text(link.displayText) }
                            CalmButton(
                                text = "×",
                                onClick = { viewModel.onRemoveLink(index) },
                                variant = CalmButtonVariant.Ghost,
                                size = CalmButtonSize.Small,
                            )
                        }
                    }
                    Div({ classes(WishlistItemEditStyleSheet.addLinkRow) }) {
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input)
                            value(newLinkTitle)
                            placeholder(WishlistStrings.linkTitlePlaceholder.translation())
                            onInput { viewModel.onNewLinkTitleChanged(it.value) }
                            if (loading) disabled()
                        }
                        Input(InputType.Text) {
                            classes(CalmStudioStyleSheet.input)
                            value(newLink)
                            placeholder(WishlistStrings.newLinkPlaceholder.translation())
                            onInput { viewModel.onNewLinkChanged(it.value) }
                            if (loading) disabled()
                        }
                        CalmButton(
                            text = WishlistStrings.addLinkButton.translation(),
                            onClick = { viewModel.onAddLink() },
                            disabled = newLink.isBlank(),
                        )
                    }
                    if (hasDuplicateLinks) {
                        P({ classes(WishlistItemEditStyleSheet.dupError) }) {
                            Text(WishlistStrings.duplicateLinksHint.translation())
                        }
                    }
                }

                FieldSet(label = WishlistStrings.imagesLabel.translation()) {
                    if (imageIds.isNotEmpty()) {
                        Div({ classes(WishlistItemEditStyleSheet.imageGrid) }) {
                            imageIds.forEachIndexed { index, id ->
                                Div({ classes(WishlistItemEditStyleSheet.imageCell) }) {
                                    Img(src = viewModel.imageUrl(id), alt = "") {
                                        classes(WishlistItemEditStyleSheet.imageThumb)
                                    }
                                    Button({
                                        classes(CalmStudioStyleSheet.btn, CalmStudioStyleSheet.danger, CalmStudioStyleSheet.sm, WishlistItemEditStyleSheet.imageRemove)
                                        onClick { viewModel.onRemoveImage(index) }
                                        if (loading) disabled()
                                    }) { Text("×") }
                                }
                            }
                        }
                    }
                    CalmButton(
                        text = if (uploadingImage) WishlistStrings.uploadingImage.translation()
                            else WishlistStrings.addImageButton.translation(),
                        onClick = { scope.launch { pickImageFile()?.let { viewModel.onAddImage(it) } } },
                        disabled = loading || uploadingImage,
                    )
                }

                Div({ classes(CalmStudioStyleSheet.formactions) }) {
                    CalmButton(
                        text = WishlistStrings.saveButton.translation(),
                        onClick = { viewModel.onSave() },
                        variant = CalmButtonVariant.Primary,
                        disabled = loading || title.isBlank() || hasDuplicateLinks,
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
