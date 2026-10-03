Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL.)
Changed files: features/auth/common/src/commonTest/kotlin/models/AuthFeatureUserTest.kt; features/admin/common/src/commonTest/kotlin/models/AdminUserTest.kt; features/users/common/src/commonTest/kotlin/models/UsersFeatureUserTest.kt; features/users/common/src/jvmTest/kotlin/repo/CacheUsersRepoSqliteTest.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; features/users/server/src/commonTest/kotlin/services/UsersServiceTest.kt; agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/022-coding.md

## VEC-05 model, public-boundary, cache, and auth proof

`AuthFeatureUserTest` and `AdminUserTest` now prove base-to-feature-to-base mapper and common JSON round trips with approved current `A`, pending candidate `B`, concrete deadline `D`, and approval set to `true`. Both suites also decode a legacy payload that omits the additive lifecycle fields and assert the compatible default state: `emailApproved = false`, `pendingEmail = null`, and `emailChangeAllowedAt = null`.

`UsersFeatureUserTest` seeds all private lifecycle values, projects the source to the public feature model, and asserts exact JSON keys `id` and `username`. Its reverse mapper passes `email`, `emailApproved`, `pendingEmail`, and `emailChangeAllowedAt` explicitly and round-trips the full source row. `UsersServiceTest` uses a populated persisted row and checks both returned-list element JSON and individually serialized response JSON for the same exact public key set, so private key names and values cannot escape through the actual service projection.

`CacheUsersRepoSqliteTest` creates a fixture-owned file database with two independently connected `ExposedUsersRepo` instances. It warms a first `CacheUsersRepo`, approves the current address through the second instance, advances its controlled clock to the exact allowed deadline, and records a replacement candidate there. The ordinary first cache remains the initial stale row, while `getByIdFresh` returns the complete newer row: approved current address, pending replacement, approval state, and persisted deadline. No cache refresh, synthetic event, or same-wrapper write participates; the existing success and failed-conditional cache coverage remains intact.

`AuthFeatureServiceTest` builds the real `AuthFeatureService` through its existing `UsersRepo` seam, issues a valid password/token under the existing direct-role authorization fixture, keeps the cache deliberately stale, mutates the underlying repository to approved current plus pending candidate and deadline, and proves authenticated `getUser` uses `getByIdFresh` to return the complete newer private lifecycle. The server service has no separate `getMe` member; its authenticated own-record boundary is `getUser`, while the client `getMe` consumes that private wire model.

The existing `KtorEmailFeatureTest.setMyEmailDecodesOnlyWellFormedCooldownResponses` already proves a non-null `EmailChangeCooldown.emailChangeAllowedAt` wire payload and rejects legacy/malformed shapes from the typed path. No missing common cooldown model assertion was found.

No production defect was demonstrated. No production, schema, route, configuration, UI, dependency, README, KDoc, or Operator Notes file changed.

## Verification

`./gradlew --no-parallel :wishlist.features.users.common:jvmTest --tests '*CacheUsersRepoSqliteTest' :wishlist.features.users.common:jvmTest --tests '*UsersFeatureUserTest' :wishlist.features.auth.common:jvmTest --tests '*AuthFeatureUserTest' :wishlist.features.auth.common:jsNodeTest :wishlist.features.admin.common:jvmTest --tests '*AdminUserTest' :wishlist.features.admin.common:jsNodeTest :wishlist.features.users.server:jvmTest --tests '*UsersServiceTest' :wishlist.features.auth.server:jvmTest --tests '*AuthFeatureServiceTest'` completed successfully. The focused XML results report zero failures and zero errors: AuthFeatureUser JVM 5 and JS 5; AdminUser JVM 5 and JS 5; UsersFeatureUser JVM 5; CacheUsersRepoSqlite JVM 2; UsersService JVM 3; AuthFeatureService JVM 24.

The cache proof was rerun after correcting its test-only controlled-clock sequencing: a positive cooldown initially rejected a replacement before expiry, as production requires. Advancing to exact expiry made the requested independent mutation valid; the rerun completed successfully with zero failures and zero errors.

`AST_INDEX_DB_PATH=/tmp/wishlist-vec05-ast.db ast-index rebuild` completed after Kotlin source changes: 812 files, 49 modules, zero parse failures. `git diff --check` completed successfully.

## Closure

VEC-05 is fully closed by the completed server/configuration/admin/repository proof in step 021 and this final model/cache/public/auth proof slice. VEC-06 remains open for its documentation and rollout work.
