# Feature: Booking

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Full-stack gift-reservation ("booking") of another user's wishlist item. Extracted from `features/wishlist` (issue #29, PR #31). An authorized user reserves an item they plan to gift; at most one active booking exists per item. Depends on `features/wishlist/common` (for `WishlistItemId` / `RegisteredWishlistItem` and the wishlist read repos used to resolve an item's owner), `features/users/common` (for `UserId`), `features/common/server` (Database, routing), and `features/auth/server` (`getCallerUserIdOrAnswerUnauthorized`).

The booking server plugin must be loaded **after** `features/wishlist/server` and `features/common/server`: it reuses their `WishlistItemRepo` / `WishlistRepo` / `Database` singletons (Koin de-duplicates). It is registered last in `server/sample.config.json`.

## Routes

> All paths below are served under the global `/api` prefix (e.g. `/api/wishlistItemBooking/book/{itemId}`). The prefix is applied centrally by `features/common/server` (`InternalApplicationRoutingConfigurator`) and added on the client by `DefaultUrlHttpClientConfigurator`, which appends `/api` to the configured server base URL.

All routes live under `/wishlistItemBooking` and the whole tree is wrapped in `authenticate { }` — anonymous callers get `401` (rule 1).

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItemBooking/state/{itemId}` | Bearer | `→ BookingState \| 400 \| 401 \| 403 \| 404` | Booking status visible to a non-owner authorized caller |
| POST | `/wishlistItemBooking/book/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404 \| 409 \| 500` | Reserve the item for the caller |
| POST | `/wishlistItemBooking/cancel/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404` | Cancel the caller's own reservation |
| GET | `/wishlistItemBooking/myPresentsBooks` | Bearer | `→ List<RegisteredWishlistItem> \| 401` | Items the caller has booked (the presents the caller plans to make) |

`403` = caller owns the item (booking hidden from owners, rule 3) or, for `cancel`, the booking belongs to another user. `409` = `book` on an already-booked item (single-booking, rule 4). `500` = `book` hit a persistence failure (e.g. a unique-index violation racing past the in-process locker); the cause is logged server-side.

## Models

- `BookingId` — `@JvmInline value class(Long)` primary key.
- `Booking` (sealed) → `NewBooking(itemId, userId)`, `RegisteredBooking(id, itemId, userId)`.
- `BookingState` — `@Serializable` **sealed interface** with exactly three booker-anonymous cases: `Free`, `Booked` (booked by another user, identity omitted — rule 2), `BookedByMe`.
- `BookingResult` / `BookResult` / `CancelResult` — sealed result interfaces in `common/models/BookingResults.kt` (extracted from the server service); shared by `BookingService` and `BookingRoutingsConfigurator`. `BookResult` adds an `Error` case (→ `500`) for persistence failures during booking.
- `BookingRepo` / `ReadBookingRepo` / `WriteBookingRepo` — CRUD; `ReadBookingRepo` adds `getByItemId(itemId)` and `getByUserId(userId)`.
- `BookingFeature` (client) / `KtorBookingFeature` — `getState` / `tryBook` / `cancelBooking` / `myPresentsBooks`.
- `BookingService` (server) — owns all rules; methods `getState` / `tryBook` / `cancel` / `myPresentsBooks`; returns `BookingResult` / `BookResult` / `CancelResult`.

## Architecture Notes

- **Rule 1 (authorized-only):** `BookingRoutingsConfigurator` wraps the route tree in `authenticate { }`; the service is never invoked for anonymous callers.
- **Rule 2 (booker-anonymous):** `BookingState` is a sealed `Free` / `Booked` / `BookedByMe` — `Booked` carries no booker identity. `myPresentsBooks` returns the caller's OWN booked items (caller is the booker), leaking no other booker's identity.
- **Rule 3 (owner-hidden):** `BookingService.getState/tryBook/cancel` resolve `ownerOf(item)` and return `OwnerForbidden` → `403` when caller owns the parent wishlist.
- **Rule 4 (single-booking):** `ExposedBookingRepo` puts a UNIQUE index on `item_id`; **and** `BookingService` holds a `SmartRWLocker` — `getState`/`myPresentsBooks` run under `withReadAcquire`, `tryBook`/`cancel` under `withWriteLock`, so the check-then-create of `tryBook` is atomic in-process; the pre-check returns `AlreadyBooked` → `409`. A DB constraint violation that still races past the locker (e.g. multi-instance), or any other persistence failure, is logged and surfaced as `Error` → `500`.
- Exposed table name `wishlist_item_bookings` is unchanged from the pre-extraction implementation, preserving existing data.
- `CacheBookingRepo` (FullCRUDCacheRepo) caches by `BookingId`; `getByItemId` / `getByUserId` delegate to the persistent repo because the flat cache is not indexed by those columns. In `common/jvmMain/JVMPlugin`, `CacheBookingRepo` is registered as its own `single { CacheBookingRepo(...) }`, then `BookingRepo` is bound to it via `single<BookingRepo> { get<CacheBookingRepo>() }`.
