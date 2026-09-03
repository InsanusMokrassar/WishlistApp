package dev.inmo.wishlist.features.common.client.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Verifies that toast state republishes equal visible text and retains its null empty sentinel. */
class ToasterTest {
    /** Equal text receives a fresh provider so a host can restart its dismissal effect. */
    @Test
    fun repeatedSameTextPublishesFreshProvider() {
        Toaster.clear()

        try {
            Toaster.show("Approved")
            val firstProvider = requireNotNull(Toaster.message.value)

            Toaster.show("Approved")
            val secondProvider = requireNotNull(Toaster.message.value)

            assertFalse(firstProvider === secondProvider)
        } finally {
            Toaster.clear()
        }

        assertNull(Toaster.message.value)
    }
}
