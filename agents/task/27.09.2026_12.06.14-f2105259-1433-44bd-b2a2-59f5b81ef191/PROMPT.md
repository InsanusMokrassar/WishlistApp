Working in PR #82. Check actuality of current branch comparing to master, if master have fresh commits - merge them into branch. Replace `Long`s which have been used as moment of time with `DateTime`. Its' serializer presented in `MicroUtils`. Write it as a rule - all timestamps must be used with `DateTime` from korlibs.

Root Git preparation: fetched origin/master and origin/feat/issue-79-user-email-change on 2026-09-27; master f786ad9 is ancestor of PR head 5664e7f; local feat/issue-79-user-email-change fast-forwarded to 5664e7f. No master merge is required.
