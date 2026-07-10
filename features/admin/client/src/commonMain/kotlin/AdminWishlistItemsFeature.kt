package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId

interface AdminWishlistItemsFeature {
    suspend fun getByWishlistId(wishlistId: WishlistId): List<AdminWishlistItem>
    suspend fun create(item: NewWishlistItem): AdminWishlistItem?
    suspend fun update(id: WishlistItemId, item: NewWishlistItem): Boolean
    suspend fun delete(id: WishlistItemId): Boolean
}
