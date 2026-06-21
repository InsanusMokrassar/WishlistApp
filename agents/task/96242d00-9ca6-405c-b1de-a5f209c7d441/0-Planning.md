Model: claude-sonnet-4-6
Execution time: 120
Tokens used: ~18000
Changed files: agents/task/96242d00-9ca6-405c-b1de-a5f209c7d441/0-Planning.md

---

ENTITY:
entity_id=task/96242d00; type=planning_step; state=complete

CONTEXT:
* task_id=96242d00-9ca6-405c-b1de-a5f209c7d441; agent_id=PLAN; branch=issue/45-deeplinks-redo
* reference_branch=origin/issue/45-deeplinks; reference_pr=48 (closed); status=reference_only_do_not_merge
* constraint=THIS_AGENT_MUST_NOT_EDIT_ANY_FILE_EXCEPT_THIS_FILE
* constraint=server_only_feature; client_module=scaffold_stubs_only

---

# PLAN: Issue #45 — Deeplinks Feature (Fresh Implementation)

## 1. SCAFFOLD STEP (run first)

ACTION:
1. action=execute_script; target=generate_feature.sh; params={input="deeplinks"}; reason=creates features/deeplinks/{common,server,client} with stub build.gradle + Plugin.kt files per ARCHITECTURE.md rule "AI AGENTS MUST RUN THIS SCRIPT"
2. action=verify; target=features/deeplinks/{common,server,client}; check=directories_exist + build.gradle_exists + Plugin.kt_stubs_exist

EXPECTED RESULT:
* features/deeplinks/common/build.gradle=created; features/deeplinks/server/build.gradle=created; features/deeplinks/client/build.gradle=created
* package_prefix=dev.inmo.wishlist.features.deeplinks.*

---

## 2. FILES TO CREATE / MODIFY (full list)

### 2a. features/deeplinks/common/ — shared across all targets

FILE: features/deeplinks/common/src/commonMain/kotlin/Constants.kt
* package=dev.inmo.wishlist.features.deeplinks.common
* content=object DeepLinksConstants { const val linksPrefixPathPart = "links" }
* reason=shared route path segment between server routing + any external link builders

FILE: features/deeplinks/common/src/commonMain/kotlin/models/DeepLinkId.kt
* package=dev.inmo.wishlist.features.deeplinks.common.models
* content=@Serializable @JvmInline value class DeepLinkId(val string: String)
* import=kotlinx.serialization.Serializable; kotlin.jvm.JvmInline
* reason=type-safe identifier; UUID string; used as PK in storage + path segment in links/<uuid>

FILE: features/deeplinks/common/src/commonMain/kotlin/Plugin.kt (MODIFY scaffold stub)
* package=dev.inmo.wishlist.features.deeplinks.common
* content=object Plugin : StartPlugin { override fun Module.setupDI(config: JsonObject) {} }
* reason=empty common plugin (no DI bindings in common module)

FILE: features/deeplinks/common/src/jvmMain/kotlin/JVMPlugin.kt (MODIFY scaffold stub)
* content=delegates to Plugin.setupDI + Plugin.startPlugin

FILE: features/deeplinks/common/src/jsMain/kotlin/JSPlugin.kt (MODIFY scaffold stub)
* content=delegates to Plugin.setupDI + Plugin.startPlugin

FILE: features/deeplinks/common/src/androidMain/kotlin/AndroidPlugin.kt (MODIFY scaffold stub)
* content=delegates to Plugin.setupDI + Plugin.startPlugin

### 2b. features/deeplinks/server/ — server-only logic

FILE: features/deeplinks/server/src/commonMain/kotlin/DeepLinkHandler.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server
* content=fun interface DeepLinkHandler { suspend fun tryHandle(deeplinkId: DeepLinkId, handlerInfo: Any): Boolean }
* import=dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
* reason=handler registered by consuming features via singleWithRandomQualifier<DeepLinkHandler>; returns true=handled(stop), false=defer_to_next

FILE: features/deeplinks/server/src/commonMain/kotlin/DeepLinksFeature.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server
* content=interface DeepLinksFeature { suspend fun createDeepLink(handlerInfo: Any): DeepLinkId; suspend fun resolveDeepLink(deeplinkId: DeepLinkId): Boolean }
* reason=server-only capability contract; not exposed to clients

