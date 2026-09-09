Model: GPT-6 Astra
Changed files: agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/010-planning.md

The Orchestrator selected GPT-6 Astra for this resumed Planning role. The HL model follows Planning's priority because correcting startup, asynchronous admission, cleanup classification, and missing authorization evidence requires reasoning across several existing boundaries. No nested agent was used. This report is the only edit and uses normal prose; the final structured handoff uses AML-HIP.

## Cycle input and scope

Cycle 2 starts from Validation 009, commit `8d7f77d`, which failed with V78-01 through V78-08. Each retained finding enters this cycle with a consecutive validation repeat count of one. Planning does not increment or reset those counts; the next independent Validator determines whether each finding is resolved or repeated. The previous 815-test successful build is valid build evidence but does not establish the untested behavior.

I read all of 009-validating.md, the relevant Architecture 002 requirements, Coding 003 and Verification 008 evidence, the current implementation and tests, the branch change inventory, and the changed feature documentation. Earlier repository instructions and full feature README reads remain applicable. I also read the complete Coding rules to verify the KDoc and control-flow findings. Code navigation used the existing ast-index through escalated cache access. The live GitHub issue remains unchanged and has no comments. No source tests were rerun during Planning, no source was edited, and no index rebuild was necessary.

The current source confirms the two High causes: UI/users registers the sealed config serializer under polymorphic Any and ViewConfig, and PasswordChangeViewModel admits requests from asynchronously updated canSubmitState. EmailPasswordChangeService also catches storage failures in the same region as SMTP and converts them into DeliveryFailed. Existing tests lack production serializer construction and immediate repeated submissions. Those concrete defects, the remaining medium findings, and the two low findings all remain in scope. There are no operator questions.

Preserve the implemented ownership and behavior: authenticated owner-only issuance; exact current approved email; Auth-owned policy and BCrypt storage; immutable canonical UUID and subject equality; 15-minute expiry; purpose/type isolation; credential digest invalidation; coordinator-before-Auth lock order; authoritative reread and consume-before-write; non-cancellable bounded commit and lock release; token-authorized completion independent of browser identity; unchanged sessions/roles; trusted browser completion origin; and existing registration/admin behavior. No finding authorizes a new token system, schema, anonymous recovery endpoint, session policy, OS link integration, or distributed transaction framework.

## Corrections in implementation order

### V78-01: restore production JSON construction

Replace the two sealed-base registrations in UI/users `Plugin.kt` with four concrete registrations: Pending and Completed under Any, and Pending and Completed under ViewConfig. Keep the sealed interface, existing ViewModel factory, and typed platform factories. A typed factory accepting the sealed parent is valid and must not be changed merely because polymorphic serialization requires concrete subtypes.

Add a common test that invokes the actual UI/users plugin and Common's production Json registration, resolves the resulting aggregated Json, and round-trips Pending and Completed through both `PolymorphicSerializer(ViewConfig::class)` and `PolymorphicSerializer(Any::class)`. Assert the pending UUID/subject remain exact and Completed contains neither approval nor password. The test must fail on the current registrations during Json construction. Hand-building a substitute module or using only the sealed serializer is insufficient. Reuse this production Json in the navigation acceptance fixture.

### V78-02: make submission admission synchronous

In PasswordChangeViewModel, treat canSubmitState only as presentation state. On the UI dispatcher, synchronously inspect the immutable Pending config, raw loading/terminal state, current password, and current confirmation. Reject a completed, terminal, busy, mismatching, or policy-invalid state before constructing a request. Snapshot the accepted plaintext and immutable approval identity, claim loading before launching, and keep that claim until the request and result handling finish. A later input edit must not alter an admitted DTO. Use when or independent guards, resolving the else-if part of V78-07 at the same time.

Extend the shared fixture with a deferred completion response. Establish eligible inputs and drain once; then invoke submit twice without advancing the scheduler. The observable result is one request and at most one success transition. Separately make confirmation differ immediately before submit, and replace the password with policy-invalid input immediately before submit, while the derived state is still stale: both must send zero requests. Also test immediate valid input admission without requiring presentation recomputation, busy Enter/IME calls, and terminal/completed submission. Preserve the lifecycle-Job join fixes from Coding 005/007; scheduler draining alone must not replace those cleanup waits.

### V78-03: retain repository and cleanup failures

Restructure issuance so only false SMTP delivery and ordinary SMTP exceptions become DeliveryFailed. Minting, post-send account/Auth/deeplink reads, and cleanup errors must propagate to the existing sanitized 500 route boundary. Retain CancellationException propagation. Enroll the exact minted id before cancellation can discard cleanup ownership, with SMTP outside the locks and outside non-cancellable work.

