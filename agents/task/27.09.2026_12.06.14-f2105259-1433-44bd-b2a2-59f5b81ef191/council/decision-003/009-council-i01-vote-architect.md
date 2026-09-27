Model: GPT-6; HL; runtime variant is not exposed; no fallback.
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/decision-003/009-council-i01-vote-architect.md

# Architect vote i01

Task: 27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191. Role: architect. Invocation class: fresh council participant. Agent: /root/pr82_preparation_decision/architect_vote_i01. Cycle: i01. Vote: AGREE.

The exact brief is agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/decision-003/001-council-brief.md. The exact candidate is agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/decision-003/007-council-i01-candidate.md. The exact issues snapshot is agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/council/decision-003/008-council-i01-issues.md. Completed proposals 002-council-proposal-architect.md, 003-council-proposal-designer.md, 004-council-proposal-programmer.md and 005-council-proposal-security.md in that same directory were supplied equally and used as completed evidence. Shared instructions are agents/council/COMMON.md, PROTOCOL.md, local.PROTOCOL.md and MODELS.md; the assigned contract is decision-003/contracts/ARCHITECT.md. No current-cycle peer vote or other role contract was read. The initial attempted shared-directory contract path did not exist; the assigned frozen contract was then read successfully.

## Whole-candidate assessment

I unconditionally consent to implementing the unchanged candidate, including its explicitly overriding amendments. This acceptance does not require an additional design edit. The candidate covers R1 through the recorded freshness evidence, R2 through application DateTime types and direct MicroUtils serialization, and R3 through the project rule and five feature documentation deltas. Duration, ID, money, size, counter and monotonic exclusions are correctly retained. The email/common helper keeps dependency direction consistent; nullable BIGINT remains a storage representation with checked conversion, rather than a competing application instant type.

ARCH-1: I adopt the prior architect concern and explicitly accept its mitigation. The inclusive finite integral ±2^52 domain, raw-Long-before-Double validation, exact checked deadline arithmetic, distinct failure classes, unchanged unsupported rows, no-event rollback, and deployment scan resolve the concern under the operator's delegated compatibility authority. ARCH-1 is RESOLVED from the architect's perspective for this exact candidate. Arbitrary Long identity is intentionally unsupported; the candidate neither silently rounds nor claims equivalence. No originator objection remains. Aggregate consensus remains the coordinator's decision after the complete wave.

The specified rejection policy is preferable to retaining dual canonical representations, replacing the required codec, clipping deadlines, clearing history, or accepting an irregular wider range. The read-only preflight and coordinated matching client/server deployment make the compatibility change operationally explicit. Existing backup/backport recovery and stopped-writer requirements are preserved; retaining BIGINT alone is correctly not treated as permission for arbitrary binary rollback.

## Evidence and research

Direct repository inspection used the current unchanged source paths named in the candidate: EmailProfile.kt, EmailChangeCooldown.kt, CurrencyRates.kt, ExposedUsersRepo.kt approval and mutation sections, EmailChangePolicy.kt, EmailChangeDeadlineText.kt, OpenExchangeRatesService.kt and TimedTemporalFilesUtilizer.kt. The models currently contain the named Long instants. Approval currently maps the lifecycle profile inside its locked transaction and performs Math.addExact before updating. Mutation samples after the durable lock and rejects only strictly before the deadline. Currency caches use separate mutexes and successful-fetch timestamps. File tracking overwrites repeated notifications and removes a map entry before disk deletion. These observations support the candidate's preservation requirements.

I reviewed README.md and the affected email, users, UI/users, currency and files READMEs, including Operator Notes; linked ordinary instruction files were not followed. The candidate's documentation deltas preserve the existing ownership, privacy, rollout and recovery constraints. features/users/common/build.gradle independently confirms that ordinary jvmTest excludes PostgresUsersRepoTest and the dedicated postgresEmailLifecycleTest requires WISHLIST_POSTGRES_TEST_JDBC_URL. Ordinary JVM success cannot stand in for that gate.

