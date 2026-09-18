Model: gpt-5.6-terra (ML; Coding priority selects ML before HL).
Changed files:
- features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt
- features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt
- features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/007-coding.md

# Persistence proof follow-up coding report

Audited the direct Exposed writer against Architecture 003. Create, single update, batch update, dedicated email and username changes, approval, and deletion all acquire the durable singleton lock row before reading user lifecycle state. Cross-slot ownership checks operate on raw current and pending columns; batches remain one transaction with post-commit-only events; the injected clock remains sampled only inside the locked transaction; and unique-constraint, lock, and other infrastructure failures continue to retain their original classification rather than becoming duplicate or cooldown feedback.

The audit found one defect: unchanged generic email writes, dedicated same-username writes, and same-address writes returned the retained record but still emitted an update event. A small internal mutation result now distinguishes durable changes from no-ops. Public return values and missing-id semantics are unchanged, while events are emitted only for committed durable mutations. The post-lock callback is a default-disabled repository test seam; it is invoked immediately after the lock-row UPDATE and lets the file-backed tests prove that a second independent repository cannot reach lifecycle work before the first writer releases the durable lock.

The SQLite suite now uses fixture-owned temporary files and two separate Exposed `Database` and repository instances. Latch barriers, not wall-clock sleeps, prove that concurrent current/current and pending/current claims of the same address serialize and leave exactly one committed claimant; a queued unrelated claim completes after the lock is released. A real SQLite `BEGIN IMMEDIATE` contention case remains an `ExposedSQLException` with `SQLITE_BUSY`, not a duplicate or cooldown exception. The lifecycle suite also proves release of pending, current, replacement, clear, and delete claims; zero update events for no-op writers; raw legacy preservation; idempotent singleton lock-row initialization; migration with and without the approval column; approved, unapproved, null-email, and malformed-email legacy rows; null new columns without fabricated history; and reopen preservation of an issued deadline plus pending replacement.

The existing PostgreSQL fixture/task remains safe for the intended external gate: the fixture requires `WISHLIST_POSTGRES_TEST_JDBC_URL`, generates a UUID-derived schema name, and drops only that fixture-created schema. The task reports a clear missing-URL failure before any database access. No developer database was accessed. PostgreSQL execution was attempted and is still externally blocked because `WISHLIST_POSTGRES_TEST_JDBC_URL` is not set; previous environment checks also established that `psql` is unavailable and Docker daemon access is denied even with escalation. The source and SQLite persistence proof are complete; only execution of the disposable PostgreSQL gate remains outstanding.

Verification completed successfully:

- `./gradlew --no-parallel :wishlist.features.users.common:jvmTest` passed with 26 tests.
- `./gradlew --no-parallel :wishlist.features.users.common:build :wishlist.features.email.server:compileKotlinJvm :wishlist.features.auth.server:compileKotlinJvm :wishlist.features.admin.server:compileKotlinJvm` passed.
- `XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast ast-index rebuild` passed after the source changes.
- `./gradlew --no-parallel :wishlist.features.users.common:postgresEmailLifecycleTest` failed as designed with `postgresEmailLifecycleTest requires WISHLIST_POSTGRES_TEST_JDBC_URL for a disposable PostgreSQL database`.

No server route, client, UI, configuration, or feature documentation was changed. The task explicitly limits this follow-up to persistence proof, so the Users README was not altered; its Operator Notes were preserved.
