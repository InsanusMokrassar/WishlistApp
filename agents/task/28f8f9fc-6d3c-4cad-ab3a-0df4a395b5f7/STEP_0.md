# STEP_0 — PLANNING

ENTITY:
entity_id=task-issue-30; type=feature; state=planned
issue=#30; title="Add opportunity to copy wishlists and wishlists items to user profile"
branch=issue/30-copy-to-profile; uuid=28f8f9fc-6d3c-4cad-ab3a-0df4a395b5f7

CONTEXT:
- constraints=[mirror existing wishlist layering; server-side auth enforce; queue persists+resumes+parallel; idempotent wishlist-by-name + item-existence; localized EN+RU; JS Bootstrap; build all modules]
- ref_features=[features/wishlist (full-stack), features/ui/wishlist (UI MVVM), features/auth (bearer auth + getCallerUserIdOrAnswerUnauthorized), features/users]

## SCOPE-DEFINITION (two user capabilities)

CAP_1=COPY_ITEM:
- actor=authorized_user; source=RegisteredWishlistItem(other user); target=caller-owned RegisteredWishlist (user-selected)
- server creates NEW RegisteredWishlistItem deep-copy of fields into target wishlist; target.userId MUST == callerId (server-enforced)
- synchronous (single HTTP call returns created item); NO queue needed for item copy
- UI: "Copy" action on WishlistItemView + target-wishlist picker (caller own wishlists list)

CAP_2=COPY_WISHLIST:
- actor=authorized_user; source=RegisteredWishlist(other user) + its items
- deep-copy: new wishlist(owner=caller) + all items copied as new items
- MUST go through SERVER-SIDE persistent queue/service (survives reload, parallel processing)
- idempotency_check_1: wishlist-by-name → recipient already has wishlist same title → reuse, else create
- idempotency_check_2: per source item → item already exists in recipient wishlist (by identifying fields) → skip, else copy
- UI: "Copy" action on WishlistView (non-owner) → enqueues job → returns immediately

## DESIGN-DECISIONS

DEC_1: item-copy = synchronous server route (no queue). Matches issue ("when user copying wishlist item - he must select which wishlist"). Reuse WishlistItemService deep-copy semantics.

DEC_2: wishlist-copy = persistent Exposed-backed job queue + background worker service draining concurrently. Worker launched in startPlugin via injected CoroutineScope (from features.common.common.Plugin single<CoroutineScope>).

DEC_3: queue persistence = new Exposed CRUD repo `ExposedWishlistCopyJobRepo` mirroring ExposedWishlistItemRepo pattern. Table `wishlist_copy_jobs`. Job model: source wishlistId + recipient userId + status. On startup worker re-scans PENDING/IN_PROGRESS jobs → resumes.

DEC_4: parallel drain = worker collects job-added flow + on-start scan; each job processed in its own child coroutine (launch per job) bounded by a Semaphore (concurrency limit). Idempotent body makes re-run after crash safe.

DEC_5: idempotency_check_1 (wishlist-by-name): recipient.getByUserId(callerId).find { it.title == source.title } ?: create. Need title match.

DEC_6: idempotency_check_2 (item-existence): recipient items getByWishlistId(targetWishlistId); skip source item if existing item matches identifying fields (title + description + priceUnits + approximatePrice + amount + priority + links + imageIds). Define ItemIdentity equality helper.

DEC_7: deep-copy field mapping: NewWishlistItem(wishlistId=target, copy all value fields from source RegisteredWishlistItem). imageIds reused by id (files feature shared store; copying file blobs out-of-scope; reference same FileId — acceptable deep-copy of item record).

DEC_8: auth: item-copy route under authenticate{}; callerId=getCallerUserIdOrAnswerUnauthorized(); target wishlist ownership verified (target.userId==callerId else 403/null). wishlist-copy enqueue route under authenticate{}; recipient=callerId (always caller). Source read is public (existing public GET).

DEC_9: place new full-stack surface INSIDE existing features/wishlist module (common+server+client) as new package `copy` — NOT a new top-level feature. Rationale: tightly coupled to wishlist repos/services; mirrors "cross-cutting sub-package" guidance + avoids settings.gradle/Main.kt/config churn. New constants added to existing Constants.kt. New routing configurators registered in existing wishlist server Plugin.kt. New worker started in existing wishlist server Plugin.startPlugin (commonMain) — repo provided by wishlist common JVMPlugin.

DEC_10: UI added INSIDE existing features/ui/wishlist:
- COPY_ITEM: new screen `WishlistItemCopyTargetView*` (target-wishlist picker) OR reuse a modal. Decision=new picker ViewConfig `WishlistItemCopyViewConfig(sourceItemId, sourceWishlistId)` pushed from WishlistItemView "Copy" button (shown to authorized non-owner). Picker lists caller own wishlists; selecting one triggers copy → pop on success.
- COPY_WISHLIST: "Copy" button on WishlistView (shown to authorized non-owner) → model.enqueueWishlistCopy(sourceWishlistId) → toast/snackbar minimal → no navigation.
- WishlistsModel extended: copyItemToWishlist(sourceItemId, sourceWishlistId, targetWishlistId), getMyWishlists() already exists, enqueueWishlistCopy(sourceWishlistId), getCurrentUserId() exists (auth gating).

## PROBLEMS/QUESTIONS
- missing=none-blocking. ambiguity: "item already exists by identifying fields" → resolved via DEC_6 full-field identity (conservative; avoids dup). 
- ambiguity: imageIds deep-copy of underlying file blobs → resolved DEC_7 (reference same FileId; record-level deep copy). NOT blocking; consistent with issue (copy item fields).
- ambiguity: queue concurrency limit value → choose 4 (constant). Non-blocking.
- NO operator-only decision required → PROCEED to ARCHITECTURE.

## EXPECTED-RESULT
entity_id=task-issue-30; new_state=architecture-ready; location=agents/task/<uuid>/STEP_1.md (next)

VERIFICATION:
- check=all-layers-identified; expected=true → model/DTO + queue-repo(Exposed) + worker-service + routes + client-feature + viewmodel + UI + strings
- check=auth-server-enforced; expected=true (authenticate{} + caller ownership)
- check=queue-persist-resume-parallel; expected=true (Exposed table + startup scan + per-job launch+Semaphore)
