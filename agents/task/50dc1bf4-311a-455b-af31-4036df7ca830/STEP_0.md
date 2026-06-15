# STEP_0 — Planning

ENTITY:
entity_id=task-50; type=github_issue; state=planned

CONTEXT:
* task_id=50dc1bf4-311a-455b-af31-4036df7ca830; agent_id=root; role=ORCHESTRATOR→PLAN
* issue=#50; title="Make amount input field to be a numeric field"
* branch=issue/50-numeric-amount-field
* constraints=[AML-HIP_reports, ast-index_only_search, no_commit_push_PR, respect_operator_notes, no_else_if, KDoc_on_new_kt, JS_bootstrap_classes]

TARGET_LOCATION:
* feature=features/ui/wishlist; screen=WishlistItemEditView (Item Edit, create/edit)
* field=amount (desired quantity, UInt>=1, integer-only)
* operator_notes_state=EMPTY → no constraint violation possible

CURRENT_STATE (read via ast-index + Read):
1. file=features/ui/wishlist/src/jsMain/kotlin/ui/WishlistItemEditView.kt:157-166
   widget=Input(InputType.Text); attrs=[inputmode=numeric, min=1, placeholder=1]; handler=viewModel.onAmountChanged
   gap=type=Text (not Number) → no spinner, no browser-level numeric enforcement; relies only on inputmode hint
2. file=features/ui/wishlist/src/jvmMain/kotlin/ui/WishlistItemEditView.kt:145-152
   widget=OutlinedTextField(material v2); keyboardOptions=ABSENT → default text keyboard
3. file=features/ui/wishlist/src/androidMain/kotlin/ui/WishlistItemEditView.kt:150-157
   widget=OutlinedTextField(material3); keyboardOptions=ABSENT → default text keyboard (alpha keyboard on device)
4. file=features/ui/wishlist/src/commonMain/kotlin/ui/WishlistItemEditViewModel.kt:188-195
   fun onAmountChanged(v): already rejects non-numeric (toUIntOrNull) by keeping prev value; clamps >=1; empty allowed transient
   note=ViewModel-level numeric rejection ALREADY EXISTS; issue is the INPUT WIDGET TYPE/KEYBOARD per platform

REASON:
* condition=issue requests "numeric field (numeric keyboard/type, reject non-numeric input)"
* requirement=widget itself must be numeric per platform: JS InputType.Number; JVM/Android KeyboardOptions(keyboardType=Number)
* requirement=non-numeric rejection already enforced in onAmountChanged (commonMain); keep as defense-in-depth

PLANNED_CHANGES (3 view files, no VM change needed):
1. action=edit; target=jsMain/WishlistItemEditView.kt; params={Input(InputType.Text)→Input(InputType.Number); keep id/classes/value/min/placeholder/onInput/disabled; drop redundant inputmode=numeric (Number type implies numeric keyboard)}
2. action=edit; target=jvmMain/WishlistItemEditView.kt; params={amount OutlinedTextField: add keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number); add imports androidx.compose.foundation.text.KeyboardOptions, androidx.compose.ui.text.input.KeyboardType}
3. action=edit; target=androidMain/WishlistItemEditView.kt; params={amount OutlinedTextField: add keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number); add same 2 imports}

NON_CHANGES (rationale):
* WishlistItemEditViewModel.onAmountChanged: UNCHANGED — already rejects non-numeric, clamps>=1
* price field: OUT OF SCOPE — issue=amount only; price is decimal Amount(Double), different semantics
* priority custom-weight field: OUT OF SCOPE — issue=amount only
* adminPanel price: OUT OF SCOPE — "price" not "amount"

VERIFICATION_PLAN:
* check=compile; cmd=./gradlew :wishlist.features.ui.wishlist:build; expected=BUILD_SUCCESSFUL
* check=ast-index_rebuild_after_source_change
* check=README Architecture Notes updated (issue #50 entry) by haiku-agent or self+flag

UNCERTAINTY:
* missing=none; ambiguity=none; operator_notes=empty → no stop condition triggered

REPETITION_OF_RESULT:
* entity_id=task-50; plan=3_view_edits + README_note; vm_unchanged; stored_in=STEP_0.md; status=available
