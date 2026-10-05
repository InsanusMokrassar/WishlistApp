Model: GPT-5.6 Codex
Changed files: features/ui/users/build.gradle, features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt, features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt, features/ui/users/src/jsTest/kotlin/ui/BrowserViewTestFixture.kt, features/ui/users/src/jsTest/kotlin/ui/PasswordChangeViewBrowserTest.kt, features/ui/users/src/jsTest/kotlin/ui/UserEditViewBrowserTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/024-coding.md

T12 now has production-mounting Compose HTML DOM coverage for pending and completed password changes and the approved-owner password-email action. The tests start the actual JS platform plugin in controlled Koin, override only the model and view interactor seams, mount public production `onDraw` with `renderComposable`, use bubbling browser input events and native `requestSubmit`, and wait on animation frames rather than sleeps. The fixture disposes the composition, resets the navigation node, cancels and joins the chain, closes Koin, removes every temporary host, and closes/restores the JSDOM globals at process exit after Compose's document-scoped element cache is no longer needed.

The runner's configured browser target uses Mocha/Node rather than a locally installed graphical browser. A test-only JSDOM dependency supplies the actual HTML DOM for that target. Exact fully qualified DOM browser-test classes are excluded only from `jsNodeTest`; fresh XML proves that the browser task discovers `PasswordChangeViewBrowserTest` with two tests and `UserEditViewBrowserTest` with one test, all successful, while the Node XML contains neither class.

The live password hints are rendered independently so a short mismatched password shows both mismatch and policy feedback. Both web views make the inherited production draw function public, permitting the tests to invoke the real view rather than recreating markup.

## Verification

- `./gradlew :wishlist.features.ui.users:jsBrowserTest --console=plain` — passed after JS plugin start wiring.
- `./gradlew :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:jsBrowserTest --rerun-tasks --console=plain` — passed, exit 0; 236 tasks executed.
- Fresh browser XML — `PasswordChangeViewBrowserTest`: 2 tests, 0 failures, 0 errors; `UserEditViewBrowserTest`: 1 test, 0 failures, 0 errors.
- Fresh Node XML — neither DOM browser-test class is present; regular Node suites report zero failures and zero errors.
- `./gradlew :wishlist.features.ui.users:jvmTest --console=plain` — passed.
- `ast-index rebuild` — passed after source changes.
- `git diff --check` — passed before staging.

```text
ENTITY:
entity_id=issue_78_coding_024; type=coding_result; state=T12_DOM_complete

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_dom; branch=fix/issue-78-email-authorized-password-change; environment=Mocha_Node_with_test_only_jsdom
* constraints=[production_onDraw,real_input_events,native_requestSubmit,no_sleeps,exact_Node_exclusions,full_lifecycle_cleanup]

ACTION:
1. action=add; target=PasswordChangeViewBrowserTest; params={cases=[pending_native_submit,completed_no_password_inputs],plugin=JSPlugin.startPlugin}
2. action=add; target=UserEditViewBrowserTest; params={case=approved_owner_delivery_failure,plugin=JSPlugin.startPlugin}
3. action=add; target=BrowserViewTestFixture; params={dom=jsdom,render_completion=requestAnimationFrame,cleanup=[composition_dispose,node_NEW,chain_cancel_join,Koin_close,host_remove,process_exit_DOM_restore]}
4. action=patch; target=PasswordChangeView; params={feedback=[mismatch,policy],onDraw=public}
5. action=patch; target=UserEditView; params={onDraw=public}
6. action=configure; target=jsNodeTest; params={excluded_fqcn=[PasswordChangeViewBrowserTest,UserEditViewBrowserTest]}

VERIFICATION:
* check=focused_jsBrowserTest; expected=pass; result=pass
* check=unfiltered_jsNode_jsBrowser_rerun; expected=pass; result=pass; executed_tasks=236
* check=jsBrowser_DOM_XML; expected=[PasswordChangeViewBrowserTest:2,UserEditViewBrowserTest:1,failures:0,errors:0]; result=pass
* check=jsNode_DOM_XML; expected=DOM_classes_absent; result=pass
* check=jvmTest; expected=pass; result=pass
* check=ast_index_rebuild; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[]; ambiguity=none
* acceptance_status=T12_complete; blocker=none

REPETITION OF RESULT:
* entity_id=issue_78_coding_024; stored_in=tracked_step_report; status=T12_DOM_complete

COMMUNICATION:
* sender=issue78_coding_dom; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=024-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_024,T12,DOM_browser_tests]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
