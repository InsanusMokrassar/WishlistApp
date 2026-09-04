Model: gpt-5.6-sol
Changed files: .github/workflows/docker_deploy.yml, README.md, features/auth/README.md, features/auth/client/build.gradle, features/auth/client/src/commonMain/kotlin/AuthFeatureService.kt, features/auth/client/src/commonMain/kotlin/KtorAuthFeature.kt, features/auth/client/src/commonTest/kotlin/KtorAuthFeatureTest.kt, features/auth/common/src/commonMain/kotlin/AuthFeature.kt, features/auth/common/src/commonTest/kotlin/models/RegisterRequestTest.kt, features/auth/server/src/commonMain/kotlin/Plugin.kt, features/auth/server/src/commonMain/kotlin/RegistrationRoleLifecycle.kt, features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt, features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt, features/common/README.md, features/common/server/src/jvmMain/kotlin/models/Config.kt, features/common/server/src/jvmTest/kotlin/models/ConfigTest.kt, features/email/README.md, features/email/server/src/commonMain/kotlin/Plugin.kt, features/email/server/src/commonMain/kotlin/models/EmailVerificationPayload.kt, features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt, features/email/server/src/commonMain/kotlin/services/EmailVerificationDeepLinkHandler.kt, features/email/server/src/commonTest/kotlin/services/EmailDeepLinkIntegrationTest.kt, features/email/server/src/commonTest/kotlin/services/EmailRegistrationInviteSenderTest.kt, features/email/server/src/commonTest/kotlin/services/EmailVerificationDeepLinkHandlerTest.kt, features/email/server/src/commonTest/kotlin/services/RegistrationCompensationIntegrationTest.kt, features/roles/README.md, features/roles/server/src/commonMain/kotlin/Plugin.kt, features/roles/server/src/commonMain/kotlin/RolesBootstrap.kt, features/roles/server/src/commonMain/kotlin/RolesRegistrationRoleLifecycle.kt, features/roles/server/src/commonTest/kotlin/RolesBootstrapTest.kt, features/roles/server/src/jvmMain/kotlin/JVMPlugin.kt, gradle/libs.versions.toml, server/dev.config.json, server/sample.config.json, local.review.74.md, agents/task/10.08.2026_09.08.17-c0ecd3ab-3385-4a7f-ab41-649fb5f51d24/003-coding.md

## Model rationale

The coding preference list names `sonnet`, `opus`, and `fable`, but none is available in this Codex session. The inherited `gpt-5.6-sol` model is the strongest available frontier coding model and was used for the cross-module concurrency, compensation, authorization, compatibility, and configuration work. The required documentation-specific `haiku` model was also unavailable; the same active model applied the architecture-mandated README deltas while preserving every Operator Notes block verbatim.

## Latest-version confirmation

I fetched `origin` with pruning before implementation. The remote PR branch remained at `55c420b9319e304775fe14ea692a08f394ae220e`, `origin/master` remained at `fe375f02e2442e75a14b95011ce7dbaf38da82ee`, and the checked-out branch contained that PR head plus the committed planning and architecture reports. I did not merge, rebase, push, or modify the pre-existing untracked task `PROMPT.md`.

## Implementation

Required-email registration now reserves a provisional user and synchronously marks that exact id pending under the global auth write lock, but does not install a password or issue a token. BCrypt hashing and invite delivery run after releasing the lock. Successful delivery enters a second locked phase that verifies the user and submitted email still match before installing the hash and issuing credentials. False delivery, an ordinary sender exception, a failed pending transition, a missing provisional user, and cancellation all compensate the user, auth state, and direct roles. Cancellation cleanup runs in a non-cancellable context and the original cancellation propagates. The optional-email path retains the original single-lock behavior. The create boundary translates only `DuplicateUserFieldException` to the existing failed-registration result.

Auth owns the new `RegistrationRoleLifecycle` inversion contract, while Roles owns its synchronized implementation. Generic and administrator-created users receive `UserRole`; only required self-registration replaces that state with `NewUserRole`. Generic grant, pending marking, promotion, and direct-role removal share one mutex. Live creation callbacks check the user still exists inside that mutex, and a deletion-flow subscription removes every direct role. The callback-before-delete, delete-before-callback, pending-before-generic, generic-before-pending, and promotion-before-delayed-callback orders now converge.

