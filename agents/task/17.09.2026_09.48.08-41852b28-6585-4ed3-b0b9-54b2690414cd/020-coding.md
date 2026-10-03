Model: gpt-5.6-terra (ML; Coding prioritizes ML before HL in agents/SHORTCUTS.md.)
Changed files:
- features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt
- features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/020-coding.md

## VEC-05 UI privacy and feedback proof

The shared ViewModel suite now exercises owner-private email state with both a pending replacement and an active change deadline while identity changes, logout, a live target retarget, and root access to another user remove ownership. Each boundary proves that the profile metadata, draft, cooldown, completed feedback, error state, and enabled actions are cleared and that no stale private publication, private read, PUT, POST, or later action occurs. Existing non-cooperative continuation coverage remains the node-destruction oracle and proves a destroyed node cannot publish or issue a later POST.

The same suite adds explicit mappings for false, 409, network, and malformed-429 mutation outcomes: all remain SaveFailed, retain the valid draft, create no cooldown, and make no verification POST. A typed cooldown rejection followed by a failed private reconciliation retains the authoritative deadline and byte-identical valid draft, exposes the separate LoadFailed state, performs exactly one reconciliation GET, and never reports Saved or sends verification. The exact expiry refresh then retires the restriction. Pending-only and deadline-only authoritative snapshot changes independently retire Sent and other completed feedback; the pre-existing negative-failure reconciliation tests continue to guard failure preservation.

Production JVM Compose renderer tests cover retained approved current address A, pending address B, and draft C; active UTC deadline text with disabled input and Save but usable Refresh; the absence of resend for approved or no-pending profiles; exact-equality expiry; and a captured real IME callback that sees a newly active restriction before it can issue PUT or POST. Root-other and live-retarget rendering now seed pending and deadline metadata and prove that the private widgets and semantics disappear before the old ViewModel collector drains.

No production defect was exposed by these regressions, so no production code was changed. The scope remains the VEC-05 UI/privacy/feedback proof only; server, configuration, shared model/cache work and VEC-06 remain outside this step.

## Verification

`AST_INDEX_DB_PATH=/tmp/wishlist-vec05-ast.db ast-index rebuild --include features/ui/users` completed successfully: 43 files indexed, zero failures.

`./gradlew --no-parallel :wishlist.features.ui.users:jvmTest :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:testDebugUnitTest :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileDebugKotlinAndroid` completed successfully. The final XML results were JVM 98 tests with zero failures and zero errors, JS Node 82 tests with zero failures and zero errors, and Android debug unit 82 tests with zero failures and zero errors. The JVM total includes 15 production `UserEditEmailRenderTest` cases.

`git diff --check` completed successfully.
