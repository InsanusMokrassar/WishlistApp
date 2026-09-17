package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.auth.client.AuthCredentialsStorage
import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import dev.inmo.wishlist.features.currency.client.CurrencyService
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import dev.inmo.wishlist.features.files.client.FilesClientService
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.client.UsersFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.client.WishlistCopyFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsFeature
import dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature
import dev.inmo.wishlist.features.wishlist.common.models.CopyItemRequest
import dev.inmo.wishlist.features.wishlist.common.models.CopyWishlistRequest
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistInFeature
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Default outside-world implementation shared by all wishlist UI screens.
 *
 * @param wishlistsFeature Wishlist CRUD capability.
 * @param itemsFeature Wishlist-item CRUD and copy capability.
 * @param copyFeature Background whole-wishlist copy capability.
 * @param meState Reactive private current-user record.
 * @param filesService Item-image storage and download service.
 * @param usersFeature Public users capability used for display-name lookup.
 * @param currencyService Currency configuration and conversion service.
 * @param viewModeStorage Persistent wishlist presentation preference.
 * @param scope Lifetime used for the derived current-user flow.
 * @param credentialsStorage Persistent authentication state exposed to editor ViewModels.
 */
class DefaultWishlistsModel(
    private val wishlistsFeature: WishlistsFeature,
    private val itemsFeature: WishlistsItemsFeature,
    private val copyFeature: WishlistCopyFeature,
    private val meState: StateFlow<AuthFeatureUser?>,
    private val filesService: FilesClientService,
    private val usersFeature: UsersFeature,
    private val currencyService: CurrencyService,
    private val viewModeStorage: WishlistViewModeStorage,
    private val scope: CoroutineScope,
    private val credentialsStorage: AuthCredentialsStorage,
) : WishlistsModel {
    /** Authentication state shared with wishlist editor ViewModels. */
    override val userAuthorisedState = credentialsStorage.userAuthorised

    /** @return The persisted view mode, defaulting to [WishlistViewMode.Grid]. */
    override suspend fun getSavedViewMode(): WishlistViewMode =
        viewModeStorage.getViewMode() ?: WishlistViewMode.Grid

    /** Persists the selected wishlist [mode]. */
    override suspend fun saveViewMode(mode: WishlistViewMode) =
        viewModeStorage.saveViewMode(mode)

    /** Exact selected-currency flow exposed by the currency service. */
    override val selectedCurrency: StateFlow<CurrencyCode?> = currencyService.selectedCurrency

    /** @return `true` when server-side currency conversion is available. */
    override suspend fun isCurrencyEnabled(): Boolean = currencyService.isFeatureEnabled()

    /** @return Currencies available for selection. */
    override suspend fun availableCurrencies(): List<CurrencyInfo> = currencyService.getCurrencies()

    /** @return Current exchange rates, or `null` when unavailable. */
    override suspend fun currencyRates(): CurrencyRates? = currencyService.getRates()

    /** Selects [code] as the shared conversion target. */
    override fun selectCurrency(code: CurrencyCode?) = currencyService.select(code)

    /** @return Wishlists owned by the authenticated caller. */
    override suspend fun getMyWishlists(): List<WishlistsFeatureWishlist> =
        wishlistsFeature.getMyWishlists()

    /** @return Wishlists owned by [userId]. */
    override suspend fun getUserWishlists(userId: UserId): List<WishlistsFeatureWishlist> =
        wishlistsFeature.getByUserId(userId)

    /** @return Wishlist [id], or `null` when absent. */
    override suspend fun getWishlist(id: WishlistId): WishlistsFeatureWishlist? =
        wishlistsFeature.getById(id)

    /** @return Items belonging to [wishlistId]. */
    override suspend fun getWishlistItems(wishlistId: WishlistId): List<WishlistsFeatureItem> =
        itemsFeature.getByWishlistId(wishlistId)

    /** @return The created wishlist with [title] and [defaultPriceUnits], or `null` on failure. */
    override suspend fun createWishlist(title: String, defaultPriceUnits: String): WishlistsFeatureWishlist? =
        wishlistsFeature.create(NewWishlistInFeature(title, defaultPriceUnits))

    /** @return `true` when wishlist [id] is updated with [title] and [defaultPriceUnits]. */
    override suspend fun updateWishlist(id: WishlistId, title: String, defaultPriceUnits: String): Boolean =
        wishlistsFeature.update(id, NewWishlistInFeature(title, defaultPriceUnits))

    /** @return `true` when wishlist [id] is deleted. */
    override suspend fun deleteWishlist(id: WishlistId): Boolean =
        wishlistsFeature.delete(id)

    /** @return The created wishlist [item], or `null` on failure. */
    override suspend fun createWishlistItem(item: NewWishlistItem): WishlistsFeatureItem? =
        itemsFeature.create(item)

    /** @return `true` when item [id] is replaced with [item]. */
    override suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean =
        itemsFeature.update(id, item)

    /** @return `true` when item [id] is deleted. */
    override suspend fun deleteWishlistItem(id: WishlistItemId): Boolean =
        itemsFeature.delete(id)

    /** @return Item copied from [sourceWishlistId] and [sourceItemId] into [targetWishlistId]. */
    override suspend fun copyItemToWishlist(
        sourceItemId: WishlistItemId,
        sourceWishlistId: WishlistId,
        targetWishlistId: WishlistId,
    ): WishlistsFeatureItem? =
        itemsFeature.copy(CopyItemRequest(sourceItemId, sourceWishlistId, targetWishlistId))

    /** @return `true` when copying [sourceWishlistId] is queued. */
    override suspend fun enqueueWishlistCopy(sourceWishlistId: WishlistId): Boolean =
        copyFeature.enqueueCopy(CopyWishlistRequest(sourceWishlistId))

    /** Caller id derived from the reactive private-user record. */
    override val currentUserIdFlow: StateFlow<UserId?> =
        meState.map {
            it?.id
        }.stateIn(scope, SharingStarted.Eagerly, meState.value?.id)

    /** @return Display name for [userId], or `null` when unknown. */
    override suspend fun getUserName(userId: UserId): String? =
        usersFeature.getAll().find { it.id == userId }?.username?.string

    /** @return Persistent identifier for uploaded [file], or `null` on failure. */
    override suspend fun uploadImage(file: MPPFile): FileId? =
        filesService.uploadFile(file)?.id

    /** @return Download URL for image [id]. */
    override fun imageUrl(id: FileId): String =
        filesService.apiFileUrl(id)

    /** @return Downloaded bytes for image [id], or `null` on failure. */
    override suspend fun loadImageBytes(id: FileId): ByteArray? =
        filesService.downloadBytes(id)
}
