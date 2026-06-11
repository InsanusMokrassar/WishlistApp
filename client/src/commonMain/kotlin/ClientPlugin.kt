package dev.inmo.wishlist.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.compose.getChainFromLocalProvider
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.NavigationNodeState
import dev.inmo.navigation.core.extensions.changesInSubtreeFlow
import dev.inmo.navigation.core.extensions.dropNodesInSubTree
import dev.inmo.wishlist.features.common.client.models.EmptyConfig
import dev.inmo.wishlist.features.common.client.models.RootNodeFactoryGetter
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminPanelViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserEditViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUserViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminUsersListViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistEditViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistItemEditViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistViewInteractor
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListViewConfig
import dev.inmo.wishlist.features.ui.adminPanel.ui.AdminWishlistsListViewInteractor
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewInteractor
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewConfig
import dev.inmo.wishlist.features.ui.booking.ui.MyPresentsBooksViewInteractor
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewInteractor
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserViewInteractor
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemCopyViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemCopyViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewInteractor
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewInteractor
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.flow.conflate
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common client-side startup plugin.
 *
 * Owns the root [NavigationChain] singleton, the current-drawing-block sink, and
 * all UI interactor bindings (intra-feature navigation wiring).
 *
 * Platform-specific root-chain bootstrapping (which config to push first) is
 * performed by `ClientJVMPlugin`, `ClientJSPlugin`, and `ClientAndroidPlugin`.
 */
object ClientPlugin : StartPlugin {
    /** Compose drawing block sink the platform shells observe to render the app. */
    val currentDrawingBlock = MutableRedeliverStateFlow<@Composable () -> Unit>({})

    /** Pre-built scaffold layout config used as the application's main screen. */
    val mainScaffoldConfig: ScaffoldViewConfig
        get() = ScaffoldViewConfig(
            topConfig = TopBarViewConfig(),
            mainConfig = UsersListViewConfig()
        )

