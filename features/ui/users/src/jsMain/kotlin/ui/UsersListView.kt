package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML view for the users list screen. Uses Bootstrap classes. */
class UsersListView(
    chain: NavigationChain<ViewConfig>,
    config: UsersListViewConfig,
) : ComposeView<UsersListViewConfig, ViewConfig, UsersListViewModel>(config, chain) {
    override val viewModel: UsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UsersListView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val isRoot by viewModel.isRootState.collectAsState()
        val deleteTarget by viewModel.deleteTargetState.collectAsState()
        val deleteStep by viewModel.deleteStepState.collectAsState()

        deleteTarget?.let { target ->
            if (deleteStep == 1) {
                ConfirmModal(
                    title = UsersListStrings.confirmDeleteUserTitle.translation(),
                    message = "${UsersListStrings.confirmDeleteUserMessageFirst.translation()} ${target.username.string}",
                    confirmLabel = UsersListStrings.continueButton.translation(),
                    onConfirm = { viewModel.onConfirmDeleteFirst() },
                    onCancel = { viewModel.onCancelDelete() }
                )
            } else if (deleteStep == 2) {
                ConfirmModal(
                    title = UsersListStrings.confirmDeleteUserFinalTitle.translation(),
                    message = "${UsersListStrings.confirmDeleteUserMessageSecond.translation()} ${target.username.string}",
                    confirmLabel = UsersListStrings.confirmDeleteButton.translation(),
                    onConfirm = { viewModel.onConfirmDeleteSecond() },
                    onCancel = { viewModel.onCancelDelete() }
                )
            }
        }

        Div({ classes("container", "py-3") }) {
            H1({ classes("h3", "mb-3") }) { Text(UsersListStrings.title.translation()) }
            if (loading) {
                P { Text(UsersListStrings.loading.translation()) }
            } else if (users.isEmpty()) {
                P({ classes("text-muted") }) { Text(UsersListStrings.empty.translation()) }
            } else {
                Ul({ classes("list-group") }) {
                    users.forEach { user ->
                        Li({
                            classes("list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center")
                        }) {
                            Span({
                                style { property("cursor", "pointer") }
                                onClick { viewModel.onUserSelected(user.id) }
                            }) { Text(user.username.string) }
                            if (isRoot) {
                                Button({
                                    classes("btn", "btn-sm", "btn-danger")
                                    onClick { viewModel.onDeleteUserRequest(user) }
                                }) { Text(UsersListStrings.deleteButton.translation()) }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfirmModal(
        title: String,
        message: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        Div({ classes("modal-backdrop", "fade", "show") })
        Div({ classes("modal", "d-block"); attr("tabindex", "-1") }) {
            Div({ classes("modal-dialog") }) {
                Div({ classes("modal-content") }) {
                    Div({ classes("modal-header") }) {
                        Div({ classes("modal-title", "h5") }) { Text(title) }
                    }
                    Div({ classes("modal-body") }) {
                        P { Text(message) }
                    }
                    Div({ classes("modal-footer") }) {
                        Button({
                            classes("btn", "btn-secondary")
                            onClick { onCancel() }
                        }) { Text(UsersListStrings.cancelButton.translation()) }
                        Button({
                            classes("btn", "btn-danger")
                            onClick { onConfirm() }
                        }) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}
