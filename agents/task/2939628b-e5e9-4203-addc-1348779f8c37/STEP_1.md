# STEP_1 — Architecturing

ENTITY:
entity_id=pr31-review-fixes-round4; type=task; state=architectured

CONTEXT:

* task_id=2939628b-e5e9-4203-addc-1348779f8c37; agent_id=orchestrator-architecture; memory_ref=[STEP_0.md]
* constraints=[Operator Notes of features/auth, features/booking, features/ui/booking, features/ui/wishlist, features/ui/users = empty → no operator constraints; Koin-qualifier rule agents/CODING.md ("extract qualifier into val"); KDoc rule agents/CODING.md]

ACTION:

1. action=design; target=features/auth/client "me" infrastructure; params={
   new_file=features/auth/client/src/commonMain/kotlin/Me.kt;
   content=[
     val meQualifier: StringQualifier = named("me"),
     val Koin.me: StateFlow<RegisteredUser?> get() = get(qualifier = meQualifier),
     val Scope.me: StateFlow<RegisteredUser?> get() = get(qualifier = meQualifier)
   ];
   rationale=qualifier defined once (CODING.md qualifier rule); Scope extension covers usage inside Module.single{} definitions, Koin extension covers startPlugin/etc.}
2. action=design; target=features/auth/client/Plugin.kt; params={
   setupDI+=[single(qualifier=meQualifier){ MutableRedeliverStateFlow<RegisteredUser?>(null) }, single<StateFlow<RegisteredUser?>>(qualifier=meQualifier){ get<MutableRedeliverStateFlow<RegisteredUser?>>(qualifier=meQualifier) }];
   startPlugin+=[koin.get<AuthCredentialsStorage>().userAuthorised.subscribeLoggingDropExceptions(koin.get<CoroutineScope>()){ authorised -> mutableMe.value = if (authorised) koin.get<ClientAuthFeature>().getMe() else null }];
   rationale=MutableRedeliverStateFlow implements MutableStateFlow → koin resolves MutableRedeliverStateFlow::class vs StateFlow::class separately under same qualifier; StateFlow emits current value on collect → initial population at startup; AuthFeatureService.getMe() already returns null when storage.userAuthorised.value==false}
3. action=design; target=consumers; params={
   features/ui/wishlist/Plugin.kt: WishlistsModel single resolves val meState = me (Scope ext); getCurrentUserId() = meState.value?.id; isOwner unchanged logic atop getCurrentUserId; drop authFeature dependency if unused after change;
   features/ui/users/Plugin.kt: UsersModel single resolves val meState = me; getCurrentUserId() = meState.value?.id; root-check = meState.value?.username?.string == "root";
   features/ui/auth/Plugin.kt:46 unchanged (login-gate must query server, not cache)}
4. action=design; target=rename onCreateWishlist/onCreateItem; params={mechanical; UserWishlistsViewInteractor declarations + ClientPlugin overrides + UserWishlistsViewModel delegation bodies and KDoc links; ViewModel public fun names unchanged (views bind to ViewModel, review comment anchored to interactor overrides only)}
5. action=design; target=rename MyPresents→MyPresentsBooks; params={mechanical identifier+file rename; git mv for MyPresentsView.kt x3, MyPresentsViewConfig.kt, MyPresentsViewInteractor.kt, MyPresentsViewModel.kt; myPresents()→myPresentsBooks() through BookingModel/BookingFeature/KtorBookingFeature/BookingService; Constants.bookingMyPresentsPathPart→bookingMyPresentsBooksPathPart value "myPresents"→"myPresentsBooks"; BookingStrings.myPresentsTitle→myPresentsBooksTitle (display text unchanged); KDoc mentions updated}

REASON:

* condition=review reply 3369456067 prescribes exact architecture; requirement=follow operator prescription verbatim

EXPECTED RESULT:

* entity_id=pr31-review-fixes-round4; new_state=architectured; location=agents/task/2939628b-e5e9-4203-addc-1348779f8c37/STEP_1.md

VERIFICATION:

* check=design conforms to agents/CODING.md plugin/DI/KDoc/qualifier rules; expected=true
* check=no Operator Notes violated; expected=true (all empty)

UNCERTAINTY:

* missing=none; ambiguity=none

REPETITION OF RESULT:

* entity_id=pr31-review-fixes-round4; stored_in=agents/task/2939628b-e5e9-4203-addc-1348779f8c37/STEP_1.md; status=available
