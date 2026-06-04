# Feature: UI / Booking

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Client-only UI scenario for gift booking, extracted from `features/ui/wishlist` (issue #29, PR #31). Wraps `features/booking/client` via `BookingModel`. JS views use Bootstrap CSS classes via Compose HTML; JVM uses Material v2; Android uses Material3.

## Screens

| Screen | ViewConfig | ViewModel | Description |
|--------|-----------|-----------|-------------|
| Book item (view A) | `BookingViewConfig(itemId, wishlistId)` | `BookingViewModel` | Gift-booking screen. Shows booking controls only when `BookingViewModel.bookingState` is non-null; the server returns no state for the item owner and for anonymous callers (rule 3 / authorized-only), so those callers see an empty screen. States: "Nobody booked this yet" + Book button (free), "You booked this item" + Cancel button (booked by caller), "Someone is going to gift this" (booked by another, no identity). This is the config returned by the booking `WishlistAdditionalConfigsProvider` from the wishlist item screen. |
| My presents (view B) | `MyPresentsViewConfig()` | `MyPresentsViewModel` | Lists every wishlist item the caller has booked (the presents the caller plans to make). **Unreachable by design** — issue #29 point #6: no navigation path pushes `MyPresentsViewConfig`. The view, ViewModel, config, and `NavigationNodeFactory` are registered, but nothing opens it yet. |

## Models

| Type | Description |
|------|-------------|
| `BookingModel` | Interface wrapping `features/booking/client` `BookingFeature`; methods `getBookingState(itemId)`, `bookItem(itemId)`, `cancelBooking(itemId)`, `myPresents()`. Single anonymous impl in the feature's common `Plugin.kt`. |
| `BookingViewInteractor` | `suspend fun onBack(node)` → `node.chain.pop()`; impl in `client/ClientPlugin`. |
| `MyPresentsViewInteractor` | `suspend fun onBack(node)` → `node.chain.pop()`; impl in `client/ClientPlugin`. |

## Architecture Notes

- Both views implement `TopBarTitleProvider` and reuse the shared `BackButton` / `ListRow` / `ScreenTitle` components from `features/common/client`.
- All booking business rules stay server-side in `features/booking`; the views only render `BookingState` and forward user intents.
- View A self-gates: when the server hides booking state (owner / anonymous) `bookingState` is `null` and the screen renders no controls (defense-in-depth on top of the server gate).
- `BookingViewModel` / `MyPresentsViewModel` are Koin `factory` registrations; both views are registered as `NavigationNodeFactory.Typed` in each platform plugin (`JSPlugin`, `JVMPlugin`, `AndroidPlugin`).
- Localized strings live in `BookingStrings` (EN + RU), moved from the wishlist UI feature.
