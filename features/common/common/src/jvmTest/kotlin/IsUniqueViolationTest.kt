package dev.inmo.wishlist.features.common.common

import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies exact PostgreSQL and SQLite unique-violation graph matching without a live database. */
class IsUniqueViolationTest {
    /** Verifies that an [SQLException] carrying Postgres's unique-violation SQL state (`23505`) is detected. */
    @Test
    fun returnsTrueForPostgresUniqueViolationSqlState() {
        assertTrue(SQLException("duplicate key value violates unique constraint", "23505").isUniqueViolation())
    }

    /** Verifies that an [SQLException] carrying an unrelated SQL state (`23503`, foreign key violation) is not flagged as a unique violation. */
    @Test
    fun returnsFalseForOtherSqlStates() {
        assertFalse(SQLException("foreign key violation", "23503").isUniqueViolation())
    }

    /** Verifies that an [SQLException] with no SQL state at all is not flagged as a unique violation. */
    @Test
    fun returnsFalseWhenSqlStateIsNull() {
        assertFalse(SQLException("generic failure").isUniqueViolation())
    }

    /** Xerial's extended UNIQUE result code is classified as a duplicate. */
    @Test
    fun returnsTrueForSqliteUniqueConstraint() {
        assertTrue(
            SQLiteException(
                "unique constraint",
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE,
            ).isUniqueViolation()
        )
    }

    /** Xerial's extended PRIMARY KEY result code is classified as a duplicate. */
    @Test
    fun returnsTrueForSqlitePrimaryKeyConstraint() {
        assertTrue(
            SQLiteException(
                "primary key constraint",
                SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY,
            ).isUniqueViolation()
        )
    }

    /** PostgreSQL state matching reaches a nested SQL exception below a generic cause wrapper. */
    @Test
    fun returnsTrueForNestedPostgresUniqueViolation() {
        val root = SQLException("root")
        root.initCause(
            IllegalStateException(
                "wrapper",
                SQLException("duplicate key", "23505"),
            )
        )

        assertTrue(root.isUniqueViolation())
    }

    /** SQLite UNIQUE matching reaches a driver exception below a generic cause wrapper. */
    @Test
    fun returnsTrueForNestedSqliteUniqueConstraint() {
        val root = SQLException("root")
        root.initCause(
            IllegalStateException(
                "wrapper",
                SQLiteException(
                    "unique constraint",
                    SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE,
                ),
            )
        )

        assertTrue(root.isUniqueViolation())
    }

    /** SQLite PRIMARY KEY matching follows the JDBC next-exception edge. */
    @Test
    fun returnsTrueForSqlitePrimaryKeyInNextExceptionChain() {
        val root = SQLException("root")
        root.setNextException(
            SQLiteException(
                "primary key constraint",
                SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY,
            )
        )

        assertTrue(root.isUniqueViolation())
    }

    /** Non-unique SQLite extended constraint codes are never classified as duplicates. */
    @Test
    fun returnsFalseForNonUniqueSqliteConstraints() {
        val resultCodes = listOf(
            SQLiteErrorCode.SQLITE_CONSTRAINT,
            SQLiteErrorCode.SQLITE_CONSTRAINT_NOTNULL,
            SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK,
            SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY,
            SQLiteErrorCode.SQLITE_CONSTRAINT_ROWID,
        )

        resultCodes.forEach { resultCode ->
            assertFalse(
                SQLiteException("constraint failure", resultCode).isUniqueViolation(),
                "$resultCode must not be classified as a duplicate",
            )
        }
    }

    /** A convincing message and ordinary JDBC code 19 do not identify a SQLite duplicate. */
    @Test
    fun returnsFalseForPlainJdbcConstraintCodeAndUniqueMessage() {
        assertFalse(
            SQLException("UNIQUE constraint failed", null, 19).isUniqueViolation()
        )
    }

    /** A UNIQUE-looking message does not broaden Xerial's base constraint result code. */
    @Test
    fun returnsFalseForBaseSqliteConstraintWithUniqueMessage() {
        assertFalse(
            SQLiteException(
                "UNIQUE constraint failed",
                SQLiteErrorCode.SQLITE_CONSTRAINT,
            ).isUniqueViolation()
        )
    }

    /** Identity tracking terminates a cycle spanning cause and next-exception edges. */
    @Test
    fun terminatesForCauseAndNextExceptionCycle() {
        val root = SQLException("root", "23503")
        val bridge = SQLException("bridge", "23503")
        root.initCause(bridge)
        bridge.setNextException(root)

        assertFalse(root.isUniqueViolation())
    }

    /** Distinct graph nodes remain visible even when their equality implementation collides. */
    @Test
    fun visitsDistinctExceptionsThatCompareEqual() {
        val root = SQLException("root")
        val negativeCause = EqualityCollidingSQLException("23503")
        val positiveNext = EqualityCollidingSQLException("23505")
        root.initCause(negativeCause)
        root.setNextException(positiveNext)

        assertTrue(root.isUniqueViolation())
    }

    /**
     * SQLException fixture whose equality deliberately ignores SQL state.
     *
     * @param sqlState State retained by the superclass for classifier inspection.
     */
    private class EqualityCollidingSQLException(sqlState: String?) :
        SQLException("synthetic", sqlState) {
        /** Treats every fixture instance as equal to expose value-based visited sets. */
        override fun equals(other: Any?): Boolean = other is EqualityCollidingSQLException

        /** Uses one shared hash bucket to mirror the deliberately broad equality relation. */
        override fun hashCode(): Int = 1
    }
}
