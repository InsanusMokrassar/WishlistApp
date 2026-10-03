Model: GPT-6.1-sol; HL; reasoning effort high; no fallback
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/020-council-i01-vote-programmer.md

# Programmer whole-candidate vote

Task: 27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191. Role: programmer. Invocation class: council participant. Mode: fresh whole-candidate vote. Cycle: i01. Actual subagent identity: /root/review_preparation_recovery/review_vote_i01_programmer. Assigned role file: agents/council/roles/PROGRAMMER.md, read only through the frozen own contract. Repository baseline: revision 1291075 on feat/issue-79-user-email-change, as supplied in the brief. Superseded output: none.

Vote: **AGREE**. I give unconditional final consent to implement the unchanged candidate identified below. No candidate edit is a prerequisite to this vote. This vote accepts a design and its proposed verification contract; no implementation or successful execution is claimed.

## Exact decision inputs

The complete candidate, issues snapshot, shared instructions, own contract and completed proposals were read. The exact packet paths are:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/009-council-brief.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/015-council-i01-candidate.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/016-council-i01-issues.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/005-shared-COMMON.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/006-shared-PROTOCOL.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/007-shared-MODELS.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/008-shared-local.PROTOCOL.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/003-contract-programmer.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/011-council-proposal-architect.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/012-council-proposal-designer.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/013-council-proposal-programmer.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/review-3212911a-d91d-4504-ab6a-44df39aa171b/014-council-proposal-security.md

The immutable operator evidence read directly is:

- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/REVIEW-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-001.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/OPERATOR-ANSWER-002.md
- agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/PROMPT.md

I adopt the programmer issue history in 013 and C01-C04/C07-C11 of 016. Completed proposals provide evidence and alternatives; none supplies a current vote. No current i01 peer vote, review or outcome, peer contract, unrestricted history, ordinary/root instruction, private sibling message or persistent memory was read. Code navigation used only ast-index with `env XDG_CACHE_HOME=/tmp/wishlistapp-review-ast-cache`; the supplied shared index excludes agents and AGENTS.md. No delegation or Git write occurred. Only the allocated report was written.

## Evidence and feasibility assessment

The affected feature READMEs were read in full before source analysis: features/users/README.md, features/email/README.md, features/ui/users/README.md, features/auth/README.md and features/admin/README.md. The actual admin Operator Notes require root access and reuse of existing capabilities. The candidate preserves both. Some non-Architecture model descriptions currently attribute the profile to email/common; the candidate explicitly records that documentation scope limitation rather than authorizing unrelated section edits.

Direct source inspection confirms features/email/common/src/commonMain/kotlin/models/EmailProfile.kt:25 is a standalone serializable six-field class. Both lifecycle properties retain DateTimeSerializer and constructor validation. The class has no User supertype, polymorphic annotation or custom profile serializer. features/email/common/src/commonTest/kotlin/models/EmailProfileTest.kt:20 already proves populated concrete JSON keys and round-trip; its other cases cover defaults, invalid required inputs, pending-first helpers, nullable legacy history and both supported timestamp endpoints. Moving this suite and adding the candidate's literal old-wire fixture and explicit-default/descriptor-field assertions supplies meaningful compatibility proof.

features/email/client/src/commonMain/kotlin/KtorEmailFeature.kt:74 calls `body<EmailProfile>()` on the concrete owner response and converts only 404 to null. features/common/client/src/commonMain/kotlin/configurators/SerializationConfigurator.kt installs the supplied Json through Ktor ContentNegotiation. features/email/server/src/commonMain/kotlin/configurators/EmailRoutingsConfigurator.kt:70 derives the GET owner from the authenticated principal and returns the concrete profile; the PUT decodes only SetEmailRequest and the POST only EmailVerificationRequest. These are evidence for unchanged direct JSON, owner confinement and narrow mutation inputs, rather than evidence of a descriptor-name-based profile protocol.

