Model: fable (claude-fable-5)
Execution time: ~540 seconds
Tokens used: ~90000
Changed files: agents/task/10.07.2026_12.25.50-0d78112b-aca8-4764-8ed4-4f6a7d67e7dc/001-planning.md

# 001 — Planning (GitHub issue #67: Add special model for UsersFeature)

**Status: BLOCKED — needs operator answer** (one question, multiple choice, recommendation provided; full plan below is otherwise complete and Architecture can start immediately after the answer).

Model-choice justification: `fable` is the top entry of the planning-role model priority list (fable / opus / sonnet) relayed by the orchestrator.

---

## 1. Task understanding

Issue #67 has three points:

1. `UsersFeature.getAll()` (both the client and server interfaces) returns `List<RegisteredUser>`. `RegisteredUser` carries `email: Email?`, and the `GET /api/users/getAll` route is **public and unauthenticated** — every registered user's email address is currently leaked to any anonymous caller. Fix: introduce a dedicated model (issue suggests `UsersFeatureUser`) without `email` and return it instead.
2. Write a general rule into `agents/*.md`: any data returned from a `*Feature` interface (excluding identifiers, primitives, and non-project library types) must be covered by a Feature-related model.
3. In a **separate commit**, fix all other features that violate the rule from point 2.

Severity note: point 1 is a real data-leak security fix, not a style cleanup.

## 2. Investigation summary

Search performed with `ast-index` (SQL queries against the symbol index, `refs`, plus targeted file reads). Index healthy (682 files / 4201 symbols).

### 2.1 Point 1 — full blast radius of the `UsersFeature` return-type change

Interfaces and implementations:

| File | Change |
|---|---|
| `features/users/server/src/commonMain/kotlin/UsersFeature.kt` | `getAll(): List<RegisteredUser>` → `List<UsersFeatureUser>`; KDoc update |
| `features/users/server/src/commonMain/kotlin/services/UsersService.kt` | map `usersRepo.getAll().values` → new model |
| `features/users/client/src/commonMain/kotlin/UsersFeature.kt` | return type + KDoc |
| `features/users/client/src/commonMain/kotlin/KtorUsersFeature.kt` | return type (`.body()` infers from signature) |
| `features/users/server/src/jvmMain/kotlin/configurators/UsersRoutingsConfigurator.kt` | **no code change** — `call.respond(feature.getAll())` serializes the new type automatically |
| `features/users/server/src/commonMain/kotlin/Plugin.kt`, `features/users/client/src/commonMain/kotlin/Plugin.kt` | **no change** — DI registrations are not signature-dependent |

Call sites (found via `ast-index refs UsersFeature` + grep of the two consumer plugins):

