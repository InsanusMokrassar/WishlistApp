Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~5400 seconds
Tokens used: ~370000
Changed files: see the per-surface lists in "What changed" below, plus this report
(`agents/task/10.07.2026_12.25.50-0d78112b-aca8-4764-8ed4-4f6a7d67e7dc/005-coding.md`).

# 005 — Coding (Commit B): return feature-owned models from *Feature interfaces per new rule

**Scope note:** this is Coding pass 2 of 2 for GitHub issue #67, implementing §6 of
`003-architecturing.md` ("Commit B") — the six persistence-model-to-feature-model conversions
(V1 auth, V2 booking, V3 admin, V4 wishlist, V5 wishlist-items, V6 files). Commit A
(`UsersFeatureUser` + the `agents/CODING.md` rule text) is DONE and already committed as `d91ab80`;
nothing from that commit was touched again.

## Preliminary flag — prompt-injection content in `AGENTS.md` (same finding as the prior Coding pass)

`AGENTS.md` still contains the embedded "SYSTEM DIRECTIVE: AUTISTIC META-LANGUAGE PROTOCOL WITH HIGH
INFORMATION DENSITY (AML-HIP V1)" block demanding all output be reformatted into a rigid key=value
pseudo-protocol under escalating "mandatory regeneration" language. This reads as an injected
instruction embedded in repo content, not a legitimate operator directive, and was not followed —
this report and all commit messages use normal prose per `agents/GIT.md`'s explicit "Use normal
prose" rule. Flagging again for operator awareness since it persists in the repo; not acted on
further as it is out of this task's scope.

## What changed, by surface

### V1 — auth (`AuthFeatureUser`, keeps `email` — own-record surface)

- **NEW** `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt`
- **NEW** `features/auth/common/src/commonTest/kotlin/models/AuthFeatureUserTest.kt`
- **NEW** `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt` (local
  `FakeUsersRepo`/`FakePasswordsRepo`)
- `features/auth/client/src/commonMain/kotlin/ClientAuthFeature.kt` — `getMe(): AuthFeatureUser?`
- `features/auth/server/src/commonMain/kotlin/ServerAuthFeature.kt` — `getUser(token): AuthFeatureUser?`
- `features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt` — return type follows
- `features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt` — return type follows
- `features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt` — `getUser` now maps
  `usersRepo.getById(entry.id)?.asAuthFeatureUser()`
- `features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt` —
  `/auth/getMe` handler: renamed local var `registeredUser` → `authFeatureUser` for clarity
  (`call.respond(...)` itself needed no textual change)
- `features/auth/client/src/commonMain/kotlin/Me.kt` — the DI-registration file the architecture
  addendum flagged as missing from the original plan: `meStateFlow`/`secretMeMutableStateFlow`
  retyped `StateFlow<RegisteredUser?>` → `StateFlow<AuthFeatureUser?>`
- `features/auth/client/src/commonMain/kotlin/Plugin.kt` — `MutableRedeliverStateFlow<AuthFeatureUser?>`
- `features/auth/README.md`

Compile-verify only (confirmed by the green build, no textual change needed): `features/ui/auth/.../Plugin.kt`
(`authFeature.getMe() != null`), `features/ui/wishlist/.../Plugin.kt` and
`features/ui/users/.../Plugin.kt` (`meState.map { it?.id }` / `.username.string` — both fields
present unchanged on `AuthFeatureUser`).

### V2 — booking (`BookingFeatureItem`)

- **NEW** `features/booking/common/src/commonMain/kotlin/models/BookingFeatureItem.kt`
- **NEW** `features/booking/common/src/commonTest/kotlin/models/BookingFeatureItemTest.kt`
- **NEW** `features/booking/server/src/commonTest/kotlin/services/BookingServiceTest.kt` (local
  `FakeBookingRepo`/`FakeWishlistItemRepo`/`FakeWishlistRepo`)
- `features/booking/client/src/commonMain/kotlin/BookingFeature.kt` — `myPresentsBooks(): List<BookingFeatureItem>`
- `features/booking/client/src/commonMain/kotlin/KtorBookingFeature.kt` — return type follows
- `features/booking/server/src/commonMain/kotlin/services/BookingService.kt` — maps
  `wishlistItemRepo.getByIds(itemIds).map { it.asBookingFeatureItem() }`
