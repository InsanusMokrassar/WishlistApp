Model: Claude Sonnet 5 (claude-sonnet-5)
Execution time: ~15 minutes wall-clock (task folder/`PROMPT.md` timestamped 15:25:55, this report written ~15:41, same day 2026-07-10).
Tokens used: not precisely instrumented from inside this agent; rough estimate ~90k-120k tokens (repo-wide greps, several full-file reads, three decompiled-library-sources jar extractions and reads: `micro_utils.repos.exposed` 0.29.4, `ktor-server-core-jvm` 3.5.1/`ktor-server-netty-jvm` 3.5.0, `exposed-jdbc`/`exposed-core` 1.3.0).
Changed files: `agents/task/10.07.2026_09.25.25-94f65eaa-1273-46db-829a-1578bc8ee2f3/001-planning.md` (this file) only. No source, config, or other doc file was edited by this step.

---

# Planning: unresolved PR #63 review comment — "Emails for users must be unique" (fix/44-email)

## 1. Task understanding

`PROMPT.md` documents exactly one unresolved GitHub review thread on PR #63: a comment by the repo
owner on `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` targeting
`private val emailColumn = text("email").nullable()`, body verbatim *"Emails for users must be
unique"*. The literal, minimal fix is adding `.uniqueIndex()` to that column, mirroring the adjacent
`usernameColumn = text("username").uniqueIndex()`. My job was not to just apply that literal fix, but
to investigate whether a DB-level unique constraint changes failure behavior for existing call sites
(`EmailFeatureService.setMyEmail` / `DisabledEmailFeature.setMyEmail` via the shared
`updateStoredEmail` helper, and `PUT /email/myEmail`'s route handler) badly enough to require
additional exception-handling as part of this same task, or whether that's a separable, genuinely
open design question for the operator.

## 2. Investigation findings

### 2.1 `ExposedUsersRepo.kt` — current state (read in full)

```kotlin
private val usernameColumn = text("username").uniqueIndex()
...
private val emailColumn = text("email").nullable()
```

No other column, index, or constraint touches `email`. `update(id, value, it)` sets both columns
unconditionally (`it[emailColumn] = value.email?.string`). `init { initTable() }` runs
`createMissingTablesAndColumns` on every startup (additive schema migration, already documented in
this file's own KDoc and in `features/users/README.md`).

### 2.2 Does a unique-constraint violation surface as an uncaught exception, or get translated to `null`/`false`?

Resolved by extracting the exact dependency version this project uses (`microutils = "0.29.4"` in
`gradle/libs.versions.toml`) from the matching `-sources.jar` in the Gradle cache (per
`agents/local.ALL.md`'s instruction, I first checked `/home/aleksey/projects/own` for a local
`dev.inmo` checkout — `MicroUtils` exists there but at a much newer, non-matching commit/tag set, so I
used the version-matched sources jar from `~/.gradle/caches` instead, which is authoritative for what
actually ships).

`AbstractExposedWriteCRUDRepo.kt` (`dev.inmo.micro_utils.repos.exposed`, jvmMain,
`micro_utils.repos.exposed-jvm-0.29.4-sources.jar`):

```kotlin
private fun updateWithoutNotification(id: IdType, value: InputValueType): ObjectType? {
    return transaction(db = database) {
        update({ selectById(id) }) { update(id, value, it as UpdateBuilder<Int>) }
    }.let {
        if (it > 0) { transaction(db = database) { selectAll().where { selectById(id) }.limit(1).firstOrNull()?.asObject } }
        else { null }
    }
}
override suspend fun update(id: IdType, value: InputValueType): ObjectType? {
    onBeforeUpdate(listOf(id to value))
    return updateWithoutNotification(id, value).let { ... }
}
```

There is **no `try`/`catch` anywhere** in this method, in `insert()`, or in `create()`. The `null`
return path exists only for "zero rows matched `selectById`" (row count `0`), i.e. "no such id" — it
has nothing to do with constraint violations. A DB-level unique-constraint violation on `emailColumn`
would be thrown by the underlying `org.jetbrains.exposed.v1.jdbc.update(...)` call as an
`ExposedSQLException` (Exposed's wrapper around the driver's `java.sql.SQLException` — Postgres raises
SQL state `23505`, "unique_violation") and would **propagate uncaught** straight out of
`AbstractExposedWriteCRUDRepo.update()`, i.e. out of `UsersRepo.update(...)`, out of
`updateStoredEmail(usersRepo, callerId, email)` (`features/email/server/.../UpdateStoredEmail.kt`,
which also has no `try`/`catch`), out of `EmailFeatureService.setMyEmail`/
`DisabledEmailFeature.setMyEmail`, and out of `EmailRoutingsConfigurator`'s `put(myEmailPathPart) {
}` route handler (also no `try`/`catch`). **Conclusion: this determines that new exception handling
would in fact be needed if the answer to §2.3 were "no", but §2.3 shows it is not required as a
correctness matter.**

The identical uncaught-exception path also exists today for `usernameColumn` (already
`.uniqueIndex()`-constrained, pre-dating this PR) via `AdminRoutingsConfigurator`'s
`PUT /admin/users/update/{id}` → `UsersManagementFeature.update(id, newUser)` →
`usersRepo.update(id, newUser) != null` — see §2.4.

### 2.3 Is an uncaught exception from a route handler already turned into *some* response, or does it crash worse than the existing `500` fallback?

Two-layer check, both confirmed from actual dependency sources (not guessed):

- **`StatusPages` is installed, but with zero registered handlers.** `features/common/server/src/jvmMain/kotlin/JVMPlugin.kt:97`:
  `singleWithRandomQualifier<KtorApplicationConfigurator> { StatusPagesConfigurator(getAllDistinct()) }`
  — `getAllDistinct()` collects every bound `StatusPagesConfigurator.Element`. Repo-wide grep for
  `StatusPagesConfigurator` (excluding `build/`) finds **zero** features registering an `Element`
  anywhere in this codebase. So `StatusPages` is installed with an empty exception-handler map; it will
  not intercept an `ExposedSQLException` and will simply re-throw it (this is `StatusPages`' own
  documented behavior when no handler matches: it doesn't swallow, it rethrows to the next
  interceptor).
- **Ktor's own engine pipeline has an unconditional fallback, independent of `StatusPages`.**
  Extracted `ktor-server-core-jvm` sources (version-matched to `ktor = "3.5.0"` in
  `libs.versions.toml`, resolved concretely to `3.5.1` in the local cache — same `DefaultEnginePipeline.kt`
  API across that patch range) — `io/ktor/server/engine/DefaultEnginePipeline.kt`:

  ```kotlin
  pipeline.intercept(EnginePipeline.Call) {
      try {
          call.application.execute(call)
      } catch (error: ChannelIOException) { ... }
        catch (error: Throwable) { handleFailure(routeCall ?: call, error) }
      finally { ... }
  }

  public suspend fun handleFailure(call: ApplicationCall, error: Throwable) {
      logError(call, error)
      val statusCode = defaultExceptionStatusCode(error) ?: HttpStatusCode.InternalServerError
      tryRespondError(call, statusCode, error.message)
  }
  ```

  `defaultExceptionStatusCode` has no special case for `ExposedSQLException`/`SQLException`, so it
  falls through to `HttpStatusCode.InternalServerError`. `tryRespondError` only skips responding if
  `call.response.isCommitted || call.response.isSent` — not the case here, since the exception is
  thrown before the route handler ever calls `call.respond(...)`. **This is the literal, engine-level
  "every uncaught exception in a route handler becomes a logged, controlled `500 Internal Server
  Error`" fallback** — a real response is always sent, the coroutine/connection is not crashed
  differently, and this is true with or without `StatusPages` being installed.

**Conclusion for §2.2+§2.3 combined:** an uncaught unique-constraint-violation exception from
`usersRepo.update(...)` on a duplicate email would produce exactly `500 Internal Server Error` at the
HTTP layer — the same status code the route already returns explicitly today for every other
`setMyEmail() == false` case. It is a *different code path* (Ktor's engine-level catch-all vs. the
route's explicit `else -> call.respond(HttpStatusCode.InternalServerError)` branch) but *not* a worse
outcome — no crash, no hung connection, no missing response, no different status family. Per the
task's own decision criteria, this does **not** meet the "MUST be fixed as part of this task" bar.

### 2.4 Existing precedent for duplicate-key handling on a `.uniqueIndex()` column (`usernameColumn`)

There is **no existing duplicate-key handling pattern anywhere in this codebase** — this part is
genuinely close to greenfield, but not because nobody has hit the scenario in principle: the exact
same latent gap already exists today for `usernameColumn`, unaddressed, predating this PR:

- `features/admin/server/src/commonMain/kotlin/UsersManagementFeature.kt:43-46`:
  ```kotlin
  suspend fun update(id: UserId, newUser: NewUser): Boolean? {
      if (!usersRepo.contains(id)) return null
      return usersRepo.update(id, newUser) != null
  }
  ```
- `features/admin/server/src/commonMain/kotlin/configurators/AdminRoutingsConfigurator.kt:105-117`
  (`PUT /admin/users/update/{id}`):
  ```kotlin
  when (adminFeature.usersManagement.update(id, newUser)) {
      true -> call.respond(HttpStatusCode.OK)
      false -> call.respond(HttpStatusCode.InternalServerError)
      null -> call.respond(HttpStatusCode.NotFound)
  }
  ```

If an admin renames a user to an already-taken username today, the exact same uncaught
`ExposedSQLException` → Ktor engine-level `500` path fires — with zero special handling, zero `409`,
zero distinguishing signal. I grepped the whole repo for `ExposedSQLException`, `SQLException`,
`409`, and `Conflict` (HTTP-status sense) outside this task's own files — **zero hits**. So: the
repo-wide precedent for "what happens when a `.uniqueIndex()`-constrained column collides" is
"nothing special — falls through to the generic `500`," and that precedent already ships in
production-facing code today for `usernameColumn`. There is no prior instance of a `409 Conflict` (or
any other duplicate-specific signal) anywhere in `agents/CODING.md`, `agents/ARCHITECTURE.md`, or the
source tree to follow.

### 2.5 Migration behavior — does adding `.uniqueIndex()` actually apply to the existing table?

Checked because `emailColumn` already exists (nullable, unindexed) in any already-migrated dev/test
DB, and `initTable()`'s `createMissingTablesAndColumns` only runs additive column creation by name at
a glance. Extracted `exposed-jdbc` sources (version-matched to `exposed = "1.3.0"` in
`libs.versions.toml`) — `SchemaUtils.kt:263-298`, `createMissingTablesAndColumns(...)`:

```kotlin
val modifyTablesStatements = checkMappingConsistence(tables = tables, withLogs).filter { it !in executedStatements }
execStatements(inBatch, modifyTablesStatements)
commit()
```

`checkMappingConsistence` → `checkMissingAndUnmappedIndices` diffs the table's Kotlin-defined indices
against what's actually in the DB and returns `CREATE INDEX`/`CREATE UNIQUE INDEX` statements for
anything missing — and `createMissingTablesAndColumns` **does execute** those statements (not just
log them), unlike its sibling `statementsRequiredToActualizeScheme` which only returns them. So: yes,
simply adding `.uniqueIndex()` to `emailColumn` is sufficient — the next server startup will
auto-create the missing unique index via the existing `initTable()` call, with no separate migration
step needed, exactly mirroring how `usernameColumn`'s original `.uniqueIndex()` was presumably applied.

**One edge-case risk worth flagging (not a blocker):** if any already-migrated database currently
contains two or more users with the *same* non-null email (possible only via `PUT /email/myEmail`,
since that's the only write path that ever sets `email`, and only since this still-unmerged branch's
earlier commits), the `CREATE UNIQUE INDEX` statement would fail at `initTable()`/server-startup time
— a startup-time failure, not a per-request one. Given `PUT /email/myEmail` and this whole feature are
still unreleased (open PR, not on `master`), this is very unlikely to have any live data to collide
with, but it's the kind of thing that would fail loudly at deploy time rather than silently, which is
an acceptable and correct failure mode for a migration precondition violation — not something to code
around.

## 3. Decision on the minimal fix

The minimal fix (`.uniqueIndex()` on `emailColumn`, nothing else) is **sufficient and safe**:

- It satisfies the literal review comment.
- `Column<T>.uniqueIndex()` is generic (`exposed-core` `Table.kt:1728`, `fun <T> Column<T>.uniqueIndex(...)`),
  so it composes fine after `.nullable()`; standard SQL unique-index semantics treat `NULL` as
  distinct from every other `NULL` (both Postgres and H2 behave this way by default), so users who
  have never set an email (`NULL`) do **not** collide with each other — only two *equal, non-null*
  email values collide. This is exactly the desired behavior and requires no extra `filterCondition`.
- The one behavioral change it introduces — an uncaught-exception path instead of a `null`-return path
  on collision — was shown in §2.2/§2.3 to degrade to the *same* `500 Internal Server Error` the route
  already returns for every other failure today, via Ktor's own engine-level fallback (not a "worse"
  outcome per the task's own criteria).
- It matches the one real precedent that exists in this codebase (`usernameColumn`, §2.4) exactly:
  add the index, add no special exception handling, accept the generic-`500` degrade.

**No mandatory code change beyond the one-line `ExposedUsersRepo.kt` diff is required for
correctness.**

## 4. Open question for the operator (per `agents/PLAN.md` step 4 — flagged, not decided unilaterally)

Per the task's own explicit instruction, since the uncaught-exception path already degrades gracefully
to a generic `500`-equivalent (not "worse"), whether to *additionally* add a distinguishable `409
Conflict` signal for "email already taken by another user" is a genuine value-add design decision with
no existing precedent either way (§2.4: not even `usernameColumn` — the closest analog — has one).
Multiple reasonable designs exist and I am not picking one:

- **(A) Do nothing beyond the `.uniqueIndex()` fix.** Matches the one real precedent in this repo
  exactly (`usernameColumn`); zero new code, zero new risk surface; `PUT /email/myEmail` keeps
  responding `500` for "email taken" exactly like it does for every other failure mode today (already
  documented as indistinguishable from other failures for `sendTest`'s root-check in
  `features/email/README.md`'s "Root guard" note — this would be a second instance of that same
  documented "the failure modes are indistinguishable at the HTTP layer" pattern).
- **(B) Catch the violation and respond `409 Conflict`.** Requires picking *where* to catch it: (i)
  inside `ExposedUsersRepo` (JVM-only file) — the constraint lives there, but `UsersRepo`/`WriteUsersRepo`
  are `commonMain` interfaces (`features/users/common/src/commonMain/kotlin/repo/`) shared by
  `CacheUsersRepo`, so signaling "duplicate" through the return type would mean either widening the
  shared interface contract (touches `admin/server/UsersManagementFeature.update` and
  `AdminRoutingsConfigurator`'s username-update path too — scope creep beyond `email`) or introducing a
  new `commonMain`-visible exception type that `ExposedUsersRepo` throws and email's route/service
  layer specifically catches (crosses the users→email module boundary in the unusual direction — email
  already depends on users, not vice versa); or (ii) catching the raw `ExposedSQLException`/`SQLException`
  directly inside `updateStoredEmail` or the route handler and pattern-matching the driver's SQL state
  (`23505` for Postgres) — cheaper, but couples `features/email/server` (or `features/users/common`) to
  Postgres-specific error codes, and only covers this one call site, not `usernameColumn`'s identical
  gap in the admin routes.
- **(C) Fix both `email` and the pre-existing `username` gap together**, if the operator considers this
  PR's scope to now include establishing a *general* duplicate-key-to-409 convention rather than a
  one-off for `email`.

**I am flagging this as the open question rather than choosing.** If the operator has no terminal
access in this run, per `agents/PLAN.md` step 4 the fallback is a `gh issue comment`; I have not posted
one — resolving that fallback path is the calling Orchestrator/Root's call per this session's explicit
instruction ("Report back to me (your caller): READY or blocked... and the path to `001-planning.md`"),
not something I execute from inside Planning.

## 5. Concrete plan handed to Architecture (applies regardless of how §4 resolves)

1. **`features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt`** — the only mandatory diff:
   ```kotlin
   /**
    * Optional email address column.
    *
    * Nullable so that existing rows (without the column) are treated as `NULL` after the
    * schema migration run by [initTable] / `createMissingTablesAndColumns`. Unique so that
    * two users cannot share the same non-null email address — `NULL` values are exempt from
    * the uniqueness check under standard SQL unique-index semantics, so users without a
    * stored email never collide with each other.
    */
   private val emailColumn = text("email").nullable().uniqueIndex()
   ```
   (KDoc updated per `agents/CODING.md`'s "KDoc on every changed declaration" rule — the existing KDoc
   only explained nullability, not uniqueness, so it needs the added sentence.) No other line in this
   file needs to change: `update()`, `asObject`, `getUserByUsername()` are unaffected.

2. **`EmailRoutingsConfigurator`'s `put(myEmailPathPart)` handler** — **no change** under resolution
   (A); a `409`-branch addition (`when { updated -> OK; ... }` gaining a distinguishable case) only
   under resolution (B)/(C), and only once the operator has picked *where* the catch lives (§4). Do
   not speculatively add error-handling code before that's resolved — it would be exactly the kind of
   unilateral design decision `agents/PLAN.md` step 4 says not to make.

3. **`EmailFeatureService` / `DisabledEmailFeature` / the shared `updateStoredEmail` helper** — **no
   change** under resolution (A). Their current contract (`Boolean`: `true` persisted, `false` = user
   not found) stays accurate — a constraint violation under (A) is not represented as `false`, it's an
   uncaught exception, and that's an accepted/documented tradeoff, not a bug to paper over with a
   `false` return.

4. **Tests** — no new unit test is feasible or expected for the `.uniqueIndex()` behavior itself: the
   only test double for `UsersRepo` in this branch's test suite is `FakeUsersRepo` (`features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt`), an in-memory `MapCRUDRepo` wrapper with no
   uniqueness enforcement and no SQL layer to violate — consistent with this repo's existing precedent
   of not unit-testing genuinely DB-backed behavior (`features/email/README.md` already documents that
   "the live-SMTP success path is intentionally not unit-tested (external integration)" for the same
   reason). Verification here means: run the module build (`./gradlew :wishlist.features.users.common:build`,
   plus the usual full-project build) per `agents/CODING.md`'s post-change build-task rule, and — if
   there's a way to exercise the real Postgres test instance — a manual `PUT /email/myEmail` duplicate
   check would be the closest thing to a positive verification, but is not mandatory given no such
   harness currently exists for this repo's other DB-constraint code either.

5. **README updates** (per `agents/CODING.md`'s "after any code change, update the feature README"
   rule — checked both candidates named in `PROMPT.md`):
   - **`features/users/README.md`** — needs an update. Its Architecture Notes "Email field (added in
     issue #44)" bullet currently reads: *"`ExposedUsersRepo` has a `nullable text("email")` column;
     `createMissingTablesAndColumns` (via `initTable()`) adds it to existing tables without data
     migration."* — this must gain a clause noting the column is now also `.uniqueIndex()`-constrained
     (`NULL` exempt), matching the corrected source. This is the right README for this fact: it's the
     feature that owns `ExposedUsersRepo`.
   - **`features/email/README.md`** — only needs an update if/once §4 resolves to (B) or (C) and the
     `PUT /email/myEmail` route/response contract actually changes (its Routes table row documenting
     `200 OK` as the only success path would then need a `409` row, and Architecture Notes would need a
     new bullet next to the existing "Root guard" note about indistinguishable failure modes). Under
     resolution (A), **no change is needed** to this README — the Routes table's existing `/ 200 OK`
     entry and the "Storage vs sending" architecture bullet both remain accurate as-is (they don't
     currently claim anything about email uniqueness one way or the other).

6. **Build/verification step for whichever role codes this:** run `./gradlew
   :wishlist.features.users.common:build` (module directly touched) and the full-project build task
   named in `agents/CODING.md`, per that file's "run required compilation task... do only one cycle of
   fixing" rule. Run `ast-index rebuild` after the `.kt` edit (per `agents/ALL.md` — README-only edits
   do not require it).

## 6. Answers received

None yet — §4 is a newly-surfaced open question from this Planning pass; no operator response exists
at the time of writing. This plan is otherwise complete and actionable for item 1 (the mandatory fix)
regardless of how §4 resolves; items 2-3 and part of item 5 are gated on that answer.
