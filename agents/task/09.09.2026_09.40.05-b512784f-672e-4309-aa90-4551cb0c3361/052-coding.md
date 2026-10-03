# 052 Coding

Implemented the bounded V78-05 retained lifecycle evidence and the V78-12 browser cleanup coverage. The common fixtures now hold root FIFO work and Ktor MockEngine responses, preserve immutable persistence snapshots, serialize real ConfigHolder hierarchies, and expose exact request and cancellation-return boundaries. Actual password-change ViewModels are exercised through destruction, cancellation-resistant completion, save failure, completed-page rejection, and safe Continue transitions.

The browser test mounts the production Compose binding and `initNavigation` in JSDOM, waits for the real restored Pending node, submits through the production Ktor transport, verifies the credential-free Completed URL and reload, then continues to UsersList. Binding rebinding proves a disposed older composition cannot prevent a newer root transition; all composition scopes and ViewModel jobs are joined or cancelled during cleanup.

## AML-HIP handoff

ENTITY:
entity_id=issue78_v78_05_v78_12_coding; type=client_lifecycle_transport_browser_evidence; state=implemented

CONTEXT:
task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; agent_id=codex; memory_ref=[agents/task/09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361/049-architecture.md#V78-05,V78-12]; constraints=[client_scope,no_production_behavior_change,no_sleep,no_skip]

ACTION:
1. action=expand_fixture; target=PasswordChangeNavigationTestFixtures; params={root_fifo_timer=true,immutable_config_holder_snapshots=true,production_ktor_mock_engine=true,request_received=true,cancellation_return=true}
2. action=expand_common_lifecycle_tests; target=PasswordChangeInteractorTest; params={actual_viewmodel_jobs=true,destruction_before_save=true,ensure_active_cancellation=true,save_failure_no_replay=true,continue_users_list_save=true}
3. action=replace_browser_mount; target=PasswordChangeNavigationBrowserTest; params={compose_html_jsdom=true,production_binding=true,production_init_navigation=true,typed_pending_gate=true,typed_completed_reload_gate=true,rebind_cleanup=true}
4. action=add_test_dependency; target=client/build.gradle; params={commonTest_ktor_client_mock=true}

REASON:
condition=V78-05_retained_lifecycle_evidence_and_V78-12_browser_cleanup_required; requirement=actual_navigation_transport_persistence_lifecycle_proof

EXPECTED RESULT:
entity_id=issue78_v78_05_v78_12_coding; new_state=verified; location=[client/commonTest,client/jsTest,client/build.gradle]

VERIFICATION:
check=jvmTest_PasswordChangeInteractorTest; expected={tests=13,failures=0,errors=0,skips=0}
check=jsBrowserTest_unfiltered; expected={tests=20,failures=0,errors=0,skips=0}
check=browser_assertions; expected={requests=1,completed_save=1,users_list_save=1,completed_url=/ui/password-changed,credential_url_state_absence=true,reload_completed=true}

UNCERTAINTY:
missing=physical_browser_device_execution; ambiguity=none_detected_for_jsdom_compose_transport_navigation_execution

REPETITION OF RESULT:
entity_id=issue78_v78_05_v78_12_coding; stored_in=repository_step_052_and_client_tests; status=available

COMMUNICATION:
sender=codex; receiver=issue78_parent_agent; task_id=09.09.2026_09.40.05-b512784f-672e-4309-aa90-4551cb0c3361; message_id=c9e090da-b143-4ea9-9dd2-087e4b8e8e7d; protocol=AML-HIP

PERSISTENCE:
local_memory=true; shared_memory=true; index_keys=[task_id,issue78_v78_05_v78_12_coding,password_change_lifecycle]

VALIDATION:
format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=true
