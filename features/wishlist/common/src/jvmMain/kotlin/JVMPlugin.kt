package dev.inmo.wishlist.features.wishlist.common

import dev.inmo.micro_utils.koin.singleWithBinds
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.wishlist.common.repo.BookingRepo
import dev.inmo.wishlist.features.wishlist.common.repo.CacheBookingRepo
import dev.inmo.wishlist.features.wishlist.common.repo.CacheWishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.CacheWishlistRepo
import dev.inmo.wishlist.features.wishlist.common.repo.ExposedBookingRepo
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
 * - [ExposedBookingRepo] → [CacheBookingRepo] bound as [BookingRepo]
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

        single { ExposedBookingRepo(get()) }
        singleWithBinds<BookingRepo> {
            CacheBookingRepo(originalRepo = get<ExposedBookingRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
