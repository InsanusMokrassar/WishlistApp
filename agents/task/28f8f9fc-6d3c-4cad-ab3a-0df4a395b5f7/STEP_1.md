# STEP_1 — ARCHITECTURE

ENTITY:
entity_id=task-issue-30; type=feature; state=architecture-ready
prev_step=STEP_0(planning); placement=inside features/wishlist + features/ui/wishlist (no new top-level module)

## LAYER-MAP (files to create/change). path=absolute-omitted, repo-relative.

### COMMON (features/wishlist/common)
A1 NEW models/CopyConstants → extend EXISTING Constants.kt (append path parts)
A2 NEW models/WishlistCopyJob.kt:
  - WishlistCopyJobId @JvmInline value class(val long: Long)
  - enum WishlistCopyJobStatus { Pending, InProgress, Done, Failed }
  - sealed interface WishlistCopyJob { sourceWishlistId: WishlistId; recipientUserId: UserId; status: WishlistCopyJobStatus }
  - data class NewWishlistCopyJob(sourceWishlistId, recipientUserId, status=Pending) : WishlistCopyJob
  - data class RegisteredWishlistCopyJob(id, sourceWishlistId, recipientUserId, status) : WishlistCopyJob
A3 NEW repo/WishlistCopyJobRepo.kt (+Read/Write split) : CRUDRepo<RegisteredWishlistCopyJob, WishlistCopyJobId, NewWishlistCopyJob>
  - extra: suspend fun getUnfinished(): List<RegisteredWishlistCopyJob>  (status in Pending,InProgress)
A4 NEW repo/CacheWishlistCopyJobRepo.kt : FullCRUDCacheRepo wrapper (mirror CacheWishlistItemRepo); getUnfinished delegates originalRepo
A5 NEW jvmMain repo/ExposedWishlistCopyJobRepo.kt : AbstractExposedCRUDRepo(tableName="wishlist_copy_jobs")
  cols: id BIGINT PK AUTO; source_wishlist_id BIGINT; recipient_user_id BIGINT; status TEXT(enum name)
  getUnfinished = selectAll where status in (Pending,InProgress)
A6 CHANGE jvmMain/JVMPlugin.kt (wishlist.common): register ExposedWishlistCopyJobRepo + Cache bind WishlistCopyJobRepo (mirror existing pattern)

### COMMON shared deep-copy + identity helpers (features/wishlist/common/commonMain)
A7 NEW models/WishlistItemCopy.kt:
  - fun RegisteredWishlistItem.toNewItem(targetWishlistId: WishlistId): NewWishlistItem  (deep-copy fields)
  - fun RegisteredWishlistItem.identityMatches(other: WishlistItem): Boolean  (title+description+priceUnits+approximatePrice+amount+priority+links+imageIds)
  used by both item-copy and wishlist-copy idempotency (DEC_6).

### SERVER (features/wishlist/server)
B1 CHANGE Constants.kt(common) path parts: wishlistItemCopyPathPart="copy"; wishlistCopyPathPart="copy" (under existing prefixes) + jobs query optional. Concretely:
   - item copy route: POST /wishlistItem/copy  body=CopyItemRequest(sourceItemId, sourceWishlistId, targetWishlistId)
   - wishlist copy enqueue: POST /wishlist/copy  body=CopyWishlistRequest(sourceWishlistId)
B2 NEW common/models DTOs (place in features/wishlist/common/commonMain/models): 
   - CopyItemRequest(sourceItemId: WishlistItemId, sourceWishlistId: WishlistId, targetWishlistId: WishlistId)
   - CopyWishlistRequest(sourceWishlistId: WishlistId)
B3 CHANGE services/WishlistItemService.kt: add suspend fun copyItem(req: CopyItemRequest, callerId: UserId): RegisteredWishlistItem?
   logic: target = wishlistRepo.getById(req.targetWishlistId) ?: return null; if target.userId != callerId return null(→403/500);
          source = wishlistItemRepo.getById(req.sourceItemId) ?: return null; (source.wishlistId==req.sourceWishlistId sanity);
          dup-check: getByWishlistId(target.id).any{ it.identityMatches(source.toNewItem(target.id)) } → if dup return existing dup; else create source.toNewItem(target.id).
