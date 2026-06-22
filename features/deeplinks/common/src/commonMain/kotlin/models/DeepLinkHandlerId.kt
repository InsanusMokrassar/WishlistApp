package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier of a [dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler].
 *
 * It is the dispatch key: stored inside [DeepLinkHandlerInfo] when a deeplink is minted, used as the
 * map key in `DeepLinksService.handlersById`, and declared as the identity property of every
 * [dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler]. Handler ids MUST be globally unique;
 * registering two handlers under the same id is a startup-fatal configuration error.
 *
 * @property string Raw handler-id value chosen by the owning handler-providing feature.
 */
@Serializable
@JvmInline
value class DeepLinkHandlerId(val string: String)
