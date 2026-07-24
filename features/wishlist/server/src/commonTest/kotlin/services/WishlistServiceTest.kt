package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlist
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import dev.inmo.wishlist.features.wishlist.common.models.asWishlistsFeatureWishlist
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** In-memory [WishlistRepo] test double, seeded via the constructor map. */
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

/**
 * Verifies each read/create method of [WishlistService] returns [WishlistsFeatureWishlist] instead
 * of the persistence entity [RegisteredWishlist].
 */
class WishlistServiceTest {

    private val ownerA = UserId(1L)
    private val ownerB = UserId(2L)
    private val wishlistA1 = RegisteredWishlist(WishlistId(10L), ownerA, "Birthday", "$")
    private val wishlistA2 = RegisteredWishlist(WishlistId(11L), ownerA, "Holiday", "$")
    private val wishlistB1 = RegisteredWishlist(WishlistId(12L), ownerB, "Wedding", "EUR")

    private fun seededRepo() = FakeWishlistRepo(
        mapOf(wishlistA1.id to wishlistA1, wishlistA2.id to wishlistA2, wishlistB1.id to wishlistB1)
    )

    /** [WishlistService.getById] projects a found wishlist onto [WishlistsFeatureWishlist]. */
    @Test
    fun getByIdReturnsFeatureWishlistForKnownId() = runTest {
        val service = WishlistService(seededRepo())

        assertEquals(wishlistA1.asWishlistsFeatureWishlist(), service.getById(wishlistA1.id))
    }

    /** [WishlistService.getById] returns `null` for an id that does not exist. */
    @Test
    fun getByIdReturnsNullForUnknownId() = runTest {
        val service = WishlistService(seededRepo())

        assertNull(service.getById(WishlistId(999L)))
    }

    /** [WishlistService.getByUserId] projects every wishlist owned by the given user. */
    @Test
    fun getByUserIdReturnsFeatureWishlistsForOwner() = runTest {
        val service = WishlistService(seededRepo())

        val result = service.getByUserId(ownerA)

        assertEquals(
            setOf(wishlistA1.asWishlistsFeatureWishlist(), wishlistA2.asWishlistsFeatureWishlist()),
            result.toSet()
        )
    }

    /** [WishlistService.getMyWishlists] delegates to the same owner filter as [WishlistService.getByUserId]. */
    @Test
    fun getMyWishlistsReturnsFeatureWishlistsForCaller() = runTest {
        val service = WishlistService(seededRepo())

        assertEquals(listOf(wishlistB1.asWishlistsFeatureWishlist()), service.getMyWishlists(ownerB))
    }

    /** [WishlistService.create] persists and returns the new wishlist as a [WishlistsFeatureWishlist]. */
    @Test
    fun createReturnsPersistedWishlistAsFeatureWishlist() = runTest {
        val repo = seededRepo()
        val service = WishlistService(repo)

        val created = service.create(NewWishlistInFeature("New list", "GBP"), ownerA)

        checkNotNull(created)
        assertEquals("New list", created.title)
        assertEquals("GBP", created.defaultPriceUnits)
        assertEquals(ownerA, created.userId)
    }
}
