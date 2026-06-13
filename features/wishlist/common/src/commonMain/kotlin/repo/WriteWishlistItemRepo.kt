package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Write-only repository for [RegisteredWishlistItem] entities.
 *
 * Accepts [NewWishlistItem] as the input type for create and update operations.
 */
interface WriteWishlistItemRepo : WriteCRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>
