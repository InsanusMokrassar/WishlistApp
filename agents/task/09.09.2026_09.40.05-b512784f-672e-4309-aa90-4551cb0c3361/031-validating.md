Model: Inherited Codex validation agent, assigned to the independent HL validation role. The inherited model was retained for the cross-boundary correctness review; no nested agent was created because the role assignment explicitly prohibited delegation.
Changed files: agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/031-validating.md

## Validation result: FAIL

I independently validated clean HEAD `8a20258f7fd9adfac1ae38758691909322e0077e` on `fix/issue-78-email-authorized-password-change`. There are two High production findings, two retained Medium test-evidence findings, two retained Low documentation/style findings, and one new Low process finding. There are no Critical findings. The required next stage is Planning, using the next monotonic step number, 032. Passing Verification 030 does not override these independently reproduced correctness failures.

I read AGENTS/SHORTCUTS, the applicable ALL/local, Validator and protocol instructions, PROMPT, the complete prior Validation 009, Planning 010, Architecture 011, Coding 012–029, and Verification 030. I checked the live issue #78 and the affected Auth, Email, DeepLinks, UI/users, and Common READMEs, including their unchanged Operator Notes. I inspected the actual `master..HEAD` change set, affected production implementations, and their tests. The diff contains 114 files, 8,771 insertions, and 62 deletions, including the accumulated reports. Caveman full was used only for internal working notes; verify-and-stop kept the work to existing-state inspection, bounded diagnostics, and this report. Product code, tests, documentation, and earlier reports were not edited.

## New High findings

### V78-09 — The global sanitized error boundary changes unrelated HTTP contracts

This is a new High production regression, with repeat count 1. At `features/common/server/src/jvmMain/kotlin/utils/CallLoggingFormat.kt:28–32`, `exception<Throwable>` converts every non-cancellation exception to HTTP 500. `features/common/server/src/jvmMain/kotlin/JVMPlugin.kt:99–104` installs that handler application-wide. Expected Ktor client-error exceptions are therefore no longer mapped to their normal statuses. Existing registration, login, and refresh routes use unguarded `call.receive` at `features/auth/server/src/commonMain/kotlin/configurators/AuthRoutingsConfigurator.kt:32`, `:41`, and `:50`; malformed requests previously used Ktor's client-error handling.

I reproduced the behavior without modifying files. A Kotlin command-line diagnostic loaded the freshly compiled Common server helper and the project's Ktor 3.5.2 runtime, then ran two otherwise identical `testApplication` instances. Both registered a GET route that threw `BadRequestException("invalid request")`; only the second installed `StatusPages { installSanitizedUnhandledErrorBoundary() }`. The exact results were `sanitized=false status=400` and `sanitized=true status=500`. The sanitized instance logged only `GET 500`, so sanitization itself worked; the status contract did not.

The fix must preserve standard client-error classification while preventing sensitive exception text from reaching responses or logs. Add a regression through existing Auth malformed-body routes and the actual global configurator, not only through the password-change routes, which already handle their own body errors. The issue authorizes secret-safe logging, not converting unrelated client errors into server errors.

### V78-10 — Completion navigation can cancel its own credential-free persistence

This is a new High production lifecycle defect, with repeat count 1. At `client/src/commonMain/kotlin/ClientPlugin.kt:184–196`, the production interactor waits for the replacement hierarchy in a child of the caller's coroutine before invoking `navigationConfigsRepo.save`. The caller is the pending screen's lifecycle-bound work scope at `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:40` and `:155–162`.

The exact installed Navigation 0.7.7 dependency destroys the replaced pending node before emitting the new stack. Its `navigation.mvvm` `commonMain/ViewModel.kt:21–25` cancels the ViewModel scope on that destruction. Consequently, the caller and its `hierarchy` child can be cancelled before `hierarchy.await()` returns and before the explicit save executes. The exact 0.7.7 `commonMain/repo/HierarchyRepoUpdater.kt:35–56` reacts to added/removed nodes but does not save replaced-only diffs, so the ordinary hierarchy saver is not a reliable fallback.

