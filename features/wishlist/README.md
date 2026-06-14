# Feature: Wishlist

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Full-stack wishlist management. Users create wishlists and add items to them. All mutation operations enforce **caller ownership**: the server resolves the caller from the bearer token and rejects mutations on resources the caller does not own. Read-only GET routes (`getByUserId`, `getById`, `getByWishlistId`) are **public** — no bearer token required. Depends on `features/users` (for `UserId`) and `features/auth/server` (for `getCallerUserIdOrAnswerUnauthorized`).

## Routes

### Wishlists (`/wishlist/...`)

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlist/getByUserId/{userId}` | None | `→ List<RegisteredWishlist> \| 400` | All wishlists owned by `userId`; public |
| GET | `/wishlist/getById/{id}` | None | `→ RegisteredWishlist \| 400 \| 404` | Single wishlist by id; public |
| GET | `/wishlist/getMy` | Bearer | `→ List<RegisteredWishlist>` | All wishlists owned by the authenticated caller |
| POST | `/wishlist/create` | Bearer | `NewWishlistInFeature → RegisteredWishlist \| 500` | Create wishlist; owner resolved from bearer token |
| POST | `/wishlist/copy` | Bearer | `CopyWishlistRequest → 202 \| 500` | Enqueue a background deep-copy of any wishlist into the caller's profile (recipient = caller) |
| PUT | `/wishlist/update/{id}` | Bearer | `NewWishlistInFeature → 200 \| 400 \| 403 \| 404` | Replace wishlist data if caller is owner |
| DELETE | `/wishlist/delete/{id}` | Bearer | `→ 200 \| 400 \| 403 \| 404` | Remove wishlist if caller is owner |

### Wishlist Items (`/wishlistItem/...`)

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItem/getByWishlistId/{wishlistId}` | None | `→ List<RegisteredWishlistItem> \| 400` | All items in a wishlist; public |
| POST | `/wishlistItem/create` | Bearer | `NewWishlistItem → RegisteredWishlistItem \| 500` | Create item; caller must own parent wishlist (null=not found or not owner → 500) |
| POST | `/wishlistItem/copy` | Bearer | `CopyItemRequest → RegisteredWishlistItem \| 500` | Deep-copy a source item (any owner) into a caller-owned target wishlist; idempotent (returns existing identical item if present); 500 when target not owned / source or target missing |
| PUT | `/wishlistItem/update/{id}` | Bearer | `NewWishlistItem → 200 \| 400 \| 403 \| 404` | Replace item data if caller owns parent wishlist |
| DELETE | `/wishlistItem/delete/{id}` | Bearer | `→ 200 \| 400 \| 403 \| 404` | Remove item if caller owns parent wishlist |

