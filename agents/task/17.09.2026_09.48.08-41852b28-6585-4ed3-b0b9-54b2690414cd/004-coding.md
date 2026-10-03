Model: gpt-5.6-terra (ML; Coding priority selects ML before HL). LL documentation filling was not separately available in this harness; KDocs were added for new declarations.
Changed files: users/email/auth/admin lifecycle sources and focused tests listed by the task-relevant commit.

# Coding report

Implemented the first bounded lifecycle slice: nullable pending email and deadline fields flow through private user projections; repository-specific email, username, fresh-read, approval, cache, policy, 429 transport, and recipient-selection seams are present. Approved replacements retain the current approved address, exact pending approval promotes it, and the sample configuration explicitly uses `P1D`. Authenticated `getMe` now bypasses the local cache.

`./gradlew --no-parallel :wishlist.features.email.server:compileKotlinJvm :wishlist.features.admin.server:compileKotlinJvm :wishlist.features.ui.users:compileKotlinJvm` passed. `ast-index rebuild` passed after source edits. Users common tests compile and pass. The focused email server test run has 12 failures because existing issue #79 assertions still require destructive current-email replacement and current-address-only delivery; no assertions were weakened or rewritten to hide the intentional lifecycle incompatibility.

The requested Architecture 003 completion remains incomplete: singleton cross-process database lock, batch atomicity, PostgreSQL task/proof, migration and concurrency tests, admin typed result, shared UI state/rendering/localization, README deltas, and the complete updated server test suite are still required. PostgreSQL was not provisioned or run. No developer database was accessed.

Deviation: the initial persistence implementation uses the existing repository transaction seam but does not yet add Architecture 003's required singleton write-lock table; therefore it must not be presented as the specified multi-instance uniqueness proof.
