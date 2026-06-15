# STEP_1 — Architecturing

ENTITY:
entity_id=task-50; type=github_issue; state=architected

CONTEXT:
* task_id=50dc1bf4-311a-455b-af31-4036df7ca830; agent_id=root; role=ARCHITECTURE
* input=STEP_0.md (plan); constraints=[no_else_if, JS_bootstrap_classes, KDoc_on_new_kt, per-platform_View_isolation]

ARCH_DECISION_1: layer=View_only (no VM/Model/common-logic change)
* reason=numeric REJECTION already lives in commonMain WishlistItemEditViewModel.onAmountChanged (toUIntOrNull-gated, clamp>=1)
* reason=issue residual = widget TYPE/KEYBOARD per platform → pure presentation concern → View layer
* relation: View(numeric_widget) → onAmountChanged(numeric_guard) → amountState(String) → onSave(toUIntOrNull.coerceAtLeast(1u))
* invariant=amountState stays String (MVVM contract unchanged); no new StateFlow; no new public VM API

ARCH_DECISION_2: per-platform numeric widget mapping (matches existing Auth-UI per-platform widget pattern in CODING.md)
* JS: InputType.Text → InputType.Number
  - InputType.Number (org.jetbrains.compose.web.attributes) renders <input type=number>: numeric soft-keyboard on mobile, spinner, browser rejects non-numeric chars
  - keep attr min=1 (semantic floor); keep placeholder=1; keep classes form-control (Bootstrap rule honored)
  - drop attr inputmode=numeric: redundant under type=number (type=number implies numeric inputmode); avoids double-signal
* JVM (material v2 OutlinedTextField): add keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)
* Android (material3 OutlinedTextField): add keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number)
  - KeyboardType.Number → device shows numeric keypad; non-numeric still defensively dropped by onAmountChanged

ARCH_DECISION_3: import additions (JVM+Android identical)
* import androidx.compose.foundation.text.KeyboardOptions
* import androidx.compose.ui.text.input.KeyboardType
* check=imports valid in both material-v2(jvm) and material3(android) source sets (foundation.text + ui.text are material-agnostic)

ARCH_NON_GOALS:
* no change to price field (decimal, different type), priority weight field, adminPanel price
* no expect/actual util needed (no platform-default value logic; widget attrs are inline per View)
* no new WishlistStrings key (amountLabel already exists)
* no else_if introduced (edits are attribute/parameter additions only)

KDoc_IMPACT:
* no NEW .kt files created → KDoc-new-file rule not triggered
* edited files: amount widget has no own KDoc block (inline in onDraw); class/method KDocs unaffected → no KDoc edit required

README_IMPACT (ARCHITECTURE.md rule: update Architecture Notes after change):
* file=features/ui/wishlist/README.md; section=## Architecture Notes; action=append issue-#50 bullet
* content=amount input now native-numeric per platform (JS InputType.Number; JVM/Android KeyboardType.Number); VM guard unchanged
* must_not_touch=## Operator Notes (empty, human-owned)
* doc-fill agent=haiku per ALL.md rule#4; if subagent unavailable → self-write + flag in STEP_2

VERIFICATION (post-coding):
* check=./gradlew :wishlist.features.ui.wishlist:build → BUILD_SUCCESSFUL
* check=ast-index rebuild
* fallback=single fix cycle only per CODING.md

REPETITION_OF_RESULT:
* entity_id=task-50; arch=View-layer-only, 3 files, 2 imports x2 platforms, README-note; stored_in=STEP_1.md; status=available
