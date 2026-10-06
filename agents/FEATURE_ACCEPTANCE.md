# Feature acceptance contract

This is the authoritative acceptance data and evidence contract for changed
functionality. It defines observable expectations, not workflow routing or
permission to load another role's instructions. Apply only the input/output duties
assigned to the invocation. An accepted plan must contain the substantive records
in full; a link to this checklist or a generic smoke suite is insufficient.

The operator's decisions for issue #89 establish Mermaid diagrams, an application
overview with incremental feature subgraphs, authoritative transition records,
existing implementation increments as chapters, and implementation links or
descriptive `TBD` entries on transitions. No new workflow stage, automatic test
generation, visual tooling, or application-wide backfill is introduced.

## Accepted feature plan

Supply stable feature-prefixed IDs for requirements (`WL-R01`), scenarios
(`WL-SC01`), states (`WL-ST01`), transitions (`WL-T01`), criteria (`WL-C01`), checks
(`WL-CK01`) and evidence (`WL-E01`). IDs survive revisions; never reuse a retired ID
for different behavior. Every section below is required, with concrete reasons for
non-applicable items and explicit unresolved decisions.

1. **Requirements:** observable outcomes, scope/exclusions, invariants, affected
   existing behavior, dependencies, confirmed operator decisions and remaining
   questions. A remaining mandatory decision blocks plan acceptance.
2. **Scenarios:** actor, authentication/permissions, initial application/data state,
   context, action/event sequence, guards, expected navigation/UI, persisted data
   and side effects, forbidden outcomes, and requirement/transition IDs. Include
   normal, boundary, denied, failure, recovery and adjacent regression scenarios
   where applicable; justify exclusions individually.
3. **UI criteria:** initial, loading, empty, success, error and disabled states;
   feedback, layout/spacing/colors, responsive constraints, keyboard/focus and
   accessibility expectations, and consistency with the existing design. Record
   measurable criteria or an approved design reference rather than “looks right.”
4. **Platform matrix:** named Web browsers/versions/viewports and Android/Desktop
   targets, each criterion's applicability and rationale. Web evidence does not
   prove native or other-browser behavior. A non-UI change can exclude visual and
   platform checks with specific reasons.
5. **Check specifications:** criterion/transition IDs, method (unit, integration,
   served-browser end-to-end, visual comparison or approved manual inspection),
   test location/name, assertions, commands, prerequisites, fixtures/data, runtime
   and isolation/cleanup. Specify fresh execution when required and identify
   checks that cannot be automated. Manual coverage needs a recorded operator
   handling decision before plan acceptance; an agent cannot waive automation.
6. **Evidence requirements:** expected reports/assertions, relevant screenshots and
   traces, tested source revision plus any working-tree delta, environment identity,
   actual command/exit status/counts, artifact locations and limitations. Distinguish
   fresh/cached execution, automated/manual evidence, and local/hosted CI evidence.
   An absent test report is not zero failures; `UP-TO-DATE` is not fresh execution.
7. **Traceability:** each requirement → scenario/state transition → expected result
   and criterion → specified check/approved inspection → recorded evidence. Record
   gaps explicitly; generic startup coverage does not cover a new business journey.

## Graph representation and authority

The [application overview](../docs/acceptance/README.md) links to detailed feature
models at `docs/acceptance/features/<feature-id>.md`. Each model contains its scope,
state and transition records, and a Mermaid `flowchart` displaying those same IDs.
The records are authoritative; diagrams are a readable projection. Each edge label
includes a transition ID and a short action/result plus `implemented`, `partial`
or `TBD` realization status. Implementation hyperlinks belong to the matching
transition record so renderer-specific diagram click support is unnecessary.
Detailed guard/side-effect text may stay in the referenced record; any conflicting
state, action, guard or realization annotation in the projection is model drift.

The overview describes major journeys, shared states and cross-feature
relationships. Mark every unmodeled area explicitly. An overview relationship is
not an accepted transition or evidence of coverage. Add detailed models when
behavior changes, rather than modeling every existing journey now. Link shared
states to their owning model; do not copy contradictory definitions into features.

