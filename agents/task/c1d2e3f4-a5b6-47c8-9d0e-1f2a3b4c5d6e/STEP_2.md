# STEP 2 — Calm Studio redesign, phase 4/5: surface gift reservations (web/JS only)

## Scope
Made gift reservation a first-class web feature in Calm Studio. The full-stack `features/booking` and the `features/ui/booking` MVVM (Model/ViewModel/Config/Interactor) already existed and were reused unchanged — only the two JS views, the booking strings, and one shared icon changed. JVM (Material v2) and Android (Material3) views untouched. Sidebar "Reserved" entry + live count were already wired in phase 3 and were reused as-is.

## Files changed
New strings / icon:
- `features/ui/booking/.../BookingStrings.kt` — added Calm-Studio-only web copy (reserveGiftButton, cancelReservationButton, reservedByYouLabel, reservedBySomeoneLabel, reservedTitle, reservedSubline, reservedEmptyTitle, reservedEmptyBody, reservedFlag), EN + RU. Existing strings left intact for the Material clients.
- `features/common/client/.../ui/components/CalmIcons.kt` — added `bookmark` glyph (Reserved empty state).

Rebuilt (Bootstrap -> Calm Studio classes), JS only:
- `features/ui/booking/src/jsMain/.../BookingView.kt` — inline reserve control: `Free` -> primary "Reserve this gift" button; `BookedByMe` -> "Reserved by you" pill + "Cancel reservation" button; `Booked` -> "Reserved by someone" pill. Emits `.btn`/`.pill` as direct `.actbar` flex siblings. Renders nothing for owner/anonymous (server hides state).
- `features/ui/booking/src/jsMain/.../MyPresentsBooksView.kt` — the "Reserved" section: `.content-inner` + `.pagehead` (title + subline) over a `.grid` of `.card`s with green `.reserved-flag`; empty state uses `CalmIcons.bookmark`. Dropped the in-content back button (now a primary sidebar section). Title provider returns "Reserved".

## Privacy
Owner-hidden / booker-anonymous rules are server-enforced in `features/booking` and unchanged. Owners see only THAT an item is reserved (item card marker + count), never WHO. The Reserved section lists only the caller's own bookings, leaking no other identity.

## Server endpoints
Reused existing `features/booking` endpoints (state / book / cancel / myPresentsBooks). No client stubs or API TODOs needed.

## Verification
`./gradlew :wishlist.features.ui.booking:compileKotlinJs` — passes.
