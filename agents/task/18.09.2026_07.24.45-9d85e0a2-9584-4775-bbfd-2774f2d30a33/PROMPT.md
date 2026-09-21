# Prompt

Continue the current pull request for issue 79.

`pendingEmail`, together with the time when the email change was requested, must belong to an email-owned model. Neither field may be present in user or admin-user models.

User-profile editing MVVM must obtain pending-change information from the email service and expose an email-feature model containing pending email verification information, rather than reading or returning those fields through a direct user model.
