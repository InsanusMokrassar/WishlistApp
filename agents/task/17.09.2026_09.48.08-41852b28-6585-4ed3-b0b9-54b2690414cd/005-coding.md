Model: gpt-5.6-terra (ML; Coding priority selects ML before HL).
Changed files: agents/task/17.09.2026_09.48.08-41852b28-6585-4ed3-b0b9-54b2690414cd/005-coding.md

# Continued coding audit

Reviewed 004-coding.md and commit `85991a7`. The current `AbstractExposedWriteCRUDRepo` implementation opens nested transactions for inherited create/update/bulk-update paths and publishes flows after each inherited body. Architecture 003 therefore requires direct replacement implementations before a singleton lock-row protocol can safely cover all writers and preserve post-commit atomic events.

No product change was made in this continuation. The earlier focused compile passed; the focused email-server test run remains failing with twelve assertions that encode the obsolete destructive-email lifecycle. Required lock/migration/PostgreSQL/UI/admin/test/README work remains incomplete. `ast-index` was used for the audit; no developer database was accessed.
