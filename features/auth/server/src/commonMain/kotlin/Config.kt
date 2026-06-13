package dev.inmo.wishlist.features.auth.server

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Serializable
data class Config(
    val tokenTtl: Duration = 15.minutes,
    val refreshTokenTtl: Duration = 7.days,
    val enableRegistration: Boolean = false,
)
