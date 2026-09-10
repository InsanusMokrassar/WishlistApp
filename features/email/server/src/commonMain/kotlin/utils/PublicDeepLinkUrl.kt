package dev.inmo.wishlist.features.email.server.utils

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import java.net.URI

/**
 * Builds the fixed externally reachable deeplink URL for a server-minted identifier.
 *
 * @param publicHttpOrigin Configured public HTTP(S) origin.
 * @param deeplinkId Persisted deeplink identifier.
 * @return Absolute `/api/links/{id}` URL.
 */
internal fun buildPublicDeepLinkUrl(publicHttpOrigin: String, deeplinkId: DeepLinkId): String =
    "${normalizePublicHttpOrigin(publicHttpOrigin)}/api/links/${deeplinkId.string}"

/**
 * Validates and normalizes an externally configured HTTP(S) origin.
 *
 * @param publicHttpOrigin Candidate origin.
 * @return Origin without a trailing slash, path, credentials, query, or fragment.
 */
internal fun normalizePublicHttpOrigin(publicHttpOrigin: String): String {
    require(publicHttpOrigin.isNotBlank()) { "publicHttpOrigin must not be blank" }
    val uri = try {
        URI(publicHttpOrigin)
    } catch (error: Exception) {
        throw IllegalArgumentException("publicHttpOrigin must be a valid absolute URI", error)
    }
    val scheme = uri.scheme?.lowercase()
    require(scheme == "http" || scheme == "https") { "publicHttpOrigin must use http or https" }
    require(!uri.host.isNullOrBlank()) { "publicHttpOrigin must contain a host" }
    require(uri.rawUserInfo == null) { "publicHttpOrigin must not contain credentials" }
    require(uri.rawQuery == null) { "publicHttpOrigin must not contain a query" }
    require(uri.rawFragment == null) { "publicHttpOrigin must not contain a fragment" }
    require(uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") {
        "publicHttpOrigin must not contain a path"
    }
    return "$scheme://${uri.rawAuthority}"
}
