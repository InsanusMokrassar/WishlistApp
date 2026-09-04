Model: OpenAI GPT-5 (high-level)
Changed files: agents/task/03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b/005-planning.md

# Revised Planning Report

## Model rationale

The Planning role prefers a high-level model. The revised requirement combines a public payload API, hot-flow buffering, Compose lifecycle behavior, queue ordering, and deterministic coroutine tests, so OpenAI GPT-5 was used to define the lifecycle and compatibility boundaries.

## Task understanding

The follow-up requirement supersedes the single-value state design where the requirements conflict. Introduce a public `ToastNotification` data class with `message: @Composable () -> String` and `timeout: Long = 2600L`, where timeout units are milliseconds and negative values are rejected. Add `show(notification: ToastNotification)` so callers can enqueue a composable message with a custom timeout. Preserve `show(text: String)` exactly as a `Unit`-returning source-compatible entry point; the string overload creates the notification with `message = { text }` and the default timeout.

Replace the singleton state with a bounded `MutableSharedFlow`. Use one replay slot plus fifteen extra buffered slots and `BufferOverflow.DROP_OLDEST`. During an active host subscription, notifications are processed in emission order and at most sixteen pending notifications are retained; overflow deterministically discards the oldest pending notification and keeps newer feedback. With no subscriber, SharedFlow retains only the newest replay value because extra buffer capacity is inactive. The approval notification is the sole startup notification today, so the replay slot preserves the `ClientJSPlugin.startPlugin` emission that occurs before `renderComposable` creates `ScaffoldView` and mounts `ToastHost`.

`ToastHost` must own only the currently rendered notification and run one sequential collection coroutine. On dequeue, the queue clears the replay cache before any suspension, preventing an already accepted notification from appearing again after host remount. The host then renders the notification's composable message, delays for the notification timeout, hides the toast, and proceeds to the next buffered value. Host cancellation discards the currently displayed notification; an already dequeued notification is never replayed. While no host exists, only the newest emission is retained by the documented replay-one policy.

The existing state-oriented `message` property and `clear()` function have no production consumers beyond `ToastHost`; the queue collector replaces both responsibilities. An internal queue abstraction in `Toaster.kt` should encapsulate the SharedFlow, replay acknowledgement, and injectable delay seam, while the public `Toaster` object keeps only the two `show` entry points. The internal abstraction also allows isolated tests without singleton state leaking between test cases.

## Investigation

The Common feature README contains no operator-specific constraints. `ast-index` confirms five direct string calls, the `Toaster::show` function reference used by email-approval startup, one `ToastHost` mount in the scaffold, and one current JS test file. `ClientJSPlugin.startPlugin` calls `consumeEmailApprovalNotification` before `ClientPlugin.startPlugin` and before `renderComposable`, so a zero-replay SharedFlow would lose the approval toast. The current 2600 ms delay and rendering styles remain the behavior baseline.

## Verification design

Replace the state-provider test with focused coroutine tests around an isolated queue instance. An injected suspending delay function or `kotlinx-coroutines-test` virtual time must make timing assertions independent of wall-clock time. Tests must prove ordered display and hide transitions, exact use of each custom timeout, pre-subscriber replay delivery, absence of stale replay after cancellation and resubscription, deterministic drop-oldest overflow, and compatibility of `(String) -> Unit = Toaster::show` with the 2600 ms default payload.

Run the Common client JS Node tests and compile the full JS client so the direct callers and `Toaster::show` function reference are checked. Rebuild `ast-index` after source changes as required by repository policy.

## Open questions

No unclear architecture decisions, requirements, or constraints remain. The bounded capacity, drop policy, no-subscriber behavior, remount behavior, timeout unit, invalid-timeout behavior, and public enqueue surface are explicitly defined, so no operator questions are required.

## Final plan and architecture handoff

