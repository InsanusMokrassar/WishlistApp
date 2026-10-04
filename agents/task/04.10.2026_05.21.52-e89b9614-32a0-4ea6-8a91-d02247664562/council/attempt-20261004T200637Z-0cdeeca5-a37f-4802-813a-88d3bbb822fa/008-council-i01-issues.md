Model: gpt-6.1-sol; HL; high reasoning; no fallback.
Changed files: agents/task/04.10.2026_05.21.52-e89b9614-32a0-4ea6-8a91-d02247664562/council/attempt-20261004T200637Z-0cdeeca5-a37f-4802-813a-88d3bbb822fa/008-council-i01-issues.md

# i01 complete issue snapshot

The task is 04.10.2026_05.21.52-e89b9614-32a0-4ea6-8a91-d02247664562. Preparation collates, without a vote. The exact brief is agents/task/04.10.2026_05.21.52-e89b9614-32a0-4ea6-8a91-d02247664562/council/attempt-20261004T200637Z-0cdeeca5-a37f-4802-813a-88d3bbb822fa/001-council-brief.md and candidate is agents/task/04.10.2026_05.21.52-e89b9614-32a0-4ea6-8a91-d02247664562/council/attempt-20261004T200637Z-0cdeeca5-a37f-4802-813a-88d3bbb822fa/007-council-i01-candidate.md. Completed proposals are 003 through 006 in this attempt. This snapshot preserves every originating comment verbatim with source attribution. Independent source-reassessment RESOLVED statements do not count as fresh candidate consent. P-REENTRY-01 through 04 remain RESOLUTION_PROPOSED until Programmer explicitly dispositions the exact candidate. Every role must explicitly adopt and disposition its own history in its fresh vote. No technical conflict or operator question was reported in the completed proposal wave. Remaining notes require precise proof limits and no prerequisite design edit.

All four source findings and the conditional risk are mapped to AC1 through AC7 in the unchanged Architect sections. The completed excerpt's A-1 through A-4, A-2/i01 recovery supplement, D2-R1, P2-R1, SEC-I01-01, P1, D1 and D8 are carried as safe technical requirements in A-R1 through A-R5 and the other attributed comments below. Their earlier acceptance is not reused. Their supplied technical limits and test/README instructions remain included in the candidate. No inaccessible historical comment or vote is fabricated.

## Architect origins, source 003-council-proposal-architect.md

## Comments, disposition, questions and exclusions

A-R1 addresses findings one and three and AC1/AC3/AC6. The required raw terminal guard, secret clearing, renderer exits and approval-free persisted navigation are present in current source and covered by the named specifications. Disposition: resolved for this independent source reassessment; retain unchanged behavior. Historical A-1 is adopted as technical evidence only, without inheriting an earlier vote.

A-R2 addresses finding two and AC2. Result publication precedes GET; retained recipient identity survives temporary profile absence; paired clearing and authoritative retirement are present. Disposition: resolved for this independent reassessment. The completed excerpt's Architect A-2 and i01 recovery correction, Designer D2-R1, Programmer P2-R1 and Security SEC-I01-01 are adopted only as supplied completed technical requirements. The inspected implementation satisfies their captured-identity/paired-clearing concern; no current peer artifact or fresh peer assent was consulted.

A-R3 addresses finding four and AC4. Immediate finite raw paging, exact-purpose/expiry deletion, failure backoff and SMTP-independent lifecycle start are present. Disposition: resolved for scoped bounded cleanup; no hard retention promise or expiry-schema expansion is selected. Historical A-3 is independently reassessed rather than treated as consent.

A-R4 addresses conditional finding five and AC5/AC6. Exact-record committed deletion and actual independent PostgreSQL contention/deferred-commit-failure tests are present. Disposition: resolved as a design remedy for the conditional race; deployed applicability remains unknown and no incident is asserted. Historical A-4 is independently reassessed. Account-wide concurrency beyond the same approval remains excluded.

A-R5 preserves completed Programmer P1, Designer D1 and D8 constraints from the safe excerpt. Transport uncertainty classification excludes known-success navigation errors; terminal admission is page-local; safe exit persists users navigation without a success claim. Disposition: resolved by inspected implementation and retained specs; no receipt endpoint or arbitrary-reload guarantee is selected.

A-N1 is a nonblocking integration note: Exposed nested transactions may share an outer commit, so future callers must preserve the inspected top-level consumption-before-write boundary. Evidence is the current callback path and Exposed transaction documentation. No current outer transaction was observed; no production change is required. Disposition: nonblocking note, carried as an invariant rather than an allegation or prerequisite change.

A-N2 is a nonblocking evidence note: observed HEAD differs from the application repair commit, while the restricted application-path diff is empty. Historical full-build and focused-gate results remain historical; this council ran no gates. Disposition: nonblocking note; source equivalence and proof limits are explicitly recorded.