I reproduced the race against the compiled production `ClientPlugin` interactor, a real 0.7.7 `NavigationChain`, and the real base ViewModel lifecycle. The diagnostic created a users-list node and a pending node, attached a base ViewModel to the pending node, and called the production interactor from that ViewModel's scope. A recording `NavigationConfigsRepo` counted saves. Thirty iterations produced `iterations=30 explicit_saves=14 caller_cancelled=16`. The diagnostic used an Unconfined work dispatcher while retaining the real ViewModel job and its Default-dispatcher destruction collector; this demonstrates a permitted lifecycle ordering, not a measured platform failure rate. The production password-change ViewModel retains the same lifecycle job.

Existing `client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt:64–75` creates Empty nodes without a ViewModel and invokes the interactor from the independent test scope. The browser restoration test also does not combine successful submission with destruction of the actual submitting ViewModel. Those tests cannot catch cancellation caused by their own navigation operation. On the failing ordering, the visible chain can become Completed while persisted navigation, including the browser URL, remains Pending with the approval UUID. Refresh restores the consumed approval instead of the completed screen.

The next cycle must assign completion-transition persistence to an owner that survives destruction of the pending screen, preserve intended cancellation behavior, and prove successful submission, actual node destruction, persisted Completed state, and reload restoration together. A test that merely calls the interactor outside the screen lifecycle is insufficient.

## Retained findings

### V78-04 — The security matrix remains incomplete

This remains Medium, test evidence rather than a demonstrated authorization exploit, with repeat count 2. The new issuance, commit, graph, routing, transport, and cancellation tests materially improve coverage. However, Coding 028's claim that the existing tests retain sibling invalidation, credential/session, and role evidence is not supported by the actual assertions.

There is still no real second password-change approval for the same user followed by successful completion of the first and rejection of the sibling, together with an independently usable approval for another user. The records called “sibling” and “unrelated” at `features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeIssuanceTest.kt:83–89` are generic handler/string records used for cleanup noninterference, not usable password-change approvals.

The production routing fixture obtains existing owner and unrelated credentials at `features/email/server/src/commonTest/kotlin/PasswordChangeFlowRoutingTest.kt:189–201`, but retains only access tokens and does not prove that pre-existing access and refresh sessions survive completion. Its completion helper at `:265–285` tests new-password login, not existing-session preservation. Role fixtures also lack role-mutation counters or before/after authorization snapshots sufficient to prove no role grants or changes. These invariants look preserved by the inspected production write path, but were explicitly required as regression evidence.

The absent-role-bridge case at `features/email/server/src/commonTest/kotlin/services/EmailPasswordChangeServiceTest.kt:236–239` also supplies the invalid fingerprint `untrusted-state`, so rejection does not independently establish the missing-bridge guard. Seed an otherwise valid approval for each independent rejection condition. Retain the positive same-plaintext salted administrator replacement, final reread, overlap, write-failure, and cancellation cases already present. Do not replace the missing assertions with test-name or suite-count claims.

### V78-05 — Owner visibility and lifecycle-integrated navigation evidence remain incomplete

This remains Medium, test evidence, with repeat count 2. The six owner ViewModel tests now establish permitted owner/root-on-self requests, several raw authorization rejections, busy admission, result handling, and identity-loss suppression. They never assert `canRequestPasswordChangeEmailState`; the sole owner browser DOM test is positive-only. Unknown/loading capability, a held refresh, and negative rendered visibility under disabled SMTP, missing/unapproved email, another user, and root-on-other therefore lack the requested direct evidence. The “stale derived” test at `features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelPasswordChangeTest.kt:166–182` uses an Unconfined dispatcher and awaits reconciliation before the valid submission; it does not prove both deliberately stale derived eligibility directions. The test at `:190` does not start a later mutation despite its name claiming protection of a later mutation.

Source inspection supports the intended gating: raw admission requires current authorization/identity, Enabled capability, the matching private profile and non-null approved email, and no loading/busy operation. Refresh clears the profile before probing and on failure at `features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt:379–422`. The form renders the request button in the approved-profile branch and disables it using request eligibility. I did not establish a production SMTP-gating bypass.