```text
ENTITY:
entity_id=toast_notification_queue; type=bounded_MutableSharedFlow_queue; state=planned

CONTEXT:
* task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; agent_id=planning; memory_ref=[PROMPT.md,004-coding.md,Toaster.kt,ToasterTest.kt,ClientJSPlugin.kt,ScaffoldView.kt,features/common/README.md]
* constraints=[payload_type=ToastNotification,message_type=@Composable_()_to_String,timeout_type=Long_milliseconds,default_timeout=2600,replay=1,extra_buffer_capacity=15,total_pending_capacity=16,overflow=DROP_OLDEST]; compatibility=[show_String_signature_preserved,approval_pre_mount_delivery_preserved]

ACTION:
1. action=define_payload; target=ToastNotification; params={fields=[message:@Composable_()_to_String,timeout:Long],default_timeout_ms=2600,negative_timeout=reject}
2. action=replace_state_storage; target=toast_notification_queue; params={storage=MutableSharedFlow<ToastNotification>,replay=1,extra_buffer_capacity=15,on_buffer_overflow=DROP_OLDEST}
3. action=define_enqueue_surfaces; target=Toaster; params={custom_api=show(ToastNotification):Unit,compatibility_api=show(String):Unit,string_wrapper=ToastNotification(message={ text })}
4. action=consume_sequentially; target=ToastHost; params={collector_count=1,current_state=host_local,acknowledge=resetReplayCache_before_suspension,display_order=FIFO,delay_source=ToastNotification.timeout,hide_between_messages=true}
5. action=preserve_startup_notification; target=ClientJSPlugin_email_approval_toast; params={emission_phase=before_renderComposable,no_subscriber_retention=newest_one,replay_delivery_count=1,ClientJSPlugin_changes=none}
6. action=avoid_stale_remount_replay; target=toast_notification_queue; params={accepted_value_replay_reset=immediate,host_cancellation_policy=current_value_discarded,new_host_behavior=receive_only_unacknowledged_replay}
7. action=add_deterministic_tests; target=ToasterTest; params={cases=[FIFO_order,per_item_timeout,pre_subscriber_delivery,no_stale_remount_replay,String_function_reference_compatibility,default_timeout,custom_payload,drop_oldest_overflow],wall_clock_waits=false}
8. action=verify_implementation; target=wishlist_JS_client; params={test_command=./gradlew_:wishlist.features.common.client:jsNodeTest,compile_command=./gradlew_:wishlist.client:compileKotlinJs,ast_index_rebuild=true}

REASON:
* condition=approval_notification_emitted_before_ToastHost_subscription → action=configure_replay_one → result=approval_notification_delivered_after_mount; requirement=consumed_notification_not_replayed_after_remount
* condition=toast_bursts_require_ordered_processing → action=collect_sequentially_with_bounded_buffer → result=FIFO_display_with_per_notification_timeout; requirement=overflow_policy_explicit_and_deterministic

EXPECTED RESULT:
* entity_id=ToastNotification; new_state=public_composable_payload_with_custom_timeout; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=toast_notification_queue; new_state=bounded_replay_aware_SharedFlow_with_sequential_consumer; location=features/common/client/src/jsMain/kotlin/ui/components/Toaster.kt
* entity_id=ToasterTest; new_state=deterministic_queue_lifecycle_and_compatibility_coverage; location=features/common/client/src/jsTest/kotlin/ui/components/ToasterTest.kt

VERIFICATION:
* check=notification_order_and_timeout_tests; expected=FIFO_transitions_and_exact_configured_delays
* check=pre_subscriber_and_remount_tests; expected=early_approval_shape_delivered_once_and_acknowledged_value_not_replayed
* check=buffer_overflow_test; expected=oldest_pending_value_dropped_and_newest_values_retained_at_capacity_16
* check=compatibility_compile_and_test; expected=Toaster_show_String_function_reference_valid_and_default_timeout_2600_ms

UNCERTAINTY:
* missing=none; ambiguity=none; operator_questions=none

REPETITION OF RESULT:
* entity_id=toast_notification_queue; stored_in=shared_memory; status=available

COMMUNICATION:
* sender=planning; receiver=architecture; task_id=03.09.2026_05.03.15-85c7f771-7731-48e8-a20a-7b6a6cfb836b; message_id=35dd58e3-be1d-4f8d-b193-7abd7a20e27a; protocol=AML-HIP

PERSISTENCE:
* local_memory=false; shared_memory=true; index_keys=[task_id,entity_id,intent]

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
