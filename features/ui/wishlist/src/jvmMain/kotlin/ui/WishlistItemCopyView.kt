package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JVM Compose-Desktop copy-target picker: lists the caller's wishlists. */
class WishlistItemCopyView(
    chain: NavigationChain<ViewConfig>,
    config: WishlistItemCopyViewConfig,
) : ComposeView<WishlistItemCopyViewConfig, ViewConfig, WishlistItemCopyViewModel>(config, chain), TopBarTitleProvider {
    override val viewModel: WishlistItemCopyViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@WishlistItemCopyView)
    }

    override val title: String
        @Composable get() = WishlistStrings.copyTargetTitle.translation()

    @Composable
    override fun onDraw() {
        super.onDraw()
        val targets by viewModel.targetsState.collectAsState()
        val loading by viewModel.loadingState.collectAsState()
        val error by viewModel.errorState.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
            CreateWishlistButton(isOwner = true) { viewModel.onCreateWishlist() }

            if (error) {
                Text(WishlistStrings.copyFailed.translation(), style = MaterialTheme.typography.caption)
            }

            when {
                loading -> CircularProgressIndicator()
                targets.isEmpty() -> Text(WishlistStrings.copyNoTargets.translation())
                else -> {
                    Text(WishlistStrings.copySelectTargetPrompt.translation(), style = MaterialTheme.typography.subtitle2)
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(targets) { wishlist ->
                            ListRow(
                                text = wishlist.title,
                                onSelect = { viewModel.onSelectTarget(wishlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
