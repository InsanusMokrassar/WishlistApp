# STEP_2 — CODING

ENTITY:
entity_id=task-issue-30; type=feature; state=implemented+compiled
prev_steps=[STEP_0 planning, STEP_1 architecture]

## FILES-CREATED (path; layer)
- features/wishlist/common/src/commonMain/kotlin/models/WishlistCopyJob.kt; model (queue job + status enum)
- features/wishlist/common/src/commonMain/kotlin/models/CopyRequests.kt; DTO (CopyItemRequest, CopyWishlistRequest)
- features/wishlist/common/src/commonMain/kotlin/models/WishlistItemCopy.kt; helpers (toNewItem deep-copy, hasSameContentAs identity)
- features/wishlist/common/src/commonMain/kotlin/repo/WishlistCopyJobRepo.kt; repo interfaces (Read/Write/combined + getUnfinished)
- features/wishlist/common/src/commonMain/kotlin/repo/CacheWishlistCopyJobRepo.kt; cache repo
- features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistCopyJobRepo.kt; DB-queue (table wishlist_copy_jobs)
- features/wishlist/server/src/commonMain/kotlin/services/WishlistCopyService.kt; worker-service (Channel+Semaphore, resume on start)
- features/wishlist/client/src/commonMain/kotlin/WishlistCopyFeature.kt; client-feature interface
- features/wishlist/client/src/commonMain/kotlin/KtorWishlistCopyFeature.kt; client-feature Ktor impl
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemCopyViewConfig.kt; UI config
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemCopyViewInteractor.kt; UI interactor
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemCopyViewModel.kt; viewmodel (target picker)
- features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemCopyView.kt; UI view JS (Bootstrap list-group)
- features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemCopyView.kt; UI view JVM (Material2)
- features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemCopyView.kt; UI view Android (Material3)

## FILES-CHANGED (path; change)
- features/wishlist/common/src/commonMain/kotlin/Constants.kt; +wishlistItemCopyPathPart, +wishlistCopyPathPart
- features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; +ExposedWishlistCopyJobRepo single + CacheWishlistCopyJobRepo bind WishlistCopyJobRepo
- features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt; +copyItem(request, callerId) idempotent deep-copy
- features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt; +POST copy route (authenticate{})
- features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; +ctor WishlistCopyService, +POST copy enqueue route (202)
- features/wishlist/server/src/commonMain/kotlin/Plugin.kt; +single WishlistCopyService, configurator gets 2 args
- features/wishlist/server/src/jvmMain/kotlin/JVMPlugin.kt; +koin.get<WishlistCopyService>().start() in startPlugin
- features/wishlist/client/src/commonMain/kotlin/WishlistsItemsFeature.kt; +copy(request)
- features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt; +copy POST impl
- features/wishlist/client/src/commonMain/kotlin/Plugin.kt; +KtorWishlistCopyFeature + WishlistCopyFeature bind
- features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt; +7 EN+RU keys (copy*)
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt; +copyItemToWishlist, +enqueueWishlistCopy
- features/ui/wishlist/src/commonMain/kotlin/Plugin.kt; +inject WishlistCopyFeature, +impl 2 methods, +factory WishlistItemCopyViewModel, +polymorphic serializer
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewInteractor.kt; +onCopyItem
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt; +canCopyState, +onCopyItem
- features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt; +canCopyState, +copyRequestedState, +copyFailedState, +onCopyWishlist
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt; +Copy button (canCopy)
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistView.kt; +Copy button + status msg
- features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/{JSPlugin,JVMPlugin,AndroidPlugin}.kt; +WishlistItemCopyView node factory
- client/src/commonMain/kotlin/ClientPlugin.kt; +WishlistItemViewInteractor.onCopyItem impl, +WishlistItemCopyViewInteractor single
- features/wishlist/README.md; routes+models+architecture notes (copy + queue)
- features/ui/wishlist/README.md; screens+models+architecture notes (copy actions)

## BUILD/VERIFY
- :wishlist.features.wishlist.common:compileKotlinJvm = SUCCESS
- :wishlist.features.wishlist.server:compileKotlinJvm = SUCCESS
- :wishlist.features.wishlist.client:compileKotlinJvm = SUCCESS
- :wishlist.features.ui.wishlist:compileKotlinJvm + compileKotlinJs + compileDebugKotlinAndroid = SUCCESS
- :wishlist.client:compileKotlinJvm + compileKotlinJs = SUCCESS
- :wishlist.client.android:compileDebugKotlin (android app) = SUCCESS
- :wishlist.server:build = SUCCESS
- ast-index rebuild (post-change) = exit 0

## AUTH (server-side)
- item copy: route in authenticate{}; callerId=getCallerUserIdOrAnswerUnauthorized(); WishlistItemService.copyItem requires target.userId==callerId else null→500.
- wishlist copy: route in authenticate{}; recipient ALWAYS=callerId (never client body); enqueue→202.

## QUEUE (persist/resume/parallel)
- persist: ExposedWishlistCopyJobRepo table wishlist_copy_jobs (status TEXT).
- resume: WishlistCopyService.start() (called wishlist.server.JVMPlugin.startPlugin) scans getUnfinished()→re-submit.
- parallel: Channel(UNLIMITED)+per-job launch under Semaphore(4) in shared CoroutineScope.

## IDEMPOTENCY
- check_1 wishlist-by-name: getByUserId(recipient).find{title==source.title} ?: create.
- check_2 item-existence: per source item, existingTargetItems.none{ hasSameContentAs(newItem) } → create only when absent.

VERIFICATION:
- check=all-modules-compile; expected=true; actual=true
- check=auth-server-enforced; expected=true; actual=true
- check=queue-persist-resume-parallel; expected=true; actual=true
EXPECTED-RESULT: entity_id=task-issue-30; new_state=done; location=working-tree (no commit per instructions)
