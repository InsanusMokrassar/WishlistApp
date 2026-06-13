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
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.adminPanel.AdminPanelStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop view for the admin user detail screen. Shows user info and wishlists inline. */
class AdminUserView(
    chain: NavigationChain<ViewConfig>,
    config: AdminUserViewConfig,
) : ComposeView<AdminUserViewConfig, ViewConfig, AdminUserViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: AdminUserViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@AdminUserView)
    }

    override val title: String
        @Composable get() {
            val user by viewModel.userState.collectAsState()
            return user?.username?.string ?: "#${config.userId.long}"
        }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val user by viewModel.userState.collectAsState()
        val wishlists by viewModel.wishlistsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(AdminPanelStrings.backButton.translation()) { viewModel.onBack() }
                Button(onClick = { viewModel.onEditUser() }) {
                    Text(AdminPanelStrings.editButton.translation())
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (loading) {
                CircularProgressIndicator()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AdminPanelStrings.userWishlistsSection.translation(), style = MaterialTheme.typography.h6)
                    Button(onClick = { viewModel.onAddWishlist() }) {
                        Text(AdminPanelStrings.addWishlistForUserButton.translation())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (wishlists.isEmpty()) {
                    Text(AdminPanelStrings.noWishlistsForUser.translation())
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(wishlists) { wishlist ->
                            ListRow(onSelect = { viewModel.onOpenWishlist(wishlist.id) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(wishlist.title)
                                    Text("#${wishlist.id.long}", color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
