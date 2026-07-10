package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [AdminWishlistItem]'s wire shape and its [asAdminWishlistItem] mapper. */
class AdminWishlistItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 2u,
        approximatePrice = Amount(50.0),
        priceUnits = "EUR",
        links = emptyList(),
        description = "",
        priority = Priority.Small,
        imageIds = emptyList()
    )

    /** Encoded JSON carries exactly the item's nine declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val json = Json.encodeToJsonElement(AdminWishlistItem.serializer(), fixture.asAdminWishlistItem()).jsonObject

        assertEquals(
            setOf(
                "id", "wishlistId", "title", "amount", "approximatePrice",
                "priceUnits", "links", "description", "priority", "imageIds"
            ),
            json.keys
        )
    }

    /** Every display field is projected unchanged from the source [RegisteredWishlistItem]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        assertEquals(
            AdminWishlistItem(
                id = WishlistItemId(1L),
                wishlistId = WishlistId(2L),
                title = "Bicycle",
                amount = 2u,
                approximatePrice = Amount(50.0),
                priceUnits = "EUR",
                links = emptyList(),
                description = "",
                priority = Priority.Small,
                imageIds = emptyList()
            ),
            fixture.asAdminWishlistItem()
        )
    }
}