B4 NEW services/WishlistCopyService.kt (commonMain): the QUEUE WORKER.
   ctor(copyJobRepo: WishlistCopyJobRepo, wishlistRepo, wishlistItemRepo, scope: CoroutineScope)
   - suspend fun enqueue(sourceWishlistId, recipientUserId): RegisteredWishlistCopyJob  → repo.create(NewWishlistCopyJob(...Pending)); emit id to channel; return job
   - fun start(): launches worker in scope: 
       (a) on start: repo.getUnfinished().forEach{ submit(it.id) }  (RESUME after reload)
       (b) collect channel of job ids; per id launch child coroutine guarded by Semaphore(permits=4) → processJob(id) (PARALLEL)
   - private suspend fun processJob(id):
       job=repo.getById(id) ?: return; set status=InProgress (repo.update)
       source=wishlistRepo.getById(job.sourceWishlistId) ?: { status=Failed; return }
       // idempotency_check_1 wishlist-by-name:
       target = wishlistRepo.getByUserId(job.recipientUserId).find{ it.title==source.title }
                ?: wishlistRepo.create(NewWishlist(job.recipientUserId, source.title, source.defaultPriceUnits)).first()
       // idempotency_check_2 per-item existence:
       existing = wishlistItemRepo.getByWishlistId(target.id)
       wishlistItemRepo.getByWishlistId(source.id).forEach{ srcItem ->
           val newItem = srcItem.toNewItem(target.id)
           if (existing.none{ it.identityMatches(newItem) }) wishlistItemRepo.create(newItem)
       }
       status=Done (repo.update)
   note: channel = kotlinx.coroutines.channels.Channel(UNLIMITED) inside service; concurrency via launch+Semaphore in scope.
B5 NEW configurators routes: extend EXISTING WishlistItemRoutingsConfigurator (add authenticate{} POST copy) + WishlistRoutingsConfigurator (add authenticate{} POST copy → enqueue).
   item copy: result null→500 (parent/target not found or not owner), else respond created item.
   wishlist copy: callerId from token; service.enqueue(req.sourceWishlistId, callerId); respond 202 Accepted (job queued).
B6 CHANGE server/Plugin.kt(common): register WishlistCopyService single (gets copyJobRepo, wishlistRepo, wishlistItemRepo, scope). NOTE repos are JVM-only (provided by wishlist common JVMPlugin) → WishlistCopyService construction needs them in DI. Register single{WishlistCopyService(get(),get(),get(),get())}. start() called in JVMPlugin.startPlugin (NOT common Plugin.startPlugin) because repos only exist on JVM. Routing configurators stay in common Plugin (they just inject service + item service).
   REVISION: WishlistCopyService injected types (WishlistCopyJobRepo, WishlistRepo, WishlistItemRepo) are interfaces declared in common; their impls registered on JVM. single{} resolves lazily → safe to declare single in common Plugin; only resolved when JVM impls present. start() invoked from server JVMPlugin.startPlugin after common JVMPlugin started.
B7 CHANGE server/jvmMain/JVMPlugin.kt: after delegating startPlugin, call koin.get<WishlistCopyService>().start().

### CLIENT (features/wishlist/client)
C1 CHANGE WishlistsItemsFeature.kt: add suspend fun copy(req: CopyItemRequest): RegisteredWishlistItem?
C2 CHANGE KtorWishlistItemFeature.kt: POST /wishlistItem/copy setBody(req); success→body else null
C3 NEW WishlistCopyFeature.kt (client interface): suspend fun enqueueCopy(req: CopyWishlistRequest): Boolean
C4 NEW KtorWishlistCopyFeature.kt: POST /wishlist/copy setBody(req); return status.isSuccess()
C5 CHANGE client/Plugin.kt: register KtorWishlistCopyFeature + bind WishlistCopyFeature

### UI (features/ui/wishlist)
D1 CHANGE WishlistsModel.kt: add
   - suspend fun copyItemToWishlist(sourceItemId, sourceWishlistId, targetWishlistId): RegisteredWishlistItem?
   - suspend fun enqueueWishlistCopy(sourceWishlistId): Boolean
   (getMyWishlists already exists for picker; getCurrentUserId already exists for auth-gate)
