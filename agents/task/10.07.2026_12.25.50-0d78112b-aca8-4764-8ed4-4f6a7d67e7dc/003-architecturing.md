Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~2600 seconds
Tokens used: ~155000
Changed files: agents/task/10.07.2026_12.25.50-0d78112b-aca8-4764-8ed4-4f6a7d67e7dc/003-architecturing.md

# 003 — Architecturing (GitHub issue #67: Add special model for UsersFeature)

**Status: READY FOR CODING.** Two independent Coding passes: Commit A (§5) and Commit B (§6). No
operator escalation needed — `002-planning.md` (base + addendum) already resolved the one real
ambiguity (Q1 → strict reading, Option A). This step turns that plan into byte-exact model code, the
final `CODING.md` rule text, concrete test stubs, one clarified cross-surface interaction the addendum
did not spell out (§1.1), and consolidated, ready-to-execute file lists per commit so Coding does not
need to cross-reference `001-planning.md`/`002-planning.md` at all.

All source facts below (exact current signatures, field sets, build.gradle dependency graphs) were
independently re-verified against current source on this branch during this step, in addition to what
`002-planning.md` §7 already re-verified — see §1 for the one correction this pass found.

---

## 1. Flags (read before Coding starts)

Per the Test Planning Requirement in `agents/ARCHITECTURE.md`, anything below that would block Coding
must be raised before hand-off. **Nothing here blocks Coding** — both items are formalization/one
concrete interaction the addendum didn't spell out, not a redesign, and not a genuine testability gap
beyond what already exists repo-wide.

### 1.1 NEW — B-V3/B-V4 interaction: `AdminRoutingsConfigurator`'s wishlist routes flow through the same `WishlistService` that B-V4 retypes

`002-planning.md` §3 (B-V3) says admin's `wishlistsGetByUserIdPathPart`/`wishlistsGetByIdPathPart`/
`wishlistsCreatePathPart` handlers go "via `wishlistService`, map results" but does not spell out *what*
they map from. Verified directly against `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt`
(lines 165–197): these three handlers call `wishlistService.getByUserId(userId)` / `.getById(id)` /
`.create(...)` on the **exact same `WishlistService` instance** that B-V4 retypes to return
`WishlistsFeatureWishlist` instead of `RegisteredWishlist`. So after B-V4 lands (both are in Commit B,
so this is an intra-commit ordering note, not a cross-commit blocker), these three admin handlers
receive `WishlistsFeatureWishlist`/`WishlistsFeatureWishlist?`, not `RegisteredWishlist`/`RegisteredWishlist?`
— the existing single mapper `RegisteredWishlist.asAdminWishlist()` cannot be applied at these three call
sites; a second overload is required. The other three admin wishlist routes
(`wishlistsUpdatePathPart` → `wishlistRepo.update(...)`, wishlist-item routes → `wishlistItemRepo.*`
directly) bypass `WishlistService`/`WishlistItemService` entirely and are unaffected — they keep the
single `Registered*` → `Admin*` mapper.

**Resolution (decided here, not escalated — mechanical, one unambiguous fix):** add a second mapper
overload in the same `AdminWishlist.kt` file: `fun WishlistsFeatureWishlist.asAdminWishlist(): AdminWishlist`.
Exact code is in §3.4. Use it at exactly the three `wishlistService.*`-routed call sites in
`AdminRoutingsConfigurator`; keep `RegisteredWishlist.asAdminWishlist()` for the `wishlistRepo.update(...)`
call site. §6.3 (B-V3 file list) and §6.4 (B-V4 file list) both cross-reference this so neither pass
implements one side without the other — **implement B-V4's `WishlistService` retype in the same commit
before wiring `AdminRoutingsConfigurator`'s three call sites** (already guaranteed since both are inside
the single Commit B).

No equivalent interaction exists for wishlist items: confirmed all three admin wishlist-item routes call
`wishlistItemRepo` directly (not `WishlistItemService`), so B-V5 does not affect `AdminWishlistItem`'s
single `RegisteredWishlistItem.asAdminWishlistItem()` mapper.

### 1.2 `AdminRoutingsConfigurator` / all `*RoutingsConfigurator` inline mappings — no route-level automated test exists or is being added, by design, matching repo precedent

