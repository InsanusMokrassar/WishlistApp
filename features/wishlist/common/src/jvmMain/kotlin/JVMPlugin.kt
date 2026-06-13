package dev.inmo.wishlist.features.wishlist.common

import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.common.repo.CacheWishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.CacheWishlistRepo
import dev.inmo.wishlist.features.wishlist.common.repo.ExposedWishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.ExposedWishlistRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM-specific startup plugin for the wishlist common module.
 *
 * Registers Exposed JDBC repos wrapped in in-memory cache repos:
 * - [ExposedWishlistRepo] → [CacheWishlistRepo] bound as [WishlistRepo]
 * - [ExposedWishlistItemRepo] → [CacheWishlistItemRepo] bound as [WishlistItemRepo]
 *
 * Booking repositories were extracted into `features/booking`.
 *
 * Also delegates to [Plugin] for platform-agnostic DI bindings.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        single { ExposedWishlistRepo(get()) }
        singleWithBinds<WishlistRepo> {
            CacheWishlistRepo(originalRepo = get<ExposedWishlistRepo>(), scope = get())
        }

        single { ExposedWishlistItemRepo(get()) }
        singleWithBinds<WishlistItemRepo> {
            CacheWishlistItemRepo(originalRepo = get<ExposedWishlistItemRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
