Model: Codex GPT-6 (HL independent Validation reasoning, following HL-before-ML priority); gpt-5.6-luna (required LL report materialization).
Changed files: agents/task/18.09.2026_07.24.45-9d85e0a2-9584-4775-bbfd-2774f2d30a33/009-validation.md

# Validation: email-owned verification state

## Validation Result: FAIL

The independent review found two Medium issues in test correctness and required migration coverage. No production defect, security defect, or destructive schema change was identified. The implementation should return to Coding for the focused corrections below before a new Verification and Validation pass. Neither finding requires a new product design or an operator decision.

## Reviewed scope and evidence

The review covers the incremental source, test, documentation, and report changes from task bootstrap commit `c046d7a` through verification commit `110f3299ee1c391e51ef757d303e852b7ec2334f`, in the context of issue 79 and pull request 82, `feat/issue-79-user-email-change`. The task prompt's email-model ownership requirement and architecture 002 govern this continuation. The live issue and PR were read for context; their earlier verification totals are not substituted for this task's evidence.

Validation read the repository instructions, the available local override, the task prompt and steps 001 through 008, and the email, users, auth, admin, users-UI, and admin-panel READMEs. Navigation used the existing ast-index and direct source reads; focused literal checks supplemented property searches. The actual production and test diffs were inspected, including persistence transactions, reduced serializers and mappers, fresh-read adapters, coordinator and SMTP paths, authenticated routing, client decoding, MVVM reconciliation, and the changed test doubles.

Verification 008 records fresh successful gates against coding commit `e0bbbd59efbae1724bedd60a6a99de320aa23242`: 377 JVM test executions, 298 JS/Android executions, and eight PostgreSQL executions, totaling 683 with zero failures, errors, or skips. It also records successful JVM/JS/Android compiles and the aggregate build with 4,605 actionable tasks and exit code zero. These results were accepted as prior Verification evidence, with test sources inspected independently; Validation did not rerun the aggregate build or claim a new execution of those gates. Available test-result artifacts and the report-only verification commit were inspected.

## Medium findings

### VEO-01: Explicit clearing is incorrectly treated as a no-op by migrated test doubles

The introduced `updateObject` branch at `features/email/server/src/commonTest/kotlin/services/FakeUsersRepo.kt:78` compares `newValue.email` with both nullable stored slots before handling `newValue.email == null`. The same branch ordering appears at `features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt:66`, `features/admin/server/src/commonTest/kotlin/UsersManagementFeatureTest.kt:60`, and `features/roles/server/src/commonTest/kotlin/FakeUsersRepo.kt:32`.

With current email A and no pending replacement, `update(id, NewUser(username, null))` matches the null pending slot and preserves A instead of clearing the email lifecycle. The previous full-update fake cleared the current email in this case, and the production repository still clears correctly. This is a regression in the service-test dependency behavior: the affected doubles can conceal a failure or produce misleading results when full updates explicitly clear an address.

Move explicit-null handling before same-address comparisons in the affected full-update implementations. Check the corresponding dedicated-write branches for the same ordering issue while touching these doubles. Add focused assertions proving that full update clears both an unapproved first address and an approved current address with no pending replacement, including all lifecycle metadata, while non-null same-address updates remain no-ops. This finding is Medium because production behavior is correct but the changed test infrastructure no longer preserves the required clear contract.

### VEO-02: Migration tests do not cover the immediately preceding populated lifecycle schema

The legacy schema created at `features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt:468` contains neither `pending_email` nor `email_change_allowed_at`. The PostgreSQL fixture at `features/users/common/src/jvmTest/kotlin/repo/PostgresUsersRepoTest.kt:310` has the same limitation. Pending addresses and approval deadlines used by later assertions are written only after initialization with the new repository.

These tests prove an older additive migration and persistence of newly written state, but they do not prove the explicit architecture requirement to preserve populated current/pending/deadline values when adding only `email_change_requested_at`. They also do not prove that an already pending legacy candidate keeps an unknown request time after reopening and same-address saves. The passing test totals therefore do not establish this part of the promised migration contract.

Add SQLite and PostgreSQL fixtures representing the immediately preceding schema, including populated pending and deadline columns but no request-time column. Seed an approved current address with a pending replacement and deadline, plus a first unapproved address and an empty account. Initialize and reopen twice; assert unchanged raw existing values and null requested-at. Save the same current and pending candidates and verify that their unknown time stays null; then verify that a different accepted candidate receives the controlled clock value. Retain the existing disposable-database and bounded cleanup discipline. This finding is Medium because the source uses a suitable additive nullable column, but the required preservation proof is incomplete.

