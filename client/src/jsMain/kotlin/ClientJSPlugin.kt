package dev.inmo.wishlist.client

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.toString
import dev.inmo.micro_utils.common.Warning
import dev.inmo.micro_utils.common.withReplaced
import dev.inmo.micro_utils.common.withReplacedAt
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.utils.findConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditView
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module

object ClientJSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ClientPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<ViewConfig>> {
            @OptIn(Warning::class)
            UrlParametersNavigationConfigsRepo<ViewConfig>(
                {
                    val openedWishlistConfig = it.findConfig { it as? WishlistViewConfig }
                    if (openedWishlistConfig != null) {
                        parameter("wishlist", openedWishlistConfig.wishlistId.long.toString())

                        val openedWishlistItemEditConfig = it.findConfig { it as? WishlistItemEditViewConfig }
                        if (openedWishlistItemEditConfig != null) {
                            parameter("wishlist_item", openedWishlistItemEditConfig.wishlistItemId ?.long ?.toString() ?: "new")
                        }

                        val openedWishlistItemConfig = it.findConfig { it as? WishlistItemViewConfig }
                        if (openedWishlistItemConfig != null) {
                            parameter("wishlist_item", openedWishlistItemConfig.wishlistItemId.long.toString())
                        }
                    } else {
                        val editWishlistConfig = it.findConfig { it as? WishlistEditViewConfig }
                        if (editWishlistConfig != null) {
                            pathSegment("edit")

                            editWishlistConfig.wishlistId ?.let {
                                pathSegment(it.long.toString())
                            }
                        }
                    }

                    val editModeConfig = it.findConfig { it as? WishlistItemEditViewConfig ?: it as? WishlistEditViewConfig }
                    if (editModeConfig != null) {
                        parameter("edit", "true")
                    }
                },
                {
                    val configs = mutableListOf<ViewConfig>()
                    configs.add(WishlistsListViewConfig())

                    var wishlistId: WishlistId? = null
                    var wishlistItemId: WishlistItemId? = null

                    val params = it.urlSearchParams
                    val wishlistAsParam = params.get("wishlist")
                    val wishlistItemAsParam = params.get("wishlist_item")
                    val editAsParam = params.get("edit")

                    if (wishlistAsParam.isNullOrBlank() == false && wishlistAsParam.all { it.isDigit() }) {
                        wishlistId = WishlistId(wishlistAsParam.toLong())
                    }
                    if (wishlistItemAsParam.isNullOrBlank() == false && wishlistItemAsParam.all { it.isDigit() }) {
                        wishlistItemId = WishlistItemId(wishlistItemAsParam.toLong())
                    }
                    val editModeEnabled = editAsParam != null

                    when (editModeEnabled) {
                        true -> when {
                            wishlistItemId == null && wishlistId == null -> {
                                configs.add(WishlistEditViewConfig(null))
                            }
                            wishlistId != null && wishlistItemId == null -> {
                                configs.add(WishlistViewConfig(wishlistId))
                                configs.add(WishlistEditViewConfig(wishlistId))
                            }
                            wishlistItemId != null && wishlistId != null -> {
                                configs.add(WishlistViewConfig(wishlistId))
                                configs.add(WishlistItemEditViewConfig(wishlistItemId, wishlistId))
                            }
                        }
                        false -> when {
                            wishlistId == null -> {} // do nothing
                            wishlistItemId == null -> {
                                configs.add(WishlistViewConfig(wishlistId))
                            }
                            else -> {
                                configs.add(WishlistItemViewConfig(wishlistItemId, wishlistId))
                            }
                        }
                    }

                    val asNodes = mutableListOf<ConfigHolder.Node<ViewConfig>>()
                    configs.reversed().forEach {
                        val nodeHolder = asNodes.lastOrNull()
                        val thisNode = ConfigHolder.Node<ViewConfig>(it, nodeHolder, emptyList())
                        asNodes.add(thisNode)
                    }

                    ConfigHolder.Chain(asNodes.last())
                },
                {
                    "wishlist"
                }
            )
//            NavigationConfigsRepo.InMemory<ViewConfig>()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        renderComposable("content") {
            ClientPlugin.currentDrawingBlock.collectAsState().value.invoke()
        }
    }
}
