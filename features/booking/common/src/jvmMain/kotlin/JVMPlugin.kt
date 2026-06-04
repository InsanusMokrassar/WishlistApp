package dev.inmo.wishlist.features.booking.common

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.booking.common.repo.BookingRepo
import dev.inmo.wishlist.features.booking.common.repo.CacheBookingRepo
import dev.inmo.wishlist.features.booking.common.repo.ExposedBookingRepo
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JVM-specific startup plugin for the booking common module.
 *
 * Registers the Exposed JDBC booking repo wrapped in an in-memory cache repo:
 * - [ExposedBookingRepo] → [CacheBookingRepo] bound as [BookingRepo].
 *
 * The required [org.jetbrains.exposed.v1.jdbc.Database] singleton is provided by
 * `features/common/server`, so this plugin must run after the common server plugin.
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (Plugin) { setupDI(config) }

        single { ExposedBookingRepo(get()) }
        single { CacheBookingRepo(originalRepo = get<ExposedBookingRepo>(), scope = get()) }
        single<BookingRepo> { get<CacheBookingRepo>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
