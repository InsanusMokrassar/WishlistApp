@file:OptIn(Warning::class)

package dev.inmo.wishlist.client

import dev.inmo.micro_utils.common.Warning
import dev.inmo.navigation.core.repo.ConfigHolder
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo
import dev.inmo.navigation.core.urls.UrlParametersNavigationConfigsRepo.LocationData
import dev.inmo.wishlist.features.common.client.models.LeftNavigationChainId
import dev.inmo.wishlist.features.common.client.models.MainNavigationChainId
import dev.inmo.wishlist.features.common.client.models.TopNavigationChainId
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserEditViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UserViewConfig
import dev.inmo.wishlist.features.ui.users.ui.UsersListViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.UserWishlistsViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemEditViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistItemViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.browser.document
import org.w3c.dom.url.URL

/** Default browser-tab/site title; always resolved regardless of the current navigation state. */
private const val SITE_TITLE = "wishlist"

/** Path segment introducing a user-scoped subtree (`user/<userId>`). */
private const val USER_SEGMENT = "user"

/** Path segment for a user's wishlists overview (`user/<userId>/wishlists`). */
private const val WISHLISTS_SEGMENT = "wishlists"

/** Path segment introducing a wishlist-scoped subtree (`wishlist/<wishlistId>`). */
private const val WISHLIST_SEGMENT = "wishlist"

/** Path segment introducing an item-scoped subtree (`wishlist/<wishlistId>/item/<itemId>`). */
private const val ITEM_SEGMENT = "item"

/** Trailing path segment marking an edit screen (`.../edit`). */
private const val EDIT_SEGMENT = "edit"

/** Path segment marking a create screen, where no entity id exists yet (`.../new`). */
private const val NEW_SEGMENT = "new"

/**
 * Path segments of the directory the web client is mounted under (e.g. `/ui/` → `["ui"]`), derived
 * from the document base URL (`<base href>`).
 *
 * The client is served under a sub-path (see issue #43), so every saved URL must be prefixed with it
 * and every parsed URL must have it stripped before the app-specific segments are interpreted.
 * Resolving it from `<base href>` keeps this client free of any hard dependency on the server-side
 * sub-path constant.
 *
 * @return The non-empty segments of the base path; empty when the client is served from the root.
 */
private fun appBasePathSegments(): List<String> =
    URL(document.baseURI).pathname.split("/").filter { it.isNotBlank() }

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
 * Maps a single deep-linkable [config] to its canonical, self-contained URL path (app segments only,
 * without the [appBasePathSegments] prefix).
 *
 * Each path embeds the full ancestry of its screen, so it can be reversed by [parseMainStack] into a
 * sensible breadcrumb stack. Edit screens append [EDIT_SEGMENT]; create screens (no entity id yet)
 * use [NEW_SEGMENT].
 *
 * @return the path segments, or `null` when [config] is not a deep-linkable content screen.
 */
private fun configPath(config: ViewConfig): List<String>? = when (config) {
    is UserEditViewConfig ->
        listOf(USER_SEGMENT, config.userId.long.toString(), EDIT_SEGMENT)
    is UserWishlistsViewConfig ->
        listOf(USER_SEGMENT, config.userId.long.toString(), WISHLISTS_SEGMENT)
    is UserViewConfig ->
        listOf(USER_SEGMENT, config.userId.long.toString())
    is WishlistItemEditViewConfig -> buildList {
        add(WISHLIST_SEGMENT)
        add(config.wishlistId.long.toString())
        add(ITEM_SEGMENT)
        val itemId = config.wishlistItemId
        if (itemId != null) {
            add(itemId.long.toString())
            add(EDIT_SEGMENT)
        } else {
            add(NEW_SEGMENT)
        }
    }
    is WishlistItemViewConfig ->
        listOf(WISHLIST_SEGMENT, config.wishlistId.long.toString(), ITEM_SEGMENT, config.wishlistItemId.long.toString())
    is WishlistEditViewConfig -> config.wishlistId?.let {
        listOf(WISHLIST_SEGMENT, it.long.toString(), EDIT_SEGMENT)
    } ?: listOf(WISHLIST_SEGMENT, NEW_SEGMENT)
    is WishlistViewConfig ->
        listOf(WISHLIST_SEGMENT, config.wishlistId.long.toString())
    else -> null
}

/**
 * Finds the currently focused deep-linkable screen anywhere in [holder].
 *
 * The navigation tree is walked tracking each node's depth (every pushed `subnode` and branched
 * `subchain` deepens the stack); the deep-linkable config sitting at the greatest depth wins, with
 * ties resolving to the last visited (i.e. the most recently pushed screen). Ranking by depth — not
 * by path length — is what makes a later, shallower-pathed screen (e.g. a profile `user/<id>` opened
 * on top of a wishlists overview `user/<id>/wishlists`) correctly replace the URL.
 *
 * @return the focused screen's app path segments, or `null` when no deep-linkable screen is present.
 */
private fun deepestConfigPath(holder: ConfigHolder<ViewConfig>): List<String>? {
    var best: List<String>? = null
    var bestDepth = -1
    fun visit(current: ConfigHolder<ViewConfig>, depth: Int) {
        when (current) {
            is ConfigHolder.Chain -> current.firstNodeConfig?.let { visit(it, depth) }
            is ConfigHolder.Node -> {
                configPath(current.config)?.let { path ->
                    if (depth >= bestDepth) {
                        best = path
                        bestDepth = depth
                    }
                }
                current.subchains.forEach { visit(it, depth + 1) }
                current.subnode?.let { visit(it, depth + 1) }
            }
        }
    }
    visit(holder, 0)
    return best
}