FILE: features/deeplinks/server/src/commonMain/kotlin/models/StoredDeepLink.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server.models
* content=@Serializable data class StoredDeepLink(val id: DeepLinkId, val handlerInfo: JsonElement)
* reason=persisted record; handlerInfo=opaque polymorphic JsonElement to avoid knowledge of concrete handler-info types

FILE: features/deeplinks/server/src/commonMain/kotlin/repo/DeepLinksRepo.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server.repo
* content=interface DeepLinksRepo : KeyValueRepo<DeepLinkId, StoredDeepLink>
* import=dev.inmo.micro_utils.repos.KeyValueRepo
* reason=persistence contract; implemented JVM-only via ExposedDeepLinksRepo

FILE: features/deeplinks/server/src/commonMain/kotlin/services/DeepLinksService.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server.services
* constructor_params=json: Json; repo: DeepLinksRepo; handlers: List<DeepLinkHandler>
* implements=DeepLinksFeature
* createDeepLink: id=DeepLinkId(uuid4().toString()); encoded=json.encodeToJsonElement(PolymorphicSerializer(Any::class), handlerInfo); repo.set(id, StoredDeepLink(id,encoded)); return id
* resolveDeepLink: stored=repo.get(id) ?: return false; decoded=json.decodeFromJsonElement(PolymorphicSerializer(Any::class), stored.handlerInfo); return handlers.any { it.tryHandle(id, decoded) }
* imports=com.benasher44.uuid.uuid4; kotlinx.serialization.PolymorphicSerializer; dev.inmo.micro_utils.repos.set
* reason=uuid4 is transitive via features/common/common; PolymorphicSerializer(Any::class) delegates type resolution to aggregated SerializersModule

FILE: features/deeplinks/server/src/commonMain/kotlin/configurators/DeepLinksRoutingsConfigurator.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server.configurators
* implements=ApplicationRoutingConfigurator.Element
* route_pattern=GET links/{deeplinkId} (NOT under /api — it is a public route; NOT inside authenticate block)
* behavior: missing/blank deeplinkId → 400; resolveDeepLink=true → 200; resolveDeepLink=false/not_found → 404
* reason=public route; deeplink UUID itself is capability token; no auth required per previous analysis

FILE: features/deeplinks/server/src/commonMain/kotlin/Plugin.kt (MODIFY scaffold stub)
* package=dev.inmo.wishlist.features.deeplinks.server
* setupDI: single { DeepLinksService(get(), get(), getAllDistinct()) }; single<DeepLinksFeature> { get<DeepLinksService>() }; singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { DeepLinksRoutingsConfigurator(get()) }
* imports=dev.inmo.micro_utils.koin.getAllDistinct; dev.inmo.micro_utils.koin.singleWithRandomQualifier; dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator

FILE: features/deeplinks/server/src/jvmMain/kotlin/repo/ExposedDeepLinksRepo.kt (NEW)
* package=dev.inmo.wishlist.features.deeplinks.server.repo
* pattern=ExposedKeyValueRepo<String,String>(database, keyCol={ text("deeplink_id") }, valueCol={ text("data_json") }, tableName="deeplinks").withMapper(...)
* mapper: keyFromToTo={ string }; valueFromToTo={ json.encodeToString(StoredDeepLink.serializer(), this) }; keyToToFrom={ DeepLinkId(this) }; valueToToFrom={ json.decodeFromString(StoredDeepLink.serializer(), this) }
* class ExposedDeepLinksRepo(database: Database, json: Json) : DeepLinksRepo, KeyValueRepo<DeepLinkId, StoredDeepLink> by createDelegate(database, json)
* reason=mirrors ExposedFilesMetaInfoRepo pattern; stores whole StoredDeepLink as JSON string; schema-flexible for opaque handlerInfo

FILE: features/deeplinks/server/src/jvmMain/kotlin/JVMPlugin.kt (MODIFY scaffold stub)
* setupDI: with(dev.inmo.wishlist.features.deeplinks.common.JVMPlugin) { setupDI(config) }; with(Plugin) { setupDI(config) }; single<DeepLinksRepo> { ExposedDeepLinksRepo(get(), get()) }
* startPlugin: delegates to common JVMPlugin + Plugin
* reason=JVM-only repo binding added here; Database+Json from features/common/server (transitive)

### 2c. features/deeplinks/server/build.gradle (MODIFY scaffold)

