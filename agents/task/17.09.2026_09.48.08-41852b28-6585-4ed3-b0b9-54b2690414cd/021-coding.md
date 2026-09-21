Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL.)
Changed files: features/email/server/src/commonTest/kotlin/PluginTest.kt; features/email/server/src/commonTest/kotlin/EmailConfigTest.kt; features/email/server/src/commonTest/kotlin/services/EmailVerificationAccountCoordinatorTest.kt; features/email/server/src/commonTest/kotlin/services/EmailFeatureServiceTest.kt; features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt; features/admin/server/src/commonTest/kotlin/configurators/AdminRoutingsConfiguratorTest.kt; features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt; agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/021-coding.md

## VEC-05 server, configuration, repository, and admin proof

The production implementation already owns the relevant invariants at the correct seams: `Plugin.setupDI` validates the root policy before graph selection, `EmailVerificationAccountCoordinator` is shared by enabled and disabled feature implementations, `ExposedUsersRepo` evaluates a locked deadline before mutation, and the admin route translates the repository exception only after authorization. No production source defect was demonstrated, so this step adds regression proof only.

`PluginTest` now executes actual Email `Plugin.setupDI` graphs for omitted, zero, `P1D`, and positive fractional policies across absent, explicit-null, and configured SMTP shapes. It proves configured graphs expose `EmailFeatureService`, disabled graphs expose `DisabledEmailFeature` without an `EmailsService`, and the coordinator is singleton-shared. Negative, malformed, JSON-null, positive/negative infinity, and oversized duration forms each fail setup independently of the SMTP shape. `EmailConfigTest` reads the real sample configuration and proves its explicit `P1D` policy decodes to `86_400_000` milliseconds with the same unknown-key behavior as startup.

`EmailVerificationAccountCoordinatorTest` uses an actual Plugin graph, a controlled-clock `ExposedUsersRepo`, and a fixture-owned SQLite file for both SMTP-enabled and disabled graphs. Approval through the retrieved shared coordinator issues the exact `1010` deadline from `1000 + PT0.01S`; replacement B and clearing reject with the typed exception before expiry and leave the durable row unchanged; a same-current request is a no-op; a dedicated rename remains allowed; and at exact expiry B becomes the pending candidate while approved A and its deadline remain intact. No SMTP operation is invoked.

`EmailFeatureServiceTest` now uses a real durable repository, the real coordinator, real deeplink service, and controlled non-network transport. A positive-policy rejection of B occurs before any HTML delivery or link creation, proving the service cannot create transport side effects ahead of the repository decision.

`UsersManagementFeatureTest` exercises a real `ExposedUsersRepo` and controlled clock. Full administrative B and null updates before expiry return the exact typed deadline with neither a partial username write nor lifecycle change; `updateUsername` still works; exact expiry permits the replacement and retains approved A with pending B. `AdminRoutingsConfiguratorTest` drives the real Ktor route and repository: root receives typed `429` with deadline `1010`, anonymous receives `401`, non-root receives `403`, denied bodies do not disclose a deadline, the dedicated rename succeeds, exact expiry succeeds, and duplicate email preservation remains `409`.

`ExposedUsersRepoSqliteTest` adds the missing checked-addition proof. At `Long.MAX_VALUE - 5`, approving with ten milliseconds raises `ArithmeticException`, rolls the row back completely, and emits no update event.

## Verification

`AST_INDEX_DB_PATH=/tmp/wishlist-vec05-ast.db ast-index rebuild` completed successfully after Kotlin test edits: 812 files, 49 modules, and zero parse failures.

`./gradlew --no-parallel :wishlist.features.email.server:jvmTest :wishlist.features.admin.server:jvmTest :wishlist.features.users.common:jvmTest --tests '*ExposedUsersRepoSqliteTest'` completed successfully. The focused XML results include 93 email-server tests, 4 `UsersManagementFeatureTest` tests, 5 `AdminRoutingsConfiguratorTest` tests, and 28 `ExposedUsersRepoSqliteTest` tests; every listed suite has zero failures and zero errors.

`git diff --check` completed successfully. Only JVM test source changed, so no common production, JS, or Android source compilation surface required rebuilding.

## Scope and remaining work

This closes the VEC-05 server/configuration/admin/repository proof slice without changing routes, API semantics, dependencies, schema, samples, UI, models, cache, documentation, or live SMTP behavior. Remaining VEC-05 work is the separate public/auth/model/cache surface; VEC-06 remains outside this step.
