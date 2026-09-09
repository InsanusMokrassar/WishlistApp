Model: GPT-6 Astra
Changed files: agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/009-validating.md

The Orchestrator explicitly selected GPT-6 Astra with ultra reasoning for this independent Validating stage. This follows the role's HL-first priority because the review crosses authorization, asynchronous client state, persistence, cancellation, and platform startup. The role restriction and explicit prohibition on nested agents require this agent to write its own report. This report is the only edited file; no implementation or test source was changed.

## Validation result: FAIL

There are two High, four Medium, two Low, and zero Critical findings. Four findings concern production behavior: V78-01, V78-02, V78-03, and V78-06. Two concern missing acceptance evidence, and two concern documentation or coding conventions. All eight findings have a consecutive validation-cycle count of one; this task has no earlier Validating report, so no repeat escalation applies. The High findings require a restart from Planning under the stage state machine. Independently, the Medium count and authorization-related test gaps would prevent acceptance under the Medium Findings Decision Rule.

## Scope and independent evidence

I read AGENTS.md, SHORTCUTS.md, ALL.md and local.ALL.md, VALIDATOR.md, ORCHESTRATOR.md, MODELS.md, PROTOCOL.md, GIT.md, TOOLS.md, AST_INDEX.md, and the applicable Coding rules. No local Validator or Orchestrator override exists. I read the task PROMPT and numbered reports 001 through 008 in order, the complete branch diff against master, and the complete Auth, Email, deeplinks, users, UI/users, common, admin, and UI/auth feature READMEs, including Operator Notes. The live issue #78 still matches the captured requirements and has no comments. The review used the existing ast-index cache through approved escalated read access and began local dependency inspection under /home/aleksey/projects/own as required. The caveman skill applied to internal notes; this persisted report uses normal prose. No source-index rebuild was necessary because no source changed.

The fresh Verification evidence is valid as build evidence. I independently parsed the current XML and obtained exactly 182 suites, 815 tests, zero skipped, zero failures, and zero errors. The retained build log ends with BUILD SUCCESSFUL in 3m 27s and 4,530 actionable tasks, all executed. I did not rerun that already-fresh aggregate gate. Instead, I ran targeted read-only Kotlin diagnostics against the compiled production classes and the project's serialization/coroutine runtime dependencies. Those diagnostics exposed V78-01 and V78-02, which the current suite does not exercise. The installed diagnostic compiler is Kotlin 2.1.20 and used its metadata-version opt-out to load the project's already-built Kotlin 2.3.21 classes; the executed serialization and coroutine libraries were the project's 1.11.0 versions. No source or scratch test file was created.

Corrections 005 and 007 are justified and narrowly scoped. Joining the inherited lifecycle Job after cancellation observes the actual completion boundary, whereas draining the injected test scheduler does not await the inherited default-dispatcher collector. The two changed tests now preserve their original assertions while awaiting cleanup and cancellation output. Report 008 supplies a fresh successful repository build and fresh Node/browser results for both affected suites. The earlier Coding report's claim that Android common tests did not execute because a resource task was NO-SOURCE was explicitly corrected in 005; current Android XML contains the shared tests. These repaired lifecycle tests do not establish the missing production startup, submission-race, or acceptance coverage described below.

## Findings

### V78-01 — High: registering a sealed base as a polymorphic subtype prevents client JSON construction

At features/ui/users/src/commonMain/kotlin/Plugin.kt:68 and :69, the new registrations add PasswordChangeViewConfig itself as the subtype of Any and ViewConfig. PasswordChangeViewConfig.serializer() is a SEALED serializer, not a concrete subtype serializer. features/common/common/src/commonMain/kotlin/Plugin.kt:35 constructs the production Json and includes every registered module at :39. A read-only diagnostic using the built config class, useArrayPolymorphism=true, and the exact new registration failed during Json construction with: "Serializer for PasswordChangeViewConfig can't be registered as a subclass for polymorphic serialization because its kind SEALED is not concrete." This fails before encoding any approval. Every platform loading UI/users contributes the invalid module, so client JSON resolution and startup fail, including ordinary client operations.

