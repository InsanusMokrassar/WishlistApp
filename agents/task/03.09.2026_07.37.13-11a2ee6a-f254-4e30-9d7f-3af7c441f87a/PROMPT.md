# Task Prompt

Address selected review findings and explain the remaining findings:

1. Add an `unsafeSsl` email/SMTP configuration option and set `mail.smtp.ssl.trust` only when that option is enabled.
2. Leave the sample SQLite database path unchanged; the sample is intended for self-hosting operators to adapt.
3. Explain precisely how the new registration response affects clients talking to legacy servers, including whether the issue affects old devices.
4. Recommend a fix for SQLite unique-constraint classification; do not implement without a later operator decision.
5. Fix the sample SMTP settings so port 587 uses STARTTLS without implicit SSL.
6. Accept the breaking source/API compatibility change to the two-argument registration method for now; do not restore the legacy return type.
7. Fix orphaned delivered verification links after later registration finalization failure, if possible without breaking the current public API.
8. Recommend a fix for cancellation after the committed user insert but before registration compensation is installed; do not implement without a later operator decision.

Preserve unrelated behavior and add focused regression coverage for implemented fixes.
