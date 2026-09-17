# GitHub issue #78

Add a self-service password-change flow for the authenticated profile owner. The entry point must be available only when the server email service is configured and the current user has a non-null approved email address.

The user requests a password-change email. The server mints an approval UUID through the existing deeplink infrastructure and emails its URL to the approved address. Opening that deeplink must invoke a purpose-specific handler and redirect to a client password-change page. The page must carry and submit the same approval UUID from the opened deeplink; the server must validate that UUID as the authorization for changing the associated user password.

Acceptance criteria:

- The profile editor exposes the request action only when SMTP is enabled and the owner's current email is approved.
- Requesting the action sends a deeplink to the approved current email.
- Opening the deeplink redirects to a dedicated password-change page with the same approval UUID.
- Submitting matching new-password fields uses that exact UUID and changes only the associated user's password.
- Missing, unknown, mismatched, or no-longer-valid approvals fail closed and do not change a password.
- The flow reuses existing deeplink and password-storage boundaries and has server, client, navigation, and UI tests.

Source: https://github.com/InsanusMokrassar/WishlistApp/issues/78