| File | Change |
|---|---|
| `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt` | `getAllUsers()` / `getUser(id)` return types → `UsersFeatureUser`; KDocs |
| `features/ui/users/src/commonMain/kotlin/Plugin.kt` | anonymous `UsersModel` impl return types + import |
| `features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt` | `_usersState: MutableRedeliverStateFlow<List<RegisteredUser>>` → `List<UsersFeatureUser>` |
| `features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt` | `_userState: MutableRedeliverStateFlow<RegisteredUser?>` → `UsersFeatureUser?` |
| `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (line ~177) | **no change** — `usersFeature.getAll().find { it.id == userId }?.username?.string` only touches `id`/`username`, structurally compatible; must be compile-verified |

Verified: no platform view or ViewModel in `features/ui/users` reads `.email` from the public list (grep found zero `.email` property reads across `features/ui` — the only email consumers are the `Email` type imports in `features/ui/adminPanel`, which flow through `EmailFeature.setMyEmail`, unrelated to this change). The mutation paths in `UsersModel` (`updateUsername`, `setPassword`, `deleteUser`) go through the admin feature, not `UsersFeature`, and are unaffected.

Serialization compatibility: the endpoint's JSON changes from `[{"id":..,"username":..,"email":..}]` to `[{"id":..,"username":..}]`. Client and server are shipped together from this repo, and old clients parse the new payload fine anyway (`email` has a default). No migration concern.

### 2.2 New model — final decision

- **Name:** `UsersFeatureUser` (issue-suggested; establishes the `<FeatureInterface><Entity>` naming precedent).
- **Location:** `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt` — must live in the feature's `common` module because client and server share the wire format (same reason `RegisteredUser` lives there). It remains a users-feature-owned model.
- **Shape:**
  - `@Serializable data class UsersFeatureUser(val id: UserId, val username: Username)`
  - Deliberately does **not** implement the sealed `User` interface — `User` requires `val email: Email?`, which would reintroduce the leaked field.
  - No inline value classes are introduced, so the value-class property-naming rule is not triggered; full KDoc per `agents/CODING.md` (class purpose + `@property` tags, no placeholders).
  - Plus a mapping extension in the same file: `fun RegisteredUser.asUsersFeatureUser(): UsersFeatureUser = UsersFeatureUser(id = id, username = username)` (KDoc'd), used by `UsersService`.

### 2.3 Point 2 — rule placement and wording

**Placement decision:** `agents/CODING.md`, as a new section titled `## Feature Interface Return Model Rule`, inserted directly after the `## CRUD Repository Pattern` section (it is a model-layer coding convention and directly references the CRUD pattern's `New*`/`Registered*` types). `agents/ARCHITECTURE.md` is not touched — the rule is a coding convention, and duplicating it in two files invites drift.

