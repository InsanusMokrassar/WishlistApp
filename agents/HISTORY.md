# Agent Long-Term Memory — WishlistApp

## Session Log

---

### 2026-05-26 — Session 4 (continued): Initial README.md for all features

**Prompt summary:** Create README.md for all existing features following the rule from ALL.md.

**Actions:**
- action=create; target=features/sample/README.md
- action=create; target=features/users/README.md
- action=create; target=features/auth/README.md
- action=create; target=features/common/README.md
- action=create; target=features/wishlist/README.md
- action=create; target=features/ui/sample/README.md
- action=create; target=features/ui/auth/README.md

**All READMEs contain:**
- `## Operator Notes` section (empty, ready for human input)
- Overview, Routes table, Models table, Architecture Notes

---

### 2026-05-26 — Session 4 (continued): Feature README.md rule

**Prompt summary:** Establish a mandatory README.md rule for all features. Coders and architects must update it after changes. Human operator writes `## Operator Notes`; agents must read and respect it, never modify it.

**Actions:**
- action=update; target=agents/ALL.md; adds=[Feature README.md rule: structure, Operator Notes semantics, agent obligations for coding/architecture agents]
- action=update; target=agents/CODING.md; adds=[Feature README.md cross-ref section at top]
- action=update; target=agents/ARCHITECTURE.md; adds=[Feature README.md cross-ref section at top]

**Rule summary:**
- Every feature must have README.md with `## Operator Notes` section at top
- Agents MUST read README.md (especially Operator Notes) before working on any feature
- Agents MUST NOT modify `## Operator Notes`; violation → stop and ask operator
- Coding agents: update routes/models/behavior/deps after code changes
- Architecture agents: update `## Architecture Notes` after architectural decisions
- README.md structure: Overview + Routes table + Models + Architecture Notes

---

### 2026-05-26 — Session 4 (continued): renames + getMyWishlists + item ownership

**Prompt summary:** Rename WishlistFeature→WishlistsFeature, WishlistItemFeature→WishlistsItemsFeature. Add getMyWishlists to WishlistsFeature. Enforce caller ownership on WishlistItem mutations.

**Actions:**
- action=delete; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt
- action=delete; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistsFeature.kt; adds=[getMyWishlists(): List<RegisteredWishlist>]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistsItemsFeature.kt; changes=[KDocs updated to document ownership enforcement]
- action=update; target=features/wishlist/common/src/commonMain/kotlin/Constants.kt; adds=wishlistGetMyPathPart="getMy"
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; adds=getMyWishlists(callerId: UserId): List<RegisteredWishlist>
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt; changes=[drops WishlistItemFeature impl; adds WishlistRepo param; create(item,callerId)->RegisteredWishlistItem?; update(id,item,callerId)->Boolean?; delete(id,callerId)->Boolean?]; semantics=[null=not_found/parent_not_found, false=unauthorized, true=success]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; adds=GET /wishlist/getMy route using getCallerUserIdOrAnswerUnauthorized
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt; changes=[takes WishlistItemService; create/update/delete call getCallerUserIdOrAnswerUnauthorized; update/delete return 403 on false / 404 on null]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; changes=[WishlistItemService(get(),get()) — two deps; removes WishlistItemFeature binding]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; changes=[implements WishlistsFeature; adds getMyWishlists() → GET /wishlist/getMy]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt; changes=[implements WishlistsItemsFeature]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; changes=[binds WishlistsFeature, WishlistsItemsFeature]

**Route additions:**
- GET /wishlist/getMy → caller from bearer → List<RegisteredWishlist>

**WishlistItem ownership semantics:**
- create: checks wishlistRepo.getById(newItem.wishlistId).userId == callerId; null=not_found_or_unauthorized
- update/delete: checks item→parent_wishlist.userId == callerId; null=not_found, false=unauthorized, true=success
- routes: update/delete → 403 on false / 404 on null (matching WishlistRoutingsConfigurator pattern)

---

### 2026-05-25 — Session 4: onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/SHORTCUTS.md; result=task→file map loaded
- action=read; target=agents/ALL.md; result=mandatory rules loaded
- action=read; target=agents/HISTORY.md; result=project history loaded (Sessions 1-3)

**Project state at session start:**
- branch=master; last_session=Session 3; status=wishlist ownership enforcement complete
- features present: sample, users, auth, ui/auth, ui/serverAddress, ui/sample, wishlist (full-stack)
- wishlist routes: POST /wishlist/create, PUT /wishlist/update/{id}, DELETE /wishlist/delete/{id}; userId from bearer
- WishlistService: create(NewWishlistInFeature,userId), update(id,NewWishlistInFeature,callerId)->Boolean?, delete(id,callerId)->Boolean?; null=not_found, false=unauthorized, true=success
- client: sends NewWishlistInFeature (no userId); server extracts userId from auth

