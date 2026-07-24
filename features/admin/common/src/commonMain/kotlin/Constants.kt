package dev.inmo.wishlist.features.admin.common

import dev.inmo.wishlist.features.roles.common.FunctionalityId

object Constants {
    /** Role-gated functionality id for the whole `/admin/...` route surface (`AdminRoutingsConfigurator`). */
    val adminPanelFunctionalityId = FunctionalityId("admin.panel")

    const val adminPrefixPathPart = "admin"

    const val usersPathPart = "users"
    const val usersGetAllPathPart = "getAll"
    const val usersCreatePathPart = "create"
    const val usersUpdatePathPart = "update"
    const val usersSetPasswordPathPart = "setPassword"
    const val usersDeletePathPart = "delete"

    const val wishlistsPathPart = "wishlists"
    const val wishlistsGetByUserIdPathPart = "getByUserId"
    const val wishlistsGetByIdPathPart = "getById"
    const val wishlistsCreatePathPart = "create"
    const val wishlistsUpdatePathPart = "update"
    const val wishlistsDeletePathPart = "delete"

    const val usersGetByIdPathPart = "getById"
    const val wishlistsGetAllPathPart = "getAll"
    const val wishlistItemsPathPart = "wishlistItems"
    const val wishlistItemsGetByWishlistIdPathPart = "getByWishlistId"
    const val wishlistItemsCreatePathPart = "create"
    const val wishlistItemsUpdatePathPart = "update"
    const val wishlistItemsDeletePathPart = "delete"
}
