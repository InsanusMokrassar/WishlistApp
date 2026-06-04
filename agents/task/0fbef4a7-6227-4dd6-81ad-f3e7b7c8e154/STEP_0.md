# STEP_0 — Planning

ENTITY:
entity_id=task-29-booking; type=feature; state=planned

CONTEXT:
- task_id=issue-29; agent_id=root-orchestrator; memory_ref=[features/wishlist, features/ui/wishlist]
- constraints=[mirror wishlist-items feature end-to-end; enforce 4 rules server-side; no new gradle module if avoidable; KDoc all .kt; EN+RU strings; build server+UI]

ISSUE:
- title="Making a \"book\" functionality"
- rule_1: only OTHER people see whether item is booked-or-not
- rule_2: only AUTHORIZED (logged-in) users access booking feature
- rule_3: item OWNER must NOT see booking state
- rule_4: cannot book if already booked by anybody

REFERENCE_TRACE (wishlist-items feature, layers):
- model: features/wishlist/common/commonMain/models/WishlistItem.kt (NewX/RegisteredX sealed + Id value class)
- constants: features/wishlist/common/commonMain/Constants.kt
- repo_iface: features/wishlist/common/commonMain/repo/{Read,Write,}WishlistItemRepo.kt
- repo_cache: features/wishlist/common/commonMain/repo/CacheWishlistItemRepo.kt (FullCRUDCacheRepo)
- repo_exposed: features/wishlist/common/jvmMain/repo/ExposedWishlistItemRepo.kt (AbstractExposedCRUDRepo)
- common_jvm_plugin: features/wishlist/common/jvmMain/JVMPlugin.kt (single ExposedX + singleWithBinds Cache)
- service: features/wishlist/server/commonMain/services/WishlistItemService.kt (callerId ownership)
- route: features/wishlist/server/commonMain/configurators/WishlistItemRoutingsConfigurator.kt (authenticate{} + getCallerUserIdOrAnswerUnauthorized)
- server_plugin: features/wishlist/server/commonMain/Plugin.kt (single service + singleWithRandomQualifier route element)
- client_iface: features/wishlist/client/commonMain/WishlistsItemsFeature.kt
- client_ktor: features/wishlist/client/commonMain/KtorWishlistItemFeature.kt
- client_plugin: features/wishlist/client/commonMain/Plugin.kt
- ui_model: features/ui/wishlist/commonMain/ui/WishlistsModel.kt (+impl in Plugin.kt)
- ui_vm: features/ui/wishlist/commonMain/ui/WishlistItemViewModel.kt
- ui_view: features/ui/wishlist/{jsMain,jvmMain,androidMain}/ui/WishlistItemView.kt
- strings: features/ui/wishlist/commonMain/WishlistStrings.kt

DECISIONS:
1. HOST booking inside EXISTING features/wishlist (full-stack) + features/ui/wishlist (UI), NOT a new gradle module.
   reason: booking is intrinsic to wishlist items, same as items live beside wishlists in one module → mirrors existing layering, no settings.gradle/Main.kt/config wiring churn. README pattern "wishlistItem beside wishlist" replicated by "booking beside wishlistItem".
2. PERSISTENCE: CRUD repo `BookingRepo` mirroring ExposedWishlistItemRepo (AbstractExposedCRUDRepo) with table `wishlist_item_bookings`:
   - id BIGINT PK AUTO → BookingId
   - item_id BIGINT UNIQUE INDEX → WishlistItemId (UNIQUE enforces rule_4 single active booking; concurrent 2nd insert fails on constraint)
   - user_id BIGINT → UserId (the booker)
   accessor getByItemId(itemId): RegisteredBooking? mirrors getByWishlistId.
3. MODEL: BookingId value class; sealed Booking{NewBooking(itemId,userId), RegisteredBooking(id,itemId,userId)}.
4. SERVICE BookingService enforces ALL 4 rules server-side:
   - rule_2: routes under authenticate{} → anonymous gets 401, never reaches service.
   - rule_3: getBookingState(itemId, callerId): owner→hidden. service resolves item→wishlist; if wishlist.userId==callerId return OwnerHidden (response model omits booked flag) → owner never learns booking state.
   - rule_1: non-owner authorized → returns BookingState{booked:Boolean, bookedByMe:Boolean} WITHOUT booker identity (who is hidden).
   - rule_4: book(itemId,callerId): if existing booking present → fail (Conflict). create guarded by unique index for concurrency.
   - cancel(itemId,callerId): only the booker (booking.userId==callerId) may cancel own booking.
5. WIRE-MODEL (DTO) BookingState: @Serializable data class(booked:Boolean, bookedByMe:Boolean). For owner the route returns 403/!! — choose: route returns `booked=false,bookedByMe=false` is WRONG (leaks "not booked"). Instead OWNER route returns HTTP 403 Forbidden (owner has no access to booking feature for own item) → owner client never shows booking UI. This satisfies rule_3 strictly (server hides; owner gets no state at all).
6. ROUTES under /wishlistItemBooking:
   - GET  /wishlistItemBooking/state/{itemId}  auth → BookingState | 403(owner) | 404(item) | 400
   - POST /wishlistItemBooking/book/{itemId}    auth → 200 | 403(owner) | 404 | 409(already booked) | 400
   - POST /wishlistItemBooking/cancel/{itemId}  auth → 200 | 403(owner or not-booker) | 404 | 400
7. CLIENT BookingFeature iface + KtorBookingFeature (HTTP only). Returns nullable state (null on 403/owner → UI hides booking section).
8. UI: WishlistsModel adds booking surface; WishlistItemViewModel adds bookingStateState + onBook/onCancel; WishlistItemView (js/jvm/android) renders booking section ONLY when state!=null (non-owner authorized). Owner & anonymous → state null → nothing shown (also satisfies rules in UI, defense-in-depth).
9. STRINGS: bookingLabel, bookButton, cancelBookingButton, bookedByOther, bookedByYou, notBooked (EN+RU).

RULE_ENFORCEMENT_SERVER_SIDE (summary):
- rule_2 authorized-only → ktor authenticate{} wrapper on all 3 routes (401 for anonymous).
- rule_3 owner-hidden → BookingService compares callerId to parent wishlist.userId; owner → 403 on ALL booking routes (state/book/cancel). Owner receives NO booking data.
- rule_1 visibility-without-identity → BookingState DTO carries only booked:Boolean + bookedByMe:Boolean; booker UserId never serialized to other users.
- rule_4 single-booking → DB UNIQUE index on item_id + service pre-check; concurrent 2nd insert throws → mapped to 409.

OPEN_QUESTIONS: none blocking. Owner-gets-403 chosen over owner-gets-empty-state to avoid leaking "not booked" to owner (strict reading of rule_3).

EXPECTED_RESULT:
- entity_id=task-29-booking; new_state=architecture-pending; location=STEP_1.md

VERIFICATION:
- check=plan_mirrors_reference_layers; expected=true
