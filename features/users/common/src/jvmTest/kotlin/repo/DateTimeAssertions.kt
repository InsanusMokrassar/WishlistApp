package dev.inmo.wishlist.features.users.common.repo

import korlibs.time.DateTime

/** Keeps lifecycle assertions readable while application timestamps use DateTime. */
internal fun assertEquals(expected: Long, actual: DateTime?, message: String? = null) {
    kotlin.test.assertEquals(expected, actual?.unixMillis?.toLong(), message)
}

/** Delegates ordinary repository assertions to Kotlin test. */
internal fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    kotlin.test.assertEquals(expected, actual, message)
}