Concrete UI evidence is no longer missing: the JS tests mount production Compose HTML in JSDOM and exercise DOM input/submission; JVM Compose and Android Robolectric tests use production factories and assert masked fields, IME Done, invalid input, busy state, and completed-form absence. However, the remaining production-interactor/lifecycle combination is absent and demonstrably matters, as separately recorded in V78-10. Preserve the genuine host tests while adding the missing combined proof.

### V78-07 — The required declaration audit is not complete

This remains Low, style/documentation, with repeat count 2. Stable `@SerialName("email.password_change.v1")` and the introduced else-if issue are corrected. Nevertheless, the newly introduced backing declarations at `features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt:45`, `:50`, `:55`, `:60`, and `:65` still lack the KDocs required by the repository's all-declarations rule. Several new route/client/service overrides and view constructor members also lack declaration-level documentation. The platform plugin descriptions remain inaccurate; for example, `features/ui/users/src/jsMain/kotlin/JSPlugin.kt:19` still lists only three factories while a password-change factory is registered at `:39–42`. Coding 028's T16 closure statement is therefore too strong. Audit the bounded issue-78 declarations without an unrelated repository-wide rewrite.

### V78-08 — README accuracy is only partially repaired

This remains Low, documentation, with repeat count 2. The four-screen count and Auth's distinction between legacy character checks and the new UTF-8-byte limit are now accurate. However, `features/ui/users/README.md:13` still says the web edit screen remains Material/Bootstrap, while `features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt:119–125` and its owner-email controls use Calm Studio. The new explicit completion persistence/null history-state implementation and globally installed StatusPages behavior are not explained in the affected feature documentation despite the cycle's architecture/documentation audit requirement. Update the descriptions after correcting V78-09 and V78-10; do not document the current defects as intended behavior. Operator Notes were unchanged.

## New Low process finding

V78-11 has repeat count 1. The structured handoff blocks in Coding 012–029 omit mandatory `REASON` and `EXPECTED RESULT` sections. Verification 030 omits `REASON` and `UNCERTAINTY`. These blocks nevertheless claim `format_valid=true`. A read-only check of the actual fenced ENTITY blocks reproduced those omissions in all 19 reports. This violates the explicit mandatory AML-HIP structure, independently of the correctness of their prose or build results. Existing step files are immutable; the next cycle should acknowledge superseded incomplete handoffs and emit complete new blocks, rather than rewrite prior steps. This report supplies all mandatory sections and an actual UUID message identifier.

## Resolved prior findings and acceptance audit

V78-01 is resolved: production serializer construction now registers concrete Pending and Completed types under both polymorphic bases, and the production Json path is exercised. V78-02 is resolved: submission checks raw password/confirmation, policy, terminal/succeeded state, and loading synchronously, claims loading before launching, and captures an immutable request. Derived StateFlow lag cannot admit a mismatched or duplicate request through that guard. V78-03 is resolved: only SMTP exceptions become DeliveryFailed; one cleanup owner removes the exact allocated identifier, preserves the primary failure with cleanup suppressed, and propagates cancellation/repository failures. V78-06 is resolved: live mismatch/policy feedback and masked, single-line native fields with Done actions are present. Each resolved finding had historical repeat count 1; the consecutive unresolved count is now zero. No finding reaches the three-cycle automatic escalation threshold.

For the issue's six acceptance conditions, the inspected implementation supports owner/SMTP/approved-email admission, sends only to the currently approved address, redirects through a purpose-specific read-only deeplink handler with the exact UUID, and submits that immutable UUID without substituting the browser's current account. The server validates canonical UUID, positive subject, payload type/purpose, expiry, approved-email identity, direct role, account/password existence, and the salted credential fingerprint. Invalid local policy does not consume the approval; a valid completion performs a final payload/expiry reread, consumes before writing under the existing Auth password lock, and makes old approvals stale by changing the fingerprint. Cancellation before commit avoids writes; cancellation during the non-cancellable consume/write section is propagated after the commit boundary. The source does not mutate sessions or grant roles, but the retained V78-04 proof gaps remain.