Register PasswordChangeViewConfig.Pending and PasswordChangeViewConfig.Completed separately under both Any and ViewConfig using their concrete serializers. Add a test that resolves the production aggregated Json after the actual UI/users plugin registers its module, then round-trips both variants through the polymorphic ViewConfig and Any boundaries. A test using only the sealed serializer directly would miss the defect. Consecutive validation-cycle count: one.

### V78-02 — High: asynchronous eligibility permits a mismatched password submission and duplicate requests

features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:125 authorizes submission using canSubmitState.value, whose combine/stateIn pipeline at :80 updates asynchronously. The method does not synchronously recheck the current password, confirmation, loading, and terminal state before constructing the request at :135. Setting _loadingState at :136 does not immediately change the derived eligibility value.

Two diagnostics against the production ViewModel reproduced both consequences with the same StandardTestDispatcher seam used by the repository tests. After establishing matching valid inputs and draining the scheduler, two immediate onSubmitPasswordChange calls reported loading=true and canSubmit=true, then produced two completion requests. In a separate run, changing confirmation to a different value and immediately submitting reported Inputs match=false and still produced one request. The server receives no confirmation field, so it cannot repair that client matching-input violation. Server single-use validation prevents a second accepted password write, but does not prevent competing client results and navigation transitions. The existing invalidApprovalPreventsDuplicateSubmission test at features/ui/users/src/commonTest/kotlin/ui/PasswordChangeViewModelTest.kt:95 only retries after advanceUntilIdle and a terminal server result; it does not test admission while the first request is pending.

Read the current immutable config and mutable input/loading/terminal values synchronously in onSubmitPasswordChange, reject mismatch or policy failure there, and claim the loading slot before launching. Keep canSubmitState as presentation state. Add deterministic tests for two submissions without a scheduler turn, confirmation changed after eligibility became true, password invalidated after eligibility became true, and no second navigation on duplicate input. Both symptoms belong to this one root-cause finding. Consecutive validation-cycle count: one.

### V78-03 — Medium: issuance masks repository and cleanup failures as ordinary SMTP failure

features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt:102 catches every ordinary Throwable from minting, SMTP, post-send account reads, and cleanup, discards the cause, and returns DeliveryFailed at :104. A deeplink persistence failure with a suppressed cleanup failure is therefore hidden when createDeepLink never returns an id. A cleanup failure at :86 or :94 is also caught and retried; if the second removal succeeds, the first storage failure disappears entirely. The implementation consequently does not satisfy Architecture's explicit requirement that repository/cleanup failures remain server/transport failures, or the claim in features/email/README.md that cleanup failures remain visible. This does not itself prove an unauthorized password write, but it makes storage uncertainty indistinguishable from SMTP refusal and loses the compensation failure evidence.

Restrict ordinary DeliveryFailed conversion to SMTP refusal or ordinary SMTP exceptions. Perform exact-id cleanup while preserving the initiating exception and suppressed cleanup failures, and propagate minting, account-revalidation, and removal failures to the sanitized route error boundary. Add tests for mint-commit-then-throw with failed cleanup, initial removal failure, and post-send repository failure. Consecutive validation-cycle count: one.

### V78-04 — Medium: the security acceptance matrix is materially incomplete

The new server integration suite at features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt:127 uses one account and does not assert the recorded email recipient or actual emailed URL, existing access/refresh preservation, role preservation, or a second account's unchanged credential. Its invalid-state test at :155 never submits a mismatched user id despite its KDoc, and its revoked-role assertion at :174 still has the already-mismatched email from :170, so that completion rejection does not independently prove direct-role enforcement. The concurrent test at :222 starts two async calls over immediate in-memory operations without deferred commit gates; it proves repeated single-use behavior but does not force overlap at the lock or write boundary.

