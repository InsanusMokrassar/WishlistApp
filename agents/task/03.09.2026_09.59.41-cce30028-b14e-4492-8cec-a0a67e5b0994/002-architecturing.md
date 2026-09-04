Model: OpenAI GPT-5 (HL)
Changed files: agents/task/03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994/002-architecturing.md

Model rationale: The Architecture role prefers an HL model. The implementation is narrow, but correctness depends on JDBC exception graphs, identity semantics, Xerial extended result codes, two resolved Exposed versions, and leak-free shared-memory database fixtures.

# Architecturing report

## Scope and outcome

Fix finding 4 only at `ExposedUsersRepo`'s existing error-classification boundary. Preserve PostgreSQL SQL state `23505`, add only Xerial's exact UNIQUE and PRIMARY KEY extended result codes, retain both public repository APIs and exception types, and reject every broader SQLite constraint heuristic. Add isolated classifier tests and real Exposed/Xerial repository regressions for create and update.

Every planned behavior is automatable. Users Operator Notes are empty and remain untouched. No schema, route, service, configuration, or dependency-version change is required.

## Process note

The Architecturing agent briefly spawned a read-only dependency-inspection helper, contrary to `agents/ORCHESTRATOR.md`'s prohibition on nested subagents. The root interrupted that helper before completion. A subsequent `git status --short` showed only the task's pre-existing untracked `PROMPT.md`; the helper edited no workspace file. All API validation recorded below was completed directly, and no further delegation occurred.

## Resolved API and dependency facts

The Users JVM test compile classpath currently resolves `org.jetbrains.exposed:exposed-jdbc:1.3.0` through MicroUtils 0.30.1 and contains no Xerial driver. The server runtime resolves Exposed 1.4.0 by conflict resolution and already resolves `org.xerial:sqlite-jdbc:3.53.4.0`. The version catalog alias `libs.xerial.sql` points to that exact Xerial version.

Both resolved Exposed 1.3.0 and 1.4.0 have the same relevant APIs and behavior. `BlockingExecutable.executeIn` catches the driver's `SQLException` and constructs `ExposedSQLException(cause, contexts, transaction)`. `ExposedSQLException` keeps the driver exception as its direct `cause`, mirrors only that immediate exception's SQL state and ordinary JDBC vendor code, and does not expose Xerial's extended result enum. Both versions expose `org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.closeAndUnregister(Database)`.

Xerial 3.53.4.0 exposes `org.sqlite.SQLiteException(String, SQLiteErrorCode)` and `SQLiteException.resultCode: SQLiteErrorCode`. Its constructor passes `resultCode.code and 0xff` to `SQLException`, so UNIQUE 2067, PRIMARYKEY 1555, NOTNULL 1299, CHECK 275, FOREIGNKEY 787, ROWID 2579, and base CONSTRAINT 19 all present as ordinary JDBC `errorCode == 19`. Exact `resultCode` inspection is therefore mandatory; `errorCode`, base code 19, SQL message text, and `SQLITE_CONSTRAINT` are unsafe classifiers.

## Dependency boundary

Add only this private JVM implementation dependency to `features/users/common/build.gradle`:

```groovy
kotlin {
    sourceSets {
        commonMain {
            // Existing dependencies stay unchanged.
        }
        jvmMain {
            dependencies {
                implementation libs.xerial.sql
            }
        }
    }
}
```

`jvmMain` is the minimal correct scope because production classifier bytecode references Xerial classes while no public signature exposes them. The associated `jvmTest` compilation receives `jvmMain` implementation dependencies, so no duplicate `jvmTest` declaration is needed. Keep the server's existing explicit dependency and do not add or align an Exposed version in this module.

## Exact classifier design

Keep `internal fun SQLException.isUniqueViolation(): Boolean` in `ExposedUsersRepo.kt`. Add imports for `org.sqlite.SQLiteErrorCode`, `org.sqlite.SQLiteException`, `java.util.ArrayDeque`, `java.util.Collections`, and `java.util.IdentityHashMap`. Replace only the root SQL-state comparison with this iterative breadth-first graph traversal:

