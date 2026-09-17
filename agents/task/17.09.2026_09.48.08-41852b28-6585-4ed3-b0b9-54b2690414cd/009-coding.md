Model: gpt-5.6-terra (ML; Coding priority selects ML before HL).
Changed files:
- features/ui/users/src/androidMain/kotlin/ui/UserEditView.kt
- features/ui/users/src/commonMain/kotlin/UsersListStrings.kt
- features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt
- features/ui/users/src/commonMain/kotlin/utils/EmailChangeDeadlineText.kt
- features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt
- features/ui/users/src/commonTest/kotlin/utils/EmailChangeDeadlineTextTest.kt
- features/ui/users/src/jsMain/kotlin/ui/UserEditView.kt
- features/ui/users/src/jvmMain/kotlin/ui/UserEditView.kt
- features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/009-coding.md

# Owner profile UI coding report

The owner-email editor now retains the approved current address separately from a pending replacement and the editable draft. A clean draft initializes from the pending candidate when present; otherwise it initializes from the current address. Save treats either persisted slot as already stored, confirms a newly submitted address against either persisted slot after reconciliation, and only asks SMTP to verify the actual persisted candidate. Resend likewise uses the pending candidate or an unapproved first address, never a retained approved address or an unsaved draft.

The ViewModel accepts an injectable epoch-millisecond Klock clock. An active persisted deadline disables mutation and Save, including synchronous admission through a captured IME callback. A refresh re-evaluates the clock even for an equal profile object, so the exact deadline re-enables editing without a background timer. A well-formed `EmailChangeCooldownException` is handled as authoritative feedback, retains the raw draft, reconciles once, and never starts verification; a failed reconciliation preserves both the cooldown and the independently reported load failure. Feedback snapshots now include current address, approval, pending address, and deadline so checked lifecycle changes retire only obsolete completed claims. Existing owner/session/target invalidation still clears every private cooldown value.

JS, JVM, and Android render the current address, a separate read-only pending replacement, candidate status, and the localized UTC deadline restriction. Existing private owner predicates remain around all of those fields. The production desktop editor keeps `settings-email-saved` and adds `settings-email-pending` and `settings-email-cooldown` where desktop semantics support stable identities. The view changes use existing form/text-field and button components; the JVM/Android read-only fields are the existing local Material fallback because the shared web component cannot express those platform widgets. No shared Calm Studio component or stylesheet was modified.

Focused shared tests cover retained-current/pending/draft behavior, candidate resend, typed rejection without POST, and equality-safe expiry refresh. The desktop production panel test now renders simultaneous approved-current and pending fields. Formatter tests cover the UTC epoch and a year boundary. Existing issue #79 state, stale-operation, dirty-draft, privacy, root-other, and renderer tests remain in the selected suites.

Verification passed:

- `./gradlew --no-parallel :wishlist.features.ui.users:jvmTest --tests '*UserEditViewModelEmailTest' --tests '*UserEditEmailRenderTest' --tests '*EmailChangeDeadlineTextTest'`
- `./gradlew --no-parallel :wishlist.features.ui.users:jsNodeTest :wishlist.features.ui.users:testDebugUnitTest :wishlist.features.ui.users:compileKotlinJs :wishlist.features.ui.users:compileKotlinJvm :wishlist.features.ui.users:compileDebugKotlinAndroid`
- `XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast ast-index rebuild` indexed 812 files.
- `git diff --check`

The UI slice is complete. README prose was intentionally left unchanged for the documentation slice requested by the task coordinator. The overall task is not claimed complete because the remaining documentation and external PostgreSQL proof are outside this UI slice.
