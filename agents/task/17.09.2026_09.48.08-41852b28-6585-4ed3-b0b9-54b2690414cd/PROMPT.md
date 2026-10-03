# Source and request

User request:

> “In current PR (in context of its issue) check if there are any restrictions on amount of changes of user emails per some time. Lets make it configurable and prohibit user change email for some time after email approve. Besides, current user email must be latest approved email until user approve new one. It must be reflected in the UI of user profile”

# PR and issue context

Current PR: [#82](https://github.com/InsanusMokrassar/WishlistApp/pull/82), “Harden owner email replacement privacy and feedback handling”. The PR is open, targets `master`, uses head branch `feat/issue-79-user-email-change`, and closes issue #79.

Issue #79: [Allow users to change their own email address](https://github.com/InsanusMokrassar/WishlistApp/issues/79).

Original acceptance requires that an authenticated owner can replace an existing or approved email through the existing endpoint; successful replacement refreshes the private profile and resets approval; SMTP-enabled replacement enters verification; duplicate, invalid, transport, and stale changes receive explicit feedback; root editing another user has no private email controls; and JS/JVM/Android/shared tests and documentation remain consistent.

Prior task context: `agents/task/10.09.2026_16.32.33-2f63e0e4-5fa6-4bb0-819c-3c99ea86af3d`, completed before this follow-up.

# Requested outcome

Investigate whether restrictions already limit the frequency or number of user email changes within a time period. Make the applicable restriction configurable and prevent a user from changing the email for a configurable period after email approval. Preserve the current user email as the latest approved email until the replacement email is approved. Reflect the pending/restriction state and resulting behavior in the user profile UI.

The default cooldown duration, persistence mechanism, and storage design must be investigated rather than assumed.
