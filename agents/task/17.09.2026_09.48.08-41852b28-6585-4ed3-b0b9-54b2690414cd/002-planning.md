Model: gpt-6-astra
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/002-planning.md

Planning continues with gpt-6-astra as the available HL model, following the HL-before-ML priority in `agents/SHORTCUTS.md`. The latest step, 001-planning.md, was read before this correction. Its investigation, recommendations, acceptance criteria, and verification scope remain unchanged.

The structured handoff in 001 omitted the mandatory bullets and numbered actions. The fully regenerated block below supersedes that block. The original report is preserved under the monotonic step protocol. No source, configuration, tests, prior reports, or feature documentation were changed, and no build was run. Architecture can proceed using the narrative evidence in 001 and the corrected handoff below.

```aml-hip
ENTITY:
entity_id=email_approval_cooldown_plan; type=planning_handoff; state=ready_for_architecture

CONTEXT:
* task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; agent_id=planning; memory_ref=[PROMPT,001,002]; branch=feat/issue-79-user-email-change
* constraints=[report_only,no_push,private_owner_state,SMTP_independent_storage,approved_email_retention,duplicate_protection]; issue=79; pull_request=82
* entity_id=email_approval_cooldown_plan; evidence_report=001-planning.md; superseded_component=001_structured_handoff; preserved_component=001_narrative

ACTION:
1. action=record_investigation; target=email_approval_cooldown_plan; params={layers:[repository,cache,coordinator,verification,configuration,HTTP,private_profile,UI],existing_frequency_limit:false,existing_approval_timestamp:false}
2. action=recommend_lifecycle; target=email_approval_cooldown_plan; params={storage:[pending_email,approval_deadline],configuration:independent_Duration,omitted_default:zero,sample_positive_value:P1D,deadline_policy:future_approvals}
3. action=define_acceptance; target=email_approval_cooldown_plan; params={requirements:[retained_approved_email,pending_candidate_delivery,post_approval_rejection,deadline_boundary,idempotent_approval,private_profile_rendering,issue_79_regression_preservation]}
4. action=require_architecture_resolution; target=email_approval_cooldown_plan; params={decisions:[transactional_cross_slot_uniqueness,wire_representation,typed_cooldown_rejection],operator_questions:zero}
5. action=regenerate_structured_handoff; target=email_approval_cooldown_plan; params={corrected_lists:bullets,corrected_actions:numbered,prior_report:preserved,product_edits:zero}

REASON:
* condition=single_email_column_and_Boolean_approval; requirement=approved_email_retention_and_post_approval_delay; causal_chain=separate_pending_storage+durable_deadline→conditional_promotion_and_rejection→requested_profile_behavior
* condition=no_repository_duration_policy; requirement=explicit_compatibility_recommendation; causal_chain=zero_omission_default+positive_configuration→configured_restriction_without_fabricated_history
* condition=001_structured_handoff_missing_mandatory_list_markers; requirement=AML_HIP_block_shape; causal_chain=regenerate_handoff_in_002→valid_bullets_and_numbered_actions→preserved_monotonic_history

EXPECTED RESULT:
* entity_id=email_approval_cooldown_plan; new_state=architecture_input_available; location=agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/002-planning.md
* entity_id=email_approval_cooldown_plan; implementation_state=pending; non_goals=[rolling_quotas,resend_throttling,email_history,new_admin_controls,new_credentials_policy,animated_countdown]

VERIFICATION:
* check=existing_behavior; expected=no_time_or_count_guard; evidence=001_source_citations; investigation_state=complete
* check=planned_proofs; expected=[exact_boundary,idempotent_approval,cross_slot_uniqueness,migration,reopen,SMTP_graphs,privacy,transport_errors,JVM_JS_Android_UI]; execution_state=pending
* check=correction_scope; expected=002_only; product_edits=zero; prior_report_edits=zero
* check=mandatory_block_shape; expected=all_sections_present_with_required_markers; observed=all_sections_present_with_required_markers

UNCERTAINTY:
* missing=historical_approval_times; ambiguity=none; resolution=legacy_deadline_null
* missing=existing_nonzero_duration_policy; ambiguity=none; resolution=zero_compatibility_default_recommended_and_P1D_sample_explicitly_illustrative
* missing=final_cross_slot_serialization_design; ambiguity=none; resolution=Architecture_required_transactional_invariant

REPETITION OF RESULT:
* entity_id=email_approval_cooldown_plan; stored_in=shared_memory; status=available; next_role=Architecture; narrative_evidence=001-planning.md; corrected_handoff=002-planning.md

COMMUNICATION:
* sender=planning; receiver=Orchestrator_and_Architecture; task_id=17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd; message_id=3e9389ae-d429-4b1a-9691-2aa1cf67e981; protocol=AML-HIP

PERSISTENCE:
* local_memory=true; shared_memory=true; index_keys=[task_id,email_approval_cooldown_plan,issue_79,PR_82]; storage=002-planning.md; auto_memory=false

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
```
