package dev.inmo.wishlist.features.booking.common.models

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

/** Verifies [BookingFeatureItem]'s wire shape and its [asBookingFeatureItem] mapper. */
class BookingFeatureItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 1u,
        approximatePrice = Amount(199.99),
        priceUnits = "$",
        links = listOf(WishlistItemLink("https://example.com/bike")),
        description = "Road bike, size M",
        priority = Priority.High,
        imageIds = listOf(FileId("file-1"))
    )

    /** Encoded JSON carries exactly the item's nine declared fields — no stray persistence-only field. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val item = fixture.asBookingFeatureItem()

        val json = Json.encodeToJsonElement(BookingFeatureItem.serializer(), item).jsonObject

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
        val projected = fixture.asBookingFeatureItem()

        assertEquals(
            BookingFeatureItem(
                id = WishlistItemId(1L),
                wishlistId = WishlistId(2L),
                title = "Bicycle",
                amount = 1u,
                approximatePrice = Amount(199.99),
                priceUnits = "$",
                links = listOf(WishlistItemLink("https://example.com/bike")),
                description = "Road bike, size M",
                priority = Priority.High,
                imageIds = listOf(FileId("file-1"))
            ),
            projected
        )
    }

    /** Optional [BookingFeatureItem.approximatePrice] maps through as `null` when the source has none. */
    @Test
    fun mapperHandlesNullApproximatePrice() {
        val projected = fixture.copy(approximatePrice = null).asBookingFeatureItem()

        assertEquals(null, projected.approximatePrice)
    }

    /** Round trip base → feature → base restores the original unchanged — no extra arguments required. */
    @Test
    fun reverseMapperRoundTripsToOriginalRegisteredWishlistItem() {
        assertEquals(fixture, fixture.asBookingFeatureItem().asRegisteredWishlistItem())
    }
}
