package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

/**
 * Full CRUD repository for [RegisteredWishlistItem] entities.
 *
 * Combines [ReadWishlistItemRepo] and [WriteWishlistItemRepo] into a single interface
 * used by service and cache layers.
 */
interface WishlistItemRepo : ReadWishlistItemRepo, WriteWishlistItemRepo, CRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>
