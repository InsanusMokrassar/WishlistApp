package dev.inmo.wishlist.features.common.common

/**
 * URL path segment under which every server API endpoint is mounted: `/api/...`.
 *
 * Shared by the server routing (which wraps all feature routes in `route(apiPathPart)`) and the
 * client HTTP layer (which prepends this segment to every outgoing request) so the web client can be
 * served from the site root (`/`) while API requests stay collision-free under `/api`.
 */
const val apiPathPart = "api"
