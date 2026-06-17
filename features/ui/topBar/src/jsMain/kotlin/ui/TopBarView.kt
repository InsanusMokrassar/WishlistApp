package dev.inmo.wishlist.features.ui.topBar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.topBar.TopBarStrings
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.B
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
            Div({ classes("topbar") }) {
                Label(attrs = { classes("search") }) {
                    SearchIcon()
                    Input(type = InputType.Text) {
                        value(searchQuery)
                        placeholder(TopBarStrings.searchPlaceholder.translation())
                        onInput { viewModel.onSearchQueryChanged(it.value) }
                    }
                    Span({ classes("kbd") }) { Text("⌘K") }
                }
                Div({ classes("sp") })
                Div({
                    style {
                        property("display", "flex")
                        property("align-items", "center")
                        property("gap", "8px")
                    }
                }) {
                    InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }
                }
            }
            val crumbTitles = titleProviders.map { it.title }.filter { it.isNotBlank() }
            if (crumbTitles.isNotEmpty()) {
                Div({ classes("crumbbar") }) {
                    Div({ classes("crumb") }) {
                        crumbTitles.forEachIndexed { index, title ->
                            if (index > 0) {
                                Span({ classes("sep") }) { Text("/") }
                            }
                            if (index == crumbTitles.lastIndex) {
                                B { Text(title) }
                            } else {
                                Span { Text(title) }
                            }
                        }
                    }
                }
            }
        }
    }
}