**No changes made to source code this session.**

---

### 2026-05-25 — Session 3 (continued): wishlist ownership enforcement

**Prompt summary:** Enforce caller ownership on wishlist mutations. Add `NewWishlistInFeature` (no UserId). Server extracts userId from auth call. Client must not send userId.

**Actions:**
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; adds=NewWishlistInFeature(title: String) — client-facing create/update body model
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; changes=[create(NewWishlistInFeature), update(WishlistId,NewWishlistInFeature), delete(WishlistId)]; removes=NewWishlist from interface
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; changes=[drops WishlistFeature impl; create(NewWishlistInFeature,userId), update(id,NewWishlistInFeature,callerId)->Boolean?, delete(id,callerId)->Boolean?]; semantics=[null=not found, false=unauthorized, true=success]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; changes=[takes WishlistService; uses getCallerUserIdOrAnswerUnauthorized; 403 on false/404 on null]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; changes=[removes single<WishlistFeature>; WishlistService registered plain]
- action=update; target=features/wishlist/server/build.gradle; adds=api project(":wishlist.features.auth.server")
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; changes=[create+update send NewWishlistInFeature body (no userId)]

**Route behavior:**
- POST /wishlist/create: body=NewWishlistInFeature; userId from bearer; 200=created/500=failure
- PUT /wishlist/update/{id}: body=NewWishlistInFeature; 200=ok/403=not owner/404=missing
- DELETE /wishlist/delete/{id}: no body; 200=ok/403=not owner/404=missing

---

### 2026-05-25 — Session 1: Initial onboarding

**Prompt:** `USE @AGENTS.md`

**Actions:**
- action=read; target=AGENTS.md; result=AML-HIP protocol loaded
- action=read; target=agents/ARCHITECTURE.md; result=full architecture doc loaded
- action=create; target=agents/local.HISTORY.md; reason=mandatory per AGENTS.md

**Project state at session start:**
- branch=master; commits=1 (Initial commit); status=clean
- features present: sample, users, auth, ui/auth, ui/serverAddress, ui/sample
- client targets: JS, JVM, Android
- server: Ktor+Netty, Exposed+PostgreSQL, plugin-based startup

**Key architecture facts loaded:**
- root_package=dev.inmo.wishlist
- build_system=Gradle Groovy DSL + TOML version catalog
- DI=Koin; serialization=kotlinx.serialization.json; navigation=dev.inmo:navigation.mvvm
- feature scaffold: run `./generate_feature.sh` (full-stack) or `./generate_scenario.sh` (UI-only)
- plugin registration: server via sample.config.json "plugins" array; client via Main.kt/MainActivity.kt hardcoded list
- CRUD pattern: ExposedRepo → CacheRepo → singleWithBinds (Read+Write+Full interfaces)
- auth pattern: BCrypt passwords, UUID tokens, bearer Ktor plugin, AuthCredentialsStorage per-platform
- UI MVVM: Model (data/IO) + ViewInteractor (app-level behavior) + ViewModel (state) + View (dumb Compose)
- interactor impls live in client/ClientPlugin.kt, NOT in feature plugin
- HttpClientConfigurator pattern: singleWithRandomQualifier, per-request URL override via onRequest suspend

**No changes made to source code this session.**

---

### 2026-05-25 — Session 1 (continued): wishlist feature + Amount.kt

**Prompt summary:** Add Amount.kt to common.common models; create full wishlist feature (Wishlist + WishlistItem CRUD, server + client + exposed + cache repos).

