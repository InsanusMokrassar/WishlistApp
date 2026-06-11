# STEP_1 — Architecture

ENTITY:
entity_id=task-29-booking; type=feature; state=architectured

CONTEXT:
- task_id=issue-29; agent_id=root-orchestrator
- host_feature=features/wishlist (full-stack) + features/ui/wishlist (UI); no new gradle module.

## Layer files (CREATE unless marked EDIT)

### common (features/wishlist/common)
- commonMain/models/Booking.kt — `BookingId(Long)` value class; sealed `Booking{ itemId }`; `NewBooking(itemId,userId)`; `RegisteredBooking(id,itemId,userId)`.
- commonMain/models/BookingState.kt — @Serializable `BookingState(booked:Boolean, bookedByMe:Boolean)` wire DTO (NO booker identity → rule_1).
- commonMain/repo/ReadBookingRepo.kt — `ReadCRUDRepo<RegisteredBooking,BookingId>` + `suspend getByItemId(itemId):RegisteredBooking?`.
- commonMain/repo/WriteBookingRepo.kt — `WriteCRUDRepo<RegisteredBooking,BookingId,NewBooking>`.
- commonMain/repo/BookingRepo.kt — `ReadBookingRepo, WriteBookingRepo, CRUDRepo<...>`.
- commonMain/repo/CacheBookingRepo.kt — FullCRUDCacheRepo wrapper; getByItemId delegates to originalRepo.
- jvmMain/repo/ExposedBookingRepo.kt — AbstractExposedCRUDRepo(tableName="wishlist_item_bookings"); columns id(PK auto), item_id(long, uniqueIndex → rule_4), user_id(long). getByItemId via transaction selectAll where item_id eq.
- EDIT commonMain/Constants.kt — add bookingPrefix/state/book/cancel path parts.
- EDIT jvmMain/JVMPlugin.kt — register `single { ExposedBookingRepo(get()) }` + `singleWithBinds<BookingRepo>{ CacheBookingRepo(get<ExposedBookingRepo>(), get()) }`.

### server (features/wishlist/server)
- commonMain/services/BookingService.kt — ctor(bookingRepo:BookingRepo, wishlistItemRepo:WishlistItemRepo, wishlistRepo:WishlistRepo).
  - private suspend ownerOf(itemId):Pair<RegisteredWishlistItem?,UserId?> resolves item→wishlist→ownerId.
  - getState(itemId, callerId): sealed result {ItemNotFound, OwnerForbidden, State(BookingState)}.
      rule_3 owner: if ownerId==callerId → OwnerForbidden.
      else booking=getByItemId → State(BookingState(booked=booking!=null, bookedByMe=booking?.userId==callerId)).
  - book(itemId, callerId): result {ItemNotFound, OwnerForbidden, AlreadyBooked, Ok}.
      owner→OwnerForbidden; existing booking→AlreadyBooked; else create NewBooking; catch insert exception(unique)→AlreadyBooked.
  - cancel(itemId, callerId): result {ItemNotFound, OwnerForbidden, NotBooker, Ok}.
      owner→OwnerForbidden; booking==null→Ok(idempotent? choose ItemBooking semantics: booking==null→NotBooker? ) → DECISION: booking==null → Ok (nothing to cancel, idempotent, no leak). booking.userId!=callerId→NotBooker; else deleteById→Ok.
- commonMain/configurators/BookingRoutingsConfigurator.kt — ctor(bookingService). ALL routes inside `authenticate { route(bookingPrefix){...} }` (rule_2). getCallerUserIdOrAnswerUnauthorized.
  - GET state/{itemId}: 400 bad id; map result: ItemNotFound→404, OwnerForbidden→403, State→respond(BookingState).
  - POST book/{itemId}: ItemNotFound→404, OwnerForbidden→403, AlreadyBooked→409 Conflict, Ok→200.
  - POST cancel/{itemId}: ItemNotFound→404, OwnerForbidden→403, NotBooker→403, Ok→200.
- EDIT commonMain/Plugin.kt — `single { BookingService(get(),get(),get()) }` + `singleWithRandomQualifier<ApplicationRoutingConfigurator.Element>{ BookingRoutingsConfigurator(get()) }`.

### client (features/wishlist/client)
- commonMain/BookingFeature.kt — iface: `suspend getState(itemId):BookingState?` (null=owner/forbidden/anon), `suspend book(itemId):Boolean`, `suspend cancel(itemId):Boolean`.
- commonMain/KtorBookingFeature.kt — HTTP-only impl. getState: GET, if success body else null. book/cancel: POST, return isSuccess.
- EDIT commonMain/Plugin.kt — `single { KtorBookingFeature(get()) }` + `single<BookingFeature>{ get<KtorBookingFeature>() }`.

### ui (features/ui/wishlist)
- EDIT commonMain/ui/WishlistsModel.kt — add: `suspend getBookingState(itemId):BookingState?`, `suspend bookItem(itemId):Boolean`, `suspend cancelBooking(itemId):Boolean`.
- EDIT commonMain/Plugin.kt — model impl delegates to injected BookingFeature; add `get<BookingFeature>()`.
- EDIT commonMain/ui/WishlistItemViewModel.kt — add `_bookingState:MutableRedeliverStateFlow<BookingState?>(null)` + public bookingState; `onBook()`, `onCancelBooking()` (loadingState guard; reload state after). Load booking state in the resume init block AFTER item resolved (only when item!=null). Owner/anon → getBookingState returns null → section hidden.
- EDIT {jsMain,jvmMain,androidMain}/ui/WishlistItemView.kt — render booking section only when bookingState != null:
    booked && bookedByMe → "Booked by you" text + Cancel button.
    booked && !bookedByMe → "Booked by someone" text (no who) + no book button.
    !booked → "Not booked" + Book button.
- EDIT commonMain/WishlistStrings.kt — bookingLabel, bookButton, cancelBookingButton, bookedByOther, bookedByYou, notBooked (EN+RU).

## RULE→ENFORCEMENT MATRIX (server-side)
- rule_1 (others see booked-or-not, not WHO): BookingState DTO = {booked,bookedByMe} only; UserId of booker never serialized.
- rule_2 (authorized only): all booking routes inside ktor `authenticate{}`; anonymous→401; getCallerUserIdOrAnswerUnauthorized.
- rule_3 (owner hidden): BookingService compares callerId vs parent wishlist.userId; owner→OwnerForbidden→HTTP 403 on state/book/cancel; owner receives no booking data at all.
- rule_4 (single booking): ExposedBookingRepo item_id UNIQUE index + service pre-check; concurrent insert→constraint violation caught→AlreadyBooked→409.

## NOTES
- README updates: features/wishlist/README.md (routes+models+arch), features/ui/wishlist/README.md (booking UI). Must NOT touch Operator Notes.
- Build targets: `:wishlist.features.wishlist.server:build` (pulls common+client jvm) and `:wishlist.features.ui.wishlist:build`; if time, broader `:server:build` and `:client:build`.
- ast-index rebuild after source changes.

EXPECTED_RESULT:
- entity_id=task-29-booking; new_state=coding-pending; location=STEP_2.md
VERIFICATION:
- check=all_4_rules_have_server_enforcement_point; expected=true
