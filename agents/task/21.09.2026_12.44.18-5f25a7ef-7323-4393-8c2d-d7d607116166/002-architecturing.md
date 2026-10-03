Model: OpenAI Codex GPT-6 Astra (HL)
Changed files: agents/task/21.09.2026_12.44.18-5f25a7ef-7323-4393-8c2d-d7d607116166/002-architecturing.md

# Architecture: preserve Detekt enforcement while merging current master

## Model, scope, and decision

The assigned HL model satisfies Architecturing's HL-first priority. Caveman full applies to working notes, while this substantive architecture report uses normal prose. No local instruction overrides were found. The explicit role assignment authorizes this report despite the generic LL preference for mechanical documentation. Only this report is written and committed; no merge, implementation edit, build, push, or operator contact is performed.

Use the incoming master Verification document as the semantic baseline and transplant the PR's blocking Detekt gate and report section. Preserve master’s Preparation terminology and exclusive return-to-root outcomes. The resulting document remains a mechanical verifier, not an orchestrator or council auditor. No application architecture, dependency, public interface, auth behavior, baseline, or custom rule change is required.

## Evidence and exact identities

The accepted input is `001-planning.md` in this task. The initial PR head is `1cbab45e1579cd8168bd3c88e34d15278ac8397f`; the fixed incoming master is `f786ad93c3e8a1ceeb3025a35a4922bd55d7ed3a`; their merge base is `a220b3d2f9f4224e09880022e3051c374bd69a6a`. The report-only bootstrap and Planning commits are already above the PR head and must remain preserved.

All three versions of `agents/VERIFICATION.md` were read directly through Git. The base has six steps and directs failures to Coding and success to Validating. The PR adds blocking Detekt as step 2, renumbers the remaining steps, and adds a Detekt report section. Master replaces Architecture with Preparation, removes the future-stage introduction, and sends all outcomes to root. A read-only three-tree merge preview confirms two conflict regions in this single path. The rest of master should merge cleanly; the actual merge must independently establish the conflict set.

The root Gradle file already aggregates Detekt for Kotlin-bearing subprojects, scans recursive `src/**/*.kt`, loads the custom rule provider, preserves module-specific baselines, and keeps `ignoreFailures=false` with `FailOnSeverity.Error`. `detekt-rules/build.gradle` already supplies Kotlin/JVM 17 and the required API/test dependencies. The focused tests contain eight syntax cases for `NoElseIf`, plus one service-loader/provider test. These tracked files are preservation targets, not implementation work.

## Current best-practice research

Consulted on 2026-09-21:

- Git merge manual, <https://git-scm.com/docs/git-merge>, particularly `--no-commit`, conflict presentation, and conflict resolution. The official manual recommends inspecting the common ancestor and both sides, editing the actual conflict, staging the resolved files, and completing the merge. Applied here as a true two-parent merge of the pinned commit, with deliberate combination of both Verification changes. Whole-file `ours` or `theirs`, squash, rebase, and force push are rejected because they either discard behavior or do not preserve the requested merge history.
- Detekt Gradle integration documentation, <https://detekt.dev/docs/gettingstarted/gradle/>. The documented `ignoreFailures=false`, severity gate, and baseline behavior match the PR's implementation. Applied by keeping the existing configuration and requiring a separate successful Detekt invocation before build/test commands. Regenerating baselines, suppressing lint failures, or changing plugin versions merely to obtain green output is rejected.

The advertised web search/fetch tools returned an unavailable-handler response. Public official pages were therefore retrieved successfully with read-only HTTPS `curl`, and relevant documented sections were searched directly. No unavailable-tool response is represented as a successful web search. No new library or custom tool is proposed.

## Exact merge resolution contract

Start a normal non-squash merge of the pinned master commit into the current PR worktree with `--no-commit --no-ff`, retaining the current PR/task commits as the first-parent history. Inspect the actual unmerged entries and stage-1/base, stage-2/PR, and stage-3/master blobs. If additional conflicts appear, report them to root before resolving outside this contract.

Resolve `agents/VERIFICATION.md` to the PR-head version with exactly these five substitutions, and no other content changes:

1. Replace `Verification runs after Coding and before Validating. Its sole purpose is to confirm the build compiles and tests pass.` with `Its sole purpose is to confirm the build compiles and tests pass.`
2. In step 1, replace `specified by Architecture.` with `specified by Preparation.` Keep `Read the latest step report` unchanged.
3. In the Detekt failure paragraph, replace `write \`result=FAIL\`, hand back to Coding, and do not start` with `write \`result=FAIL\`, return the report to root, and do not start`.
4. In both build-failure and test-failure steps, replace `and hand back to Coding. Do NOT proceed to Validating.` with `and return the report to root.`
5. In the final success step, replace `and hand off to Validating.` with `and return it to root.`

The five substitution rules cover six occurrences because rule 4 applies twice. They produce the complete intended file deterministically from the immutable starting PR blob. Steps remain numbered 1 through 7. The Detekt command is exactly `./gradlew detekt`, precedes the build command, and retains `set -o pipefail`, the immediate recorded status, the no-build/no-explicit-tests failure condition, and the mandatory findings report. The final PASS condition remains `If Detekt, build, and all tests pass`. The `### Detekt` report section remains before `### Build` and `### Tests`.

The document must contain no direct stage transfer to Coding/Validating, no future-stage order, and no obsolete Architecture stage name. Describing what was coded and referring to Preparation's completed specifications is permitted prior-work context. Root alone chooses subsequent transitions.

