Model: fable (claude-fable-5)
Execution time: ~420 seconds
Tokens used: ~55000
Changed files: agents/task/10.07.2026_12.25.50-0d78112b-aca8-4764-8ed4-4f6a7d67e7dc/002-planning.md

# 002 — Planning finalization (GitHub issue #67)

**Status: READY — no remaining open questions.**

This step file supersedes step `001-planning.md` section 5.3 (the Option-B delta) and section 5.2's
provisional wording. `001-planning.md` is left untouched (not overwritten, per monotonic step
numbering) — read it first for the full investigation/audit tables (sections 1–4); this file only
restates and finalizes the concrete plan sections (5.1/5.2) now that the interpretation question is
resolved, plus a few implementation details discovered while finalizing.

## 1. Operator's answer to Q1 (verbatim, relayed by Root's caller)

> Operator answered Q1 (via Root's caller): **(A) Strict** — a `*Feature` interface method must never
> return a persistence/CRUD model (`New*`/`Registered*`), not even one owned by the same feature; every
> non-exempt return gets a dedicated feature/wire model. Point 3's audit scope is confirmed as you laid
> out: auth (`getMe`/`getUser`/`meStateFlow`), booking (`myPresentsBooks`), admin (users/wishlists/items
> surfaces), `WishlistsFeature`, `WishlistsItemsFeature`, `FilesFeature` (`finalize`/`getMeta`).

This is `001-planning.md`'s recommended Option A. Rule wording and placement from `001-planning.md`
section 2.3 stands as written (the "not even the feature's own" bullet stays in; no B/C delta clause
needed).

## 2. New questions surfaced while finalizing? None genuine.

While confirming exact file lists below, three implementation details were checked and resolved by
direct investigation (not escalated, since each has one unambiguous answer visible in the code):

- **`Email` type availability for `AuthFeatureUser`/`AdminUser`:** `features/auth/common/build.gradle`
  and `features/admin/common/build.gradle` both already declare `api project(":wishlist.features.users.common")`,
  and `features/users/common/build.gradle` already declares `api project(":wishlist.features.email.common")`.
  Gradle `api` dependencies are transitive, so `Email` is already visible in both `auth/common` and
  `admin/common` — **no new Gradle dependency line needed for `Email`**.
- **`wishlist.common` dependency for `admin/common`:** `features/admin/common/build.gradle` currently
  depends only on `users.common` + `auth.common`, NOT `wishlist.common`. `AdminWishlist`/`AdminWishlistItem`
  need `WishlistId`/`WishlistItemId`/etc. — **`admin/common/build.gradle` needs a new line
  `api project(":wishlist.features.wishlist.common")`**.
- **Server-side asymmetry for wishlist and admin-wishlist surfaces (important for Architecture):**
  - `features/wishlist/server/src/commonMain/kotlin/Plugin.kt`'s own KDoc states explicitly: *"Neither
    `WishlistService` nor `WishlistItemService` is bound to a client-facing feature interface because
    their mutation methods carry an explicit caller `UserId` parameter that is absent from the
    client-facing interfaces."* I.e. **there is no server-side `WishlistsFeature`/`WishlistsItemsFeature`
    interface at all** — `WishlistRoutingsConfigurator`/`WishlistItemRoutingsConfigurator` call
    `WishlistService`/`WishlistItemService` directly and `call.respond(...)` the persistence model. The
    client `WishlistsFeature`/`WishlistsItemsFeature` interfaces are the only formal `*Feature` contracts
    for this surface; the server's half of the same wire contract lives in the routing configurators +
    services, which must change together with the client interface to keep the HTTP contract type-safe.
  - `features/admin/server/src/commonMain/kotlin/AdminRoutingsConfigurator.kt` handles ALL wishlists/items
    admin routes **inline** (directly calling `wishlistService`/`wishlistRepo`/`wishlistItemRepo`), not
    through any `AdminWishlistsFeature`/`AdminWishlistItemsFeature`-shaped server object — only the users
    routes go through the `AdminFeature.usersManagement` object. So on server side, "where does
    `RegisteredWishlist`/`RegisteredWishlistItem` get returned for admin routes" = directly inside
    `AdminRoutingsConfigurator`'s route handlers (lines ~160–277 as read), which is exactly where the
    server-side mapping to `AdminWishlist`/`AdminWishlistItem` must be inserted.

Neither point changes scope or requires an operator decision — both are mechanical "where exactly does
the code live" facts needed so Architecture doesn't have to re-derive them.

## 3. Finalized concrete plan — two commits, full file lists

### Commit A — point 1 fix + point 2 rule (unchanged from `001-planning.md` section 5.1)

Confirmed as final, no changes from the prior step file. Restated in full so Architecture has one
authoritative source:

1. **NEW** `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt` —
   `@Serializable data class UsersFeatureUser(val id: UserId, val username: Username)` (no `email`) +
   `fun RegisteredUser.asUsersFeatureUser(): UsersFeatureUser` mapper, full KDoc.
2. `features/users/server/src/commonMain/kotlin/UsersFeature.kt` — `getAll(): List<UsersFeatureUser>` + KDoc.
3. `features/users/server/src/commonMain/kotlin/services/UsersService.kt` —
   `usersRepo.getAll().values.map { it.asUsersFeatureUser() }`.
4. `features/users/client/src/commonMain/kotlin/UsersFeature.kt` — return type + KDoc.
5. `features/users/client/src/commonMain/kotlin/KtorUsersFeature.kt` — return type.
6. `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt` — retype `getAllUsers`/`getUser` + KDocs.
7. `features/ui/users/src/commonMain/kotlin/Plugin.kt` — retype anonymous impl + import swap.
8. `features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt` — retype `_usersState`.
9. `features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt` — retype `_userState`.
10. `agents/CODING.md` — insert `## Feature Interface Return Model Rule` section (exact text in
    `001-planning.md` section 2.3) directly after `## CRUD Repository Pattern`.
11. `features/users/README.md` — Routes table response type, Models table, Architecture Notes.
12. `features/ui/users/README.md` — Models section.

No-change-but-verify-compiles: `UsersRoutingsConfigurator` (just `call.respond(feature.getAll())`, no
type name used), both users `Plugin.kt` DI files, `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt`
line ~177 (`usersFeature.getAll().find { it.id == userId }?.username?.string` — only touches
`id`/`username`, present on `UsersFeatureUser`).

Build gate: `./gradlew :wishlist.features.users.common:build :wishlist.features.users.server:build :wishlist.features.users.client:build :wishlist.features.ui.users:build :wishlist.features.ui.wishlist:build` (or full `./gradlew build`), then `ast-index rebuild`.

Commit message topic: `fix(users): hide emails from public users listing behind UsersFeatureUser model; document feature-model rule`.

### Commit B — point 3 cross-feature audit fixes, ALL 6 violation surfaces (final, supersedes 001's 5.2/5.3 split)

#### B-V1 — Auth (`getMe` / `getUser` / `meStateFlow`)

New model: `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt` —
`@Serializable data class AuthFeatureUser(val id: UserId, val username: Username, val email: Email?)`
+ mapper `RegisteredUser.asAuthFeatureUser()`. Keeps `email` deliberately (own-record self-service
surface) — KDoc must state why explicitly so a future auditor doesn't flag it as a re-leak.

Files to change:
- `features/auth/client/src/commonMain/kotlin/ClientAuthFeature.kt` — `getMe(): AuthFeatureUser?`.
- `features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt` — return type.
- `features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt` — retype (wraps `ClientAuthFeature`).
- `features/auth/client/src/commonMain/kotlin/Me.kt` — both `StateFlow<RegisteredUser?>` qualifier
  helper pairs (`meStateFlow` public + `secretMeMutableStateFlow` internal) → `StateFlow<AuthFeatureUser?>`.
- `features/auth/server/src/commonMain/kotlin/ServerAuthFeature.kt` — `getUser(token): AuthFeatureUser?`.
- `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt` — `getUser` impl, map result.
- `features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt` — line ~80
  `getMePathPart` handler (`val registeredUser = authFeature.getUser(token)`), respond type follows
  automatically; verify no other field of the old `RegisteredUser` is read here.
- **Not touched:** `BearerAuthenticationConfigurator` — verified it authenticates via
  `authenticate(token): Username?`/similar, never calls `getUser`; unaffected by this retype.
- Consumers (compile-verify, retype where the field is stored/typed): `features/ui/auth/src/commonMain/kotlin/Plugin.kt`
  (AuthModel `isAlreadyLoggedIn`/login flow touches `meStateFlow` only via login success, no field access),
  `features/ui/sidebar/src/commonMain/kotlin/Plugin.kt`, `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt`
  + `ui/WishlistItemViewModel.kt` + `ui/WishlistViewModel.kt` + `ui/UserWishlistsViewModel.kt` +
  `ui/WishlistsModel.kt` + `ui/WishlistsListViewModel.kt` (all consume `meStateFlow`/`getMe()` — audit each
  for `.email` reads; investigation so far found none, but Architecture/Coding must re-check since these
  files were not read in full during Planning), `features/ui/users/src/commonMain/kotlin/Plugin.kt`
  (`meState.map { it?.id }` / `{ it?.username?.string == "root" }` — structurally compatible, `id`/`username`
  both present on `AuthFeatureUser`).

#### B-V2 — Booking (`myPresentsBooks`)

New model: `features/booking/common/src/commonMain/kotlin/models/BookingFeatureItem.kt` — mirrors
`RegisteredWishlistItem`'s full display field set (`id: WishlistItemId, wishlistId: WishlistId, title: String,
amount: UInt, approximatePrice: Amount?, priceUnits: String, links: List<WishlistItemLink>, description: String,
priority: Priority, imageIds: List<FileId>`) + mapper `RegisteredWishlistItem.asBookingFeatureItem()`.
`features/booking/common/build.gradle` already depends on `wishlist.common` — no new Gradle dependency.

Files to change:
- `features/booking/client/src/commonMain/kotlin/BookingFeature.kt` — `myPresentsBooks(): List<BookingFeatureItem>`.
- `features/booking/client/src/commonMain/kotlin/KtorBookingFeature.kt` — return type.
- `features/booking/server/src/commonMain/kotlin/services/BookingService.kt` — `myPresentsBooks` impl, map results.
- `features/booking/server/src/commonMain/kotlin/configurators/BookingRoutingsConfigurator.kt` — verify
  the `myPresentsBooks` route handler needs no code change beyond the service's new return type (confirm
  during Coding — not read in full during Planning).
- `features/ui/booking/src/commonMain/kotlin/ui/BookingModel.kt` — `myPresentsBooks(): List<BookingFeatureItem>`.
- `features/ui/booking/src/commonMain/kotlin/ui/MyPresentsBooksViewModel.kt` — retype `_presentsState`.
- `features/ui/booking/src/commonMain/kotlin/Plugin.kt` — retype anonymous impl.
- `features/ui/booking/src/jvmMain/kotlin/ui/MyPresentsBooksView.kt`,
  `features/ui/booking/src/androidMain/kotlin/ui/MyPresentsBooksView.kt` — compile-verify field access
  (title/amount/price/links/etc. all present on the new model).

#### B-V3 — Admin (users/wishlists/items management surfaces)

New models in `features/admin/common/src/commonMain/kotlin/models/` (new package; `admin/common` already
has a `models/` dir with `NewUserWithPassword.kt`):
- `AdminUser.kt` — `AdminUser(id: UserId, username: Username, email: Email?)` + mapper
  `RegisteredUser.asAdminUser()`. Root-only surface (`features/admin/README.md` Operator Notes: "Only
  `root` user must have access to the admin panel and features") — `email` kept deliberately, KDoc states why.
- `AdminWishlist.kt` — `AdminWishlist(id: WishlistId, userId: UserId, title: String, defaultPriceUnits: String)`
  + mapper `RegisteredWishlist.asAdminWishlist()`.
- `AdminWishlistItem.kt` — mirrors `RegisteredWishlistItem`'s full field set (same shape as B-V2's
  `BookingFeatureItem` but this is a separate admin-owned type per the strict per-feature-model rule) +
  mapper `RegisteredWishlistItem.asAdminWishlistItem()`.

Gradle: `features/admin/common/build.gradle` — add `api project(":wishlist.features.wishlist.common")`
(see section 2 above; `Email`/`users.common` already available transitively).

Files to change:
- `features/admin/client/src/commonMain/kotlin/UsersManagementFeature.kt` — `getAll/getById/create` → `AdminUser`.
- `features/admin/client/src/commonMain/kotlin/KtorUsersManagementFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminWishlistsFeature.kt` — `getAll/getByUserId/getById/create` → `AdminWishlist`.
- `features/admin/client/src/commonMain/kotlin/KtorAdminWishlistsFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminWishlistItemsFeature.kt` — `getByWishlistId/create` → `AdminWishlistItem`.
- `features/admin/client/src/commonMain/kotlin/KtorAdminWishlistItemsFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminFeature.kt` — no type change (just exposes sub-interfaces).
- `features/admin/client/src/commonMain/kotlin/KtorAdminFeature.kt` — verify no type-specific code (likely none).
- `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — `getAll/create` → `AdminUser`
  (`update`/`setPassword`/`delete` stay `Boolean?`, unaffected).
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — per section 2's
  finding, this file does the actual wishlists/items admin routing inline. Map results to the new models
  before each `call.respond(...)`: `usersGetAllPathPart`/`usersGetByIdPathPart`/`usersCreatePathPart`
  handlers (already covered by `adminFeature.usersManagement` retype, but the `usersGetByIdPathPart`
  handler calls `usersRepo.getById(id)` directly — line ~94–98 — and must map to `AdminUser` explicitly
  since it bypasses `UsersManagementFeature`), `wishlistsGetAllPathPart` (`wishlistRepo.getAll().values.toList()`
  — map to `AdminWishlist`), `wishlistsGetByUserIdPathPart`/`wishlistsGetByIdPathPart`/`wishlistsCreatePathPart`
  (via `wishlistService`, map results), `wishlistsUpdatePathPart` (`wishlistRepo.update(...)` returns
  `RegisteredWishlist?` — map), `wishlistItemsGetByWishlistIdPathPart` (`wishlistItemRepo.getByWishlistId`
  — map list), `wishlistItemsCreatePathPart`/`wishlistItemsUpdatePathPart` (`wishlistItemRepo.create`/`.update`
  — map).
- `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt` — retype `getAllUsers/getUserById/createUser`
  → `AdminUser`, `getAllWishlists/getWishlistsByUser/getWishlistById/createWishlist` → `AdminWishlist`,
  `getItemsByWishlist/createWishlistItem` → `AdminWishlistItem`.
- `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` — retype anonymous impl + imports throughout.
- `features/ui/adminPanel/src/{jvmMain,jsMain,androidMain}/kotlin/ui/AdminPanelView.kt` — compile-verify
  field access (all three platform views import `Email` already; verify `.email` read sites still resolve
  against `AdminUser.email`).
- Any admin ViewModels under `features/ui/adminPanel/src/commonMain/kotlin/ui/` not yet enumerated during
  Planning — Architecture/Coding must `ast-index outline` the `ui/` package to catch any missed file (this
  directory was not fully listed during Planning; only `AdminPanelModel.kt`/`Plugin.kt` were read).

#### B-V4 — `WishlistsFeature` (wishlist client)

New model: `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureWishlist.kt` —
mirrors `RegisteredWishlist` (`id: WishlistId, userId: UserId, title: String, defaultPriceUnits: String`)
+ mapper `RegisteredWishlist.asWishlistsFeatureWishlist()`.

Files to change:
- `features/wishlist/client/src/commonMain/kotlin/WishlistsFeature.kt` — `getById/getByUserId/getMyWishlists/create` → new type; `update`/`delete` stay `Boolean`.
- `features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt` — return types.
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt` — `getById/getByUserId/getMyWishlists/create` map to new type (note the Plugin.kt KDoc caveat from section 2 — this service is not bound to a formal interface, so retyping it directly IS the fix here).
- `features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt` — `call.respond(...)` sites for the above 4 methods (lines ~57, ~68, ~75, ~84 per the grep in section 2) — verify each responds with the mapped type, not the raw service/repo result directly (the `create` handler stores `result` from `wishlistService.create(...)`, already covered by the service retype).
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt`, `ui/WishlistsListViewModel.kt`,
  `ui/UserWishlistsViewModel.kt`, `Plugin.kt` — retype wherever `RegisteredWishlist` currently flows from
  `WishlistsFeature` calls (NOT from `AdminWishlistsFeature`/`WishlistsItemsFeature`, which are separate
  surfaces with their own new types per B-V3/B-V5). These files were not read in full during Planning —
  Architecture must `ast-index outline` + `ast-index refs RegisteredWishlist` scoped to `features/ui/wishlist`
  to get the exact line list.

#### B-V5 — `WishlistsItemsFeature` (wishlist client)

New model: `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureItem.kt` — mirrors
`RegisteredWishlistItem` (same full field set as B-V2/B-V3's item mirrors — again a separate type per
the strict per-feature-model rule) + mapper `RegisteredWishlistItem.asWishlistsFeatureItem()`.

Files to change:
- `features/wishlist/client/src/commonMain/kotlin/WishlistsItemsFeature.kt` — `getByWishlistId/create/copy` → new type.
- `features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt` — return types.
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt` — `getByWishlistId/create/copyItem` map to new type.
- `features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt` — `call.respond(...)` sites for the above.
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt`, `ui/WishlistViewModel.kt`,
  `Plugin.kt` — retype wherever `RegisteredWishlistItem` flows from `WishlistsItemsFeature` calls
  specifically (not the booking or admin surfaces). Same "not fully read, ast-index outline first" caveat
  as B-V4.

#### B-V6 — `FilesFeature` (`finalize` / `getMeta`)

New model: `features/files/common/src/commonMain/kotlin/models/FilesFeatureMetaInfo.kt` — mirrors
`RegisteredFileMetaInfo` (`id: FileId, fileName: FileName, mimeType: String, size: Long, uploaderId: UserId`)
+ mapper `RegisteredFileMetaInfo.asFilesFeatureMetaInfo()`.

Files to change:
- `features/files/client/src/commonMain/kotlin/FilesFeature.kt` — `finalize/getMeta` → `FilesFeatureMetaInfo?`.
- `features/files/client/src/commonMain/kotlin/KtorFilesFeature.kt` — return types.
- `features/files/client/src/commonMain/kotlin/FilesClientService.kt` — `uploadFile(...): FilesFeatureMetaInfo?`
  (wraps `finalize`; not itself a `*Feature` interface, but must retype to stay call-site-compatible).
- `features/files/server/src/commonMain/kotlin/services/FilesService.kt` — `finalize`/`getMeta`-equivalent
  methods, map to new type (exact method names to be confirmed by Architecture — not read in full during
  Planning; `ast-index outline` the file first).
- `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt` — corresponding `call.respond(...)` sites.
- Consumers verified structurally compatible (only `.id` accessed): `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt`
  line ~180 (`filesService.uploadFile(file)?.id`). `features/ui/users/src/commonMain/kotlin/Plugin.kt`'s
  `getAvatar`/`uploadAvatar` return `FileId?` directly (not the meta-info type) — unaffected, no change needed there.

### Shared conventions for every commit-B model

- Same naming convention as commit A: `<FeatureInterfaceName><Entity>` (e.g. `BookingFeatureItem`,
  `WishlistsFeatureWishlist`) — chosen over a shorter purpose name (e.g. `BookingItem`) because it makes
  the owning `*Feature` interface traceable directly from the type name, matching `UsersFeatureUser`.
  Exception: `AdminUser`/`AdminWishlist`/`AdminWishlistItem` drop the "Feature" infix — admin already has
  three separate `*Feature` interfaces (`UsersManagementFeature`, `AdminWishlistsFeature`,
  `AdminWishlistItemsFeature`) sharing one `admin` model namespace, so `Admin<Entity>` reads clearer than
  three different `<InterfaceName><Entity>` prefixes for what is conceptually one "admin view of X".
- Every mapper is a `fun Registered<Entity>.as<NewTypeName>(): <NewTypeName>` extension in the same file
  as its model, full KDoc, placed in the feature's `common` module (never `client`/`server` — both sides
  need it).