/**
 * Encodes the current navigation [holder] into the URL path.
 *
 * Emits the [appBasePathSegments] prefix followed by the [deepestConfigPath] of the focused screen
 * (nothing beyond the prefix when only the root users list is shown). Mirrored by [parsePath].
 *
 * The [Builder] arrives pre-filled with the current URL's path segments, so they are reset before
 * rebuilding; otherwise each navigation re-appends the prefix and screen path, duplicating segments.
 */
private fun LocationData.Builder.buildPath(holder: ConfigHolder<ViewConfig>) {
    // reset: Builder arrives pre-filled with the current URL's segments; without clearing, each navigation re-appends and duplicates them
    pathSegments.clear()
    appBasePathSegments().forEach { pathSegment(it) }
    deepestConfigPath(holder)?.forEach { pathSegment(it) }
}

/**
 * Rebuilds the main-chain stack (top-to-bottom) from the app-specific path [segments] produced by
 * [configPath], always rooted at [UsersListViewConfig].
 *
 * @return the stack, or `null` when [segments] do not describe a known screen.
 */
private fun parseMainStack(segments: List<String>): List<ViewConfig>? = when (segments.firstOrNull()) {
    USER_SEGMENT -> parseUserStack(segments.drop(1))
    WISHLIST_SEGMENT -> parseWishlistStack(segments.drop(1))
    else -> null
}

private fun parseUserStack(rest: List<String>): List<ViewConfig>? {
    val userId = rest.getOrNull(0)?.toLongOrNull()?.let(::UserId) ?: return null
    val root = UsersListViewConfig()
    return when (rest.getOrNull(1)) {
        null -> listOf(root, UserViewConfig(userId))
        EDIT_SEGMENT -> listOf(root, UserViewConfig(userId), UserEditViewConfig(userId))
        WISHLISTS_SEGMENT -> listOf(root, UserWishlistsViewConfig(userId))
        else -> null
    }
}

private fun parseWishlistStack(rest: List<String>): List<ViewConfig>? {
    val root = UsersListViewConfig()
    if (rest.getOrNull(0) == NEW_SEGMENT) {
        return listOf(root, WishlistEditViewConfig(null))
    }
    val wishlistId = rest.getOrNull(0)?.toLongOrNull()?.let(::WishlistId) ?: return null
    return when (rest.getOrNull(1)) {
        null -> listOf(root, WishlistViewConfig(wishlistId))
        EDIT_SEGMENT -> listOf(root, WishlistViewConfig(wishlistId), WishlistEditViewConfig(wishlistId))
        ITEM_SEGMENT -> parseItemStack(root, wishlistId, rest.drop(2))
        else -> null
    }
}

private fun parseItemStack(
    root: ViewConfig,
    wishlistId: WishlistId,
    rest: List<String>
): List<ViewConfig>? {
    val wishlist = WishlistViewConfig(wishlistId)
    if (rest.getOrNull(0) == NEW_SEGMENT) {
        return listOf(root, wishlist, WishlistItemEditViewConfig(null, wishlistId))
    }
    val itemId = rest.getOrNull(0)?.toLongOrNull()?.let(::WishlistItemId) ?: return null
    val item = WishlistItemViewConfig(itemId, wishlistId)
    return when (rest.getOrNull(1)) {
        null -> listOf(root, wishlist, item)
        EDIT_SEGMENT -> listOf(root, wishlist, item, WishlistItemEditViewConfig(itemId, wishlistId))
        else -> null
    }
}

/**
 * Rebuilds the navigation hierarchy from the URL path produced by [buildPath].
 *
 * The [appBasePathSegments] prefix is stripped, then the remaining segments are decoded into the
 * main-chain stack via [parseMainStack]. The scaffold skeleton (empty root → scaffold → top + left +
 * main chains) is recreated around that stack so [ScaffoldView] can reattach each restored chain to
 * its slot. The left slot (the Calm Studio sidebar) carries no deep state, so it is restored as a
 * single node seeded from the scaffold's `leftConfig`; without it a reloaded deep link would come back
 * with no sidebar.
 *
 * @return the restored root [ConfigHolder.Chain], or `null` when the path holds no deep-linkable
 * screen (so the default scaffold is created by the standard navigation init path instead).
 */
private fun parsePath(data: LocationData): ConfigHolder.Chain<ViewConfig>? {
    val base = appBasePathSegments()
    val segments = data.pathSegments.filter { it.isNotBlank() }
    val appSegments = if (segments.take(base.size) == base) segments.drop(base.size) else segments
    if (appSegments.isEmpty()) return null

    val mainStack = parseMainStack(appSegments) ?: return null

    val scaffoldConfig = ClientPlugin.mainScaffoldConfig
    val scaffoldNode = ConfigHolder.Node<ViewConfig>(
        config = scaffoldConfig,
        subnode = null,
        subchains = buildList {
            add(
                ConfigHolder.Chain(
                    ConfigHolder.Node<ViewConfig>(TopBarViewConfig(), null, emptyList()),
                    TopNavigationChainId
                )
            )
            scaffoldConfig.leftConfig?.let { leftConfig ->
                add(
                    ConfigHolder.Chain(
                        ConfigHolder.Node<ViewConfig>(leftConfig, null, emptyList()),
                        LeftNavigationChainId
                    )
                )
            }
            add(ConfigHolder.Chain(mainStack.toNodeChain(), MainNavigationChainId))
        }
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
 * Builds the JS [NavigationConfigsRepo] that stores the navigation hierarchy in the page URL path, so
 * deep links to the content screens are shareable and survive a reload.
 */
fun WishlistsAppUrlNavigationConfigsRepo(): NavigationConfigsRepo<ViewConfig> =
    UrlParametersNavigationConfigsRepo(
        buildSearchParams = { holder -> buildPath(holder) },
        parseSearchParams = ::parsePath,
        titleResolver = { SITE_TITLE }
    )
