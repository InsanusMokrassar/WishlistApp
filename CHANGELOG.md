# Changelog

## 0.2.0

- Added a root-only Admin Panel entry to the web sidebar.
- Public user data no longer exposes email addresses, and feature APIs now use feature-owned models.
- Added role-based authorization with `SuperAdmin`, `User`, and `NewUser` roles plus feature-specific access checks.
- Registration can now require email verification, with SMTP verification links, pending-account access restrictions, and approval feedback.
- Hardened email approval with atomic updates, unique-address conflict handling, and cancellation-safe compensation.
- Added public-origin, SMTP, Mailpit, registration-policy, and roles configuration for local and production environments.
- Docker deployment now runs only on `master` and preserves the version declared by Gradle.

## 0.1.0

- Web top bar search field disabled with a "coming soon" tooltip, styled per Calm Studio.
- Web client now falls back to the current page origin for the server URL when none is configured.
- Added server-side deeplinks support.
- Edit screens now return to their read view on logout.
- Top bar breadcrumbs are now clickable for navigation.
- API is now served under the /api prefix with static content from root; fixes deep-link asset resolution and double-prefixed uploads.
- Updated URL navigation configs repository handling.