The full ast-index EmailProfile reference inventory includes production repository/email/UI consumers, distant test fakes and the fully qualified constructor in AuthFeatureServiceSqliteTest. It agrees with the candidate's consumer inventory. Direct inspection also observes qualified old-profile ownership KDoc in features/auth/common/src/commonMain/kotlin/models/AuthFeatureUser.kt and features/admin/common/src/commonMain/kotlin/models/AdminUser.kt. Those references belong to the candidate's existing final completeness check for remaining old production profile references; they do not require an additional feature or behavioral change.

features/users/common/build.gradle already exports email/common, features/email/server/build.gradle already exports users/common, and features/email/client/build.gradle lacks that users dependency. features/email/common/build.gradle contains no users dependency. The proposed email/client commonMain API edge therefore exposes the relocated public return type without a cycle. The existing users/common multiplatform template provides JVM, JS and Android targets; the email/server template remains JVM-only. features/ui/sidebar/build.gradle also uses the JVM/JS/Android template, so the candidate's sidebar JVM regression gate is a real configured target. No version upgrade, source-set redesign or persistence migration is necessary.

features/users/common/build.gradle excludes PostgresUsersRepoTest from ordinary jvmTest and supplies a separate postgresEmailLifecycleTest task whose doFirst fails without a disposable JDBC URL. The candidate accurately treats that gate as mandatory and unexecuted. features/email/common/src/commonMain/kotlin/utils/EmailTimestamp.kt retains checked finite integral DateTime milliseconds within inclusive ±4503599627370496, raw-storage conversion before use, and checked deadline arithmetic. The move provides no reason to alter those boundaries.

I independently reopened the official [Kotlin SerialName documentation](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) and [multiplatform dependency documentation](https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html) on 2026-10-02. The first confirms that the default class descriptor identity follows the fully qualified name and can affect polymorphic or schema-oriented consumers; the second supports placing shared project dependencies in commonMain. The application-specific inference is that descriptor identity changes, but the observed concrete JSON transport does not encode that identity. These documents do not imply a dependency upgrade or establish an external profile descriptor consumer.

## Whole-candidate and originating-issue dispositions

C01: Accept the proposed resolution as resolved from the programmer perspective. Moving the entire private profile and dependent helpers to users/common satisfies AC1 while leaving User identity and public UsersFeatureUser distinct. The existing owner email GET remains suitable transport. A public pending field, duplicate DTO or new endpoint is unnecessary and would increase privacy or coherence risk.

C02: Accept the candidate's retained EmailProfile name and withdraw the programmer recommendation to require UsersFeatureEmailProfile. The module, package, external projection contract and unchanged public-list separation establish ownership. The recorded programmer proposal identified no functional failure caused solely by the shorter name. Retaining the name does not weaken AC1.

C03: Accept the candidate's descriptor treatment and withdraw the programmer recommendation to require the old class-level SerialName. The proposal explicitly distinguished precautionary metadata stabilization from evidence of a real polymorphic consumer. Fresh source inspection supports the candidate's concrete six-field JSON contract, coordinated rebuild and absence of an observed descriptor-name consumer. T1 and T5 prove the supported old-wire surface. I accept the documented source/binary/descriptor identity change and do not require descriptor-name preservation for AC2.

C04: Accept the proposed resolution as resolved from the programmer perspective. T1-T9 specify tests or document checks for every planned model/helper/dependency/reference/documentation change and each acceptance condition, including malformed input, null history, privacy, cancellation, exact expiry, stale approval, duplicate ownership, raw corruption and overflow. Ownership review plus consumer compilation is appropriate; a package-spelling-only executable test would add no behavioral proof. JVM regression execution and configured JS/Android production and changed-test compilation are proportionate to unchanged algorithms and serializers.