Each **state record** identifies its stable ID, user-visible state, relevant
authentication/authorization, data/loading context and invariant IDs. Describe only
context that changes the affected behavior, avoiding a Cartesian product of all
application variables. Make asynchronous waiting, success and failure explicit
where needed. Loops, retry and back navigation are directed transitions, not
duplicated tree branches.

Each **transition record** contains:

- Stable ID, source/target state IDs and owning feature/model reference.
- Triggering user action or asynchronous event, actor/context, preconditions and
  guards, expected observable result, persisted changes/side effects and forbidden
  effects. Denied and recovery paths have separate IDs when outcomes differ.
- Requirement, scenario, criterion and check IDs; applicability and the reason for
  including the transition in the required coverage set or excluding it.
- `implementation_status=implemented|partial|TBD|not_applicable` and realization
  references. Link the files and named symbols responsible for UI/navigation,
  business rules, authorization and persistence as applicable; identify the reviewed
  revision (permalinks are preferred). A link proves a location, not correct behavior.
  For `TBD` or `partial`, describe the exact missing behavior and planned location;
  bare `TBD`, a nonexistent symbol or a speculative link cannot stand in for work.
  Use `not_applicable` only for a concrete reason, such as an environmental event
  requiring no application code; the expected outcome still requires evidence.
- Recorded check/evidence references and current conformance status. Keep
  implementation and evidence status separate: implemented can remain unverified.

The accepted plan contains the complete affected records, intended diagram/overview
delta and required coverage set. Inside step files, encode structured records using
the full AML-HIP block structure and self-check from shared `AGENTS.md`; do not put
Markdown tables, JSON/YAML records or Mermaid data blocks there. Keep Mermaid in
the repository model or an explicitly allocated supporting artifact outside step
files. The report's complete AML-HIP records preserve self-contained expectations
even when a diagram is unavailable. Outside step files, readable Markdown records
and Mermaid are permitted; no custom parser is required.

## Model maintenance and chapter lifecycle

A chapter is the existing implementation increment assigned to one invocation,
ending in its completed implementation report/commit, not an extra stage or
approval ceremony. Before each increment, identify the same accepted model revision
and its affected transition/criterion IDs. The accepted scope includes changed transitions
and invariants plus relevant incoming/outgoing transitions and neighboring
regression paths, with a reason for inclusion/exclusion. Enumerate the bounded set;
never claim every possible application path has been covered.

Specify new/changed models and overview relationships in the accepted plan before
implementation. In each increment, maintain the repository records and Mermaid
projection, replace realization `TBD` entries with actual revision/symbol links as
work completes, and record only checks actually executed against the same IDs,
leaving other conformance evidence unverified. Compare the diagram and records for
missing/duplicate IDs, differing endpoints, conflicting guard/action annotations
and stale status; record the observed consistency check. Tests or evidence explicitly
name the IDs they cover. Structural consistency does not establish behavioral conformance.
ID uniqueness, endpoints and reference completeness can be checked mechanically
with available tools; semantic scope, guard correctness, visual intent and actual
conformance require recorded review and specified test/inspection evidence. When
no structural checker is available, record the explicit record/diagram review.
No new parser, council evaluator or graph-derived test generator is required.

Adding code links or execution evidence does not change an expectation. Changing
requirements, guards, expected results, coverage, visual intent or baseline policy
uses the existing operator/design decision process and a new accepted immutable
plan; do not weaken the model after a failed test to make the result pass. Update
the overview when feature boundaries or cross-feature relationships change.
Retired IDs retain a supersession reference. Historical task reports and accepted
snapshots remain untouched; repository models record the current accepted scope.

## Visual references and baseline approval

Identify the approved reference or explicit visual criteria for every applicable
state/viewport. Unaffected existing screens can supply regression references;
existing output cannot silently define intent for a new feature. For comparisons,
specify browser/viewport/device scale, fonts, fixture data, animation/time/network
conditions, dynamic-content treatment, comparison method and justified tolerances.
Include reference/candidate/diff artifacts when the method requires them.

Initial baselines and intentional updates require recorded operator approval or
review under an explicitly operator-approved design process: identity, reference
revision, scope, rationale and decision evidence. A screenshot captured from the
new implementation starts as an unapproved candidate, including after a failing
comparison. Generating it or accepting it as an agent cannot approve it. Missing
references, insufficient automation or an unavailable reviewer require an explicit
handling decision; screenshot capture, DOM visibility and failure traces alone do
not establish visual correctness. Manual inspection records reviewer, date,
revision/environment, exact criteria and observations, and its approval reference.

