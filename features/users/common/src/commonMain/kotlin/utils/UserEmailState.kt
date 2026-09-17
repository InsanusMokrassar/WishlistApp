package dev.inmo.wishlist.features.users.common.utils

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser

/** Returns the address that may receive a verification message for [user], if any. */
fun RegisteredUser.verificationCandidate(): Email? = pendingEmail ?: email?.takeUnless { emailApproved }

/** Returns the persisted address from which an owner editor starts its draft. */
fun RegisteredUser.emailDraftBaseline(): Email? = pendingEmail ?: email
