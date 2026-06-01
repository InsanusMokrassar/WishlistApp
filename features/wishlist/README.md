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
| `Wishlist` | common | Sealed interface; base for `NewWishlist` and `RegisteredWishlist` (`userId`, `title`) |
| `NewWishlist` | common | Internal create payload: `userId: UserId`, `title: String`; never sent over the wire |
| `NewWishlistInFeature` | common | Client-facing create/update payload: `title: String` only; owner resolved server-side |
| `RegisteredWishlist` | common | Persisted entity: `id: WishlistId`, `userId: UserId`, `title: String` |
| `WishlistsFeature` | client | Client-side interface: `getByUserId`, `getMyWishlists`, `create`, `update`, `delete` |

### Wishlist Item

| Type | Package | Description |
|------|---------|-------------|
| `Priority` | common | Sealed interface with `val weight: UInt`; variants: `Small` (weight 0), `Medium` (weight 50), `High` (weight 100), `Custom(weight: UInt)`. Companion `fromWeight(UInt): Priority` maps known weights to presets, else Custom. `PrioritySerializer` encodes weight-only as a single `Long` (`PrimitiveKind.LONG`, serial name `wishlist_item_priority`); deserialize narrows the `Long` back to `UInt` and calls `fromWeight` to reconstruct the preset/custom variant. |
| `WishlistItemId` | common | `@JvmInline value class(val long: Long)` — primary key |
| `WishlistItem` | common | Sealed interface; base for `NewWishlistItem` and `RegisteredWishlistItem` |
| `NewWishlistItem` | common | Create/update payload: `wishlistId`, `title`, `priority: Priority = Priority.Medium`, `approximatePrice?: Amount`, `priceUnits: String`, `links: List<String>`, `description: String` |
| `RegisteredWishlistItem` | common | Persisted entity: adds `id: WishlistItemId` to `NewWishlistItem` fields |
| `WishlistsItemsFeature` | client | Client-side interface: `getByWishlistId`, `create`, `update`, `delete` |

## Architecture Notes

- `WishlistService` and `WishlistItemService` are **not** bound to `WishlistsFeature` / `WishlistsItemsFeature` in Koin because their mutation methods carry an explicit `callerId: UserId` parameter absent from the client interfaces. Routing configurators inject the services directly.
- Ownership check for wishlists: `WishlistService.update`/`delete` fetch the entity from the repo, compare `userId == callerId`; `null` = not found, `false` = not owner.
- Ownership check for items: `WishlistItemService.create` resolves parent wishlist from `NewWishlistItem.wishlistId`; returns `null` for both not found and not owner. `update`/`delete` fetch the item then the parent wishlist; `null` = item or parent not found, `false` = not owner.
- `WishlistItemService(get(), get())` — two Koin `get()` calls: first resolves `WishlistItemRepo`, second resolves `WishlistRepo` (registered by `wishlist.common.JVMPlugin`).
- DB tables:
  - `wishlists`: `id BIGINT PK AUTO`, `user_id BIGINT`, `title TEXT`
  - `wishlist_items`: `id BIGINT PK AUTO`, `wishlist_id BIGINT`, `title TEXT`, `priority_weight BIGINT` (default 50 = Priority.Medium), `approx_price_int BIGINT NULL`, `approx_price_dec BIGINT NULL`, `price_units TEXT`, `description TEXT`
  - `wishlist_item_links`: `item_id BIGINT FK→wishlist_items.id ON DELETE CASCADE`, `link TEXT`, `PK(item_id, link)`
- `Priority` stored as single BIGINT column `priority_weight = priority.weight.toLong()`. Deserialization via `Priority.fromWeight(stored_weight.toUInt())` reconstructs the preset (Small/Medium/High) if weight matches; otherwise returns `Custom(weight)`.
- `Amount` stored as two BIGINT columns (`approx_price_int` = integer part, `approx_price_dec` = `ULong.decimalPart` stored as signed Long bit pattern). Both null → `Amount` is null.
- `links` are stored in a separate `wishlist_item_links` table, managed exclusively by `ExposedWishlistItemRepo` (private `linksTable`). On item delete, cascade FK removes link rows automatically. On read, a sub-query per item row fetches links within the same transaction (N+1 trade-off).
- Client-side interfaces (`WishlistsFeature`, `WishlistsItemsFeature`) are declared in `features/wishlist/client` and implemented by `KtorWishlistFeature` / `KtorWishlistItemFeature`.
