Model: GPT-5 Codex (ML; assigned continuation coding model)
Changed files: `client/src/commonMain/kotlin/ClientPlugin.kt`, `client/src/jsMain/kotlin/UrlNavigationConfigsRepo.kt`, `client/src/commonTest/kotlin/PasswordChangeInteractorTest.kt`, `client/src/jsTest/kotlin/PasswordChangeNavigationBrowserTest.kt`, and this report.

Coding 027 supersedes Coding 026's restore-lifecycle blocker. The browser fixture now supplies the root
chain and factory, attaches its saver before restoration, starts that supplied root, and awaits actual
root, scaffold, and main stack emissions. It deliberately uses empty navigation nodes: T15 validates
the navigation, persistence, and URL chain without duplicating platform view lifecycle coverage.

The production password-change interactor registers a `Completed` stack observer before enqueueing the
replacement. Once the queued replacement is visible, it snapshots the completed root hierarchy and
saves that snapshot through the injected navigation repository. This avoids a post-transition,
non-replaying StateFlow subscription and gives the interactor a deterministic completion boundary.
The T14 regression provides a recording repository and proves the completed hierarchy is saved without
the approval UUID.

The focused browser test uses a real `WishlistsAppUrlNavigationConfigsRepo` behind a test-owned wrapper;
its completion signal fires only after the real adapter has saved a completed holder. During the full
browser suite this exposed a circular Kotlin/JS object graph in the dependency adapter's
`JSON.stringify(holder)` history state. URL restoration already parses only the canonical path, so the
app adapter now writes the resolved URL with a null browser-history state. It retains the path and title
contract while avoiding serialization of live navigation objects. The test also scopes every root and
saver job under a dedicated supervisor, closes its local Koin application, and restores JSDOM globals.
The email approval fixture uses `emailApproval=approved`.

## Verification

- Focused browser test: `timeout 300s ./gradlew --no-daemon :wishlist.client:jsBrowserTest --tests dev.inmo.wishlist.client.PasswordChangeNavigationBrowserTest --console=plain` — passed.
- T14 JVM test: `timeout 180s ./gradlew --no-daemon :wishlist.client:jvmTest --tests dev.inmo.wishlist.client.PasswordChangeInteractorTest --rerun-tasks --console=plain` — passed.
- Unfiltered JS gates: `timeout 900s ./gradlew --no-daemon :wishlist.client:jsNodeTest :wishlist.client:jsBrowserTest --rerun-tasks --console=plain --info` — passed.
- Fresh XML: browser FQCN `jsBrowserTest.dev.inmo.wishlist.client.PasswordChangeNavigationBrowserTest` contains one test with zero failures and zero errors; Node XML contains no browser FQCN, preserving the expected exclusion.
- `ast-index rebuild` and `git diff --check` — passed.

```text
ENTITY:
entity_id=issue_78_coding_027; type=coding_result; state=verified

CONTEXT:
* task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=issue78_navigation_finish; base_head=ea9728cac0a03fd58bad85f169b3a13b0dbc6744; branch=fix/issue-78-email-authorized-password-change
* constraints=[production_interactor,real_URL_adapter,queued_restore,deterministic_signals,no_fixed_delay,scoped_cleanup]

ACTION:
1. action=restore_fixture; target=PasswordChangeNavigationBrowserTest; params={root_chain=supplied,root_factory=Empty_navigation_nodes,savers=attached_before_restore,awaited_stacks=[root,scaffold,main]}
2. action=persist_completion; target=PasswordChangeViewInteractor; params={observer=pre_replace_Completed_stack_flow,snapshot=root_storeHierarchy,save=NavigationConfigsRepo}
3. action=repair_URL_adapter; target=WishlistsAppUrlNavigationConfigsRepo; params={history_state=null,path=canonical_URL,title=wishlist,reason=live_Kotlin_JS_graph_not_JSON_serializable}
4. action=verify_T14; target=PasswordChangeInteractorTest; params={repo=recording_in_memory,assertions=[Completed_saved,approval_UUID_absent]}
5. action=cleanup; target=browser_fixture; params={scope=SupervisorJob,cancel_join=[saver,root,scope],koin=local_close,JSDOM=restored}

VERIFICATION:
* check=focused_jsBrowserTest; expected=pass; result=pass
* check=T14_jvmTest; expected=pass; result=pass
* check=unfiltered_jsNodeTest_jsBrowserTest_rerun; expected=pass; result=pass
* check=browser_XML_FQCN; expected={tests:1,failures:0,errors:0}; result={tests:1,failures:0,errors:0}
* check=node_XML_browser_FQCN; expected=absent; result=absent
* check=ast_index_rebuild; expected=pass; result=pass
* check=diff_whitespace; expected=pass; result=pass

UNCERTAINTY:
* missing=[]; ambiguity=none

REPETITION OF RESULT:
* entity_id=issue_78_coding_027; stored_in=shared_memory; status=verified

COMMUNICATION:
* sender=issue78_navigation_finish; receiver=root; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=027-coding-result; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,issue_78_coding_027,T14,T15,password_change_navigation]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
