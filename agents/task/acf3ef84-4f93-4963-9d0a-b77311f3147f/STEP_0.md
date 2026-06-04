ENTITY:
entity_id=task_issue_24; type=feature_change; state=planned

CONTEXT:
task_id=issue_24; agent_id=planning; protocol=AML-HIP
goal=add_integer_field_amount_to_wishlist_item; default=1; show_in_card_only_if!=1; edit_form_min=1
constraints=[backward_compatible_default_1, mirror_existing_field_flow, js_client_bootstrap_controls, no_branch_switch, no_commit]

TRACE_EXISTING_FIELD: reference_field=priority(Priority) end-to-end:
- model: features/wishlist/common/src/commonMain/kotlin/models/WishlistItem.kt; sealed interface WishlistItem + NewWishlistItem + RegisteredWishlistItem; priority:Priority=Priority.Medium default
- db: features/wishlist/common/src/jvmMain/kotlin/repo/ExposedWishlistItemRepo.kt; priorityWeightColumn=long("priority_weight").default(...); read via asObject; write via update()+InsertStatement.asObject
- serializer: @Serializable data class w/ default value → backward-compat for absent JSON field; DB .default() → backward-compat for absent column
- api server: features/wishlist/server/.../WishlistItemRoutingsConfigurator.kt receive<NewWishlistItem>() — whole DTO; NO per-field code
- api client: features/wishlist/client/.../KtorWishlistItemFeature.kt — sends/receives whole DTO; NO per-field code
- ui viewmodel: features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt; per-field _state + onXChanged + load-in-init + build-on-onSave
- ui edit views: features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemEditView.kt
- ui card views: features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemCard.kt
- ui detail views: features/ui/wishlist/src/{jsMain,jvmMain,androidMain}/kotlin/ui/WishlistItemView.kt (optional show)
- strings: features/ui/wishlist/src/commonMain/kotlin/WishlistStrings.kt

PLAN_STEPS:
1. model: add amount:Int=1 to WishlistItem interface + NewWishlistItem + RegisteredWishlistItem (mirror priority default mechanism)
2. db: add amountColumn=integer("amount").default(1) in ExposedWishlistItemRepo; map in asObject(read), update(write), InsertStatement.asObject(insert)
3. serializer: @Serializable default=1 (automatic w/ data class default) → absent JSON field deserializes to 1
4. api: NONE (DTO passthrough) — verify only
5. ui viewmodel: add _amountState(String, "1") + amountState + onAmountChanged(min1 clamp) + load in init + include in NewWishlistItem on onSave
6. ui edit views js/jvm/android: add amount input (js=Bootstrap form-control number min=1)
7. ui card views js/jvm/android: show amount only when item.amount!=1 (e.g. "x{amount}")
8. ui detail views js/jvm/android: show amount when !=1 (consistency)
9. strings: add amountLabel (+Russian)
10. README updates (haiku-domain; orchestrator updates per CODING.md): features/wishlist/README.md model+db; features/ui/wishlist/README.md edit/card

PROBLEMS_FOUND: none_blocking
- amount type=Int (issue says integer); min=1 enforced in viewmodel onAmountChanged + js input min attr
- card display format: prefix "x" before number ("×2") chosen; localized label not strictly needed for count badge but add amountLabel for edit form

EXPECTED_RESULT:
entity_id=task_issue_24; new_state=architecture_pending; location=agents/task/acf3ef84-4f93-4963-9d0a-b77311f3147f/STEP_1.md

VERIFICATION:
check=field_present_all_layers; expected=true
check=default_1_serializer_and_db; expected=true
check=card_hides_amount_eq_1; expected=true

REPETITION_OF_RESULT:
entity_id=task_issue_24; stored_in=STEP_0.md; status=available
