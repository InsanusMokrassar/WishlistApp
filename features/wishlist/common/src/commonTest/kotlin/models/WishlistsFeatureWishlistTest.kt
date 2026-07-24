package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [WishlistsFeatureWishlist]'s wire shape and its [asWishlistsFeatureWishlist] mapper. */
class WishlistsFeatureWishlistTest {

    /** Encoded JSON carries exactly the wishlist's four declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val wishlist = WishlistsFeatureWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        val json = Json.encodeToJsonElement(WishlistsFeatureWishlist.serializer(), wishlist).jsonObject

        assertEquals(setOf("id", "userId", "title", "defaultPriceUnits"), json.keys)
    }

    /** Every field is projected unchanged from the source [RegisteredWishlist]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(
            WishlistsFeatureWishlist(WishlistId(1L), UserId(2L), "Birthday", "$"),
            registered.asWishlistsFeatureWishlist()
        )
    }

    /** Round trip base → feature → base restores the original unchanged — no extra arguments required. */
    @Test
    fun reverseMapperRoundTripsToOriginalRegisteredWishlist() {
        val original = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(original, original.asWishlistsFeatureWishlist().asRegisteredWishlist())
    }
}
