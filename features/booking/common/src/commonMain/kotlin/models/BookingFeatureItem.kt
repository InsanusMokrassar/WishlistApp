package dev.inmo.wishlist.features.booking.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.Serializable

/**
 * Feature model returned by
 * [dev.inmo.wishlist.features.booking.client.BookingFeature.myPresentsBooks] — the wishlist items the
 * authenticated caller has booked (the presents the caller plans to make).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set; introduced so the booking feature returns
 * its own model instead of the wishlist feature's persistence entity, per the Feature Interface Return
 * Model Rule.
 *
 * @property id Unique persistent identifier of the booked item.
 * @property wishlistId Wishlist the booked item belongs to.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External links related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class BookingFeatureItem(
    val id: WishlistItemId,
    val wishlistId: WishlistId,
    val title: String,
    val amount: UInt,
    val approximatePrice: Amount?,
    val priceUnits: String,
    val links: List<WishlistItemLink>,
    val description: String,
    val priority: Priority,
    val imageIds: List<FileId>
)

/**
 * Projects this [RegisteredWishlistItem] onto [BookingFeatureItem], carrying every display field
 * through unchanged.
 *
 * @return A [BookingFeatureItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asBookingFeatureItem(): BookingFeatureItem = BookingFeatureItem(
    id = id,
    wishlistId = wishlistId,
    title = title,
    amount = amount,
    approximatePrice = approximatePrice,
    priceUnits = priceUnits,
    links = links,
    description = description,
    priority = priority,
    imageIds = imageIds
)
