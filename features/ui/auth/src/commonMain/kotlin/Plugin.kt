package dev.inmo.wishlist.features.ui.auth

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.client.ClientAuthFeature
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthModel
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewConfig
import dev.inmo.wishlist.features.ui.auth.ui.AuthViewModel
import dev.inmo.wishlist.features.ui.auth.ui.DefaultAuthModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Platform-agnostic startup plugin for the auth widget.
 *
 * Registers the polymorphic serializer for [AuthViewConfig], the [AuthViewModel]
 * factory, and the [AuthModel] singleton composing [AuthCredentialsStorage] +
 * [ClientAuthFeature].
 *
 * Note: server URL editing is owned by `features/ui/serverUrl` — this plugin no
 * longer depends on `ServerUrlStorage`.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, AuthViewConfig::class, AuthViewConfig.serializer())
                polymorphic(ViewConfig::class, AuthViewConfig::class, AuthViewConfig.serializer())
            }
        }
        factory { AuthViewModel(node = it.get(), model = get(), interactor = get()) }
        single<AuthModel> {
            DefaultAuthModel(
                authFeature = get(),
                credentialsStorage = get(),
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