Give each failed issued operation one explicit cleanup attempt. Keep the initiating repository or cancellation exception as the primary cause and attach cleanup failure as suppressed. A normal SMTP refusal may become DeliveryFailed only after successful cleanup; a failed cleanup is a server failure even when the triggering SMTP result was ordinary. For an SMTP exception followed by failed cleanup, preserve both causes rather than discarding the SMTP cause or returning a domain result. Do not catch a cleanup exception and silently retry its removal in the broad outer catch. DeepLinksService already owns cleanup when minting throws before returning the allocated id; preserve its original and suppressed exceptions unchanged through the Email service.

Regression tests must independently cover mint-before-commit failure, mint-commit-then-throw with cleanup failure, false SMTP followed by first-removal failure, ordinary SMTP exception with successful/failed cleanup, post-send repository failure with successful/failed cleanup, and cancellation. Record removed IDs and calls so a second successful removal cannot hide the first failure. Preserve unrelated registration and sibling approvals. Assert service exception identity/suppressed causes, then assert the route returns a sanitized server failure without exposing UUID, address, or credential data.

### V78-06: render current validation and native Done behavior

Expose current local validation from the shared ViewModel and collect it in each PasswordChangeView. Mismatch and policy feedback must be visible during ordinary editing even while the submit button is disabled. Keep untouched empty fields quiet while displaying the existing policy guidance. Feedback must clear when inputs become valid; terminal approval rejection and uncertain transport feedback remain distinguishable. Reuse the existing localized strings and submission enum where appropriate, without adding a parallel form framework. The admission method still reads raw values synchronously rather than trusting this derived feedback.

Configure both native fields as single-line password inputs with PasswordVisualTransformation, password keyboard semantics, and IME Done routed to the corrected guarded submit method. JS retains its real Form and submit handling. Test field editing, visible mismatch/policy feedback, disabled/busy submission, correction, and Enter/Done reaching the same one-request admission path. Completed views must render success/Continue without password inputs.

### V78-07 and V78-08: finish required conventions and documentation

Audit only Kotlin files newly introduced by issue #78 and declarations changed by these corrections. Add purpose-bearing KDocs for their classes, functions, class-level properties, overrides, and constructor parameters; do not rewrite unrelated legacy files. Replace new else-if chains with when. Add an explicit stable persisted name, proposed `email.password_change.v1`, to EmailPasswordChangePayload before its first release, keeping required security fields and redacted diagnostics. The production aggregated-Json test must assert that name and round-trip the payload within DeepLinkHandlerInfo. No new persistence migration or permissive missing-field defaults are needed for this unshipped feature.

Correct UI/users' three-screen overview to include the pending/completed password screen. Correct Auth's older claim that a Kotlin string-length limit prevents BCrypt's byte truncation; explicitly distinguish unchanged legacy registration behavior from the new UTF-8 limit. Update Email's cleanup description to match V78-03's actual exception and ownership behavior. Amend affected model/serializer/form prose only where behavior changes. Preserve every Operator Notes section.

## V78-04: security and transport evidence required for acceptance

Extend existing controllable in-memory repositories rather than replacing services with mocks. Use real Email orchestration, account coordinator, Auth, and deeplink services. Record password writes separately from bootstrap writes, and use a fresh eligible fixture for each invalid condition. The current combined stale-email/revoked-role test must be separated: revoked-role rejection must start with a matching approved email. Include an explicit mismatched submitted user ID, which the current test documentation claims but never submits.

One complete happy-path integration must assert the recorded recipient and actual HTML URL, open that exact emailed UUID through the real deeplink route, verify GET causes no consumption/password mutation, and complete only the bound account. Include a second account and pre-existing access/refresh credentials. Check the other hash is unchanged, the old password stops working, the new password works, existing access credentials still resolve their original subject, and existing refresh credentials remain usable. Snapshot roles or record bridge mutations to prove completion does not grant/revoke roles. Do not infer session preservation merely from a subsequent new login.

Use isolated table-driven or named cases for missing/unknown/malformed/consumed approvals, wrong handler ID, wrong payload type, subject mismatch, missing/deleted user, missing/purged password while the user row remains, cleared/changed/unapproved email, absent/revoked role bridge, changed fingerprint, and expiry at and after the boundary. Each rejection performs zero completion password writes. Prove invalid password leaves an otherwise valid approval available. Prove a same-plaintext administrator replacement invalidates an earlier approval because its salted hash changed, and prove one successful completion invalidates sibling approvals while another account's approval remains independent.

Introduce deferred entry/release gates around authoritative reread, removal, and password persistence. Force two submitters to overlap while the first holds the final critical section; do not count two immediate in-memory calls as contention proof. While a final callback is held, start a coordinated email mutation and an Auth password replacement/purge, verify each waits at its boundary, then release and assert completion and subsequent progress without deadlock. Replace or remove the initially read link before its final reread and assert rejection. Use completion signals and recorded event order, never sleep-based timing.

