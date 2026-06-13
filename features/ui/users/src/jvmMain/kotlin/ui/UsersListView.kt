package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the users list screen. */
class UsersListView(
    chain: NavigationChain<ViewConfig>,
    config: UsersListViewConfig,
) : ComposeView<UsersListViewConfig, ViewConfig, UsersListViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UsersListViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UsersListView)
    }

    override val title: String
        @Composable get() = UsersListStrings.title.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val users by viewModel.usersState.collectAsState()
        val avatars by viewModel.avatarsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val currentUserId by viewModel.currentUserIdState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentUserId != null) {
                    Button(onClick = { viewModel.onMyProfile() }) {
                        Text(UsersListStrings.myProfileButton.translation())
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                loading -> CircularProgressIndicator()
                users.isEmpty() -> Text(UsersListStrings.empty.translation())
                else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(users) { user ->
                        ListRow(
                            onSelect = { viewModel.onUserSelected(user.id) },
                            leading = {
                                val avatarModifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                val avatarId = avatars[user.id]
                                if (avatarId != null) {
                                    RemoteImage(
                                        key = avatarId.string,
                                        loader = { viewModel.loadImageBytes(avatarId) },
                                        contentDescription = null,
                                        modifier = avatarModifier
                                    )
                                } else {
                                    Box(
                                        modifier = avatarModifier.background(
                                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                            }
                        ) {
                            Text(user.username.string)
                        }
                    }
                }
                }
            }
        }
    }
}
