package dev.inmo.wishlist.features.common.server.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies backward-compatible and explicit public HTTP origin configuration. */
class ConfigTest {
    /** Omission derives the compatibility development origin from public host and bind port. */
    @Test
    fun omittedPublicHttpOriginUsesCompatibilityDefault() {
        val config = Json.decodeFromString<Config>(
            """{"publicHost":"127.0.0.1","port":8196}"""
        )

        assertEquals("http://127.0.0.1:8196", config.publicHttpOrigin)
    }

    /** An explicit reverse-proxy origin survives decoding without a bind-port rewrite. */
    @Test
    fun explicitPublicHttpOriginIsPreserved() {
        val config = Json.decodeFromString<Config>(
            """{"publicHost":"internal","port":8196,"publicHttpOrigin":"https://wishlist.example"}"""
        )

        assertEquals("https://wishlist.example", config.publicHttpOrigin)
    }
}