Retain each distinct commit-uncertainty case: removal throwing before commit; removal throwing after commit; password storage throwing before commit; password storage throwing after commit. No password write may follow either removal exception. Both password-write failures leave the approval consumed; retry cannot overwrite again. Cancel before entry to the bounded commit and during removal/password persistence. Before-entry cancellation consumes/writes nothing; in-region cancellation allows the bounded work to finish, propagates afterward, releases both locks, and cannot replay a committed change. These four failure points and three cancellation positions are materially different and must not be collapsed into one generic failure test. Issuance cancellation after mint return and after SMTP acceptance must likewise demonstrate exact-link cleanup ownership.

Resolve the actual Email/Auth/deeplink service, handlers, port, coordinator, and route registrations in fresh Koin graphs with Auth-first and deeplinks-first resolution. Cover SMTP enabled and disabled; verify one coordinator, working deferred resolution, production aggregated payload serialization, and existing registration bindings. Missing optional infrastructure must still produce the existing fail-closed contract. External repositories and SMTP may be faked, but the registration modules and service wiring under test must be production code.

Upgrade KtorPasswordChangeFeatureTest to install the real BearerAuthHttpClientConfigurator and DefaultUrlHttpClientConfigurator. Use recording credential and server-URL storage. Logged-out and unrelated logged-in completion, including a 401 response, must have no Authorization header, no refresh/login/getMe requests, and zero credential writes. Assert the exact final URL equals the issuing-origin completion endpoint even when the saved URL contains a different host, path, query, and credentials; assert the body retains the exact UUID, target assertion, and untrimmed password. Test ordinary non-opt-out requests and native completion retain current saved-server behavior. Keep existing non-success/malformed-result/cancellation checks.

Add tests for the production method/status logging helper and captured Ktor request logs on successful and failed approval/completion requests. Sensitive sentinels in path, query, body, and Location must not appear. Test DTO/config/payload redaction and the actual HTML shell's no-referrer meta appearing before external resources, together with real deeplink/completion response headers. Do not include complete sensitive test bodies in assertion messages.

## V78-05: owner action, real navigation, and concrete platforms

Add tests that call `onRequestPasswordChangeEmail`, not the older verification action. Cover eligible ordinary owner and root-on-self, anonymous/non-owner/root-on-other, null/unapproved email, mismatching private profile, disabled/unknown/loading/failed SMTP capability, refresh/busy state, and repeated clicks. Assert the exact displayed address, result feedback, reconciliation, retry after delivery/transport failure, and no admin/verification transport call. Suspend the request and change caller identity or authorization; obsolete results must not publish private feedback or issue a later private read. If those tests expose stale derived approval eligibility, tighten the action's raw admission check at the existing mutation boundary rather than introducing another lifecycle system.

Add a common real-chain test using the production ClientPlugin PasswordChangeViewInteractor. A confirmed change replaces Pending with Completed exactly once, removes the actionable UUID from the active tree, and Continue/Back leaves a usable users-list destination. Use the production serializers fixed by V78-01. A recording interactor alone is not this test.

Extend browser tests to instantiate WishlistsAppUrlNavigationConfigsRepo, start from the emailed redirect path, restore the scaffold's top/sidebar/main chains, save/load or recreate the adapter to simulate reload, and assert the exact subject/UUID survives. Drive the production interactor and verify the actual browser URL changes to `/password-changed` with no UUID. Direct completed-page load must not submit. Keep representative malformed-route and existing user/wishlist/email-marker regression checks. Segment-helper tests remain useful unit coverage but cannot substitute for the adapter and browser state.

Use a small browser-only Compose HTML fixture to mount the real owner editor and PasswordChangeView with controlled Koin/model state, then dispose composition and navigation jobs after each test. Assert actual password field types/labels, owner action visibility, current feedback, busy/disabled state, form submit, and completed content. Existing browser tests already execute; no screenshot service or live application deployment is required.

Instantiate the actual JVM and Android typed password factories for both concrete config variants and assert they produce PasswordChangeView with the original config. Add focused native form assertions for fields, feedback, completed branch, and password/Done semantics. The Gradle templates already provide Compose UI test dependencies for JVM and Android unit tests. Architecture should select the smallest supported host fixture; a test-scoped local runtime dependency is acceptable if Android rendering needs it, while new device-farm or emulator-provisioning work is outside scope. Compilation and shared ViewModel execution remain required and must be reported separately from concrete rendering assertions. Do not claim Android shared tests are absent: Verification 008/Validation 009 establish that they run.

