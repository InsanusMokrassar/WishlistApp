package dev.inmo.wishlist.features.users.common.repo

/** Delegates ordinary repository assertions to Kotlin test. */
internal fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    kotlin.test.assertEquals(expected, actual, message)
}
