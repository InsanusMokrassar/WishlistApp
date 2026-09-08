Model: OpenAI Sol (HL planning), OpenAI Luna (LL report transcription)
Changed files: agents/task/08.09.2026_07.33.26-becae363-8f30-4035-8e9e-77af5e3a4145/001-planning.md

Planning selected Sol as the highest-available-priority HL model under MODELS/SHORTCUTS. Luna transcribed the initial report under the LL documentation rule. The Orchestrator then identified the conflicting prohibition on nested agents in ORCHESTRATOR.md, interrupted the writer, and directed Planning to review, finish, and commit this step directly. No further delegation occurred. Caveman compression applied only to internal working notes; this report uses normal prose.

## Task understanding

The task is to implement missing self-email setup when SMTP is enabled, add an explicit persisted address-approval state, and connect emailed approval handling to that state. The admin landing page must show actual user rows and clearly explain the purpose of its current input. Work was prompt-driven because the GitHub token was invalid; the source PROMPT remains untouched.

## Evidence and reuse

I read ALL/local.ALL, PLAN (local.PLAN was absent), PROTOCOL, GIT, TOOLS, AST_INDEX, MODELS, and the complete relevant README material for email, auth, users, admin, ui/adminPanel, ui/users, deeplinks, and roles. The only substantive relevant Operator Notes are in admin: administration requires root-only access and must reuse existing capabilities. No requested behavior conflicts with those notes. The AST index was initially unbuilt; rebuilding required escalated filesystem permission and completed successfully. Code navigation then used the rebuilt index exclusively; rg was used only to discover documentation. The Git tree was initially clean.

In `features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt`, updateStoredEmail holds the mutex around get/update. verifyInvitedEmailAndPromote checks the exact recipient and promotes NewUserRole, but does not persist approval. Existing EmailVerificationDeepLinkHandler and EmailRegistrationInviteSender in that same directory already provide opaque deeplinks, exact user/email payloads, a fixed approval redirect marker, SMTP confirmation, and failed-delivery cleanup. EmailFeatureService.setMyEmail delegates only to storage. DisabledEmailFeature shares the coordinator and existing storage, which must continue working when SMTP is disabled. The common SetEmailRequest and client KtorEmailFeature already support self PUT; the enabled probe is public and self identity is bearer-derived.

`features/users/common/src/commonMain/kotlin/models/User.kt` has nullable email only and no approval flag. `features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt` has a unique nullable email and additive initTable schema behavior. AuthFeatureUser and AdminUser are private projections, while UsersFeatureUser is public and excludes email. The users UI UserEditViewModel currently permits only root text save; self-email setup should be a separate action in the existing own-profile editor across JS, JVM, and Android. `features/ui/users/src/commonMain/kotlin/Plugin.kt` updateUsername currently sends NewUser(username), implicitly clearing email; username-only saves must preserve email and approval. The users UI build file also needs the email client dependency; adminPanel already declares that dependency.

`features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelViewModel.kt` currently supports navigation and sending a test message. AdminUsersListViewModel in the same directory already loads on initialization/resume, and AdminPanelModel.getAllUsers uses the protected admin API. `features/ui/adminPanel/src/jsMain/kotlin/ui/AdminPanelView.kt` lacks an SMTP section heading, while the JVM counterpart already has one. Existing list-row styles and navigation should be reused, and the current user-list screen must remain accessible.

## Acceptance and safe assumptions

1. A signed-in owner with no email and enabled email capability can enter a valid address, save, and receive a verification invite. Anonymous users and another user’s editor cannot mutate the caller’s email through a mismatched profile. The UI distinguishes loading, validation, success, pending, and failure states, supplies English and Russian strings on every platform, and refreshes the authenticated owner record after save or resume. SMTP-disabled UI hides setup while backend storage behavior remains unchanged.
2. Persist Boolean emailApproved with default false. A newly set, changed, or cleared address resets approval to false; the same address and username-only writes preserve true. Existing rows default false because historic approval evidence is untrustworthy; migration changes no user role or login behavior. Approval is exposed only through own/auth and, if needed, admin DTOs for faithful mapping, never through public lists. Generic client create/update data cannot assert approval.
3. The existing emailed link verifies the current exact user and address and persists true. Stale, wrong, legacy-null, or missing recipients reject. NewUser→User registration promotion, logged-out pending registration, approval-toast redirect, and confirmation remain intact. Authenticated approved users adding an email remain in their account role and session. Storage and approval remain coordinated, including admin email replacements.
4. A new address triggers the existing sender/link flow. Delivery failure leaves the address unapproved and permits retry of the same pending address; the UI never falsely reports mail sent. Preserve the old storage-success contract, adding a request-verification endpoint/result only if necessary. No SMTP/network operation runs under the coordinator mutex. After asynchronous delivery, stale-link checks remain authoritative and cleanup cannot erase concurrent edits.
5. The admin landing page loads the protected user list initially and on resume, rendering username/id rows with existing detail navigation plus loading, empty, and error states. Wishlist and users/create navigation remain. The SMTP test area is separately labeled with recipient input and the explanation “Send a test message to check the server email configuration.” Capability-disabled state is explicit or consistently hidden; no input is mislabeled as user search.

