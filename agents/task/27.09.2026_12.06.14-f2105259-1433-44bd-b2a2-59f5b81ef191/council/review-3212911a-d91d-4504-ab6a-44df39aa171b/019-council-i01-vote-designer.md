Model: gpt-6.1-sol; HL; reasoning effort high; no fallback
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/019-council-i01-vote-designer.md

# Designer vote on frozen candidate i01

Task: 27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191.
Role: designer. Assigned role file: agents/council/roles/DESIGNER.md, supplied through the frozen own contract listed below. Invocation class: council participant. Mode: fresh whole-candidate vote. Cycle: i01. Actual subagent identity: /root/review_preparation_recovery/review_vote_i01_designer. Repository baseline: 1291075 on feat/issue-79-user-email-change, as supplied by the brief. Superseded output: none.

Vote: **AGREE_WITH_NOTES**. I give unconditional final consent to implement the complete unchanged candidate identified below. Neither note requires a candidate edit or additional scope. No blocking designer issue or operator question remains. The vote accepts a design and its proposed verification gates; no application implementation, test execution, production scan or deployment is claimed.

## Exact frozen inputs

Brief: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/009-council-brief.md.

Candidate: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/015-council-i01-candidate.md.

Issues: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/016-council-i01-issues.md.

Own role contract: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/002-contract-designer.md.

Shared instruction inputs read in full:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/005-shared-COMMON.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/006-shared-PROTOCOL.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/007-shared-MODELS.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/008-shared-local.PROTOCOL.md

Completed proposal evidence read in full, supplied equally and treated as evidence rather than votes:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/011-council-proposal-architect.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/012-council-proposal-designer.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/013-council-proposal-programmer.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/014-council-proposal-security.md

Immutable operator/task evidence read:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/REVIEW-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-002.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/PROMPT.md

The complete candidate and issues were read, including the implementation order, reference inventory, all T1–T9 specifications, compatibility and recovery policy, complete five-README delta, alternatives and coverage limits. I adopt the recorded designer history in 012 and its issue groups in 016. No other role contract or current i01 peer vote, review or outcome was read. No unrestricted task/Git history traversal, private sibling communication, persistent memory or further delegation was used. All code navigation used ast-index with the required `env XDG_CACHE_HOME=/tmp/wishlistapp-review-ast-cache` prefix and the supplied code-only index. Only this allocated report was written. These are observed access practices, not a claim of enforced per-participant filesystem isolation.

## Evidence and whole-candidate assessment

I read features/users/README.md, features/email/README.md, features/ui/users/README.md, features/auth/README.md and features/admin/README.md in full before application analysis. Admin Operator Notes require root-only management and reuse of existing functionality for other admin operations. The candidate preserves those requirements: admin exposes no new pending-profile read, and existing mutations continue through the coordinator. Its proposed edits preserve every Operator Notes section.

Direct source checks confirm the current ownership defect and preservation strategy. features/email/common/src/commonMain/kotlin/models/EmailProfile.kt:25 defines one six-field owner snapshot, with both nullable DateTime properties using MicroUtils DateTimeSerializer and constructor range validation. features/email/common/src/commonMain/kotlin/utils/EmailProfileState.kt:13 selects pending replacement before unapproved current; line 20 selects pending before current for the draft baseline. Moving both together to users/common avoids splitting an authoritative snapshot or creating the reverse dependency. The candidate preserves their executable behavior, scalar owner identifier, defaults and wire keys.

features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt:23 has only id and username. Its KDoc explains that anonymous callers see every field and that reverse identity mapping cannot reconstruct lifecycle state. The proposed separate users-feature EmailProfile honors AC1 without broadening the public list. Completed proposal evidence supplies the reduced User/auth/admin descriptor tests, fresh repository projection, cache bypass, lock, cross-slot uniqueness and event semantics; candidate T2, T3, T7 and T8 explicitly preserve those contracts rather than changing persistence algorithms.

features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt:70 derives the GET owner from bearer identity and distinguishes present profile from missing account. Its PUT at line 79 decodes SetEmailRequest and passes only callerId and request.email to the feature. features/email/common/src/commonMain/kotlin/models/SetEmailRequest.kt:14 declares only email. features/email/client/src/commonMain/kotlin/KtorEmailFeature.kt:74 maps only 404 to null and otherwise preserves status/decode failures. Directly inspected KtorEmailFeatureTest.kt:34, :61, :88 and :115 covers populated/empty success, only-404 null, malformed or transport failure, and cancellation. Retaining this transport with a moved return model is feasible and does not add a new owner journey.