D2 CHANGE ui/wishlist/Plugin.kt: implement new model methods (delegate to itemsFeature.copy + new copyFeature.enqueueCopy); inject WishlistCopyFeature in model anonymous impl. Register WishlistItemCopyViewModel factory + ViewConfig polymorphic serializer.
D3 NEW ui/WishlistItemCopyViewConfig.kt: data class(sourceItemId: WishlistItemId, sourceWishlistId: WishlistId) : ViewConfig  (target picker screen)
D4 NEW ui/WishlistItemCopyViewInteractor.kt: onBack(node); onCopied(node) (both pop)
D5 NEW ui/WishlistItemCopyViewModel.kt: loads model.getMyWishlists() → targetsState; onSelectTarget(targetWishlistId){ copyItemToWishlist(...); interactor.onCopied(node) }; loadingState; onBack()
D6 CHANGE WishlistItemViewModel.kt: add canCopyState (authorized && !owner) derived combine(_currentUserIdState,_wishlistState→isOwner); onCopyItem()→interactor.onCopyItem(node). Add interactor method.
D7 CHANGE WishlistItemViewInteractor.kt: add suspend fun onCopyItem(node): push WishlistItemCopyViewConfig(node.config.wishlistItemId, node.config.wishlistId)
D8 CHANGE WishlistViewModel.kt: add canCopyState (authorized && !owner); onCopyWishlist(){ model.enqueueWishlistCopy(config.wishlistId); set copyRequestedState=true } ; expose copyRequestedState for UI feedback.
   NOTE: WishlistViewModel.isOwnerState exists; canCopy = currentUserId!=null && !isOwner && wishlist!=null. currentUserId already loaded in loadWishlist.
D9 NEW Views WishlistItemCopyView (js/jvm/android) : list caller wishlists as selectable rows → onSelectTarget; back button. JS Bootstrap list-group; JVM Material2; Android Material3. Mirror WishlistsListView structure.
D10 CHANGE WishlistItemView (js/jvm/android): add "Copy" button (shown when canCopy) next to/instead-of Edit (Edit only owner; Copy only non-owner-authorized).
D11 CHANGE WishlistView (js/jvm/android): add "Copy wishlist" button when canCopy; show transient confirmation text when copyRequested.
D12 CHANGE WishlistStrings.kt: add EN+RU keys: copyButton, copyItemButton, copyWishlistButton, copyTargetTitle, copySelectTargetPrompt, copyQueued, copyFailed, copyNoTargets.
D13 CHANGE platform plugins (js/jvm/android JSPlugin/JVMPlugin/AndroidPlugin): register NavigationNodeFactory for WishlistItemCopyView.
D14 CHANGE client/ClientPlugin.kt: register single<WishlistItemCopyViewInteractor>{ pop on back/copied }; add WishlistItemViewInteractor.onCopyItem impl (push WishlistItemCopyViewConfig).

## AUTH-ENFORCEMENT (server-side)
- item copy route inside authenticate{}; callerId=getCallerUserIdOrAnswerUnauthorized(); WishlistItemService.copyItem verifies target.userId==callerId.
- wishlist copy route inside authenticate{}; recipient ALWAYS=callerId (never client-supplied) → user can only copy INTO own profile.
- source reads use existing public GET (any wishlist/item readable) — copying FROM others allowed per issue.

## QUEUE PERSIST/RESUME/PARALLEL (summary)
- persist: wishlist_copy_jobs Exposed table (status column).
- resume: WishlistCopyService.start() scans getUnfinished() on startup → re-submits → processJob idempotent.
- parallel: Channel-fed worker launches one child coroutine per job under Semaphore(4) in shared CoroutineScope.
- idempotent: re-run safe via wishlist-by-name reuse + per-item identityMatches skip.

## RISKS / NOTES
- WishlistCopyService.start() must be idempotent if called once; called from server JVMPlugin.startPlugin only.
- Channel inside service shared across enqueue+worker; service is single → ok.
- imageIds: deep-copy references same FileId (record deep-copy; blob share). consistent w/ issue.
- DTOs need @Serializable + polymorphic registration NOT required (plain bodies via ContentNegotiation, like NewWishlistItem). Just @Serializable.

VERIFICATION:
- check=mirrors-existing-layering; expected=true (Exposed repo=ExposedWishlistItemRepo pattern; routes=configurator; client=Ktor feature; UI=MVVM)
- check=builds; expected=verified-in-STEP_2
EXPECTED-RESULT: entity_id=task-issue-30; new_state=coding-ready; location=STEP_2.md
