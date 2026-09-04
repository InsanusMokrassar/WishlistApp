# Task Prompt

Fix review finding 4: checked-in configurations use SQLite, while `ExposedUsersRepo.isUniqueViolation()` currently recognizes only PostgreSQL SQL state `23505`. Xerial SQLite uniqueness failures must map to the existing `DuplicateUserFieldException` contract instead of escaping as raw `ExposedSQLException` and producing HTTP 500 responses.

Preserve PostgreSQL unique-violation support. Classify SQLite uniqueness precisely without treating unrelated constraint violations as duplicates. Account for the actual Exposed/JDBC exception wrapper and chaining shape, and protect traversal against cycles. Add focused regression coverage, including real in-memory SQLite duplicate username and non-null email behavior plus negative non-unique constraint cases where practical.

Keep the patch at the narrowest responsible repository layer, preserve unrelated behavior and public APIs, run focused and repository-wide verification, commit the completed work, and push the branch after the full workflow passes.
