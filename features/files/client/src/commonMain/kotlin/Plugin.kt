package dev.inmo.wishlist.features.files.client

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the files client module.
 *
 * Registers:
 * - [KtorFilesFeature] bound as [FilesFeature]
 * - [FilesClientService] — the high-level upload/url helper consumed by UI models
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorFilesFeature(get()) }
        single<FilesFeature> { get<KtorFilesFeature>() }
        single { FilesClientService(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
