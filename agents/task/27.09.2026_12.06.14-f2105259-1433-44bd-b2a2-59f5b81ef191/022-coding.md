Model: GPT-6 Codex
Changed files: features/users/common/src/jvmTest/kotlin/repo/EmailLifecycleRegression.kt; agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/022-coding.md

## Result

Implemented ARCH-021-1 in the shared `verifyWarmedCacheFailureMatrix` helper. The new standalone case precedes the existing pending-promotion overflow case. It creates a unique, unapproved first email through the real `CacheUsersRepo`, settles setup work, checks the complete raw row's branch preconditions, and confirms that the created user is present in the warmed cache. Approval with a valid maximum-minus-five instant and a ten-millisecond cooldown must throw `ArithmeticException`. The measured window then requires exactly one clock sample, equality of every raw user row and every cached entry, an unchanged cache-set count, and no backing or cache update events. Existing pending-overflow, exact-maximum success, invalid-sample, and batch cases remain unchanged. No production code, API, schema, dependency, or README changed; the users README and Operator Notes remain byte-for-byte unchanged.

## Verification

After the final source edit, `./gradlew :wishlist.features.users.common:jvmTest --no-parallel --rerun-tasks --no-build-cache` passed: `BUILD SUCCESSFUL in 44s`, with 14 actionable tasks executed. Fresh JUnit XML records 49 tests, zero failures, zero errors, and zero skips. `CacheUsersRepoSqliteTest.invalidClockFailurePreservesWarmedCacheAndWriteCount[jvm]` appears as a passing testcase in a three-test class report.

With `WISHLIST_POSTGRES_TEST_JDBC_URL` set to the isolated loopback database, `./gradlew :wishlist.features.users.common:postgresEmailLifecycleTest --no-parallel --rerun-tasks --no-build-cache` passed separately: `BUILD SUCCESSFUL in 36s`, with 14 actionable tasks executed. Fresh JUnit XML records 13 tests, zero failures, zero errors, and zero skips. `PostgresUsersRepoTest.invalidClockFailurePreservesWarmedCacheAndWriteCount` appears as a passing testcase. Both engine tasks executed; neither was up-to-date, cached, skipped, absent, or failed.

The required `./gradlew :wishlist.features.users.common:build --no-parallel` passed after the final source edit: `BUILD SUCCESSFUL in 19s`, with 425 actionable tasks, seven executed and 418 up-to-date. `git diff --check` passed. The source diff is exactly 30 added lines in the authorized test helper. `ast-index rebuild` completed after the final Kotlin edit and indexed 826 source files.

The first SQLite attempt found a type error in the new cache precondition; changing the assertion to look up the cached map by user ID fixed it before both fresh final gates. A subsequent sandboxed Gradle invocation could not write its existing wrapper lock under the home directory, so the final Gradle gates used narrow cache access. The supplied temporary PostgreSQL endpoint had disappeared. A fresh PostgreSQL 18.6 cluster and database were created solely under `/tmp` and bound to loopback port 55482; the passing PostgreSQL gate used that disposable fixture. These setup failures are not counted as passing regression evidence. No blocker remains and no push was performed.
