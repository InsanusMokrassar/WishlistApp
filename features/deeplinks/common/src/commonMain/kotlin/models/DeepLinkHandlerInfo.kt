package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * Serializable record stored as JSON for each deeplink.
 *
 * [handlerId] selects the owning [dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler] via the
 * service's handler map; [value] is that handler's own payload, serialized polymorphically. Because
 * [value] is `@Polymorphic Any`, its concrete runtime type must be registered by the owning
 * handler-providing feature with `polymorphic(Any::class, T::class, T.serializer())` in a Koin
 * `SerializersModule`; the global `Json` (`features/common/common/.../Plugin.kt`,
 * `useArrayPolymorphism = true`) aggregates every such module via `getAllDistinct<SerializersModule>()`
 * and resolves the value at encode/decode time. An unregistered value type fails fast with a
 * `SerializationException`.
 *
 * @property handlerId Identifier of the handler that owns this deeplink; the dispatcher looks the
 *   handler up by it.
 * @property value The owning handler's own payload, serialized via the global polymorphic module and
 *   handed back to that handler (without the id) at dispatch time.
 */
@Serializable
data class DeepLinkHandlerInfo(
    val handlerId: DeepLinkHandlerId,
    @Polymorphic val value: Any
)
