package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink

/**
 * Persistent store of declared deeplinks, keyed by [DeepLinkId].
 *
 * Holds each [StoredDeepLink] (id plus the encoded handler-info JSON). Implemented on the server by
 * an Exposed-backed [KeyValueRepo] storing the record as a JSON string (see `ExposedDeepLinksRepo`).
 */
interface DeepLinksRepo : KeyValueRepo<DeepLinkId, StoredDeepLink>
