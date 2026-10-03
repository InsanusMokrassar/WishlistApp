package dev.inmo.wishlist.features.currency.common.models

import korlibs.time.DateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Checks the historical rates wire key and numeric DateTime compatibility. */
class CurrencyRatesSerializationTest {
    /** Legacy integer input and new numeric output retain the same retrieval instant. */
    @Test
    fun numericInstantsRoundTrip() {
        listOf(0L, 1_700_000_000_000L, -1L).forEach { millis ->
            val legacy = """{"base":"USD","rates":{"EUR":0.9},"fetchedAtMillis":$millis}"""
            val decoded = Json.decodeFromString(CurrencyRates.serializer(), legacy)
            assertEquals(DateTime.fromUnixMillis(millis), decoded.fetchedAtMillis)
            assertEquals(CurrencyCode.of("USD"), decoded.base)
            assertEquals(0.9, decoded.rateOf(CurrencyCode.of("EUR")))
            assertNull(decoded.rateOf(CurrencyCode.of("JPY")))
            val encoded = Json.encodeToString(CurrencyRates.serializer(), decoded)
            val numeric = Json.parseToJsonElement(encoded).jsonObject.getValue("fetchedAtMillis").jsonPrimitive
            assertEquals(millis.toDouble(), numeric.content.toDouble())
            assertFalse(numeric.isString)
            assertEquals(decoded, Json.decodeFromString(CurrencyRates.serializer(), encoded))
        }
    }
}
