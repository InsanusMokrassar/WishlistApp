package dev.inmo.wishlist.features.deeplinks.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.models.HandleResult

/**
 * Server-only, in-process API for the deeplinks feature: mints deeplinks with attached handler info
 * and dispatches an opened deeplink to the first handler that claims it.
 *
 * There is NO public HTTP create endpoint — other server features call [createDeepLink] directly to
 * mint a link (avoiding unauthenticated link minting). The collection of [handlers] is snapshotted
 * once at construction via `getAllDistinct()` in the server plugin; the feature ships zero concrete
 * handlers, so until another feature registers one [handle] returns [HandleResult.Unhandled] for any
 * stored link.
 *
 * @param repo Persistent store of `DeepLinkId -> DeepLinkHandlerInfo`.
 * @param handlers Every registered [DeepLinkHandler]; dispatch is first-true-wins in list order.
 */
class DeepLinksService(
    private val repo: DeepLinksRepo,
    private val handlers: List<DeepLinkHandler>
) {
    /**
     * Generate a fresh UUID, persist `id -> handlerInfo`, and return the new id.
     *
     * Server-only; the caller (another feature) constructs the [DeepLinkHandlerInfo] (its own `type`
     * key plus its payload encoded to a `JsonElement`) for its own handler.
     *
     * @param handlerInfo Handler info to attach to the new deeplink.
     * @return Identifier of the newly created deeplink.
     */
    suspend fun createDeepLink(handlerInfo: DeepLinkHandlerInfo): DeepLinkId {
        val id = DeepLinkId(uuid4().toString())
        repo.set(id, handlerInfo)
        return id
    }

    /**
     * Resolve an opened deeplink.
     *
     * Loads the stored [DeepLinkHandlerInfo]; if absent returns [HandleResult.NotFound]. Otherwise
     * passes it (as `Any`) to each registered [DeepLinkHandler.tryHandle] in turn, returning
     * [HandleResult.Handled] at the first `true`; if none claim it returns [HandleResult.Unhandled].
     *
     * @param deeplinkId Identifier of the opened deeplink.
     * @return The dispatch outcome.
     */
    suspend fun handle(deeplinkId: DeepLinkId): HandleResult {
        val info = repo.get(deeplinkId) ?: return HandleResult.NotFound
        handlers.forEach { handler ->
            if (handler.tryHandle(deeplinkId, info)) {
                return HandleResult.Handled
            }
        }
        return HandleResult.Unhandled
    }
}
