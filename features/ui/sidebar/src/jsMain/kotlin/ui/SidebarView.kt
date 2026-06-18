package dev.inmo.wishlist.features.ui.sidebar.ui

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
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.sidebar.SidebarStrings
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * JS Compose-HTML view for the Calm Studio left sidebar (the scaffold's left slot).
 *
 * Renders, top to bottom: the brand word-mark, the primary navigation (My Lists / Discover /
 * Reserved with a live count / Settings), the caller's pinned lists with a "New list" affordance,
 * and a bottom profile row (or the inline login widget when anonymous). Class names mirror the
 * Calm Studio reference so `CalmStudioStyleSheet` (in `features/common/client`) styles it directly.
 */
class SidebarView(
    chain: NavigationChain<ViewConfig>,
    config: SidebarViewConfig,
) : ComposeView<SidebarViewConfig, ViewConfig, SidebarViewModel>(config, chain) {
    override val viewModel: SidebarViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        parametersOf(this@SidebarView)
    }

    /**
     * One primary navigation row.
     *
     * @param icon Inner SVG markup from [LucideIcons].
     * @param label Visible item label.
     * @param active Whether this item owns the current content; toggles the `on` highlight.
     * @param count Optional live badge value; rendered only when greater than zero.
     * @param onSelect Invoked when the row is clicked.
     */
    @Composable
    private fun NavItem(icon: String, label: String, active: Boolean, count: Int = 0, onSelect: () -> Unit) {
        Button(attrs = {
            if (active) classes(CalmStudioStyleSheet.navitem, CalmStudioStyleSheet.on) else classes(CalmStudioStyleSheet.navitem)
            onClick { onSelect() }
        }) {
            LucideIcon(icon)
            Text(label)
            if (count > 0) {
                Span(attrs = { classes(CalmStudioStyleSheet.count) }) { Text(count.toString()) }
            }
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()
        val currentUserId by viewModel.currentUserIdState.collectAsState()
        val userName by viewModel.userNameState.collectAsState()
        val myLists by viewModel.myListsState.collectAsState()
        val reservedCount by viewModel.reservedCountState.collectAsState()
        val activeSection by viewModel.activeSectionState.collectAsState()

        Div(attrs = { classes(CalmStudioStyleSheet.sidebar) }) {
            Div(attrs = { classes(CalmStudioStyleSheet.logo) }) {
                Span(attrs = { classes(CalmStudioStyleSheet.mk) }) { LucideIcon(LucideIcons.gift) }
                Text(SidebarStrings.brand.translation())
            }

            Nav(attrs = { classes(CalmStudioStyleSheet.navsec) }) {
                NavItem(
                    icon = LucideIcons.home,
                    label = SidebarStrings.myLists.translation(),
                    active = activeSection == SidebarSection.MyLists
                ) { viewModel.onSelectMyLists() }
                NavItem(
                    icon = LucideIcons.compass,
                    label = SidebarStrings.discover.translation(),
                    active = activeSection == SidebarSection.Discover
                ) { viewModel.onSelectDiscover() }
                NavItem(
                    icon = LucideIcons.bookmark,
                    label = SidebarStrings.reserved.translation(),
                    active = activeSection == SidebarSection.Reserved,
                    count = reservedCount
                ) { viewModel.onSelectReserved() }
                NavItem(
                    icon = LucideIcons.settings,
                    label = SidebarStrings.settings.translation(),
                    active = activeSection == SidebarSection.Settings
                ) { viewModel.onSelectSettings() }
            }

            if (currentUserId != null && myLists.isNotEmpty()) {
                Nav(attrs = { classes(CalmStudioStyleSheet.navsec) }) {
                    Div(attrs = { classes(CalmStudioStyleSheet.navlabel) }) { Text(SidebarStrings.yourLists.translation()) }
                    myLists.forEach { wishlist ->
                        Button(attrs = {
                            classes(CalmStudioStyleSheet.navitem)
                            onClick { viewModel.onSelectWishlist(wishlist.id) }
                        }) {
                            Span(attrs = { classes(CalmStudioStyleSheet.swatch, tintClass(wishlist.id.long)) })
                            Text(wishlist.title)
                        }
                    }
                    Button(attrs = {
                        classes(CalmStudioStyleSheet.navitem)
                        onClick { viewModel.onCreateList() }
                    }) {
                        LucideIcon(LucideIcons.plus)
                        Text(SidebarStrings.newList.translation())
                    }
                }
            }

            Div(attrs = { classes(CalmStudioStyleSheet.spacer) })

            val resolvedUserId = currentUserId
            if (resolvedUserId != null) {
                Div(attrs = {
                    classes(CalmStudioStyleSheet.me)
                    onClick { viewModel.onOpenProfile() }
                }) {
                    Span(attrs = { classes(CalmStudioStyleSheet.av, tintClass(resolvedUserId.long)) })
                    Span(attrs = { classes(CalmStudioStyleSheet.nm) }) {
                        Text(userName ?: "")
                        Small { Text(SidebarStrings.viewProfile.translation()) }
                    }
                }
            } else {
                InjectNavigationChain<ViewConfig> { InjectNavigationNode(AuthViewConfig()) }
            }
        }
    }

    /**
     * Deterministic tint class (`t0`..`t7`) for a swatch/avatar, keyed by [seed] so a given list or
     * user keeps the same color across renders.
     *
     * @param seed Stable numeric id (wishlist or user).
     * @return One of the `t0`..`t7` media-tint classes defined in [CalmStudioStyleSheet].
     */
    private fun tintClass(seed: Long): String = when (((seed % 8 + 8) % 8).toInt()) {
        0 -> CalmStudioStyleSheet.t0
        1 -> CalmStudioStyleSheet.t1
        2 -> CalmStudioStyleSheet.t2
        3 -> CalmStudioStyleSheet.t3
        4 -> CalmStudioStyleSheet.t4
        5 -> CalmStudioStyleSheet.t5
        6 -> CalmStudioStyleSheet.t6
        else -> CalmStudioStyleSheet.t7
    }
}
