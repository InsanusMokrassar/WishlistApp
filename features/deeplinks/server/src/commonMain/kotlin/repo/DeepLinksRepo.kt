package dev.inmo.wishlist.features.deeplinks.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.models.StoredDeepLink

/**
 * Persistence contract for deep links. Key = [DeepLinkId], Value = [StoredDeepLink].
 *
 * JVM target implementation: [ExposedDeepLinksRepo] (Exposed JDBC backed by table `deeplinks`).
 */
interface DeepLinksRepo : KeyValueRepo<DeepLinkId, StoredDeepLink>