## Acceptance areas that passed source review

Model ownership is correct. `EmailProfile` in email/common contains the six intended fields and does not wrap or implement a user model. `RegisteredUser`, `AuthFeatureUser`, and `AdminUser` retain only identity, current email, and approval; their serializers, mappings, and reverse-mapping arguments contain no pending/request/deadline metadata. Public `UsersFeatureUser` remains exactly id and username. Exact serializer/JSON-key tests and compatible decoding of old extra fields support the contraction. No hidden user-model compatibility wrapper or pending-state alias was found.

The profile-editing boundary is correct. `UsersModel.getMyEmailProfile()` returns `EmailProfile`, and `DefaultUsersModel` delegates directly to `EmailFeature.getMyEmail()`. The obsolete direct `ClientAuthFeature` constructor dependency and `getMyProfile` surface are removed. Auth state remains responsible for caller identity and authorization. The ViewModel exposes email-owned state and compares its Long owner identifier with the expected user id.

Production lifecycle and timestamp writes are coherent. The repository stores first unapproved candidates in current email and retains the approved current address while a replacement occupies pending email. New candidates receive a post-lock clock sample in the same transaction. Same-current/same-pending and username-only paths retain request time; reads and SMTP resends do not write it. Approval and explicit clear remove request time, and raw complete-clear classification includes timestamp-only residue. Cooldown remains rooted at approval and admits exact expiry. The inspected rollback, batch, stale-link, overflow, lock-order, raw cross-slot uniqueness, and post-commit event paths preserve their established behavior. VEO-02 limits the migration-test proof, rather than identifying a faulty production transition.

Owner privacy and failure handling are correct. The authenticated GET route derives its target exclusively from the bearer principal, ignores forged target query parameters, returns an empty profile for an existing email-less owner, and returns 404 for a missing profile. Anonymous and invalid bearer requests are rejected before service access. The client enforces successful status and non-null model decoding; only explicit 404 maps to null, while other status, malformed-body, transport, and cancellation failures propagate. Route/client tests exercise these boundaries.

Freshness and delivery ownership are correct. `getEmailProfileFresh` is a mandatory repository capability, the Exposed implementation projects one users row, and the cache adapter delegates directly to backing storage. Both SMTP graph shapes use the shared coordinator. Verification chooses the candidate from email-owned state, passes an exact id/recipient pair to the sender, and rechecks after SMTP. The registration adapter uses only the first unapproved current address. Existing request-local link compensation and stale-delivery classifications remain intact; no synthetic user is constructed to carry a pending recipient.

UI behavior is preserved by the shared state transition and platform contracts. All four feedback-snapshot construction sites include requested-at, and new tests prove that an otherwise identical timestamp refresh retires saved and successful-delivery feedback while preserving a dirty draft. Owner/session/target guards, cancellation, checked reconciliation reads, approved-current retention, cooldown feedback, disabled-SMTP storage, and desktop renderer privacy remain covered. The recorded JS Node, JS Browser harness, Android unit, and platform compile gates support the common model transition.

The schema and scope remain lean. The new nullable request-time column has no backfill or fabricated legacy timestamp, and no table split or destructive column contraction was introduced. Documentation describes coordinated matching-version rollout, stopped old writers, backup restoration or a compatible backport for rollback, and the limits of mixed-version support. README and KDoc changes generally match the implementation, and Operator Notes were not modified in the reviewed diff. The report sequence follows the planned ownership checkpoints; the two findings identify the remaining acceptance gaps.

## Hygiene and residual limits

`git diff --check c046d7a..HEAD` passed, and `git status --porcelain=v1` was empty during review and again before preparing this report. Validation changed no implementation, tests, feature documentation, schema, configuration, or dependency files. It did not push or rerun the stopped PostgreSQL fixture. The only repository change belonging to this role is the validation report materialized from the reviewed payload.

The automated evidence does not claim live SMTP delivery, browser DOM interaction beyond the configured harness, physical Android rendering/IME execution, or production deployment. Those remain existing limits. No additional product defect was found, but the green gates cannot replace the missing migration scenario or correct the contradictory fake-clear behavior.

## Conclusion

Validation fails on VEO-01 and VEO-02, both Medium. Return to Coding for the test-double clear correction and the populated-legacy migration proof, run the affected fresh gates including PostgreSQL, and request independent revalidation of the resulting changes. No High or Critical finding was identified.