> **Note** Gift booking (`/wishlistItemBooking/...`) was extracted into the standalone `features/booking` feature (issue #29, PR #31). See `features/booking/README.md`.

HTTP status semantics:
- `200` — success
- `400` — path parameter not a valid Long
- `403` — resource exists, caller is not the owner
- `404` — resource not found
- `500` — create failure (repo returned nothing, parent not found, or caller not owner of parent)

Note: item `create` maps both "parent not found" and "caller not owner" to `null` → `500`, unlike wishlist `update`/`delete` which return `null`=not_found / `false`=unauthorized → `404`/`403`.

## Models

### Wishlist

| Type | Package | Description |
|------|---------|-------------|
| `WishlistId` | common | `@JvmInline value class(val long: Long)` — primary key |
| `Wishlist` | common | Sealed interface; base for `NewWishlist` and `RegisteredWishlist` (`userId`, `title`, `defaultPriceUnits: String`) |
| `NewWishlist` | common | Internal create payload: `userId: UserId`, `title: String`, `defaultPriceUnits: String = ""`; never sent over the wire |
| `NewWishlistInFeature` | common | Client-facing create/update payload: `title: String`, `defaultPriceUnits: String = ""`; owner resolved server-side |
| `RegisteredWishlist` | common | Persisted entity: `id: WishlistId`, `userId: UserId`, `title: String`, `defaultPriceUnits: String` |
| `WishlistsFeature` | client | Client-side interface: `getByUserId`, `getMyWishlists`, `create`, `update`, `delete` |

### Wishlist Item

| Type | Package | Description |
|------|---------|-------------|
| `Priority` | common | Sealed interface with `val weight: UInt`; variants: `Small` (weight 0), `Medium` (weight 50), `High` (weight 100), `Custom(weight: UInt)`. Companion `fromWeight(UInt): Priority` maps known weights to presets, else Custom. `PrioritySerializer` encodes weight-only as a single `Long` (`PrimitiveKind.LONG`, serial name `wishlist_item_priority`); deserialize narrows the `Long` back to `UInt` and calls `fromWeight` to reconstruct the preset/custom variant. |
| `WishlistItemId` | common | `@JvmInline value class(val long: Long)` — primary key |
| `WishlistItem` | common | Sealed interface; base for `NewWishlistItem` and `RegisteredWishlistItem` |
| `NewWishlistItem` | common | Create/update payload: `wishlistId`, `title`, `amount: Int = 1` (desired quantity, always `>= 1`), `priority: Priority = Priority.Medium`, `approximatePrice?: Amount`, `priceUnits: String`, `links: List<WishlistItemLink>`, `description: String`, `imageIds: List<FileId> = emptyList()` — ordered ids of attached images (default empty) |
| `RegisteredWishlistItem` | common | Persisted entity: adds `id: WishlistItemId` to `NewWishlistItem` fields |
| `WishlistItemLink` | common | `@Serializable data class(url: String, title: String? = null)` — one external link with an optional title (issue #38). Extension `WishlistItemLink.displayText` returns `title` (when non-blank) else `url` — the single "title-as-link or bare-link" display rule shared by all client views |
| `FileId` | common | Imported from `features/files` — string type wrapping a file identifier |
| `WishlistsItemsFeature` | client | Client-side interface: `getByWishlistId`, `create`, `copy`, `update`, `delete` |

### Copy (deep-copy of items and wishlists)

| Type | Package | Description |
|------|---------|-------------|
| `CopyItemRequest` | common | `@Serializable data class(sourceItemId, sourceWishlistId, targetWishlistId)` — body of `POST /wishlistItem/copy` |
| `CopyWishlistRequest` | common | `@Serializable data class(sourceWishlistId)` — body of `POST /wishlist/copy`; recipient is always the caller |
| `WishlistCopyJobId` | common | `@JvmInline value class(val long: Long)` — queue row primary key |
| `WishlistCopyJobStatus` | common | enum `Pending`, `InProgress`, `Done`, `Failed`; `Pending`+`InProgress` = unfinished (resumed on startup) |
| `WishlistCopyJob` | common | Sealed base for `NewWishlistCopyJob` / `RegisteredWishlistCopyJob` (`sourceWishlistId`, `recipientUserId`, `status`) |
| `RegisteredWishlistCopyJob.toNewItem` / `WishlistItem.hasSameContentAs` | common | `RegisteredWishlistItem.toNewItem(targetWishlistId)` deep-copies value fields; `hasSameContentAs` compares all value fields (used by both idempotency checks) |
| `WishlistCopyFeature` | client | Client-side interface: `enqueueCopy(CopyWishlistRequest): Boolean` |

> **Note** Booking models (`BookingId` / `Booking` / `NewBooking` / `RegisteredBooking` / `BookingState`) and the `BookingFeature` client interface were moved to `features/booking`. The wishlist read repos (`WishlistItemRepo` / `WishlistRepo`) are still used by `features/booking/server` to resolve an item's owner.

## Architecture Notes

- `WishlistService` and `WishlistItemService` are **not** bound to `WishlistsFeature` / `WishlistsItemsFeature` in Koin because their mutation methods carry an explicit `callerId: UserId` parameter absent from the client interfaces. Routing configurators inject the services directly.
- Ownership check for wishlists: `WishlistService.update`/`delete` fetch the entity from the repo, compare `userId == callerId`; `null` = not found, `false` = not owner.
- Ownership check for items: `WishlistItemService.create` resolves parent wishlist from `NewWishlistItem.wishlistId`; returns `null` for both not found and not owner. `update`/`delete` fetch the item then the parent wishlist; `null` = item or parent not found, `false` = not owner.
- `WishlistItemService(get(), get())` — two Koin `get()` calls: first resolves `WishlistItemRepo`, second resolves `WishlistRepo` (registered by `wishlist.common.JVMPlugin`).
- DB tables:
  - `wishlists`: `id BIGINT PK AUTO`, `user_id BIGINT`, `title TEXT`, `default_price_units TEXT` (default `""`)
  - `wishlist_items`: `id BIGINT PK AUTO`, `wishlist_id BIGINT`, `title TEXT`, `amount INT` (default 1 = desired quantity), `priority_weight BIGINT` (default 50 = Priority.Medium), `approx_price_int BIGINT NULL`, `approx_price_dec BIGINT NULL`, `price_units TEXT`, `description TEXT`
  - `wishlist_item_links`: `item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE`, `link TEXT` (= url), `title TEXT NULL` (optional link title, issue #38), `PK(item_id, link)`
  - `wishlist_item_images`: `item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE`, `file_id TEXT`, `order INT` (display order), `PK(item_id, file_id)` — managed by `ExposedWishlistItemRepo`
- `amount` (item desired quantity) stored as INT column `amount` with `.default(1)`. Backward compatible: existing rows without the column get `1` via the column default applied by `SchemaUtils.createMissingTablesAndColumns`; absent JSON `amount` deserializes to `1` via the `Int = 1` default on the `@Serializable` model. Invariant: `amount >= 1`.
- `Priority` stored as single BIGINT column `priority_weight = priority.weight.toLong()`. Deserialization via `Priority.fromWeight(stored_weight.toUInt())` reconstructs the preset (Small/Medium/High) if weight matches; otherwise returns `Custom(weight)`.
- `Amount` stored as two BIGINT columns (`approx_price_int` = integer part, `approx_price_dec` = `ULong.decimalPart` stored as signed Long bit pattern). Both null → `Amount` is null.
- `links` are stored in a separate `wishlist_item_links` table, managed exclusively by `ExposedWishlistItemRepo` (private `linksTable`). On item delete, cascade FK removes link rows automatically. On read, a sub-query per item row fetches links within the same transaction (N+1 trade-off).
- **Optional link titles (issue #38):** a link is the model type `WishlistItemLink(url, title?)` (was a bare `String`). `links` is `List<WishlistItemLink>`. The `link` column of `wishlist_item_links` keeps storing the **url** (no rename → no data migration); a new nullable `title` TEXT column stores the optional title. Backward-compatible: `SchemaUtils.createMissingTablesAndColumns` (via `initTable`) adds the nullable `title` column on startup; pre-#38 rows have `title = NULL` and read back as `WishlistItemLink(url, null)` (bare-url display). PK stays `(item_id, link)` — url is the natural key, title is not part of identity. **Wire/JSON note:** `WishlistItemLink` uses a custom `WishlistItemLinkSerializer` so a title-less link is serialized as a plain JSON **string** (the url), and a titled link as a `{"url","title"}` object. Decoding accepts either form (string → `title = null`, object → both fields). This keeps the `links` JSON wire-compatible with the legacy `["url", ...]` format, so a mixed-version client round-trips correctly.
- `imageIds` are stored in a separate `wishlist_item_images` table, managed exclusively by `ExposedWishlistItemRepo` (private `imagesTable`). Columns: `item_id` (BIGINT FK → wishlist_items.id ON DELETE CASCADE), `file_id` (TEXT), `order` (INT for display order); PK = (item_id, file_id). On item delete, cascade FK removes image rows automatically. On read, images are fetched ordered by `order` column within the same transaction. On update, image rows are deleted and reinserted (same pattern as links).
- Client-side interfaces (`WishlistsFeature`, `WishlistsItemsFeature`, `WishlistCopyFeature`) are declared in `features/wishlist/client` and implemented by `KtorWishlistFeature` / `KtorWishlistItemFeature` / `KtorWishlistCopyFeature`.
- **Booking (gift reservation, issue #29)** was extracted into the standalone `features/booking` feature (PR #31). The `wishlist_item_bookings` table and all four server-enforced rules now live there; `features/wishlist` only still exposes the `WishlistItemRepo` / `WishlistRepo` read repos that `features/booking/server` uses to resolve an item's owner. See `features/booking/README.md`.

### Copy feature (issue #30)

- **Single-item copy (synchronous):** `WishlistItemService.copyItem(CopyItemRequest, callerId)` enforces `target.userId == callerId` server-side, deep-copies the source item's value fields via `RegisteredWishlistItem.toNewItem(targetWishlistId)`, and is idempotent — if the target wishlist already holds an item with identical content (`WishlistItem.hasSameContentAs`) the existing item is returned instead of inserting a duplicate. Route: `POST /wishlistItem/copy` inside `authenticate {}`. Source items are read via the existing (public) item repo.
- **Whole-wishlist copy (asynchronous, persistent queue):** `POST /wishlist/copy` (inside `authenticate {}`) calls `WishlistCopyService.enqueue(sourceWishlistId, callerId)` which persists a `wishlist_copy_jobs` row (status `Pending`) and returns `202 Accepted`. Recipient is always the authenticated caller, never client-supplied.
- **Queue persistence:** `ExposedWishlistCopyJobRepo` (table `wishlist_copy_jobs`: `id BIGINT PK AUTO`, `source_wishlist_id BIGINT`, `recipient_user_id BIGINT`, `status TEXT`) wrapped by `CacheWishlistCopyJobRepo`, bound as `WishlistCopyJobRepo` in `wishlist.common.JVMPlugin`. Jobs survive process restarts.
- **Resume on restart + parallelism:** `WishlistCopyService.start()` (invoked from `wishlist.server.JVMPlugin.startPlugin`, after the common JVM repo plugin) re-scans `WishlistCopyJobRepo.getUnfinished()` (Pending+InProgress) and re-submits them. Submitted job ids flow through an unbounded `Channel`; the worker launches one child coroutine per job, bounded by a `Semaphore(4)`, so up to 4 jobs are deep-copied concurrently in the shared application `CoroutineScope`.
- **Two idempotency checks** in `WishlistCopyService.processJob` make a restart-resumed re-run safe (no duplicates):
  1. *Wishlist-by-name:* `wishlistRepo.getByUserId(recipient).find { it.title == source.title }` is reused if present, otherwise a new wishlist is created.
  2. *Item-existence:* for each source item, the new payload is compared against existing target items with `hasSameContentAs`; the item is created only when absent.
  Status transitions: `Pending → InProgress → Done`; a missing source wishlist or unobtainable target sets `Failed`.
- `WishlistCopyService` is registered as a `single` in the common server `Plugin` (its repo dependencies are JVM-only and resolved lazily); its worker is started only on JVM. `WishlistRoutingsConfigurator` gained a second ctor arg (`WishlistCopyService`).
