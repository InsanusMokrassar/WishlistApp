# Feature: Booking

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Full-stack gift-reservation ("booking") of another user's wishlist item. Extracted from `features/wishlist` (issue #29, PR #31). An authorized user reserves an item they plan to gift; at most one active booking exists per item. Depends on `features/wishlist/common` (for `WishlistItemId` / `RegisteredWishlistItem` and the wishlist read repos used to resolve an item's owner), `features/users/common` (for `UserId`), `features/common/server` (Database, routing), and `features/auth/server` (`getCallerUserIdOrAnswerUnauthorized`).

The booking server plugin must be loaded **after** `features/wishlist/server` and `features/common/server`: it reuses their `WishlistItemRepo` / `WishlistRepo` / `Database` singletons (Koin de-duplicates). It is registered last in `server/sample.config.json`.

## Routes

All routes live under `/wishlistItemBooking` and the whole tree is wrapped in `authenticate { }` — anonymous callers get `401` (rule 1).

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| GET | `/wishlistItemBooking/state/{itemId}` | Bearer | `→ BookingState \| 400 \| 401 \| 403 \| 404` | Booking status visible to a non-owner authorized caller |
| POST | `/wishlistItemBooking/book/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404 \| 409` | Reserve the item for the caller |
| POST | `/wishlistItemBooking/cancel/{itemId}` | Bearer | `→ 200 \| 400 \| 401 \| 403 \| 404` | Cancel the caller's own reservation |
| GET | `/wishlistItemBooking/myPresents` | Bearer | `→ List<RegisteredWishlistItem> \| 401` | Items the caller has booked (the presents the caller plans to make) |

`403` = caller owns the item (booking hidden from owners, rule 3) or, for `cancel`, the booking belongs to another user. `409` = `book` on an already-booked item (single-booking, rule 4).

## Models

- `BookingId` — `@JvmInline value class(Long)` primary key.
- `Booking` (sealed) → `NewBooking(itemId, userId)`, `RegisteredBooking(id, itemId, userId)`.
- `BookingState(booked, bookedByMe)` — wire DTO; carries **no booker identity** (rule 2).
- `BookingRepo` / `ReadBookingRepo` / `WriteBookingRepo` — CRUD; `ReadBookingRepo` adds `getByItemId(itemId)` and `getByUserId(userId)`.
- `BookingFeature` (client) / `KtorBookingFeature` — `getState` / `book` / `cancel` / `myPresents`.
- `BookingService` (server) — owns all rules; results `BookingResult`, `BookResult`, `CancelResult`.

## Architecture Notes

- **Rule 1 (authorized-only):** `BookingRoutingsConfigurator` wraps the route tree in `authenticate { }`; the service is never invoked for anonymous callers.
- **Rule 2 (booker-anonymous):** `BookingState` exposes only booleans. `myPresents` returns the caller's OWN booked items (caller is the booker), leaking no other booker's identity.
- **Rule 3 (owner-hidden):** `BookingService.getState/book/cancel` resolve `ownerOf(item)` and return `OwnerForbidden` → `403` when caller owns the parent wishlist.
- **Rule 4 (single-booking):** `ExposedBookingRepo` puts a UNIQUE index on `item_id`; `BookingService.book` pre-checks and catches the constraint violation → `AlreadyBooked` → `409`.
- Exposed table name `wishlist_item_bookings` is unchanged from the pre-extraction implementation, preserving existing data.
- `CacheBookingRepo` (FullCRUDCacheRepo) caches by `BookingId`; `getByItemId` / `getByUserId` delegate to the persistent repo because the flat cache is not indexed by those columns.
