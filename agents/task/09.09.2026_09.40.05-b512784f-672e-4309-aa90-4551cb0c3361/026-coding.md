Model: GPT-5.6 Codex
Changed files: client/build.gradle, client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt, client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt, agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/026-coding.md

T14 is implemented and passes on JVM. It resolves the real `PasswordChangeViewInteractor` from `ClientPlugin`, starts a real navigation chain, proves pending-to-completed replacement and continuation to a users-list destination, and round-trips the pending config through production Json registration.

T15 adds a test-only JSDOM browser target and an exact Node exclusion for the browser class. Its focused adapter test passes before the live-chain extension: it covers a controlled base URL, canonical pending restore/save/reload, scaffold and chain IDs, credential-free completed reload, malformed rejection, wishlist/item reconstruction, and the email-approved marker consumer.

The live-chain extension remains blocked by the navigation restore fixture: `restoreHierarchy(restored, factory)` returns a `NavigationChain` whose `stackFlow.value` is empty immediately after restoration and `start`, while the restored holder is the expected root wrapper containing `EmptyConfig` then the scaffold. Attempting to traverse the documented wrapper shape therefore throws `NoSuchElementException` before interactor invocation. No production source was changed to hide that discrepancy. The fresh focused XML records the precise failure at `PasswordChangeNavigationBrowserTest.kt:102` with `stackFlow.value.single()` on an empty root stack.

## Verification

- `./gradlew :wishlist.client:jvmTest --tests '*PasswordChangeInteractorTest' --console=plain` — passed.
- `./gradlew :wishlist.client:jsBrowserTest --tests '*PasswordChangeNavigationBrowserTest*' --console=plain` — adapter portion passed before the live-chain addition; current focused run fails at the diagnostic restored-root-stack assertion described above.

```text
ENTITY:
entity_id=issue_78_coding_026; type=coding_result; state=T14_complete_T15_live_restore_blocked

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_coding_dom; branch=fix/issue-78-email-authorized-password-change; environment=Mocha_Node_with_test_only_jsdom

ACTION:
1. action=add; target=PasswordChangeInteractorTest; params={DI=ClientPlugin,chain=real,pending_to_completed=true,continue_to_users_list=true}
2. action=add; target=PasswordChangeNavigationBrowserTest; params={adapter=WishlistsAppUrlNavigationConfigsRepo,base=controlled,history=replaceState_pushState,marker=email_approved}
3. action=diagnose; target=restoreHierarchy_live_chain; params={holder=root_EmptyConfig_scaffold,observed_root_stack=[]}

VERIFICATION:
* check=T14_JVM; expected=pass; result=pass
* check=T15_adapter_browser; expected=pass; result=pass_before_live_extension
* check=T15_live_restore; expected=restored_root_stack_nonempty; result=fail; diagnostic=NoSuchElementException_at_PasswordChangeNavigationBrowserTest.kt_102

UNCERTAINTY:
* missing=[live_interactor_persistence_assertion,unfiltered_client_JS_gate]; ambiguity=restoreHierarchy_returned_chain_ownership
* acceptance_status=T15_blocked; blocker=concrete_navigation_restore_fixture_shape

REPETITION OF RESULT:
* entity_id=issue_78_coding_026; stored_in=tracked_step_report; status=T14_complete_T15_live_restore_blocked

COMMUNICATION:
* sender=issue78_coding_dom; receiver=orchestrator; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=026-coding-blocker; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,issue_78_coding_026,T14,T15,restoreHierarchy]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
