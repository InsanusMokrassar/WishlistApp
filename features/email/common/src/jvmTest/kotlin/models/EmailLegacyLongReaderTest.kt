package dev.inmo.wishlist.features.email.common.models

import korlibs.time.DateTime
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertFails

/** Shows why a strict prior Long reader needs a coordinated wire rollout. */
class EmailLegacyLongReaderTest {
    /** JVM Double output for a cooldown deadline is not accepted by the old strict Long reader. */
    @Test
    fun doubleNumericOutputRejectsStrictLongReader() {
        val encoded = Json.encodeToString(
            EmailChangeCooldown.serializer(),
            EmailChangeCooldown(DateTime.fromUnixMillis(123456789L)),
        )
        val number = Json.parseToJsonElement(encoded).jsonObject.getValue("emailChangeAllowedAt")
        assertFails { Json.decodeFromJsonElement(Long.serializer(), number) }
    }
}
