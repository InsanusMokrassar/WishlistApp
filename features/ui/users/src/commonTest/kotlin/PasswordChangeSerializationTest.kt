package dev.inmo.wishlist.features.ui.users

import dev.inmo.wishlist.features.common.client.models.ViewConfig
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.ui.users.ui.PasswordChangeViewConfig
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Proves the production Json aggregation accepts concrete password-change navigation variants. */
class PasswordChangeSerializationTest {
    /** Resolves the actual contributed serializers before round-tripping both polymorphic boundaries. */
    /** Verifies production JSON registers both concrete password route configurations. */
    @Test
    fun productionJsonRoundTripsConcretePasswordChangeConfigs() {
        val application = startKoin {
            modules(module {
                with(dev.inmo.wishlist.features.common.common.Plugin) { setupDI(JsonObject(emptyMap())) }
                with(Plugin) { setupDI(JsonObject(emptyMap())) }
            })
        }
        try {
            val json = application.koin.get<Json>()
            val pending = PasswordChangeViewConfig.Pending(
                UserId(7L), DeepLinkId("123e4567-e89b-42d3-a456-426614174000"),
            )
            listOf<PasswordChangeViewConfig>(pending, PasswordChangeViewConfig.Completed).forEach { config ->
                val viewConfigEncoded = json.encodeToString(PolymorphicSerializer(ViewConfig::class), config)
                assertEquals(config, json.decodeFromString(PolymorphicSerializer(ViewConfig::class), viewConfigEncoded))
                val anyEncoded = json.encodeToString(PolymorphicSerializer(Any::class), config)
                assertEquals(config, json.decodeFromString(PolymorphicSerializer(Any::class), anyEncoded))
            }
            val completed = json.encodeToString(PolymorphicSerializer(ViewConfig::class), PasswordChangeViewConfig.Completed)
            assertFalse(completed.contains(pending.approvalId.string))
            assertFalse(completed.contains("new-password"))
        } finally {
            stopKoin()
        }
    }
}