All other paths use Git’s clean merge result. In particular, accept incoming master’s Preparation/council framework and deletion of `agents/PLAN.md` and `agents/ARCHITECTURE.md`; do not resurrect legacy role files for the current pre-merge reports. Preserve all historical task artifacts from both sides byte-for-byte. Parent root must explicitly handle the workflow-version boundary; completed pre-merge Planning/Architecturing artifacts must not be rewritten retroactively. Subsequent workers need root-filtered instruction bundles from the merged tree, not the full root routing catalog.

## Ordered implementation and preservation

First confirm the worktree is clean and record the pre-merge HEAD. Merge the pinned master, resolve only Verification according to the deterministic substitutions, and inspect every staged path against both parent histories. Preserve clean auto-merges, especially the new root routing, Preparation, council contracts, and KDoc-only change in `features/roles/common/src/commonMain/kotlin/Plugin.kt`. Stage the resolution and the assigned Coding report only in addition to Git's legitimate merge staging. Complete a true merge commit with the required real-newline co-author trailer; do not stage another role's uncommitted report.

The PR-owned `build.gradle`, `settings.gradle`, `gradle/libs.versions.toml`, `config/detekt/**`, and `detekt-rules/**` must remain byte-identical to the initial PR head. Never regenerate baselines to accommodate a documentation merge. `.gitignore`, Operator Notes, and runtime application behavior are protected. The incoming source extension change is KDoc-only; no source mutation beyond that clean incoming change is planned. Rebuild the index if required for the source-bearing merged tree, without staging generated index data.

Other PR branches/worktrees are outside scope. Worktree-specific temporary logs must replace shared `/tmp/build-output.txt` paths at execution time so parallel agents cannot overwrite evidence; record the selected paths without broad cleanup of shared directories. A failing check must be diagnosed and returned to root, not suppressed. Root owns the eventual non-force push to `origin/fix/issue-72-detekt-conventions` and live PR verification.

## Tests and acceptance specifications

No new functions, classes, routes, data schemas, or UI behavior are planned. Reuse the meaningful existing Detekt tests and build rather than add tests mirroring prose. All resolution and preservation requirements below are mechanically checkable; no untestable new functionality requires an operator decision.

1. **Exact resolution:** derive the expected Verification bytes from the pinned PR blob using the five substitutions above, asserting every expected occurrence count, then compare the merged file byte-for-byte. Assert Detekt before build, all three `pipefail` examples retained, steps 1–7 present, Detekt/Build/Tests report sections present, and no `Architecture`, `Validating`, `hand back to Coding`, or `hand off` strings in active Verification.
2. **Merge integrity:** assert the merge commit has two parents; the second is pinned master; both pinned master and the initial PR head are ancestors of the final head; no unmerged index entries or conflict markers remain. Compare the PR-owned Detekt paths to the pinned PR head with an empty diff. For paths changed only by master, compare the merged bytes to pinned master. Account for this task's new reports separately and preserve prior report blobs from both parents.
3. **Blocking gate:** run `./gradlew detekt --console=plain` with an existing supported JDK/SDK and command-local environment. Run it before build or any explicit test command. Capture Gradle, tee, and pipeline statuses immediately under Bash `pipefail`; record findings and log path. Expected outcome: zero status. On any nonzero status, stop the sequence with FAIL and do not launch subsequent build/test checks.
4. **Custom rule regression:** only after Detekt passes, run `./gradlew :wishlist.detekt-rules:test --rerun-tasks --console=plain`. Require all eight `NoElseIfTest` cases and `WishlistRuleSetProviderTest.loadsWishlistRuleSet` to pass. Cases cover braced/expression/multiline/comment-separated chains, both locations in a multi-link chain, accepted binary else, no-else conditional, explicitly nested conditional, and service discovery of exactly the expected custom rule. Read XML results for real counts; do not label Gradle task counts as tests.
5. **Complete build/test gate:** after the prior checks pass, run `./gradlew build --console=plain`. Record actual failures, executed test tasks, test counts, and logs. If the build output shows no executed test tasks, run `./gradlew allTests --console=plain` explicitly and record its status/results. Do not treat cached or absent tests as newly executed cases. The targeted forced custom-rule run independently establishes live custom-rule coverage.
6. **Final hygiene:** run `git diff --check` for the new resolution/report changes, verify parsed commit trailer and clean tracked/index state, and compare the committed merge resolution against the exact expected bytes. Full-history whitespace warnings must be distinguished from newly introduced errors, never silently rewritten. No lint baseline or generated/build artifact may be committed.

Detekt, focused custom-rule tests, full build, and any required explicit `allTests` must all pass before root may treat the task as ready to push. This Architecturing report does not claim any of these Gradle gates have run.

## README updates

None beyond incoming master's clean README changes. The manual conflict is an agent instruction file; no feature semantics change. Preserve all incoming feature documentation and Operator Notes exactly rather than adding unrelated feature notes.

## Tool limits and remaining risks

`ast-index` exists but reports `Index not found. Run 'ast-index rebuild' first.` No index rebuild was performed by this report-only role. Inspection used Git's known changed-path inventory and direct reads; no code symbol navigation claim depends on an absent index. The plan anticipates possible environment/resource contention from concurrent builds and existing generated state, but authorizes no dependency or baseline changes to hide failures. Actual merge conflict count and build outcomes remain checks for implementation/verification.

## Result

The merge has one bounded manual resolution with an exact byte-level contract. It preserves the PR's blocking Detekt behavior and all clean incoming master changes while removing obsolete stage routing. No operator questions remain; implementation can proceed within the approved merge scope.