Confirmed by search: **no `ktor-server-test-host` dependency and no `testApplication` usage exists
anywhere in this repo** (`grep -r "testApplication\|ktor-server-test-host"` returns nothing, and
`gradle/libs.versions.toml` declares no Ktor test-host artifact). Every existing `*RoutingsConfigurator`
in the codebase (auth, booking, wishlist, files, admin — pre- and post- this change) is therefore
already untested at the HTTP-routing level; only service/repo layers get unit tests. This is a
pre-existing, repo-wide limitation, **not a new gap introduced by this plan**, and it does not block
Coding: the actual logic these configurators need to exercise for this task — projecting a persistence
model to a feature model — is a pure, fully unit-testable function (the mapper extensions in §3, plus
the retyped service methods in §4.2), and every configurator touched by this plan either needs **zero
textual change** (the retype propagates through `call.respond(...)`'s inferred type) or a one-line
`.map { it.asXxx() }` / `.asXxx()` insertion with no branching logic of its own. Per ARCHITECTURE.md's
Test Planning Requirement this is flagged for visibility, but it is consistent with the rest of the
codebase and does not require an operator decision — proceed to Coding.

### 1.3 Confirmed, not flags — housekeeping facts checked during this pass

- All READMEs the plan needs (both Commit A and Commit B touched features + their `ui/*` counterparts,
  including `features/ui/wishlist/README.md`, which `002-planning.md` §3 left as "to be confirmed by
  Architecture") **exist**. No README needs to be created.
- `features/admin/README.md`'s Operator Notes (verified verbatim): *"Only `root` user must have access
  to the admin panel and features"* / *"If `root` use is not registered - it must be registered in
  server `Plugin`"* — respected: `AdminUser`/`AdminWishlist`/`AdminWishlistItem` change nothing about
  who can reach the admin surface, only what shape the root-only responses carry.
- `WishlistItemService.copyItem` (used by the `POST /wishlistItem/copy` route, i.e. the server side of
  `WishlistsItemsFeature.copy`) **is** in scope for B-V5's retype — confirmed directly from
  `WishlistItemRoutingsConfigurator.kt`'s `wishlistItemCopyPathPart` handler, which calls
  `wishlistItemService.copyItem(request, callerId)` and responds with the raw result. `002-planning.md`
  §3 (B-V5) already lists `copyItem` in `WishlistItemService`'s bullet — confirmed correct, no change.
- `FilesRoutingsConfigurator` touches the post-B-V6 `FilesService.getMeta` result at **two** route
  handlers, not one: `GET /files/meta/{id}` (`call.respond(meta)` — type follows automatically) **and**
  the raw-byte-serving `GET /files/{id}` handler, which calls `getMeta` purely to read
  `meta.fileName.string` / `meta.mimeType` for response headers before streaming bytes — both fields
  exist unchanged on `FilesFeatureMetaInfo`, so this second call site is compile-verify-only too. Neither
  requires a textual change; both are noted explicitly in §6.6 so Coding does not miss the second one.

---

## 2. Feature Interface Return Model Rule — final text for `agents/CODING.md`

`001-planning.md` §2.3's Option-A draft is correct and matches everything `002-planning.md` finalized
(including the `Admin<Entity>` naming exception decided in `002-planning.md` §3 "Shared conventions").
Adopted with two light additions (marked below) — no redesign.

**Insertion point:** `agents/CODING.md`, as a new `## Feature Interface Return Model Rule` section,
inserted **directly after the existing `## CRUD Repository Pattern` section and before `## Bearer Auth
Pattern`** (confirmed both anchor sections exist in `agents/CODING.md` at the point read for this step).

**Exact text to insert (Commit A, file list item 10 in §5):**

```markdown
## Feature Interface Return Model Rule

Any data returned from a `*Feature` interface method (client, server, or common variant) MUST be
covered by a model that belongs to that feature's own model set and is designed for that feature's
API surface — a "feature model". This also covers concrete `*Feature`-named service/capability
classes that play the same role as a formal interface (e.g. admin's `UsersManagementFeature`,
`AdminWishlistsFeature`) — the rule is about the capability surface, not about `interface` vs `class`.

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
  `<FeatureInterfaceName><Entity>` (e.g. `UsersFeatureUser`, `BookingFeatureItem`), a purpose-named
  wire DTO (e.g. `BookingState`, `AuthCredentials`), or a namespace-prefixed entity name when one
  feature module umbrellas several sibling `*Feature` interfaces around the same domain
  (e.g. admin's `AdminUser`/`AdminWishlist`/`AdminWishlistItem`, shared across
  `UsersManagementFeature`/`AdminWishlistsFeature`/`AdminWishlistItemsFeature`).
- Fields of a feature model may reference other features' identifiers and small value types
  (e.g. `UserId`, `Priority`, `Amount`), but must not embed another feature's persistence entity
  wholesale.
- A feature model deliberately keeping a sensitive field (e.g. an authenticated caller's own
  `email` on a "me"/self-record surface) MUST document why in its class KDoc, so a future auditor
  does not mistake it for a re-leak of the exact bug class this rule exists to prevent.
- The rule covers **return types** of `*Feature` methods. Input parameters are out of its scope
  (though the same instinct applies — see `NewWishlistInFeature` for the input-side precedent).
```

(The two additions beyond `001-planning.md`'s draft are the first sentence's "concrete
`*Feature`-named service/capability classes" clarification, the naming bullet's third option, and the
new "document why" bullet — all directly reflect decisions `002-planning.md` already made in practice
for `AuthFeatureUser`/`AdminUser`/admin's class-not-interface surfaces; nothing here is a new design
choice.)

---

## 3. Exact Kotlin for all 8 new models

Shared conventions applied to every model below (per `002-planning.md` §3 "Shared conventions", already
finalized, not re-derived): `@Serializable data class` in the feature's `common` module under a
`models/` package; one `fun Registered<Entity>.as<Model>(): <Model>` mapper co-located in the same file;
full KDoc (class + every `@property` + mapper); no inline value classes introduced (the value-class
naming rule from `agents/CODING.md` is not triggered by any of these 8 models).

### 3.1 Commit A — `UsersFeatureUser`

**File (NEW):** `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt`

```kotlin
package dev.inmo.wishlist.features.users.common.models

import kotlinx.serialization.Serializable

/**
 * Public-facing projection of a [RegisteredUser] returned by
 * [dev.inmo.wishlist.features.users.server.UsersFeature.getAll] and its client mirror
 * [dev.inmo.wishlist.features.users.client.UsersFeature.getAll].
 *
 * Deliberately omits [RegisteredUser.email]: the `getAll` route requires no authentication (per that
 * interface's own KDoc), so any field carried by this model is visible to anonymous callers — email
 * must never be exposed there. This is the model that fixes the data leak in issue #67.
 *
 * Does not implement the sealed [User] interface: [User] requires a nullable [User.email], which would
 * reintroduce the leaked field through a shared supertype.
 *
 * @property id Database-assigned identifier of the user.
 * @property username Unique login name of the user.
 */
@Serializable
data class UsersFeatureUser(
    val id: UserId,
    val username: Username
)

/**
 * Projects this [RegisteredUser] onto the public-facing [UsersFeatureUser], dropping
 * [RegisteredUser.email].
 *
 * @return A [UsersFeatureUser] carrying only this user's [RegisteredUser.id] and [RegisteredUser.username].
 */
fun RegisteredUser.asUsersFeatureUser(): UsersFeatureUser = UsersFeatureUser(
    id = id,
    username = username
)
```

No new imports needed — `RegisteredUser`, `UserId`, `Username` all live in the same
`dev.inmo.wishlist.features.users.common.models` package (confirmed: `User.kt`, `Username.kt`).

### 3.2 Commit B-V1 — `AuthFeatureUser`

**File (NEW):** `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt`

```kotlin
package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the auth "me" surface —
 * [dev.inmo.wishlist.features.auth.client.ClientAuthFeature.getMe],
 * [dev.inmo.wishlist.features.auth.server.ServerAuthFeature.getUser], and the
 * [dev.inmo.wishlist.features.auth.client.meStateFlow] state — the authenticated caller's own record.
 *
 * Deliberately keeps [email]: this is a self-service, own-record surface (the caller reading their own
 * profile), not the public listing that leaked email in issue #67 point 1. A future "show/edit my
 * email" screen can read it directly from this model without a further "me" projection. Do not
 * interpret the presence of [email] here as a regression of the point-1 fix — [UsersFeatureUser] (the
 * public, unauthenticated listing) is the surface that must never carry it.
 *
 * @property id Database-assigned identifier of the authenticated user.
 * @property username Unique login name of the authenticated user.
 * @property email Stored email of the authenticated user, or `null` when unset. Kept intentionally —
 *   see class KDoc.
 */
@Serializable
data class AuthFeatureUser(
    val id: UserId,
    val username: Username,
    val email: Email?
)

/**
 * Projects this [RegisteredUser] onto [AuthFeatureUser], carrying every field through unchanged
 * (including [RegisteredUser.email] — see [AuthFeatureUser] KDoc for why this surface keeps it).
 *
 * @return An [AuthFeatureUser] mirroring this user's [RegisteredUser.id], [RegisteredUser.username]
 *   and [RegisteredUser.email].
 */
fun RegisteredUser.asAuthFeatureUser(): AuthFeatureUser = AuthFeatureUser(
    id = id,
    username = username,
    email = email
)
```

Dependency check (confirmed, no `build.gradle` change): `features/auth/common/build.gradle` depends on
`users.common`, which depends on `email.common` — `Email` is transitively visible.

### 3.3 Commit B-V2 — `BookingFeatureItem`

**File (NEW):** `features/booking/common/src/commonMain/kotlin/models/BookingFeatureItem.kt`

```kotlin
package dev.inmo.wishlist.features.booking.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.Serializable

/**
 * Feature model returned by
 * [dev.inmo.wishlist.features.booking.client.BookingFeature.myPresentsBooks] — the wishlist items the
 * authenticated caller has booked (the presents the caller plans to make).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set; introduced so the booking feature returns
 * its own model instead of the wishlist feature's persistence entity, per the Feature Interface Return
 * Model Rule.
 *
 * @property id Unique persistent identifier of the booked item.
 * @property wishlistId Wishlist the booked item belongs to.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External links related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class BookingFeatureItem(
    val id: WishlistItemId,
    val wishlistId: WishlistId,
    val title: String,
    val amount: UInt,
    val approximatePrice: Amount?,
    val priceUnits: String,
    val links: List<WishlistItemLink>,
    val description: String,
    val priority: Priority,
    val imageIds: List<FileId>
)

/**
 * Projects this [RegisteredWishlistItem] onto [BookingFeatureItem], carrying every display field
 * through unchanged.
 *
 * @return A [BookingFeatureItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asBookingFeatureItem(): BookingFeatureItem = BookingFeatureItem(
    id = id,
    wishlistId = wishlistId,
    title = title,
    amount = amount,
    approximatePrice = approximatePrice,
    priceUnits = priceUnits,
    links = links,
    description = description,
    priority = priority,
    imageIds = imageIds
)
```

Dependency check (confirmed, no `build.gradle` change): `features/booking/common/build.gradle` already
declares `api project(":wishlist.features.common.common")` (for `Amount`) and
`api project(":wishlist.features.wishlist.common")` (for everything else, which transitively carries
`files.common` for `FileId`).

### 3.4 Commit B-V3 — `AdminUser`, `AdminWishlist` (+ second mapper, see §1.1), `AdminWishlistItem`

**Gradle (Commit B, apply before/with the model files):** add one line to
`features/admin/common/build.gradle`'s `commonMain.dependencies` block:

```groovy
api project(":wishlist.features.wishlist.common")
```

(`email.common`/`users.common` types are already transitively available via the existing
`users.common` dependency — confirmed, no other Gradle change needed for admin.)

**File (NEW):** `features/admin/common/src/commonMain/kotlin/models/AdminUser.kt`

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin users-management surface
 * ([dev.inmo.wishlist.features.admin.client.UsersManagementFeature.getAll]/`getById`/`create`, and the
 * server-side [dev.inmo.wishlist.features.admin.server.UsersManagementFeature] equivalents).
 *
 * Root-only surface (`features/admin/README.md` Operator Notes: only the `root` user may reach the
 * admin panel/features), so [email] is kept deliberately — an unprivileged caller never reaches this
 * model.
 *
 * @property id Database-assigned identifier of the user.
 * @property username Unique login name of the user.
 * @property email Stored email of the user, or `null` when unset. Kept intentionally — see class KDoc.
 */
@Serializable
data class AdminUser(
    val id: UserId,
    val username: Username,
    val email: Email?
)

/**
 * Projects this [RegisteredUser] onto [AdminUser], carrying every field through unchanged.
 *
 * @return An [AdminUser] mirroring this user's [RegisteredUser.id], [RegisteredUser.username] and
 *   [RegisteredUser.email].
 */
fun RegisteredUser.asAdminUser(): AdminUser = AdminUser(
    id = id,
    username = username,
    email = email
)
```

**File (NEW):** `features/admin/common/src/commonMain/kotlin/models/AdminWishlist.kt`

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin wishlists-management surface
 * ([dev.inmo.wishlist.features.admin.client.AdminWishlistsFeature.getAll]/`getByUserId`/`getById`/`create`,
 * and the equivalent inline handlers in
 * [dev.inmo.wishlist.features.admin.server.configurators.AdminRoutingsConfigurator]).
 *
 * Root-only surface; introduced so the admin feature returns its own model instead of the wishlist
 * feature's persistence entity, per the Feature Interface Return Model Rule.
 *
 * @property id Unique persistent identifier of the wishlist.
 * @property userId Owner of the wishlist.
 * @property title Display name of the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class AdminWishlist(
    val id: WishlistId,
    val userId: UserId,
    val title: String,
    val defaultPriceUnits: String
)

/**
 * Projects this [RegisteredWishlist] onto [AdminWishlist], carrying every field through unchanged.
 *
 * Used by the one `AdminRoutingsConfigurator` wishlist route that bypasses
 * [dev.inmo.wishlist.features.wishlist.server.services.WishlistService] and reads the repo directly
 * (`wishlistsUpdatePathPart`). See [asAdminWishlist] (the [WishlistsFeatureWishlist] overload) for the
 * three routes that go through [dev.inmo.wishlist.features.wishlist.server.services.WishlistService].
 *
 * @return An [AdminWishlist] mirroring this wishlist's full field set.
 */
fun RegisteredWishlist.asAdminWishlist(): AdminWishlist = AdminWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)

/**
 * Projects this [WishlistsFeatureWishlist] onto [AdminWishlist], carrying every field through
 * unchanged.
 *
 * Exists specifically for the three `AdminRoutingsConfigurator` wishlist routes
 * (`wishlistsGetByUserIdPathPart`/`wishlistsGetByIdPathPart`/`wishlistsCreatePathPart`) that call
 * [dev.inmo.wishlist.features.wishlist.server.services.WishlistService] — a service also retyped by
 * this plan (B-V4) to return [WishlistsFeatureWishlist] instead of [RegisteredWishlist]. See §1.1 of
 * `003-architecturing.md` for why a second overload (rather than a single `Registered*`-sourced mapper)
 * is required here.
 *
 * @return An [AdminWishlist] mirroring this wishlist-feature model's full field set.
 */
fun WishlistsFeatureWishlist.asAdminWishlist(): AdminWishlist = AdminWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)
```

**File (NEW):** `features/admin/common/src/commonMain/kotlin/models/AdminWishlistItem.kt`

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.Serializable

/**
 * Feature model returned by the admin wishlist-items-management surface
 * ([dev.inmo.wishlist.features.admin.client.AdminWishlistItemsFeature.getByWishlistId]/`create`, and the
 * equivalent inline handlers in
 * [dev.inmo.wishlist.features.admin.server.configurators.AdminRoutingsConfigurator], all of which read
 * `WishlistItemRepo` directly rather than through a service).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set. A separate type from
 * [dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem] and
 * [dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureItem] even though the field sets
 * coincide today — each `*Feature` surface owns its own model per the Feature Interface Return Model
 * Rule, so the three can evolve independently.
 *
 * @property id Unique persistent identifier of the item.
 * @property wishlistId Wishlist the item belongs to.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External links related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class AdminWishlistItem(
    val id: WishlistItemId,
    val wishlistId: WishlistId,
    val title: String,
    val amount: UInt,
    val approximatePrice: Amount?,
    val priceUnits: String,
    val links: List<WishlistItemLink>,
    val description: String,
    val priority: Priority,
    val imageIds: List<FileId>
)

/**
 * Projects this [RegisteredWishlistItem] onto [AdminWishlistItem], carrying every display field
 * through unchanged.
 *
 * @return An [AdminWishlistItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asAdminWishlistItem(): AdminWishlistItem = AdminWishlistItem(
    id = id,
    wishlistId = wishlistId,
    title = title,
    amount = amount,
    approximatePrice = approximatePrice,
    priceUnits = priceUnits,
    links = links,
    description = description,
    priority = priority,
    imageIds = imageIds
)
```

### 3.5 Commit B-V4 — `WishlistsFeatureWishlist`

**File (NEW):** `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureWishlist.kt`

```kotlin
package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.wishlist.client.WishlistsFeature]'s
 * read/create operations (`getById`/`getByUserId`/`getMyWishlists`/`create`).
 *
 * Mirrors [RegisteredWishlist] verbatim; introduced so the client-facing wishlist feature returns its
 * own model instead of the persistence entity directly, per the Feature Interface Return Model Rule.
 *
 * @property id Unique persistent identifier of the wishlist.
 * @property userId Owner of the wishlist.
 * @property title Display name of the wishlist.
 * @property defaultPriceUnits Default currency/units label for new items; empty when none.
 */
@Serializable
data class WishlistsFeatureWishlist(
    val id: WishlistId,
    val userId: UserId,
    val title: String,
    val defaultPriceUnits: String
)

/**
 * Projects this [RegisteredWishlist] onto [WishlistsFeatureWishlist], carrying every field through
 * unchanged.
 *
 * @return A [WishlistsFeatureWishlist] mirroring this wishlist's full field set.
 */
fun RegisteredWishlist.asWishlistsFeatureWishlist(): WishlistsFeatureWishlist = WishlistsFeatureWishlist(
    id = id,
    userId = userId,
    title = title,
    defaultPriceUnits = defaultPriceUnits
)
```

`WishlistId` and `RegisteredWishlist` need no import — same package as `Wishlist.kt`
(`dev.inmo.wishlist.features.wishlist.common.models`).

### 3.6 Commit B-V5 — `WishlistsFeatureItem`

**File (NEW):** `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureItem.kt`

```kotlin
package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.wishlist.client.WishlistsItemsFeature]'s
 * read/create/copy operations (`getByWishlistId`/`create`/`copy`).
 *
 * Mirrors [RegisteredWishlistItem]'s full display field set; introduced so the client-facing wishlist
 * item feature returns its own model instead of the persistence entity directly, per the Feature
 * Interface Return Model Rule. A separate type from
 * [dev.inmo.wishlist.features.booking.common.models.BookingFeatureItem] and
 * [dev.inmo.wishlist.features.admin.common.models.AdminWishlistItem] by design — see the Feature
 * Interface Return Model Rule in `agents/CODING.md`.
 *
 * @property id Unique persistent identifier of the item.
 * @property wishlistId Wishlist the item belongs to.
 * @property title Display name of the item.
 * @property amount Desired quantity of the item.
 * @property approximatePrice Optional estimated cost.
 * @property priceUnits Currency or unit label for [approximatePrice].
 * @property links External links related to the item.
 * @property description Free-form additional notes.
 * @property priority Relative importance of the item.
 * @property imageIds Ids of images attached to the item, in display order.
 */
@Serializable
data class WishlistsFeatureItem(
    val id: WishlistItemId,
    val wishlistId: WishlistId,
    val title: String,
    val amount: UInt,
    val approximatePrice: Amount?,
    val priceUnits: String,
    val links: List<WishlistItemLink>,
    val description: String,
    val priority: Priority,
    val imageIds: List<FileId>
)

/**
 * Projects this [RegisteredWishlistItem] onto [WishlistsFeatureItem], carrying every display field
 * through unchanged.
 *
 * @return A [WishlistsFeatureItem] mirroring this item's full display field set.
 */
fun RegisteredWishlistItem.asWishlistsFeatureItem(): WishlistsFeatureItem = WishlistsFeatureItem(
    id = id,
    wishlistId = wishlistId,
    title = title,
    amount = amount,
    approximatePrice = approximatePrice,
    priceUnits = priceUnits,
    links = links,
    description = description,
    priority = priority,
    imageIds = imageIds
)
```

`WishlistId`, `WishlistItemId`, `Priority`, `WishlistItemLink`, `RegisteredWishlistItem` need no import
— same package as `WishlistItem.kt`/`Priority.kt`/`WishlistItemLink.kt`.

### 3.7 Commit B-V6 — `FilesFeatureMetaInfo`

**File (NEW):** `features/files/common/src/commonMain/kotlin/models/FilesFeatureMetaInfo.kt`

```kotlin
package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Feature model returned by [dev.inmo.wishlist.features.files.client.FilesFeature.finalize]/`getMeta`
 * (and the server-side [dev.inmo.wishlist.features.files.server.services.FilesService] equivalents).
 *
 * Mirrors [RegisteredFileMetaInfo] verbatim; introduced so the files feature returns its own model
 * instead of the persistence entity directly, per the Feature Interface Return Model Rule.
 *
 * @property id Identifier under which the payload is stored.
 * @property fileName Original file name supplied by the client.
 * @property mimeType MIME type of the payload.
 * @property size Size of the payload in bytes.
 * @property uploaderId Uploading user.
 */
@Serializable
data class FilesFeatureMetaInfo(
    val id: FileId,
    val fileName: FileName,
    val mimeType: String,
    val size: Long,
    val uploaderId: UserId
)

/**
 * Projects this [RegisteredFileMetaInfo] onto [FilesFeatureMetaInfo], carrying every field through
 * unchanged.
 *
 * @return A [FilesFeatureMetaInfo] mirroring this file's full metadata field set.
 */
fun RegisteredFileMetaInfo.asFilesFeatureMetaInfo(): FilesFeatureMetaInfo = FilesFeatureMetaInfo(
    id = id,
    fileName = fileName,
    mimeType = mimeType,
    size = size,
    uploaderId = uploaderId
)
```

`FileId` and `RegisteredFileMetaInfo` need no import — same package as `FileMetaInfo.kt`/`FileId.kt`.

---

## 4. Test stubs

Test infra confirmed reusable, no new Gradle test dependency needed anywhere: every `common` module
(`mppJvmJsAndroid` template) and every `server` module (`mppJavaProject` template) already gets
`kotlin-test-common`/`kotlin-test-annotations-common`/`kotlinx-coroutines-test` in `commonTest` via
`defaultProject`, plus platform test artifacts (`kotlin-test-junit` for jvm/android,
`kotlin-test-js` for js) via the enabling templates — confirmed by reading
`gradle/templates/defaultProject.gradle`, `enableMPPJvm.gradle`, `enableMPPJs.gradle`,
`enableMPPAndroid.gradle`. No `common` module in this plan currently has a `commonTest` directory —
Coding creates it fresh; no extra `build.gradle` wiring needed, the source set is already declared by
the applied templates.

### 4.1 Model + mapper tests — one test class per model, full code (all 8)

Placed at `features/<feature>/common/src/commonTest/kotlin/models/<Model>Test.kt`. Pattern: one
`@Test` asserting the encoded JSON key set is *exactly* the model's declared properties (the leak-class
regression test), plus one `@Test` per mapper overload asserting field-by-field projection — including,
for models with an optional field like `email`, both the non-null and null cases so a future
accidental `?: someDefault` doesn't silently swallow a real value.

**`features/users/common/src/commonTest/kotlin/models/UsersFeatureUserTest.kt`**

```kotlin
package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [UsersFeatureUser]'s wire shape and its [asUsersFeatureUser] mapper. This is the regression
 * test for the issue #67 leak: [RegisteredUser.email] must never reach the public users listing.
 */
class UsersFeatureUserTest {

    /** Encoded JSON carries exactly the `id`/`username` keys — no `email` key present. */
    @Test
    fun serializedFormContainsExactlyIdAndUsernameNoEmail() {
        val user = UsersFeatureUser(id = UserId(1L), username = Username("alice"))

        val json = Json.encodeToJsonElement(UsersFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps to id/username and drops the email. */
    @Test
    fun mapperDropsNonNullEmail() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(7L), Username("bob")), projected)
    }

    /** A [RegisteredUser] with no email still maps id/username correctly. */
    @Test
    fun mapperHandlesNullEmail() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(8L), Username("carol")), projected)
    }
}
```

**`features/auth/common/src/commonTest/kotlin/models/AuthFeatureUserTest.kt`**

```kotlin
package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AuthFeatureUser]'s wire shape (which deliberately keeps `email`, unlike
 * [dev.inmo.wishlist.features.users.common.models.UsersFeatureUser]) and its [asAuthFeatureUser]
 * mapper.
 */
