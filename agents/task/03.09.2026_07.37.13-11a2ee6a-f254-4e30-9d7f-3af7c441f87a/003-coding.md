Model: GPT-5.6-terra (ML)
Changed files: features/auth/README.md; features/auth/server/src/commonMain/kotlin/RegistrationEmailSender.kt; features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt; features/deeplinks/README.md; features/email/README.md; features/email/server/src/commonMain/kotlin/EmailConfig.kt; features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt; features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt; features/email/server/src/commonTest/kotlin/EmailConfigTest.kt; features/email/server/src/commonTest/kotlin/services/EmailRegistrationInviteSenderTest.kt; features/email/server/src/commonTest/kotlin/services/RegistrationCompensationIntegrationTest.kt; features/email/server/src/commonTest/kotlin/services/SmtpSessionPropertiesTest.kt; server/sample.config.json; agents/task/03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a/003-coding.md

Model rationale: The Coding role prefers an ML model. GPT-5.6-terra is an ML model and was used for the scoped Auth and Email remediation.

# Coding report

Implemented findings 1, 5, and 7 only. SMTP trust now remains certificate-validating by default, the port-587 sample uses STARTTLS without implicit SSL, and required-email registration owns an exact delivered-link rollback handle until finalization succeeds. Feature documentation changes exclude every Operator Notes section.

ENTITY:
entity_id=review_remediation_1_5_7; type=coding_result; state=implemented_and_verified

CONTEXT:

* task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; agent_id=coding; memory_ref=[001-planning.md,002-architecturing.md]; model=GPT-5.6-terra_ML
* constraints=[implemented_findings_1_5_7,preserved_findings_2_6,unimplemented_findings_3_4_8,legacy_RegistrationEmailSender_signature_preserved,Operator_Notes_unchanged]

ACTION:

1. action=add_safe_smtp_option; target=features/email/server/src/commonMain/kotlin/EmailConfig.kt; params={field=unsafeSsl,type=Boolean,default=false,serialized=true}
2. action=extract_smtp_properties; target=features/email/server/src/commonMain/kotlin/services/SmtpEmailService.kt; params={function=buildSmtpSessionProperties,trust_property=conditional_unsafeSsl,default_trust_property=absent}
3. action=correct_sample_submission_mode; target=server/sample.config.json; params={port=587,useTls=true,useSsl=false,unsafeSsl=false,database_path=unchanged}
4. action=add_compensable_sender_boundary; target=features/auth/server/src/commonMain/kotlin/RegistrationEmailSender.kt; params={types=[RegistrationEmailDeliveryHandle,CompensableRegistrationEmailSender],legacy_method=unchanged}
5. action=implement_request_local_link_handle; target=features/email/server/src/commonMain/kotlin/services/EmailRegistrationInviteSender.kt; params={success=exact_DeepLinkId_closure,legacy_bridge=Boolean,ordinary_failure=remove_link,cancellation_cleanup=NonCancellable}
6. action=enroll_reverse_link_rollback; target=features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt; params={branch=runtime_capability,operation=rollableBackOperation,cleanup_context=NonCancellable,cleanup_error=RollbackContext.error.addSuppressed,legacy_fallback=Boolean_sender}
7. action=add_regression_coverage; target=[EmailConfigTest,SmtpSessionPropertiesTest,EmailRegistrationInviteSenderTest,AuthFeatureServiceTest,RegistrationCompensationIntegrationTest]; params={cases=[safe_default,unsafe_opt_in,sample_587,exact_handle,independent_handles,success_retention,finalization_rollback,suppressed_cleanup,cancellation_rollback,legacy_fallback]}
8. action=update_feature_documentation; target=[features/email/README.md,features/auth/README.md,features/deeplinks/README.md]; params={topics=[unsafeSsl,STARTTLS,request_local_ownership,reverse_rollback,crash_limit]}

REASON:

* condition=unconditional_mail.smtp.ssl.trust; requirement=certificate_validation_by_default
* condition=post_delivery_finalization_failure_or_cancellation; requirement=exact_deeplink_removal_before_provisional_account_cleanup
* condition=legacy_sender_implementations; requirement=source_compatible_Boolean_contract

EXPECTED RESULT:

* entity_id=smtp_certificate_policy; new_state=unsafe_opt_in_only; location=EmailConfig_and_buildSmtpSessionProperties
* entity_id=sample_smtp_submission; new_state=STARTTLS_587_without_implicit_SSL; location=server/sample.config.json
* entity_id=verification_link_ownership; new_state=request_local_compensable_handle; location=Auth_Email_boundary

VERIFICATION:

* check=focused_tests; expected=BUILD_SUCCESSFUL; command=./gradlew_:wishlist.features.email.server:allTests_:wishlist.features.auth.server:allTests
* check=affected_module_builds; expected=BUILD_SUCCESSFUL; command=./gradlew_:wishlist.features.email.server:build_:wishlist.features.auth.server:build
* check=ast_index_rebuild; expected=1391_files_114_modules; command=ast-index_rebuild
* check=diff_whitespace; expected=clean; command=git_diff_--check

UNCERTAINTY:

* missing=durable_process_crash_recovery; ambiguity=SMTP_acceptance_to_finalization_process_termination_remains_out_of_scope
* missing=operator_authorization_for_findings_3_4_8; ambiguity=wire_compatibility_SQLite_classification_create_cancellation_remain_unimplemented

REPETITION OF RESULT:

* entity_id=review_remediation_1_5_7; stored_in=shared_memory; status=available_for_verification

COMMUNICATION:

* sender=coding; receiver=verification; task_id=03.09.2026_07.37.13-11a2ee6a-f254-4e30-9d7f-3af7c441f87a; message_id=399fe93d-a407-4190-ad88-9d7fa26c0ce1; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,review_remediation_1_5_7,smtp_certificate_policy,verification_link_ownership]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=coding_handoff
