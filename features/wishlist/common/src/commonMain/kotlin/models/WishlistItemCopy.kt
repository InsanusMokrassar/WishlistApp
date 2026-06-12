package dev.inmo.wishlist.features.wishlist.common.models

/**
 * Produces a [NewWishlistItem] that is a deep copy of the value fields of this registered item,
 * re-parented to [targetWishlistId].
 *
 * Every user-visible field (title, amount, price, units, links, description, priority, image ids) is
 * carried over. The original id is dropped so the repository assigns a brand-new identity. Image ids
 * are referenced as-is (the underlying file blobs live in the shared files store), which makes the
 * resulting record a faithful copy without duplicating binary payloads.
 *
 * @param targetWishlistId Wishlist that will own the copied item.
 * @return A create payload describing the new item.
 */
fun RegisteredWishlistItem.toNewItem(targetWishlistId: WishlistId): NewWishlistItem =
    NewWishlistItem(
        wishlistId = targetWishlistId,
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
 * Identity comparison used by the copy idempotency checks: two items are considered "the same item"
 * when all of their user-visible value fields match, ignoring their ids and parent wishlist.
 *
 * Used to decide whether a source item already exists in a recipient wishlist (so a re-run after a
 * reload does not produce duplicates) and to short-circuit a redundant single-item copy.
 *
 * @param other Candidate item to compare against (typically an already-present recipient item).
 * @return `true` when every compared value field is equal.
 */
fun WishlistItem.hasSameContentAs(other: WishlistItem): Boolean =
    title == other.title &&
        amount == other.amount &&
        approximatePrice == other.approximatePrice &&
        priceUnits == other.priceUnits &&
        links == other.links &&
        description == other.description &&
        priority == other.priority &&
        imageIds == other.imageIds
