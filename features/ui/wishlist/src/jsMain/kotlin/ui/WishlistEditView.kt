package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewModel
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.forId
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the wishlist create/edit screen. Uses Bootstrap classes. */
class WishlistEditView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistEditViewConfig,
) : ComposeView<WishlistEditViewConfig, ViewConfig, WishlistEditViewModel>(config, chain) {
    override val viewModel: WishlistEditViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistEditView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val title by viewModel.titleState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val showDialog by viewModel.showConfirmDialogState.collectAsState()
        val showDeleteDialog by viewModel.showDeleteDialogState.collectAsState()

        if (showDeleteDialog) {
            Div({ classes("modal-backdrop", "fade", "show") })
            Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
                Div({ classes("modal-dialog") }) {
                    Div({ classes("modal-content") }) {
                        Div({ classes("modal-header") }) {
                            Div({ classes("modal-title", "h5") }) {
                                Text(WishlistStrings.confirmDeleteWishlistTitle.translation())
                            }
                        }
                        Div({ classes("modal-body") }) {
                            P { Text(WishlistStrings.confirmDeleteWishlistMessage.translation()) }
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
                ScreenTitle(
                    if (viewModel.isCreating) WishlistStrings.newWishlistTitle.translation()
                    else WishlistStrings.editWishlistTitle.translation()
                )
            }
            Div({ classes("mb-3") }) {
                Label("wl-title") {
                    Text(WishlistStrings.titleLabel.translation())
                }
                Input(InputType.Text) {
                    id("wl-title")
                    classes("form-control")
                    value(title)
                    placeholder(WishlistStrings.titleLabel.translation())
                    onInput { viewModel.onTitleChanged(it.value) }
                    if (loading) disabled()
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
}
