package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.BackButton
import dev.inmo.wishlist.features.common.client.ui.components.ListRow
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarTitleProvider
import dev.inmo.wishlist.features.ui.wishlist.WishlistStrings
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** JS Compose-HTML copy-target picker: lists the caller's wishlists. Uses Bootstrap classes. */
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

        Div({ classes("container", "py-3") }) {
            Div({ classes("d-flex", "align-items-center", "mb-3", "gap-2") }) {
                BackButton(WishlistStrings.backButton.translation()) { viewModel.onBack() }
                CreateWishlistButton(isOwner = true) { viewModel.onCreateWishlist() }
            }

            if (error) {
                Div({ classes("alert", "alert-danger", "py-2") }) {
                    Text(WishlistStrings.copyFailed.translation())
                }
            }

            when {
                loading -> P { Text(WishlistStrings.loading.translation()) }
                targets.isEmpty() -> P({ classes("text-muted") }) { Text(WishlistStrings.copyNoTargets.translation()) }
                else -> {
                    P { Text(WishlistStrings.copySelectTargetPrompt.translation()) }
                    Ul({ classes("list-group") }) {
                        targets.forEach { wishlist ->
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
