Model: GPT-6; HL; exact runtime variant is not exposed; no ML fallback.
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/002-preparation.md and its allocated council/batched-002/ evidence directory.

## Task understanding

PR #82 requires three outcomes. R1/AC1 is branch freshness against master, merging master only if needed. R2/AC2 is replacing application Long timestamps with korlibs DateTime and using the MicroUtils serializer. R3/AC3 is documenting a project-wide DateTime timestamp rule. Durations, identifiers, counters, amounts, sizes and monotonic elapsed-time measurements are excluded from timestamp conversion.

The preparation coordinator is /root/pr82_preparation_retry. The latest completed input was 001-preparation.md, followed by PROMPT.md and OPERATOR-ANSWER-001.md in this task directory. The earlier capacity failure remains unchanged. Root supplied fresh-fetch evidence dated 2026-09-27 that master f786ad9 is an ancestor of PR head 5664e7f and the local branch was fast-forwarded. No master merge is required. Preparation observed HEAD b228486fe1a1030c46f1a53f4123c0d00747e733 after local process-rule commits.

The operator approved independent council waves in two batches using identical frozen input. The approval is recorded in local preparation/protocol/check rules. This invocation performed investigation only through council participants; no source, feature README or application configuration was changed and no tests were executed.

## Council outcome

Result: BLOCKED. Terminal state: NEEDS_INFORMATION.

The complete roster was recursively discovered and read: architect from agents/council/roles/ARCHITECT.md, designer from DESIGNER.md, programmer from PROGRAMMER.md, and security from SECURITY.md in the same role directory. All four original contracts are preserved in council/batched-002/contracts/. The immutable common brief is council/batched-002/001-council-brief.md. Five voting cycles were available; none was entered because a required compatibility decision remains unresolved. No candidate or vote exists, and no consensus or implementation readiness is claimed.

All four participants completed independent proposals using fresh contexts and the same frozen packet. The actual invocation names were /root/pr82_preparation_retry/architect_proposal, /root/pr82_preparation_retry/designer_proposal, /root/pr82_preparation_retry/programmer_proposal and /root/pr82_preparation_retry/security_proposal_retry. Their reports are respectively 002-council-proposal-architect.md through 005-council-proposal-security.md under council/batched-002/. All used inherited GPT-6 at HL capability without fallback. No proposal is missing.

The first batch completed normally. The second batch encountered “agent thread limit reached” when launching security after programmer. Programmer was interrupted; a fresh security retry succeeded; four programmer resume attempts failed while the host retained completed threads; after security finished, programmer resumed its original incomplete invocation and completed. Root confirmed this delayed execution fits the operator's approved batching exception. The exact dispatch evidence is retained in 006-council-launch.md. No proposal was collated until all four returned. Packet contents and output allocations never changed. No peer-output exposure was observed or reported. Isolation was enforced by fresh contexts and explicit access restrictions; no filesystem audit is claimed.

## Investigation and unresolved compatibility

The architect and programmer independently verified that the pinned MicroUtils 0.30.1 DateTimeSerializer decodes and encodes Double epoch milliseconds, and korlibs-time 5.4.0 DateTime stores Double milliseconds. Their exact-version evidence comes from installed source archives, after beginning dev.inmo inspection in /home/aleksey/projects/own/MicroUtils. The architect found a library-documented lossless range of minus to plus 2^52 milliseconds. Arbitrary Long timestamps cannot retain exact millisecond identity after conversion.

The current users repository uses Math.addExact for email approval deadlines. ExposedUsersRepoSqliteTest.approvalDeadlineOverflowRollsBackWithoutPublishingAnEvent exercises Long.MAX_VALUE minus five and expects rollback with no event. A mechanical conversion to DateTime arithmetic would remove or alter that contract. Restricting accepted persisted/generated timestamps also changes the supported input and legacy-row contract. Architect ARCH-1 and programmer P1 therefore require an explicit decision rather than silently rounding or narrowing the domain.

The architect proposes finite integral durable email instants within [-2^52, +2^52] milliseconds, checked addition and storage conversion, fail-closed rejection without rewriting unsupported rows, and a read-only pre-deployment data check. This proposal is not accepted policy. The designer and security participant supplied compatible precision and rollback requirements but did not independently require an operator question; their position does not close the architect's or programmer's open question.

## QUESTIONS FOR OPERATOR

May durable email timestamps and issued deadlines be restricted to finite integral milliseconds within DateTime's documented lossless range of [-2^52, +2^52] milliseconds, rejecting out-of-range stored values and configurations without rounding or rewriting existing rows? Or must existing extreme Long values remain usable under a separately specified compatibility policy?

The architect and programmer recommend the checked, fail-closed range. Ordinary contemporary timestamps fit within this range. An answer is required because existing extreme values and overflow behavior are otherwise changed without an agreed contract. If extreme stored values must remain usable, their required behavior must be specified before an implementation-ready plan can be accepted.

This question follows agents/PREPARATION.md: “Any unclear requirement, constraint or architecture decision must become an explicit operator question”. No other immediate operator question was established. PostgreSQL test-database availability has not been checked; an unavailable disposable database would be an explicitly unexecuted verification gate, not a passed test.