```kotlin
internal fun SQLException.isUniqueViolation(): Boolean {
    val pending = ArrayDeque<Throwable>()
    val visited = Collections.newSetFromMap(
        IdentityHashMap<Throwable, Boolean>()
    )
    pending.addLast(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (!visited.add(current)) continue

        if (current is SQLException) {
            if (current.sqlState == "23505") return true
            if (current is SQLiteException) {
                when (current.resultCode) {
                    SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE,
                    SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY -> return true
                    else -> Unit
                }
            }
        }

        current.cause?.let(pending::addLast)
        if (current is SQLException) {
            current.nextException?.let(pending::addLast)
        }
    }

    return false
}
```

The queue order is deliberately deterministic: inspect the current node, enqueue `cause`, then enqueue `nextException`. Every throwable cause is traversed; the next edge exists only for `SQLException`. Marking on removal is safe because visited nodes enqueue no further edges. `Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())` deduplicates object identities rather than overridable `equals`, so cause/next cross-cycles terminate without hiding distinct equality-colliding exceptions. `ArrayDeque` avoids recursive stack growth.

Check PostgreSQL state on every encountered `SQLException`, not only the outer Exposed wrapper. Check SQLite only by concrete `SQLiteException.resultCode`. Do not inspect numeric `errorCode`, messages, class-name strings, or masked/base constraint values.

Leave both `ExposedUsersRepo.create` and `update` catch blocks byte-for-byte equivalent to their current form. A positive match must still throw `DuplicateUserFieldException(cause = e)`, retaining the exact outer `ExposedSQLException` as cause. A negative match must still execute `else throw e`, rethrowing that same outer instance unchanged.

## Synthetic classifier regressions

Extend `IsUniqueViolationTest.kt` with direct imports for `SQLiteErrorCode` and `SQLiteException`. Keep all three existing tests, then add these cases:

- Direct Xerial `SQLITE_CONSTRAINT_UNIQUE` and `SQLITE_CONSTRAINT_PRIMARYKEY` each return true.
- PostgreSQL `23505` below a generic `IllegalStateException` cause wrapper returns true.
- Xerial UNIQUE below a generic cause wrapper returns true.
- Xerial PRIMARYKEY reached only through `SQLException.nextException` returns true.
- Base `SQLITE_CONSTRAINT`, NOTNULL, CHECK, FOREIGNKEY, and ROWID each return false.
- `SQLException("UNIQUE constraint failed", null, 19)` returns false, proving both message and ordinary code 19 are ignored.
- `SQLiteException("UNIQUE constraint failed", SQLITE_CONSTRAINT)` returns false, proving a convincing message cannot broaden the base enum.

Use this equality-collision fixture and graph to distinguish identity visitation from a normal equality-based set:

```kotlin
private class EqualityCollidingSQLException(sqlState: String?) :
    SQLException("synthetic", sqlState) {
    override fun equals(other: Any?): Boolean = other is EqualityCollidingSQLException
    override fun hashCode(): Int = 1
}

@Test
fun visitsDistinctExceptionsThatCompareEqual() {
    val root = SQLException("root")
    val negativeCause = EqualityCollidingSQLException("23503")
    val positiveNext = EqualityCollidingSQLException("23505")
    root.initCause(negativeCause)
    root.setNextException(positiveNext)

    assertTrue(root.isUniqueViolation())
}
```

The classifier's cause-before-next queue order visits the negative equal node first. A regular `HashSet` would then suppress the positive node and fail this test; the identity set visits both.

Use a cross-edge cycle rather than mutating an already cyclic `SQLException.nextException` chain:

```kotlin
@Test
fun terminatesForCauseAndNextExceptionCycle() {
    val root = SQLException("root", "23503")
    val bridge = SQLException("bridge", "23503")
    root.initCause(bridge)
    bridge.setNextException(root)

    assertFalse(root.isUniqueViolation())
}
```

