package dev.inmo.wishlist.features.email.server

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

/** Root email-replacement policy, intentionally independent from optional SMTP configuration. */
@Serializable
data class EmailChangePolicyConfig(
    /** Duration issued after a new approval; zero disables newly issued cooldowns. */
    val emailChangeCooldown: Duration = ZERO,
)
