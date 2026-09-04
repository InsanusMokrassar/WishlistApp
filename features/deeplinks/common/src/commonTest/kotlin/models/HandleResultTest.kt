package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies serialization preserves every deeplink handling outcome. */
class HandleResultTest {
    /** Every result variant round-trips with its exact payload. */
    @Test
    fun resultVariantsRoundTrip() {
        listOf<HandleResult>(
            HandleResult.NotFound,
            HandleResult.Unhandled,
            HandleResult.Handled.Common,
            HandleResult.Handled.Redirect("/target?x=1"),
        ).forEach { result ->
            assertEquals(result, Json.decodeFromString<HandleResult>(Json.encodeToString(result)))
        }
    }
}
