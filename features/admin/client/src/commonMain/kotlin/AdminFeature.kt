package dev.inmo.wishlist.features.admin.client

interface AdminFeature {
    val usersManagement: UsersManagementFeature
    val wishlists: AdminWishlistsFeature
    val wishlistItems: AdminWishlistItemsFeature
}
