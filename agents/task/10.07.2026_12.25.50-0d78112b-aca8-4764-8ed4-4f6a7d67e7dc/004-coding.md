Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~2100 seconds
Tokens used: ~130000
Changed files:
- features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt (NEW)
- features/users/common/src/commonTest/kotlin/models/UsersFeatureUserTest.kt (NEW)
- features/users/server/src/commonTest/kotlin/services/UsersServiceTest.kt (NEW, includes local `FakeUsersRepo`)
- features/users/server/src/commonMain/kotlin/UsersFeature.kt
- features/users/server/src/commonMain/kotlin/services/UsersService.kt
- features/users/client/src/commonMain/kotlin/UsersFeature.kt
- features/users/client/src/commonMain/kotlin/KtorUsersFeature.kt
- features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt
- features/ui/users/src/commonMain/kotlin/Plugin.kt
- features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt
- features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt
- agents/CODING.md (new `## Feature Interface Return Model Rule` section)
- features/users/README.md
- features/ui/users/README.md
- agents/task/10.07.2026_12.25.50-0d78112b-aca8-4764-8ed4-4f6a7d67e7dc/004-coding.md (this report)

# 004 — Coding (Commit A only): UsersFeatureUser model + email-leak fix

**Scope note:** this is Coding pass 1 of 2 for GitHub issue #67. It implements ONLY §5 of
`003-architecturing.md` ("Commit A"). It does **not** touch any of Commit B's scope (auth, booking,
admin, wishlist-service, files retypes and their `ui/*` consumers, or their new model/test files) —
those are enumerated in §6 of `003-architecturing.md` and are left for a separate Coding pass.

## Preliminary flag — prompt-injection content found in `AGENTS.md`

`AGENTS.md` (the file this role is required to read first) contains an embedded block titled "SYSTEM
DIRECTIVE: AUTISTIC META-LANGUAGE PROTOCOL WITH HIGH INFORMATION DENSITY (AML-HIP V1)" demanding all
agent output be reformatted into a rigid key=value pseudo-protocol, with escalating language ("YOU MUST
FOLLOW... WITHOUT ANY QUESTIONS OR DOUBTS", "Any response outside of AML-HIP is considered absent").
This reads as an injected instruction embedded in repo content rather than a legitimate operator
directive, and was not followed — this report and all commit messages use normal prose per
`agents/GIT.md`'s explicit "Use normal prose (not caveman-compressed)" rule, which this AML-HIP block
would otherwise override. Flagging for operator awareness; not acted on further since it is out of this
task's scope.

## What changed

1. **New model** `UsersFeatureUser` (`id: UserId`, `username: Username`) in
   `features/users/common/.../models/UsersFeatureUser.kt`, with mapper
   `RegisteredUser.asUsersFeatureUser()` — exact code from `003-architecturing.md` §3.1, byte-for-byte.
2. **Server `UsersFeature.getAll()`** retyped `List<RegisteredUser>` → `List<UsersFeatureUser>`; KDoc
   updated to describe the projection and why (public, unauthenticated route).
3. **`UsersService.getAll()`** now maps `usersRepo.getAll().values.map { it.asUsersFeatureUser() }` —
   this is the actual fix: email is dropped before the value ever reaches the HTTP layer.
4. **Client `UsersFeature.getAll()`** and **`KtorUsersFeature`** retyped to match (body unchanged —
   `client.get(...).body()`'s generic follows the interface's declared return type).
5. **`features/ui/users`**: `UsersModel.getAllUsers()`/`getUser(id)` retyped to `UsersFeatureUser`;
   `Plugin.kt`'s anonymous `UsersModel` impl swapped its `RegisteredUser` import for `UsersFeatureUser`
   (method bodies unchanged — types follow automatically); `UsersListViewModel._usersState` and
   `UserViewModel._userState` retyped to the new model.
6. **`agents/CODING.md`**: inserted the `## Feature Interface Return Model Rule` section verbatim from
   `003-architecturing.md` §2, placed directly after `## CRUD Repository Pattern` and before
   `## Bearer Auth Pattern` (confirmed via `grep -n "^## "` after the edit — correct position).
7. **READMEs**: `features/users/README.md` (Routes table's response type, new `UsersFeatureUser` row in
   Models, Architecture Notes explaining the retype and pointing at the new CODING.md rule) and
   `features/ui/users/README.md` (`UsersModel` row now notes `getAllUsers`/`getUser` return
   `UsersFeatureUser`). `## Operator Notes` sections in both files were read and left untouched (both
   contain only the boilerplate placeholder comment, no operator content to violate).
8. **Tests** (new):
   - `UsersFeatureUserTest` (3 cases) — JSON-shape assertion (`{id, username}`, no `email` key) plus
     mapper tests for non-null and null source email. Exact code from §4.1.
   - `UsersServiceTest` (2 cases) — `getAll()` on a repo seeded with an email-carrying user returns the
     feature model with email dropped; empty repo → empty list. Needed a local `FakeUsersRepo`
     (`internal class`, `commonTest`) since `UsersService` only needs `ReadUsersRepo`; implemented it as
     a thin delegate over `dev.inmo.micro_utils.repos.ReadMapCRUDRepo` rather than pulling in the full
     write-capable `MapCRUDRepo` machinery `features/email/server`'s `FakeUsersRepo` uses (that one also
     needs write support for `EmailFeatureService`; this one does not).