class AuthFeatureUserTest {

    /** Encoded JSON carries exactly `id`/`username`/`email` — `email` is kept deliberately on this own-record surface. */
    @Test
    fun serializedFormContainsExactlyIdUsernameAndEmail() {
        val user = AuthFeatureUser(UserId(1L), Username("alice"), Email("alice@example.com"))

        val json = Json.encodeToJsonElement(AuthFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps every field through unchanged, including email. */
    @Test
    fun mapperCarriesNonNullEmailThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        val projected = registered.asAuthFeatureUser()

        assertEquals(AuthFeatureUser(UserId(7L), Username("bob"), Email("bob@example.com")), projected)
    }

    /** A [RegisteredUser] with no email maps to a null email, not a default/placeholder value. */
    @Test
    fun mapperCarriesNullEmailThrough() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        val projected = registered.asAuthFeatureUser()

        assertEquals(AuthFeatureUser(UserId(8L), Username("carol"), null), projected)
    }
}
```

**`features/booking/common/src/commonTest/kotlin/models/BookingFeatureItemTest.kt`**

```kotlin
package dev.inmo.wishlist.features.booking.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [BookingFeatureItem]'s wire shape and its [asBookingFeatureItem] mapper. */
class BookingFeatureItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 1u,
        approximatePrice = Amount(199.99),
        priceUnits = "$",
        links = listOf(WishlistItemLink("https://example.com/bike")),
        description = "Road bike, size M",
        priority = Priority.High,
        imageIds = listOf(FileId("file-1"))
    )

    /** Encoded JSON carries exactly the item's nine declared fields — no stray persistence-only field. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val item = fixture.asBookingFeatureItem()

        val json = Json.encodeToJsonElement(BookingFeatureItem.serializer(), item).jsonObject

        assertEquals(
            setOf(
                "id", "wishlistId", "title", "amount", "approximatePrice",
                "priceUnits", "links", "description", "priority", "imageIds"
            ),
            json.keys
        )
    }

    /** Every display field is projected unchanged from the source [RegisteredWishlistItem]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        val projected = fixture.asBookingFeatureItem()

        assertEquals(
            BookingFeatureItem(
                id = WishlistItemId(1L),
                wishlistId = WishlistId(2L),
                title = "Bicycle",
                amount = 1u,
                approximatePrice = Amount(199.99),
                priceUnits = "$",
                links = listOf(WishlistItemLink("https://example.com/bike")),
                description = "Road bike, size M",
                priority = Priority.High,
                imageIds = listOf(FileId("file-1"))
            ),
            projected
        )
    }

    /** Optional [BookingFeatureItem.approximatePrice] maps through as `null` when the source has none. */
    @Test
    fun mapperHandlesNullApproximatePrice() {
        val projected = fixture.copy(approximatePrice = null).asBookingFeatureItem()

        assertEquals(null, projected.approximatePrice)
    }
}
```

**`features/admin/common/src/commonTest/kotlin/models/AdminUserTest.kt`**

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AdminUser]'s wire shape (deliberately keeps `email` — root-only surface) and its
 * [asAdminUser] mapper.
 */
class AdminUserTest {

    /** Encoded JSON carries exactly `id`/`username`/`email` — kept deliberately on this root-only surface. */
    @Test
    fun serializedFormContainsExactlyIdUsernameAndEmail() {
        val user = AdminUser(UserId(1L), Username("alice"), Email("alice@example.com"))

        val json = Json.encodeToJsonElement(AdminUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps every field through unchanged. */
    @Test
    fun mapperCarriesNonNullEmailThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        assertEquals(AdminUser(UserId(7L), Username("bob"), Email("bob@example.com")), registered.asAdminUser())
    }

    /** A [RegisteredUser] with no email maps to a null email. */
    @Test
    fun mapperCarriesNullEmailThrough() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        assertEquals(AdminUser(UserId(8L), Username("carol"), null), registered.asAdminUser())
    }
}
```

