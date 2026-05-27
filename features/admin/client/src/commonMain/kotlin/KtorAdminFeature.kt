package dev.inmo.wishlist.features.admin.client

class KtorAdminFeature(
    override val usersManagement: UsersManagementFeature,
    override val wishlists: AdminWishlistsFeature,
    override val wishlistItems: AdminWishlistItemsFeature
) : AdminFeature
