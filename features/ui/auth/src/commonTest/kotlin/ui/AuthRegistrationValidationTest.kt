package dev.inmo.wishlist.features.ui.auth.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies the registration email policy used by all platform views. */
class AuthRegistrationValidationTest {
    /** Login-style optional registration accepts an empty email field. */
    @Test
    fun optionalEmailAllowsBlankValue() {
        assertTrue(isRegistrationEmailValid("", required = false))
    }

    /** Required registration rejects an empty email field. */
    @Test
    fun requiredEmailRejectsBlankValue() {
        assertFalse(isRegistrationEmailValid("", required = true))
    }

    /** Optional registration still rejects a malformed nonblank value. */
    @Test
    fun optionalEmailRejectsMalformedNonBlankValue() {
        assertFalse(isRegistrationEmailValid("not-an-email", required = false))
    }

    /** Both policies accept a syntactically valid address. */
    @Test
    fun validEmailIsAccepted() {
        assertTrue(isRegistrationEmailValid("alice@example.com", required = true))
        assertTrue(isRegistrationEmailValid("alice@example.com", required = false))
    }
}
