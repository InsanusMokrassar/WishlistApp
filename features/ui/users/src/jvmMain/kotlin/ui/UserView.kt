package dev.inmo.wishlist.features.ui.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the public user profile detail screen. */
class UserView(
    chain: NavigationChain<ViewConfig>,
    config: UserViewConfig,
) : ComposeView<UserViewConfig, ViewConfig, UserViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: UserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserView)
    }

    override val title: String
        @Composable get() = UsersListStrings.profileTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val backLabel by viewModel.backLabelState.collectAsState()
        val avatarId by viewModel.avatarIdState.collectAsState()
        val canEdit by viewModel.canEditState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(backLabel ?: UsersListStrings.backButton.translation()) { viewModel.onBack() }
                if (canEdit) {
                    Button(onClick = { viewModel.onEditUser() }) {
                        Text(UsersListStrings.editButton.translation())
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (loading) {
                CircularProgressIndicator()
            } else {
                val id = avatarId
                if (id != null) {
                    RemoteImage(
                        key = id.string,
                        loader = { viewModel.loadImageBytes(id) },
                        contentDescription = UsersListStrings.avatarLabel.translation(),
                        modifier = Modifier.size(160.dp)
                    )
                } else {
                    UserAvatarPlaceholder(
                        modifier = Modifier.size(160.dp),
                        contentDescription = UsersListStrings.avatarPlaceholderAlt.translation()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    user?.username?.string ?: "#${config.userId.long}",
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}
