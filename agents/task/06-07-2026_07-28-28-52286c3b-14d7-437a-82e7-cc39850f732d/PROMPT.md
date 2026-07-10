# Source prompt

> fix merge conflict for #63

## Context (orchestrator-derived, not part of raw prompt)

- PR #63: `feat(email): base email feature (#44)` — head `fix/44-email`, base `master`.
- GitHub state: `mergeStateStatus=DIRTY`, `mergeable=CONFLICTING`.
- Merge base: `37c520e`. PR head: `3417880`. Base (origin/master): `36d1d9e`.
- 10 conflicting files:
  1. client/android/src/main/kotlin/MainActivity.kt
  2. client/build.gradle
  3. client/src/jsMain/kotlin/Main.kt
  4. client/src/jvmMain/kotlin/Main.kt
  5. features/ui/adminPanel/README.md
  6. features/ui/adminPanel/src/commonMain/kotlin/Plugin.kt
  7. features/ui/adminPanel/src/commonMain/kotlin/ui/AdminPanelModel.kt
  8. server/build.gradle
  9. server/sample.config.json
  10. settings.gradle
- Goal: make PR #63 mergeable by merging `origin/master` into `fix/44-email`, resolving all conflicts keeping BOTH sides' intent (email feature + Calm Studio redesign / topbar-search-disable master changes), building affected modules, committing on `fix/44-email`.
- Note: local `master` is 1 commit ahead of `origin/master` (`daf38c8`, unpushed). Base used = `origin/master`. Do NOT push `master`.
