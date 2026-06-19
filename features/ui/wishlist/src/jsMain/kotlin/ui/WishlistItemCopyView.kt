package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.ContentColumn
import dev.inmo.wishlist.features.common.client.ui.components.FormHint
import dev.inmo.wishlist.features.common.client.ui.components.ListCard
import dev.inmo.wishlist.features.common.client.ui.components.ListCardsGrid
import dev.inmo.wishlist.features.common.client.ui.components.PageHead
import dev.inmo.wishlist.features.common.client.ui.components.Subline
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML copy-target picker: lists the caller's wishlists as a Calm Studio [ListCardsGrid]. */
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

        ContentColumn {
            PageHead(
                title = WishlistStrings.copyTargetTitle.translation(),
                subline = WishlistStrings.copySelectTargetPrompt.translation(),
                actions = { CreateWishlistButton(isOwner = true) { viewModel.onCreateWishlist() } },
            )

            if (error) {
                FormHint(WishlistStrings.copyFailed.translation())
            }

            when {
                loading -> Subline(WishlistStrings.loading.translation())
                targets.isEmpty() -> Div({ classes("empty") }) {
                    H3 { Text(WishlistStrings.copyNoTargets.translation()) }
                }
                else -> ListCardsGrid {
                    targets.forEach { wishlist ->
                        ListCard(
                            title = wishlist.title,
                            tintClass = tintClass(wishlist.id.long),
                            onOpen = { viewModel.onSelectTarget(wishlist.id) },
                        )
                    }
                }
            }
        }
    }
}
