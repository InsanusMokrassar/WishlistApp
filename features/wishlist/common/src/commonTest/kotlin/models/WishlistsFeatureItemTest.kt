package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [WishlistsFeatureItem]'s wire shape and its [asWishlistsFeatureItem] mapper. */
class WishlistsFeatureItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 1u,
        approximatePrice = null,
        priceUnits = "",
        links = listOf(WishlistItemLink("https://example.com", "Store")),
        description = "Notes",
        priority = Priority.Medium,
        imageIds = listOf(FileId("a"), FileId("b"))
    )

    /** Encoded JSON carries exactly the item's nine declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val json = Json.encodeToJsonElement(WishlistsFeatureItem.serializer(), fixture.asWishlistsFeatureItem()).jsonObject

        assertEquals(
            setOf(
                "id", "wishlistId", "title", "amount", "approximatePrice",
                "priceUnits", "links", "description", "priority", "imageIds"
            ),
            json.keys
        )
    }

    /** Every display field, including a multi-element [WishlistsFeatureItem.imageIds] list, is projected unchanged. */
    @Test
    fun mapperProjectsEveryFieldUnchangedIncludingMultipleImageIds() {
        val projected = fixture.asWishlistsFeatureItem()

        assertEquals(listOf(FileId("a"), FileId("b")), projected.imageIds)
        assertEquals(listOf(WishlistItemLink("https://example.com", "Store")), projected.links)
        assertEquals(null, projected.approximatePrice)
    }
}