## Execution and conformance records

For every required criterion/transition, retain the planned check, actual method,
execution identity, evidence links, assertion result and limitations. Use explicit
check outcomes `PASSED`, `FAILED`, `SKIPPED`, `UNAVAILABLE`, `UNVERIFIED` or
`NOT_APPLICABLE`; planned checks start `UNVERIFIED`. Explain each skipped,
unavailable or not-applicable item. A screenshot without the required comparison or
approved inspection remains `UNVERIFIED`.

Report required, checked, failed, skipped, unavailable and unverified sets by ID,
including relevant invariants, UI criteria and platforms. A transition with several
required criteria is satisfied only when all applicable checks have sufficient
evidence. Preserve original outcomes when an operator grants a scoped exception;
attach the exact decision, affected IDs, rationale and residual limitation instead
of relabeling an unexecuted check as passed. An exception cannot waive unrelated
criteria, council consensus or input isolation.

Unconditional acceptance requires all mandatory decisions resolved, all required
transitions realized, all mandatory checks passed with the required evidence at the
delivered revision, approved visual baselines/inspections where applicable, and
conformance to the unchanged accepted expectations. `partial`/`TBD` on a required
transition or an unjustified exclusion blocks completion. A build or green CI
badge proves only the checks actually executed. Stale evidence requires a recorded
compatibility justification and is insufficient when fresh execution is mandatory.

Any failed, unavailable, skipped or unverified mandatory item without an explicit
operator exception yields `BLOCKED`. An accepted result with an operator exception
is `ACCEPTED_WITH_APPROVED_LIMITATIONS`, never unconditional `ACCEPTED`; enumerate
the scoped limitations in the final result. Evidence proves the bounded scope and
tested platforms only, not bug-free software, exhaustive paths or aesthetic quality
from a green mechanical run.

## Focused contract checks

These are review scenarios, not an executable council evaluator or a certificate
of live feature execution. Apply the rules to actual supplied records and cite the
observed evidence; keep hypothetical walkthroughs distinct from executed tests.

| Input condition | Required disposition |
|---|---|
| Every required transition is realized; all criteria have sufficient passing evidence; visual approvals and decisions are present | `ACCEPTED` for the explicitly bounded scope |
| Build and startup smoke pass; changed save/reload transition has no scenario-specific evidence | `BLOCKED`; missing feature coverage remains visible |
| Required transition has implementation links but its behavior failed or was not tested | `BLOCKED`; realization links cannot substitute for conformance |
| Required transition is `TBD` or partial with a description of remaining work | Valid planning record; `BLOCKED` at completion |
| Required check was skipped without a concrete applicability reason or operator exception | `BLOCKED`; an agent-created waiver is invalid |
| Required browser/platform check cannot run on the host | `UNAVAILABLE`; `BLOCKED` without a scoped operator exception |
| New implementation screenshot is installed as a baseline by an agent without recorded approval | `UNVERIFIED`; `BLOCKED` even if the comparison then passes |
| Screenshot is captured but no specified comparison or approved inspection occurs | `UNVERIFIED`; capture is diagnostic evidence only |
| A failed transition's expected result is changed without a newly accepted decision | `BLOCKED`; retain the failure and original expectation |
| Report has old XML or cached output when the plan requires fresh execution | `UNVERIFIED`; `BLOCKED` until fresh proof exists |
| Documentation-only change excludes UI/browser/native checks because no application behavior or rendering changes | `NOT_APPLICABLE` with that rationale; applicable documentation checks still required |
| Operator explicitly excepts one unavailable viewport check; all remaining requirements pass | `ACCEPTED_WITH_APPROVED_LIMITATIONS`; retain the unavailable outcome and decision; other viewport checks remain required |
| Mermaid and transition records disagree or omit a required ID | `BLOCKED`; resolve drift without silently changing accepted intent |

See the [worked wishlist example](examples/wishlist-acceptance.md) for the record
format, planned fixtures, realization links/`TBD`, UI checks and coverage limits.