Architecture-required proofs remain absent for password-purpose/type isolation, deleted/purged credentials, sibling approvals, same-plaintext admin replacement, missing role bridge, authoritative reread replacement/removal, account-versus-Auth lock contention, and cancellation before/during consumption and password persistence. There are no separate remove-before/after-commit and password-write-before/after-commit failure tests. The new Email payload is not round-tripped through the production aggregated serializer, and the production new service/handler/Auth/deeplinks graph is not resolved in both construction orders. Existing unrelated registration and coordinator tests do not exercise these new boundaries.

The transport assertion at features/auth/client/src/commonTest/kotlin/KtorPasswordChangeFeatureTest.kt:62 checks request attributes, but its client at :126 installs only ContentNegotiation. It cannot demonstrate that the actual bearer configurator skips Authorization and refresh on a 401, that credential storage remains untouched, or that the actual DefaultUrlHttpClientConfigurator cannot overwrite a window-origin completion with a saved host/path/query/credentials. The required production formatter/captured logging and browser referrer checks are also absent. Implement the existing fake-repository, Ktor/MockEngine, clock, and coroutine-gate seams specified in 002. Isolate each invalid condition, assert zero password writes, and exercise the real configurators and serializers. These are available functional/security seams, independent of a DOM harness. Consecutive validation-cycle count: one.

### V78-05 — Medium: owner issuance and real navigation/platform acceptance were not tested

features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt:721 adds the actual password-email action, but no test invokes that action or asserts canRequestPasswordChangeEmailState. Existing owner-email verification tests exercise a different operation. The new fixture methods and model pass-through assertion do not prove SMTP/approved-email visibility, owner-versus-other/root identity, repeated clicks, request result reconciliation, or suppression of stale caller feedback for the password request.

client/src/jsTest/kotlin/PasswordChangeNavigationTest.kt:15 invokes only two segment helpers. It does not construct WishlistsAppUrlNavigationConfigsRepo, restore the scaffold, simulate reload, exercise the production ClientPlugin interactor at client/src/commonMain/kotlin/ClientPlugin.kt:173, observe the actual browser URL after completion, or resolve the production serializer. The ViewModel tests use a recording interactor and an empty node, so they cannot substitute for those lifecycle checks. Native factories and views compile, but no new test instantiates the typed password factories or asserts the concrete form/completed branches. These omissions explain why V78-01 and V78-02 survived the green aggregate build.

Add the planned owner-operation matrix, actual navigation-chain/interactor and serializer tests, browser adapter/reload tests, and concrete platform factory/form assertions. The lack of an existing controlled Compose HTML DOM harness is accurately disclosed and can be separately documented or addressed with a minimal test fixture; it does not block the common ViewModel, production Koin, navigation-chain, or Ktor seams. Android common ViewModel tests already execute, as the later evidence establishes. Consecutive validation-cycle count: one.

### V78-06 — Medium: planned form feedback and native keyboard submission are missing

The live passwordsMismatchState at features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:74 is not collected by any new platform view. The JS view reads only resultState at features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt:45 and disables submit at :91 when inputs mismatch. The JVM and Android views follow the same result-only pattern. A normal button-driven edit therefore presents a disabled submit button without the planned mismatch feedback; setting Mismatch inside an attempted submission does not make the ordinary disabled-button path display that message.

The two native password fields at features/ui/users/src/jvmMain/kotlin/ui/PasswordChangeView.kt:61 and features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt:63 omit the Architecture-required KeyboardOptions/KeyboardActions for guarded IME Done. They use only PasswordVisualTransformation and retain ordinary text-field keyboard/line defaults. Collect and render current mismatch/policy feedback independently of a disabled submit attempt, and configure native password keyboard semantics, a single-line field, and guarded Done handling through the corrected ViewModel method. Add focused form behavior assertions. Consecutive validation-cycle count: one.

### V78-07 — Low: new Kotlin files do not fully follow the required documentation and control-flow rules

features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:129 introduces an else-if chain despite the explicit ban in agents/CODING.md. Private class-level properties at :45, :50, :55, :60, and :65 have no required KDoc. New view overrides and constructor parameters, for example features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt:27 and :31, and new routing overrides such as features/auth/server/src/commonMain/kotlin/configurators/PasswordChangeRoutingsConfigurator.kt:34 also omit required documentation. The new server payload lacks the explicitly requested stable SerialName, relying on its Kotlin qualified name instead. Replace the branching chain with a suitable when, complete the required purpose/parameter KDocs across newly created files, and give the persisted payload an explicit stable serial name with a serialization test before shipping its first format. Consecutive validation-cycle count: one.

