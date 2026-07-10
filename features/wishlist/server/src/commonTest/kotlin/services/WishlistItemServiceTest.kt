package dev.inmo.wishlist.features.wishlist.server.services

import dev.inmo.micro_utils.repos.MapCRUDRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.CopyItemRequest
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.asWishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.repo.WishlistItemRepo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * In-memory [WishlistItemRepo] test double, seeded via the constructor map.
 *
 * [FakeWishlistRepo] (sibling fake, same package/module — see `WishlistServiceTest.kt`) supplies the
 * parent-wishlist double these tests also need.
 */
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

/**
 * Verifies [WishlistItemService.getByWishlistId]/[WishlistItemService.create]/[WishlistItemService.copyItem]
 * return [WishlistsFeatureItem] instead of the persistence entity [RegisteredWishlistItem].
 */
class WishlistItemServiceTest {

    private val ownerId = UserId(1L)
    private val otherOwnerId = UserId(2L)
    private val wishlist = RegisteredWishlist(WishlistId(10L), ownerId, "Birthday", "$")
    private val sourceWishlist = RegisteredWishlist(WishlistId(11L), otherOwnerId, "Wedding", "$")
    private val item = RegisteredWishlistItem(WishlistItemId(100L), wishlist.id, "Bicycle")
    private val sourceItem = RegisteredWishlistItem(WishlistItemId(101L), sourceWishlist.id, "Kettle")

    private fun buildService(
        items: Map<WishlistItemId, RegisteredWishlistItem> = emptyMap(),
        wishlists: Map<WishlistId, RegisteredWishlist> = mapOf(wishlist.id to wishlist, sourceWishlist.id to sourceWishlist)
    ) = WishlistItemService(FakeWishlistItemRepo(items), FakeWishlistRepo(wishlists))

    /** [WishlistItemService.getByWishlistId] projects every stored item onto [WishlistsFeatureItem]. */
    @Test
    fun getByWishlistIdReturnsFeatureItems() = runTest {
        val service = buildService(items = mapOf(item.id to item))

        assertEquals(listOf(item.asWishlistsFeatureItem()), service.getByWishlistId(wishlist.id))
    }

    /** [WishlistItemService.create] persists and returns the new item as a [WishlistsFeatureItem] when the caller owns the parent wishlist. */
    @Test
    fun createReturnsPersistedItemAsFeatureItemForOwner() = runTest {
        val service = buildService()

        val created = service.create(NewWishlistItem(wishlist.id, "New item"), ownerId)

        checkNotNull(created)
        assertEquals("New item", created.title)
        assertEquals(wishlist.id, created.wishlistId)
    }

    /** [WishlistItemService.create] returns `null` when the caller does not own the parent wishlist. */
    @Test
    fun createReturnsNullWhenCallerDoesNotOwnParentWishlist() = runTest {
        val service = buildService()

        assertNull(service.create(NewWishlistItem(wishlist.id, "New item"), otherOwnerId))
    }

    /** [WishlistItemService.copyItem] deep-copies the source item into the caller-owned target and returns a [WishlistsFeatureItem]. */
    @Test
    fun copyItemReturnsCreatedItemAsFeatureItem() = runTest {
        val service = buildService(items = mapOf(sourceItem.id to sourceItem))

        val copied = service.copyItem(CopyItemRequest(sourceItem.id, sourceWishlist.id, wishlist.id), ownerId)

        checkNotNull(copied)
        assertEquals(sourceItem.title, copied.title)
        assertEquals(wishlist.id, copied.wishlistId)
    }

    /** [WishlistItemService.copyItem]'s idempotent branch (an identical item already exists in the target) also returns the mapped [WishlistsFeatureItem], not the raw repo type. */
    @Test
    fun copyItemReturnsExistingItemAsFeatureItemWhenAlreadyCopied() = runTest {
        val existingCopy = RegisteredWishlistItem(WishlistItemId(200L), wishlist.id, sourceItem.title)
        val service = buildService(items = mapOf(sourceItem.id to sourceItem, existingCopy.id to existingCopy))

        val result = service.copyItem(CopyItemRequest(sourceItem.id, sourceWishlist.id, wishlist.id), ownerId)

        assertEquals(existingCopy.asWishlistsFeatureItem(), result)
    }
}
