package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier of a declared deeplink.
 *
 * Backed by an opaque [String] (a UUID generated server-side when the deeplink is created). Used as
 * the primary key of the deeplink store and as the path segment in the resolution URL
 * `links/<deeplink_uuid>`.
 *
 * @property string Raw identifier value.
 */
@Serializable
@JvmInline
value class DeepLinkId(val string: String)
