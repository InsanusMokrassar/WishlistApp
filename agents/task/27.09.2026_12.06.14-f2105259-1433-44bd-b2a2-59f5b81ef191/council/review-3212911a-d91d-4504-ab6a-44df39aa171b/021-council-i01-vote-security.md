Model: gpt-6.1-sol; HL; reasoning effort high; no fallback
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/021-council-i01-vote-security.md

# Security whole-candidate vote i01

Task: 27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191.
Role: security. Invocation class: council participant. Mode: fresh whole-candidate vote. Cycle: i01.
Actual subagent identity: /root/review_preparation_recovery/review_vote_i01_security.
Assigned role file: agents/council/roles/SECURITY.md, read only through the frozen own contract identified below.
Repository baseline: revision 1291075, branch feat/issue-79-user-email-change, as supplied in the frozen brief.
Superseded output: none. Vote: AGREE.

I unconditionally accept implementation of the exact unchanged candidate identified below. No prerequisite candidate edit, new test, naming change, scope expansion, or operator answer is attached to acceptance. The vote is a design judgment; application implementation, tests, production scans, and deployment have not been executed by this participant.

## Exact frozen inputs and isolation

Brief: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/009-council-brief.md.

Candidate: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/015-council-i01-candidate.md.

Issues: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/016-council-i01-issues.md.

Own contract: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/004-contract-security.md.

Shared instructions read in full:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/005-shared-COMMON.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/006-shared-PROTOCOL.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/007-shared-MODELS.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/008-shared-local.PROTOCOL.md.

Completed proposal evidence read in full, with no proposal treated as a vote:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/011-council-proposal-architect.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/012-council-proposal-designer.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/013-council-proposal-programmer.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/014-council-proposal-security.md.

Immutable operator/task evidence read directly:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/REVIEW-001.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-001.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-002.md.
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/PROMPT.md.

I read the complete candidate and issues and adopted the security role's recorded proposal history, including SEC-01 through SEC-04, the explicit-name recommendation, and the proposed hostile lifecycle-field PUT case. Current i01 peer outputs, peer contracts, ordinary/root instructions, unrestricted task/Git history, private sibling messages, and persistent agent memory were not read. The supplied local batching override applies without waiving independent votes or packet isolation. Code navigation used only ast-index, every invocation prefixed with `env XDG_CACHE_HOME=/tmp/wishlistapp-review-ast-cache`, against the supplied code-only index excluding agents and AGENTS.md. The report records observed instruction-based isolation, not enforced filesystem isolation. Only the allocated report was written; no delegation or Git writes occurred.

## Whole-candidate assessment and independent evidence

The candidate satisfies AC1 by relocating the complete canonical owner profile and its receiver-dependent helpers into users/common and removing the original email/common declarations. User identity and the anonymous users list remain distinct reduced models. The preserved authenticated email route can transport a users-owned model without making the public list private or adding another endpoint. AC2 is covered by unchanged method semantics, route guards, narrow request DTOs, locked lifecycle operations, fresh projection, cache bypass, fail-closed transport errors, and owner-confined UI state. AC3 retains the current DateTime/MicroUtils contract and established checked range rather than requiring a timestamp redesign. AC4 supplies concrete Architecture Notes deltas with Operator Notes preserved. AC5 supplies ordered files/interfaces, alternatives, primary research, risks, acceptance-linked T1-T9 specifications, and explicit execution limits.

Affected feature READMEs were read in full before repository analysis: features/users/README.md, features/email/README.md, features/ui/users/README.md, features/auth/README.md, and features/admin/README.md. Admin Operator Notes explicitly require root management and reuse of existing capabilities; the candidate adds no admin capability and preserves the root guard and coordinator. The candidate's observed-evidence sentence describing every Operator Notes section as only a placeholder is imprecise for admin, but the planned behavior and unchanged-Operator-Notes requirement respect the actual operator constraints. The imprecision is nonblocking and requires no candidate edit for acceptance.

Directly inspected source evidence at the supplied revision:

- features/users/common/src/commonMain/kotlin/models/User.kt:18-61 defines User with username/current email, NewUser with username/email only, and RegisteredUser with id/username/current email/current approval. Pending and lifecycle timestamps are absent. features/users/common/src/commonMain/kotlin/models/UsersFeatureUser.kt:11-25 explicitly protects the unauthenticated listing from private email data and declares only id/username. The required ownership fix cannot place pending state in that public projection.
- features/email/common/src/commonMain/kotlin/models/EmailProfile.kt:25-37 declares the six-field owner snapshot, explicit DateTimeSerializer annotations, defaults, and timestamp validation. Moving the intact class preserves data meaning; no User reconstruction can recover omitted pending state.
- features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt:57-100 authenticates GET/PUT/verification, derives caller identity before service access, decodes PUT specifically as SetEmailRequest, and passes only callerId plus request.email. features/email/common/src/commonMain/kotlin/models/SetEmailRequest.kt:14 declares only email. The full profile is never the input DTO. POST similarly accepts only EmailVerificationRequest.expectedEmail. The candidate changes routing KDoc, not these executable bodies.
- features/email/server/src/commonTest/kotlin/configurators/EmailRoutingsConfiguratorTest.kt:109-150 covers missing/invalid bearer rejection and GET owner confinement despite hostile query selectors. At :174-213, PUT bearer rejection and forged body selectors are covered. At :274-287, malformed/invalid bodies do not call the feature. At :306-360, verification caller confinement and the explicit tolerant Json route fixture are preserved. These observations support reuse of existing negative coverage; no hostile lifecycle-field test execution is claimed.
- features/users/common/src/commonMain/kotlin/repo/ReadUsersRepo.kt:19-30 makes the complete fresh projection mandatory and explicitly forbids fabricated cache/identity reconstruction. CacheUsersRepo.kt:60-63 delegates directly. features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt:129-153 separately maps identity and all six owner fields, and :212-215 performs one fresh users-row select.
- ExposedUsersRepo.kt:336-374 locks before exact-candidate approval, promotes pending or unapproved current only on equality, clears candidate history, and emits only after commit for durable changes. At :407-465, changing mutations validate stored timestamps and one clock sample, enforce strict-before cooldown, check cross-slot ownership, retain approved current, and distinguish no-op state. At :477-515, raw timestamp validation, durable singleton lock, and both-slot ownership checks remain explicit. The candidate preserves these bodies and T3/T8 negative regressions.
- features/email/client/src/commonMain/kotlin/KtorEmailFeature.kt:74-85 maps only GET 404 to null and propagates other errors; :108-118 sends SetEmailRequest rather than the profile. Relocation therefore need not alter absence/error handling or write shape.
- features/email/common/src/commonMain/kotlin/utils/EmailTimestamp.kt:6-63 establishes inclusive ±4503599627370496 whole milliseconds, checked raw BIGINT conversion, finite/integral validation, and checked deadline arithmetic. The candidate retains validators and raw storage unchanged.
- features/users/common/build.gradle already exports email/common, features/email/server/build.gradle already exports users/common, and features/email/client/build.gradle needs the planned exported users/common edge. The resulting dependency chain is acyclic. users/common's build explicitly excludes PostgreSQL cases from ordinary jvmTest and fails postgresEmailLifecycleTest when the disposable JDBC URL is absent.

The completed security/designer proposals also supply relevant ViewModel and desktop renderer evidence for all-five-field feedback, wrong-owner rejection, logout/live-retarget invalidation, current/pending/draft distinction, and synchronous deadline admission. I accept that evidence as completed supplied research, not a new test result or peer vote. Candidate T6 retains the demanding shared/desktop scenarios while excluding renderer changes.

Native primary-source research was checked again on 2026-10-02. OWASP recommends dedicated input DTOs containing only editable properties; the applicable inference is to retain SetEmailRequest/NewUser and keep the owner profile read-only. [OWASP Mass Assignment Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html). OWASP's authorization guidance supports least privilege and request-level enforcement; the applicable inference is to preserve bearer-derived owner scope and the restricted anonymous list. [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html). Neither source mandates a particular Kotlin class name or unrelated authentication work.

## Disposition of every frozen issue group

C01: RESOLVED from the security originator's perspective. I accept the complete users/common model/helper relocation, separate User hierarchy, unchanged bearer-owner transport, and id/username-only public projection. Candidate model/interfaces and T1-T5 implement the ownership correction while preserving SEC-01 privacy.

C02: WITHDRAWN_BY_ORIGINATOR for the security preference for the UsersFeatureEmailProfile simple name. I accept EmailProfile in dev.inmo.wishlist.features.users.common.models as the sole canonical model. Module/package, output-only role, and explicit KDoc establish the required ownership; no evidence identifies a security failure caused by retaining the simple name. The candidate's rejection rationale is sufficient and no rename is a prerequisite.

C03: Accepted candidate treatment; no security objection. The moved concrete profile changes generated serializer descriptor identity, and Kotlin source/binary consumers must rebuild. The observed route transports concrete six-field JSON rather than a polymorphic discriminator. Candidate T1/T5 preserve the supported direct wire surface with a literal old body and exact fields/defaults; the candidate explicitly records external-consumer uncertainty. Preserving an unobserved descriptor consumer is not a demonstrated security requirement. The programmer originator retains authority over the programmer disposition.

C04: RESOLVED from the security originator's perspective. T1-T9 provide proportional model/wire/privacy/fresh-read/lifecycle/timestamp/UI/compiler coverage. The explicit relocated model reference is part of a meaningful old-wire compatibility fixture, not a package-spelling-only test. Preserved behavioral suites and the PostgreSQL gate cover changed fixtures and important invariant boundaries without import-mirroring tests.

C05: WITHDRAWN_BY_ORIGINATOR for the proposed additional hostile PUT containing pendingEmail, emailApproved, requested-at, and allowed-at. I accept the unchanged candidate without requiring that new case. Direct evidence shows PUT decodes the unchanged single-field SetEmailRequest and invokes a method accepting only bearer-derived owner and Email; the relocated profile remains output-only. Existing forged-selector, bearer, malformed-input and lifecycle tests plus candidate T4/T5 cover preservation of that narrow boundary. The earlier test recommendation did not establish an affected executable binding or concrete regression unique to relocation. SEC-02 remains required and is accepted; withdrawal concerns only the extra test prescription.