## Requirement and comment dispositions

R1/AC1 is covered by root's ancestry evidence. R2/AC2 is investigated but blocked on ARCH-1/P1. R3/AC3 has proposed documentation text but remains unimplemented and unaccepted. No proposed implementation or test is counted as completed work.

ARCH-1 remains OPEN: the architect requires the precision and legacy-data decision above. P1 remains OPEN: the programmer independently requires the same decision. Both originators are retained; neither question is closed by the coordinator or merged away. Their full original text, evidence and proposed mitigation remain in their proposals.

DES-01 remains a proposed AC2 requirement to preserve the complete owner journey, null history, exact-equality admission, Refresh availability, private-state removal and unchanged UI text. DES-02 remains a proposed requirement to preserve wire keys and legacy-input decoding while explicitly documenting Double output and coordinated deployment. DES-03 remains a proposed precision/error-classification requirement and overlaps the open ARCH-1/P1 question without closing either. DES-04 remains a proposed BIGINT continuity and cache/upload boundary requirement. DES-05 remains a proposed AC3 rule and five-feature documentation update. No designer requirement was discarded; none was voted on or administratively marked resolved.

SEC-01 remains an open precision and fail-closed issuance requirement, including checked addition and rollback without events. SEC-02 remains an open unchanged-BIGINT and explicit wire-compatibility requirement. SEC-03 remains an open authority/privacy requirement: post-lock server clock sampling, exact equality, unchanged private projections, idempotent approval and typed 429 versus 409. SEC-04 remains an open cache/cleanup requirement preserving TTL boundaries, mutex ownership, deletion order and last-good fallback. SEC-05 remains a NONBLOCKING_NOTE recommending the project rule and Architecture Notes updates; it is retained as proposed documentation, not dismissed. No security requirement has originator-backed closure because no voting candidate exists.

The programmer also identifies the dedicated PostgreSQL concurrency gate, whose absence cannot be replaced by SQLite coverage. The designer and architect explicitly distinguish platform compilation from browser/device execution. The architect states all proposed functionality can be covered by automated tests; no manual-only test waiver is requested. These notes remain part of the retained evidence.

## Proposed scope and verification evidence

Participant proposals identify EmailProfile, EmailChangeCooldown, the users cooldown exception, ExposedUsersRepo clocks/mapping/deadlines, EmailChangePolicy's instant parameter, UserEditViewModel state/clock/snapshots, emailChangeDeadlineText, CurrencyRates and OpenExchangeRatesService caches, and TimedTemporalFilesUtilizer first-seen tracking. Existing nullable BIGINT columns would remain explicit boundary encodings. Existing auth timestamps already use DateTime. No schema rewrite, custom serializer, dependency upgrade, UI redesign or monotonic-clock redesign is proposed.

The architect supplies a full provisional ordered plan and test specifications T1–T9 in its proposal. Coverage includes model round-trip and legacy numeric input, null/default preservation, representability helpers, SQLite/PostgreSQL raw storage and concurrency, policy rounding and overflow, typed transport errors, UI equality/privacy/feedback identity, deterministic currency TTL/fallback tests, deterministic temporal-file expiry/cancellation, compilation and documentation checks. Precision tests and the proposed helper remain conditional on ARCH-1. Those sections are retained as authored and have not been promoted to an accepted plan.

The architect's mandatory external research includes Kotlin serializer documentation, KorGE date documentation and Exposed column-transformation guidance. The proposal explains why exact pinned source overrides newer documentation examples, why existing explicit BIGINT mapping is preferred, and why ISO serialization, custom Long serializers and SQL timestamp migrations are rejected. Relevant source links and exact paths are retained in the proposal. No coordinator-authored technical design has been substituted.

## README updates

The architect proposes a root README Architecture Notes rule requiring korlibs.time.DateTime for application instants and dev.inmo.micro_utils.common.DateTimeSerializer for serialized timestamps, distinguishing IDs, counters, durations and explicit boundary encodings. Proposed Architecture Notes updates cover features/email/README.md, features/users/README.md, features/ui/users/README.md, features/currency/README.md and features/files/README.md. The complete proposed wording remains in the architect proposal.

The proposed text explains numeric serialization and coordinated clients, nullable history, unchanged BIGINT boundaries, post-lock sampling, exact deadline equality, typed UI clocks, currency cache timestamps, and temporary-file first-seen instants. Email/users representability text awaits the operator answer. All Operator Notes remain unchanged. No README was edited.

## Completion checks and re-entry

The complete four-role proposal set, exact common packet, fresh-context identities, output allocations, model policy and host retry evidence are present. Roster paths were re-enumerated before publication. No source change or unexpected tracked change was observed. ast-index was present but uninitialized; participants recorded scoped read-only rg fallback rather than generating an index. Historical task records and the prior blocked report remain unchanged.

Unanimous same-candidate voting, accepted implementation-text identity and blocking-comment closure cannot pass because no candidate or voting cycle exists. The outcome is NEEDS_INFORMATION rather than a process failure because the full independent proposal wave completed under root's confirmed interpretation of the approved capacity exception. After the operator supplies the precision policy, a new invocation needs a new common brief and fresh full-roster proposals and votes. No stage transition is selected by this report.