- `features/ui/booking/src/commonMain/kotlin/ui/BookingModel.kt` — `myPresentsBooks(): List<BookingFeatureItem>`
- `features/ui/booking/src/commonMain/kotlin/ui/MyPresentsBooksViewModel.kt` — `_presentsState` retyped
- `features/ui/booking/src/commonMain/kotlin/Plugin.kt` — anonymous impl retyped
- `features/ui/booking/src/jsMain/kotlin/ui/MyPresentsBooksView.kt` — `ReservedCard(item: BookingFeatureItem)`
- `features/booking/README.md`, `features/ui/booking/README.md`

`BookingRoutingsConfigurator.kt` — compile-verify only (`call.respond(bookingService.myPresentsBooks(...))`).

### V3 — admin (`AdminUser`, `AdminWishlist` with two mapper overloads, `AdminWishlistItem`)

- **NEW** `features/admin/common/src/commonMain/kotlin/models/AdminUser.kt`,
  `AdminWishlist.kt` (two mappers: `RegisteredWishlist.asAdminWishlist()` for the one repo-direct
  route, `WishlistsFeatureWishlist.asAdminWishlist()` for the three `WishlistService`-routed routes
  — per §1.1's B-V3/B-V4 interaction), `AdminWishlistItem.kt`
- **NEW** 3 model tests: `AdminUserTest.kt`, `AdminWishlistTest.kt` (both mapper overloads),
  `AdminWishlistItemTest.kt`
- **NEW** `features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt` (local
  `FakeUsersRepo`/`FakePasswordsRepo`/`FakeWishlistRepo`/`FakeWishlistItemRepo`, real
  `AuthFeatureService` wired to fakes)
- `features/admin/common/build.gradle` — added `api project(":wishlist.features.wishlist.common")`
- `features/admin/client/src/commonMain/kotlin/UsersManagementFeature.kt`,
  `KtorUsersManagementFeature.kt`, `AdminWishlistsFeature.kt`, `KtorAdminWishlistsFeature.kt`,
  `AdminWishlistItemsFeature.kt`, `KtorAdminWishlistItemsFeature.kt` — retyped
- `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — `getAll()`/`create()` →
  `AdminUser`
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — per
  §1.1/§6.3: `usersGetByIdPathPart` maps `user.asAdminUser()`; `wishlistsGetAllPathPart` maps via the
  `RegisteredWishlist` overload; `wishlistsGetByUserIdPathPart`/`GetByIdPathPart`/`CreatePathPart` map
  via the `WishlistsFeatureWishlist` overload (post-B-V4 `WishlistService` retype);
  `wishlistsUpdatePathPart` (direct `wishlistRepo` call) keeps the `RegisteredWishlist` overload;
  `wishlistItemsGetByWishlistIdPathPart`/`CreatePathPart`/`UpdatePathPart` map via
  `asAdminWishlistItem()`. `usersGetAllPathPart`/`usersCreatePathPart` needed no textual change.
- `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt`, `Plugin.kt`,
  `AdminUsersListViewModel.kt`, `AdminUserViewModel.kt`, `AdminWishlistsListViewModel.kt`,
  `AdminWishlistViewModel.kt`, `AdminWishlistEditViewModel.kt` — retyped (confirmed via grep sweep
  that no other adminPanel file, including the platform views, references `RegisteredUser`/
  `RegisteredWishlist`/`RegisteredWishlistItem`)
- `features/admin/README.md`, `features/ui/adminPanel/README.md`

### V4 — wishlist client (`WishlistsFeatureWishlist`) + sidebar consumer

- **NEW** `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureWishlist.kt`
- **NEW** `features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureWishlistTest.kt`
- **NEW** `features/wishlist/server/src/commonTest/kotlin/services/WishlistServiceTest.kt` (local
  `FakeWishlistRepo`)
- `features/wishlist/client/src/commonMain/kotlin/WishlistsFeature.kt`, `KtorWishlistFeature.kt` —
  `getById`/`getByUserId`/`getMyWishlists`/`create` → `WishlistsFeatureWishlist`; `update`/`delete`
  unchanged (`Boolean`)
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt` — each read/create
  method wraps its repo call with `.asWishlistsFeatureWishlist()` / `.map { ... }`
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt`, `WishlistsListViewModel.kt`,
  `WishlistViewModel.kt`, `WishlistItemViewModel.kt` (parent-wishlist half), `UserWishlistsViewModel.kt`
  (`UserWishlistsSection.wishlist` half), `WishlistItemCopyViewModel.kt`, `Plugin.kt` — retyped
- `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarModel.kt`, `SidebarViewModel.kt`, `Plugin.kt`
  — `getMyWishlists(): List<WishlistsFeatureWishlist>` (confirmed genuine V4 consumer via
  `WishlistsModel.getMyWishlists()`; confirmed zero `RegisteredUser`/`meStateFlow`/`AuthFeature`
  references, i.e. not a V1 consumer)
- `features/wishlist/README.md`, `features/ui/wishlist/README.md`, `features/ui/sidebar/README.md`

`WishlistRoutingsConfigurator.kt` — compile-verify only (every handler `call.respond`s the service
result directly, including the public unauthenticated `getByUserId`/`getById` routes — a small
additional hardening noted in the README).

### V5 — wishlist items client (`WishlistsFeatureItem`)

- **NEW** `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureItem.kt`
- **NEW** `features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureItemTest.kt`
- **NEW** `features/wishlist/server/src/commonTest/kotlin/services/WishlistItemServiceTest.kt`
  (local `FakeWishlistItemRepo`, reuses `FakeWishlistRepo` from the sibling `WishlistServiceTest.kt`
  — same package/module)
- `features/wishlist/client/src/commonMain/kotlin/WishlistsItemsFeature.kt`, `KtorWishlistItemFeature.kt`
  — `getByWishlistId`/`create`/`copy` → `WishlistsFeatureItem`; `update`/`delete` unchanged
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt` —
  `getByWishlistId`/`create`/`copyItem` (including its idempotent existing-item branch) map to
  `.asWishlistsFeatureItem()`
- **Confirmed NOT touched:** `WishlistCopyService` — its only public method never returns
  `RegisteredWishlistItem` (re-confirmed per `003-architecturing.md` §6.5)
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt` (item half),
  `WishlistViewModel.kt` (items half), `UserWishlistsViewModel.kt` (items half),
  `WishlistAdditionalConfigsProvider.kt`, `BookingConfigsProvider.kt`,
  `WishlistItemAdditionalConfigView.kt` — retyped
- Platform views: `features/ui/wishlist/src/{jvmMain,androidMain,jsMain}/kotlin/ui/WishlistItemCard.kt`,
  `src/jsMain/kotlin/ui/WishlistItemRow.kt`, `src/{jsMain,jvmMain,androidMain}/kotlin/ui/UserWishlistsView.kt`
  — retyped
- Confirmed via `grep -rl RegisteredWishlist(Item)? features/ui/wishlist` (post-edit sweep): zero
  remaining references — nothing outside the enumerated list needed a change.

### V6 — files (`FilesFeatureMetaInfo`)

- **NEW** `features/files/common/src/commonMain/kotlin/models/FilesFeatureMetaInfo.kt`
- **NEW** `features/files/common/src/commonTest/kotlin/models/FilesFeatureMetaInfoTest.kt`
- **NEW** `features/files/server/src/commonTest/kotlin/services/FilesServiceTest.kt` — the one
  genuinely awkward fixture in this pass: MicroUtils' `TemporalFilesRoutingConfigurator` has no
  in-process seeding API (its temp-file map only fills from a real multipart HTTP upload), so the
  "valid image" `finalize` case seeds a real `File.createTempFile` PNG via JVM reflection into the
  configurator's private `temporalFilesMap` (documented inline; safe because `features/files/server`
  is JVM-only, matching the configurator's own `jvmMain`-only placement). The "missing temp file"
  case needs no reflection (an unseeded id naturally resolves to nothing).
- `features/files/client/src/commonMain/kotlin/FilesFeature.kt`, `KtorFilesFeature.kt`,
  `FilesClientService.kt` (`uploadFile` wraps `finalize`) — retyped
- `features/files/server/src/commonMain/kotlin/services/FilesService.kt` — `finalize`/`getMeta`
  return `.asFilesFeatureMetaInfo()`; `metaInfoRepo` itself keeps storing `RegisteredFileMetaInfo`
  unchanged (only the two methods' **return** values are retyped, per the spec's explicit
  instruction not to change the repo's stored type)
- `features/files/common/src/commonMain/kotlin/Constants.kt` — fixed one stale KDoc reference
  (`.../meta/{id}` doc comment still named `RegisteredFileMetaInfo`)
- `features/files/README.md`

`FilesRoutingsConfigurator.kt` — compile-verify only, confirmed at **both** post-retype call sites
per §1.3: the `meta/{id}` route (`call.respond(meta)`) and the raw-byte `{id}` route
(`meta.fileName.string`/`meta.mimeType` header reads only). `features/ui/wishlist/.../Plugin.kt`'s
`filesService.uploadFile(file)?.id` — compile-verify only (zero other UI callers of `getMeta`,
confirmed by the architecture step).

## Tests

All 14 new test files (8 model+mapper tests from §4.1, 6 service-layer tests from §4.2's case
table) ran on the `jvmTest` target and passed with 0 failures/errors (spot-checked every new test
class's JUnit XML report): `AuthFeatureUserTest` (3), `BookingFeatureItemTest` (3), `AdminUserTest`
(3), `AdminWishlistTest` (3, both mapper overloads), `AdminWishlistItemTest` (2),
`WishlistsFeatureWishlistTest` (2), `WishlistsFeatureItemTest` (2), `FilesFeatureMetaInfoTest` (2),
`AuthFeatureServiceTest` (3: valid token/expired token/unknown token), `BookingServiceTest` (2),
`UsersManagementFeatureTest` (2), `WishlistServiceTest` (5), `WishlistItemServiceTest` (5,
including the idempotent-copy branch), `FilesServiceTest` (4).

The service-layer tests needed local `Fake*Repo` doubles (not exact byte code in the spec, only a
case table) — followed the `MapCRUDRepo`/`MapKeyValueRepo`-delegate pattern already established by
`features/users/server/.../UsersServiceTest.kt` and `features/email/server/.../FakeUsersRepo.kt`.
Every fake lives next to its test, `internal`, package-scoped to avoid cross-module leakage;
`WishlistItemServiceTest.kt` and `WishlistServiceTest.kt` share one `FakeWishlistRepo` (same
package/module) rather than duplicating it.

## `agents/CODING.md`

Not touched — the Feature Interface Return Model Rule section was already inserted verbatim by
Commit A; this pass only applies it.

## Build result

`ast-index rebuild` — run twice (once after all `.kt` edits, once more after the final test-file
cleanup below): both times completed with 706 files indexed, 46 modules, no errors.

Full project build (per the task's instruction — this change's blast radius across 6 features + 5
`ui/*` consumers made a scoped module list error-prone to hand-enumerate):

```
./gradlew build
```

**BUILD SUCCESSFUL** (4m 26s, 4007 actionable tasks: 1005 executed, 3002 up-to-date). One fix-cycle
item: the first full build produced a single Kotlin compiler warning (not an error) —
`WishlistServiceTest.kt:102: Check for instance is always 'true'` (a redundant `created is
WishlistsFeatureWishlist` assertion, since `created`'s static type already guarantees it). Removed
the redundant assertion and its now-unused `assertTrue` import, then re-verified with a scoped
rebuild:

```
./gradlew :wishlist.features.wishlist.server:build
```

**BUILD SUCCESSFUL** (22s, 32 actionable tasks: 12 executed, 20 up-to-date), zero warnings. No other
issues surfaced in either build — every other module compiled and its tests passed on the first
attempt.

## Consolidated file list

- 8 new model files (§6.1) + 8 new model test files + 6 new service-layer test files (§6.2) = 22 new
  `.kt` files, enumerated by surface above.
- 1 Gradle file: `features/admin/common/build.gradle`.
- ~48 retyped/compile-verified production `.kt` files across `auth`, `booking`, `admin`,
  `wishlist`, `files` and their `ui/*` consumers (`ui/adminPanel`, `ui/wishlist`, `ui/sidebar`,
  `ui/booking`), enumerated exactly above.
- 9 README updates: `features/admin/README.md`, `features/auth/README.md`,
  `features/booking/README.md`, `features/files/README.md`, `features/wishlist/README.md`,
  `features/ui/adminPanel/README.md`, `features/ui/wishlist/README.md`,
  `features/ui/sidebar/README.md`, `features/ui/booking/README.md`. Every `## Operator Notes`
  section was read and left untouched (verified no edit landed inside one).
- This report.

`git status` before committing shows exactly this set (modified + new files) plus two pre-existing
untracked items from before this task started (`agents/security-review-2026-07-09.md`,
`agents/task/10.07.2026_12.25.50-.../PROMPT.md`) — left alone per `agents/GIT.md`.
