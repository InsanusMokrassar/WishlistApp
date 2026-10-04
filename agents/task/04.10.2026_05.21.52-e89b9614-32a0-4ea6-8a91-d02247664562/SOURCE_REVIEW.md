# Local review of PR #81 — password change by email

**Pull request:** [InsanusMokrassar/WishlistApp #81](https://github.com/InsanusMokrassar/WishlistApp/pull/81)  
**Reviewed revision:** `2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06`  
**Base revision:** `master` at `0b7bf58649a0a8844af710078d9d04c3cd75c1c4`  
**Result:** Four Medium findings and one **conditional Medium risk**. No confirmed High or Critical findings within the reviewed scope.

The server and client were reviewed independently by two reviewers, and the findings were additionally checked against code. Improve failure handling and approval cleanup before merging.

## Findings

### 1. Medium — The form permits retry after an uncertain completion

If the server changes the password but its response is lost, the client enters `Unconfirmed`. Clearing the loading state permits another submission, although the UI text explicitly advises against retrying. The retry can then return `InvalidApproval` even though the password was already changed. That denial must not be presented as proof that the first operation failed.

**Recommendation:** Block resubmission from `Unconfirmed` and offer a safe exit with truthful unknown-result messaging.

**References:** [PasswordChangeViewModel.kt:181–187](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt#L181-L187); [availability condition:98–112](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt#L98-L112); [request handling:145–187](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt#L145-L187).

### 2. Medium — A sent email can be reported as a delivery failure

The server returns `Sent`, after which the ViewModel independently reloads the profile. If that read fails, the UI says the email could not be sent, even though it may already have been delivered. This false failure message can encourage requests for additional links.

**Recommendation:** Separate the delivery outcome from profile reconciliation and read errors, preserving truthful feedback about the email request.

**Reference:** [UserEditViewModel.kt:1096–1109](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/UserEditViewModel.kt#L1096-L1109).

### 3. Medium — Terminal approval denial retains entered passwords

`InvalidApproval` is terminal, but the password and confirmation fields are not cleared, and their handlers then ignore edits. The web form exposes `Continue` only after success, so terminal denial lacks an equivalent safe exit.

**Recommendation:** Clear entered secrets on terminal denial and provide a safe exit or new-request flow on the relevant platforms. Clearing immutable strings does **not** guarantee physical memory zeroization.

**References:** [PasswordChangeViewModel.kt:174–177](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt#L174-L177); [field handlers:122–136](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/commonMain/kotlin/ui/PasswordChangeViewModel.kt#L122-L136); [web form:60–69](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt#L60-L69), [109–115](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/ui/users/src/jsMain/kotlin/ui/PasswordChangeView.kt#L109-L115).

### 4. Medium — Expired unused approval records have no bounded cleanup

The fifteen-minute expiry rejects authorization but does not delete expired, unused stored links. Deletion occurs on issuance failure or successful consumption; no background cleanup was found. Never-opened or never-used records therefore accumulate metadata including an email address and a credential-state fingerprint.

**Recommendation:** Add bounded cleanup or retention for unused links, without disrupting other deeplink types.

**References:** [EmailPasswordChangeService.kt:83](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt#L83); [141–165](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt#L141-L165).

### 5. Conditional Medium risk — One-use approval may race across server processes

`EmailVerificationAccountCoordinator` uses a local `Mutex`, which coordinates only within one process. **If** multiple server processes share PostgreSQL, the read/check followed by unconditional deletion may let two processes accept the same approval before either deletes it, allowing both to write a new password. Existing concurrency tests cover one-instance coordination. Applicability to the actual deployment was **not established**; this is neither a confirmed deployment vulnerability nor a reproduced incident.

**Recommendation:** Use atomic consumption in shared storage with consistent handling of the password update, or explicitly restrict supported deployment to one process.

**References:** [EmailVerificationAccountCoordinator.kt:32](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/email/server/src/commonMain/kotlin/services/EmailVerificationAccountCoordinator.kt#L32); [EmailPasswordChangeService.kt:157–164](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/email/server/src/commonMain/kotlin/services/EmailPasswordChangeService.kt#L157-L164); [AuthFeatureService.kt:353–359](https://github.com/InsanusMokrassar/WishlistApp/blob/2ca82d1f7b5c6d5b79e82605fa64b8d0a8e8fc06/features/auth/server/src/commonMain/kotlin/services/AuthFeatureService.kt#L353-L359).

## Implementation choices and scope

Retaining existing sessions after a password change and consuming an approval before writing the new hash are explicit implementation choices, not accidental regressions identified by this review. Their suitability against security requirements should be confirmed separately.

This review was based on static analysis of code and existing tests. No fresh tests or builds were run. The Git revisions were checked, but a live CI recheck through the GitHub API was unavailable because it returned HTTP 502. No source code was changed, and no review or comments were published to GitHub. This document is an English **local copy** of the review; it does not claim that fixes are complete or describe the current PR or CI status.
