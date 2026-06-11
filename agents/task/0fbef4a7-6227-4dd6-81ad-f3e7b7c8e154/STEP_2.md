# STEP_2 — Coding (result report)

ENTITY:
entity_id=task-29-booking; type=feature; state=implemented+built

CONTEXT:
- task_id=issue-29; agent_id=root-orchestrator; branch=issue/29-book-functionality
- host=features/wishlist (full-stack) + features/ui/wishlist (UI); no new gradle module.

## FILES CREATED
- features/wishlist/common/src/commonMain/kotlin/models/Booking.kt — BookingId, sealed Booking, NewBooking, RegisteredBooking
- features/wishlist/common/src/commonMain/kotlin/models/BookingState.kt — @Serializable BookingState(booked,bookedByMe)
- features/wishlist/common/src/commonMain/kotlin/repo/ReadBookingRepo.kt — +getByItemId
- features/wishlist/common/src/commonMain/kotlin/repo/WriteBookingRepo.kt
- features/wishlist/common/src/commonMain/kotlin/repo/BookingRepo.kt
- features/wishlist/common/src/commonMain/kotlin/repo/CacheBookingRepo.kt — FullCRUDCacheRepo
- features/wishlist/common/src/jvmMain/kotlin/repo/ExposedBookingRepo.kt — AbstractExposedCRUDRepo, table wishlist_item_bookings, item_id uniqueIndex
- features/wishlist/server/src/commonMain/kotlin/services/BookingService.kt — service + BookingResult/BookResult/CancelResult sealed results
- features/wishlist/server/src/commonMain/kotlin/configurators/BookingRoutingsConfigurator.kt — authenticate{} routes
- features/wishlist/client/src/commonMain/kotlin/BookingFeature.kt
- features/wishlist/client/src/commonMain/kotlin/KtorBookingFeature.kt

## FILES EDITED
- features/wishlist/common/src/commonMain/kotlin/Constants.kt — booking path parts
- features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt — ExposedBookingRepo + CacheBookingRepo bound BookingRepo
- features/wishlist/server/src/commonMain/kotlin/Plugin.kt — BookingService + BookingRoutingsConfigurator
- features/wishlist/client/src/commonMain/kotlin/Plugin.kt — KtorBookingFeature + BookingFeature binding
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt — getBookingState/bookItem/cancelBooking
- features/ui/wishlist/src/commonMain/kotlin/Plugin.kt — model impl delegates to BookingFeature
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt — bookingState + onBook/onCancelBooking
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt — 6 booking strings EN+RU
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemView.kt — booking section (Bootstrap) + disabled import
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemView.kt — booking section (Material v2)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemView.kt — booking section (Material3)
- features/wishlist/README.md — booking routes/models/arch notes (Operator Notes untouched)
- features/ui/wishlist/README.md — booking UI screen+arch notes (Operator Notes untouched)

## LAYER→FILE MAP
- model/DTO: Booking.kt, BookingState.kt
- DB: ExposedBookingRepo.kt (+repo ifaces + cache) ; table wishlist_item_bookings
- route: BookingRoutingsConfigurator.kt (+server Plugin.kt)
- service: BookingService.kt
- client-feature: BookingFeature.kt + KtorBookingFeature.kt (+client Plugin.kt)
- ui-model: WishlistsModel.kt (+ui Plugin.kt impl)
- viewmodel: WishlistItemViewModel.kt
- UI: 3× WishlistItemView.kt + WishlistStrings.kt

## 4 RULES — SERVER ENFORCEMENT (authoritative)
- rule_2 authorized-only: BookingRoutingsConfigurator wraps ALL routes in ktor authenticate{}; anonymous→401; getCallerUserIdOrAnswerUnauthorized.
- rule_3 owner-hidden: BookingService.ownerOf(itemId)==callerId → OwnerForbidden on getState/book/cancel → HTTP 403; owner receives zero booking data.
- rule_1 visibility-without-identity: BookingState DTO = {booked,bookedByMe} only; booker UserId never serialized.
- rule_4 single-booking: ExposedBookingRepo item_id uniqueIndex + book() pre-check; concurrent insert throws → caught → AlreadyBooked → 409.
(UI also hides booking section when bookingState==null → defense-in-depth, but server is authoritative.)

## BUILD/VERIFY
- :wishlist.features.wishlist.server:build → SUCCESS
- :wishlist.features.ui.wishlist:build (js+jvm+android) → SUCCESS (after adding org.jetbrains.compose.web.attributes.disabled import on JS view)
- :wishlist.server:build → SUCCESS
- :wishlist.client:build (js+jvm) → SUCCESS
- :wishlist.client.android:assembleDebug → SUCCESS
- ast-index rebuild → 523 files / 35 modules indexed.

VERIFICATION:
- check=all_modules_compile; expected=true; actual=true
- check=4_rules_server_enforced; expected=true; actual=true
- check=EN_RU_strings_added; expected=true; actual=true
- check=READMEs_updated_operator_notes_untouched; expected=true; actual=true

UNCERTAINTY:
- missing=none; ambiguity=owner-hidden modeled as HTTP 403 (no data) over empty-state to avoid leaking "not booked" to owner. Documented in STEP_0 decision 5.
