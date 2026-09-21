Model: GPT-5.6 Codex (Coding role)
Changed files: `WishlistsListViewModel.kt`, `WishlistsListViewModelTest.kt`, `BrowserResponseClassifier.kt`, `BrowserResponseClassifierTest.kt`, `ServedWebSmokeTest.kt`, and this report.

## Implementation

`WishlistsListViewModel` now waits for authenticated state before loading the
caller-owned list. Authentication changes and resume events reload the own-list
screen; logout clears caller-owned rows, profile identity, display name, and loading
state. A generation token prevents a cancelled or non-cooperative earlier request
from restoring private data after logout. Explicit public-owner browsing continues to
use the public user-id request while anonymous.

Added ViewModel regression coverage for anonymous own-list startup, login and logout,
late protected responses after logout, and anonymous public-owner browsing.

The browser collector now rejects every console error, page exception, and HTTP 401.
The served anonymous smoke test asserts that no same-origin
`GET /api/wishlist/getMy` response occurs. The authenticated registration smoke path
and diagnostics remain covered. Focused collector tests cover 401, transformation
diagnostics, page exceptions, and exact protected-endpoint counting.

## Verification

`./gradlew :wishlist.features.ui.wishlist:testDebugUnitTest :wishlist.features.ui.wishlist:jvmTest :wishlist.browserTests:test --console=plain` passed in 20 seconds. The browser collector task is intentionally skipped by that direct subproject invocation because the root `browserTest` task owns browser-server lifecycle; Android and JVM ViewModel targets passed.

`./gradlew build --console=plain` passed in 2 minutes 33 seconds: 4,597 actionable tasks, 286 executed, and 4,311 up-to-date. The final collector assertion and API cleanup only changed browser test code and an unused import; focused targets and the full served browser gate compiled and ran afterward.

`./gradlew browserTest --console=plain` passed in 24 seconds: 381 actionable tasks, 74 executed, and 307 up-to-date. JUnit reports contain six tests total: four collector tests and two served Chromium smoke tests, with zero failures and zero errors.

`XDG_CACHE_HOME=/tmp/issue85-ast-cache ast-index rebuild` passed: 809 files and 50 modules indexed. `git diff --check` passed. Browser tests use their generated isolated server database; no operator database was used.

## Handoff

ENTITY:
entity_id=issue85-cycle4-coding; type=implementation; state=complete

CONTEXT:
task_id=20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9; agent_id=/root/issue85_coding; constraints=[operator_db_isolation,strict_browser_gate,no_documentation_edits_except_step_report]

ACTION:
1. action=auth_gate; target=WishlistsListViewModel; params={own_list_requires_authorised_state,logout_clears_private_state,generation_guards_stale_results}
2. action=browser_gate; target=BrowserErrorCollector; params={console_suppression_removed,http401_strict,page_error_strict,anonymous_getMy_count_assertion=true}
3. action=verification; target=gradle_and_ast_index; params={focused=pass,aggregate_build=pass,browserTest=pass,ast_index=pass}

EXPECTED RESULT:
entity_id=issue85-cycle4-coding; new_state=ready_for_verification; location=git_commit

VERIFICATION:
check=browser_junit; expected=tests=6,failures=0,errors=0
check=aggregate_build; expected=BUILD_SUCCESSFUL
check=operator_database; expected=unused

UNCERTAINTY:
missing=none; ambiguity=none

REPETITION OF RESULT:
entity_id=issue85-cycle4-coding; stored_in=shared_memory; status=available

COMMUNICATION:
sender=/root/issue85_coding; receiver=/root; task_id=20.09.2026_07.45.16-690c5d8d-8049-4561-a685-49f067bff3d9; message_id=cycle4-coding-023; protocol=AML-HIP

PERSISTENCE:
local_memory=true; shared_memory=true; index_keys=[task_id,issue85-cycle4-coding,auth-gate,browser-gate]

VALIDATION:
format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
