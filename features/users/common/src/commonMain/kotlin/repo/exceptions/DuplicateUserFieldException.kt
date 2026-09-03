package dev.inmo.wishlist.features.users.common.repo.exceptions

/**
 * Thrown by [dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo]'s write operations
 * (`update`, `create`) when the underlying storage rejects the write because a
 * unique-constrained `users` column (`username` or `email`) already holds the given value for a
 * different user.
 *
 * Thrown from the JVM-only Exposed implementation after it translates a caught PostgreSQL
 * unique-violation (`23505`) or Xerial's exact SQLite UNIQUE/PRIMARY KEY extended result code.
 * The original `ExposedSQLException` remains available as [cause]. The exception propagates
 * unchanged through `CacheUsersRepo`; unrelated SQLite constraints are not translated.
 *
 * This is the repo-wide convention for signalling "duplicate key" from any [WriteUsersRepo]
 * write: callers that need to distinguish it from other failures — most commonly HTTP route
 * handlers that should respond `409 Conflict` instead of the generic `500 Internal Server Error`
 * an unmapped exception would otherwise produce (Ktor's engine-level
 * `DefaultEnginePipeline.handleFailure` fallback) — catch this type.
 *
 * @param cause The outer persistence exception retained for diagnostics, if available.
 */
class DuplicateUserFieldException(cause: Throwable? = null) :
    RuntimeException("A user with the same username or email already exists.", cause)
