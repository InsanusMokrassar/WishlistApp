package dev.inmo.wishlist.features.ui.serverUrl

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.ServerUrlStorage
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlModel
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewConfig
import dev.inmo.wishlist.features.ui.serverUrl.ui.ServerUrlViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the server URL editor UI feature.
 *
 * Registers polymorphic serializer for [ServerUrlViewConfig], the
 * [ServerUrlViewModel] factory, and the [ServerUrlModel] singleton wrapping
 * the shared [ServerUrlStorage] from `features/auth/client`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, ServerUrlViewConfig::class, ServerUrlViewConfig.serializer())
                polymorphic(ViewConfig::class, ServerUrlViewConfig::class, ServerUrlViewConfig.serializer())
            }
        }
        factory { ServerUrlViewModel(node = it.get(), model = get(), interactor = get()) }
        single<ServerUrlModel> {
            val storage = get<ServerUrlStorage>()
            object : ServerUrlModel {
                override suspend fun getServerUrl(): String? = storage.getServerUrl()
                override suspend fun saveServerUrl(url: String?) {
                    storage.saveServerUrl(url?.takeIf { it.isNotBlank() })
                }
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
