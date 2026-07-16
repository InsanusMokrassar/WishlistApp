package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.wishlist.client.WishlistsFeature]'s
 * read/create operations (`getById`/`getByUserId`/`getMyWishlists`/`create`).
 *
 * Mirrors [RegisteredWishlist] verbatim; introduced so the client-facing wishlist feature returns its
 * own model instead of the persistence entity directly, per the Feature Interface Return Model Rule.
 *
 * @property id Unique persistent identifier of the wishlist.
 * @property userId Owner of the wishlist.
 * @property title Display name of the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class WishlistsFeatureWishlist(
    val id: WishlistId,
    val userId: UserId,
    val title: String,
    val defaultPriceUnits: String
)

/**
 * Projects this [RegisteredWishlist] onto [WishlistsFeatureWishlist], carrying every field through
 * unchanged.
 *
 * @return A [WishlistsFeatureWishlist] mirroring this wishlist's full field set.
 */
fun RegisteredWishlist.asWishlistsFeatureWishlist(): WishlistsFeatureWishlist = WishlistsFeatureWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)

/**
 * Projects this [WishlistsFeatureWishlist] back onto the persistence-layer [RegisteredWishlist],
 * carrying every field through unchanged (this feature model mirrors the base verbatim, so no extra
 * arguments are required).
 *
 * @return A [RegisteredWishlist] mirroring this model's full field set.
 */
fun WishlistsFeatureWishlist.asRegisteredWishlist(): RegisteredWishlist = RegisteredWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)
