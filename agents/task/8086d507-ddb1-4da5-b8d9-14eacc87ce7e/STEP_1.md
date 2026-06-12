# STEP_1 — Analysis (security findings + README delta)

ENTITY:
entity_id=security-analysis; type=analysis-result; state=complete

CONTEXT:

* task_id=8086d507-ddb1-4da5-b8d9-14eacc87ce7e; agent_id=root-orchestrator
* scope=server features(auth,users,files,admin,wishlist,booking,common,currency) + deploy(Dockerfile,deploy.sh,compose) + config(dev/sample) + dep-versions + JS-client token storage

FINDINGS (severity; entity=file:line; impact→fix):

1. HIGH; finalize accepts image/svg+xml; FilesService.kt:45-50 + FilesRoutingsConfigurator.kt:67-81 + LocalStorageAuthCredentialsStorage.kt; impact=stored-XSS→token-theft(localStorage wishlist.auth.credentials)→account-takeover(refresh TTL P7D); fix=svg-block+magic-byte-allowlist+nosniff+Content-Disposition:attachment+CSP+separate-file-origin
2. HIGH; /temp_upload unauthenticated + no size cap + default utilizer=no-op Job(); MicroUtils TemporalFilesRoutingConfigurator.kt:88-124 + files/server/Plugin.kt:28; impact=disk/mem-exhaustion-DoS(temp never purged)+OOM(finalize readBytes whole file); fix=authenticate temp_upload+max-content-length+ByTimerUtilizer TTL+stream-to-disk
3. MEDIUM; no rate-limit on login/register/refresh + BCrypt.checkpw under global withWriteLock; AuthFeatureService.kt:47-54 + AuthRoutingsConfigurator.kt; impact=brute-force + auth-serialization-DoS; fix=RateLimit plugin/proxy + move checkpw out of write-lock + backoff
4. MEDIUM; plain HTTP bind 0.0.0.0:8196 no-TLS; common/JVMPlugin.kt connector; impact=cleartext password+token on wire; fix=mandatory TLS reverse-proxy+HSTS; also publicHost="0.0.0.0" wrong
5. MEDIUM; no password/username policy + jBCrypt 0.4 truncates>72B; AuthFeatureService.register:96-106 + value-classes Username/Password no-validation; fix=min-length+charset+>72B-reject
6. LOW-MED; secrets in logs; auth/server/JVMPlugin.kt:48-50 (root pass INFO) + common CallLogging TRACE debug; fix=prod log controls+never TRACE prod+scrub Authorization
7. LOW; public user-enum/info-disclosure; UsersRoutingsConfigurator.kt (GET /users/getAll public) + WishlistRoutingsConfigurator getByUserId public + register 400-on-dup; fix=gate-if-not-intended+generic-errors
8. LOW; path defense-in-depth; DiskFilesRepo.kt:26 File(folder,id.string) raw; currently gated by meta-existence(server-UUID) → not exploitable; fix=canonical-path validate
9. LOW; in-mem token store unbounded; AuthFeatureService getUser no-purge; fix=periodic-sweep/persistent-store
10. INFO; deps current (ktor 3.5.0, pg 42.7.11, logback 1.5.32, kotlin 2.3.21); jBCrypt 0.4 old-but-latest; fix=add OWASP dependency-check CI
11. INFO; committed dev creds test/test (dev.config.json, docker-compose.yml — acceptable); OXR appId only in gitignored local.config.json (VERIFIED not in git history) — rotate, keep out VCS; sample.config.json uses TEST_* placeholders (good)

POSITIVE CONTROLS (report-must-note):
ownership-checks wishlist/item/admin correct; booking authz solid(owner-hidden,single-booking,anon-booker); BCrypt hashing; bearer-in-header=CSRF-immune; secrets externalized+gitignore; cascade-delete purges sessions

README-DELTA:

* dev-run arg: sample.config.json→dev.config.json
* server-config section: reference dev.config.json for dev; add rows useCache/openExchangeRatesAppId/openExchangeRatesRefreshTTLMillis
* NEW "Production deployment" section: explain server/sample.config.json (prod config template, /static + /data, TEST_* placeholders, mounted /config.json), server/sample.docker-compose.yml (prod compose template), server/Dockerfile (image build), server/deploy.sh (build+push); warn TLS-proxy + replace placeholders

EXPECTED RESULT:
entity_id=local.security.report.md; new_state=to-be-written-by-haiku
entity_id=README.md; new_state=to-be-edited-by-haiku
