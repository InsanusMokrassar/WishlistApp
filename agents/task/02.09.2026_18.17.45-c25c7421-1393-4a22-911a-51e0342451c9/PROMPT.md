# Task Prompt

1. Make outgoing approving email contain a normally displayed link, not a link shown as raw markup.
2. Add a way for deep-link handling to return a destination where the user must be redirected. For example, introduce a `Handled` sealed interface with `Common`, matching current handled behavior, and `Redirect(val url: String)`, which redirects the user to a page.
3. Rework the email-approval deep-link handler to redirect to the main page and show a message such as “Email has been approved.” The notification presentation may use an additional view, snackbar, or another suitable existing pattern.
4. After a user approves the account, send the user an email confirming approval.
