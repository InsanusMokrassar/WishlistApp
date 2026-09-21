package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.server.utils.validatedCooldownMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

/** Verifies durable cooldown conversion independently from SMTP graph selection. */
class EmailChangePolicyConfigTest {
    /** Omission and explicit zero disable future deadline issuance. */
    @Test
    fun omittedAndZeroPolicyDisableCooldown() {
        assertEquals(0L, EmailChangePolicyConfig().validatedCooldownMillis(nowMillis = 1L))
        assertEquals(0L, EmailChangePolicyConfig(Duration.ZERO).validatedCooldownMillis(nowMillis = 1L))
    }

    /** Positive fractional milliseconds round up rather than becoming an ineffective zero delay. */
    @Test
    fun positiveFractionalMillisecondsRoundUp() {
        assertEquals(1L, EmailChangePolicyConfig(1.nanoseconds).validatedCooldownMillis(nowMillis = 1L))
        assertEquals(2L, EmailChangePolicyConfig(1_500_000.nanoseconds).validatedCooldownMillis(nowMillis = 1L))
    }

    /** Negative, infinite, and deadline-overflow policies are refused before server startup completes. */
    @Test
    fun invalidOrUnrepresentablePoliciesFailValidation() {
        assertFailsWith<IllegalArgumentException> {
            EmailChangePolicyConfig((-1).milliseconds).validatedCooldownMillis(nowMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException> {
            EmailChangePolicyConfig(Duration.INFINITE).validatedCooldownMillis(nowMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException> {
            EmailChangePolicyConfig(1.milliseconds).validatedCooldownMillis(nowMillis = Long.MAX_VALUE)
        }
    }
}
