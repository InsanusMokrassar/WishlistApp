package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Type-safe identifier for a [Wishlist]. Backed by a [Long] primary key. */
@Serializable
@JvmInline
value class WishlistId(val long: Long)

/**
 * Common interface for all wishlist variants.
 *
 * Sealed to allow exhaustive handling of [NewWishlist] and [RegisteredWishlist].
 */
@Serializable
sealed interface Wishlist {
    /** Owner of this wishlist. */
    val userId: UserId

    /** Human-readable name of this wishlist. */
    val title: String

    /**
     * Default currency/units label pre-filled into the price-units field when a new item is created
     * in this wishlist (e.g. `"$"`, `"USD"`). Empty when the wishlist has no default.
     */
    val defaultPriceUnits: String
}

/**
 * Wishlist not yet persisted — used as input to repository create operations.
 *
 * @property userId Owner of the new wishlist.
 * @property title Display name for the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class NewWishlist(
    override val userId: UserId,
    override val title: String,
    override val defaultPriceUnits: String = ""
) : Wishlist

/**
 * Wishlist already persisted in the database.
 *
 * @property id Unique persistent identifier assigned by the server.
 * @property userId Owner of the wishlist.
 * @property title Display name for the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class RegisteredWishlist(
    val id: WishlistId,
    override val userId: UserId,
    override val title: String,
    override val defaultPriceUnits: String = ""
) : Wishlist
