# Issue #72: Add detekt to mechanically enforce agents coding conventions (else-if ban, KDoc)

GitHub: https://github.com/InsanusMokrassar/WishlistApp/issues/72

Operator request: `git checkout master && try to fix one left issue on gh`

The agents coding rules in `agents/CODING.md` currently rely on model-based review (Validator role) for mechanical style rules. These should be enforced by a linter so Verification catches violations deterministically and cheaply.

Tasks:

- Add detekt to the Gradle build (root convention, applied to all Kotlin modules).
- Add a custom rule or forbidden-pattern configuration banning `else if` chains. A single binary `if`/`else` remains allowed, per the Control Flow rule in `agents/CODING.md`.
- Enable `UndocumentedPublicClass`, `UndocumentedPublicFunction`, and `UndocumentedPublicProperty` to back the KDoc Requirements rule.
- Wire `./gradlew detekt` into the Verification stage in `agents/VERIFICATION.md` as a blocking step.

Origin: agents configuration review (`local.agents.review.md`, finding M8), 2026-07-14.