## Test demand review and stop condition

The authorization, exception-ordering, cancellation, production serializer/DI, real HTTP configurator, owner lifecycle, and browser navigation tests above remain mandatory. They exercise application-specific risks, and two demonstrated production defects escaped the previous broad build. Test-count growth alone cannot close a finding; each finding needs an assertion that fails against the faulty boundary or independently establishes its retained invariant.

Narrow Architecture 002's broad test wording to avoid duplicate work: do not repeat the entire server failure matrix through every HTTP route or on each UI platform, enumerate every combination of independent invalid fields, or assert purely incidental private helper organization. Use parameterized fresh-fixture cases, shared native/JS ViewModel tests, one production web integration per critical path, and focused concrete platform smoke/form checks. Do not add visual pixel baselines, a full application end-to-end harness, native OS incoming links, or live SMTP. For browser referrer protection, production HTML ordering plus actual response policy-header assertions are the required application proof; rebuilding browser-standard policy conformance through a new external network test server is unnecessary unless the existing browser fixture can perform that check without additional infrastructure. Captured application logging and trusted-origin HTTP tests are not waived.

Coding should land the serializer/admission fixes first with their direct regressions, then correction-specific cleanup/form changes and the missing boundary tests, followed by KDocs and README corrections. Architecture must turn this plan into a bounded file/test inventory and retain all eight finding IDs. After source edits, rebuild ast-index and run focused suites before handing to Verification. Verification must execute the added JVM/JS/Android seams and the repository-required fresh build, record exact task/report evidence, and distinguish assertions from compilation. An unavailable platform execution target is a reported limitation, never a passing test. Stop after a full independent Validation closes all retained findings or reports the next concrete blocker. No operator choice or external coordination is currently required.

```text
ENTITY:
entity_id=issue_78_planning_010; type=correction_plan; state=ready_for_architecture

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_planning; memory_ref=[002-architecturing.md,008-verification.md,009-validating.md,010-planning.md]
* constraints=[report_only_edits,no_nested_agents,no_push,preserve_authorization_boundaries]; cycle=2; input_validation_commit=8d7f77d

ACTION:
1. action=retain_and_correct; target=V78-01; params={severity=High,repeat_count=1,acceptance=concrete_variants_round_trip_through_production_aggregated_json}
2. action=retain_and_correct; target=V78-02; params={severity=High,repeat_count=1,acceptance=synchronous_admission_one_request_zero_mismatched_requests}
3. action=retain_and_correct; target=V78-03; params={severity=Medium,repeat_count=1,acceptance=repository_failures_propagate_cleanup_causes_preserved}
4. action=retain_and_correct; target=V78-04; params={severity=Medium,repeat_count=1,acceptance=isolated_security_commit_cancellation_transport_and_graph_proof}
5. action=retain_and_correct; target=V78-05; params={severity=Medium,repeat_count=1,acceptance=owner_action_real_navigation_and_concrete_platform_proof}
6. action=retain_and_correct; target=V78-06; params={severity=Medium,repeat_count=1,acceptance=current_validation_feedback_and_guarded_native_done}
7. action=retain_and_correct; target=V78-07; params={severity=Low,repeat_count=1,acceptance=required_kdocs_when_branching_stable_payload_serialname}
8. action=retain_and_correct; target=V78-08; params={severity=Low,repeat_count=1,acceptance=accurate_screen_count_legacy_policy_and_cleanup_documentation}

REASON:
* condition=validation_009_failed; requirement=retained_findings→bounded_correction_cycle→independent_revalidation
* condition=green_build_without_required_boundary_assertions; requirement=production_regressions→targeted_acceptance_tests→evidence_backed_closure

EXPECTED RESULT:
* entity_id=issue_78_planning_010; new_state=architecture_handoff; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/010-planning.md

VERIFICATION:
* check=retained_finding_count; expected=8; repeat_count_policy=preserve_1_until_next_validation
* check=operator_questions; expected=none; implementation_tests=not_run_by_planning
* check=role_edit_boundary; expected=010_planning_report_only; source_changes=none

UNCERTAINTY:
* missing=next_cycle_execution_evidence; ambiguity=platform_runtime_fixture_support_pending_architecture
* missing=none_for_product_decisions; ambiguity=none_for_authorized_scope

REPETITION OF RESULT:
* entity_id=issue_78_planning_010; stored_in=shared_memory; status=available; persistence_medium=tracked_step_report_only

COMMUNICATION:
* sender=issue78_planning; receiver=architecture; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=22577218-054e-483f-a8ca-c851cbfd3701; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_planning_010,cycle_2_corrections]; persistence_medium=tracked_step_report_only

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true
* high_density=true; causal_chain_present=true; ambiguity_detected=false
```
