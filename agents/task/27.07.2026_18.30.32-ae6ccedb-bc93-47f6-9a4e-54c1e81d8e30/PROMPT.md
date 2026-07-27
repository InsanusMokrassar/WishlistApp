# Task Prompt

Solve GitHub issue #73, “Complete emails support for users”, in repository `InsanusMokrassar/WishlistApp`.

Issue requirements:

1. Add an option to require users to provide an email address during registration.
2. Add a service that sends invite links; invite links must use the existing deeplinks feature.
3. When email is required, assign new users the `NewUser` role instead of `User`.
4. After a user verifies the email address, change the role from `NewUser` to `User`.

Scope note: inspect the current implementation and configuration, including `server/docker-compose.yml`, and implement the complete issue with appropriate tests and validation. Preserve unrelated working-tree changes.

Workflow: execute planning, architecturing, coding, verification, and validating stages sequentially. Each stage must create its own numbered step report and commit according to `agents/ALL.md`, `agents/PROTOCOL.md`, and `agents/GIT.md`.