**`features/admin/common/src/commonTest/kotlin/models/AdminWishlistTest.kt`**

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlist
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistsFeatureWishlist
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AdminWishlist]'s wire shape and BOTH of its mapper overloads — see `003-architecturing.md`
 * §1.1 for why [AdminWishlist] needs a second overload sourced from [WishlistsFeatureWishlist] rather
 * than only [RegisteredWishlist].
 */
class AdminWishlistTest {

    /** Encoded JSON carries exactly the wishlist's four declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val wishlist = AdminWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        val json = Json.encodeToJsonElement(AdminWishlist.serializer(), wishlist).jsonObject

        assertEquals(setOf("id", "userId", "title", "defaultPriceUnits"), json.keys)
    }

    /** The [RegisteredWishlist]-sourced overload projects every field unchanged. */
    @Test
    fun registeredWishlistMapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(AdminWishlist(WishlistId(1L), UserId(2L), "Birthday", "$"), registered.asAdminWishlist())
    }

    /** The [WishlistsFeatureWishlist]-sourced overload (used by the three admin routes that go through
     * `WishlistService`) projects every field unchanged. */
    @Test
    fun wishlistsFeatureWishlistMapperProjectsEveryFieldUnchanged() {
        val fromFeature = WishlistsFeatureWishlist(WishlistId(3L), UserId(4L), "Holiday", "EUR")

        assertEquals(AdminWishlist(WishlistId(3L), UserId(4L), "Holiday", "EUR"), fromFeature.asAdminWishlist())
    }
}
```

**`features/admin/common/src/commonTest/kotlin/models/AdminWishlistItemTest.kt`**

```kotlin
package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.wishlist.common.models.Priority
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredWishlistItem
import dev.inmo.wishlist.features.wishlist.common.models.WishlistId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemId
import dev.inmo.wishlist.features.wishlist.common.models.WishlistItemLink
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [AdminWishlistItem]'s wire shape and its [asAdminWishlistItem] mapper. */
class AdminWishlistItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 2u,
        approximatePrice = Amount(50.0),
        priceUnits = "EUR",
        links = emptyList(),
        description = "",
        priority = Priority.Small,
        imageIds = emptyList()
    )

    /** Encoded JSON carries exactly the item's nine declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val json = Json.encodeToJsonElement(AdminWishlistItem.serializer(), fixture.asAdminWishlistItem()).jsonObject

        assertEquals(
            setOf(
                "id", "wishlistId", "title", "amount", "approximatePrice",
                "priceUnits", "links", "description", "priority", "imageIds"
            ),
            json.keys
        )
    }

    /** Every display field is projected unchanged from the source [RegisteredWishlistItem]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        assertEquals(
            AdminWishlistItem(
                id = WishlistItemId(1L),
                wishlistId = WishlistId(2L),
                title = "Bicycle",
                amount = 2u,
                approximatePrice = Amount(50.0),
                priceUnits = "EUR",
                links = emptyList(),
                description = "",
                priority = Priority.Small,
                imageIds = emptyList()
            ),
            fixture.asAdminWishlistItem()
        )
    }
}
```

**`features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureWishlistTest.kt`**

```kotlin
package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [WishlistsFeatureWishlist]'s wire shape and its [asWishlistsFeatureWishlist] mapper. */
class WishlistsFeatureWishlistTest {