features/ui/users/src/commonMain/kotlin/ui/DefaultUsersModel.kt:88 delegates the owner read directly to EmailFeature. UsersModel.kt keeps auth and the public list separate from lifecycle reads. UserEditViewModel.kt:575 constructs feedback identity from current email, current approval, pending email, requested-at and allowed-at; line 588 applies strict-before cooldown, and the imperative admission at line 623 rechecks owner, target, capability, busy state, returned userId and deadline. The candidate keeps all five values and all guards. T6 covers current A, pending B and draft C; uncertain storage without automatic verification; disabled SMTP storage; requested-at-only feedback retirement; equality admission; cancellation; and immediate private-state withdrawal.

Direct inspection of features/ui/users/src/jvmTest/kotlin/ui/UserEditEmailRenderTest.kt:132 confirms meaningful live-retarget coverage: the old ViewModel still contains private email/deadline values while rendered private semantics disappear after the raw navigation target changes. This supports preserving the renderer rather than adding an unrelated interaction redesign. Compilation of changed shared consumers plus existing shared/desktop behavioral tests is proportionate to the relocation; browser announcements, device execution, physical IME and live SMTP remain accurately stated limits.

features/email/common/src/commonMain/kotlin/utils/EmailTimestamp.kt:6 and :15 confirms the existing inclusive ±4503599627370496 finite whole-millisecond policy. Storage conversion validates raw Long before DateTime conversion, and checked deadline arithmetic rejects unsupported results. The candidate retains this rule and the documented counts-only deployment and recovery constraints without migration or inferred history.