C06: Accepted candidate treatment; no security objection. Pending-first helper behavior is retained and T1/T4 preserves exact recipients. The ownership KDoc work is adequate for this correction; the designer's separate wording observation does not create a security prerequisite. The designer originator determines the designer disposition.

C07: RESOLVED from the security originator's perspective. I explicitly accept preservation of the complete invariant set: mandatory fresh single-row projection and cache bypass; durable lock and current/pending uniqueness; no-op timestamp/event preservation; exact-candidate approval and stale-link compensation; owner/session/live-target confinement; current/pending/draft separation; all-five-field feedback; and strict-before/equality-allowed cooldown. Candidate evidence/interfaces, T2-T8, and recovery sections retain SEC-01 through SEC-04 without migration, table extraction, new endpoint, or optional UX.

C08: RESOLVED from the security originator's perspective. The complete five-feature Architecture Notes delta clearly distinguishes users model ownership from email transport/service ownership, preserves root/admin privacy and all Operator Notes, and excludes unrelated prose. Existing non-Architecture ownership wording is an acknowledged scope limitation, not permission to widen edits. The admin Operator Notes imprecision noted above does not alter the respected constraints or require a plan change.

C09: RESOLVED from the security originator's perspective. DateTime/MicroUtils serializers, inclusive checked ±2^52 integral-millisecond range, raw nullable BIGINT columns, unknown-history nulls, fail-closed stored/configured handling, counts-only scan, coordinated writers/server/UI rollout, and compatible backup recovery remain unchanged. No rounding, automatic repair, mailbox/row disclosure, or range redesign is authorized. SEC-04 is fully retained.

C10: NONBLOCKING_NOTE confirmed. Disposable PostgreSQL and platform toolchain availability are unexecuted verification prerequisites; an unavailable gate cannot be recorded as passed. The candidate introduces no browser/device/physical IME/live SMTP behavior change that requires a new manual waiver. No operator question is currently required, and no unavailable-gate waiver is presumed.

C11: NONBLOCKING_NOTE confirmed. This fresh vote accurately reports the supplied native gpt-6.1-sol HL binding with high reasoning effort and no fallback. Historical proposal headers remain immutable; their broad GPT-6 family labels do not supply model evidence for this vote or create a source-design condition.

## Explicit disposition of the security-origin history

SEC-01: RESOLVED. The canonical private users-owned EmailProfile is distinct from public UsersFeatureUser; neither pending email nor lifecycle timestamps enter anonymous output. Candidate model/interfaces, T2/T7 and the users/auth/admin README delta preserve the privacy boundary.

SEC-02: RESOLVED. SetEmailRequest/NewUser remain narrow inputs, approval and timestamps remain repository-owned, owner identity is bearer-derived, and exact-candidate approval stays unchanged. Candidate T2/T4/T5 and direct route/DTO evidence support acceptance. C05 explicitly withdraws the additional hostile lifecycle-field test, not the invariant.

SEC-03: RESOLVED. Mandatory complete fresh reads, direct cache bypass, exact owner snapshot validation, existing empty-versus-absent distinction, and fail-closed decode are retained in the interfaces and T3-T6. No reduced identity or fabricated empty profile substitutes for lifecycle state.

SEC-04: RESOLVED. Locked mutation ordering, checked clock/stored instants, cross-slot ownership, no-op behavior, post-commit events, counts-only reporting, and rollout/recovery remain unchanged and receive T3/T8 regression specifications.

The security proposal's implementation alternatives are accepted as rejected in the candidate: retaining email/common ownership, adding private state to public/users/auth/admin identity, duplicate DTO mapping, reverse dependency alias, physical storage move, new endpoint, and unrelated timestamp/security/UX work. The retained-name alternative supersedes my prior naming preference as dispositioned in C02. The security proposal's T1-T6 subjects are covered by candidate T1-T9: model/helper/serialization, public privacy, transport/services, persistence/cache, UI/transitive compilation, and documentation/build integration. C05 is the only withdrawn extra case. Existing residual risks—unsupported old/direct writers, process-local role coordination, no resend rate limit, uncertain Boolean PUT responses, and unobservable same-owner credential epochs—remain scope limits rather than new objections to this correction.

## Completion

Operator questions: none remain for the frozen ownership correction and preserved timestamp policy. Known unknowns are unexecuted tests/platform gates, disposable PostgreSQL availability, production scan counts, and external consumers beyond inspected evidence. Confidence is high in the focused design and security preservation, bounded by implementation and execution results not yet available.

Final vote: AGREE to the exact unchanged brief/candidate/issues packet identified above. All security-origin issues have an explicit accepted mitigation or withdrawal; no blocking security objection remains. Acceptance does not decide another role's disposition or the aggregate council outcome. Only this allocated report was written, and the participant returns the report and stops.