    /** Encoded JSON carries exactly the wishlist's four declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val wishlist = WishlistsFeatureWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        val json = Json.encodeToJsonElement(WishlistsFeatureWishlist.serializer(), wishlist).jsonObject

        assertEquals(setOf("id", "userId", "title", "defaultPriceUnits"), json.keys)
    }

    /** Every field is projected unchanged from the source [RegisteredWishlist]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredWishlist(WishlistId(1L), UserId(2L), "Birthday", "$")

        assertEquals(
            WishlistsFeatureWishlist(WishlistId(1L), UserId(2L), "Birthday", "$"),
            registered.asWishlistsFeatureWishlist()
        )
    }
}
```

**`features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureItemTest.kt`**

```kotlin
package dev.inmo.wishlist.features.wishlist.common.models

import dev.inmo.wishlist.features.common.common.models.Amount
import dev.inmo.wishlist.features.files.common.models.FileId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [WishlistsFeatureItem]'s wire shape and its [asWishlistsFeatureItem] mapper. */
class WishlistsFeatureItemTest {

    private val fixture = RegisteredWishlistItem(
        id = WishlistItemId(1L),
        wishlistId = WishlistId(2L),
        title = "Bicycle",
        amount = 1u,
        approximatePrice = null,
        priceUnits = "",
        links = listOf(WishlistItemLink("https://example.com", "Store")),
        description = "Notes",
        priority = Priority.Medium,
        imageIds = listOf(FileId("a"), FileId("b"))
    )

    /** Encoded JSON carries exactly the item's nine declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val json = Json.encodeToJsonElement(WishlistsFeatureItem.serializer(), fixture.asWishlistsFeatureItem()).jsonObject

        assertEquals(
            setOf(
                "id", "wishlistId", "title", "amount", "approximatePrice",
                "priceUnits", "links", "description", "priority", "imageIds"
            ),
            json.keys
        )
    }

    /** Every display field, including a multi-element [WishlistsFeatureItem.imageIds] list, is projected unchanged. */
    @Test
    fun mapperProjectsEveryFieldUnchangedIncludingMultipleImageIds() {
        val projected = fixture.asWishlistsFeatureItem()

        assertEquals(listOf(FileId("a"), FileId("b")), projected.imageIds)
        assertEquals(listOf(WishlistItemLink("https://example.com", "Store")), projected.links)
        assertEquals(null, projected.approximatePrice)
    }
}
```

**`features/files/common/src/commonTest/kotlin/models/FilesFeatureMetaInfoTest.kt`**

```kotlin
package dev.inmo.wishlist.features.files.common.models

import dev.inmo.micro_utils.common.FileName
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [FilesFeatureMetaInfo]'s wire shape and its [asFilesFeatureMetaInfo] mapper. */
class FilesFeatureMetaInfoTest {

    /** Encoded JSON carries exactly the metadata's five declared fields. */
    @Test
    fun serializedFormContainsExactlyDeclaredFields() {
        val meta = FilesFeatureMetaInfo(
            id = FileId("file-1"),
            fileName = FileName("avatar.png"),
            mimeType = "image/png",
            size = 1024L,
            uploaderId = UserId(1L)
        )

        val json = Json.encodeToJsonElement(FilesFeatureMetaInfo.serializer(), meta).jsonObject

        assertEquals(setOf("id", "fileName", "mimeType", "size", "uploaderId"), json.keys)
    }

