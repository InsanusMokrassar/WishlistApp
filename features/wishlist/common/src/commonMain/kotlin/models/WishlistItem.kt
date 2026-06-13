package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Type-safe identifier for a [WishlistItem]. Backed by a [Long] primary key. */
@Serializable
@JvmInline
value class WishlistItemId(val long: Long)

/**
 * Common interface for all wishlist item variants.
 *
 * Sealed to allow exhaustive handling of [NewWishlistItem] and [RegisteredWishlistItem].
 */
@Serializable
sealed interface WishlistItem {
    /** Wishlist this item belongs to. */
    val wishlistId: WishlistId

    /** Short display name of the item. */
    val title: String

    /** Desired quantity of the item; always `>= 1` and defaults to `1`. */
    val amount: UInt

    /** Optional approximate price; `null` means no price specified. */
    val approximatePrice: Amount?

    /** Currency or unit label for [approximatePrice] (e.g. `"$"`, `"€"`). Empty when not applicable. */
    val priceUnits: String

    /** External URLs related to the item (e.g. product pages). */
    val links: List<String>

    /** Additional free-form description. */
    val description: String

    /** Relative importance of the item; defaults to [Priority.Medium]. */
    val priority: Priority

    /** Ids of images attached to the item, in display order. Empty when the item has no images. */
    val imageIds: List<FileId>
}

/**
 * Wishlist item not yet persisted — used as input to create operations.
 *
 * @property wishlistId Parent wishlist.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item; defaults to `1`.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External URLs related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class NewWishlistItem(
    override val wishlistId: WishlistId,
    override val title: String,
    override val amount: UInt = 1u,
    override val approximatePrice: Amount? = null,
    override val priceUnits: String = "",
    override val links: List<String> = emptyList(),
    override val description: String = "",
    override val priority: Priority = Priority.Medium,
    override val imageIds: List<FileId> = emptyList()
) : WishlistItem

/**
 * Wishlist item already persisted in the database.
 *
 * @property id Unique persistent identifier assigned by the server.
 * @property wishlistId Parent wishlist.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item; defaults to `1`.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External URLs related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class RegisteredWishlistItem(
    val id: WishlistItemId,
    override val wishlistId: WishlistId,
    override val title: String,
    override val amount: UInt = 1u,
    override val approximatePrice: Amount? = null,
    override val priceUnits: String = "",
    override val links: List<String> = emptyList(),
    override val description: String = "",
    override val priority: Priority = Priority.Medium,
    override val imageIds: List<FileId> = emptyList()
) : WishlistItem
