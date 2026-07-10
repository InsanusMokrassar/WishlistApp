# Feature: UI / Booking

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Client-only UI scenario for gift booking, extracted from `features/ui/wishlist` (issue #29, PR #31). Wraps `features/booking/client` via `BookingModel`. Web (JS) views rebuilt to Calm Studio markup; JVM uses Material v2; Android uses Material3. New Calm-Studio-only string resources in `BookingStrings` keep existing Material copy untouched.

## Screens

| Screen | ViewConfig | ViewModel | Description |
|--------|-----------|-----------|-------------|
| Book item (view A) | `BookingViewConfig(itemId, wishlistId)` | `BookingViewModel` | **Compact, embedded-inline** gift-reservation control rendered as `.btn` / `.pill` siblings in the wishlist item screen's `.actbar`. Renders only when `BookingViewModel.bookingState` is non-null; server hides state for item owner and anonymous (rule 3 / authorized-only), so those callers see nothing. States (sealed `BookingState`): `Free` → primary ".btn" with "Reserve this gift" copy; `BookedByMe` → green `.pill` "Reserved by you" + ".btn" "Cancel reservation"; `Booked` → green `.pill` "Reserved by someone" (no booker identity exposed). This is the config returned by the booking `WishlistAdditionalConfigsProvider` from the wishlist item screen. |
| My presents (view B) | `MyPresentsBooksViewConfig()` | `MyPresentsBooksViewModel` | **Reserved items section** — lists every wishlist item the caller has booked (the presents the caller plans to make). Reachable via sidebar primary "Reserved" entry; wired through `SidebarViewInteractor.onSelectReserved` → `MyPresentsBooksViewConfig` (bound in `client/ClientPlugin`). Sidebar shows live reserved-count badge sourced from `BookingModel.myPresentsBooks().size`. Web view renders `.content-inner` + `.pagehead` (title + subline) over `.grid` of `.card`s, each bearing `.reserved-flag` in Calm Studio green; empty state displays `CalmIcons.bookmark` glyph. Dropped in-content back button (now primary sidebar section). Exposes only caller's own bookings; no other user identity leaked. |

## Models

| Type | Description |
|------|-------------|
| `BookingModel` | Interface wrapping `features/booking/client` `BookingFeature`; methods `getBookingState(itemId)`, `bookItem(itemId)` → `BookingFeature.tryBook`, `cancelBooking(itemId)` → `BookingFeature.cancelBooking`, `myPresentsBooks(): List<BookingFeatureItem>`. Single anonymous impl in the feature's common `Plugin.kt`. |
| `MyPresentsBooksViewInteractor` | `suspend fun onSelectReserved(config)` wires view B to sidebar "Reserved" entry; impl in `client/ClientPlugin`. |

> `BookingViewInteractor` was REMOVED: view A is embedded inline with no back navigation, so `BookingViewModel` takes only `(node, model)` and the `client/ClientPlugin` binding is gone.

## Architecture Notes

- View A (`BookingView`) is compact and embedded inline — renders `.btn` / `.pill` as `.actbar` flex siblings; owns NO top-bar title and NO back button. View B (`MyPresentsBooksView`) is now a primary sidebar section; renders `.content-inner` + `.pagehead` shell + `.grid` card layout (web-only Calm Studio).
- All booking business rules stay server-side in `features/booking`; views only render `BookingState` and forward user intents.
- View A self-gates: when server hides booking state (owner / anonymous) `bookingState` is `null` and view renders nothing (defense-in-depth on top of server gate).
- `BookingViewModel` / `MyPresentsBooksViewModel` are Koin `factory` registrations; both views registered as `NavigationNodeFactory.Typed` in each platform plugin (`JSPlugin`, `JVMPlugin`, `AndroidPlugin`).
- Web (JS) views use Calm Studio markup (`.btn`, `.pill`, `.card`, `.reserved-flag`, `.content-inner`, `.grid`). JVM and Android views unchanged; Material v2 and Material3 copy untouched.
- Localized strings live in `BookingStrings` (EN + RU). Calm-Studio-only strings (reserveGiftButton, cancelReservationButton, reservedByYouLabel, reservedBySomeoneLabel, reservedTitle, reservedSubline, reservedEmptyTitle, reservedEmptyBody, reservedFlag) kept separate from Material strings.
- Privacy: owner-hidden / booker-anonymous rules server-enforced in `features/booking`. Owners see only THAT item is reserved (item card marker + count), never WHO. Reserved section lists only caller's own bookings.
- **Feature Interface Return Model Rule:** `BookingModel.myPresentsBooks()` and `MyPresentsBooksViewModel.presentsState` now hold `BookingFeatureItem` (from `booking.common.models`) instead of `RegisteredWishlistItem`, per `agents/CODING.md`'s Feature Interface Return Model Rule — `BookingFeature.myPresentsBooks` (client) and `BookingService.myPresentsBooks` (server) were retyped to match. `MyPresentsBooksView`'s `ReservedCard(item: BookingFeatureItem)` field access is unchanged since the new model mirrors `RegisteredWishlistItem`'s display fields.
