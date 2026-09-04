Model: OpenAI GPT-5.6 Terra (ML)
Changed files: features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt; features/common/client/src/jsTest/kotlin/ui/components/ToasterTest.kt; features/common/README.md; agents/task/03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b/007-coding.md

# Coding Report

## Model rationale

The Coding role prefers an ML model. OpenAI GPT-5.6 Terra was used for the revised implementation because the change combines a Compose-annotated public payload, `MutableSharedFlow` replay and overflow behavior, cancellation/remount semantics, and virtual-time coroutine coverage.

## Result

The superseded state-based toaster now uses public `ToastNotification(message, timeout)` payloads and an internal replay-one, zero-extra-capacity `MutableSharedFlow` queue with `DROP_OLDEST`. Negative millisecond timeouts are rejected. The existing string publisher remains source-compatible, creates a default-2600 ms notification, and continues to support the email-startup function reference.

The single scaffold host owns local current-notification state and sequentially consumes the queue. Each dequeued notification clears replay before display, delay, or any other suspension, preventing acknowledged notifications from replaying after host remount. The Common README documents the bounded, lossy queue and host lifecycle without altering Operator Notes.

`ToasterTest` now uses coroutine virtual time to cover public overloads, timeout validation, per-notification timing, pre-host replay delivery, acknowledgement/remount behavior, pending notification survival, and oldest-pending overflow dropping.

## Verification

- `./gradlew --quiet :wishlist.features.common.client:jsNodeTest` — passed (exit 0).
- `./gradlew --quiet :wishlist.client:compileKotlinJs` — passed (exit 0).
- `./gradlew --quiet :wishlist.client:jsNodeTest` — passed (exit 0).
- `ast-index rebuild` — passed; 1,390 files indexed.
- `ast-index refs Toaster` — six unchanged production publishers; string and notification overload test references compile.
- `ast-index refs ToastHost` — one unchanged scaffold mount.
- `git diff --check` — passed.

The first focused test attempt exposed only nullable expected-list type inference. The second attempt identified equal test fixtures in the overflow assertion; distinct timeout fixtures corrected that false-positive assertion. The final focused suite passed with no SharedFlow behavior mismatch.

## Constraints

The implementation follows architecture step 006 rather than the earlier fifteen-extra-slot planning proposal: queue configuration is exactly replay `1`, extra buffer capacity `0`, and `DROP_OLDEST`. `PROMPT.md` remains untracked and is excluded from the coding commit. No caller, style, dependency, or build configuration change was made.

## Architecture handoff

```text
ENTITY:
entity_id=toast_notification_queue_architecture; type=bounded_shared_flow_ui_queue; state=implemented_and_verified

CONTEXT:
* task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; agent_id=coding; memory_ref=[005-planning.md,006-architecturing.md,Toaster.kt,ToasterTest.kt,features/common/README.md]
* constraints=[payload=ToastNotification,timeout_unit=milliseconds,default_timeout=2600,public_overloads=[show(String),show(ToastNotification)],replay=1,extra_buffer_capacity=0,overflow=DROP_OLDEST,subscriber_count=1]

ACTION:
1. action=define_payload; target=ToastNotification; params={fields=[message:@Composable_()_to_String,timeout:Long],default_timeout=2600,negative_timeout=IllegalArgumentException,zero_timeout=immediate_hide}
2. action=configure_queue; target=ToastQueue.notifications; params={type=MutableSharedFlow<ToastNotification>,replay=1,extra_buffer_capacity=0,on_buffer_overflow=DROP_OLDEST,active_pending_capacity=1}
3. action=preserve_publishers; target=Toaster; params={string_signature=show(String):Unit,notification_signature=show(ToastNotification):Unit,string_mapping=ToastNotification(message={text}),emission=tryEmit}
4. action=consume_sequentially; target=ToastQueue.consume_and_ToastHost; params={collector=single_LaunchedEffect_Unit,transition_order=[show,delay,hide],delay_source=ToastNotification.timeout,current_state=host_local}
5. action=acknowledge_replay; target=ToastQueue.consume; params={operation=resetReplayCache,position=after_dequeue_before_suspension,opt_in=ExperimentalCoroutinesApi,new_subscriber_effect=no_acknowledged_replay}
6. action=replace_tests; target=ToasterTest; params={cases=[public_overloads,timeout_contract,FIFO_retained_order,per_item_timeout,pre_subscriber_replay,no_stale_remount,pending_survives_remount,drop_oldest_overflow],clock=kotlinx_coroutines_test_virtual_time}
7. action=update_feature_documentation; target=features/common/README.md; params={section=Architecture_Notes,operator_notes_changed=false,queue_lifecycle_documented=true}

REASON:
* condition=approval_event_precedes_host_subscription; requirement=replay_one_retains_latest_pre_host_event; condition→action→result=pre_host_emit→replay_cache_store→first_host_delivery
* condition=non_suspending_Unit_publisher_plus_slow_consumer; requirement=bounded_overflow_policy_without_arbitrary_depth; condition→action→result=third_active_burst_value→drop_oldest_pending→newest_pending_retained
* condition=host_remount_after_current_dequeue; requirement=acknowledged_current_value_not_replayed; condition→action→result=dequeue→reset_replay_before_suspend→remount_excludes_current

EXPECTED RESULT:
* entity_id=ToastNotification; new_state=public_composable_message_plus_millisecond_timeout_payload; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toast_notification_queue_architecture; new_state=single_consumer_bounded_lossy_FIFO_with_pre_host_latest_replay; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toaster_queue_tests; new_state=virtual_time_lifecycle_order_timeout_and_overflow_coverage; location=features/common/client/src/jsTest/kotlin/ui/components/ToasterTest.kt
* entity_id=common_toaster_documentation; new_state=queue_retention_and_remount_semantics_documented; location=features/common/README.md

VERIFICATION:
* check=common_client_js_node_tests; expected=all_queue_timeout_compatibility_replay_remount_overflow_cases_pass; actual=exit_code_0
* check=web_client_js_compile; expected=existing_String_publishers_and_composable_host_compile_without_caller_edits; actual=exit_code_0
* check=broader_web_client_js_node_tests; expected=client_test_suite_passes; actual=exit_code_0
* check=post_change_ast_index; expected={Toaster_production_publishers=6,ToastHost_mounts=1}; actual={Toaster_production_publishers=6,ToastHost_mounts=1}
* check=git_diff_check; expected=no_whitespace_errors; actual=no_whitespace_errors

UNCERTAINTY:
* missing=[]; ambiguity=none; queue_guarantee=retained_values_only; lossless_delivery=false

REPETITION OF RESULT:
* entity_id=toast_notification_queue_architecture; stored_in=shared_step_file; status=implemented_and_verified

COMMUNICATION:
* sender=coding; receiver=verification; task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; message_id=891fa555-cea8-4e01-963c-6f6f880cbdf9; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
