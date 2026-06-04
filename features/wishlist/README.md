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
| PUT | `/wishlist/update/{id}` | Bearer | `NewWishlistInFeature → 200 \| 400 \| 403 \| 404` | Replace wishlist data if caller is owner |
| DELETE | `/wishlist/delete/{id}` | Bearer | `→ 200 \| 400 \| 403 \| 404` | Remove wishlist if caller is owner |

### Wishlist Items (`/wishlistItem/...`)

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItem/getByWishlistId/{wishlistId}` | None | `→ List<RegisteredWishlistItem> \| 400` | All items in a wishlist; public |
| POST | `/wishlistItem/create` | Bearer | `NewWishlistItem → RegisteredWishlistItem \| 500` | Create item; caller must own parent wishlist (null=not found or not owner → 500) |
| PUT | `/wishlistItem/update/{id}` | Bearer | `NewWishlistItem → 200 \| 400 \| 403 \| 404` | Replace item data if caller owns parent wishlist |
| DELETE | `/wishlistItem/delete/{id}` | Bearer | `→ 200 \| 400 \| 403 \| 404` | Remove item if caller owns parent wishlist |

### Item Bookings (`/wishlistItemBooking/...`)

Gift-reservation ("booking") of another user's wishlist item. **Every route requires a valid bearer token** (the whole route tree is wrapped in `authenticate { }`): anonymous callers get `401` and never see booking state. The item **owner** is fully cut off — every booking route returns `403` when the caller owns the parent wishlist, so an owner cannot learn whether their item is booked. Responses never reveal WHO booked an item; only `booked` / `bookedByMe` booleans are returned.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItemBooking/state/{itemId}` | Bearer | `→ BookingState \| 400 \| 401 \| 403 \| 404` | Booking status visible to a non-owner authorized caller |
| POST | `/wishlistItemBooking/book/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404 \| 409` | Reserve the item for the caller |
| POST | `/wishlistItemBooking/cancel/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404` | Cancel the caller's own reservation |

Booking HTTP status semantics:
- `200` — success
- `400` — `{itemId}` not a valid Long
- `401` — no/invalid bearer token (anonymous)
- `403` — caller owns the item (state hidden from owner) OR, for `cancel`, the booking belongs to another user
- `404` — item or parent wishlist not found
- `409` — `book` on an already-booked item (single active booking enforced)

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
| `NewWishlistItem` | common | Create/update payload: `wishlistId`, `title`, `amount: Int = 1` (desired quantity, always `>= 1`), `priority: Priority = Priority.Medium`, `approximatePrice?: Amount`, `priceUnits: String`, `links: List<String>`, `description: String`, `imageIds: List<FileId> = emptyList()` — ordered ids of attached images (default empty) |
| `RegisteredWishlistItem` | common | Persisted entity: adds `id: WishlistItemId` to `NewWishlistItem` fields |
| `FileId` | common | Imported from `features/files` — string type wrapping a file identifier |
| `WishlistsItemsFeature` | client | Client-side interface: `getByWishlistId`, `create`, `update`, `delete` |

### Booking

| Type | Package | Description |
|------|---------|-------------|
| `BookingId` | common | `@JvmInline value class(val long: Long)` — primary key |
| `Booking` | common | Sealed interface (`itemId: WishlistItemId`, `userId: UserId`); base for `NewBooking` and `RegisteredBooking` |
| `NewBooking` | common | Create payload: `itemId`, `userId` (the booker) |
| `RegisteredBooking` | common | Persisted entity: adds `id: BookingId` |
| `BookingState` | common | Wire DTO returned to non-owner authorized callers: `booked: Boolean`, `bookedByMe: Boolean`. **Carries no booker identity** — others learn only that the item is reserved, never by whom. Owners never receive this DTO (server answers `403`). |
| `BookingFeature` | client | Client-side interface: `getState(itemId): BookingState?` (null when owner/unauthorized/missing), `book(itemId): Boolean`, `cancel(itemId): Boolean` |

## Architecture Notes