## Implementation plan

1. Define a narrow repository-owned approval mutation instead of trusting an externally writable NewUser field. Add mapper compatibility, mutation/reset/cache flow, and retry result semantics; coordinate persistence and delivery without widening the locking boundary.
2. Add the persisted field and additive schema with conservative defaults, including source and JSON defaults. Provide a repository helper/internal mutation that cannot reset approval during unrelated edits.
3. Extend the email coordinator, approval handler, delivery hook, and dependency-injection shared boundary. Avoid introducing a parallel approval framework.
4. Wire the owner profile model and feature to shared ViewModel state, then add localized JS/JVM/Android views. Preserve owner and admin handling of all unrelated fields.
5. Complete dashboard state, list, and selection interactor registration in ClientPlugin. Add clear test-email copy and retain existing navigation.
6. Update touched README narratives within permitted Coding scope, never Operator Notes, and rebuild the AST index after code changes.

## Verification plan

Focused ExposedUsersRepoSqliteTest coverage should include legacy-table addition, durable reload, false default, marking true, replacement/clear reset, unchanged and username-only preservation, duplicate constraints, and cache behavior. AuthFeatureUserTest, AdminUserTest, and UsersFeatureUserTest should cover legacy decoding, accurate round trips, and public privacy.

Extend EmailVerificationAccountCoordinatorTest and EmailVerificationDeepLinkHandlerTest for explicit approval, registered approved-role users, pending registration, stale/legacy/null/missing/repeated links, deterministic concurrent update/approval, and cross-user identity. EmailFeatureServiceTest and PluginTest should cover enabled/disabled dependency injection, send failure, retry, cancellation cleanup, and duplicates. Existing EmailDeepLinkIntegrationTest, EmailRegistrationInviteSenderTest, and RegistrationCompensationIntegrationTest cover related delivery and registration seams; preserve their contracts and the client EmailApprovalNotificationTest. No users/adminPanel UI tests currently exist. Add focused shared ViewModel tests for owner/enabled/missing and retry states, refresh, and admin initialization/resume rows, navigation, loading, and failure. Prefer meaningful model tests; do not add snapshot tests that mirror markup.

Run affected JVM tasks such as :wishlist.features.users.common:jvmTest, :wishlist.features.email.server:jvmTest, :wishlist.features.auth.common:jvmTest, and :wishlist.features.admin.common:jvmTest, along with relevant auth/roles server tests and ui users/adminPanel JVM/JS compilation; include Android when the SDK is available. Select actual task names from the build configuration. Manual/browser smoke coverage on a supported environment should exercise own missing email→pending→click→approved and admin row navigation with the labeled SMTP field. Report unavailable platforms explicitly. Planning ran no source tests because this step is report-only.

## Non-goals and handoff

Non-goals are new login policy or account revocation, resend scheduling or rate limiting, email normalization overhaul, admin management rewrite, pagination/search subsystem, a new design system, mail-provider or recovery changes, deployment, and GitHub posting with invalid authentication. Token expiry or single-use redesign is out of scope unless required to prevent a new stale-approval bug. Existing process-local coordination limits must not be described as distributed safety.

## QUESTIONS FOR OPERATOR

There are no unresolved operator questions. No questions were relayed and no answers were required. Existing feature seams and conservative defaults resolve the request; architecture may choose internal API details consistent with the invariants, without human choice or expanded authorization.

The next handoff is Architecture. User-visible completion requires both email and admin acceptance, focused verification, and review. This step is planning only.
