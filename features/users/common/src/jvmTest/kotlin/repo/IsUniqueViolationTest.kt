package dev.inmo.wishlist.features.users.common.repo

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies [isUniqueViolation]'s SQL-state matching in isolation, without a live database. */
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
}
