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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

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
     * Deep-copies a single item from another user's wishlist into one of the caller's own wishlists.
     *
     * The server enforces that [targetWishlistId] is owned by the caller and performs the copy
     * idempotently.
     *
     * @param sourceItemId Item to copy.
     * @param sourceWishlistId Wishlist the source item belongs to.
     * @param targetWishlistId Caller-owned wishlist that receives the new item.
     * @return The created (or pre-existing identical) item, or `null` on failure / authorization error.
     */
    suspend fun copyItemToWishlist(
        sourceItemId: WishlistItemId,
        sourceWishlistId: WishlistId,
        targetWishlistId: WishlistId
    ): RegisteredWishlistItem?

    /**
     * Enqueues a background server-side deep copy of another user's whole wishlist into the caller's
     * profile.
     *
     * @param sourceWishlistId Wishlist to copy.
     * @return `true` when the copy job was queued, `false` on failure.
     */
    suspend fun enqueueWishlistCopy(sourceWishlistId: WishlistId): Boolean

    /**
     * Reactive id of the authenticated caller ("me"), or `null` when anonymous / not yet resolved.
     *
     * Backed by the auth "me" [StateFlow], so it self-corrects as the first `getMe()` round-trip
     * completes and on every later login/logout. Ownership-derived UI state ([isOwnerFlow]) MUST be
     * built on top of this flow rather than a one-shot snapshot, otherwise it stays stale after a
     * cold start that races the auth subscription (PR #31 F2).
     */
    val currentUserIdFlow: StateFlow<UserId?>

    /**
     * Pure ownership predicate: returns `true` iff a caller is authenticated ([currentUserId] is
     * non-`null`) AND ([ownerId] is `null` OR [ownerId] equals [currentUserId]). A `null` [ownerId]
     * means "the caller's own context" (e.g. the wishlists list opened without a target user) and is
     * owned by any authenticated caller.
     *
     * Callers holding a nullable wishlist MUST treat a `null` wishlist as not-owned BEFORE calling,
     * e.g. `wishlist != null && isOwner(wishlist.userId, currentUserId)` — this predicate cannot
     * distinguish "no wishlist" from "own context".
     *
     * @param ownerId Owner id of the checked context, or `null` for the caller's own context.
     * @param currentUserId Id of the authenticated caller, or `null` when anonymous.
     * @return `true` when the authenticated caller owns the context; `false` for anonymous callers.
     */
    fun isOwner(ownerId: UserId?, currentUserId: UserId?): Boolean =
        currentUserId != null && (ownerId == null || ownerId == currentUserId)

    /**
     * Reactive ownership of the context identified by [ownerId], tracking [currentUserIdFlow] so the
     * result self-corrects on login/logout and after the cold-start "me" round-trip.
     *
     * Use only for contexts with a fixed [ownerId] (e.g. a screen's target user id). For a context
     * whose owner is itself loaded asynchronously (a nullable wishlist), combine [currentUserIdFlow]
     * with the loaded value and call [isOwner] directly, so a missing context reads as not-owned.
     *
     * @param ownerId Owner id of the checked context, or `null` for the caller's own context.
     * @return Flow emitting `true` while the authenticated caller owns the context.
     */
    fun isOwnerFlow(ownerId: UserId?): Flow<Boolean> =
        currentUserIdFlow.map { isOwner(ownerId, it) }

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

    /**
     * Reads the last persisted items view mode for the wishlist screens.
     *
     * Backed by per-platform local storage (browser `localStorage` on JS), so a returning user keeps
     * the presentation chosen before the refresh / reopen.
     *
     * @return The saved [WishlistViewMode], or [WishlistViewMode.List] when nothing was ever saved.
     */
    suspend fun getSavedViewMode(): WishlistViewMode

    /**
     * Persists [mode] as the latest user-selected items view mode.
     *
     * @param mode View mode to store; restored by [getSavedViewMode] on the next screen open.
     */
    suspend fun saveViewMode(mode: WishlistViewMode)
}
