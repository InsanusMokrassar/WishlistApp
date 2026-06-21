package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier for a deep link. Wraps a UUID string used as the primary key
 * in storage and as the path segment in `GET /api/links/{deeplinkId}`.
 *
 * @param string UUID string value generated via [com.benasher44.uuid.uuid4].
 */
@Serializable
@JvmInline
value class DeepLinkId(val string: String)