The single `setNextException` call targets an empty next pointer, so JDK chain-appending logic cannot loop during fixture construction. The resulting graph cycle is `root.cause -> bridge.nextException -> root`, and a false result proves traversal termination.

## Shared-memory SQLite repository fixture

Add `features/users/common/src/jvmTest/kotlin/repo/ExposedUsersRepoSqliteTest.kt`. Use a distinct URI name per test and hold one JDBC keeper connection open while Exposed opens and closes its own connections:

```kotlin
private suspend fun withSqliteRepo(
    block: suspend (ExposedUsersRepo) -> Unit,
) {
    val url = "jdbc:sqlite:file:users-${UUID.randomUUID()}?mode=memory&cache=shared"
    val database = Database.connect(url = url, driver = "org.sqlite.JDBC")
    val keeper = try {
        DriverManager.getConnection(url)
    } catch (error: Throwable) {
        TransactionManager.closeAndUnregister(database)
        throw error
    }

    try {
        block(ExposedUsersRepo(database))
    } finally {
        try {
            TransactionManager.closeAndUnregister(database)
        } finally {
            keeper.close()
        }
    }
}
```

Required imports are `org.jetbrains.exposed.v1.jdbc.Database`, `org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager`, `java.sql.DriverManager`, and `java.util.UUID`. `Database.connect` loads `org.sqlite.JDBC`; the keeper then establishes the named shared-memory database before `ExposedUsersRepo` initializes its table. Explicit repository database arguments isolate all Exposed transactions. The nested cleanup guarantees database unregistration and keeper closure on success, assertion failure, or setup/body exception. Closing the final keeper destroys the unique in-memory database, so no file or table cleanup is required.

## Real repository regression specifications

Each test uses `runTest { withSqliteRepo { repo -> ... } }` and imports the existing `dev.inmo.micro_utils.repos.create` convenience overload.

1. `createDuplicateUsernameMapsToDuplicateUserFieldException`: create one user, attempt a second user with the same username and a distinct email, assert `DuplicateUserFieldException`, assert its cause is `ExposedSQLException`, assert that cause's direct cause is `SQLiteException` with `SQLITE_CONSTRAINT_UNIQUE`, and assert `repo.getAll()` still equals the single seeded row.
2. `createDuplicateEmailMapsToDuplicateUserFieldException`: repeat with distinct usernames and the same non-null email; assert the same exception/cause chain and unchanged single-row state.
3. `updateDuplicateUsernameMapsToDuplicateUserFieldException`: seed two users, update the second to the first username while retaining the second email, assert the same exception/cause chain, and assert the complete two-row map equals the pre-update map.
4. `updateDuplicateEmailMapsToDuplicateUserFieldException`: seed two users, update the second to the first non-null email while retaining the second username, assert the same exception/cause chain, and assert the complete two-row map equals the pre-update map.
5. `multipleNullEmailsRemainValid`: create two distinct usernames with null email in one repository call; assert both results exist, both emails are null, and `getAll()` equals both created rows keyed by id.
6. `nonUniqueDatabaseFailureRemainsExposedSQLException`: initialize the repository, execute `transaction(db = repo.database) { SchemaUtils.drop(repo) }`, then call `repo.create`; assert the thrown type is `ExposedSQLException`, not `DuplicateUserFieldException`, and assert its direct Xerial cause has `SQLiteErrorCode.SQLITE_ERROR`. This exercises the unchanged negative catch branch against a real driver failure. Synthetic exact-constraint negatives cover NOTNULL/CHECK/FOREIGNKEY/ROWID, which the typed Users write API cannot naturally generate.

For the four duplicate tests, capture `before = repo.getAll()` immediately before the failing operation and assert `repo.getAll() == before` afterward. This proves failed creates add no row and failed updates preserve both target fields. A small private assertion helper may validate the repeated `DuplicateUserFieldException -> ExposedSQLException -> SQLiteException(SQLITE_CONSTRAINT_UNIQUE)` shape; no production seam is needed.

## Red-first reproduction sequence