- Full KDoc on every new model/mapper per `agents/CODING.md` (class purpose, `@property` tags, no
  placeholders); no inline value classes introduced by these models, so the value-class naming rule does
  not apply here.
- Every touched feature's `README.md` gets its Routes/Models/Architecture-Notes sections updated (never
  Operator Notes). Confirmed READMEs exist for all 6 touched features + their `ui/*` counterparts: `auth`,
  `booking`, `admin`, `wishlist`, `files`, plus `features/ui/users`, `features/ui/booking`,
  `features/ui/adminPanel` (README existence verified in `001-planning.md`; `features/ui/wishlist`'s
  README existence to be confirmed by Architecture — not checked during Planning).

Build gate for commit B: full `./gradlew build` (touches 6 features + their ui/* consumers — a scoped
module list would be error-prone to enumerate correctly by hand), then `ast-index rebuild`.

Commit message topic: `fix(features): return feature-owned models from *Feature interfaces per new rule`.

## 4. Test stubs (restated from `001-planning.md` section 5.4, unchanged)

Test infra confirmed present: `features/users/common/src/jvmTest`, `features/email/server/src/commonTest`.
For each new model + mapper pair introduced in commit A and commit B, minimum test coverage:
- Serialization round-trip / field-set check: the model's JSON contains exactly its declared fields (no
  leaked persistence-only fields — this is the regression test for the original bug class).
- Mapper unit test: given a fake/constructed `Registered*` instance, `as<NewType>()` produces the expected
  projected fields.
- Where a service method was retyped (`UsersService.getAll`, `BookingService.myPresentsBooks`,
  `WishlistService.*`, `WishlistItemService.*`, `FilesService.*`, `AuthFeatureService.getUser`,
  admin's `UsersManagementFeature.getAll`/`create`), a test with a fake underlying repo verifying the
  returned list/object is the new type with correctly mapped fields.

## 5. Ordering handed to Architecture

1. Architecture may proceed on BOTH commits now (Q1 resolved) — spec commit A and commit B in one pass,
   producing detailed design + the above test stubs in its own step file.
2. Coding pass 1 → commit A (source files + step report, single commit, message topic per section 3).
3. Coding pass 2 → commit B (source files + step report, single commit, message topic per section 3).
   Two separate commits remain mandatory per the issue's point 3 — do not squash.
4. Both commits stay on branch `fix/67-users-feature-model`; no push, no PR, no `gh` commands — issue-executor
   (Root's caller) handles that after the full cycle completes successfully.

## 6. Result

- Q1 answered by operator: **(A) Strict** (verbatim in section 1). No further open questions — plan is READY.
- Final plan: section 3 (commit A file list unchanged from `001-planning.md`; commit B file list finalized
  across all 6 violation surfaces with concrete per-file changes, two Gradle-dependency findings, and two
  server-side architecture asymmetries documented so Architecture does not have to re-derive them).
- Handoff: Architecture may proceed immediately on both commits per section 5.

---

## 7. Independent re-verification pass (addendum)

Addendum step header — Model: Sonnet 5 (claude-sonnet-5) / Execution time: ~1400 seconds / Tokens used:
~150000 / Changed files: this file only (appended section 7).

**Process note (why this section exists):** this exact finalization task was dispatched twice. Sections
1–6 above were written and committed (`ac5574e`) by a concurrent Planning pass while this independent pass
was already investigating from a cold start. Rather than overwrite or fork a competing version of the plan,
this section re-verifies sections 1–6 against a fresh read of current source (three parallel read-only
investigation agents covering commit A's 12 files, commit B's V1–V3, and commit B's V4–V6, plus direct
spot-reads of build.gradle files, feature interfaces, and `RegisteredFileMetaInfo`) and records the delta.
Sections 1–6 stand except where superseded below. **Status remains READY.**

### 7.1 Commit A (section 3, commit A) — fully confirmed, no changes

All 12 files re-read against current source: exact match, including the "no-change-but-verify-compiles"
list. Cross-referenced `ast-index refs UsersFeature` / `ast-index usages getAll` plus repo-wide grep for
`RegisteredUser` importers — no missing call site found. File list, build gate, and commit message in
section 3 stand unmodified.

### 7.2 Commit B corrections (supersedes the specific bullets named below; everything else in section 3 stands)

**B-V1 (auth) — one missing file, one over-inclusion:**
- **Add** `features/auth/client/src/commonMain/kotlin/Plugin.kt` to the file list: it registers the DI
  qualifier backing `meStateFlow` — `singleSecretMeMutableStateFlow { MutableRedeliverStateFlow<RegisteredUser?>(null) }`
  (line 40) plus the `RegisteredUser` import (line 19). This is the concrete file behind section 3's
  "`meStateFlow` DI surface" phrase, which named `Me.kt` but not the plugin registration that instantiates
  the flow — retype to `MutableRedeliverStateFlow<AuthFeatureUser?>`.
- **Remove** `features/ui/sidebar` from the B-V1 consumer list. Verified via grep across all of
  `features/ui/sidebar/src/`: zero references to `RegisteredUser`, `meStateFlow`, or `AuthFeature`. Sidebar
  only consumes `WishlistsModel.currentUserIdFlow: StateFlow<UserId?>` (already an id) — not a V1 consumer.
  (Sidebar **is** a genuine V4 consumer — see below — the two are unrelated surfaces.)

**B-V2 (booking) — one missing file:**
- **Add** `features/ui/booking/src/jsMain/kotlin/ui/MyPresentsBooksView.kt` explicitly to the file list.
  It directly imports and type-annotates `RegisteredWishlistItem` (`private fun ReservedCard(item: RegisteredWishlistItem)`,
  lines ~20/51) — this is a real retype, not the "compile-verify field access only" treatment section 3
  gave the jvmMain/androidMain platform views (those two remain compile-verify-only).

**B-V3 (admin) — confirmed, no file-list change.** The "server-side equivalents" phrasing in section 3's
B-V3 file list is already correctly qualified by this file's own section 2 (no server-side
`AdminWishlistsFeature`/`AdminWishlistItemsFeature` class exists; the fix point is inline in
`AdminRoutingsConfigurator`) — re-verification found no additional discrepancy beyond what section 2
already documents. `AdminWishlistItemsFeature.kt` / `KtorAdminWishlistItemsFeature.kt` reconfirmed to exist
client-side (the admin README does not document them — pre-existing doc staleness, unrelated to this plan,
folded into the mandatory README update).

**B-V4 (`WishlistsFeature`) — one missing consumer:**
- **Add** `features/ui/sidebar` (`ui/SidebarModel.kt:26`, `ui/SidebarViewModel.kt:55`, `Plugin.kt:40`) to
  the file list — all three type `List<RegisteredWishlist>`, wrapping `WishlistsModel.getMyWishlists()`
  transitively. Retype to `List<WishlistsFeatureWishlist>`.
- Confirmed exact current signatures of all 4 non-exempt `WishlistsFeature` methods match section 3
  verbatim; `update`/`delete` correctly excluded (return `Boolean`).
- Confirmed booking (V2) has not landed on this branch yet (still returns `RegisteredWishlistItem` today) —
  expected, since Coding hasn't run; not an inconsistency.

**B-V5 (`WishlistsItemsFeature`) — one file removed, several files added:**
- **Remove** `WishlistCopyService` from the file list. Verified its only public method,
  `enqueue(sourceWishlistId, recipientUserId): RegisteredWishlistCopyJob?` (line 67), never returns
  `RegisteredWishlistItem` — it calls `wishlistItemRepo.create(...)` at the repo layer internally and
  exposes only `RegisteredWishlistCopyJob` outward. `WishlistCopyFeature.enqueueCopy(): Boolean` (the
  actual `*Feature` method) was already correctly marked compliant in `001-planning.md` section 2.4's audit
  table — section 3's B-V5 bullet contradicted that finding by including the service; the audit table wins.
- **Supplement** (concrete examples for section 3's own "not fully read, `ast-index outline` first"
  caveat — not exhaustive, Architecture should still run the outline): `features/ui/wishlist/ui/BookingConfigsProvider.kt`,
  `ui/WishlistAdditionalConfigsProvider.kt`, `ui/WishlistItemAdditionalConfigView.kt`,
  `ui/WishlistItemCopyViewModel.kt`, plus platform-specific views (`UserWishlistsView.kt` in jvmMain/androidMain/jsMain,
  `WishlistItemCard.kt`, jsMain `WishlistItemRow.kt`) all reference `RegisteredWishlistItem` and fall inside
  V5's retype scope.

**B-V6 (`FilesFeature`) — confirmed accurate, blast radius smaller than section 3 implied:**
- Exact signatures and `RegisteredFileMetaInfo` field set (`id`, `fileName`, `mimeType`, `size`,
  `uploaderId`) re-confirmed verbatim against `features/files/common/.../models/FileMetaInfo.kt:57-63`.
- Repo-wide grep found **zero** UI callers of client `getMeta`; the only `uploadFile()`/`finalize()`
  consumer is `features/ui/wishlist/Plugin.kt:180`, which narrows to `?.id` (`FileId`) immediately — no UI
  file needs retyping beyond the four core files (`FilesFeature.kt`, `KtorFilesFeature.kt`,
  `FilesClientService.kt`, server `FilesService.kt` + `FilesRoutingsConfigurator.kt`) already named in
  section 3.

### 7.3 V4/V5 naming — finalized by Planning now, not handed to Architecture

Per this file's task instructions, Planning decides rather than deferring: **`WishlistsFeatureWishlist`**
(V4) and **`WishlistsFeatureItem`** (V5), exactly as section 3 already named them. Rationale: strict
adherence to the established `<FeatureInterfaceName><Entity>` convention (matches `UsersFeatureUser`,
`BookingFeatureItem`) beats a shorter purpose name for greppability across a large multi-module codebase,
even though "Wishlists...Wishlist" reads slightly redundant — the codebase already tolerates a comparable
overlap (`WishlistItem` / `WishlistsItemsFeature`). An independent investigation agent, asked to propose
an alternative, converged on the same recommendation unprompted. No further input needed from Architecture
on this point.

### 7.4 Dependency additions — confirmed exact, with verified transitivity

- **`auth/common` → `email/common`:** confirmed **not required**. `features/auth/common/build.gradle`
  depends only on `common.common` + `users.common`; `features/users/common/build.gradle` declares
  `api project(":wishlist.features.email.common")`. Gradle `api` is transitive, so `Email` is already
  visible inside `auth/common` through the existing `users.common` dependency. No `build.gradle` edit needed.
- **`admin/common` → `wishlist/common`:** confirmed **required**. `features/admin/common/build.gradle`
  currently depends only on `users.common` + `auth.common` — neither transitively carries `wishlist.common`.
  Exact line to add to `features/admin/common/build.gradle`'s `commonMain.dependencies` block:
  ```groovy
  api project(":wishlist.features.wishlist.common")
  ```
- **`admin/common` → `email/common`:** confirmed **not required** — reachable transitively via the same
  `users.common` → `email.common` chain as auth above.
- **`booking/common` → `wishlist/common`:** already present (`api project(":wishlist.features.wishlist.common")`
  confirmed in `features/booking/common/build.gradle`) — no change, as section 3 already stated.

### 7.5 Test-stub candidates — made explicit per-model (section 4's generic rule, enumerated)

Section 4's "for each new model + mapper pair" is confirmed to cover every model introduced by this plan;
enumerated explicitly here so Coding has an unambiguous checklist. For **each** of the 8 new models —
`UsersFeatureUser`, `AuthFeatureUser`, `BookingFeatureItem`, `AdminUser`, `AdminWishlist`,
`AdminWishlistItem`, `WishlistsFeatureWishlist`, `WishlistsFeatureItem`, `FilesFeatureMetaInfo` — write:
1. A serialization/field-set test asserting the encoded JSON contains exactly the model's declared
   properties (regression test for the leak class point 1 fixes — most important for `UsersFeatureUser`,
   `AuthFeatureUser`, and `AdminUser`, the three that sit near an `email` field).
2. A mapper unit test (`as<NewType>()`) asserting every field is projected correctly from a constructed
   `Registered*` fixture.
3. A service-layer test with a fake underlying repo/service for each retyped method:
   `UsersService.getAll`, `AuthFeatureService.getUser` (server), `BookingService.myPresentsBooks`,
   admin `UsersManagementFeature.getAll`/`create`, `WishlistService.getById`/`getByUserId`/`getMyWishlists`/`create`,
   `WishlistItemService.getByWishlistId`/`create`/`copyItem`, `FilesService`'s `finalize`/`getMeta`-equivalent
   methods — each asserting the returned value(s) are the new feature model with correctly mapped fields.

Test infra confirmed present and reusable as scaffolding precedent: `features/users/common/src/jvmTest/kotlin/repo/IsUniqueViolationTest.kt`,
`features/email/server/src/commonTest/kotlin/**` (including its `Fake*Repo` pattern, directly reusable for
the fake-repo service tests above).

### 7.6 Result of this addendum

- All corrections above are additive/corrective deltas on section 3, not a reinterpretation of Q1 or the
  rule text — no new open question was surfaced.
- Status: **READY**, unchanged. Architecture should read sections 1–6 for the base plan and section 7 for
  the corrected file lists (7.2), finalized V4/V5 names (7.3, already reflected in section 3's own text),
  confirmed Gradle deltas (7.4), and the explicit test checklist (7.5).