CHANGE: ensure commonMain deps include:
* api project(":wishlist.features.deeplinks.common")
* api project(":wishlist.features.common.server")
* NO extra dep needed: uuid4 transitive via common.common; getAllDistinct/singleWithRandomQualifier transitive via micro_utils; ExposedKeyValueRepo+withMapper transitive via common.server

### 2d. features/deeplinks/client/ — scaffold stubs only (NO logic)

FILES: Plugin.kt, JSPlugin.kt, JVMPlugin.kt, AndroidPlugin.kt
* content=empty delegating plugins (same pattern as sample feature client stubs)
* reason=PROMPT requirement "must not have any code (excluding template one) on client side"

### 2e. features/deeplinks/README.md (NEW)

* Required structure per ALL.md: title + Operator Notes (empty placeholder) + Overview + Routes + Models + Architecture Notes
* MUST be written by haiku agent (per agents/SHORTCUTS.md rule 4: "all fillings of documentations and other *.md files must be done with haiku agent")

---

## 3. BUILD.GRADLE CHANGES

### features/deeplinks/common/build.gradle
* apply from: "$mppJvmJsAndroid"
* plugins: kotlin.multiplatform, kotlin.plugin.serialization, com.android.library
* commonMain deps: api project(":wishlist.features.common.common")
* reason=matches sample/common pattern; provides DeepLinkId + Constants to all targets

### features/deeplinks/server/build.gradle
* apply from: "$mppJavaProject"
* plugins: kotlin.multiplatform, kotlin.plugin.serialization
* commonMain deps: api project(":wishlist.features.deeplinks.common"), api project(":wishlist.features.common.server")
* reason=JVM-only server module; no extra explicit deps (uuid transitive, exposed transitive)

### features/deeplinks/client/build.gradle
* apply from: "$mppJvmJsAndroidWithCompose"
* plugins: kotlin.multiplatform, kotlin.plugin.serialization, com.android.library, compose, kt.compose
* commonMain deps: api project(":wishlist.features.deeplinks.common"), api project(":wishlist.features.common.client")
* reason=scaffold template output; compose plugins already in scaffold even though no compose used (matches generate_feature.sh output pattern)

---

## 4. SETTINGS.GRADLE

FILE: settings.gradle (MODIFY)
* ADD after ":features:booking:client":
  ":features:deeplinks:common",
  ":features:deeplinks:server",
  ":features:deeplinks:client",
* reason=registers Gradle submodules with dotted name transformation

---

## 5. SERVER/BUILD.GRADLE

FILE: server/build.gradle (MODIFY)
* ADD in dependencies block: api project(":wishlist.features.deeplinks.server")
* reason=includes deeplinks.server in server fatjar + compileKotlin classpath

---

## 6. SERVER/SAMPLE.CONFIG.JSON

FILE: server/sample.config.json (MODIFY)
* ADD as last element in "plugins" array: "dev.inmo.wishlist.features.deeplinks.server.JVMPlugin"
* reason=deeplinks.server.JVMPlugin must load AFTER features/common/server (provides Database+Json); appending last satisfies ordering constraint

---

## 7. CLIENT WIRING (entry points)

FILE: client/build.gradle (MODIFY)
* ADD in commonMain dependencies: api project(":wishlist.features.deeplinks.client")
* reason=deeplinks.client module must be on classpath for client builds

FILE: client/src/jsMain/kotlin/Main.kt (MODIFY)
* ADD plugin: dev.inmo.wishlist.features.deeplinks.client.JSPlugin
* location=after booking.client.JSPlugin; before ui.sample.JSPlugin

FILE: client/src/jvmMain/kotlin/Main.kt (MODIFY)
* ADD plugin: dev.inmo.wishlist.features.deeplinks.client.JVMPlugin
* location=after booking.client.JVMPlugin

FILE: client/android/src/main/kotlin/MainActivity.kt (MODIFY)
* ADD plugin: dev.inmo.wishlist.features.deeplinks.client.AndroidPlugin
* location=after booking.client.AndroidPlugin

---

## 8. HANDLER-INFO POLYMORPHIC REGISTRATION (consuming feature guide)

RULE:
* consuming_feature→registers_handler_info_type: singleWithRandomQualifier<SerializersModule> { SerializersModule { polymorphic(Any::class, MyHandlerInfo::class, MyHandlerInfo.serializer()) } }
* consuming_feature→registers_handler: singleWithRandomQualifier<DeepLinkHandler> { MyHandler() }
* reason=shared Json in features/common/common aggregates all SerializersModule singletons via getAllDistinct() + combine(); useArrayPolymorphism=true carries type discriminator in [type,body] array format

