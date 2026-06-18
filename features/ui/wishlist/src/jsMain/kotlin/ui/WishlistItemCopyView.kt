package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.tintClass
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML copy-target picker: lists the caller's wishlists as a Calm Studio `.listgrid`. */
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

        Div({ classes(CalmStudioStyleSheet.`content-inner`) }) {
            Div({ classes(CalmStudioStyleSheet.pagehead) }) {
                Div {
                    H1 { Text(WishlistStrings.copyTargetTitle.translation()) }
                    P({ classes(CalmStudioStyleSheet.subline) }) { Text(WishlistStrings.copySelectTargetPrompt.translation()) }
                }
                Div({ classes(CalmStudioStyleSheet.acts) }) {
                    CreateWishlistButton(isOwner = true) { viewModel.onCreateWishlist() }
                }
            }

            if (error) {
                P({ classes(CalmStudioStyleSheet.hint) }) { Text(WishlistStrings.copyFailed.translation()) }
            }

            when {
                loading -> P({ classes(CalmStudioStyleSheet.subline) }) { Text(WishlistStrings.loading.translation()) }
                targets.isEmpty() -> Div({ classes("empty") }) {
                    H3 { Text(WishlistStrings.copyNoTargets.translation()) }
                }
                else -> Div({ classes(CalmStudioStyleSheet.listgrid) }) {
                    targets.forEach { wishlist ->
                        Div({
                            classes(CalmStudioStyleSheet.listcard)
                            onClick { viewModel.onSelectTarget(wishlist.id) }
                        }) {
                            Div({ classes(CalmStudioStyleSheet.cover, tintClass(wishlist.id.long)) })
                            Div({ classes(CalmStudioStyleSheet.c) }) { H3 { Text(wishlist.title) } }
                        }
                    }
                }
            }
        }
    }
}
