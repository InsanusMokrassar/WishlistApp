# Feature: Wishlist

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Full-stack wishlist management. Users can create wishlists and add items to them. All mutation operations enforce **caller ownership**: the server resolves the caller from the bearer token and rejects mutations on resources the caller does not own. Depends on `features/users` (for `UserId`) and `features/auth/server` (for `getCallerUserIdOrAnswerUnauthorized`).

## Routes

### Wishlists (`/wishlist/...`)

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlist/getByUserId/{userId}` | Bearer | `→ List<RegisteredWishlist>` | All wishlists owned by a specific user |
| GET | `/wishlist/getMy` | Bearer | `→ List<RegisteredWishlist>` | All wishlists owned by the authenticated caller |
| POST | `/wishlist/create` | Bearer | `NewWishlistInFeature → RegisteredWishlist \| 500` | Create wishlist; owner resolved from bearer token |
| PUT | `/wishlist/update/{id}` | Bearer | `NewWishlistInFeature → 200 \| 403 \| 404` | Replace wishlist data if caller is owner |
| DELETE | `/wishlist/delete/{id}` | Bearer | `→ 200 \| 403 \| 404` | Remove wishlist if caller is owner |

### Wishlist Items (`/wishlistItem/...`)

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItem/getByWishlistId/{wishlistId}` | Bearer | `→ List<RegisteredWishlistItem>` | All items in a wishlist |
| POST | `/wishlistItem/create` | Bearer | `NewWishlistItem → RegisteredWishlistItem \| 500` | Create item; caller must own parent wishlist |
| PUT | `/wishlistItem/update/{id}` | Bearer | `NewWishlistItem → 200 \| 403 \| 404` | Replace item data if caller owns parent wishlist |
| DELETE | `/wishlistItem/delete/{id}` | Bearer | `→ 200 \| 403 \| 404` | Remove item if caller owns parent wishlist |

HTTP ownership semantics: `200` = success, `403` = caller not owner, `404` = not found, `500` = create failure.

## Models

### Wishlist

| Type | Description |
|------|-------------|
| `WishlistId` | `@JvmInline value class(Long)` — primary key |
| `NewWishlist` | Internal create payload: `userId: UserId`, `title: String` |
| `NewWishlistInFeature` | Client-facing create/update payload: `title: String` (no userId) |
| `RegisteredWishlist` | Persisted entity: `id`, `userId`, `title` |
| `WishlistsFeature` | Client-facing interface: `getByUserId`, `getMyWishlists`, `create`, `update`, `delete` |

### Wishlist Item

| Type | Description |
|------|-------------|
| `WishlistItemId` | `@JvmInline value class(Long)` — primary key |
| `NewWishlistItem` | Create/update payload: `wishlistId`, `title`, `approximatePrice?`, `priceUnits`, `links`, `description` |
| `RegisteredWishlistItem` | Persisted entity: adds `id: WishlistItemId` to `NewWishlistItem` fields |
| `WishlistsItemsFeature` | Client-facing interface: `getByWishlistId`, `create`, `update`, `delete` |

## Architecture Notes

- `WishlistService` and `WishlistItemService` are **not** bound to client-facing feature interfaces in Koin because their mutation methods carry an explicit `callerId: UserId` parameter absent from the client interfaces. Routing configurators inject the services directly.
- Ownership check for items: `WishlistItemService` resolves parent wishlist via `WishlistRepo` to compare `userId`. For `update`/`delete`, the item is fetched first, then the parent wishlist. For `create`, `NewWishlistItem.wishlistId` identifies the parent.
- `WishlistItemService(get(), get())` — second `get()` resolves `WishlistRepo` (registered by `wishlist.common.JVMPlugin`).
- DB tables: `wishlists` (id BIGINT, user_id BIGINT, title TEXT); `wishlist_items` (id BIGINT, wishlist_id BIGINT, title TEXT, approx_price_int BIGINT NULL, approx_price_dec BIGINT NULL, price_units TEXT, links TEXT JSON, description TEXT).
- `Amount` stored as two separate BIGINT columns (int + decimal parts) — no floating-point.
- `links` stored as JSON text array in PostgreSQL.
