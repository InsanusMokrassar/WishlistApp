package dev.inmo.wishlist.client

import androidx.compose.runtime.collectAsState
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * JS-platform shell.
 *
 * Provides a URL-parameter-backed navigation configs repository (so deep links to
 * content screens are shareable and survive a reload) and bootstraps the root chain
 * with the main scaffold layout. The browser controls the server origin, so no server
 * URL editor is registered.
 */
object ClientJSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(ClientPlugin) { setupDI(config) }

        single<NavigationConfigsRepo<ViewConfig>> {
            urlNavigationConfigsRepo()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        renderComposable("content") {
            ClientPlugin.currentDrawingBlock.collectAsState().value.invoke()
        }
    }
}
