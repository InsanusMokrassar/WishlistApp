package dev.inmo.wishlist.features.auth.server.utils

import dev.inmo.wishlist.features.users.common.models.UserId
import java.security.MessageDigest

/** Fixed domain separation text for the password-change credential-state digest. */
private const val passwordChangeCredentialStateDomain = "wishlist.password-change.v1"

/** Exact lower-case SHA-256 hexadecimal representation accepted from persisted approval payloads. */
private val passwordChangeCredentialStatePattern = Regex("^[0-9a-f]{64}$")

/**
 * Produces a server-private credential-state fingerprint tied to one account and BCrypt record.
 *
 * @param userId Credential owner.
 * @param storedHash Full stored BCrypt representation.
 * @return Lower-case SHA-256 hexadecimal digest; never the BCrypt value itself.
 */
internal fun passwordChangeCredentialState(userId: UserId, storedHash: String): String {
    val bytes = buildString {
        append(passwordChangeCredentialStateDomain)
        append('\u0000')
        append(userId.long)
        append('\u0000')
        append(storedHash)
    }.encodeToByteArray()
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

/**
 * Compares a persisted credential-state string without accepting malformed digest encodings.
 *
 * @param expected Stored password-approval fingerprint.
 * @param actual Freshly calculated fingerprint.
 * @return `true` only when both values are valid equal SHA-256 byte sequences.
 */
internal fun matchesPasswordChangeCredentialState(expected: String, actual: String): Boolean {
    if (!passwordChangeCredentialStatePattern.matches(expected)) return false
    if (!passwordChangeCredentialStatePattern.matches(actual)) return false
    val expectedBytes = expected.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val actualBytes = actual.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return MessageDigest.isEqual(expectedBytes, actualBytes)
}
