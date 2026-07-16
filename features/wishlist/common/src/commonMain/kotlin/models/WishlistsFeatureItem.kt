package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature]'s
 * read/create/copy operations (`getByWishlistId`/`create`/`copy`).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set; introduced so the client-facing wishlist
 * item feature returns its own model instead of the persistence entity directly, per the Feature
 * Interface Return Model Rule. A separate type from
 * [dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem] and
 * [dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem] by design — see the Feature
 * Interface Return Model Rule in `agents/CODING.md`.
 *
 * @property id Unique persistent identifier of the item.
 * @property wishlistId Wishlist the item belongs to.
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
data class WishlistsFeatureItem(
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
 * Projects this [RegisteredWishlistItem] onto [WishlistsFeatureItem], carrying every display field
 * through unchanged.
 *
 * @return A [WishlistsFeatureItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asWishlistsFeatureItem(): WishlistsFeatureItem = WishlistsFeatureItem(
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

/**
 * Projects this [WishlistsFeatureItem] back onto the persistence-layer [RegisteredWishlistItem],
 * carrying every display field through unchanged (this feature model mirrors the base's full field
 * set, so no extra arguments are required).
 *
 * @return A [RegisteredWishlistItem] mirroring this model's full field set.
 */
fun WishlistsFeatureItem.asRegisteredWishlistItem(): RegisteredWishlistItem = RegisteredWishlistItem(
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