- `WishlistService` and `WishlistItemService` are **not** bound to `WishlistsFeature` / `WishlistsItemsFeature` in Koin because their mutation methods carry an explicit `callerId: UserId` parameter absent from the client interfaces. Routing configurators inject the services directly.
- Ownership check for wishlists: `WishlistService.update`/`delete` fetch the entity from the repo, compare `userId == callerId`; `null` = not found, `false` = not owner.
- Ownership check for items: `WishlistItemService.create` resolves parent wishlist from `NewWishlistItem.wishlistId`; returns `null` for both not found and not owner. `update`/`delete` fetch the item then the parent wishlist; `null` = item or parent not found, `false` = not owner.
- `WishlistItemService(get(), get())` — two Koin `get()` calls: first resolves `WishlistItemRepo`, second resolves `WishlistRepo` (registered by `wishlist.common.JVMPlugin`).
- DB tables:
  - `wishlists`: `id BIGINT PK AUTO`, `user_id BIGINT`, `title TEXT`, `default_price_units TEXT` (default `""`)
  - `wishlist_items`: `id BIGINT PK AUTO`, `wishlist_id BIGINT`, `title TEXT`, `amount INT` (default 1 = desired quantity), `priority_weight BIGINT` (default 50 = Priority.Medium), `approx_price_int BIGINT NULL`, `approx_price_dec BIGINT NULL`, `price_units TEXT`, `description TEXT`
  - `wishlist_item_links`: `item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE`, `link TEXT`, `PK(item_id, link)`
  - `wishlist_item_images`: `item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE`, `file_id TEXT`, `order INT` (display order), `PK(item_id, file_id)` — managed by `ExposedWishlistItemRepo`
- `amount` (item desired quantity) stored as INT column `amount` with `.default(1)`. Backward compatible: existing rows without the column get `1` via the column default applied by `SchemaUtils.createMissingTablesAndColumns`; absent JSON `amount` deserializes to `1` via the `Int = 1` default on the `@Serializable` model. Invariant: `amount >= 1`.
- `Priority` stored as single BIGINT column `priority_weight = priority.weight.toLong()`. Deserialization via `Priority.fromWeight(stored_weight.toUInt())` reconstructs the preset (Small/Medium/High) if weight matches; otherwise returns `Custom(weight)`.
- `Amount` stored as two BIGINT columns (`approx_price_int` = integer part, `approx_price_dec` = `ULong.decimalPart` stored as signed Long bit pattern). Both null → `Amount` is null.
- `links` are stored in a separate `wishlist_item_links` table, managed exclusively by `ExposedWishlistItemRepo` (private `linksTable`). On item delete, cascade FK removes link rows automatically. On read, a sub-query per item row fetches links within the same transaction (N+1 trade-off).
- `imageIds` are stored in a separate `wishlist_item_images` table, managed exclusively by `ExposedWishlistItemRepo` (private `imagesTable`). Columns: `item_id` (BIGINT FK → wishlist_items.id ON DELETE CASCADE), `file_id` (TEXT), `order` (INT for display order); PK = (item_id, file_id). On item delete, cascade FK removes image rows automatically. On read, images are fetched ordered by `order` column within the same transaction. On update, image rows are deleted and reinserted (same pattern as links).
- Client-side interfaces (`WishlistsFeature`, `WishlistsItemsFeature`) are declared in `features/wishlist/client` and implemented by `KtorWishlistFeature` / `KtorWishlistItemFeature`.
- **Booking (gift reservation, issue #29):** hosted inside this feature beside wishlist items (same layering as items live beside wishlists), no separate gradle module.
  - DB table `wishlist_item_bookings`: `id BIGINT PK AUTO`, `item_id BIGINT UNIQUE INDEX`, `user_id BIGINT`. The **unique index on `item_id`** enforces the single-active-booking invariant: a concurrent second insert for an already-booked item fails on the constraint, which `BookingService.book` catches and maps to `409 Conflict`.
  - Repo: `BookingRepo` (`Read`/`Write` split) with `getByItemId(itemId): RegisteredBooking?`; `ExposedBookingRepo` (`AbstractExposedCRUDRepo`, mirrors `ExposedWishlistItemRepo`) wrapped by `CacheBookingRepo` (`FullCRUDCacheRepo`) and bound as `BookingRepo` in `wishlist.common.JVMPlugin`.
  - `BookingService(bookingRepo, wishlistItemRepo, wishlistRepo)` enforces all four issue-#29 rules **server-side**:
    1. *Authorized-only* — routing wraps every booking route in Ktor `authenticate { }`; anonymous → `401`, service never invoked.
    2. *Others see booked-or-not, not WHO* — `BookingState` DTO carries only `booked` + `bookedByMe`; booker `UserId` is never serialized.
    3. *Owner hidden* — service resolves item → parent wishlist → owner id and compares to caller; owner → `OwnerForbidden` on `getState`/`book`/`cancel` → HTTP `403`, owner receives no booking data at all.
    4. *Single booking* — `book` pre-checks `getByItemId`; the DB unique index guards concurrency; conflict → `409`.
  - `cancel` is owner-anonymous-safe and idempotent: only the booker may cancel; a missing booking returns `Ok` (no leak).
  - `BookingService` is **not** bound to `BookingFeature` in Koin (its methods take an explicit `callerId`); `BookingRoutingsConfigurator` injects the service directly, mirroring `WishlistItemService`.
  - Client: `BookingFeature` declared in `features/wishlist/client`, implemented by `KtorBookingFeature` (HTTP only). `getState` returns `null` on any non-2xx (owner `403`, unauthorized, missing) so callers without visibility show no booking UI.