I revisited primary research with the native web tool. [W3C WAI user notification guidance](https://www.w3.org/WAI/tutorials/forms/notifications/) supports clear submission outcomes and actionable recovery feedback. My application-specific inference is to retain the existing storage/delivery distinction and Refresh recovery; the guidance does not require new copy or controls for a DTO relocation. [Kotlin SerialName documentation](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) confirms that default class descriptor names reflect qualified Kotlin names and can be overridden. Therefore descriptor identity changes are real; preserving the observed concrete six-field JSON contract does not establish binary or polymorphic descriptor compatibility. Candidate T1/T5 and its explicit limitation address the evidenced direct JSON surface. No evidenced descriptor consumer creates a prerequisite for adding SerialName.

AC1 is satisfied by actual users/common model/helper ownership, separate from User and the public projection. AC2 is addressed across the repository, transport, authorization, candidate lifecycle and visible owner state. AC3 preserves DateTime, serializers, range validation and recovery. AC4 supplies a complete Architecture Notes delta and preserves operator constraints. AC5 supplies concrete implementation order, interfaces, alternatives, risks, primary research, meaningful acceptance-linked tests and honest coverage limits. No proposed migration, endpoint change, reverse dependency or optional UX work is necessary.

## Disposition of every issue group

**C01 — accepted; designer originating concern resolved.** Candidate Task understanding, Concrete model and interfaces, and T1–T5 relocate the complete private projection to users/common while keeping UsersFeatureUser id/username-only and the existing bearer-owner email transport. This directly closes the designer ownership/privacy concern in 012.

**C02 — accepted from the designer perspective.** Retaining the simple name EmailProfile with the explicit users/common package and separate private model is the designer's recorded recommendation. No supplied requirement makes a UsersFeatureEmailProfile spelling necessary. Programmer/security must independently disposition their naming recommendations; this vote does not supply their assent.

**C03 — accepted from the designer perspective.** I accept the candidate's intentional generated descriptor-name change and coordinated rebuild. It preserves concrete JSON keys/defaults and adds literal old-wire compatibility proof. The completed programmer proposal expressly distinguishes its SerialName recommendation from evidence of actual polymorphic use. No observed consumer is lost under the supported contract. This does not close the programmer's issue on the programmer's behalf.

**C04 — accepted; designer originating concern resolved.** T1's old-wire fixture and explicit-default keys prove behavior beyond a repeated import; T2–T8 reuse substantive privacy, lifecycle, consumer and timestamp suites. Cross-target compilation addresses the type/dependency move. No package-spelling-only test is needed or authorized. The preservation specifications cover every executable change proportionately.

**C05 — accepted from the designer perspective.** The unchanged SetEmailRequest input and bearer-derived caller remain separate from the moved output model. T4/T5 preserve narrow mutation bodies and authorization; no changed input-decoding policy or full-profile write is introduced. The hostile-field case remains the security role's recorded proposal, and its originator must decide its disposition. I find no designer acceptance failure requiring that additional case as a prerequisite to this candidate.

**C06 — NONBLOCKING_NOTE; designer confirms optional status.** UsersModel.kt's verification KDoc describes the displayed current address, while the helper and actual behavior prefer pending replacement. The discrepancy predates the ownership correction. Candidate step 3 already includes UsersModel ownership KDoc work and preserves pending-first behavior through T1/T4/T6. My prior observation is an optional documentation accuracy note, not a condition to accepting the unchanged candidate; no additional KDoc prescription or executable journey change is required by this vote.

**C07 — accepted; designer originating concerns resolved.** The complete invariant set is retained: mandatory fresh projection and cache bypass; durable locked cross-slot integrity; no-op timestamp/event preservation; exact approval and stale-link compensation; owner/session/live-target checks; current/pending/draft separation; five-field feedback identity; and exact deadline equality. Candidate Interfaces and T2–T8 include these outcomes and exclude algorithm or storage extraction. Security SEC-01 through SEC-04 are consistent with that preservation strategy; their originator retains authority over security issue closure.

**C08 — accepted; designer originating documentation concern resolved, with a nonblocking factual note.** The complete five-feature Architecture Notes delta establishes ownership and transport separately, keeps Operator Notes unchanged, and retains privacy, lifecycle and timestamp text. Existing Overview/Models statements are explicitly outside the authorized delta. The candidate's Observed evidence paragraph says all five Operator Notes contain only placeholders; direct reading shows that admin contains substantive root/reuse constraints. The concrete candidate respects both constraints, so this evidence-description discrepancy does not require a design edit and does not condition acceptance. No additional documentation scope is proposed.

**C09 — accepted; designer originating timestamp/rollout concern resolved.** The candidate preserves nullable DateTime with MicroUtils serializers, checked integral inclusive ±2^52 milliseconds, raw BIGINT storage, unknown history, fail-closed corruption handling, no automatic repair, counts-only scans, coordinated deployment and backup/recovery requirements. The ownership move supplies no evidence requiring a new range policy. T1/T3/T8 and Compatibility cover the preserved boundary.

**C10 — NONBLOCKING_NOTE confirmed.** No operator question or unautomated changed-functionality waiver is required now. PostgreSQL and platform availability remain unexecuted prerequisites; an unavailable required gate must be reported as unmet rather than passed. The designer's earlier keyboard/screen-reader smoke suggestion was conditional on changing widgets or semantics; the candidate changes neither. Existing browser/device/live SMTP limits do not create a new acceptance exception for this type relocation.

**C11 — NONBLOCKING_NOTE confirmed.** This fresh report records the exact supplied native binding gpt-6.1-sol, HL, high reasoning effort and no fallback. Historical GPT-6 family headers remain immutable and the coordinator's actual binding evidence is retained. The recording mismatch creates no application design condition.

All designer-originating subjects from 012 are adopted and explicitly covered above: ownership and naming in C01/C02; wire/helper tests in C04; verification documentation in C06; journeys, feedback, privacy, persistence and cooldown in C07; documentation in C08; timestamp/recovery policy in C09; and automation/accessibility limits in C10. There are no additional designer-originating issue IDs or unresolved objections in the supplied history.

## Final acceptance and limits

Final vote: **AGREE_WITH_NOTES**, tied exclusively to the exact 009 brief, 015 candidate and 016 issues paths above. Acceptance is unconditional for that unchanged candidate; C06 and the C08 evidence-description observation are nonblocking notes. No candidate edit, optional scope, new operator answer or waiver is requested.

Confidence is high in the ownership boundary, preserved journeys and dependency direction. Actual build/test results, available disposable PostgreSQL infrastructure, platform toolchains and deployment data remain unknown. The candidate correctly requires verification rather than claiming those gates passed. Only the allocated vote report was changed; source, READMEs, Git state and persistent memory were not written. The designer participant returns this report and stops without deriving an aggregate outcome.