---

## 9. DI WIRING SUMMARY

| Binding | Registered by | Collected by |
|---|---|---|
| DeepLinksRepo (JVM only) | deeplinks.server.JVMPlugin | DeepLinksService (constructor) |
| DeepLinksService | deeplinks.server.Plugin | — |
| DeepLinksFeature | deeplinks.server.Plugin | consuming features |
| ApplicationRoutingConfigurator.Element (routing) | deeplinks.server.Plugin | features/common/server getAllDistinct() |
| DeepLinkHandler (0..N) | consuming features (singleWithRandomQualifier) | DeepLinksService via getAllDistinct() |
| SerializersModule (handler-info types) | consuming features (singleWithRandomQualifier) | features/common/common Json constructor |

---

## 10. EXECUTION ORDER FOR CODING AGENT

1. Run: echo "deeplinks" | bash generate_feature.sh (creates scaffold)
2. Verify scaffold created (3 dirs + build.gradle + stubs)
3. Modify features/deeplinks/common/build.gradle
4. Create/modify files in features/deeplinks/common/src/
5. Modify features/deeplinks/server/build.gradle
6. Create files in features/deeplinks/server/src/
7. Modify features/deeplinks/client/build.gradle (verify Compose plugins present from scaffold)
8. Verify features/deeplinks/client/src/ stubs (scaffold output should be sufficient)
9. Modify settings.gradle (add 3 submodule entries)
10. Modify server/build.gradle (add deeplinks.server dep)
11. Modify server/sample.config.json (add JVMPlugin last)
12. Modify client/build.gradle (add deeplinks.client dep)
13. Modify client JS/JVM/Android entry points (add client plugins)
14. Run ast-index rebuild
15. Build: ./gradlew :wishlist.features.deeplinks.common:build :wishlist.features.deeplinks.server:build :wishlist.features.deeplinks.client:build
16. Build: ./gradlew :wishlist.server:compileKotlin
17. Delegate README.md creation to haiku agent
18. Write step report to agents/task/96242d00-9ca6-405c-b1de-a5f209c7d441/1-Coding.md

---

## 11. OPEN QUESTIONS / RISKS

RISK:
* risk_id=R1; description=scaffold output from generate_feature.sh may include generate UI scaffolding instead of feature scaffolding (script calls hierarchy_generator.kts from .templates/standard_module_kts); mitigation=verify created dirs match expected structure before proceeding; fallback=create build.gradle + stubs manually

RISK:
* risk_id=R2; description=client/build.gradle template output may NOT include compose plugins (depends on .templates/standard_module_kts content); mitigation=verify after scaffold; add manually if missing

RISK:
* risk_id=R3; description=PolymorphicSerializer(Any::class) requires consuming features to register their handler-info subtypes in shared Json's SerializersModule; if not registered → encode/decode throws SerializationException at runtime; mitigation=document in README.md Architecture Notes; not a blocking issue for this feature's code

UNCERTAINTY:
* missing=confirmation_that_deeplinks_route_should_be_outside_/api_prefix; description=SampleRoutingsConfigurator is registered as ApplicationRoutingConfigurator.Element under InternalApplicationRoutingConfigurator which wraps all routes under /api; if deeplinks route should be at GET /links/{id} (not /api/links/{id}) a different routing mechanism may be needed
* analysis=previous attempt used same ApplicationRoutingConfigurator.Element pattern placing route at /api/links/{deeplinkId}; PROMPT says "links/<deeplink_uuid>" without /api prefix; this ambiguity must be resolved
* recommendation=place under public routing (not authenticate block) using same ApplicationRoutingConfigurator.Element approach; final path will be /api/links/{id} unless a separate top-level routing hook exists; surface to orchestrator for decision

UNCERTAINTY:
* missing=PR_closure_reason_for_origin/issue/45-deeplinks; description=PR #48 was closed without merge; reason unknown; risk=previous implementation had a defect that caused rejection
* analysis=previous coding step report states "BUILD SUCCESSFUL" with no noted defects; likely closed for process/branch-management reasons rather than code quality
* recommendation=proceed with same approach; differences from fresh redo = run generate_feature.sh properly, verify scaffold output, write fresh README via haiku

VALIDATION:
* format_valid=true; no_pronouns=true; entities_explicit=true; high_density=true; causal_chain_present=true; ambiguity_detected=false
