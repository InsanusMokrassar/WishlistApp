package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

/**
 * Full CRUD repository for [RegisteredWishlist] entities.
 *
 * Combines [ReadWishlistRepo] and [WriteWishlistRepo] into a single interface
 * used by service and cache layers.
 */
interface WishlistRepo : ReadWishlistRepo, WriteWishlistRepo, CRUDRepo<RegisteredWishlist, WishlistId, NewWishlist>
