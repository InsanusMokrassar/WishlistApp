package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe opaque identifier of a stored deeplink.
 *
 * Backed by a server-generated UUID [String]; it is the `{deeplink_uuid}` path part of
 * `links/{deeplink_uuid}` and the primary key in
 * [dev.inmo.wishlist.features.deeplinks.common.repo.DeepLinksRepo].
 *
 * @property string Raw UUID value.
 */
@Serializable
@JvmInline
value class DeepLinkId(val string: String)
