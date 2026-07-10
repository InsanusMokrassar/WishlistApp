package dev.inmo.wishlist.features.users.common.repo

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies [isUniqueViolation]'s SQL-state matching in isolation, without a live database. */
class IsUniqueViolationTest {
    @Test
    fun returnsTrueForPostgresUniqueViolationSqlState() {
        assertTrue(SQLException("duplicate key value violates unique constraint", "23505").isUniqueViolation())
    }

    @Test
    fun returnsFalseForOtherSqlStates() {
        assertFalse(SQLException("foreign key violation", "23503").isUniqueViolation())
    }

    @Test
    fun returnsFalseWhenSqlStateIsNull() {
        assertFalse(SQLException("generic failure").isUniqueViolation())
    }
}
