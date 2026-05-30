package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
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
            Text(UsersListStrings.title.translation(), style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (users.isEmpty()) {
                Text(UsersListStrings.empty.translation())
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(users) { user ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user.username.string,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.onUserSelected(user.id) }
                                        .padding(vertical = 16.dp)
                                )
                                if (isRoot) {
                                    TextButton(onClick = { viewModel.onDeleteUserRequest(user) }) {
                                        Text(
                                            UsersListStrings.deleteButton.translation(),
                                            color = MaterialTheme.colors.error
                                        )
                                    }
                                }
                            }
                        }
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
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
