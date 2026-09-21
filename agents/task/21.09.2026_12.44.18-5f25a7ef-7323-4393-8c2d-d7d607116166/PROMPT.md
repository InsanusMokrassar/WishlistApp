# Merge current master into PR #75

Merge `origin/master` commit `f786ad93c3e8a1ceeb3025a35a4922bd55d7ed3a` into PR #75 branch `fix/issue-72-detekt-conventions`, whose starting head is `1cbab45e1579cd8168bd3c88e34d15278ac8397f`.

Resolve every conflict while preserving both current master framework/content and PR #75's intent from issue #72:

- repository-wide Detekt convention enforcement for Kotlin projects;
- custom `NoElseIf` rule and focused tests;
- public KDoc rules with project-scoped baselines;
- blocking `./gradlew detekt` in mechanical Verification.

Follow the repository's ordinary root workflow completely. Run relevant Detekt, custom-rule, build, and test checks. Create a merge commit, push the resulting head to `origin/fix/issue-72-detekt-conventions`, and verify live PR #75 state. Do not merge PR #75 into `master` and do not modify other PR branches or worktrees.