Fresh native internet research checked [RFC 8259 section 6](https://www.rfc-editor.org/rfc/rfc8259.html) and [PostgreSQL 16 numeric types](https://www.postgresql.org/docs/16/datatype-numeric.html). The former supports explicit numeric interoperability bounds; the latter confirms BIGINT's broader integer domain. Neither source mandates this application's narrower range: the candidate's ±2^52 choice is an application policy derived from the pinned library contract and delegated decision. I accept the candidate's completed exact-version MicroUtils/korlibs investigation rather than substituting a latest-version assumption. Research is complete for this design.

Additional read-only pinned-source inspection used /home/aleksey/.gradle/caches/modules-2/files-2.1/com.soywiz.korge/korlibs-time/5.4.0/61d4352742d68e8926840fe0a8e4acc2fa89bbc6/korlibs-time-5.4.0-sources.jar. jvmAndroidMain/korlibs/time/Time.internal.jvm.kt obtains the ordinary JVM clock from System.currentTimeMillis().toDouble(); thus the unchanged default DateTime.now() in the JVM-only startup policy does not introduce fractional samples. commonMain/korlibs/time/PatternDateFormat.kt formats yyyy using yearInt.padded(4), unlike yy/yyy modulo formatting. This supports feasibility of faithful expanded-year output, while the candidate's endpoint tests remain necessary proof. No application test was executed.

## Amendment dispositions

I accept the programmer/security production-clock amendment. Normalization is confined to default production adapters; validation of injected or persisted values remains strict. Its separate adapter-resolution tests prevent accidental normalization from weakening domain rejection.

I accept the error-class amendment. Computed unrepresentable deadlines use ArithmeticException; invalid supplied DateTime uses IllegalArgumentException; persisted corruption uses IllegalStateException; startup policy failures remain IllegalArgumentException. The valid setup followed by maximum-minus-five plus ten regression preserves the former transactional invariant without a setup failure obscuring the tested operation.

I accept the decoded-wire amendment and P3 mitigation. The unmodified Double codec cannot promise arbitrary JSON lexeme precision. Validating the decoded value, documenting legacy integer input and Double output, demonstrating strict old-reader incompatibility, and coordinating versions accurately describe the boundary. No custom serializer is necessary.

I accept the designer's endpoint formatter amendment. Both admitted endpoints must render faithful signed or expanded UTC years; ordinary output stays unchanged. The requirement is automatically testable through the shared formatter and does not require a new widget or platform UI redesign.

I accept the P2 infrastructure amendment. PostgreSQL must be provisioned or supplied before a passing execution claim; otherwise execution is blocked and reported. This is a test-environment dependency, not functionality lacking automated coverage. Browser/device host behavior is unchanged and neither compiles nor desktop tests are represented as browser/device execution.

I accept the security/programmer counts-only preflight amendment: per-column counts plus a combined affected-row count suffice to decide whether deployment can proceed. Restricted row identifiers are unnecessary to this gate because remediation requires a separate authorized task. DES-PREFLIGHT remains subject to its designer originator's disposition; I do not close that role's issue.

I also accept the retained DES-01 through DES-05 and SEC-01 through SEC-05 requirements: owner confinement, null history, equality and Refresh, feedback identity, unchanged keys/storage, typed 429 versus 409, lock-time sampling, idempotence, cache TTL/fallback, file deletion/cancellation and documentation. P1 has the same sound substantive mitigation as ARCH-1, while programmer ownership of its closure remains intact.

## Automated coverage assessment

I adopt the entire candidate's ordered implementation and automated verification contract without alteration. Every planned changed function, model or state family has a specified automated oracle:

- EmailTimestampTest covers all four helpers with epoch/normal/negative/bounds, raw out-of-range Long values, fractional/nonfinite rejection, exact storage round trips, negative/zero/huge durations, exact maximum and overflow, including the amended error classes.
- EmailProfile, EmailChangeCooldown and CurrencyRates tests cover construction/copy, nullable defaults, required fields, legacy integer and Double decoding, stable keys, numeric output and boundary identity; email invalid inputs fail. Currency rateOf and numeric conversion expectations remain unchanged. Strict old-reader incompatibility and production adapter resolution are separately specified.
- SQLite and dedicated PostgreSQL suites cover creation, replacement, clear, promotion, replay, null history, raw preservation, invalid stored columns, invalid runtime samples, exact equality, checked overflow, no events/cache publication, batch rollback, reduced reads/username updates, post-lock sampling and concurrent connections. Setup uses valid clocks before injecting failures.
- Policy tests cover zero/omission, fractional-duration ceiling, negative/infinite/huge durations, invalid clocks and boundary deadlines with startup classification. Client/server/admin route tests cover typed legacy/new 429, malformed body rejection, unchanged 409, owner authorization, empty 200, missing 404 and private projections.
- ViewModel, shared formatter and desktop renderer tests cover before/equality/after admission, Refresh, IME, feedback snapshot changes, null history, draft preservation, owner loss/retarget cancellation, UTC ordinary and extreme output and private semantics removal. Changed common code is compiled for affected targets; common serializer tests run on configured JVM/JS targets.
- Ktor MockEngine plus mutable DateTime clock covers both currency caches: disabled/initial/hit/equality/expired paths, independent ages, success-only timestamps, concurrent fetch sharing, failures and unchanged stale fallback, sorting and backward-clock behavior.
- Coroutine test scheduler, mutable clock and temporary-file fixtures cover temporal collector/sweeper start, before/equality expiry, map removal and deletion, repeated IDs, finalized files and returned/parent-job cancellation without real sleeps.
- Boundary/invalid SQL fixtures verify the read-only preflight predicate. Documentation comparison verifies all six deltas, corrected model/KDoc statements, preserved Operator Notes and the final semantic Long inventory. Compiler-guided call-site propagation catches signatures outside the central files.

No planned functionality lacks an automated specification. No manual-only waiver or operator handling decision is needed. Actual execution results remain unclaimed, and unavailable mandatory infrastructure must remain a reported blocker.

## README updates

I accept the candidate's complete README updates section unchanged for README.md, features/email/README.md, features/users/README.md, features/ui/users/README.md, features/currency/README.md and features/files/README.md. The project rule, checked email domain, direct serializer, Double wire compatibility, nullable BIGINT preservation, private owner state, UTC formatting, DateTime cache/tracking state, unchanged durations and deployment/recovery boundaries are all present. Operator Notes remain verbatim; conflicting model descriptions and currency clock wording are corrected. No additional documentation prerequisite is proposed.

No operator question remains. Confidence is high in design feasibility and coverage; implementation and test success are intentionally unclaimed. Only this allocated vote was written. Vote: AGREE, unconditional final consent to the exact candidate i01.
