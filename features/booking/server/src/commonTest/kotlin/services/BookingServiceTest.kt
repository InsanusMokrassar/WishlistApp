package dev.inmo.wishlist.features.booking.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem
import dev.inmo.wishlist.features.booking.common.models.BookingId
import dev.inmo.wishlist.features.booking.common.models.NewBooking
import dev.inmo.wishlist.features.booking.common.models.RegisteredBooking
import dev.inmo.wishlist.features.booking.common.models.asBookingFeatureItem
import dev.inmo.wishlist.features.booking.common.repo.BookingRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** In-memory [BookingRepo] test double, keyed by [BookingId], seeded via the constructor map. */
internal class FakeBookingRepo(
    initialBookings: Map<BookingId, RegisteredBooking> = emptyMap()
) : BookingRepo, MapCRUDRepo<RegisteredBooking, BookingId, NewBooking>(initialBookings.toMutableMap()) {

    private var nextId: Long = (initialBookings.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewBooking, id: BookingId, old: RegisteredBooking): RegisteredBooking =
        old.copy(itemId = newValue.itemId, userId = newValue.userId)

    override suspend fun createObject(newValue: NewBooking): Pair<BookingId, RegisteredBooking> {
        val id = BookingId(nextId++)
        return id to RegisteredBooking(id, newValue.itemId, newValue.userId)
    }

    override suspend fun getByItemId(itemId: WishlistItemId): RegisteredBooking? =
        getAll().values.firstOrNull { it.itemId == itemId }

    override suspend fun getByUserId(userId: UserId): List<RegisteredBooking> =
        getAll().values.filter { it.userId == userId }
}

/** In-memory [WishlistItemRepo] test double, seeded via the constructor map. */
internal class FakeWishlistItemRepo(
    initialItems: Map<WishlistItemId, RegisteredWishlistItem> = emptyMap()
) : WishlistItemRepo, MapCRUDRepo<RegisteredWishlistItem, WishlistItemId, NewWishlistItem>(initialItems.toMutableMap()) {

    private var nextId: Long = (initialItems.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewWishlistItem, id: WishlistItemId, old: RegisteredWishlistItem): RegisteredWishlistItem =
        old.copy(
            wishlistId = newValue.wishlistId,
            title = newValue.title,
            amount = newValue.amount,
            approximatePrice = newValue.approximatePrice,
            priceUnits = newValue.priceUnits,
            links = newValue.links,
            description = newValue.description,
            priority = newValue.priority,
            imageIds = newValue.imageIds
        )

    override suspend fun createObject(newValue: NewWishlistItem): Pair<WishlistItemId, RegisteredWishlistItem> {
        val id = WishlistItemId(nextId++)
        return id to RegisteredWishlistItem(
            id = id,
            wishlistId = newValue.wishlistId,
            title = newValue.title,
            amount = newValue.amount,
            approximatePrice = newValue.approximatePrice,
            priceUnits = newValue.priceUnits,
            links = newValue.links,
            description = newValue.description,
            priority = newValue.priority,
            imageIds = newValue.imageIds
        )
    }

    override suspend fun getByWishlistId(wishlistId: WishlistId): List<RegisteredWishlistItem> =
        getAll().values.filter { it.wishlistId == wishlistId }

    override suspend fun getByIds(ids: List<WishlistItemId>): List<RegisteredWishlistItem> {
        val all = getAll()
        return ids.distinct().mapNotNull { all[it] }
    }
}

/** In-memory [WishlistRepo] test double, seeded via the constructor map. Empty by default — [BookingService.myPresentsBooks] never reads it. */
internal class FakeWishlistRepo(
    initialWishlists: Map<WishlistId, RegisteredWishlist> = emptyMap()
) : WishlistRepo, MapCRUDRepo<RegisteredWishlist, WishlistId, NewWishlist>(initialWishlists.toMutableMap()) {

    private var nextId: Long = (initialWishlists.keys.maxOfOrNull { it.long } ?: 0L) + 1L

    override suspend fun updateObject(newValue: NewWishlist, id: WishlistId, old: RegisteredWishlist): RegisteredWishlist =
        old.copy(userId = newValue.userId, title = newValue.title, defaultPriceUnits = newValue.defaultPriceUnits)

    override suspend fun createObject(newValue: NewWishlist): Pair<WishlistId, RegisteredWishlist> {
        val id = WishlistId(nextId++)
        return id to RegisteredWishlist(id, newValue.userId, newValue.title, newValue.defaultPriceUnits)
    }

    override suspend fun getByUserId(userId: UserId): List<RegisteredWishlist> =
        getAll().values.filter { it.userId == userId }
}

/** Verifies [BookingService.myPresentsBooks] projects the caller's booked items onto [BookingFeatureItem]. */
class BookingServiceTest {

    private val callerId = UserId(1L)
    private val item = RegisteredWishlistItem(
        id = WishlistItemId(10L),
        wishlistId = WishlistId(20L),
        title = "Bicycle"
    )

    /** The caller's own booking maps to a [BookingFeatureItem] mirroring the booked item's fields. */
    @Test
    fun myPresentsBooksReturnsCallersBookedItemsAsFeatureItems() = runTest {
        val bookingRepo = FakeBookingRepo(mapOf(BookingId(1L) to RegisteredBooking(BookingId(1L), item.id, callerId)))
        val wishlistItemRepo = FakeWishlistItemRepo(mapOf(item.id to item))
        val service = BookingService(bookingRepo, wishlistItemRepo, FakeWishlistRepo())

        val result = service.myPresentsBooks(callerId)

        assertEquals(listOf(item.asBookingFeatureItem()), result)
    }

    /** A caller with no bookings gets an empty list. */
    @Test
    fun myPresentsBooksReturnsEmptyListWhenNoBookings() = runTest {
        val service = BookingService(FakeBookingRepo(), FakeWishlistItemRepo(), FakeWishlistRepo())

        assertTrue(service.myPresentsBooks(callerId).isEmpty())
    }
}
