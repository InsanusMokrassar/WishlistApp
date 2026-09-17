Model: gpt-5.6-terra (ML Coding role; LL agent used solely for report filling)
Changed files: features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt, features/ui/users/src/commonTest/kotlin/ui/UserEditViewModelEmailTest.kt, features/auth/server/src/commonTest/kotlin/services/AuthFeatureServiceTest.kt, features/auth/server/src/jvmTest/kotlin/services/AuthFeatureServiceSqliteTest.kt, agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/026-coding.md

# Coding correction report

All VEC-05 proof gaps reported in 025 are closed by this correction. The change is limited to VEC-05 evidence; it makes no claim about VEC-06, VEC-07, or VEC-08.

The IME cooldown proof now starts unrestricted with a distinct valid dirty candidate@example.com draft, verifies Save is enabled and the real production IME callback is non-null, activates the cooldown with Refresh while preserving the draft, then invokes the captured callback and verifies that neither PUT nor POST occurs. The retarget proof uses an injected clock with an active pending value and deadline, pauses the ViewModel scheduler, and demonstrates that stale ViewModel pending/deadline metadata remains while the renderer omits saved value, pending value, editor, and cooldown. The skipped-POST reconciliation proof now uses the exact PUT, GET, GET sequence, with the final GET adding the later value and zero POST.

The authenticated cache proof now uses a fixture-owned SQLite file, two independent Database connections with Exposed repositories, and a live cache scope. The first ordinary cache is warmed, the second connection performs the exact approval and expiry replacement lifecycle, and the proof shows the ordinary cache remains stale while authenticated getUser returns fresh full state. The cache scope, connections, and fixture file close cleanly.

Verification passed: auth JVM focused common 23 plus SQLite 1; UI JVM focused 74 ViewModel plus 15 renderer; UI JS Node 82; UI JS browser 82; Android 82; users Exposed SQLite 28; auth server build; and git diff --check. The first auth compile attempt failed only because of fixture shape; the fixture was corrected and the final build passed. ast-index rebuild was attempted before and after the work and failed with `Read-only file system`; fallback navigation used the exact known files and rg, with no source concern identified.
