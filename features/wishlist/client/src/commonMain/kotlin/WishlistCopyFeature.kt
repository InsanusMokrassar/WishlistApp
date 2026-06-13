package dev.inmo.wishlist.features.wishlist.client

import dev.inmo.wishlist.features.wishlist.common.models.CopyWishlistRequest

/**
 * Feature contract for enqueuing a whole-wishlist deep copy into the caller's profile.
 *
 * Copying a wishlist is asynchronous on the server: the request only enqueues a persistent job that
 * is processed in the background and survives reloads. The recipient is always the authenticated
 * caller, enforced server-side.
 *
 * Implemented on client by [dev.inmo.wishlist.features.wishlist.client.KtorWishlistCopyFeature].
 */
interface WishlistCopyFeature {
    /**
     * Enqueues a background job that deep-copies the wishlist named in [request] into the caller's
     * profile.
     *
     * @param request Source wishlist to copy (may belong to any user).
     * @return `true` when the job was accepted/queued, `false` on failure (e.g. not authenticated).
     */
    suspend fun enqueueCopy(request: CopyWishlistRequest): Boolean
}
