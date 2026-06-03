package dev.inmo.wishlist.features.ui.wishlist.ui

import dev.inmo.micro_utils.common.MPPFile
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.NewWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.currency.common.models.CurrencyCode
import dev.inmo.wishlist.features.currency.common.models.CurrencyInfo
import dev.inmo.wishlist.features.currency.common.models.CurrencyRates
import kotlinx.coroutines.flow.StateFlow

/**
 * Model interface for all wishlist UI screens.
 *
 * Wraps [dev.inmo.wishlist.features.wishlist.client.WishlistsFeature] and
 * [dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature] into a
 * single surface consumed by the four wishlist ViewModels.
 */
interface WishlistsModel {
    /** Returns all wishlists owned by the authenticated caller. */
    suspend fun getMyWishlists(): List<RegisteredWishlist>

    /**
     * Returns all wishlists owned by the user identified by [userId].
     *
     * Used by the public main page when browsing another user's wishlists —
     * no auth required server-side.
     *
     * @param userId Owner to filter by.
     * @return List of wishlists; empty when none found.
     */
    suspend fun getUserWishlists(userId: UserId): List<RegisteredWishlist>

    /**
     * Returns a single wishlist by [id], resolved from the caller's wishlist list.
     *
     * @param id Identifier of the wishlist to retrieve.
     * @return Matching wishlist, or `null` when not found.
     */
    suspend fun getWishlist(id: WishlistId): RegisteredWishlist?

    /**
     * Returns all items belonging to [wishlistId].
     *
     * @param wishlistId Parent wishlist identifier.
     * @return List of items; empty when none found.
     */
    suspend fun getWishlistItems(wishlistId: WishlistId): List<RegisteredWishlistItem>

    /**
     * Creates a new wishlist with [title] for the authenticated caller.
     *
     * @param title Display name of the new wishlist.
     * @param defaultPriceUnits Default currency/units label pre-filled into new items; empty for none.
     * @return Created wishlist, or `null` on failure.
     */
    suspend fun createWishlist(title: String, defaultPriceUnits: String): RegisteredWishlist?

    /**
     * Replaces [id] wishlist title with [title] and its default price units with [defaultPriceUnits].
     *
     * @param id Wishlist to update.
     * @param title New display name.
     * @param defaultPriceUnits New default currency/units label for new items; empty for none.
     * @return `true` on success, `false` when not found or not owner.
     */
    suspend fun updateWishlist(id: WishlistId, title: String, defaultPriceUnits: String): Boolean

    /**
     * Deletes wishlist [id].
     *
     * @param id Wishlist to delete.
     * @return `true` on success, `false` when not found or not owner.
     */
    suspend fun deleteWishlist(id: WishlistId): Boolean

    /**
     * Creates a new wishlist item from [item].
     *
     * @param item Data for the new item including parent [WishlistId].
     * @return Created item, or `null` on failure or authorization error.
     */
    suspend fun createWishlistItem(item: NewWishlistItem): RegisteredWishlistItem?

    /**
     * Replaces item [id] data with [item].
     *
     * @param id Item to update.
     * @param item Replacement data.
     * @return `true` on success, `false` when not found or not owner.
     */
    suspend fun updateWishlistItem(id: WishlistItemId, item: NewWishlistItem): Boolean

    /**
     * Deletes item [id].
     *
     * @param id Item to delete.
     * @return `true` on success, `false` when not found or not owner.
     */
    suspend fun deleteWishlistItem(id: WishlistItemId): Boolean

    /**
     * Returns the [UserId] of the authenticated caller, or `null` when not available.
     *
     * Used to determine ownership of wishlists in [WishlistViewModel].
     */
    suspend fun getCurrentUserId(): UserId?

    /**
     * Resolves the display name of the user identified by [userId].
     *
     * Used to build the personalized screen titles of the wishlists list and all-items screens.
     *
     * @param userId User whose name to resolve.
     * @return Username string, or `null` when no such user is known.
     */
    suspend fun getUserName(userId: UserId): String?

    /**
     * Uploads [file] as an image and returns its persistent [FileId] on success.
     *
     * Performs the two-step temporal-upload + finalize flow via the files feature; the JS path
     * uses `XMLHttpRequest` under the hood so arbitrarily large images upload from the browser.
     *
     * @param file Image file chosen by the user on the current platform.
     * @return Stored [FileId], or `null` when the upload/finalize was rejected.
     */
    suspend fun uploadImage(file: MPPFile): FileId?

    /**
     * Builds the download URL of the image stored under [id], suitable as an `<img>` source or
     * an HTTP GET target.
     *
     * @param id Image file identifier.
     * @return Relative URL resolved against the configured server base URL.
     */
    fun imageUrl(id: FileId): String

    /**
     * Downloads the raw bytes of the image [id]. Used by platforms that decode images themselves
     * (JVM desktop / Android) rather than rendering directly from a URL.
     *
     * @param id Image file identifier.
     * @return Payload bytes, or `null` on failure.
     */
    suspend fun loadImageBytes(id: FileId): ByteArray?

    /**
     * Shared currency-conversion target selected by the user, agreed across all wishlist screens.
     * A `null` value means prices are shown in their original units with no conversion.
     */
    val selectedCurrency: StateFlow<CurrencyCode?>

    /**
     * Whether the currency-conversion feature is enabled on the server.
     *
     * @return `true` when conversion is available (an upstream App ID is configured); `false` otherwise.
     */
    suspend fun isCurrencyEnabled(): Boolean

    /**
     * Lists the currencies offered in the conversion dropdown.
     *
     * @return Available currencies, or an empty list when the feature is disabled.
     */
    suspend fun availableCurrencies(): List<CurrencyInfo>

    /**
     * Returns the latest exchange-rate snapshot used by views to convert displayed prices.
     *
     * @return Current [CurrencyRates], or `null` when the feature is disabled/unavailable.
     */
    suspend fun currencyRates(): CurrencyRates?

    /**
     * Updates the shared conversion target.
     *
     * @param code Target currency, or `null` to show original prices.
     */
    fun selectCurrency(code: CurrencyCode?)
}
