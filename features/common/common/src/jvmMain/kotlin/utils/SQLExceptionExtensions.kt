package dev.inmo.wishlist.features.common.common.utils

import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.SQLException
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Returns whether this exception graph contains PostgreSQL `unique_violation` SQL state `23505`
 * or Xerial's exact SQLite UNIQUE/PRIMARY KEY extended constraint result code.
 *
 * Traverses every [Throwable.cause] and [SQLException.getNextException] edge iteratively, tracking
 * object identity so wrapper cycles terminate without merging distinct exceptions that compare
 * equal. Generic JDBC code 19, message text, base SQLite constraints, and unrelated constraints
 * are deliberately not classified as duplicates.
 *
 * @return `true` only when an exact supported unique-violation marker is reachable.
 */
fun SQLException.isUniqueViolation(): Boolean {
    val pending = ArrayDeque<Throwable>()
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
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