`AuthFeature` again exposes the original abstract two-argument `register` method and adds a distinct email-aware overload with a default bridge. Current server, Ktor, and client-service implementations explicitly bridge the legacy call to the email-aware method. `KtorAuthFeature.getConfig` now probes the legacy availability endpoint after a failed/non-successful/undecodable modern config response and treats a legacy server as optional-email; it does not make the fallback request after a successful modern response.

New verification payloads contain both the user id and invited email. The handler rejects a legacy null email, missing user, cleared address, or current address mismatch before changing roles. The invite sender removes a minted link in non-cancellable context before propagating delivery cancellation.

Common server configuration now exposes `publicHttpOrigin`, with the compatibility default `http://{publicHost}:{port}`. The invite sender validates a plain absolute HTTP(S) origin at construction and resolves the deeplink path without consulting `wss` or appending the internal bind port. Development and sample production JSON declare explicit origins, and the Auth, Roles, Email, Common, and root documentation describe the resulting contracts.

The Docker deployment workflow now runs only for pushes to `master`. The invalid branch-qualified Gradle version rewrite was removed, while the separate Build workflow remains enabled for every push.

## Local review

The requested repository-root `local.review.74.md` reviews the original PR head and recommends changes. It contains the seven planning findings, the High-severity address-binding exploit confirmed by architecture, and the blocking Docker release-safety issue. Every entry includes original-head line evidence, a concrete failure sequence, the bounded remediation, and the local fix status. The repository's existing `local.*` ignore rule keeps this review local; it is intentionally not staged and no GitHub review was posted.

## Regression coverage

The Auth tests exercise delivery suspension while an unrelated seeded user logs in, denial of provisional login, successful finalization, missing hooks, failed pending marking, false and throwing senders, cancellation propagation after cleanup, retry after failure, and duplicate-email translation without sender/lifecycle calls. The common compatibility fixture implements only the pre-email method set. MockEngine client tests cover a 404 modern route, invalid modern JSON, a successful required-email response without fallback, and both probes failing closed.

Roles tests exercise generic administrator-style creation, root privileges, both pending callback orders, delayed callbacks after promotion, explicit pending preservation during backfill, both deletion/callback orders, idempotent direct-role cleanup, and real create/delete flow collection. Email tests cover bound payload persistence and polymorphic round-trip, legacy payload decoding, matching and mismatching/cleared/missing users, repeated opens, false/throwing/cancelled SMTP cleanup, valid external origin variants, invalid origin rejection, and a real Auth/Roles/Email/DeepLinks failed-registration integration that leaves all four stores clean. Common config tests cover the derived compatibility default and explicit reverse-proxy origin.

## Checks run

The affected production sources compiled successfully with:

```text
./gradlew :wishlist.features.auth.common:compileKotlinJvm :wishlist.features.auth.client:compileKotlinJvm :wishlist.features.auth.server:compileKotlinJvm :wishlist.features.roles.server:compileKotlinJvm :wishlist.features.email.server:compileKotlinJvm :wishlist.features.common.server:compileKotlinJvm
```

The first combined focused-test invocation found only two test calls selecting the list-valued `deleteById` member instead of the single-id extension. I added the correct import once, as the coding rule permits, and removed one unnecessary experimental scheduler call. The repeated focused suite passed:

```text
./gradlew :wishlist.features.auth.common:jvmTest :wishlist.features.auth.client:jvmTest :wishlist.features.auth.server:jvmTest :wishlist.features.roles.server:jvmTest :wishlist.features.email.server:jvmTest :wishlist.features.common.server:jvmTest
```

`jq empty` accepted both checked-in server JSON files. Deterministic workflow assertions confirmed a master-only Docker trigger, no branch version rewrite, retained registry/deploy steps, and the unchanged all-push Build trigger. `git diff --check` passed. `actionlint` is not installed, so no actionlint result is available. The subsequent Verification role owns the independent full `./gradlew build`, per the orchestrator's coding-stage scope direction.

I rebuilt `ast-index` after the Kotlin changes. The completed index contains 1,377 files, 10,096 symbols, and 56,201 references.

## Scope and handoff

The patch stays within the eight approved product findings plus the master-only deploy correction. It does not add an application-wide `UserRole` gate, resend/link-consumption policy, verification timestamp or schema, password reset, or re-verification policy for already-approved users who later edit their email.

The implementation and focused tests are ready for independent Verification. The ignored `local.review.74.md` and the pre-existing untracked `PROMPT.md` must remain unstaged; all tracked source, test, configuration, documentation, workflow, and this coding report should be committed together without pushing.
