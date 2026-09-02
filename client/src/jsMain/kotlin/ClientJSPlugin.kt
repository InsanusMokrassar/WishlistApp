package dev.inmo.wishlist.client

import androidx.compose.runtime.collectAsState
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.common.client.ui.CalmStudioStyleSheet
import dev.inmo.wishlist.features.common.client.ui.components.Toaster
import dev.inmo.wishlist.features.email.common.EmailConstants
import dev.inmo.wishlist.features.ui.scaffold.ui.ScaffoldViewConfig
import dev.inmo.wishlist.features.ui.sidebar.ui.SidebarViewConfig
import dev.inmo.wishlist.features.ui.topBar.ui.TopBarViewConfig
import dev.inmo.wishlist.features.ui.wishlist.ui.WishlistsListViewConfig
import kotlinx.browser.window
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module
import org.w3c.dom.url.URL

/** Fixed user-facing text shown after a successful email approval redirect. */
private const val EMAIL_APPROVED_MESSAGE = "Email has been approved."

/**
 * Consumes the fixed email-approval marker from [rawUrl] and emits one transient notification.
 *
 * Only the exact shared marker is recognized. Removing every occurrence before startup prevents a
 * page refresh from replaying the notification while preserving the path, unrelated query entries,
 * and fragment.
 *
 * @param rawUrl Absolute browser URL to inspect.
 * @param showMessage Presentation effect for the fixed confirmation message.
 * @param replaceUrl Browser-history effect receiving a same-origin relative URL.
 * @return `true` when the approval marker was consumed.
 */
internal fun consumeEmailApprovalNotification(
    rawUrl: String,
    showMessage: (String) -> Unit,
    replaceUrl: (String) -> Unit,
): Boolean {
    val url = URL(rawUrl)
    if (url.searchParams.get(EmailConstants.approvalQueryParameter) != EmailConstants.approvalQueryValue) {
        return false
    }
    url.searchParams.delete(EmailConstants.approvalQueryParameter)
    showMessage(EMAIL_APPROVED_MESSAGE)
    replaceUrl("${url.pathname}${url.search}${url.hash}")
    return true
}

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

        // Web shell: persistent Calm Studio sidebar on the left, landing on "My Lists".
        ClientPlugin.mainScaffoldConfigProvider = {
            ScaffoldViewConfig(
                topConfig = TopBarViewConfig(),
                leftConfig = SidebarViewConfig(),
                mainConfig = WishlistsListViewConfig()
            )
        }

        single<NavigationConfigsRepo<ViewConfig>> {
            WishlistsAppUrlNavigationConfigsRepo()
//            NavigationConfigsRepo.InMemory()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        consumeEmailApprovalNotification(
            rawUrl = window.location.href,
            showMessage = Toaster::show,
            replaceUrl = { cleanedRelativeUrl ->
                window.history.replaceState(null, "", cleanedRelativeUrl)
            },
        )
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        // Register the Calm Studio design stylesheet into the aggregator before the first draw. The
        // views reference its raw class strings (`.btn`, `.card`, …), so nothing else loads the object.
        CalmStudioStyleSheet.ensureRegistered()
        renderComposable("content") {
            StyleSheetsAggregator.draw()
            ClientPlugin.currentDrawingBlock.collectAsState().value.invoke()
        }
    }
}
