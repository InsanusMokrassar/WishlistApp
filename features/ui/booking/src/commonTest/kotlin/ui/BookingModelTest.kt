package dev.inmo.wishlist.features.ui.booking.ui

import dev.inmo.wishlist.features.booking.client.BookingFeature
import dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem
import dev.inmo.wishlist.features.booking.common.models.BookingState
import dev.inmo.wishlist.features.ui.booking.Plugin
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies all booking delegations and the production singleton binding. */
class BookingModelTest {
    @Test
    fun delegatesEveryOperationWithExactArgumentsAndResults() = runTest {
        val feature = RecordingBookingFeature()
        val model = DefaultBookingModel(bookingFeature = feature)
        val stateId = WishlistItemId(1L)
        val bookId = WishlistItemId(2L)
        val cancelId = WishlistItemId(3L)

        assertEquals(BookingState.BookedByMe, model.getBookingState(stateId))
        assertTrue(model.bookItem(bookId))
        assertTrue(model.cancelBooking(cancelId))
        assertSame(feature.presents, model.myPresentsBooks())
        assertEquals(listOf(stateId), feature.stateCalls)
        assertEquals(listOf(bookId), feature.bookCalls)
        assertEquals(listOf(cancelId), feature.cancelCalls)
        assertEquals(1, feature.presentsCalls)
    }

    @Test
    fun pluginBindsDefaultModelAsOneInterfaceSingleton() {
        val koin = startKoin {
            modules(
                module {
                    with(Plugin) { setupDI(JsonObject(emptyMap())) }
                    single<BookingFeature> { RecordingBookingFeature() }
                }
            )
        }
        try {
            val first = koin.koin.get<BookingModel>()
            val second = koin.koin.get<BookingModel>()

            assertIs<DefaultBookingModel>(first)
            assertSame(first, second)
        } finally {
            stopKoin()
        }
    }

    private class RecordingBookingFeature : BookingFeature {
        val stateCalls = mutableListOf<WishlistItemId>()
        val bookCalls = mutableListOf<WishlistItemId>()
        val cancelCalls = mutableListOf<WishlistItemId>()
        val presents = emptyList<BookingFeatureItem>()
        var presentsCalls = 0

        override suspend fun getState(itemId: WishlistItemId): BookingState {
            stateCalls += itemId
            return BookingState.BookedByMe
        }

        override suspend fun tryBook(itemId: WishlistItemId): Boolean {
            bookCalls += itemId
            return true
        }

        override suspend fun cancelBooking(itemId: WishlistItemId): Boolean {
            cancelCalls += itemId
            return true
        }

        override suspend fun myPresentsBooks(): List<BookingFeatureItem> {
            presentsCalls += 1
            return presents
        }
    }
}
