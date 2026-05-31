package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the users list screen. */
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
            when (deleteStep) {
                1 -> ConfirmDialog(
                    title = UsersListStrings.confirmDeleteUserTitle.translation(),
                    message = "${UsersListStrings.confirmDeleteUserMessageFirst.translation()} ${target.username.string}",
                    confirmLabel = UsersListStrings.continueButton.translation(),
                    onConfirm = { viewModel.onConfirmDeleteFirst() },
                    onCancel = { viewModel.onCancelDelete() }
                )
                2 -> ConfirmDialog(
                    title = UsersListStrings.confirmDeleteUserFinalTitle.translation(),
                    message = "${UsersListStrings.confirmDeleteUserMessageSecond.translation()} ${target.username.string}",
                    confirmLabel = UsersListStrings.confirmDeleteButton.translation(),
                    onConfirm = { viewModel.onConfirmDeleteSecond() },
                    onCancel = { viewModel.onCancelDelete() }
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScreenTitle(UsersListStrings.title.translation())
            Spacer(modifier = Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (users.isEmpty()) {
                Text(UsersListStrings.empty.translation())
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(users) { user ->
                        ListRow(
                            text = user.username.string,
                            onSelect = { viewModel.onUserSelected(user.id) },
                            trailing = if (isRoot) {
                                {
                                    TextButton(onClick = { viewModel.onDeleteUserRequest(user) }) {
                                        Text(
                                            UsersListStrings.deleteButton.translation(),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfirmDialog(
        title: String,
        message: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(UsersListStrings.cancelButton.translation())
                }
            }
        )
    }
}
