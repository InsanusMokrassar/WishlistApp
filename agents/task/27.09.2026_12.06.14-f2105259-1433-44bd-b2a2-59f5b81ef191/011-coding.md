Model: GPT-6 Sol
Changed files: agents/task/27.09.2026_12.06.14-f2105259-1433-44bd-b2a2-59f5b81ef191/011-coding.md; features/users/server/src/commonTest/kotlin/services/UsersServiceTest.kt; features/ui/wishlist/src/commonTest/kotlin/ui/WishlistsModelTest.kt

## Coding retry

Corrected the three test-source type errors reported by `010-verification.md`. The users service fixture now wraps its requested-at and allowed-at millisecond values with `DateTime.fromUnixMillis`. The wishlist model's currency-rates fixture wraps its fetched-at value the same way. The values and assertions retain their original test intent. No production behavior or feature documentation changed.

Audited feature-test references with `ast-index` for `EmailProfile`, `EmailChangeCooldown`, `CurrencyRates`, the migrated timestamp field assignments, and the affected repository and service clocks. The other direct timestamp assignments and cooldown constructions already use `DateTime`, null, or an existing DateTime value; primitive clock inputs found in email tests use the repository's explicit compatibility constructor. No further clear Long instant fixture needed correction. Rebuilt the index after the Kotlin edits.

## Validation

`./gradlew :wishlist.features.users.server:jvmTest :wishlist.features.ui.wishlist:jvmTest :wishlist.features.ui.wishlist:jsTest --no-parallel` passed with exit code 0. The wishlist JS task included both browser and Node tests.

`./gradlew :wishlist.features.users.server:build :wishlist.features.ui.wishlist:build --no-parallel --quiet` passed with exit code 0. `git diff --check` passed. A repository-wide `./gradlew build` remains for the separate verification stage; this retry did not run that gate.
