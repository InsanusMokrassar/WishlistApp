package dev.inmo.wishlist.features.email.server.utils

import dev.inmo.wishlist.features.email.server.EmailChangePolicyConfig
import kotlin.time.Duration.Companion.milliseconds

/**
 * Validates a configured cooldown and converts it to the durable epoch-millisecond representation.
 *
 * Positive fractional milliseconds round up so a positive policy cannot silently become an
 * immediately-expired restriction. The returned value is checked against [nowMillis] because every
 * approved address must be able to receive a representable deadline.
 *
 * @param nowMillis Server epoch milliseconds used for startup representability validation.
 * @return Zero for a disabled policy, otherwise the checked positive cooldown in milliseconds.
 * @throws IllegalArgumentException When the configured duration cannot issue a durable deadline.
 */
internal fun EmailChangePolicyConfig.validatedCooldownMillis(
    nowMillis: Long = System.currentTimeMillis(),
): Long {
    val duration = emailChangeCooldown
    require(duration.isFinite() && !duration.isNegative()) {
        "emailChangeCooldown must be a finite, non-negative duration"
    }
    if (duration == kotlin.time.Duration.ZERO) return 0

    require(duration <= Long.MAX_VALUE.milliseconds) {
        "emailChangeCooldown cannot be represented as milliseconds"
    }
    val truncatedMillis = duration.inWholeMilliseconds
    val cooldownMillis = if (duration == truncatedMillis.milliseconds) {
        truncatedMillis
    } else {
        if (truncatedMillis == Long.MAX_VALUE) {
            throw IllegalArgumentException("emailChangeCooldown cannot be represented as milliseconds")
        }
        truncatedMillis + 1
    }
    if (nowMillis > Long.MAX_VALUE - cooldownMillis) {
        throw IllegalArgumentException("emailChangeCooldown cannot issue a representable deadline")
    }
    return cooldownMillis
}
