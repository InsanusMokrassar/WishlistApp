package dev.inmo.wishlist.features.deeplinks.common

/**
 * Shared URL path parts for the deeplinks feature, kept in one place so server routing and any
 * future client code never drift from the wire format.
 */
object DeepLinksConstants {
    /**
     * Path segment under which a deeplink resolves: `GET {deeplink_uuid}`. Served under the standard
     * global `/api` prefix like every other feature, so the full resolved route is
     * `/api/links/{deeplink_uuid}`.
     */
    const val linksPrefixPathPart = "links"

    /**
     * Name of the path parameter holding the deeplink UUID; shared by the route declaration and the
     * handler reading `call.parameters[...]`, so the two never diverge.
     */
    const val deeplinkIdParameter = "deeplink_uuid"
}
