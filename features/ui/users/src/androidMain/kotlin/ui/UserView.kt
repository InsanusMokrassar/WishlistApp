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
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ScreenTitle
import dev.inmo.wishlist.features.ui.users.UsersListStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** Android Compose-Material3 view for the public user profile detail screen. */
class UserView(
    chain: NavigationChain<ViewConfig>,
    config: UserViewConfig,
) : ComposeView<UserViewConfig, ViewConfig, UserViewModel>(config, chain) {
    override val viewModel: UserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@UserView)
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val resources = LocalResources.current
        val user by viewModel.userState.collectAsState()
        val avatarId by viewModel.avatarIdState.collectAsState()
        val canEdit by viewModel.canEditState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackButton(UsersListStrings.backButton.translation(resources)) { viewModel.onBack() }
                    ScreenTitle(UsersListStrings.profileTitle.translation(resources))
                }
                if (canEdit) {
                    Button(onClick = { viewModel.onEditUser() }) {
                        Text(UsersListStrings.editButton.translation(resources))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (loading) {
                CircularProgressIndicator()
            } else {
                avatarId?.let { id ->
                    RemoteImage(
                        key = id.string,
                        loader = { viewModel.loadImageBytes(id) },
                        contentDescription = UsersListStrings.avatarLabel.translation(resources),
                        modifier = Modifier.size(160.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    user?.username?.string ?: "#${config.userId.long}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