    override fun Module.setupDI(config: JsonObject) {
        single<RootNodeFactoryGetter> {
            val nodeFactory: NavigationNodeFactory<ViewConfig> = getKoin().nodeFactory<ViewConfig>()
            return@single RootNodeFactoryGetter { nodeFactory }
        }
        single {
            NavigationChain<ViewConfig>(
                null,
                get<RootNodeFactoryGetter>().invoke()
            )
        }
        single {
            { drawable: @Composable () -> Unit ->
                currentDrawingBlock.value = drawable
            }
        }

        single<UsersListViewInteractor> {
            object : UsersListViewInteractor {
                override suspend fun onUserSelected(
                    node: NavigationNode<UsersListViewConfig, ViewConfig>,
                    userId: UserId
                ) {
                    node.chain.push(UserWishlistsViewConfig(userId))
                }
                override suspend fun onOpenProfile(
                    node: NavigationNode<UsersListViewConfig, ViewConfig>,
                    userId: UserId
                ) {
                    node.chain.push(UserViewConfig(userId))
                }
            }
        }

        single<UserViewInteractor> {
            object : UserViewInteractor {
                override suspend fun onBack(node: NavigationNode<UserViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onEditUser(node: NavigationNode<UserViewConfig, ViewConfig>) {
                    node.chain.push(UserEditViewConfig(node.config.userId))
                }
            }
        }

        single<UserEditViewInteractor> {
            object : UserEditViewInteractor {
                override suspend fun onNavigateBack(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onSaved(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onDeleted(node: NavigationNode<UserEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
            }
        }

        single<TopBarViewInteractor> {
            val rootChain = get<NavigationChain<ViewConfig>>()
            object : TopBarViewInteractor {
                override suspend fun onChangeServerUrl(
                    node: NavigationNode<TopBarViewConfig, ViewConfig>
                ) {
                    rootChain.push(ServerUrlViewConfig())
                }
            }
        }

        single<ServerUrlViewInteractor> {
            val rootChain = get<NavigationChain<ViewConfig>>()
            object : ServerUrlViewInteractor {
                override suspend fun onSaved(
                    node: NavigationNode<ServerUrlViewConfig, ViewConfig>
                ) {
                    val hasScaffold = rootChain.stackFlow.value.any { it.config is ScaffoldViewConfig }
                    if (!hasScaffold) {
                        rootChain.push(mainScaffoldConfig)
                    }
                    rootChain.dropNodesInSubTree { it.config is ServerUrlViewConfig }
                }
            }
        }

        single<WishlistsListViewInteractor> {
            object : WishlistsListViewInteractor {
                override suspend fun onWishlistSelected(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
                    wishlistId: WishlistId
                ) {
                    node.chain.push(WishlistViewConfig(wishlistId))
                }
                override suspend fun onCreateWishlist(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistEditViewConfig(null))
                }
                override suspend fun onBack(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onShowUserWishlists(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
                    userId: UserId
                ) {
                    node.chain.push(UserWishlistsViewConfig(userId))
                }
                override suspend fun onShowUser(
                    node: NavigationNode<WishlistsListViewConfig, ViewConfig>,
                    userId: UserId
                ) {
                    node.chain.push(UserViewConfig(userId))
                }
            }
        }

        single<UserWishlistsViewInteractor> {
            object : UserWishlistsViewInteractor {
                override suspend fun onItemSelected(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
                    itemId: WishlistItemId,
                    wishlistId: WishlistId
                ) {
                    node.chain.push(WishlistItemViewConfig(itemId, wishlistId))
                }
                override suspend fun onWishlistSelected(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
                    wishlistId: WishlistId
                ) {
                    node.chain.push(WishlistViewConfig(wishlistId))
                }
                override suspend fun onBack(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onOpenProfile(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
                    userId: UserId
                ) {
                    node.chain.push(UserViewConfig(userId))
                }
                override suspend fun onCreateWishlistClick(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistEditViewConfig(null))
                }
                override suspend fun onCreateItemClick(
                    node: NavigationNode<UserWishlistsViewConfig, ViewConfig>,
                    wishlistId: WishlistId
                ) {
                    node.chain.push(WishlistItemEditViewConfig(null, wishlistId))
                }
            }
        }

        single<WishlistViewInteractor> {
            object : WishlistViewInteractor {
                override suspend fun onBack(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onEditWishlist(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistEditViewConfig(node.config.wishlistId))
                }
                override suspend fun onViewItem(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>,
                    itemId: WishlistItemId
                ) {
                    node.chain.push(WishlistItemViewConfig(itemId, node.config.wishlistId))
                }
                override suspend fun onAddItem(
                    node: NavigationNode<WishlistViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistItemEditViewConfig(null, node.config.wishlistId))
                }
            }
        }

        single<WishlistEditViewInteractor> {
            object : WishlistEditViewInteractor {
                override suspend fun onNavigateBack(
                    node: NavigationNode<WishlistEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onSaved(
                    node: NavigationNode<WishlistEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
            }
        }

        single<WishlistItemEditViewInteractor> {
            object : WishlistItemEditViewInteractor {
                override suspend fun onNavigateBack(
                    node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onSaved(
                    node: NavigationNode<WishlistItemEditViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
            }
        }

        single<WishlistItemViewInteractor> {
            object : WishlistItemViewInteractor {
                override suspend fun onBack(
                    node: NavigationNode<WishlistItemViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onEditItem(
                    node: NavigationNode<WishlistItemViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistItemEditViewConfig(node.config.wishlistItemId, node.config.wishlistId))
                }
                override suspend fun onCopyItem(
                    node: NavigationNode<WishlistItemViewConfig, ViewConfig>
                ) {
                    node.chain.push(WishlistItemCopyViewConfig(node.config.wishlistItemId, node.config.wishlistId))
                }
            }
        }

        single<WishlistItemCopyViewInteractor> {
            object : WishlistItemCopyViewInteractor {
                override suspend fun onBack(
                    node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
                override suspend fun onCopied(
                    node: NavigationNode<WishlistItemCopyViewConfig, ViewConfig>
                ) {
                    node.chain.pop()
                }
            }
        }

        single<MyPresentsBooksViewInteractor> {
            object : MyPresentsBooksViewInteractor {
                override suspend fun onBack(node: NavigationNode<MyPresentsBooksViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
            }
        }

        single<AdminPanelViewInteractor> {
            object : AdminPanelViewInteractor {
                override suspend fun onOpenUsers(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) {
                    node.chain.push(AdminUsersListViewConfig())
                }
                override suspend fun onOpenWishlists(node: NavigationNode<AdminPanelViewConfig, ViewConfig>) {
                    node.chain.push(AdminWishlistsListViewConfig())
                }
            }
        }

        single<AdminUsersListViewInteractor> {
            object : AdminUsersListViewInteractor {
                override suspend fun onUserSelected(node: NavigationNode<AdminUsersListViewConfig, ViewConfig>, userId: UserId) {
                    node.chain.push(AdminUserViewConfig(userId))
                }
                override suspend fun onCreateUser(node: NavigationNode<AdminUsersListViewConfig, ViewConfig>) {
                    node.chain.push(AdminUserEditViewConfig(null))
                }
            }
        }

        single<AdminUserViewInteractor> {
            object : AdminUserViewInteractor {
                override suspend fun onBack(node: NavigationNode<AdminUserViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onEditUser(node: NavigationNode<AdminUserViewConfig, ViewConfig>) {
                    node.chain.push(AdminUserEditViewConfig(node.config.userId))
                }
                override suspend fun onOpenWishlist(node: NavigationNode<AdminUserViewConfig, ViewConfig>, wishlistId: WishlistId) {
                    node.chain.push(AdminWishlistViewConfig(wishlistId))
                }
                override suspend fun onAddWishlist(node: NavigationNode<AdminUserViewConfig, ViewConfig>, userId: UserId) {
                    node.chain.push(AdminWishlistEditViewConfig(null, userId))
                }
            }
        }

        single<AdminUserEditViewInteractor> {
            object : AdminUserEditViewInteractor {
                override suspend fun onNavigateBack(node: NavigationNode<AdminUserEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onSaved(node: NavigationNode<AdminUserEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
            }
        }

        single<AdminWishlistsListViewInteractor> {
            object : AdminWishlistsListViewInteractor {
                override suspend fun onWishlistSelected(node: NavigationNode<AdminWishlistsListViewConfig, ViewConfig>, wishlistId: WishlistId) {
                    node.chain.push(AdminWishlistViewConfig(wishlistId))
                }
                override suspend fun onCreateWishlist(node: NavigationNode<AdminWishlistsListViewConfig, ViewConfig>) {
                    node.chain.push(AdminWishlistEditViewConfig(null))
                }
            }
        }

        single<AdminWishlistViewInteractor> {
            object : AdminWishlistViewInteractor {
                override suspend fun onBack(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onEditWishlist(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>) {
                    node.chain.push(AdminWishlistEditViewConfig(node.config.wishlistId))
                }
                override suspend fun onAddItem(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>, wishlistId: WishlistId) {
                    node.chain.push(AdminWishlistItemEditViewConfig(null, wishlistId))
                }
                override suspend fun onEditItem(node: NavigationNode<AdminWishlistViewConfig, ViewConfig>, itemId: WishlistItemId, wishlistId: WishlistId) {
                    node.chain.push(AdminWishlistItemEditViewConfig(itemId, wishlistId))
                }
            }
        }

        single<AdminWishlistEditViewInteractor> {
            object : AdminWishlistEditViewInteractor {
                override suspend fun onNavigateBack(node: NavigationNode<AdminWishlistEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onSaved(node: NavigationNode<AdminWishlistEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
            }
        }

        single<AdminWishlistItemEditViewInteractor> {
            object : AdminWishlistItemEditViewInteractor {
                override suspend fun onNavigateBack(node: NavigationNode<AdminWishlistItemEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
                override suspend fun onSaved(node: NavigationNode<AdminWishlistItemEditViewConfig, ViewConfig>) {
                    node.chain.pop()
                }
            }
        }

        single<AuthViewInteractor> {
            val userLoggedIn = MutableRedeliverStateFlow(false)
            object : AuthViewInteractor {
                override suspend fun onUserLoggedIn(node: NavigationNode<AuthViewConfig, ViewConfig>) {
                    userLoggedIn.value = true
                }

                override suspend fun onUserLoggedOut() {
                    userLoggedIn.value = false
                }
            }
        }
    }

    private fun NavigationNode<*, ViewConfig>.makeNodeString(spacers: String): String {
        return "$spacers[Node ${id.string}] ${this::class.simpleName} -> ${this.config::class.simpleName}; State: $state\n${subchainsFlow.value.joinToString("\n") { it.makeChainString("$spacers  ") }}"
    }
    private fun NavigationChain<ViewConfig>.makeChainString(spacers: String): String {
        val (id, stack) = runCatching {
            id to stackFlow.value
        }.getOrElse {
            return "${spacers}Error"
        }
        return "$spacers[Chain ${id?.string ?: "Anonymous"}] State ${parentNode ?.state ?: NavigationNodeState.RESUMED}\n${stack.joinToString("\n") { it.makeNodeString("$spacers  ") }}"
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        val rootChain = koin.get<NavigationChain<ViewConfig>>()
        koin.get<(@Composable () -> Unit) -> Unit>().invoke {
            initNavigation<ViewConfig>(
                EmptyConfig(),
                configsRepo = koin.get(),
                nodesFactory = koin.get<RootNodeFactoryGetter>().invoke(),
                dropRedundantChainsOnRestore = true,
                rootChain = rootChain
            ) {
                val rootChain = getChainFromLocalProvider<ViewConfig>()!!
                LaunchedEffect(rootChain) {
                    rootChain.either<NavigationChain<ViewConfig>, NavigationNode<out ViewConfig, ViewConfig>>().changesInSubtreeFlow().conflate().collect {
                        println(rootChain.makeChainString(""))
                    }
                }
                InjectNavigationChain<ViewConfig> {
                    InjectNavigationNode(mainScaffoldConfig)
                }
            }
        }
    }
}
