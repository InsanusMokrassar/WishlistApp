# GitHub issue #79

Extend the profile editor so an authenticated owner can replace an existing email address, including an already approved address. Keep the current self-only authorization boundary and use the existing email storage endpoint.

Acceptance criteria:

- The owner can enter and save a different valid email when an address already exists.
- A successful replacement refreshes the private profile and resets approval for the new address through the existing repository invariant.
- When SMTP verification is enabled, the replacement address can enter the existing verification flow.
- Duplicate-address conflicts, invalid input, transport failures, and stale owner or session changes produce explicit feedback and do not publish a false success.
- A root user editing another account does not gain access to that account's private self-service email controls.
- JS, JVM, Android, shared ViewModel tests, and feature documentation remain consistent.

Source: https://github.com/InsanusMokrassar/WishlistApp/issues/79
