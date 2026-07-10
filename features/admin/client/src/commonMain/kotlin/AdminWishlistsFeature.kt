package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.admin.common.models.AdminWishlist
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

interface AdminWishlistsFeature {
    suspend fun getAll(): List<AdminWishlist>
    suspend fun getByUserId(userId: UserId): List<AdminWishlist>
    suspend fun getById(id: WishlistId): AdminWishlist?
    suspend fun create(newWishlist: NewWishlist): AdminWishlist?
    suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean
    suspend fun delete(id: WishlistId): Boolean
}