The dedicated completion route is outside bearer authentication, while issuance authenticates the actual caller. Production DI, serialized payload reconstruction, anonymous/unrelated/expired-browser authorization independence, HTTP status/result handling, SMTP failure separation, transport uncertainty, cancellation propagation, canonical absolute browser endpoint, Auth circuit-breaker/URL-configuration bypass, no-store/no-referrer headers, and status-only request logging have focused evidence. The new global error boundary fails preservation of unrelated HTTP status behavior under V78-09. Passwords remain in ViewModel memory and DTO bodies rather than navigation configurations; cleanup on destruction and success exists. The completed-route persistence guarantee fails under V78-10.

The JS URL repository's `pushState(null, ...)` at `client/src/jsMain/kotlin/UrlNavigationConfigsRepo.kt:343–353` is not itself a demonstrated defect: the repository restores from pathname/search, not history.state. Existing user/wishlist parsing is retained, password-change routes validate their extra segments, and the browser adapter test exercises base-path save/restore. The null state avoids attempting to structured-clone Kotlin hierarchy objects. That evidence is separate from the lifecycle race that can prevent save from being called at all. JSDOM 26.1.0 is a DOM host, not a graphical browser; its successful tests must not be described as proof of native browser keyboard default actions or full browser navigation behavior. JVM and Robolectric form tests are concrete hosts, not merely shared ViewModel tests.

## Verification evidence and limits

Verification 030 ran `./gradlew build --rerun-tasks 2>&1 | tee /tmp/build-output.txt` with pipefail and reported exit 0. I checked the log's `BUILD SUCCESSFUL in 5m 7s` and `4548 actionable tasks: 4548 executed`, and independently aggregated the current XML: 208 suites, 944 tests, zero failures, errors, or skipped tests. The build's source HEAD was `9ab5a618f358474558ecae73c21df8d6c0e1e857`; the only difference from that SHA to the validated SHA is Verification 030 itself. The build evidence therefore matches the validated product tree. I did not rerun the broad build unnecessarily.

I independently ran `git diff --check master..HEAD`, which passed, and both read-only compiled-runtime diagnostics described above. Kotlin scripting used installed project runtime jars and `-Xskip-metadata-version-check` because the available scripting compiler was older than the compiled project's metadata. Initial script attempts needed classpath corrections, including the installed Maven-local Navigation jars and Compose runtime; those tool setup failures were not product findings. The final diagnostics completed successfully and printed the reproduced failures recorded above. No temporary source or test files were introduced. The working tree was clean before creating this report.

Planning must address both High defects and retain the four unresolved prior findings with their identities/counts. The Orchestrator still owns the Medium/Low disposition, but cannot accept a PASS while either High production defect remains. Validation changes only this report and makes no external write, push, or PR mutation.

