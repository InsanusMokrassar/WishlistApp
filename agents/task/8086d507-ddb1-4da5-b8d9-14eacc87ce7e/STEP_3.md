# STEP_3 — Coding (security fixes on branch fix/security_issues)

ENTITY:
entity_id=security-fixes; type=coding-result; state=complete; branch=fix/security_issues

CONTEXT:

* task_id=8086d507-ddb1-4da5-b8d9-14eacc87ce7e; agent_id=root-orchestrator
* constraint=each fix own commit; model-rule=use fable where opus would be used (this session)
* base=master@06fa6f1 (docs commit already pushed)

ACTION (each fix = own commit):

1. commit=1822f48; fix(files) FilesService.kt; finalize() now allowlist raster MIME (png/jpeg/gif/bmp/webp) + magic-byte match; blocks image/svg+xml → kills stored-XSS vector (finding #1 HIGH)
2. commit=76da4ec; fix(files) FilesRoutingsConfigurator.kt; GET /files/{id} adds `X-Content-Type-Options: nosniff` + `Content-Disposition: attachment` (finding #1 defense-in-depth)
3. commit=acdf0ac; fix(files) TimedTemporalFilesUtilizer.kt(new) + Plugin.kt; temporal uploads not finalized within 1h TTL purged from map+disk; replaces default no-op utilizer (finding #2 HIGH DoS)
4. commit=ea343f4; fix(auth) AuthFeatureService.kt; register() rejects password length outside 8..72 (finding #5 MEDIUM)

VERIFICATION:

* check=./gradlew :wishlist.features.files.server:build; result=BUILD SUCCESSFUL
* check=./gradlew :wishlist.features.auth.server:build; result=BUILD SUCCESSFUL
* one fix cycle used: 1 compile error fixed (`HttpHeaders.XContentTypeOptions` unresolved in ktor 3.5.0 → literal "X-Content-Type-Options")
* ast-index rebuilt after changes
* feature READMEs updated (files, auth) via haiku agent per md-fill rule

NOT-FIXED (with reason, finding#→reason):

* #2 require-auth on /temp_upload → SKIPPED: JS client uploads via raw XMLHttpRequest (micro_utils ActualTemporalUpload.kt jsMain) WITHOUT bearer token; adding auth breaks web upload. Mitigated instead via timed cleanup (#3 above). Needs separate client-side change to send token on XHR.
* #3 auth rate-limit / move BCrypt out of write-lock → SKIPPED: needs Ktor RateLimit plugin + dependency/infra; higher risk; out of contained-fix scope
* #4 transport TLS/HSTS → ops/deploy, not code (reverse-proxy)
* #6 log scrubbing → ops/config
* #7 public user enumeration → intentional public-read design (per README)
* #8 DiskFilesRepo path → gated by meta-existence; not exploitable; defense-in-depth only
* #9 token store unbounded → availability; deferred
* admin setPassword length policy → root-only trusted path; left unchanged to keep fix contained

NOTE: branch fix/security_issues NOT pushed (user asked push only for master current-changes; branch left local for review/PR).

EXPECTED RESULT:
entity_id=fix/security_issues; new_state=4-fix-commits+docs; status=local; builds=green