**Actions:**
- action=run; target=generate_feature.sh; params={module_path=wishlist}; result=scaffold created
- action=create; target=features/common/common/src/commonMain/kotlin/models/Amount.kt; source=ResourcesCounter Amount.kt; package=dev.inmo.wishlist.features.common.common.models
- action=create; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; entities=[WishlistId, Wishlist, NewWishlist, RegisteredWishlist]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt; entities=[WishlistItemId, WishlistItem, NewWishlistItem, RegisteredWishlistItem]; note=approximatePrice=Amount?, priceUnits=String, links=List<String>, description=String
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; methods=[getByUserId, create, update, delete]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt; methods=[getByWishlistId, create, update, delete]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/repo/[ReadWishlistRepo, WriteWishlistRepo, WishlistRepo, CacheWishlistRepo]
- action=create; target=features/wishlist/common/src/commonMain/kotlin/repo/[ReadWishlistItemRepo, WriteWishlistItemRepo, WishlistItemRepo, CacheWishlistItemRepo]
- action=create; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistRepo.kt; table=wishlists
- action=create; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; table=wishlist_items; note=Amount stored as (approx_price_int BIGINT NULL, approx_price_dec BIGINT NULL); links stored as JSON text
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; adds=[ExposedWishlistRepo, CacheWishlistRepo, ExposedWishlistItemRepo, CacheWishlistItemRepo]
- action=update; target=features/wishlist/common/build.gradle; adds=api project(":wishlist.features.users.common")
- action=create; target=features/wishlist/server/src/commonMain/kotlin/services/[WishlistService, WishlistItemService]
- action=create; target=features/wishlist/server/src/commonMain/kotlin/configurators/[WishlistRoutingsConfigurator, WishlistItemRoutingsConfigurator]
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; registers=[WishlistService, WishlistFeature, WishlistItemService, WishlistItemFeature, routing configurators]
- action=create; target=features/wishlist/client/src/commonMain/kotlin/[KtorWishlistFeature, KtorWishlistItemFeature]
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; registers=[KtorWishlistFeature as WishlistFeature, KtorWishlistItemFeature as WishlistItemFeature]
- action=update; target=settings.gradle; adds=[:features:wishlist:common, :features:wishlist:server, :features:wishlist:client]
- action=update; target=server/build.gradle; adds=api project(":wishlist.features.wishlist.server")
- action=update; target=client/build.gradle; adds=api project(":wishlist.features.wishlist.client")
- action=update; target=server/sample.config.json; adds="dev.inmo.wishlist.features.wishlist.server.JVMPlugin"
- action=update; target=client/src/jsMain/kotlin/Main.kt; adds=wishlist.client.JSPlugin
- action=update; target=client/src/jvmMain/kotlin/Main.kt; adds=wishlist.client.JVMPlugin
- action=update; target=client/android/src/main/kotlin/MainActivity.kt; adds=wishlist.client.AndroidPlugin

**Route map:**
- GET /wishlist/getByUserId/{userId} → List<RegisteredWishlist>
- POST /wishlist/create (body: NewWishlist) → RegisteredWishlist?
- PUT /wishlist/update/{id} (body: NewWishlist) → 200/404
- DELETE /wishlist/delete/{id} → 200
- GET /wishlistItem/getByWishlistId/{wishlistId} → List<RegisteredWishlistItem>
- POST /wishlistItem/create (body: NewWishlistItem) → RegisteredWishlistItem?
- PUT /wishlistItem/update/{id} (body: NewWishlistItem) → 200/404
- DELETE /wishlistItem/delete/{id} → 200

**All routes inside authenticate {} block.**

---

### 2026-05-25 — Session 2 (continued): KDocs + CODING.md

**Prompt summary:** Add KDocs to all created .kt files; add KDoc rule to agents/CODING.md; fill CODING.md with verbatim coding rules from ARCHITECTURE.md.

**Actions:**
- action=create; target=agents/CODING.md; content=[KDoc requirement rule + verbatim coding sections from ARCHITECTURE.md]
- action=update; target=features/common/common/src/commonMain/kotlin/models/Amount.kt; adds=KDocs (class, all properties, all constructors, all methods, companion)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/Wishlist.kt; adds=KDocs (WishlistId, Wishlist, NewWishlist, RegisteredWishlist)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt; adds=KDocs (WishlistItemId, WishlistItem, NewWishlistItem, RegisteredWishlistItem)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistFeature.kt; adds=KDocs (interface + all methods with @param/@return)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/WishlistItemFeature.kt; adds=KDocs (interface + all methods with @param/@return)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/Constants.kt; adds=KDocs (object + all constants)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/ReadWishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WriteWishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WishlistRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/CacheWishlistRepo.kt; adds=KDocs (class with @param, getByUserId delegation note)
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/ReadWishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WriteWishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/WishlistItemRepo.kt; adds=KDocs
- action=update; target=features/wishlist/common/src/commonMain/kotlin/repo/CacheWishlistItemRepo.kt; adds=KDocs (class with @param, getByWishlistId delegation note)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistRepo.kt; adds=KDocs (class with schema doc, getByUserId)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; adds=KDocs (class with full schema + Amount/links encoding notes, amountOrNull, getByWishlistId)
- action=update; target=features/wishlist/common/src/jvmMain/kotlin/JVMPlugin.kt; adds=KDocs
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt; adds=KDocs (class + all methods)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt; adds=KDocs (class + all methods)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt; adds=KDocs (class with route table, @param)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt; adds=KDocs (class with route table, @param)
- action=update; target=features/wishlist/server/src/commonMain/kotlin/Plugin.kt; adds=KDocs
- action=update; target=features/wishlist/server/src/jvmMain/kotlin/JVMPlugin.kt; adds=KDocs
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt; adds=KDocs (class with auth note, create/update/delete)
- action=update; target=features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt; adds=KDocs (class with auth note, create/update/delete)
- action=update; target=features/wishlist/client/src/commonMain/kotlin/Plugin.kt; adds=KDocs
