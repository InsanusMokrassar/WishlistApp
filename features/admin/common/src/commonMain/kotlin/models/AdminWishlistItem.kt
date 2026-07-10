package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin wishlist-items-management surface
 * ([dev.inmo.wishlist.features.admin.client.AdminWishlistItemsFeature.getByWishlistId]/`create`, and the
 * equivalent inline handlers in
 * [dev.inmo.wishlist.features.admin.server.configurators.AdminRoutingsConfigurator], all of which read
 * `WishlistItemRepo` directly rather than through a service).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set. A separate type from
 * [dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem] and
 * [dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem] even though the field sets
 * coincide today — each `*Feature` surface owns its own model per the Feature Interface Return Model
 * Rule, so the three can evolve independently.
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
data class AdminWishlistItem(
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
 * Projects this [RegisteredWishlistItem] onto [AdminWishlistItem], carrying every display field
 * through unchanged.
 *
 * @return An [AdminWishlistItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asAdminWishlistItem(): AdminWishlistItem = AdminWishlistItem(
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
