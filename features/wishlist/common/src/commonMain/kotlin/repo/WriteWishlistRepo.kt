package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Write-only repository for [RegisteredWishlist] entities.
 *
 * Accepts [NewWishlist] as the input type for create and update operations.
 */
interface WriteWishlistRepo : WriteCRUDRepo<RegisteredWishlist, WishlistId, NewWishlist>
