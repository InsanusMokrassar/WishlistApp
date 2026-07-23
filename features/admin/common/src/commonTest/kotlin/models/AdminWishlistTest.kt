package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AdminWishlist]'s wire shape and BOTH of its mapper overloads — see `003-architecturing.md`
 * §1.1 for why [AdminWishlist] needs a second overload sourced from [WishlistsFeatureWishlist] rather
 * than only [RegisteredWishlist].
 */
class AdminWishlistTest {

    /** Encoded JSON carries exactly the wishlist's four declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val wishlist = AdminWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        val json = Json.encodeToJsonElement(AdminWishlist.serializer(), wishlist).jsonObject

        assertEquals(setOf("id", "userId", "title", "defaultPriceUnits"), json.keys)
    }

    /** The [RegisteredWishlist]-sourced overload projects every field unchanged. */
    @Test
    fun registeredWishlistMapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(AdminWishlist(WishlistId(1L), UserId(2L), "Birthday", "$"), registered.asAdminWishlist())
    }

    /** The [WishlistsFeatureWishlist]-sourced overload (used by the three admin routes that go through
     * `WishlistService`) projects every field unchanged. */
    @Test
    fun wishlistsFeatureWishlistMapperProjectsEveryFieldUnchanged() {
        val fromFeature = WishlistsFeatureWishlist(WishlistId(3L), UserId(4L), "Holiday", "EUR")

        assertEquals(AdminWishlist(WishlistId(3L), UserId(4L), "Holiday", "EUR"), fromFeature.asAdminWishlist())
    }

    /** Round trip base → feature → base restores the original unchanged — no extra arguments required. */
    @Test
    fun reverseMapperRoundTripsToOriginalRegisteredWishlist() {
        val original = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(original, original.asAdminWishlist().asRegisteredWishlist())
    }
}
