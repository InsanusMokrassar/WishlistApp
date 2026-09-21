package dev.inmo.wishlist.features.email.common.utils

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.common.models.EmailProfile

/**
 * Returns the exact address that currently needs verification, preferring a replacement candidate
 * over an unapproved first address.
 *
 * @return Pending replacement, unapproved current address, or `null` when no verification target
 *   exists.
 */
fun EmailProfile.verificationCandidate(): Email? = pendingEmail ?: email?.takeUnless { emailApproved }

/**
 * Returns the persisted address that should initialize an owner-editing draft.
 *
 * @return Pending replacement when present, otherwise the current address.
 */
fun EmailProfile.emailDraftBaseline(): Email? = pendingEmail ?: email