**Proposed exact wording** (final text for Coding to paste; written for the recommended Option A of the open question — see section 4; if the operator picks Option B, drop the second bullet's "even the feature's own" clause and the bullet explicitly forbidding own-repo-model returns):

```markdown
## Feature Interface Return Model Rule

Any data returned from a `*Feature` interface method (client, server, or common variant) MUST be
covered by a model that belongs to that feature's own model set and is designed for that feature's
API surface — a "feature model".

- **Exempt from the rule** (may be returned as-is, alone or inside collections/nullable wrappers):
  identifiers (inline value-class ids such as `UserId`, `FileId`), primitive types (`String`,
  `Boolean`, numbers, `Unit`), and non-project library types (streams, flows, etc.). Collections
  (`List`, `Map`) and `Flow`/`StateFlow` of allowed types are allowed.
- **Repo/persistence-layer models — the `New*`/`Registered*` CRUD entities from the CRUD Repository
  Pattern — MUST NOT be returned by `*Feature` methods, not even the feature's own.** A persistence
  model's field set evolves with storage needs; reusing it as an API return type silently widens the
  wire surface whenever a field is added (this is exactly how `RegisteredUser.email` leaked through
  the public `UsersFeature.getAll`).
- A feature model exposes exactly the fields the feature intends to expose, is `@Serializable`, and
  lives in the feature's `common` module so client and server share one wire format. Naming: either
  `<FeatureInterfaceName><Entity>` (e.g. `UsersFeatureUser`) or a purpose-named wire DTO
  (e.g. `BookingState`, `AuthCredentials`).
- Fields of a feature model may reference other features' identifiers and small value types
  (e.g. `UserId`, `Priority`, `Amount`), but must not embed another feature's persistence entity
  wholesale.
- The rule covers **return types** of `*Feature` methods. Input parameters are out of its scope
  (though the same instinct applies — see `NewWishlistInFeature` for the input-side precedent).
```

Existing in-repo precedents supporting this rule (found during investigation): `NewWishlistInFeature` (feature-boundary input model without `UserId`), `BookingState` (deliberately booker-anonymous wire DTO), `AuthCredentials` (auth wire DTO). The codebase already does this where it mattered — the rule generalizes it.

**Scope decisions made by Planning (documented, not escalated):**
- Rule covers return types only ("data returned" — issue's literal wording). Admin's `update(id, NewUser)` input parameter is therefore not a violation.
- Rule text lives in `CODING.md` only.
- Feature models that legitimately expose `email` (admin root-only surfaces, the authenticated caller's own "me" record) keep it deliberately, with KDoc stating why.

### 2.4 Point 3 — complete `*Feature` audit

Every `*Feature` interface in the repo (enumerated via ast-index symbol query over `features/*/client`, `features/*/server`, `features/*/common`; admin's server side uses classes rather than interfaces — included because they are the same capability surface):

**Compliant under any reading (no action):**

| Interface (module) | Methods → returns | Verdict |
|---|---|---|
| `SampleFeature` (sample client+server) | `getSampleText(): String` | primitive — OK |
| `EchoFeature` (common client+server) | `getEcho(): String` | primitive — OK |
| `EmailFeature` (email client+server) | `isFeatureEnabled/sendTestEmail/setMyEmail → Boolean` | primitives — OK |
| `WishlistCopyFeature` (wishlist client) | `enqueueCopy(...): Boolean` | primitive — OK |
| `AuthFeature` (auth common) | `login/refresh/register → AuthCredentials?`, `isRegistrationAvailable → Boolean` | `AuthCredentials` is auth's own wire DTO — OK |
| `CurrencyFeature` (currency common) | `getCurrencies → List<CurrencyInfo>`, `getRates → CurrencyRates?`, `isFeatureEnabled → Boolean` | currency's own API models, no repo behind them — OK |
| `BookingFeature.getState` (booking client) | `getState → BookingState?` | booking's own wire DTO (exemplary) — OK |
| `FilesFeature.getAvatar/setAvatar` (files client) | `FileId?` / `Boolean` | identifier/primitive — OK |
| `AdminFeature` (admin client iface + server class) | `val`s exposing sub-feature interfaces | not data — OK |
| `EmailsService` (email server) | internal server service, not a `*Feature` interface | out of scope |

**Violations under BOTH readings (cross-feature persistence models):**

| # | Surface | Offending returns | Notes |
|---|---|---|---|
| V1 | `ClientAuthFeature.getMe(): RegisteredUser?` (auth client), `ServerAuthFeature.getUser(Token): RegisteredUser?` (auth server), plus the `meStateFlow: StateFlow<RegisteredUser?>` DI surface (`features/auth/client/Me.kt`) fed from `getMe` | users' persistence model returned by the auth feature over `GET /auth/me` | self-record, so the email exposure itself is legitimate; the model ownership is the violation |
| V2 | `BookingFeature.myPresentsBooks(): List<RegisteredWishlistItem>` (booking client) + server route in `BookingRoutingsConfigurator`/`BookingService` | wishlist's persistence model returned by the booking feature | |
| V3 | Admin: `UsersManagementFeature` (client iface + server class) `getAll/getById/create → RegisteredUser`; `AdminWishlistsFeature` `getAll/getByUserId/getById/create → RegisteredWishlist`; `AdminWishlistItemsFeature` `getByWishlistId/create → RegisteredWishlistItem` | users'/wishlist's persistence models returned by the admin feature | root-only surface (per `features/admin/README.md` Operator Notes), so email in the replacement `AdminUser` model stays deliberately |

**Violations ONLY under the strict reading (feature returns its own persistence model):**

| # | Surface | Offending returns |
|---|---|---|
| V4 | `WishlistsFeature` (wishlist client): `getById/getByUserId/getMyWishlists/create → RegisteredWishlist` |
| V5 | `WishlistsItemsFeature` (wishlist client): `getByWishlistId/create/copy → RegisteredWishlistItem` |
| V6 | `FilesFeature` (files client): `finalize/getMeta → RegisteredFileMetaInfo?` |

Data-sensitivity check across models: besides `RegisteredUser.email` (point 1), no other persistence model currently carries a field that looks like an unintended leak (`RegisteredWishlist`: id/userId/title/defaultPriceUnits; `RegisteredWishlistItem`: display fields; `RegisteredFileMetaInfo`: fileName/mimeType/size/uploaderId). So V4–V6 are pure decoupling/consistency fixes, while V1–V3 fix cross-feature ownership; only point 1 fixes an actual leak today.

## 3. Rule-interpretation analysis (why this needs one operator answer)

The issue's own example makes the naive "ownership" reading incoherent: `RegisteredUser` **is** the users feature's own model, yet point 1 declares returning it a violation requiring `UsersFeatureUser`. Therefore "Feature-related model" cannot simply mean "any model in the feature's common module" — it must mean "a model designed for the feature's API surface, as opposed to a persistence/CRUD entity". That yields the **strict reading (Option A)**: no `*Feature` method may return a `New*`/`Registered*` CRUD entity, even its own; deliberately designed wire DTOs (`BookingState`, `AuthCredentials`, `CurrencyInfo/CurrencyRates`) already comply.

However, Option A pulls V4–V6 into point 3's scope: near-1:1 duplicate models for `RegisteredWishlist`, `RegisteredWishlistItem` and `RegisteredFileMetaInfo`, plus retyping their entire client/server/UI call-site chains — roughly doubling to tripling commit B versus the ownership-only reading (**Option B**: a feature may return its own models including `Registered*`, never another feature's; scope = V1–V3 only; requires accepting that point 1 is then a special case justified by the auth-boundary leak rather than by the general rule). The wasted-cycle cost of guessing wrong here is large in both directions, and the issue-executor's context explicitly directed Planning to resolve this by asking rather than guessing. Hence BLOCKED.

## 4. Open Questions

**Q1. Scope of the point-2 rule (decides point 3's audit scope). Which reading is intended?**

- **(A) Strict — recommended.** A `*Feature` interface method must never return a persistence/CRUD model (`New*`/`Registered*`), not even one owned by the same feature. Every non-exempt return gets a dedicated feature/wire model. Deliberate wire DTOs that already exist (`BookingState`, `AuthCredentials`, `CurrencyInfo`, `CurrencyRates`) already comply. Point-3 fixes: V1 (auth `getMe`/`getUser`/`meStateFlow`), V2 (booking `myPresentsBooks`), V3 (admin users/wishlists/items surfaces), V4 (`WishlistsFeature`), V5 (`WishlistsItemsFeature`), V6 (`FilesFeature.finalize/getMeta`). This is the reading consistent with the issue's own example (point 1 fixes a feature returning its *own* model), and it prevents future silent leaks of newly added persistence fields — the exact failure mode of the email bug. Cost: several new models with (today) near-identical field sets to the entities they replace, plus mechanical retyping through Ktor impls, services, routing configurators, UI Models/ViewModels.
- **(B) Ownership-only.** A feature may return its own feature's models (including its `Registered*` entities) but never another feature's. Point-3 fixes: V1, V2, V3 only; wishlist/files interfaces stay as they are. Cheaper, but the written rule will then NOT itself forbid what point 1 fixed (users returning users' own model) — the rule text would need an extra clause singling out public/unauthenticated surfaces, and future persistence-field additions to e.g. `RegisteredWishlist` would again silently widen public wire surfaces.
- **(C) Something else** — please state (e.g. strict rule but point-3 fixes limited to V1–V3 for now, with V4–V6 deferred to a follow-up issue).

Recommendation: **A**. If A is chosen, no further input needed. If B or C, the plan's commit-B file list shrinks per the delta noted in section 5.3.

## 5. Concrete plan (handoff to Architecture)

### 5.0 Preliminaries

- Branch `fix/67-users-feature-model` (already checked out; verified current). No pushes, no PR — issue-executor handles that after the cycle.
- `ast-index rebuild` after every `.kt`-changing step; not after markdown-only steps.
- Feature READMEs of every touched feature must be updated by Coding per `agents/CODING.md` (routes/models/architecture-notes sections; never touch Operator Notes). All relevant READMEs verified to exist; only `features/admin/README.md` has Operator Notes (root-only admin access) — the plan respects them (admin models keep `email`, surface stays root-only).

### 5.1 Commit A — point 1 fix + point 2 rule (single commit)

Files (exact list for Coding):

1. **NEW** `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt` — model + `asUsersFeatureUser()` mapper as specified in section 2.2, full KDocs.
2. `features/users/server/src/commonMain/kotlin/UsersFeature.kt` — `getAll(): List<UsersFeatureUser>`; KDoc explains the projection ("public endpoint; returns the feature model, never the persistence entity").
3. `features/users/server/src/commonMain/kotlin/services/UsersService.kt` — `usersRepo.getAll().values.map { it.asUsersFeatureUser() }`.
4. `features/users/client/src/commonMain/kotlin/UsersFeature.kt` — return type + KDoc.
5. `features/users/client/src/commonMain/kotlin/KtorUsersFeature.kt` — return type.
6. `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt` — retype `getAllUsers`/`getUser` + KDocs.
7. `features/ui/users/src/commonMain/kotlin/Plugin.kt` — retype anonymous impl, swap import.
8. `features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt` — retype `_usersState`.
9. `features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt` — retype `_userState`.
10. `agents/CODING.md` — insert the rule section (exact text in 2.3) after `## CRUD Repository Pattern`.
11. `features/users/README.md` — Routes table response → `List<UsersFeatureUser>`; Models table + Architecture Notes updated.
12. `features/ui/users/README.md` — models section updated.

No changes needed (verified): `UsersRoutingsConfigurator`, both users `Plugin.kt` DI files, `features/ui/wishlist/Plugin.kt` (structural compatibility — confirm via compilation).

Build gate: `./gradlew build` (or at minimum `:wishlist.features.users.common:build`, `:wishlist.features.users.server:build`, `:wishlist.features.users.client:build`, `:wishlist.features.ui.users:build`, `:wishlist.features.ui.wishlist:build`), then `ast-index rebuild`.

### 5.2 Commit B — point 3 fixes (separate commit, after operator answers Q1)

Under recommended Option A (primary plan):

**B-V1 auth "me" model.** NEW `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt`: `@Serializable data class AuthFeatureUser(val id: UserId, val username: Username, val email: Email?)` + mapper from `RegisteredUser`. Keeps `email` deliberately: it is the caller's own record (needed for future self-service email prefill; documented in KDoc). `auth/common` already depends on `users/common`; needs a new dependency on `features/email/common` for `Email` (users/common already carries it transitively via its own dependency — Architecture verifies and adds an explicit `api project` if not exposed). Retype: `ClientAuthFeature.getMe`, `KtorAuthFeature`, client `AuthFeatureService`, `ServerAuthFeature.getUser`, server `AuthFeatureService.getUser`, `AuthRoutingsConfigurator` `/auth/me` handler, `Me.kt` (both `StateFlow` helper sets). Consumers to retype/compile-verify: `features/ui/auth`, `features/ui/sidebar`, `features/ui/wishlist` (Plugin, `WishlistsModel`, 4 ViewModels), `features/ui/users` (Plugin `meState` derivations — uses only `id`/`username`, structurally compatible). Caution: verify the bearer-validator path (`BearerAuthenticationConfigurator`) — it uses the server service's `authenticate(token)`, not `getUser`, so it should be unaffected; Architecture confirms.
**B-V2 booking item model.** NEW `features/booking/common/src/commonMain/kotlin/models/BookingFeatureItem.kt` mirroring `RegisteredWishlistItem`'s display fields (`id: WishlistItemId, wishlistId: WishlistId, title, amount, approximatePrice, priceUnits, links, description, priority, imageIds`) + mapper. `booking/common` already depends on `wishlist/common` (verified in build.gradle), so the id/value-type references are fine. Retype: `BookingFeature.myPresentsBooks`, `KtorBookingFeature`, `BookingService`, `BookingRoutingsConfigurator`, `features/ui/booking` (`BookingModel`, `MyPresentsBooksViewModel`, Plugin; platform views access fields structurally).
**B-V3 admin models.** NEW in `features/admin/common/src/commonMain/kotlin/models/`: `AdminUser` (`id, username, email` — root-only surface, email deliberate), `AdminWishlist` (`id, userId, title, defaultPriceUnits`), `AdminWishlistItem` (mirror of item fields) + mappers. `admin/common` needs a dependency on `wishlist/common` (currently only users+auth — verified) and on `email/common` for `Email` (as with auth, check transitivity). Retype: client `UsersManagementFeature`/`AdminWishlistsFeature`/`AdminWishlistItemsFeature` + their three `Ktor*` impls; server `UsersManagementFeature` class + `AdminRoutingsConfigurator` (and any server wishlist/item admin handlers inside it); `features/ui/adminPanel` (`AdminPanelModel`, Plugin, ViewModels, views).
**B-V4/V5 wishlist feature models.** NEW `WishlistsFeatureWishlist` and `WishlistsFeatureItem` in `features/wishlist/common/.../models/` (exact names Architecture's call under the naming convention) + mappers. Retype `WishlistsFeature`, `WishlistsItemsFeature`, `KtorWishlistFeature`, `KtorWishlistItemFeature`, server `WishlistService`/`WishlistItemService`/`WishlistCopyService` (returns flowing to routes), both wishlist routing configurators, `features/ui/wishlist` chain (Model, ViewModels, views), `features/ui/booking` if it renders wishlist data beyond V2's model.
**B-V6 files feature model.** NEW `FilesFeatureMetaInfo` in `features/files/common/.../models/` (`id, fileName, mimeType, size, uploaderId`) + mapper. Retype `FilesFeature.finalize/getMeta`, `KtorFilesFeature`, `FilesClientService` (if it re-exposes the type), server `FilesService` + `FilesRoutingsConfigurator`, consumers of `getMeta`/`finalize` results.
README updates for every touched feature (auth, booking, admin, wishlist, files + the ui/* consumers' READMEs). Build gate: full `./gradlew build`; `ast-index rebuild`.

### 5.3 Delta if operator picks Option B

Commit B shrinks to B-V1 + B-V2 + B-V3. The rule text in commit A drops the "not even the feature's own" bullet and instead states: "a feature may return its own feature models; returning another feature's models is forbidden; public/unauthenticated endpoints must additionally never return persistence entities directly" (the last clause keeps point 1 justified by the written rule). Commit A is otherwise identical.

### 5.4 Ordering + commit protocol

1. Architecture (next step) turns this into the detailed design + test stubs — **it should proceed only after the operator answers Q1**; everything in commit A is Q1-independent, so Architecture may fully specify commit A immediately and hold commit B's breadth until the answer arrives.
2. Coding pass 1 → commit A (`fix(users): hide emails from public users listing behind UsersFeatureUser model; document feature-model rule` + step file). Coding pass 2 → commit B (`fix(features): return feature-owned models from *Feature interfaces per new rule`). Two separate commits are mandatory per the issue's point 3; both stay on this branch, no push.
3. Tests: infrastructure exists (`features/users/common/src/jvmTest`, `features/email/server/src/commonTest`). Candidate cases for Architecture's test stubs: (a) `UsersFeatureUser` serialization contains exactly `id` and `username` keys and no `email`; (b) `UsersService.getAll` maps a repo user with a non-null email to a projection without it (fake `ReadUsersRepo`); (c) `asUsersFeatureUser()` field mapping. Analogous mapper tests for each commit-B model.

## 6. Result

- Task understanding: complete (section 1).
- Open questions: one (Q1, section 4) — **BLOCKED** pending operator answer; recommendation is Option A.
- Final plan: sections 5.1–5.4, ready for the Architecture role; commit A is fully actionable regardless of the Q1 answer.
