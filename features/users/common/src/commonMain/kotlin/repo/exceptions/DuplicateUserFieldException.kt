package dev.inmo.wishlist.features.users.common.repo.exceptions

/**
 * Thrown by [dev.inmo.wishlist.features.users.common.repo.WriteUsersRepo]'s write operations
 * (`update`, `create`) when the underlying storage rejects the write because a
 * unique-constrained `users` column (`username` or `email`) already holds the given value for a
 * different user.
 *
 * Thrown from the JVM-only Exposed implementation (`ExposedUsersRepo`, the only [WriteUsersRepo]
 * that talks to a real, constraint-enforcing Postgres database) after it translates a caught
 * unique-violation `ExposedSQLException` (SQL state `23505`). Propagates unchanged through
 * `CacheUsersRepo` — the `FullCRUDCacheRepo` write wrapper it is built on does not catch
 * exceptions from the wrapped repo, it only reacts to a successful, non-throwing result.
 *
 * This is the repo-wide convention for signalling "duplicate key" from any [WriteUsersRepo]
 * write: callers that need to distinguish it from other failures — most commonly HTTP route
 * handlers that should respond `409 Conflict` instead of the generic `500 Internal Server Error`
 * an unmapped exception would otherwise produce (Ktor's engine-level
 * `DefaultEnginePipeline.handleFailure` fallback) — catch this type.
 *
 * @param cause The underlying driver exception, if any (kept for logging).
 */
class DuplicateUserFieldException(cause: Throwable? = null) :
    RuntimeException("A user with the same username or email already exists.", cause)
