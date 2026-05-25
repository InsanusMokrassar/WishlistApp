package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.common.WishlistsFeature
import dev.inmo.wishlist.features.wishlist.common.WishlistsItemsFeature
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the wishlist client module.
 *
 * Registers in Koin:
 * - [KtorWishlistFeature] (concrete + [WishlistsFeature] binding)
 * - [KtorWishlistItemFeature] (concrete + [WishlistsItemsFeature] binding)
 *
 * Platform-specific plugins (JSPlugin, JVMPlugin, AndroidPlugin) delegate to this object.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorWishlistFeature(get()) }
        single<WishlistsFeature> { get<KtorWishlistFeature>() }

        single { KtorWishlistItemFeature(get()) }
        single<WishlistsItemsFeature> { get<KtorWishlistItemFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