Operator questions: none are necessary for the recommended scoped retained implementation. No required automated functionality was identified as untestable. Replica count is deliberately unknown and unnecessary to select the storage remedy. No manual-only waiver, historical isolation assertion, new approval, live CI success or retention SLA is invented. A later actual inability to execute a required platform/database gate would be a new precise handling question, not grounds to silently skip the gate.

Exclusions are source-review rewriting, source implementation by council participants, a new issue/branch/PR, external comments/messages, push/merge/deploy, production data access, dependency upgrades, .gitignore changes, session revocation, receipt/idempotency endpoint design, universal deeplink expiry, physical zeroization, live SMTP, physical-device proof and general distributed-account redesign. All comments originating in this proposal are recorded above; there is no unresolved architectural blocker from this role and no claimed aggregate consensus.

## Designer origins, source 004-council-proposal-designer.md

## Comment dispositions and questions

Comment D-R1 concerns AC1/AC3 terminal admission and feedback. Disposition: RESOLVED in the inspected existing repair. The raw uncertainty/denial guards, active-field clearing, three stopped renderer branches, and paired terminal tests address the concrete retry/retained-secret failure. Recommendation: retain those exact behaviors and tests; no additional form design is required.

Comment D-R2 concerns AC2 acknowledged delivery during failed or temporarily absent profile reads and recovery. Disposition: RESOLVED in the inspected existing repair. The private captured recipient, comparison against fresh authoritative profile, paired clearing, and held/null/wrong-owner/failed-read matrix address the concrete feedback-loss failure. Recommendation: retain captured identity and owner-lifecycle checks; no new request or renderer-visible address is required.

Comment D-R3 concerns AC1/AC3 safe navigation and AC6 outcome truthfulness. Disposition: RESOLVED in the inspected existing repair. Continue uses users-list replacement and actual navigation integration tests cover unknown/denied outcomes and save failure without replay or success fabrication. Recommendation: retain credential-free saved exit and the explicit persistence-failure limitation.

Comment D-R4 concerns AC4 operator visibility of cleanup guarantees. Disposition: RESOLVED in the inspected existing repair. Source, plugin tests, and Email Architecture Notes agree on immediate startup, 100-row finite keyset sweeps, lifecycle ownership, SMTP independence, and the delay after sweep. Recommendation: retain the absence of a hard retention SLA through outage/table growth.

Comment D-R5 concerns conditional AC5 and preserved AC6 choices. Disposition: RESOLVED in the inspected existing repair. Committed exact-record consumption and real PostgreSQL contention/deferred-commit tests address the conditional same-approval race without asserting deployed replica count or changing separate commits/sessions. Recommendation: preserve both the conditional finding classification and the bounded storage guarantee.

Comment D-R6 concerns AC7 documentation and automated platform proof. Disposition: RESOLVED as a design/specification concern. Existing test sources cover each changed renderer branch, and the reviewed-to-repair README diff preserves Operator Notes and places repair deltas in Architecture Notes. Recommendation: retain every specified gate; historical results remain historical and no missing-gate waiver is implied.

Comment D-R7 concerns accessibility claim limits. Disposition: NONBLOCKING_NOTE. W3C status-message guidance is relevant, but the supplied task does not establish a full accessibility-conformance requirement or authorize a common-component redesign. Existing automated text/control assertions do not demonstrate screen-reader announcements. Recommendation: keep the proof claim precise; no prerequisite change or operator question is attached.

Operator questions: none. No genuine unresolved architecture or requirement ambiguity, uncovered required user-journey specification, test waiver, or new topology fact was found in this independent inspection. Confidence is high for the scoped UI state, renderer feedback, and recovery design; evidence of actual test execution remains the supplied historical report rather than a fresh run. Exclusions are receipt endpoints, session revocation, unrelated registration/admin work, global accessibility refactoring, schema/dependency upgrades, service operation, deployment, source writes, and aggregate council outcome. This completes only the allocated independent proposal.

## Programmer origins, source 005-council-proposal-programmer.md

## Comment dispositions, assumptions and questions

P-REENTRY-01 concerns AC1/AC3 and the prior P1 transport-boundary clarification. Disposition: RESOLUTION_PROPOSED for the fresh candidate. Retain the observed raw uncertainty latch, scoped catch, field clearing and three terminal renderer branches. The code evidence and existing tests above support the mitigation; known navigation failure must never be rewritten as transport uncertainty.

P-REENTRY-02 concerns AC2 and the prior P2-R1 captured-feedback correction reproduced in the permitted excerpt. Disposition: RESOLUTION_PROPOSED for the fresh candidate. Retain the private captured recipient, early checked publication, authoritative retirement and paired clearing. The recovery/lifecycle cases at UserEditViewModelPasswordChangeTest.kt:403, :450 and :495 substantiate the correction. Temporary missing profile state cannot retire a send acknowledgment.

P-REENTRY-03 concerns AC4. Disposition: RESOLUTION_PROPOSED for the fresh candidate. Retain the immediate, idempotent, lifecycle-owned worker with bounded purpose-confined scanning and explicit retention caveats. No independent hard retention requirement was supplied.