The current production failure is reproducible before changing the classifier. Coding should first add only the `jvmMain` Xerial dependency, the shared-memory fixture, and `createDuplicateUsernameMapsToDuplicateUserFieldException`. Run:

```text
./gradlew :wishlist.features.users.common:jvmTest --tests "dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepoSqliteTest.createDuplicateUsernameMapsToDuplicateUserFieldException"
```

The expected red result is an `ExposedSQLException` escaping where the test expects `DuplicateUserFieldException`; the direct cause should be `SQLiteException` with null SQL state, ordinary error code 19, and result code `SQLITE_CONSTRAINT_UNIQUE`. Record that red result in the Coding report. Then implement the classifier and add the remaining tests. The dependency addition only makes the already selected production driver available to this module; it does not alter classification behavior before the red run.

## Documentation updates

Coding must update only Users documentation and KDoc, never Operator Notes:

- In `features/users/README.md`, describe `ExposedUsersRepo` as the JVM PostgreSQL/SQLite implementation and describe the duplicate contract as PostgreSQL `23505` or exact Xerial UNIQUE/PRIMARYKEY anywhere in cause/next chains. State that code 19, message text, and other SQLite constraints are deliberately not translated.
- In `ExposedUsersRepo.kt`, update class, create/update, and classifier KDoc from PostgreSQL-only wording to the exact cross-database contract and graph traversal.
- In `DuplicateUserFieldException.kt`, update the cause/implementation KDoc to include SQLite exact result codes while retaining the existing caller contract.

## Verification and stop conditions

After the red-first proof and implementation, run these commands separately:

```text
./gradlew :wishlist.features.users.common:jvmTest
./gradlew :wishlist.features.users.common:dependencyInsight --configuration jvmTestRuntimeClasspath --dependency org.xerial:sqlite-jdbc
./gradlew build
./gradlew allTests
ast-index rebuild
```

The dependency report must resolve Xerial 3.53.4.0. Stop only when the focused suite passes all direct, nested, next-chain, cycle, equality-collision, exact-positive, exact-negative, four real duplicate, raw-failure, and null-email cases; the complete two-row state remains unchanged after update failures; the full build and all-tests gate pass; public signatures are unchanged; and the rebuilt index succeeds.

## Coding handoff

ENTITY:
entity_id=sqlite_unique_violation_classifier; type=repository_error_classifier; state=architecture_complete

CONTEXT:

* task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; agent_id=architecturing; memory_ref=[PROMPT.md,001-planning.md,features/users/README.md,features/users/common/build.gradle,ExposedUsersRepo.kt,IsUniqueViolationTest.kt,Exposed_1.3.0_API,Exposed_1.4.0_API,Xerial_3.53.4.0_API]
* constraints=[finding_4_only,repository_layer_scope,public_API_unchanged,PostgreSQL_23505_anywhere,exact_Xerial_codes,identity_cycle_safety,real_SQLite_regressions]; exclusions=[generic_code_19,message_matching,base_constraint_matching,schema_change,dependency_version_change,Operator_Notes_change]

ACTION:

