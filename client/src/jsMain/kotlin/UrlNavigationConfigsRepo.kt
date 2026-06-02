@file:OptIn(Warning::class)

package dev.inmo.wishlist.client

import dev.inmo.micro_utils.common.Warning
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo
import dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo.LocationData
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.TopNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/** Default browser-tab/site title; always resolved regardless of the current navigation state. */
private const val SITE_TITLE = "wishlist"

/** Query-param key for the [UserWishlistsViewConfig] screen (a user's wishlists overview). */
private const val USER_WISHLISTS_PARAM = "user_wishlists"

/** Query-param key for the [UserViewConfig] screen (a public user profile). */
private const val USER_PARAM = "user"

/** Query-param key for the [WishlistViewConfig] screen (a single wishlist). */
private const val WISHLIST_PARAM = "wishlist"

/** Query-param key for the [WishlistItemViewConfig] screen (a single wishlist item). */
private const val WISHLIST_ITEM_PARAM = "wishlist_item"

/** Separator between the item id and its parent wishlist id inside the [WISHLIST_ITEM_PARAM] value. */
private const val WISHLIST_ITEM_SEPARATOR = "_"

/**
 * Folds a top-to-bottom list of stack configs into the linked [ConfigHolder.Node] form used by the
 * navigation hierarchy, where each chain node references the next pushed node through `subnode`.
 *
 * @return the head node of the stack, or `null` when the list is empty.
 */
private fun List<ViewConfig>.toNodeChain(): ConfigHolder.Node<ViewConfig>? =
    foldRight(null as ConfigHolder.Node<ViewConfig>?) { config, next ->
        ConfigHolder.Node(config, next, emptyList())
    }

/**
 * Collects the URL-relevant configs found anywhere in [holder] into [target] (last occurrence wins).
 *
 * Only the four deep-linkable screens are recorded; the scaffold skeleton and edit/admin screens are
 * intentionally ignored so the URL stays a stable, shareable pointer to a content screen.
 */
private fun collectParams(holder: ConfigHolder<ViewConfig>, target: MutableMap<String, String>) {
    when (holder) {
        is ConfigHolder.Chain -> holder.firstNodeConfig?.let { collectParams(it, target) }
        is ConfigHolder.Node -> {
            when (val config = holder.config) {
                is UserWishlistsViewConfig -> target[USER_WISHLISTS_PARAM] = config.userId.long.toString()
                is UserViewConfig -> target[USER_PARAM] = config.userId.long.toString()
                is WishlistViewConfig -> target[WISHLIST_PARAM] = config.wishlistId.long.toString()
                is WishlistItemViewConfig -> target[WISHLIST_ITEM_PARAM] =
                    "${config.wishlistItemId.long}$WISHLIST_ITEM_SEPARATOR${config.wishlistId.long}"
            }
            holder.subchains.forEach { collectParams(it, target) }
            holder.subnode?.let { collectParams(it, target) }
        }
    }
}

/**
 * Encodes the current navigation [holder] into URL query parameters.
 *
 * Mirrors [parseSearchParams]: it walks the hierarchy and emits one parameter per deep-linkable
 * screen it finds (see the `*_PARAM` keys).
 */
private fun LocationData.Builder.buildSearchParams(holder: ConfigHolder<ViewConfig>) {
    val params = linkedMapOf<String, String>()
    collectParams(holder, params)
    params.forEach { (key, value) -> parameter(key, value) }
}

/**
 * Rebuilds the navigation hierarchy from URL query parameters produced by [buildSearchParams].
 *
 * The reconstructed main-chain stack always starts at [UsersListViewConfig] and then deepens in a
 * fixed order that respects the app's tree of possible moves
 * (`UsersList → UserWishlists → Wishlist → WishlistItem → User`), appending only the nodes whose
 * parameters are present. The scaffold skeleton (empty root → scaffold → top + main chains) is
 * recreated around that stack so [ScaffoldView] can reattach each restored chain to its slot.
 *
 * @return the restored root [ConfigHolder.Chain], or `null` when no deep-link parameter is present
 * (so the default scaffold is created by the standard navigation init path instead).
 */
private fun parseSearchParams(data: LocationData): ConfigHolder.Chain<ViewConfig>? {
    val params = data.urlSearchParams
    val userWishlists = params.get(USER_WISHLISTS_PARAM)?.toLongOrNull()
    val user = params.get(USER_PARAM)?.toLongOrNull()
    val wishlist = params.get(WISHLIST_PARAM)?.toLongOrNull()
    val wishlistItem = params.get(WISHLIST_ITEM_PARAM)?.let { raw ->
        val parts = raw.split(WISHLIST_ITEM_SEPARATOR)
        val itemId = parts.getOrNull(0)?.toLongOrNull()
        val wishlistId = parts.getOrNull(1)?.toLongOrNull()
        if (itemId != null && wishlistId != null) itemId to wishlistId else null
    }

    if (userWishlists == null && user == null && wishlist == null && wishlistItem == null) {
        return null
    }

    val mainStack = buildList<ViewConfig> {
        add(UsersListViewConfig())
        userWishlists?.let { add(UserWishlistsViewConfig(UserId(it))) }
        wishlist?.let { add(WishlistViewConfig(WishlistId(it))) }
        wishlistItem?.let { (itemId, wishlistId) ->
            add(WishlistItemViewConfig(WishlistItemId(itemId), WishlistId(wishlistId)))
        }
        user?.let { add(UserViewConfig(UserId(it))) }
    }

    val scaffoldNode = ConfigHolder.Node<ViewConfig>(
        config = ClientPlugin.mainScaffoldConfig,
        subnode = null,
        subchains = listOf(
            ConfigHolder.Chain(
                ConfigHolder.Node<ViewConfig>(TopBarViewConfig(), null, emptyList()),
                TopNavigationChainId
            ),
            ConfigHolder.Chain(mainStack.toNodeChain(), MainNavigationChainId)
        )
    )

    return ConfigHolder.Chain(
        ConfigHolder.Node<ViewConfig>(
            config = dev.inmo.wishlist.features.common.client.models.EmptyConfig(),
            subnode = null,
            subchains = listOf(ConfigHolder.Chain(scaffoldNode, null))
        ),
        null
    )
}

/**
 * Builds the JS [NavigationConfigsRepo] that stores the navigation hierarchy in the page URL, so
 * deep links to the four content screens are shareable and survive a reload.
 */
fun WishlistsAppUrlNavigationConfigsRepo(): NavigationConfigsRepo<ViewConfig> =
    UrlParametersNavigationConfigsRepo(
        buildSearchParams = { holder -> buildSearchParams(holder) },
        parseSearchParams = ::parseSearchParams,
        titleResolver = { SITE_TITLE }
    )