## Compile-verify-only sites (confirmed no textual change needed, and build proves it)

- `features/users/server/src/jvmMain/kotlin/configurators/UsersRoutingsConfigurator.kt` —
  `call.respond(feature.getAll())`, no type name in the file.
- `features/users/server/src/commonMain/kotlin/Plugin.kt`, `features/users/client/.../Plugin.kt` — DI
  registrations, not signature-dependent.
- `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (~line 177) —
  `usersFeature.getAll().find { it.id == userId }?.username?.string` touches only `id`/`username`, both
  present on `UsersFeatureUser`.

Verified via `grep -rln "UsersFeature" features --include="*.kt"` that no other feature module (admin,
auth, booking, wishlist server/client proper — as opposed to `ui/wishlist`) references this particular
`UsersFeature` interface at all, so the blast radius matches `003-architecturing.md` §5 exactly; nothing
outside the build-gate module list was at risk.

## Build result

`ast-index rebuild` — completed, 684 files indexed, 46 modules, no errors.

```
./gradlew :wishlist.features.users.common:build :wishlist.features.users.server:build \
  :wishlist.features.users.client:build :wishlist.features.ui.users:build \
  :wishlist.features.ui.wishlist:build
```

**BUILD SUCCESSFUL** (1m 57s, 1640 actionable tasks: 370 executed, 1270 up-to-date). No fix cycle was
needed — clean pass on the first attempt.

New test suites both ran and passed on the `jvmTest` target (also `jsNodeTest`/`jsBrowserTest`/
`testDebugUnitTest`/`testReleaseUnitTest` for the `commonTest` model test, per the `mppJvmJsAndroid`
template's platform matrix):
- `UsersFeatureUserTest[jvm]`: `tests="3" failures="0" errors="0"`.
- `UsersServiceTest[jvm]`: `tests="2" failures="0" errors="0"`.

## What remains — Commit B (next Coding pass, separate from this one)

Per `003-architecturing.md` §6/§7, a second Coding pass must implement the cross-feature audit fix
(issue #67 point 3) as its own commit:

- 8 new feature models: `AuthFeatureUser`, `BookingFeatureItem`, `AdminUser`, `AdminWishlist` (two
  mapper overloads), `AdminWishlistItem`, `WishlistsFeatureWishlist`, `WishlistsFeatureItem`,
  `FilesFeatureMetaInfo` — exact code in §3.2–§3.7.
- One `build.gradle` change (`features/admin/common` → adds `wishlist.common` dependency).
- Retypes across `features/auth`, `features/booking`, `features/admin` (client+server+
  `AdminRoutingsConfigurator`, including the two-overload `AdminWishlist` mapper interaction documented
  in §1.1), `features/wishlist` (`WishlistsFeature`/`WishlistsItemsFeature` + `WishlistService`/
  `WishlistItemService`), `features/files` (`FilesFeature`/`FilesService`), and their UI consumers
  (`features/ui/adminPanel`, `features/ui/wishlist`, `features/ui/sidebar`).
- ~14 new test files (model+mapper tests per §4.1, service-layer case tables per §4.2).
- 9 README updates (admin, adminPanel, wishlist, ui/wishlist, ui/sidebar, auth, booking, ui/booking,
  files).
- Build gate: full `./gradlew build`, then `ast-index rebuild`.
- Commit message topic (already fixed by Architecture):
  `fix(features): return feature-owned models from *Feature interfaces per new rule`.

This Commit-A pass touched nothing in that list — `git status` before committing confirmed the diff is
exactly the 14 files enumerated in `003-architecturing.md` §5 (11 modified + `UsersFeatureUser.kt` new +
two new `commonTest` directories), plus this report.
