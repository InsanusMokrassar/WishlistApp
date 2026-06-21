package dev.inmo.wishlist.features.deeplinks.common.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId

/**
 * Persistent store mapping each [DeepLinkId] to its [DeepLinkHandlerInfo].
 *
 * Implemented on the server by an Exposed-backed [KeyValueRepo] (see `ExposedDeepLinksRepo`).
 * Mirrors `FilesMetaInfoRepo`: a single JSON-blob key-value store with no read/write split and no
 * cache, since deeplinks are write-once (mint) and read-rarely (only when a link is opened).
 */
interface DeepLinksRepo : KeyValueRepo<DeepLinkId, DeepLinkHandlerInfo>
