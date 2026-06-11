# STEP_0 — Planning

ENTITY:
entity_id=pr31-review-fixes-round4; type=task; state=planned

CONTEXT:

* task_id=2939628b-e5e9-4203-addc-1348779f8c37; agent_id=orchestrator-planning; memory_ref=[PR#31 unresolved review threads 3364639949(+reply 3369456067), 3366908494, 3366908940, review body 2026-06-07 "MyPresents"->"MyPresentsBooks"]
* constraints=[branch=issue/29-book-functionality; follow agents/CODING.md; feature README.md updates via haiku agent; ast-index rebuild after source changes]

ACTION:

1. action=rename; target=UserWishlistsViewInteractor.onCreateWishlist; params={new_name=onCreateWishlistClick; files=[features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewInteractor.kt, client/src/commonMain/kotlin/ClientPlugin.kt:237, features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewModel.kt:314-316(call+KDoc)]}
2. action=rename; target=UserWishlistsViewInteractor.onCreateItem; params={new_name=onCreateItemClick; files=[features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewInteractor.kt, client/src/commonMain/kotlin/ClientPlugin.kt:242, features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewModel.kt:320-325(call+KDoc)]}
3. action=rename; target=identifier-family "MyPresents"/"myPresents"; params={new_names="MyPresentsBooks"/"myPresentsBooks"; scope=[features/ui/booking (MyPresentsViewConfig/ViewModel/ViewInteractor/View x3 platforms + files + Plugin.kt + BookingStrings.myPresentsTitle + BookingModel.myPresents), features/booking/client (BookingFeature.myPresents, KtorBookingFeature.myPresents), features/booking/common (Constants.bookingMyPresentsPathPart name+value), features/booking/server (BookingService.myPresents, BookingRoutingsConfigurator route+KDoc), client/ClientPlugin.kt (imports + MyPresentsViewInteractor single)]}
4. action=create; target=features/auth/client "me" StateFlow infrastructure; params={qualifier=named("me"); extensions=[Koin.me, Scope.me] returning StateFlow<RegisteredUser?>; setupDI: single MutableRedeliverStateFlow<RegisteredUser?>(null) under qualifier + single<StateFlow<RegisteredUser?>> under same qualifier delegating; startPlugin: subscribe AuthCredentialsStorage.userAuthorised via subscribeLoggingDropExceptions(scope) -> authorised==true => feature.getMe() into mutable flow, authorised==false => null}
5. action=replace; target=raw ClientAuthFeature.getMe() in UI models; params={sites=[features/ui/wishlist/src/commonMain/kotlin/Plugin.kt:142-152 (getCurrentUserId, isOwner), features/ui/users/src/commonMain/kotlin/Plugin.kt:62-65 (getCurrentUserId, isRoot-check)]; replacement=me-flow .value; excluded_site=features/ui/auth/Plugin.kt:46 (login-gate source-of-truth check, must stay raw request)}
6. action=compile; target=gradle; params={tasks=affected module builds}
7. action=update; target=feature README.md files [features/booking, features/ui/booking, features/auth, features/ui/wishlist, features/ui/users]; params={executor=haiku agent}
8. action=rebuild; target=ast-index

REASON:

* condition=PR#31 unresolved review threads; requirement=operator review comments must be applied verbatim

UNCERTAINTY:

* missing=none-blocking; ambiguity=review-comment "onCreateWishlistClick" anchored to interactor overrides in ClientPlugin.kt:237/242 → rename applied to UserWishlistsViewInteractor methods only; sibling WishlistsListViewInteractor.onCreateWishlist (ClientPlugin.kt:186) not commented → unchanged
* ambiguity=MyPresentsBooks rename scope → applied to all MyPresents/myPresents identifiers incl. path-part constant value (pre-merge branch, URL not shipped, server+client share constant => consistent)

EXPECTED RESULT:

* entity_id=pr31-review-fixes-round4; new_state=planned; location=agents/task/2939628b-e5e9-4203-addc-1348779f8c37/STEP_0.md

VERIFICATION:

* check=all 4 review items mapped to concrete file actions; expected=true

REPETITION OF RESULT:

* entity_id=pr31-review-fixes-round4; stored_in=agents/task/2939628b-e5e9-4203-addc-1348779f8c37/STEP_0.md; status=available
