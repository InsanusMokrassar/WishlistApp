package dev.inmo.wishlist.features.ui.topBar.ui

import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.components.Breadcrumb
import dev.inmo.wishlist.features.common.client.ui.components.CrumbItem
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.topBar.TopBarStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the Calm Studio top bar (the scaffold's top slot).
 *
 * Renders the global search field (people / lists / items) and the auth action (the embedded
 * `features/ui/auth` widget — Log in / Log out). Below the bar it keeps a slim breadcrumb fed by the
 * main chain's [TopBarTitleProvider]s so users still see their depth in the content. Class names
 * match the Calm Studio shell CSS (`.topbar`, `.search`, `.kbd`, `.crumb`).
 */
class TopBarView(
    chain: NavigationChain<ViewConfig>,
    config: TopBarViewConfig,
) : ComposeView<TopBarViewConfig, ViewConfig, TopBarViewModel>(config, chain) {
    override val viewModel: TopBarViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@TopBarView)
    }

    /** Lucide "search" glyph, injected as raw SVG (Compose-HTML has no SVG DOM builder). */
    @Composable
    private fun SearchIcon() {
        Span(attrs = {
            ref { element ->
                element.innerHTML =
                    """<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3-3"/></svg>"""
                onDispose { }
            }
        })
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val titleProviders by viewModel.titleProviders.collectAsState()
        val searchQuery by viewModel.searchQueryState.collectAsState()

        Div {
            Div({ classes(CalmStudioStyleSheet.topbar) }) {
                Label(attrs = { classes(CalmStudioStyleSheet.search); attr("disabled", ""); attr("title", TopBarStrings.searchComingSoonTooltip.translation()) }) {
                    SearchIcon()
                    Input(type = InputType.Text) {
                        value(searchQuery)
                        placeholder(TopBarStrings.searchPlaceholder.translation())
                        onInput { viewModel.onSearchQueryChanged(it.value) }
                        disabled()
                    }
                    Span({ classes(CalmStudioStyleSheet.kbd) }) { Text("⌘K") }
                }
                Div({ classes(CalmStudioStyleSheet.sp) })
                Div({ classes(CalmStudioStyleSheet.hstack) }) {
                    InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }
                }
            }
            // Read all @Composable titles inside the composable context before building lambdas.
            val crumbs = titleProviders.map { provider -> provider to provider.title }
                .filter { (_, title) -> title.isNotBlank() }
            if (crumbs.isNotEmpty()) {
                Div({ classes(CalmStudioStyleSheet.crumbbar) }) {
                    val ancestors = crumbs.dropLast(1).map { (provider, title) ->
                        CrumbItem(
                            label = title,
                            onClick = { viewModel.onCrumbSelected(provider) }
                        )
                    }
                    val current = crumbs.last().second
                    Breadcrumb(items = ancestors, current = current)
                }
            }
        }
    }
}
