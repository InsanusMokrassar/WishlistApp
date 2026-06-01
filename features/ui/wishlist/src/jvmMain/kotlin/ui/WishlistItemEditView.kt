package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewModel
import dev.inmo.wishlist.features.ui.wishlist.utils.pickImageFile
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the wishlist item create/edit screen. */
class WishlistItemEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemEditViewConfig,
) : ComposeView<WishlistItemEditViewConfig, ViewConfig, WishlistItemEditViewModel>(config, chain) {
    override val viewModel: WishlistItemEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val description by viewModel.descriptionState.collectAsState()
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
            AlertDialog(
                onDismissRequest = { viewModel.onCancelDelete() },
                title = { Text(WishlistStrings.confirmDeleteItemTitle.translation()) },
                text = { Text(WishlistStrings.confirmDeleteItemMessage.translation()) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmDelete() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text(WishlistStrings.confirmDeleteButton.translation())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelDelete() }) {
                        Text(WishlistStrings.cancelButton.translation())
                    }
                }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onCancelBack() },
                title = { Text(WishlistStrings.confirmDiscardTitle.translation()) },
                text = { Text(WishlistStrings.confirmDiscardMessage.translation()) },
                confirmButton = {
                    Button(onClick = { viewModel.onConfirmBack() }) {
                        Text(WishlistStrings.confirmButton.translation())
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCancelBack() }) {
                        Text(WishlistStrings.cancelButton.translation())
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                ScreenTitle(
                    if (viewModel.isCreating) WishlistStrings.newItemTitle.translation()
                    else WishlistStrings.editItemTitle.translation()
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text(WishlistStrings.titleLabel.translation()) },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text(WishlistStrings.descriptionLabel.translation()) },
                minLines = 3,
                maxLines = 5,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { viewModel.onPriceChanged(it) },
                    label = { Text(WishlistStrings.priceLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priceUnits,
                    onValueChange = { viewModel.onPriceUnitsChanged(it) },
                    label = { Text(WishlistStrings.priceUnitsLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(WishlistStrings.priorityLabel.translation(), style = MaterialTheme.typography.subtitle2)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip(WishlistStrings.prioritySmall.translation(), priority == Priority.Small, !loading) {
                    viewModel.onPrioritySelected(Priority.Small)
                }
                PriorityChip(WishlistStrings.priorityMedium.translation(), priority == Priority.Medium, !loading) {
                    viewModel.onPrioritySelected(Priority.Medium)
                }
                PriorityChip(WishlistStrings.priorityHigh.translation(), priority == Priority.High, !loading) {
                    viewModel.onPrioritySelected(Priority.High)
                }
                PriorityChip(WishlistStrings.priorityCustom.translation(), priority is Priority.Custom, !loading) {
                    viewModel.onPrioritySelected(Priority.Custom((priority as? Priority.Custom)?.weight ?: 0u))
                }
            }
            if (priority is Priority.Custom) {
                OutlinedTextField(
                    value = (priority as Priority.Custom).weight.toString(),
                    onValueChange = { viewModel.onCustomWeightChanged(it) },
                    label = { Text(WishlistStrings.priorityCustomWeightLabel.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(WishlistStrings.linksLabel.translation(), style = MaterialTheme.typography.subtitle2)
            links.forEachIndexed { index, link ->
                ListRow(
                    trailing = {
                        TextButton(onClick = { viewModel.onRemoveLink(index) }) {
                            Text("×")
                        }
                    }
                ) {
                    Text(link)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newLink,
                    onValueChange = { viewModel.onNewLinkChanged(it) },
                    label = { Text(WishlistStrings.newLinkPlaceholder.translation()) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.onAddLink() },
                    enabled = newLink.isNotBlank()
                ) {
                    Text(WishlistStrings.addLinkButton.translation())
                }
            }
            Text(WishlistStrings.imagesLabel.translation(), style = MaterialTheme.typography.subtitle2)
            if (imageIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imageIds.forEachIndexed { index, id ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RemoteImage(
                                key = id.string,
                                loader = { viewModel.loadImageBytes(id) },
                                contentDescription = null,
                                modifier = Modifier.size(96.dp)
                            )
                            TextButton(onClick = { viewModel.onRemoveImage(index) }, enabled = !loading) {
                                Text(WishlistStrings.removeImageButton.translation())
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { scope.launch { pickImageFile()?.let { viewModel.onAddImage(it) } } },
                enabled = !loading && !uploadingImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uploadingImage) WishlistStrings.uploadingImage.translation()
                    else WishlistStrings.addImageButton.translation()
                )
            }

            Button(
                onClick = { viewModel.onSave() },
                enabled = !loading && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(WishlistStrings.saveButton.translation())
            }
            if (viewModel.canDelete) {
                Button(
                    onClick = { viewModel.onDelete() },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(WishlistStrings.deleteButton.translation())
                }
            }
        }
    }

    /**
     * Renders one priority option as a selectable button.
     *
     * @param label Localized option text.
     * @param selected `true` when this option is the active priority (filled vs outlined).
     * @param enabled Disables the button while a request is in flight.
     * @param onClick Invoked when the option is clicked.
     */
    @Composable
    private fun PriorityChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
        if (selected) {
            Button(onClick = onClick, enabled = enabled) { Text(label) }
        } else {
            OutlinedButton(onClick = onClick, enabled = enabled) { Text(label) }
        }
    }
}
