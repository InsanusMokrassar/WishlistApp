package dev.inmo.wishlist.features.auth.common.utils

import dev.inmo.wishlist.features.auth.common.models.Password

/** Minimum character count accepted by the email-authorized password-change flow. */
const val passwordChangeMinimumLength: Int = 8

/** Maximum UTF-8 byte count BCrypt can process without truncation. */
const val passwordChangeMaximumUtf8Bytes: Int = 72

/**
 * Checks password-change input without trimming, normalization, or character rewriting.
 *
 * @param password Candidate plaintext password.
 * @return `true` when the text has at least eight characters and no more than 72 UTF-8 bytes.
 */
fun isAcceptablePasswordChangePassword(password: Password): Boolean =
    password.string.length >= passwordChangeMinimumLength &&
        password.string.encodeToByteArray().size <= passwordChangeMaximumUtf8Bytes
