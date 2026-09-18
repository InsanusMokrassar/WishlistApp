Model: gpt-5.6-terra (ML; Coding priority selects ML before HL).
Changed files:
- features/users/common/build.gradle
- features/users/common/src/jvmMain/kotlin/repo/ExposedUsersRepo.kt
- features/users/common/src/jvmTest/kotlin/repo/CacheUsersRepoSqliteTest.kt
- features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt
- features/users/common/src/jvmTest/kotlin/repo/PostgresUsersRepoTest.kt
- features/users/common/src/jvmTest/kotlin/repo/PostgresUsersTestFixture.kt
- features/users/common/src/jvmTest/kotlin/repo/SqliteUsersTestFixture.kt
- agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/006-coding.md

# Persistence foundation coding report

Replaced the inherited Exposed write bodies with direct outer transactions for create, single and batch update, delete, dedicated email/username mutations, and conditional approval. Each starts by updating the additive singleton `users_write_lock` row. The lock table and id=1 row are created idempotently after the existing additive users-table initialization. Locked mutations use raw current and pending column predicates, preserve missing-id and batch omission behavior, roll back the whole batch on failure, and publish all flow events only after commit. Actual database unique violations retain their classifier and cause; prechecked cross-slot conflicts are plain `DuplicateUserFieldException` values with no invented SQL cause.

The lifecycle implementation samples the injected server clock inside the locked transaction. A state-changing email mutation checks its durable deadline before checking address ownership, permits equality at the deadline, clears the deadline for zero policy, uses checked approval-deadline addition, and leaves replayed approval unchanged without an extra update event. Generic full updates use the same lifecycle path, while dedicated username updates preserve the email state and do not apply cooldown.

Focused real SQLite tests now cover retained approved/pending lifecycle behavior, raw cross-slot conflicts, cache mirroring, exact cooldown/replay behavior with an injected clock, and failed batch rollback with zero speculative events. Existing reopen coverage continues to use an isolated file-backed SQLite database. The PostgreSQL gate is explicit: `postgresEmailLifecycleTest` selects a PostgreSQL suite, requires `WISHLIST_POSTGRES_TEST_JDBC_URL`, creates a random fixture-owned schema, and drops only that generated schema. Ordinary JVM tests exclude this external suite.

`./gradlew --no-parallel :wishlist.features.users.common:jvmTest` passed. `./gradlew --no-parallel :wishlist.features.email.server:compileKotlinJvm :wishlist.features.auth.server:compileKotlinJvm :wishlist.features.admin.server:compileKotlinJvm` passed. `XDG_CACHE_HOME=/tmp/wishlist-email-cooldown-ast ast-index rebuild` passed after source edits; the default ast-index cache location is read-only in this sandbox, so the repository-established writable cache override was used.

`./gradlew --no-parallel :wishlist.features.users.common:postgresEmailLifecycleTest` failed as designed with the exact blocker: `postgresEmailLifecycleTest requires WISHLIST_POSTGRES_TEST_JDBC_URL for a disposable PostgreSQL database`. No developer database was accessed. Local Docker provisioning is also unavailable because `docker info` cannot access `/var/run/docker.sock` (permission denied). Therefore PostgreSQL locking/concurrency execution remains unproven in this environment.

The persistence slice is source-complete for the direct writer and SQLite foundation, but not fully proven: PostgreSQL execution requires the disposable JDBC URL, and the requested independent-repository barrier concurrency proof remains a follow-up test expansion. No server route, client, or UI behavior was added. The Users README was deliberately left unchanged for the later documentation slice; Operator Notes were preserved.
