package dev.inmo.wishlist.features.ui.adminPanel.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the admin users list screen. */
class AdminUsersListView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUsersListViewConfig,
) : ComposeView<AdminUsersListViewConfig, ViewConfig, AdminUsersListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminUsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUsersListView)
    }

    override val title: String
        @Composable get() = AdminPanelStrings.usersListTitle.translation(LocalResources.current)

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val users by viewModel.usersState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.onCreateUser() }) {
                    Text(AdminPanelStrings.addUserButton.translation(resources))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (users.isEmpty()) {
                Text(AdminPanelStrings.emptyUsers.translation(resources))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(users) { user ->
                        ListRow(onSelect = { viewModel.onUserSelected(user.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(user.username.string)
                                Text("#${user.id.long}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