### V78-08 — Low: feature documentation retains contradictory or stale descriptions

features/ui/users/README.md:9 still states that the feature has three screens, despite the added password screen. features/auth/README.md:69 still claims the legacy 8..72 character check avoids BCrypt truncation beyond 72 bytes, while :76 correctly distinguishes the new UTF-8 byte policy from the unchanged legacy string-length check. Architecture explicitly required that clarification; leaving both statements gives conflicting guidance. Update the overview to include the password screen and correct the older registration-policy explanation without changing legacy behavior or Operator Notes. Other new route names, authentication placement, result names, trusted-origin behavior, TTL, and lock-order descriptions match the reviewed implementation, subject to V78-03's inaccurate failure-visibility claim. Consecutive validation-cycle count: one.

## Security, correctness, and scope conclusions

The server source follows the intended authorization ownership. Auth derives issuance caller identity from the authenticated route; Email independently requires configured SMTP/deeplinks, current exact non-null approved email, existing account/password, and direct User membership. The client predicate and existing mutation token/generation checks confine private owner feedback, including post-suspension reconciliation. SMTP runs outside the coordinator/Auth locks. Trusted URL construction reuses the existing configured-origin validator and never accepts a Host header or arbitrary redirect destination. The purpose-specific payload contains the bound subject, exact email, expiry, and a server-private credential digest. The original canonical UUID remains the key in the emailed URL, read-only handler redirect, pending config, DTO, and final lookup.

Completion is outside bearer authentication and derives its mutation target from the stored payload; the submitted user id is only an equality assertion. The JS binding is constructed from window.location.origin and a fixed completion path; the default-server opt-out and AuthCircuitBreaker are present at the correct request boundary. The ViewModel does not observe browser login identity. UUID parsing rejects malformed/noncanonical input without rewriting the approval. The final service callback rereads purpose and all payload fields, rechecks the exact 15-minute boundary with now >= expiry rejected, removes the exact record before the Auth write, and never restores an approval after an uncertain write. Auth revalidates current account/direct role/password and compares domain-separated SHA-256 credential-state digests before writing a newly salted BCrypt hash. Password replacement or purge therefore invalidates earlier credentials; no missing password is recreated by completion.

The lock order is coordinator followed by Auth write lock, and the final callback does not reacquire either. New Auth lock release is non-cancellable in finally. Cancellation is checked before the bounded non-cancellable consume/write region and after a successful commit. Removal exceptions prevent the password write; password-write exceptions occur after consumption. The source changes no token/refresh maps or role assignments. These are source-traced guarantees for the current single-process deployment, not substitutes for the missing contention/failure tests and not a distributed transaction claim.

The new DTO, payload, and pending config redact secrets from their string representation. The browser's no-referrer meta precedes external resources, deeplink/completion headers contain no-store/no-referrer, and the explicit CallLogging formatter emits only method/status. I found no demonstrated unauthorized password mutation or secret disclosure in the reviewed implementation; actual failed-request logging and referrer behavior still need the prescribed tests. Native typed factories use the navigation library's isInstance matching and can accept both variants; the failing JSON registration is a separate startup problem. Existing registration, email-verification promotion, administrator password policy, and credential/session behavior are not intentionally rewritten. The shared public-origin extraction and exact-id deeplink mint compensation are task-related changes. The code introduces no new module, schema, queue, anonymous recovery issuance, OS app-link integration, or distributed locking framework.

## Required handoff

Return to Planning for a new full cycle with V78-01 and V78-02 as correctness blockers and the remaining findings retained for Architecture and Coding. Do not mark the issue complete or publish a success handoff based solely on the passing build. No Critical finding requires an operator escalation comment, and no external message was sent by this role. The next Verification must execute the added regression seams and the required build before another independent Validating pass. This report is the only file to stage and commit.