P-REENTRY-04 concerns conditional AC5 and AC6. Disposition: RESOLUTION_PROPOSED for the fresh candidate. Retain committed exact-record consumption, unsupported-store denial and independent PostgreSQL/commit-failure proof. Keep the deployment finding conditional and preserve separate password persistence and sessions.

P-REENTRY-05 concerns AC7 and verification honesty. Disposition: NONBLOCKING_NOTE. Keep the required automated gates and Architecture Notes; report historical passes as historical. The exact four-README revision diff supports Operator Notes preservation. No executable council fixture or parser is proposed.

All comments in this proposal have explicit dispositions. No new blocker or genuine unresolved operator question was identified. Assumptions are limited to the brief's supplied repaired revision and historical execution facts; inspected source establishes the behavior described here. Confidence is high in feasibility and the source/test design, with fresh runtime proof outside this proposal. Exclusions remain source-review edits, new PR/branch, source writes by this participant, live deployment/SMTP, dependency upgrades, data rewrites, universal distributed claims and durable arbitrary-reload prevention. This proposal does not cast acceptance of an unassigned candidate.

## Security origins, source 006-council-proposal-security.md

## Comments, alternatives and dispositions

SEC-P01 concerns source finding 1 and AC1. Retain permanent raw terminal uncertainty admission and truthful Continue. Disposition: RESOLVED against inspected current source and the specified state/renderer/navigation regressions; source repair is observed, fresh aggregate acceptance remains pending.

SEC-P02 concerns source finding 2, AC2 and the prior security comment SEC-I01-01 identified in the permitted completed excerpt. Retain the independent private captured recipient and paired clearing. Comparing against a temporary profile placeholder would erase an acknowledged result on recovery; retaining feedback without lifecycle ownership could expose obsolete owner state. Current source implements both distinctions and the six supplemented recovery/lifecycle behaviors have automated coverage. Disposition: RESOLVED by this independent reassessment of the repair. Prior consent is not reused and a future frozen candidate still requires this role's fresh vote.

SEC-P03 concerns source finding 3 and AC3. Retain logical secret clearing and terminal safe exit on all platforms. Disposition: RESOLVED against production source and tests. Physical zeroization remains an explicit exclusion rather than an unproven promise.

SEC-P04 concerns source finding 4 and AC4. Retain immediate lifecycle-owned bounded purpose-confined cleanup with SMTP-disabled operation and sanitized logging. GET-only or issuance-only deletion cannot reclaim idle never-opened records; an unbounded getAll scan expands memory cost; OFFSET after deletion can skip rows. Disposition: RESOLVED against the keyset implementation and cleanup/plugin tests. Indexed purpose/expiry metadata is a credible future scaling alternative, but adds schema/backfill work without an established narrow requirement. No such change is proposed.

SEC-P05 concerns conditional finding 5 and AC5/AC6. Retain committed exact-record consumption and real PostgreSQL gates. A local mutex/read-plus-unset cannot identify the shared-storage winner. An explicit single-process restriction is an alternative requiring a supported-deployment choice; persistent one-use avoids needing to guess topology. Combining consume and password write into a rollback transaction conflicts with AC6 and is rejected. Disposition: RESOLVED for the scoped storage mitigation; deployment applicability remains conditional and unknown. Broad distributed account serialization is excluded.

SEC-P06 concerns AC6, recovery and honest claims. Preserve existing sessions and burn-before-write semantics, stop old writers before relying on the storage guarantee, and deploy matching UI/server assets. Deleted approvals cannot be reconstructed on rollback; forward repair is preferable within existing rollout constraints. Arbitrary reload persistence, a receipt/idempotency service, new recovery notifications and session revocation would expand the established task. Disposition: NONBLOCKING_NOTE recording retained limits; no prerequisite or new feature is requested.

SEC-P07 concerns AC7 and documentation integrity. All four feature READMEs have their existing Operator Notes placeholder unchanged. Read-only comparison between the reviewed and repaired revisions shows only Architecture Notes hunks: Auth at line 75, Deeplinks at 108/114, Email at 123 and UI users at 110. Current source matches the repair revision under those paths. Retain the documented terminal/reload/zeroization limits, captured-feedback identity, cleanup cadence/retention limits, storage capability/fail-closed behavior, stopped-writer rollout and separate consumption/password commits. Disposition: RESOLVED for the scoped README delta and Operator Notes preservation; do not rewrite unrelated feature documentation.

SEC-P08 concerns council evidence and authority integrity. The current capacity exception is explicitly supplied for this task and affects batching only; it does not waive independent context, complete roster, immutable packet or fresh votes. Completed technical evidence does not select the new candidate. No excluded-input exposure or artifact-tampering evidence was observed in this invocation, and no access-tracing/sandbox proof is claimed. Disposition: NONBLOCKING_NOTE; preserve the boundaries in subsequent collation and voting.