    /** Every field is projected unchanged from the source [RegisteredFileMetaInfo]. */
    @Test
    fun mapperProjectsEveryFieldUnchanged() {
        val registered = RegisteredFileMetaInfo(
            id = FileId("file-1"),
            fileName = FileName("avatar.png"),
            mimeType = "image/png",
            size = 1024L,
            uploaderId = UserId(1L)
        )

        assertEquals(
            FilesFeatureMetaInfo(
                id = FileId("file-1"),
                fileName = FileName("avatar.png"),
                mimeType = "image/png",
                size = 1024L,
                uploaderId = UserId(1L)
            ),
            registered.asFilesFeatureMetaInfo()
        )
    }
}
```

### 4.2 Service-layer retype tests — case specs (given/when/expect)

These exercise the retyped service methods that produce the new models (not the routing configurators
— see §1.2 on why route-level tests are out of scope repo-wide). Reuse the `FakeUsersRepo`/`FakeEmailsService`
double pattern from `features/email/server/src/commonTest/kotlin/services/` (in-memory `MapCRUDRepo`/`MapKeyValueRepo`-backed
fakes implementing the relevant repo interface). Place new fakes next to their service test, under
`features/<feature>/server/src/commonTest/kotlin/services/` (or, for admin's `UsersManagementFeature.kt`
and auth's `ServerAuthFeature`-implementing service, directly under `.../commonTest/kotlin/` mirroring
the production file's own top-level `services`/root package placement).

| Service method (post-retype) | New test file | Case | Given | When | Expect |
|---|---|---|---|---|---|
| `UsersService.getAll` (Commit A) | `features/users/server/src/commonTest/kotlin/services/UsersServiceTest.kt` | returns feature model, drops email | Fake `ReadUsersRepo` seeded with one user carrying a non-null `email` | `service.getAll()` | Returned list has one `UsersFeatureUser` with matching `id`/`username`; **no way to read email off the result type at all** (compile-time proof, plus the JSON test in §4.1 covers the wire form) |
| ″ | ″ | empty repo → empty list | Fake repo with no users | `service.getAll()` | `emptyList()` |
| `AuthFeatureService.getUser` (server, B-V1) | `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt` | valid unexpired token → `AuthFeatureUser` with email preserved | Fake `ReadUsersRepo`/`WriteUsersRepo` seeded with a user carrying `email`; a token issued via `login` (or seeded directly into the service's internal token map through `login`) | `service.getUser(token)` | Non-null `AuthFeatureUser` with `email` equal to the seeded user's email (regression check: B-V1 must NOT drop email, unlike Commit A's `UsersFeatureUser`) |
| ″ | ″ | expired token → `null` | Token issued with `tokenTtl = Duration.ZERO` (or a service built with a zero/negative TTL) | `service.getUser(token)` after the TTL elapses | `null` |
| ″ | ″ | unknown token → `null` | No token issued | `service.getUser(Token("unknown"))` | `null` |
| `BookingService.myPresentsBooks` (B-V2) | `features/booking/server/src/commonTest/kotlin/services/BookingServiceTest.kt` | caller's bookings map to `BookingFeatureItem` | Fake `BookingRepo` with one booking for `callerId`; fake `WishlistItemRepo`/`WishlistRepo` seeded with the matching item | `service.myPresentsBooks(callerId)` | Returned list has one `BookingFeatureItem` whose fields match the seeded `RegisteredWishlistItem` field-for-field |
| ″ | ″ | no bookings → empty list | Fake `BookingRepo` empty for `callerId` | `service.myPresentsBooks(callerId)` | `emptyList()` |
| Admin `UsersManagementFeature.getAll`/`create` (server, B-V3) | `features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt` | `getAll` maps every stored user to `AdminUser` with email preserved | Fake `UsersRepo` seeded with 2 users, one with a non-null email | `feature.getAll()` | List of `AdminUser`, matching ids/usernames/emails (email preserved — root-only surface, unlike Commit A) |
| ″ | ″ | `create` returns the persisted user as `AdminUser` | Fake `UsersRepo` + fake `AuthFeatureService`(or a real `AuthFeatureService` wired to fakes, matching the constructor shape read in this file) | `feature.create(NewUserWithPassword(...))` | Non-null `AdminUser` with the submitted username, `email = null` (username-only creation path, matching current `create`'s `NewUser(newUserWithPassword.username)` call) |
| `WishlistService.getById`/`getByUserId`/`getMyWishlists`/`create` (B-V4) | `features/wishlist/server/src/commonTest/kotlin/services/WishlistServiceTest.kt` | each read/create method returns `WishlistsFeatureWishlist` | Fake `WishlistRepo` seeded with wishlists for two different users | one test per method | Returned value(s) are `WishlistsFeatureWishlist` matching the seeded `RegisteredWishlist` fields; `getById` on an unknown id returns `null` |
| `WishlistItemService.getByWishlistId`/`create`/`copyItem` (B-V5) | `features/wishlist/server/src/commonTest/kotlin/services/WishlistItemServiceTest.kt` | each method returns `WishlistsFeatureItem` | Fake `WishlistItemRepo` + `WishlistRepo` seeded with an item/wishlist pair | one test per method | Returned value(s) are `WishlistsFeatureItem` matching the seeded item; `copyItem`'s idempotent-existing-item branch also returns the mapped type (not the raw repo type) |
| `FilesService.finalize`/`getMeta` (B-V6) | `features/files/server/src/commonTest/kotlin/services/FilesServiceTest.kt` | both return `FilesFeatureMetaInfo` | Fake `TemporalFilesRoutingConfigurator`-shaped temp file provider (or minimal stand-in exposing a valid PNG signature per `isSupportedRasterImage`'s allowlist) + fake `FilesRepo`/`FilesMetaInfoRepo` | `service.finalize(request, callerId)` then `service.getMeta(returned.id)` | Both calls return `FilesFeatureMetaInfo` (not `RegisteredFileMetaInfo`) with matching `fileName`/`mimeType`/`size`/`uploaderId` |

Every case above is a plain `kotlinx.coroutines.test.runTest` unit test with an in-memory fake — no
platform-specific rendering, no live database, no live SMTP/network call — so nothing in this plan is
genuinely untestable; §1.2 already covers the one layer (HTTP routing) this repo does not unit-test
anywhere, old or new code alike.

---

## 5. Commit A deliverables — full file list for Coding pass 1

Commit message topic (from `002-planning.md` §3, confirmed unchanged):
`fix(users): hide emails from public users listing behind UsersFeatureUser model; document feature-model rule`

1. **NEW** `features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt` — exact code in §3.1.
2. **NEW** `features/users/common/src/commonTest/kotlin/models/UsersFeatureUserTest.kt` — exact code in §4.1.
3. **NEW** `features/users/server/src/commonTest/kotlin/services/UsersServiceTest.kt` — per §4.2's case table (create the `FakeUsersRepo`-style double locally, or reuse the one under `features/email/server/src/commonTest` as a reference — it cannot be imported cross-module since it's `internal`/test-scoped, so a local equivalent is expected, not a shared test-fixtures module).
4. `features/users/server/src/commonMain/kotlin/UsersFeature.kt` — `getAll(): List<UsersFeatureUser>`; update KDoc to state this is the feature-model projection, not the persistence entity.
5. `features/users/server/src/commonMain/kotlin/services/UsersService.kt` — `usersRepo.getAll().values.map { it.asUsersFeatureUser() }`.
6. `features/users/client/src/commonMain/kotlin/UsersFeature.kt` — `getAll(): List<UsersFeatureUser>`; KDoc update.
7. `features/users/client/src/commonMain/kotlin/KtorUsersFeature.kt` — return type follows (body signature: `client.get(...).body()` — the generic infers from the declared return type, no other code change).
8. `features/ui/users/src/commonMain/kotlin/ui/UsersModel.kt` — `getAllUsers(): List<UsersFeatureUser>`, `getUser(id): UsersFeatureUser?`; import + KDoc updates.
9. `features/ui/users/src/commonMain/kotlin/Plugin.kt` — anonymous `UsersModel` impl: `getAllUsers`/`getUser` bodies unchanged (`feature.getAll()` / `feature.getAll().find { it.id == id }` — types follow the retyped `UsersFeature` automatically); swap the `RegisteredUser` import for `UsersFeatureUser`.
10. `features/ui/users/src/commonMain/kotlin/ui/UsersListViewModel.kt` — retype `_usersState: MutableRedeliverStateFlow<List<RegisteredUser>>` → `List<UsersFeatureUser>`.
11. `features/ui/users/src/commonMain/kotlin/ui/UserViewModel.kt` — retype `_userState: MutableRedeliverStateFlow<RegisteredUser?>` → `UsersFeatureUser?`.
12. `agents/CODING.md` — insert the `## Feature Interface Return Model Rule` section from §2, directly after `## CRUD Repository Pattern`.
13. `features/users/README.md` — Routes table (`getAll` response type), Models table, Architecture Notes — do not touch Operator Notes (this feature's README was verified to have none beyond the required structure, but the rule stands regardless).
14. `features/ui/users/README.md` — Models section reflecting `UsersFeatureUser`.

**No textual change, compile-verify only:**
- `features/users/server/src/jvmMain/kotlin/configurators/UsersRoutingsConfigurator.kt` — `call.respond(feature.getAll())`, no type name used in the file.
- `features/users/server/src/commonMain/kotlin/Plugin.kt`, `features/users/client/src/commonMain/kotlin/Plugin.kt` — DI registrations are not signature-dependent.
- `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (~line 177) — `usersFeature.getAll().find { it.id == userId }?.username?.string` touches only `id`/`username`, both present on `UsersFeatureUser`.

**Build gate:** `./gradlew :wishlist.features.users.common:build :wishlist.features.users.server:build :wishlist.features.users.client:build :wishlist.features.ui.users:build :wishlist.features.ui.wishlist:build` (or full `./gradlew build`), then `ast-index rebuild`. Per `agents/CODING.md`'s existing top-level rule, run one fix cycle if the build fails and report any remaining issue rather than looping.

---

## 6. Commit B deliverables — full file list for Coding pass 2

Commit message topic (confirmed unchanged): `fix(features): return feature-owned models from *Feature interfaces per new rule`

**Gradle (apply first, before any model file needs it):** add to
`features/admin/common/build.gradle`'s `commonMain.dependencies` block:
```groovy
api project(":wishlist.features.wishlist.common")
```
No other `build.gradle` file needs a change (`Email`/`Amount`/`FileId` are all transitively available
where needed — see §3.2–§3.7's per-model dependency notes).

**Intra-commit ordering note (see §1.1):** implement B-V4 (`WishlistService` retype) before finishing
B-V3's three `wishlistService.*`-routed `AdminRoutingsConfigurator` handlers — both land in this single
commit, so this is a within-commit sequencing note, not a blocker.

### 6.1 New model files (all six, exact code in §3)

1. `features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt`
2. `features/booking/common/src/commonMain/kotlin/models/BookingFeatureItem.kt`
3. `features/admin/common/src/commonMain/kotlin/models/AdminUser.kt`
4. `features/admin/common/src/commonMain/kotlin/models/AdminWishlist.kt` (two mapper overloads — §1.1/§3.4)
5. `features/admin/common/src/commonMain/kotlin/models/AdminWishlistItem.kt`
6. `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureWishlist.kt`
7. `features/wishlist/common/src/commonMain/kotlin/models/WishlistsFeatureItem.kt`
8. `features/files/common/src/commonMain/kotlin/models/FilesFeatureMetaInfo.kt`

### 6.2 New test files (exact code in §4.1, case tables in §4.2)

9. `features/auth/common/src/commonTest/kotlin/models/AuthFeatureUserTest.kt`
10. `features/booking/common/src/commonTest/kotlin/models/BookingFeatureItemTest.kt`
11. `features/admin/common/src/commonTest/kotlin/models/AdminUserTest.kt`
12. `features/admin/common/src/commonTest/kotlin/models/AdminWishlistTest.kt`
13. `features/admin/common/src/commonTest/kotlin/models/AdminWishlistItemTest.kt`
14. `features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureWishlistTest.kt`
15. `features/wishlist/common/src/commonTest/kotlin/models/WishlistsFeatureItemTest.kt`
16. `features/files/common/src/commonTest/kotlin/models/FilesFeatureMetaInfoTest.kt`
17. `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt` (§4.2 case table)
18. `features/booking/server/src/commonTest/kotlin/services/BookingServiceTest.kt` (§4.2 case table)
19. `features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt` (§4.2 case table)
20. `features/wishlist/server/src/commonTest/kotlin/services/WishlistServiceTest.kt` (§4.2 case table)
21. `features/wishlist/server/src/commonTest/kotlin/services/WishlistItemServiceTest.kt` (§4.2 case table)
22. `features/files/server/src/commonTest/kotlin/services/FilesServiceTest.kt` (§4.2 case table)

### 6.3 B-V3 — Admin (folds in the §1.1 correction)

- `features/admin/client/src/commonMain/kotlin/UsersManagementFeature.kt` — `getAll`/`getById`/`create` → `AdminUser`.
- `features/admin/client/src/commonMain/kotlin/KtorUsersManagementFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminWishlistsFeature.kt` — `getAll`/`getByUserId`/`getById`/`create` → `AdminWishlist`.
- `features/admin/client/src/commonMain/kotlin/KtorAdminWishlistsFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminWishlistItemsFeature.kt` — `getByWishlistId`/`create` → `AdminWishlistItem`.
- `features/admin/client/src/commonMain/kotlin/KtorAdminWishlistItemsFeature.kt` — return types.
- `features/admin/client/src/commonMain/kotlin/AdminFeature.kt` — no type change; compile-verify (exposes sub-interfaces by name only).
- `features/admin/client/src/commonMain/kotlin/KtorAdminFeature.kt` — compile-verify (expected: no type-specific code).
- `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt` — `getAll(): List<AdminUser>` (`usersRepo.getAll().values.map { it.asAdminUser() }`); `create(...): AdminUser?` (map the created `RegisteredUser`); `update`/`setPassword`/`delete` stay `Boolean?`, unaffected.
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt` — per-handler:
  - `usersGetAllPathPart` / `usersCreatePathPart` — no textual change (follow #`UsersManagementFeature`'s retype automatically); compile-verify.
  - `usersGetByIdPathPart` (~line 94-98) — bypasses `UsersManagementFeature`, calls `usersRepo.getById(id)` directly: change `call.respond(user)` to `call.respond(user.asAdminUser())`.
  - `wishlistsGetAllPathPart` (~line 163) — `wishlistRepo.getAll().values.toList()` → `call.respond(wishlistRepo.getAll().values.map { it.asAdminWishlist() })` (the `RegisteredWishlist`-sourced overload — bypasses `WishlistService`).
  - `wishlistsGetByUserIdPathPart` (~line 171) — `call.respond(wishlistService.getByUserId(userId))` → after B-V4, `wishlistService.getByUserId` returns `List<WishlistsFeatureWishlist>`: change to `call.respond(wishlistService.getByUserId(userId).map { it.asAdminWishlist() })` (the **`WishlistsFeatureWishlist`-sourced overload** — see §1.1).
  - `wishlistsGetByIdPathPart` (~line 173-184) — `wishlistService.getById(id)` likewise returns `WishlistsFeatureWishlist?` post-B-V4: map with `?.asAdminWishlist()` before `call.respond(...)` (same overload as above).
  - `wishlistsCreatePathPart` (~line 185-197) — `wishlistService.create(...)` likewise returns `WishlistsFeatureWishlist?` post-B-V4: map with `?.asAdminWishlist()` (same overload).
  - `wishlistsUpdatePathPart` (~line 198-218) — `wishlistRepo.update(id, ...)` is a **direct repo call**, unaffected by B-V4 (only `WishlistService` is retyped, not `WishlistRepo`): still returns `RegisteredWishlist?`; map with the **`RegisteredWishlist`-sourced overload** — `call.respond(updated.asAdminWishlist())`.
  - `wishlistItemsGetByWishlistIdPathPart` (~line 234-241) — `wishlistItemRepo.getByWishlistId(wishlistId)` (direct repo call, unaffected by B-V5): `call.respond(wishlistItemRepo.getByWishlistId(wishlistId).map { it.asAdminWishlistItem() })`.
  - `wishlistItemsCreatePathPart` (~line 242-250) — `wishlistItemRepo.create(listOf(newItem)).firstOrNull()` (direct repo call): map result with `?.asAdminWishlistItem()`.
  - `wishlistItemsUpdatePathPart` (~line 251-263) — `wishlistItemRepo.update(id, newItem)` (direct repo call): map result with `?.asAdminWishlistItem()`.
- `features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt` — retype `getAllUsers`/`getUserById`/`createUser` → `AdminUser`; `getAllWishlists`/`getWishlistsByUser`/`getWishlistById`/`createWishlist` → `AdminWishlist`; `getItemsByWishlist`/`createWishlistItem` → `AdminWishlistItem`.
- `features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt` — retype anonymous impl + imports throughout.
- `features/ui/adminPanel/src/{jvmMain,jsMain,androidMain}/kotlin/ui/AdminPanelView.kt` — compile-verify field access (`.email` reads must still resolve, now against `AdminUser.email`).
- Any additional admin ViewModel under `features/ui/adminPanel/src/commonMain/kotlin/ui/` not enumerated above — run `ast-index outline features/ui/adminPanel/src/commonMain/kotlin/ui` first to confirm nothing else references `RegisteredUser`/`RegisteredWishlist`/`RegisteredWishlistItem`.
- `features/admin/README.md`, `features/ui/adminPanel/README.md` — Routes/Models/Architecture Notes; do not touch Operator Notes (verified text in §1.3).

### 6.4 B-V4 — `WishlistsFeature` (wishlist client) + sidebar consumer

- `features/wishlist/client/src/commonMain/kotlin/WishlistsFeature.kt` — `getById`/`getByUserId`/`getMyWishlists`/`create` → `WishlistsFeatureWishlist`; `update`/`delete` stay `Boolean`.
- `features/wishlist/client/src/commonMain/kotlin/KtorWishlistFeature.kt` — return types.
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistService.kt` — `getById`/`getByUserId`/`getMyWishlists`/`create` each wrap their existing repo call with `.asWishlistsFeatureWishlist()` (or `?.let { it.asWishlistsFeatureWishlist() }` / `.map { it.asWishlistsFeatureWishlist() }` matching each method's current nullable/list shape, exact bodies read in this step — see `WishlistService.kt` code in the investigation record above).
- `features/wishlist/server/src/commonMain/kotlin/configurators/WishlistRoutingsConfigurator.kt` — no textual change; every relevant handler does `call.respond(wishlistService.xxx(...))` and follows the retype automatically (confirmed by direct read of this file — includes the **public, unauthenticated** `getByUserId`/`getById` routes, which is a small additional hardening: they now return the feature model instead of the raw persistence entity too). Compile-verify only.
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsModel.kt` — retype wherever `RegisteredWishlist` flows from `WishlistsFeature` calls.
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistsListViewModel.kt` — retype.
- `features/ui/wishlist/src/commonMain/kotlin/ui/UserWishlistsViewModel.kt` — retype.
- `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` — retype wherever `RegisteredWishlist` flows from `WishlistsFeature` calls specifically (this file is shared with B-V5 below — handle both retypes in one pass over the file).
- `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarModel.kt` (~line 26) — `List<RegisteredWishlist>` → `List<WishlistsFeatureWishlist>` (confirmed genuine V4 consumer via `WishlistsModel.getMyWishlists()`; confirmed **not** a V1 consumer — zero `RegisteredUser`/`meStateFlow`/`AuthFeature` references anywhere under `features/ui/sidebar/src/`).
- `features/ui/sidebar/src/commonMain/kotlin/ui/SidebarViewModel.kt` (~line 55) — retype.
- `features/ui/sidebar/src/commonMain/kotlin/Plugin.kt` (~line 40) — retype.
- `features/wishlist/README.md`, `features/ui/wishlist/README.md`, `features/ui/sidebar/README.md` — Routes/Models/Architecture Notes.

### 6.5 B-V5 — `WishlistsItemsFeature` (wishlist client)

- `features/wishlist/client/src/commonMain/kotlin/WishlistsItemsFeature.kt` — `getByWishlistId`/`create`/`copy` → `WishlistsFeatureItem`.
- `features/wishlist/client/src/commonMain/kotlin/KtorWishlistItemFeature.kt` — return types.
- `features/wishlist/server/src/commonMain/kotlin/services/WishlistItemService.kt` — `getByWishlistId`/`create`/`copyItem` each map to `.asWishlistsFeatureItem()`.
- `features/wishlist/server/src/commonMain/kotlin/configurators/WishlistItemRoutingsConfigurator.kt` — no textual change; `create` (`wishlistItemCreatePathPart`), `copy` (`wishlistItemCopyPathPart` → calls `copyItem`), and the public `getByWishlistId` route all `call.respond(...)` the service result directly and follow the retype automatically. Compile-verify only.
- **Not touched (confirmed):** `WishlistCopyService` — its only public method `enqueue(...): RegisteredWishlistCopyJob?` never returns `RegisteredWishlistItem`; it is not a violation (matches `001-planning.md` §2.4's original audit, which the addendum re-confirmed after a contradictory draft bullet was corrected).
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemViewModel.kt` — retype.
- `features/ui/wishlist/src/commonMain/kotlin/ui/WishlistViewModel.kt` — retype.
- `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` — retype (shared with B-V4, see above).
- `features/ui/wishlist/src/commonMain/kotlin/ui/BookingConfigsProvider.kt`, `ui/WishlistAdditionalConfigsProvider.kt`, `ui/WishlistItemAdditionalConfigView.kt`, `ui/WishlistItemCopyViewModel.kt` — reference `RegisteredWishlistItem`; retype where the value flows from `WishlistsItemsFeature` specifically (not booking/admin).
- Platform views: `features/ui/wishlist/src/{jvmMain,androidMain,jsMain}/kotlin/ui/UserWishlistsView.kt`, `ui/WishlistItemCard.kt`, `src/jsMain/kotlin/ui/WishlistItemRow.kt` — retype/compile-verify field access.
- Not exhaustive by design (per `002-planning.md`'s own caveat, reconfirmed here): run `ast-index refs RegisteredWishlistItem` scoped to `features/ui/wishlist` before closing this surface out, to catch anything the above list missed.

### 6.6 B-V6 — `FilesFeature` (`finalize`/`getMeta`)

- `features/files/client/src/commonMain/kotlin/FilesFeature.kt` — `finalize`/`getMeta` → `FilesFeatureMetaInfo?`.
- `features/files/client/src/commonMain/kotlin/KtorFilesFeature.kt` — return types.
- `features/files/client/src/commonMain/kotlin/FilesClientService.kt` — `uploadFile(...): FilesFeatureMetaInfo?` (wraps `finalize`; not itself a `*Feature` interface, but must retype to stay call-site-compatible with `uploadAvatar`'s `.id` access).
- `features/files/server/src/commonMain/kotlin/services/FilesService.kt` — `finalize` builds `meta` then `metaInfoRepo.set(fileId, meta)` (repo keeps storing `RegisteredFileMetaInfo` — **do not change the repo's stored type**, only the method's own **return** value: return `meta.asFilesFeatureMetaInfo()`); `getMeta` returns `metaInfoRepo.get(id)?.asFilesFeatureMetaInfo()`.
- `features/files/server/src/commonMain/kotlin/configurators/FilesRoutingsConfigurator.kt` — no textual change; touches the post-retype `getMeta` result at **two** call sites (§1.3): the `${Constants.metaPathPart}/{id}` route (`call.respond(meta)`, follows automatically) and the raw-byte `{id}` route (`meta.fileName.string`/`meta.mimeType` field reads only, both present unchanged on `FilesFeatureMetaInfo`). Compile-verify only, both sites.
- Not touched (confirmed — zero UI callers of client `getMeta`; the only `uploadFile()`/`finalize()` consumer narrows to `?.id` immediately): `features/ui/wishlist/src/commonMain/kotlin/Plugin.kt` (~line 180). Compile-verify only.
- `features/files/README.md` — Routes/Models/Architecture Notes.

### 6.7 Build gate

Full `./gradlew build` (touches 6 features + their `ui/*` consumers — a scoped module list would be
error-prone to enumerate by hand for this breadth), then `ast-index rebuild`. One fix cycle on failure,
same rule as Commit A.

---

## 7. Consolidated file list per Coding pass (final checklist)

### Coding pass 1 (Commit A) — 14 files, all listed in §5 items 1–14, plus the `agents/CODING.md` edit (item 12). No Gradle changes.

### Coding pass 2 (Commit B) — grouped for quick reference:
- 1 Gradle file: `features/admin/common/build.gradle`.
- 8 new model files (§6.1).
- 14 new test files (§6.2).
- ~40 retyped/compile-verified production files across auth, booking, admin, wishlist, files and their
  `ui/*` consumers, enumerated exactly in §6.3–§6.6 (with the two `ast-index`-outline/`refs` safety nets
  called out explicitly for admin's ViewModels and wishlist's B-V5 UI surface, since those two areas were
  not fully read file-by-file during Planning or this step and are the only places where an unlisted file
  might still turn up).
- README updates: `features/admin/README.md`, `features/ui/adminPanel/README.md`,
  `features/wishlist/README.md`, `features/ui/wishlist/README.md`, `features/ui/sidebar/README.md`,
  `features/auth/README.md`, `features/booking/README.md`, `features/ui/booking/README.md`,
  `features/files/README.md`.

---

## 8. Result

- Spec is complete and execution-ready for both Coding passes without needing to re-open
  `001-planning.md`/`002-planning.md`.
- One genuinely new item surfaced during this step (§1.1: the B-V3/B-V4 `WishlistService` interaction)
  — resolved here with an exact, non-ambiguous fix (a second mapper overload), not escalated.
- One pre-existing, repo-wide limitation flagged for visibility per the Test Planning Requirement
  (§1.2: no route-level HTTP tests exist anywhere in this repo) — does not block Coding; the business
  logic this plan adds is fully unit-testable and covered by §4.
- All three items `002-planning.md` §3 left unconfirmed (`features/ui/wishlist/README.md` existence,
  the exact two-touch-point nature of `FilesRoutingsConfigurator`, and the precise `WishlistService`
  method bodies) were confirmed/resolved directly against source during this step.