```text
ENTITY:
entity_id=issue_78_validation_009; type=validation_report; state=fail_restart_planning

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_validating; memory_ref=[PROMPT.md,001-planning.md,002-architecturing.md,003-coding.md,004-verification.md,005-coding.md,006-verification.md,007-coding.md,008-verification.md,009-validating.md]
* constraints=[report_only_edits,no_nested_agents,no_source_changes,no_push]; branch=fix/issue-78-email-authorized-password-change

ACTION:
1. action=record_finding; target=V78-01; params={severity=High,repeat_count=1,production_behavior=true,location=features/ui/users/src/commonMain/kotlin/Plugin.kt:68,fix=register_concrete_pending_and_completed_serializers}
2. action=record_finding; target=V78-02; params={severity=High,repeat_count=1,production_behavior=true,location=features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:125,fix=synchronous_input_loading_terminal_admission}
3. action=record_finding; target=V78-03; params={severity=Medium,repeat_count=1,production_behavior=true,location=features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt:102,fix=propagate_repository_failures_preserve_cleanup_causes}
4. action=record_finding; target=V78-04; params={severity=Medium,repeat_count=1,production_behavior=false,location=features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt:155,fix=complete_security_transport_failure_matrix}
5. action=record_finding; target=V78-05; params={severity=Medium,repeat_count=1,production_behavior=false,location=client/src/jsTest/kotlin/PasswordChangeNavigationTest.kt:15,fix=exercise_owner_operation_real_navigation_and_platform_factories}
6. action=record_finding; target=V78-06; params={severity=Medium,repeat_count=1,production_behavior=true,location=features/ui/users/src/androidMain/kotlin/ui/PasswordChangeView.kt:63,fix=render_current_validation_and_wire_native_password_done_controls}
7. action=record_finding; target=V78-07; params={severity=Low,repeat_count=1,production_behavior=false,location=features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:129,fix=complete_kdocs_when_branching_stable_payload_serialname}
8. action=record_finding; target=V78-08; params={severity=Low,repeat_count=1,production_behavior=false,location=features/auth/README.md:69,fix=correct_legacy_byte_claim_and_screen_overview}
9. action=restart; target=issue_78_validation_009; params={next_role=planning,reason=high_correctness_findings,full_cycle_required=true}

REASON:
* condition=High_findings_present; requirement=High_findings→restart_planning→full_cycle_revalidation
* condition=Medium_count_4_and_authorization_coverage_gaps; requirement=Medium_findings→mandatory_coding_correction→verification

EXPECTED RESULT:
* entity_id=issue_78_validation_009; new_state=handed_to_orchestrator; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/009-validating.md

VERIFICATION:
* check=severity_counts; expected={Low=2,Medium=4,High=2,Critical=0,total=8,production_behavior=4}
* check=fresh_existing_suite; expected={suites=182,tests=815,failures=0,errors=0,skipped=0}
* check=aggregated_serializer_registration_diagnostic; expected=IllegalArgumentException_SEALED_not_concrete; result=reproduced
* check=immediate_duplicate_submit_diagnostic; expected=request_count_1; actual=request_count_2
* check=immediate_mismatched_confirmation_diagnostic; expected=request_count_0; actual=request_count_1
* check=role_edit_boundary; expected=009_validating_report_only; result=preserved

UNCERTAINTY:
* missing=required_security_and_platform_acceptance_tests; ambiguity=runtime_boundaries_without_forced_integration_proof
* missing=controlled_browser_dom_assertions; ambiguity=acknowledged_harness_gap_separate_from_available_functional_seams

REPETITION OF RESULT:
* entity_id=issue_78_validation_009; stored_in=shared_memory; status=available; persistence_medium=tracked_step_report_only; validation_result=FAIL

COMMUNICATION:
* sender=issue78_validating; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=4e5e51e2-9eaf-4d3f-a1bf-706e52d22c26; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_validation_009,password_change_review]; persistence_medium=tracked_step_report_only

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true
* high_density=true; causal_chain_present=true; ambiguity_detected=false
```