C05: Accept the candidate's treatment from the programmer perspective. The observed PUT/POST request classes remain separate from the output projection, and candidate T4/T5 preserve bearer derivation, narrow request keys and prior error behavior. A profile relocation does not add an input field or mass-assignment mechanism. The security-originated hostile-input recommendation remains available for its originator's disposition; this programmer vote does not manufacture that role's assent.

C06: Accept the candidate's treatment from the programmer perspective. UsersModel currently describes a displayed current verification address, while the existing helper selects pending first. The candidate already preserves pending-first helpers and updates relevant ownership documentation; the observed wording does not require a new executable journey or prevent the ownership move. The designer must independently disposition its originating comment.

C07: Accept the proposed resolution as resolved from the programmer perspective. The candidate explicitly retains fresh single-row projection and cache bypass, durable participating-writer locking, cross-slot uniqueness, exact approval, no-op timestamps/events, stale-link compensation, independent identity/profile state, owner/session/live-target confinement, current/pending/draft separation, five-field feedback identity and equality-admitted cooldown. T3-T8 and the retained SQLite/PostgreSQL/client/server/UI suites cover these invariants without replacing repository or UI algorithms.

C08: Accept the proposed resolution as resolved from the programmer perspective. The complete five-feature Architecture Notes delta distinguishes users ownership from email transport, preserves timestamp and recovery claims, and leaves Operator Notes intact, including admin requirements. I accept the explicit non-Architecture scope limitation recorded in the candidate and withdraw any implication that stale Overview/Models wording requires an expanded edit before accepting this candidate. T9 provides the relevant document-consistency review.

C09: Accept the proposed resolution as resolved from the programmer perspective. The nullable korlibs DateTime and MicroUtils serializer contract, integral supported range, checked BIGINT adapters, no historical backfill or repair, counts-only predeployment scan, stopped old writers, coordinated server/UI deployment and compatible recovery remain unchanged. T1/T8 and the repository gates preserve the relevant configured/stored failure behavior. No fresh timestamp policy is needed for this ownership correction.

C10: Confirm the nonblocking note. No currently identified changed functionality lacks an automated route to verification, and no operator handling question is required before design acceptance. PostgreSQL and platform infrastructure are execution prerequisites, not passed evidence. The candidate explicitly requires honest reporting of an unavailable required gate; acceptance does not waive a gate or authorize declaring completion without proof. Existing live SMTP, browser DOM, physical IME and Android device limits do not become new untested behavior because renderer, delivery and admission implementations remain unchanged.

C11: Confirm the nonblocking process note. This report records the supplied native gpt-6.1-sol binding, HL and high reasoning effort accurately, with no fallback. Historical header mismatches stay immutable and do not change the substantive design or replace a fresh independent vote.

All programmer-originated recommendations, alternatives, risks and acceptance-linked checks in 013 are either adopted by the candidate or explicitly dispositioned above. No programmer objection remains open. These dispositions apply only to the exact unchanged brief/candidate/issues packet, and do not close another originator's issue without that originator's vote.

## Acceptance, limitations and completion

AC1 is satisfied by canonical users/common ownership and separate private/public/identity projections. AC2 is satisfied by preserved transport, lifecycle, repository and UI contracts plus T1-T7. AC3 is satisfied by unchanged timestamp policy and T1/T8. AC4 is satisfied by the complete Architecture Notes delta and T9. AC5 is satisfied by the concrete interfaces, ordered file/dependency changes, alternatives, research, risks and proposed multi-platform/regression gates.

Operator questions: none remain for the unchanged candidate. Confidence is high in feasibility and preservation, bounded by unexecuted application gates, unknown disposable-database/toolchain availability and external consumers outside the inspected repository. No production data was queried. No source, README, build configuration, application test, Git state or persistent memory was changed.

Final acceptance: **AGREE** to implement 015-council-i01-candidate.md unchanged under 009-council-brief.md and 016-council-i01-issues.md at the exact paths above. Result: allocated programmer vote complete; return the sole report and stop.
