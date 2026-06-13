package dev.inmo.wishlist.features.wishlist.common.models

import kotlinx.serialization.Serializable

/**
 * Wishlist creation request received from the client — no [dev.inmo.wishlist.features.users.common.models.UserId] field.
 *
 * The server resolves the owner from the authenticated call context;
 * clients must not include a user identifier in this payload.
 *
 * @property title Display name for the new wishlist.
 * @property defaultPriceUnits Default currency/units label pre-filled into new items of this wishlist;
 * empty when the wishlist has no default.
 */
@Serializable
data class NewWishlistInFeature(
    val title: String,
    val defaultPriceUnits: String = ""
)