```text
ENTITY:
entity_id=issue_78_validation_031; type=independent_validation; state=FAIL

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_validating_cycle2; validated_head=8a20258f7fd9adfac1ae38758691909322e0077e; branch=fix/issue-78-email-authorized-password-change
* prior_validation=009; validation_cycle=2; constraints=[report_only_edit,no_nested_agents,no_push,no_checkout,Operator_Notes_unchanged,prior_reports_immutable]

ACTION:
1. action=retain_finding; target=V78-04; params={severity=Medium,category=test_evidence,state=open,repeat_count=2,missing=[real_sibling_invalidation,other_user_approval_independence,existing_access_refresh_preservation,role_mutation_assertions,isolated_missing_bridge_case]}
2. action=retain_finding; target=V78-05; params={severity=Medium,category=test_evidence,state=open,repeat_count=2,missing=[negative_owner_DOM_visibility,held_loading_refresh,stale_derived_admission,lifecycle_integrated_persistence]}
3. action=retain_finding; target=V78-07; params={severity=Low,category=style_documentation,state=open,repeat_count=2,remaining=[backing_state_KDocs,override_KDocs,platform_factory_descriptions]}
4. action=retain_finding; target=V78-08; params={severity=Low,category=documentation,state=open,repeat_count=2,remaining=[web_edit_technology,persistence_boundary,error_boundary]}
5. action=record_finding; target=V78-09; params={severity=High,category=production,state=open,repeat_count=1,location=features/common/server/src/jvmMain/kotlin/utils/CallLoggingFormat.kt:29,cause=global_Throwable_to_500_mapping,observed=[baseline_400,sanitized_500]}
6. action=record_finding; target=V78-10; params={severity=High,category=production,state=open,repeat_count=1,location=client/src/commonMain/kotlin/ClientPlugin.kt:184,cause=pending_node_destruction_cancels_persistence_caller,iterations=30,saves=14,cancelled_callers=16}
7. action=record_finding; target=V78-11; params={severity=Low,category=process,state=open,repeat_count=1,locations=[012_coding..029_coding,030_verification],cause=mandatory_AML_HIP_sections_absent}
8. action=resolve_findings; target=[V78-01,V78-02,V78-03,V78-06]; params={state=resolved,prior_repeat_count=1,consecutive_unresolved_count=0,evidence=[concrete_serializers,raw_synchronous_admission,single_cleanup_ownership,live_feedback_native_Done]}
9. action=handoff; target=orchestrator; params={result=FAIL,restart_stage=Planning,next_step=032,critical=0,high=2,medium=2,low=3,open_total=7,resolved_prior=4,automatic_severity_escalations=0}

REASON:
* condition=V78-09_global_client_error_status_regression; requirement=preserve_unrelated_HTTP_contracts; causal_chain=global_Throwable_handler→BadRequestException_500→High_correctness_failure
* condition=V78-10_lifecycle_cancellation_before_save; requirement=credential_free_completed_persistence; causal_chain=pending_node_destruction→caller_cancellation→missing_completed_save
* condition=High_findings_present; requirement=VALIDATOR_High_restart_rule; causal_chain=High_findings→validation_FAIL→Planning_032

EXPECTED RESULT:
* entity_id=issue_78_validation_031; new_state=Planning_restart_required; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/031-validating.md
* entity_id=issue_78_cycle_3; new_state=awaiting_Planning; location=agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/032-planning.md

VERIFICATION:
* check=validated_product_tree; expected=clean_8a20258f7fd9adfac1ae38758691909322e0077e; result=matched
* check=existing_fresh_build; expected=source_equivalent_green; result=4548_executed_5m7s; source_delta=030_report_only
* check=XML_aggregation; expected=zero_failures_errors_skips; result={suites:208,tests:944,failures:0,errors:0,skipped:0}
* check=global_StatusPages_contract; expected=400_preserved; result=500_reproduced; diagnostic_runtime=Ktor_3.5.2
* check=production_interactor_lifecycle_persistence; expected=30_saves; result=14_saves_16_cancelled; diagnostic_runtime=Navigation_0.7.7
* check=git_diff_check_master_HEAD; expected=pass; result=pass
* check=report_edit_scope; expected=031_only; result=031_only

UNCERTAINTY:
* missing=[V78-04_regression_assertions,V78-05_regression_assertions,full_graphical_browser_execution]; ambiguity=none_in_reproduced_High_findings
* missing=production_platform_race_frequency; ambiguity=diagnostic_schedule_demonstrates_possible_ordering_not_field_failure_rate

REPETITION OF RESULT:
* entity_id=issue_78_validation_031; stored_in=shared_memory; status=FAIL; critical=0; high=2; medium=2; low=3; next_stage=Planning; next_step=032

COMMUNICATION:
* sender=issue78_validating_cycle2; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=768620ce-159b-4b9d-91ef-b92d24bfb021; protocol=AML-HIP; result=FAIL; restart_stage=Planning

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_validation_031,V78-04,V78-05,V78-07,V78-08,V78-09,V78-10,V78-11,Planning_restart]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
