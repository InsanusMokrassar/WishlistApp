package dev.inmo.wishlist.features.admin.client

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId

interface AdminWishlistsFeature {
    suspend fun getAll(): List<RegisteredWishlist>
    suspend fun getByUserId(userId: UserId): List<RegisteredWishlist>
    suspend fun getById(id: WishlistId): RegisteredWishlist?
    suspend fun create(newWishlist: NewWishlist): RegisteredWishlist?
    suspend fun update(id: WishlistId, newWishlist: NewWishlistInFeature): Boolean
    suspend fun delete(id: WishlistId): Boolean
}
