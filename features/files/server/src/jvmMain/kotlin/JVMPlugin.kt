package dev.inmo.wishlist.features.files.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.files.common.repo.DiskFilesRepo
import dev.inmo.wishlist.features.files.common.repo.ExposedFilesMetaInfoRepo
import dev.inmo.wishlist.features.files.common.repo.FilesMetaInfoRepo
import dev.inmo.wishlist.features.files.common.repo.FilesRepo
import dev.inmo.wishlist.features.files.server.models.FilesConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import java.io.File

/**
 * JVM-specific startup plugin for the files server module.
 *
 * Adds the JVM-only repo bindings to the DI graph:
 * - [FilesRepo] backed by [DiskFilesRepo] under the directory from [FilesConfig.filesFolder]
 *   (decoded from the same root config JSON used by `Config`/`KtorConfig`)
 * - [FilesMetaInfoRepo] backed by [ExposedFilesMetaInfoRepo]
 *
 * `Database` and `Json` are resolved from the DI graph populated by `features/common/server`.
 * Delegates DI setup to the common files [Plugin] (services + routing configurators).
 */
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(dev.inmo.wishlist.features.files.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        single<FilesRepo> {
            val filesConfig = get<Json>().decodeFromJsonElement(FilesConfig.serializer(), config)
            DiskFilesRepo(File(filesConfig.filesFolder))
        }
        single<FilesMetaInfoRepo> {
            ExposedFilesMetaInfoRepo(get(), get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        dev.inmo.wishlist.features.files.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