1. action=add_private_JVM_dependency; target=features/users/common/build.gradle; params={source_set=jvmMain,configuration=implementation,alias=libs.xerial.sql,resolved_version=3.53.4.0}
2. action=replace_root_predicate; target=SQLException.isUniqueViolation; params={algorithm=iterative_BFS,worklist=java.util.ArrayDeque,visited=IdentityHashMap_backed_set,edge_order=[cause,nextException]}
3. action=preserve_PostgreSQL_match; target=sqlite_unique_violation_classifier; params={node_type=SQLException,sql_state=23505,graph_scope=all_reachable,result=true}
4. action=add_exact_SQLite_matches; target=sqlite_unique_violation_classifier; params={node_type=SQLiteException,result_codes=[SQLITE_CONSTRAINT_UNIQUE,SQLITE_CONSTRAINT_PRIMARYKEY],result=true}
5. action=reject_broad_heuristics; target=sqlite_unique_violation_classifier; params={rejected=[errorCode_19,message_text,SQLITE_CONSTRAINT,NOTNULL,CHECK,FOREIGNKEY,ROWID],result=false}
6. action=prove_graph_safety; target=IsUniqueViolationTest.kt; params={cases=[generic_cause,nextException,cause_next_cross_cycle,equality_collision],construction=non_hanging}
7. action=prove_real_writes; target=ExposedUsersRepoSqliteTest.kt; params={database=named_shared_memory,keeper=true,cases=[create_username,create_email,update_username,update_email,null_email_control,raw_failure]}
8. action=preserve_outer_exception; target=ExposedUsersRepo_create_update; params={positive_cause=original_ExposedSQLException,negative_throw=original_ExposedSQLException,catch_blocks=unchanged}
9. action=apply_safe_fixture_cleanup; target=withSqliteRepo; params={unregister=TransactionManager.closeAndUnregister,keeper_close=finally,database_file=none,unique_name_per_test=true}
10. action=update_documentation; target=Users_feature_docs; params={files=[README.md,ExposedUsersRepo_KDoc,DuplicateUserFieldException_KDoc],Operator_Notes_change=false}
11. action=verify_and_stop; target=repository; params={gates=[focused_jvmTest,Xerial_dependencyInsight,build,allTests,ast-index_rebuild],public_API_change=false}

REASON:

* condition=Xerial_extended_codes_mask_to_JDBC_errorCode_19; requirement=SQLiteException.resultCode_exact_matching
* condition=exception_graph_contains_cause_and_next_edges_plus_cycles; requirement=deterministic_iterative_identity_traversal
* condition=existing_consumers_depend_on_DuplicateUserFieldException; requirement=outer_ExposedSQLException_retention_plus_public_contract_preservation

EXPECTED RESULT:

* entity_id=sqlite_unique_violation_classifier; new_state=exact_PostgreSQL_and_SQLite_unique_detection; location=ExposedUsersRepo.kt
* entity_id=sqlite_repository_regression_suite; new_state=real_create_update_translation_and_state_preservation_verified; location=ExposedUsersRepoSqliteTest.kt
* entity_id=users_duplicate_contract; new_state=cross_database_consistent; location=DuplicateUserFieldException_and_Users_README

VERIFICATION:

* check=PostgreSQL_23505_any_reachable_SQLException; expected=classification_true
* check=Xerial_UNIQUE_or_PRIMARYKEY_any_reachable_SQLiteException; expected=classification_true
* check=Xerial_non_unique_constraint_or_plain_code_19_or_unique_message; expected=classification_false
* check=cause_next_cycle_plus_equal_distinct_nodes; expected=termination_plus_complete_identity_traversal
* check=real_SQLite_four_duplicate_writes; expected=DuplicateUserFieldException_plus_original_ExposedSQLException_cause
* check=failed_create_or_update_state; expected=pre_failure_map_equals_post_failure_map
* check=real_SQLite_null_email_control; expected=two_rows_with_null_email
* check=real_SQLite_raw_database_failure; expected=ExposedSQLException_rethrown_without_translation

UNCERTAINTY:

* missing=live_PostgreSQL_fixture; ambiguity=existing_root_and_nested_23505_classifier_tests_preserve_PostgreSQL_contract
* missing=natural_Users_API_path_for_non_unique_SQLite_constraints; ambiguity=synthetic_exact_constraint_tests_plus_real_SQLite_ERROR_test_cover_negative_boundary

REPETITION OF RESULT:

* entity_id=sqlite_unique_violation_classifier; stored_in=shared_memory; status=available_for_coding

COMMUNICATION:

* sender=architecturing; receiver=coding; task_id=03.09.2026_09.59.41-cce30028-b14e-4492-8cec-a0a67e5b0994; message_id=78d30df2-00df-43c7-b515-750a64c1286d; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=true; index_keys=[task_id,sqlite_unique_violation_classifier,sqlite_repository_regression_suite,users_duplicate_contract]

VALIDATION:

* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true
* causal_chain_present=true; ambiguity_detected=false; validation_scope=coding_handoff
