task_id=24-06-2026_15-10-08-b1b6b605-82ca-4f2a-bebe-95bedd5ee0e5; type=feature; branch_target=master

## SOURCE PROMPT (verbatim)

For master branch create github workflow to create release with:

* apk
* deb/rpm/exe/msi (what is possible)
* Chanelog

For changelog add `CHANGELOG.md`, add https://raw.githubusercontent.com/InsanusMokrassar/ktgbotapi/refs/heads/master/changelog_info_retriever in this project as script for reading of required changes. Increase current version and include changes since redesign.

## ORCHESTRATOR-DERIVED CONTEXT (facts)

fact: build_system=gradle(groovy); root_version_prop=gradle.properties:version=0.0.2; android_code=gradle.properties:android_code_version=2
fact: client_module=:wishlist.client (dir client/) KMP targets=jvm,js,android; convention=gradle/templates/mppJvmJsAndroidWithCompose.gradle
fact: android_apk_module=:wishlist.client.android (dir client/android/) applicationId=dev.inmo.wishlist.client
fact: desktop_entrypoint=client/src/jvmMain/kotlin/Main.kt; signature=`suspend fun main()`; package=dev.inmo.wishlist.client; mainClass=dev.inmo.wishlist.client.MainKt
fact: NO compose.desktop.application{} block exists anywhere → native distribution (deb/rpm/msi/exe) NOT yet configured; coding MUST add it
fact: compose_desktop_formats: Linux→Deb,Rpm ; Windows→Msi,Exe ; macOS→Dmg,Pkg . jpackage requires building each format on its native OS → GH workflow needs OS matrix (ubuntu + windows minimum)
fact: apk_task=:wishlist.client.android:assembleRelease (or assembleDebug); output client/android/build/outputs/apk/
fact: changelog_retriever_url=https://raw.githubusercontent.com/InsanusMokrassar/ktgbotapi/refs/heads/master/changelog_info_retriever
fact: retriever_behavior: bash; args=($1=version $2=file); scans file until line matches `^#+ <version>`, then prints lines until next `^#+` heading → emits that version section body. Use to extract release notes from CHANGELOG.md.
fact: redesign_point=commit 515308f (Merge PR#52 redesign/calm-studio); changes_since = `git log 515308f..HEAD` (44 commits, curate user-facing only)
fact: no CHANGELOG.md exists yet
constraint: target branch=master; workflow triggers on release/tag for master
constraint: all *.md content authoring MUST be done by haiku agent (SHORTCUTS.md rule 4)
constraint: each role commits its own changed files (ALL.md Git Commit Rule); never stage agents/task/; do NOT push (orchestrator pushes)
