package dev.inmo.wishlist.features.deeplinks.server.services

import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.repos.set
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo
import dev.inmo.wishlist.features.deeplinks.server.models.HandleResult

/**
 * Server-only, in-process API for the deeplinks feature: mints deeplinks with attached handler info
 * and dispatches an opened deeplink to the handler registered under its stored [DeepLinkHandlerId].
 *
 * There is NO public HTTP create endpoint — other server features call [createDeepLink] directly to
 * mint a link (avoiding unauthenticated link minting). The collection of handlers is snapshotted once
 * at construction (via `getAllDistinct()` in the server plugin) and indexed by
 * [DeepLinkHandler.id]; the feature ships zero concrete handlers, so until another feature registers
 * one [handle] returns [HandleResult.Unhandled] for any stored link.
 *
 * @param repo Persistent store of `DeepLinkId -> DeepLinkHandlerInfo`.
 * @param handlers Every registered [DeepLinkHandler]; indexed by [DeepLinkHandler.id] at construction.
 *   Two handlers sharing one id is a configuration error and fails fast (see init).
 */
class DeepLinksService(
    private val repo: DeepLinksRepo,
    handlers: List<DeepLinkHandler>
) {
    /**
     * Dispatch index: every registered [DeepLinkHandler] keyed by its [DeepLinkHandler.id].
     *
     * Built eagerly at construction; a duplicate id throws [IllegalArgumentException] so a registration
     * mistake surfaces at startup rather than silently dropping a handler at link-open time.
     */
    private val handlersById: Map<DeepLinkHandlerId, DeepLinkHandler> =
        handlers.groupBy { it.id }
            .mapValues { (id, list) ->
                require(list.size == 1) { "Duplicate DeepLinkHandlerId: $id -> ${list.size} handlers" }
                list.single()
            }

    /**
     * Generate a fresh UUID, persist a [DeepLinkHandlerInfo] binding [handlerId] to [value], and return
     * the new id.
     *
     * Server-only; the caller (another feature) supplies the id of the handler that will own the link
     * plus the handler's own value. The value is serialized polymorphically (see [DeepLinkHandlerInfo]),
     * so its concrete type must be registered via `polymorphic(Any::class, ...)` by the owning feature.
     *
     * @param handlerId Id of the handler that will own (and later process) this deeplink.
     * @param value The owning handler's own payload to attach to the new deeplink.
     * @return Identifier of the newly created deeplink.
     */
    suspend fun createDeepLink(handlerId: DeepLinkHandlerId, value: Any): DeepLinkId {
        val id = DeepLinkId(uuid4().toString())
        repo.set(id, DeepLinkHandlerInfo(handlerId, value))
        return id
    }

    /**
     * Resolve an opened deeplink.
     *
     * Loads the stored [DeepLinkHandlerInfo]; if absent returns [HandleResult.NotFound]. Otherwise looks
     * the owning handler up by [DeepLinkHandlerInfo.handlerId]; if no handler is registered under that id
     * returns [HandleResult.Unhandled]. Otherwise passes the stored [DeepLinkHandlerInfo.value] (without
     * the id) to [DeepLinkHandler.tryHandle], returning [HandleResult.Handled] on `true` and
     * [HandleResult.Unhandled] on `false`.
     *
     * @param deeplinkId Identifier of the opened deeplink.
     * @return The dispatch outcome.
     */
    suspend fun handle(deeplinkId: DeepLinkId): HandleResult {
        val info = repo.get(deeplinkId) ?: return HandleResult.NotFound
        val handler = handlersById[info.handlerId] ?: return HandleResult.Unhandled
        return when (handler.tryHandle(deeplinkId, info.value)) {
            true -> HandleResult.Handled
            false -> HandleResult.Unhandled
        }
    }
}
