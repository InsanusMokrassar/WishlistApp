package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin wishlists-management surface
 * ([dev.inmo.wishlist.features.admin.client.AdminWishlistsFeature.getAll]/`getByUserId`/`getById`/`create`,
 * and the equivalent inline handlers in
 * [dev.inmo.wishlist.features.admin.server.configurators.AdminRoutingsConfigurator]).
 *
 * Root-only surface; introduced so the admin feature returns its own model instead of the wishlist
 * feature's persistence entity, per the Feature Interface Return Model Rule.
 *
 * @property id Unique persistent identifier of the wishlist.
 * @property userId Owner of the wishlist.
 * @property title Display name of the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class AdminWishlist(
    val id: WishlistId,
    val userId: UserId,
    val title: String,
    val defaultPriceUnits: String
)

/**
 * Projects this [RegisteredWishlist] onto [AdminWishlist], carrying every field through unchanged.
 *
 * Used by the one `AdminRoutingsConfigurator` wishlist route that bypasses
 * [dev.inmo.wishlist.features.wishlist.server.services.WishlistService] and reads the repo directly
 * (`wishlistsUpdatePathPart`). See [asAdminWishlist] (the [WishlistsFeatureWishlist] overload) for the
 * three routes that go through [dev.inmo.wishlist.features.wishlist.server.services.WishlistService].
 *
 * @return An [AdminWishlist] mirroring this wishlist's full field set.
 */
fun RegisteredWishlist.asAdminWishlist(): AdminWishlist = AdminWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)

/**
 * Projects this [WishlistsFeatureWishlist] onto [AdminWishlist], carrying every field through
 * unchanged.
 *
 * Exists specifically for the three `AdminRoutingsConfigurator` wishlist routes
 * (`wishlistsGetByUserIdPathPart`/`wishlistsGetByIdPathPart`/`wishlistsCreatePathPart`) that call
 * [dev.inmo.wishlist.features.wishlist.server.services.WishlistService] — a service also retyped by
 * this plan (B-V4) to return [WishlistsFeatureWishlist] instead of [RegisteredWishlist]. See §1.1 of
 * `003-architecturing.md` for why a second overload (rather than a single `Registered*`-sourced mapper)
 * is required here.
 *
 * @return An [AdminWishlist] mirroring this wishlist-feature model's full field set.
 */
fun WishlistsFeatureWishlist.asAdminWishlist(): AdminWishlist = AdminWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)
