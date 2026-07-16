Model: claude-fable-5 (fable)
Changed files: agents/task/14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b/002-architecturing.md

## Model Choice Argument

Per `agents/SHORTCUTS.md` the architecture role model priority is `fable` / `opus` / `sonnet`. The best-priority model, `fable`, was available and used: this session runs on `claude-fable-5`. No operator model override exists in `PROMPT.md`. Declared model matches actual runtime model.

## Input and Scope

Input: `001-planning.md` (complete repeat inventory D01–D14, within-file duplications W01–W03, defects X01–X02, minor repeat M01, canonical-home decisions, conflict winners). This step converts that inventory into an exact per-file edit specification the Coding stage applies mechanically. Every affected file was re-read in full in this session; all "Old text" quotes below are verbatim from current file content on `master` (working tree clean at session start). All edits are non-overlapping, so application order does not matter; the per-file order below is recommended.

Scope: 15 files edited, 35 discrete edits. `agents/local.ALL.md` and `agents/patterns/*` untouched (Planning: out of scope). No source code touched → no `ast-index rebuild` needed.

---

## Edit Specification (exact old → new)

Conventions: "Old text" is the exact current text and is unique within its file (verified). Operation `delete` removes the old text and collapses the surrounding blank lines to one. Operation `replace` swaps old text for new text verbatim. Operation `insert` gives an anchor and the text to add.

### 1. CLAUDE.md — 1 edit

**CL-1 (D10) — delete**

Old text (delete this line and the blank line before it; lines 1–3 of the file stay byte-identical):

```
YOU MUST CONTROL THAT ALL YOUR SUBAGENTS FOLLOWING THEIR INSTRUCTIONS IF OTHER IS NOT SAID IN USER PROMPT WITHOUT ANY EXCEPTIONS
```

New text: none (canonical merged wording lands in `agents/ORCHESTRATOR.md`, edit OR-7).

### 2. AGENTS.md — 1 edit

**AG-1 (D02) — replace** (precedence item 1 becomes self-contained; drops circular citation of TOOLS.md; carries GIT.md's "(not caveman-compressed)" strength qualifier):

Old text:

```
1. `agents/TOOLS.md` normal-prose requirements — step report narrative, operator questions, PR bodies, and commit messages are ALWAYS normal prose.
```

New text:

```
1. Normal-prose requirement — step report narrative, operator questions, PR bodies, and commit messages are ALWAYS normal prose, never caveman-compressed.
```

Items 2–3, the SCOPE paragraph, and the CRITICAL RULE paragraph stay unchanged (W03: accepted within-file redundancy).

### 3. agents/ALL.md — 5 edits

**AL-1 (M01) — replace** (trim Validator example; canonical example stays in `agents/VALIDATOR.md` Process section):

Old text:

```
(or the full step range the agent's role file specifies — e.g. the Validator reads all steps since the last validation)
```

New text:

```
(or the full step range the agent's role file specifies)
```

**AL-2 (D09) — insert** the new general edit-restriction rule between the write-duty line and the PROTOCOL pointer line.

Old text (anchor, replaced by New text):

```
THIS AGENT MUST WRITE `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` WITH RESULTS REPORT ABOUT ITS WORK.

See `agents/PROTOCOL.md` for TASK_ID_FORMAT and STEP_NUMBER_FORMAT specifications.
```

New text:

```
THIS AGENT MUST WRITE `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` WITH RESULTS REPORT ABOUT ITS WORK.

EVERY ROLE SUBAGENT MUST NOT EDIT ANY FILE except its current step file (`agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`). Exception: Coding may additionally edit the source, resource, and feature `README.md` files the task requires. (The Orchestrator/root additionally creates the task folder and `PROMPT.md` per `agents/PROTOCOL.md`.)

See `agents/PROTOCOL.md` for TASK_ID_FORMAT and STEP_NUMBER_FORMAT specifications.
```

**AL-3 (X01) — insert** the optional header fields note.

Old text (anchor, replaced by New text):

```
`Model` and `Changed files` are MANDATORY.
```

New text:

```
`Model` and `Changed files` are MANDATORY. Optionally add `Execution time` and `Tokens used` as rough estimates.
```

**AL-4 (D06) — replace** (mandate stays absolute when the tool is available; explicit fallback clause added; TOOLS pointer relabeled from "installation" to "installation and fallback"):

Old text:

```
ALWAYS USE `ast-index` for any code search/navigation (symbols, files, usages, hierarchy). NEVER use grep/find when `ast-index` can do the job. Read about how to use it in `agents/AST_INDEX.md`. See `agents/TOOLS.md` for installation instructions. This rule applies to ALL roles without exceptions.
```

New text:

```
ALWAYS USE `ast-index` for any code search/navigation (symbols, files, usages, hierarchy). NEVER use grep/find when `ast-index` can do the job. This rule applies to ALL roles without exceptions. If `ast-index` is unavailable, follow the installation and fallback procedure in `agents/TOOLS.md`. Command reference: `agents/AST_INDEX.md`.
```

**AL-5 (D07) — replace** (adopt the broader TOOLS.md exclusion list — conflict winner):

Old text:

```
Do NOT rebuild for markdown or step report changes.
```

New text:

```
Do NOT rebuild for markdown, step report, or config-only changes.
```

### 4. agents/SHORTCUTS.md — 2 edits

**SH-1 (D03/D05/D14) — replace** the embedded full root rule with a one-sentence pointer (rule relocates to ORCHESTRATOR.md, edit OR-1):

Old text:

```
**ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — subagents cannot spawn subagents. ROOT MUST NEVER DO ROLE WORK BY ITSELF — ONLY ORCHESTRATION: it receives the task, creates the task folder, places PROMPT.md, then spawns ONE role subagent per stage (planning → architecturing → coding → verification → validating) sequentially, reading the produced step file between stages. It must not perform planning, architecture, coding, verification, or validation itself.**
```

New text:

```
**ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — full root rule: `agents/ORCHESTRATOR.md`.**
```

**SH-2 (D08) — replace** (reading-order item stays as contract instantiation, gains rule pointer):

Old text:

```
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code
```

New text:

```
3. The feature's own `README.md` (especially `## Operator Notes`) before touching its code (rule: `agents/ALL.md`)
```

### 5. agents/ORCHESTRATOR.md — 7 edits

**OR-1 (D03/D14) — replace** line 1 with the canonical full root rule (text carried from the SHORTCUTS block at full strength; pointer direction reversed; D14 "never in parallel" strength from ISSUES_EXECUTION.md merged in):

Old text:

```
The Orchestrator is the MAIN SESSION (see the root rule in `agents/SHORTCUTS.md`) — it spawns one role subagent per stage and never does role work itself.
```

New text:

```
The Orchestrator (root) IS THE MAIN SESSION, NEVER A SUBAGENT — subagents cannot spawn subagents. ROOT MUST NEVER DO ROLE WORK BY ITSELF — ONLY ORCHESTRATION: it receives the task, creates the task folder, places `PROMPT.md`, then spawns ONE role subagent per stage (planning → architecturing → coding → verification → validating) SEQUENTIALLY — never in parallel — reading the produced step file between stages. It must not perform planning, architecture, coding, verification, or validation itself.
```

**OR-2 (D05) — replace** the bootstrap restatement with a single pointer line:

Old text:

```
Orchestrator generating a TASK_ID for current execution and creating folder for current task as described in `agents/PROTOCOL.md`.

Add `PROMPT.md` with source prompt or issue raw text there.
```

New text:

```
Bootstrap the task folder and `PROMPT.md` per `agents/PROTOCOL.md` "## Task Folder Bootstrap".
```

**OR-3 (W01) — replace** the Stage State Machine table with a merged table carrying a Duty column (duty text from the Roles Order items; Validating's restart clause stated once, in the On-failure column):

Old text:

```
| Stage | Entry condition | Exit condition (success) | On failure |
|-------|----------------|--------------------------|------------|
| 1. Planning | Task received | Plan + open questions resolved | Loop within Planning |
| 2. Architecturing | Plan complete | Architecture doc + test stubs written | Loop within Architecturing |
| 3. Coding | Architecture + test stubs ready | Code + tests implemented, committed | Loop within Coding |
| 4. Verification | Coding complete | Build passes, all tests pass | Return to Coding |
| 5. Validating | Verification passed | No High/Critical findings | Restart from Planning (stage 1) |
```

New text:

```
| Stage | Duty | Entry condition | Exit condition (success) | On failure |
|-------|------|-----------------|--------------------------|------------|
| 1. Planning | Make a plan of work; clarify requirements and architecture questions with operator before finalizing | Task received | Plan + open questions resolved | Loop within Planning |
| 2. Architecturing | Plan concrete code changes without actual coding; write test stubs/specs for every planned change | Plan complete | Architecture doc + test stubs written | Loop within Architecturing |
| 3. Coding | Create required structures and write code, guided by the test stubs from stage 2 | Architecture + test stubs ready | Code + tests implemented, committed | Loop within Coding |
| 4. Verification | Run build and tests; block handoff to Validating on any failure | Coding complete | Build passes, all tests pass | Return to Coding |
| 5. Validating | Check that each role did proper work | Verification passed | No High/Critical findings | Restart from Planning (stage 1) |
```

**OR-4 (W01) — delete** the whole Roles Order section (its duty text now lives in the table):

Old text (delete the section header, the list, and one surrounding blank line):

```
## Roles Order

1. **Planning** — make a plan of work; clarify requirements and architecture questions with operator before finalizing
2. **Architecturing** — plan concrete code changes without actual coding; write test stubs/specs for every planned change
3. **Coding** — create required structures and write code, guided by the test stubs from step 2
4. **Verification** — run build and tests; block handoff to Validating on any failure
5. **Validating** — check that each role did proper work; if High/Critical issues found → restart full cycle from Planning (stage 1)
```

New text: none.

**OR-5 (D11/D12) — replace** the enforcement restatement with an enforcement pointer:

Old text:

```
EACH STEP MUST BE FORCED TO MAKE REPORT ABOUT ITS RESULTS IN `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`. After each step its number increases by one (monotonically — never reset across restarts).
```

New text:

```
After each stage the Orchestrator verifies that the role wrote its step report (duty defined in `agents/ALL.md`); step numbering rules are in `agents/PROTOCOL.md`.
```

The following paragraph ("If some step has problems or other incompatibilities with real life — …") stays unchanged (L02: legitimate dual view).

**OR-6 (D12) — delete** (imperative no-wipe rule moves to PROTOCOL.md, edit PR-1):

Old text (delete this line and one surrounding blank line):

```
None of the steps must be wiped during work of some other step.
```

New text: none.

**OR-7 (D10 + D09) — replace** the Subagent Integrity Check paragraph: prepend the canonical control-subagents sentence (merged winner wording: "from the `agents` folder" + "without any exceptions"), and swap the inline restriction paraphrase for a pointer to the new ALL.md rule:

Old text:

```
After every role subagent completes, the Orchestrator MUST run `git status` and compare the result against the role's file-edit restriction (most roles may touch ONLY their step file; Coding may touch source files plus its step file). Any unexpected modified/deleted file → do NOT revert silently: record the violation in the next step file and ask the operator before restoring anything.
```

New text:

```
The Orchestrator MUST control that all spawned subagents follow their instructions from the `agents` folder, unless the user prompt says otherwise, without any exceptions.

After every role subagent completes, the Orchestrator MUST run `git status` and compare the result against the role's file-edit restriction (defined in `agents/ALL.md`). Any unexpected modified/deleted file → do NOT revert silently: record the violation in the next step file and ask the operator before restoring anything.
```

### 6. agents/PLAN.md — 1 edit

**PL-1 (D09) — delete** (rule generalized into `agents/ALL.md`, edit AL-2):

Old text (delete this line and the blank line after it; file then starts with "Plan stage contains several important substages:"):

```
THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`
```

New text: none.

### 7. agents/ARCHITECTURE.md — 2 edits

**AR-1 (D09) — delete**:

Old text (delete this line and the blank line after it; "# Architecture" then is followed directly by "## Test Planning Requirement"):

```
THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`
```

New text: none.

**AR-2 (D08 + D09 reference fix) — replace** the Feature README.md section intro: drop the read-before bullet (repeat of ALL.md), retarget the internal "restriction above" reference (the restriction line is deleted by AR-1), keep the two role-specific bullets:

Old text:

```
See `ALL.md` for the full rule. Short version:
- Read the feature's `README.md` (especially `## Operator Notes`) before making any architectural decisions.
- This agent MUST NOT edit the README itself (file-edit restriction above). Instead: write the intended `## Architecture Notes` delta into the current step report, in a dedicated `## README updates` section — design decisions and their rationale, constraints or invariants, dependency notes. The Coding agent applies that delta together with the code changes.
- Never specify changes to the `## Operator Notes` section.
```

New text:

```
See `ALL.md` for the full rule. Role-specific additions:
- This agent MUST NOT edit the README itself (file-edit restriction in `agents/ALL.md`). Instead: write the intended `## Architecture Notes` delta into the current step report, in a dedicated `## README updates` section — design decisions and their rationale, constraints or invariants, dependency notes. The Coding agent applies that delta together with the code changes.
- Never specify changes to the `## Operator Notes` section.
```

### 8. agents/CODING.md — 1 edit

**CO-1 (D08 + W02) — replace** the whole Feature README.md block: delete the read-before bullet and the Operator-Notes bullet (repeats of ALL.md), merge the update-README bullet with the AFTER ANY CODE CHANGE checklist into one subsection:

Old text:

```
See `ALL.md` for the full rule. Short version:
- Read the feature's `README.md` (especially `## Operator Notes`) before touching any code.
- After every coding session that changes a feature: update its `README.md` (routes, models, behavior, deps).
- Never modify the `## Operator Notes` section.

### AFTER ANY CODE CHANGE

Update the feature `README.md`:

- New/removed/changed routes (path, method, auth, behavior)
- Changed models or data types
- Changed ownership/auth semantics
- New dependencies between modules

Apply any README delta the Architecture step specified in its step report.
```

New text:

```
See `ALL.md` for the full rule. Role-specific duty:

### AFTER ANY CODE CHANGE

Update the feature `README.md`:

- New/removed/changed routes (path, method, auth, behavior)
- Changed models or data types
- Changed ownership/auth semantics
- New dependencies between modules

Apply any README delta the Architecture step specified in its step report.
```

### 9. agents/VERIFICATION.md — 1 edit

**VE-1 (D09) — delete**:

Old text (delete this line and the blank line after it; "# Verification" then is followed directly by "Verification runs after Coding…"):

```
THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`
```

New text: none.

The final line "(`Execution time` / `Tokens used` are optional rough estimates per `agents/ALL.md`.)" stays UNCHANGED — X01 fixes the dangling reference by adding the note to ALL.md (edit AL-3), making this pointer correct.

### 10. agents/VALIDATOR.md — 2 edits

**VA-1 (D09) — delete**:

Old text (delete this line and the blank line after it; file then starts with "Its main goal is to verify that:"):

```
THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`
```

New text: none.

**VA-2 (X02) — replace** ("stage" wins; "step" is reserved for step files):

Old text:

```
| **High** | Functional defect or architectural violation that breaks correctness | Restart full cycle from Planning (step 1) |
```

New text:

```
| **High** | Functional defect or architectural violation that breaks correctness | Restart full cycle from Planning (stage 1) |
```

### 11. agents/ISSUES_EXECUTION.md — 4 edits

**IS-1 (D01) — replace** (runbook step kept for ordering, annotated as pointer):

Old text:

```
1. Run `/caveman full`.
```

New text:

```
1. Run `/caveman full` (rule: `agents/ALL.md`).
```

**IS-2 (D13) — replace**:

Old text:

```
5. Sync with master: `git checkout master && git pull origin master`.
```

New text:

```
5. Sync with master per `agents/GIT.md` "## Before Branching".
```

**IS-3 (D03 + D05 + D10) — replace** step 7 (bootstrap/spawn details and control-subagents tail dissolve into ORCHESTRATOR.md):

Old text:

```
7. Resolve the issue: act as `root` (Orchestrator) yourself — the MAIN SESSION is the root role. Do NOT spawn a `root` subagent: subagents cannot spawn subagents, so a nested root could never spawn its stage subagents. Follow `agents/ORCHESTRATOR.md`: create the task folder + `PROMPT.md`, then spawn ONE role subagent per stage, sequentially. YOU MUST CONTROL THAT ALL SUBAGENTS FOLLOWING THEIR INSTRUCTIONS FROM `agents` FOLDER IF OTHER IS NOT SAID IN USER PROMPT
```

New text:

```
7. Resolve the issue: act as `root` (Orchestrator) yourself per `agents/ORCHESTRATOR.md` — the MAIN SESSION is the root role; do NOT spawn a `root` subagent.
```

**IS-4 (D14) — replace**:

Old text:

```
- One issue at a time. Role subagents run SEQUENTIALLY, never in parallel (avoid branch/index conflicts).
```

New text:

```
- One issue at a time. Stage subagents run sequentially per `agents/ORCHESTRATOR.md` (avoids branch/index conflicts).
```

### 12. agents/PROTOCOL.md — 2 edits

**PR-1 (D12) — replace** (imperative no-wipe rule, carried from ORCHESTRATOR.md OR-6 at full strength):

Old text:

```
**Numbering is monotonic and never resets** — even when the cycle restarts from Planning, the step number continues from the last used value. This ensures no step file is ever overwritten.
```

New text:

```
**Numbering is monotonic and never resets** — even when the cycle restarts from Planning, the step number continues from the last used value. This ensures no step file is ever overwritten. No agent may wipe, rename, or overwrite an existing step file.
```

**PR-2 (D04) — delete** the First-Run Rule section (canonical fallback clause stays at `agents/ALL.md` line 2):

Old text (delete the whole section including its header and one surrounding blank line):

```
## First-Run Rule

If an agent reads `agents/ALL.md` and finds no step files in the task folder, it must read `PROMPT.md` in that folder to get the task context.
```

New text: none.

### 13. agents/GIT.md — 1 edit

**GI-1 (D02) — replace**:

Old text:

```
- Use normal prose (not caveman-compressed).
```

New text:

```
- Normal prose per `AGENTS.md` "Communication Protocol Precedence".
```

### 14. agents/TOOLS.md — 4 edits

**TO-1 (D01) — replace** (start rule dissolves into ALL.md canonical; pre-installed fact stays):

Old text:

```
All agents start with `/caveman full`. Caveman is pre-installed with the project — no setup needed.
```

New text:

```
Caveman is pre-installed with the project — no setup needed (start rule: `agents/ALL.md`).
```

**TO-2 (D02) — replace** (scope statement becomes pointer; breaks the TOOLS→AGENTS→TOOLS circular attribution together with AG-1):

Old text:

```
Caveman mode applies to internal agent thinking and search only. Step reports, operator questions, PR bodies, and commit messages must be written in normal prose, not caveman-compressed.
```

New text:

```
Scope of caveman vs normal prose: see `AGENTS.md` "Communication Protocol Precedence".
```

**TO-3 (D06) — replace** (usage mandate dissolves into ALL.md canonical; pointer remains so the section is self-orienting):

Old text:

```
ALWAYS USE `ast-index` for any code search/navigation. See `agents/AST_INDEX.md` for command reference.
```

New text:

```
Usage mandate: `agents/ALL.md`. Command reference: `agents/AST_INDEX.md`.
```

**TO-4 (D07) — delete** the Rebuild Rule section (its broader exclusion list is adopted into ALL.md by AL-5):

Old text (delete the whole section including its header and one surrounding blank line):

```
### Rebuild Rule

Run `ast-index rebuild` only when source code files (.kt, .java, .ts, .js, etc.) have changed. Do NOT rebuild for markdown, step report, or config-only changes.
```

New text: none.

### 15. agents/AST_INDEX.md — 1 edit

**AS-1 (D06) — replace** (title loses directive status; conditional "if available" semantics now live in the ALL.md mandate + TOOLS.md fallback):

Old text:

```
# USE `ast-index` IF IT IS AVAILABLE IN CLASSPATH
```

New text:

```
# ast-index Command Reference
```

---

## Deviations From the Planning Spec (recorded, all Low severity, none blocking)

1. **OR-3 Duty text "step 2" → "stage 2"** — Planning's X02 treatment covered only VALIDATOR.md, but W01 rewrites the Roles Order text into the table anyway; carrying "step 2" would perpetuate the exact terminology collision X02 fixes. Changed while rewriting; semantics identical.
2. **AG-1 adds "never caveman-compressed"** — not in Planning's literal rewording note, but required by the no-weakening constraint: GI-1 deletes GIT.md's "(not caveman-compressed)" qualifier, so the canonical statement must absorb it (strongest-copy rule).
3. **Labels "Short version:" → "Role-specific additions:" / "Role-specific duty:"** (AR-2, CO-1) — after deleting the repeated bullets, "Short version" would be a false description; the new labels state what actually remains. No directive content affected.
4. **OR-1/OR-2 division of labor** — the relocated root rule keeps the clause "creates the task folder, places `PROMPT.md`" (WHAT the root does, verbatim strength from SHORTCUTS), while OR-2's single pointer line carries the procedure reference (HOW, canonical in PROTOCOL.md). This is Planning's D03+D05 treatments applied together; the in-file mention plus pointer is not a directive repeat because the bootstrap procedure itself is stated only in PROTOCOL.md.
5. **TO-3 leaves a pointer line instead of bare deletion** — Planning said "delete_mandate_sentence"; a naked "## ast-index → ### Installation" jump would leave the section without orientation. A pointer is the sanctioned end-state form per PROMPT.md ("other occurrences become short cross-references (pointers) or are removed").

## Delivery Check (AML-HIP)

ENTITY:
entity_id=DC01; type=delivery_check; name=post_edit_reading_chain_walk; state=passed

CONTEXT:

* task_id=14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b; agent_id=architecturing-002; memory_ref=[001-planning.md, all_15_files_reread_this_session]
* constraints=[no_directive_lost, no_directive_weakened, reading_order_delivery_preserved, CLAUDE.md_lines_1-3_unchanged]
* chain_definition=harness_injects_CLAUDE.md→AGENTS.md(mandatory_read)→agents/SHORTCUTS.md→agents/ALL.md+local.ALL.md→role_file→PROTOCOL/GIT/TOOLS/AST_INDEX_via_ALL.md_pointers

ACTION:

1. action=walk_role; target=root_orchestrator; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, ORCHESTRATOR.md, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01..D14_all]; delivered=[D01=ALL.md, D02=AGENTS.md, D03=ORCHESTRATOR.md(OR-1)+SHORTCUTS_pointer, D04=ALL.md:L2, D05=PROTOCOL.md_via_OR-2_pointer, D06+D07=ALL.md, D08=ALL.md, D09=ALL.md(AL-2)+OR-7_pointer, D10=ORCHESTRATOR.md(OR-7), D11=ALL.md:L4+OR-5_pointer, D12=PROTOCOL.md(PR-1)+OR-5_pointer, D13=GIT.md, D14=ORCHESTRATOR.md(OR-1)]; result=complete
2. action=walk_role; target=issue_executor; reads=[ISSUES_EXECUTION.md, CLAUDE.md, AGENTS.md, SHORTCUTS.md(step2_chain), ALL.md, ORCHESTRATOR.md(via_IS-3_pointer), GIT.md(via_IS-2_pointer+ALL.md_pointer), PROTOCOL.md]; needs=[D01, D02, D03, D05, D10, D13, D14+runbook]; delivered=[D01=ALL.md+IS-1_annotated_step, D02=AGENTS.md, D03=ORCHESTRATOR.md_via_IS-3, D05=PROTOCOL.md_via_ORCHESTRATOR_OR-2, D10=ORCHESTRATOR.md_OR-7_via_IS-3, D13=GIT.md_via_IS-2, D14=ORCHESTRATOR.md_OR-1_via_IS-4_pointer]; result=complete
3. action=walk_role; target=planning; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, PLAN.md, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01, D02, D04, D06, D08, D09, D11, D12, D13_commit_rules]; delivered=[D09=ALL.md_AL-2(replaces_deleted_PLAN.md:L1), rest=ALL.md/AGENTS.md/PROTOCOL.md/GIT.md_unchanged_homes]; result=complete
4. action=walk_role; target=coding; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, CODING.md+pattern_file, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01, D02, D04, D06, D07, D08+coding_specializations, D09_coding_exception, D11, D12, D13]; delivered=[D09_exception=ALL.md_AL-2_explicit_Coding_line(NEW_coverage—was_absent_before), D08_full=ALL.md+CO-1_role_duty, D07=ALL.md_AL-5, rest=unchanged_homes]; result=complete
5. action=walk_role; target=architecture; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, ARCHITECTURE.md, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01, D02, D04, D06, D08+arch_specializations, D09, D11, D12, D13]; delivered=[D09=ALL.md_AL-2(replaces_deleted_ARCHITECTURE.md:L3; AR-2_reference_retargeted), D08_specializations=AR-2_kept_bullets, rest=unchanged_homes]; result=complete
6. action=walk_role; target=verification; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, VERIFICATION.md, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01, D02, D04, D09, D11, D12, D13, X01_optional_fields]; delivered=[D09=ALL.md_AL-2(replaces_deleted_VERIFICATION.md:L3), X01=ALL.md_AL-3+VERIFICATION_pointer_now_resolves, rest=unchanged_homes]; result=complete
7. action=walk_role; target=validator; reads=[CLAUDE.md, AGENTS.md, SHORTCUTS.md, ALL.md, VALIDATOR.md, PROTOCOL.md, GIT.md, TOOLS.md]; needs=[D01, D02, D04, D09, D11, D12, D13, severity_definitions, step_range_rule]; delivered=[D09=ALL.md_AL-2(replaces_deleted_VALIDATOR.md:L1), step_range_rule=VALIDATOR.md_Process(canonical_after_AL-1_trim), severity=VALIDATOR.md_unchanged+CLAUDE.md:L3_pointer_intact, rest=unchanged_homes]; result=complete

VERIFICATION:

* check=each_deleted_occurrence_has_canonical_statement_on_consumer_reading_path; expected=true; actual=true(walks_1-7)
* check=no_directive_weakened(strongest_wording_carried: D02+GIT_qualifier, D06_absolute+fallback, D07_broader_exclusions, D10_merged_qualifiers, D12_imperative_added, D03_full_SHORTCUTS_text+D14_never_in_parallel); expected=true; actual=true
* check=no_new_directive_beyond_planning_approved(AL-2_D09_Coding_line=planning_prescribed); expected=true; actual=true
* check=CLAUDE.md_lines_1-3_byte_identical; expected=true; actual=true(CL-1_touches_only_line_5)
* check=ALL.md_pointer_lines_to_PROTOCOL/GIT/TOOLS_preserved; expected=true; actual=true(AL-2_anchor_preserves_them; AL-4_keeps_TOOLS+AST_INDEX_pointers)
* check=circular_attribution_TOOLS↔AGENTS_eliminated; expected=true; actual=true(AG-1_drops_TOOLS.md_citation; TO-2_becomes_one_way_pointer)
* check=SHORTCUTS_routing_table+model_priorities_untouched_except_root_bold_block_and_item3_annotation; expected=true; actual=true

UNCERTAINTY:

* missing=none; ambiguity=none_blocking(5_recorded_deviations_above=Low)

REPETITION OF RESULT:

* entity_id=DC01; stored_in=agents/task/14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b/002-architecturing.md; status=available_for_coding_stage

COMMUNICATION:

* sender=architecturing-002; receiver=coding-003; task_id=14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b; message_id=3d7a91f2-6c4e-4b8a-9e1d-2f8c5a0b7e64; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=step_file_only; index_keys=[task_id, DC01, D01-D14, W01-W02, X01-X02, M01, edit_ids_CL-1..AS-1]

## Test Planning Requirement — Untestable Functionality Flag

Per `agents/ARCHITECTURE.md` Test Planning Requirement: this task changes only markdown framework files — there is no compilable or automatically testable surface, so no automated test stubs are possible. This is flagged explicitly here as the rule requires. The operator-relayed task definition (PROMPT.md orchestrator notes + Planning handoff, uncontested by the operator at the Planning question gate) already prescribes the substitute: the textual verification checklist below is the "test stubs" deliverable for the Verification stage. Not blocking handoff to Coding; if the Orchestrator judges otherwise, relay to the operator before spawning Coding.

## Verification Checklist (the test stubs of this task — run by the Verification stage)

Run every command from the repo root (`/home/aleksey/projects/own/WishlistApp`). Each check lists the expected result; any mismatch = FAIL, hand back to Coding. Framework-file greps deliberately target `CLAUDE.md AGENTS.md agents/*.md` (non-recursive — excludes `agents/task/`, `agents/reports/`, `agents/patterns/`). Note for the Verification agent: these checks inspect markdown, which `ast-index` does not index — grep here is the correct tool, and there is no `./gradlew build` surface for this diff (no source files touched; running the build is optional sanity, expected unaffected).

C01 (D09 — exactly one normative edit-restriction statement):
```bash
grep -c "MUST NOT EDIT ANY FILE" CLAUDE.md AGENTS.md agents/*.md | grep -v ":0$"
```
Expected output exactly: `agents/ALL.md:1`

C02 (D01 — caveman start canonical in ALL.md; every other mention carries an ALL.md pointer):
```bash
grep -n -- "/caveman full" CLAUDE.md AGENTS.md agents/*.md
```
Expected: exactly 3 lines — `AGENTS.md` (contains ``per `agents/ALL.md` ``), `agents/ALL.md` ("Always starts with `/caveman full`" — the only line WITHOUT a pointer), `agents/ISSUES_EXECUTION.md` (contains ``(rule: `agents/ALL.md`)``). No match in `agents/TOOLS.md`.

C03 (D02 — prose rule canonical in AGENTS.md; circular citation gone):
```bash
grep -c "TOOLS.md" AGENTS.md; grep -in "normal prose" agents/GIT.md agents/TOOLS.md
```
Expected: first command prints `0`; second prints exactly 2 lines, each containing `AGENTS.md`.

C04 (D03 — root rule canonical in ORCHESTRATOR.md; others are pointers):
```bash
grep -n "MAIN SESSION" CLAUDE.md AGENTS.md agents/*.md
```
Expected: exactly 3 lines — `agents/ORCHESTRATOR.md` (canonical, contains "NEVER DO ROLE WORK"), `agents/SHORTCUTS.md` (contains ``full root rule: `agents/ORCHESTRATOR.md` ``), `agents/ISSUES_EXECUTION.md` (contains ``per `agents/ORCHESTRATOR.md` ``).

C05 (D04 — First-Run Rule section deleted; fallback clause lives only in ALL.md):
```bash
grep -c "First-Run Rule" agents/PROTOCOL.md; grep -c "read \`PROMPT.md\` in the task folder instead" agents/ALL.md
```
Expected: `0` then `1`.

C06 (D05 — bootstrap procedure only in PROTOCOL.md; ORCHESTRATOR points at it):
```bash
grep -rn "mkdir -p agents/task" CLAUDE.md AGENTS.md agents/*.md; grep -c "Task Folder Bootstrap" agents/ORCHESTRATOR.md
```
Expected: first prints exactly 1 line, in `agents/PROTOCOL.md`; second prints `1`.

C07 (D06 — single absolute mandate with fallback clause; AST_INDEX retitled; TOOLS demoted to pointer):
```bash
grep -c "NEVER use grep" CLAUDE.md AGENTS.md agents/*.md | grep -v ":0$"; head -1 agents/AST_INDEX.md; grep -c "ALWAYS USE" agents/TOOLS.md
```
Expected: `agents/ALL.md:1`, then `# ast-index Command Reference`, then `0`.

C08 (D06 — fallback clause present in the canonical mandate):
```bash
grep -c "installation and fallback procedure in \`agents/TOOLS.md\`" agents/ALL.md
```
Expected: `1`

C09 (D07 — rebuild rule once, with the broader exclusion list; TOOLS section gone):
```bash
grep -c "config-only changes" agents/ALL.md; grep -c "Rebuild Rule" agents/TOOLS.md; grep -c "markdown or step report changes" agents/ALL.md
```
Expected: `1`, `0`, `0`.

C10 (D08 — read-before/Operator-Notes bullets deduplicated out of role files):
```bash
grep -c "Operator Notes" agents/CODING.md; grep -c "before making any architectural decisions" agents/ARCHITECTURE.md; grep -c "before touching any code" agents/CODING.md
```
Expected: `0`, `0`, `0`.

C11 (D09 — new canonical rule text present in ALL.md, including the Coding exception):
```bash
grep -c "EVERY ROLE SUBAGENT MUST NOT EDIT ANY FILE" agents/ALL.md; grep -c "Exception: Coding may additionally edit" agents/ALL.md
```
Expected: `1` then `1`.

C12 (D10 — control-subagents rule exactly once, in ORCHESTRATOR.md):
```bash
grep -ic "control that all" CLAUDE.md; grep -ic "CONTROL THAT ALL" agents/ISSUES_EXECUTION.md; grep -c "MUST control that all spawned subagents" agents/ORCHESTRATOR.md
```
Expected: `0`, `0`, `1`.

C13 (D11 — write-duty canonical in ALL.md; ORCHESTRATOR keeps only the enforcement pointer):
```bash
grep -c "RESULTS REPORT ABOUT ITS WORK" agents/ALL.md; grep -c "FORCED TO MAKE REPORT" agents/ORCHESTRATOR.md; grep -c "duty defined in \`agents/ALL.md\`" agents/ORCHESTRATOR.md
```
Expected: `1`, `0`, `1`.

C14 (D12 — imperative no-wipe once, in PROTOCOL.md):
```bash
grep -c "wipe" agents/ORCHESTRATOR.md; grep -c "No agent may wipe, rename, or overwrite an existing step file" agents/PROTOCOL.md
```
Expected: `0` then `1`.

C15 (D13 — sync command only in GIT.md; ISSUES uses a pointer):
```bash
grep -rn "git checkout master && git pull" CLAUDE.md AGENTS.md agents/*.md; grep -c "Before Branching" agents/ISSUES_EXECUTION.md
```
Expected: first prints exactly 1 line, in `agents/GIT.md`; second prints `1`.

C16 (D14 — sequencing canonical in ORCHESTRATOR root rule; SHORTCUTS block dissolved):
```bash
grep -ic "sequential" agents/SHORTCUTS.md; grep -c "SEQUENTIALLY — never in parallel" agents/ORCHESTRATOR.md; grep -c "sequentially per \`agents/ORCHESTRATOR.md\`" agents/ISSUES_EXECUTION.md
```
Expected: `0`, `1`, `1`.

C17 (W01 — Roles Order merged away; restart rule once; Duty column present):
```bash
grep -c "Roles Order" agents/ORCHESTRATOR.md; grep -c "Restart from Planning (stage 1)" agents/ORCHESTRATOR.md; grep -c "| Stage | Duty |" agents/ORCHESTRATOR.md
```
Expected: `0`, `1`, `1`.

C18 (W02 — CODING README duty stated once):
```bash
grep -c "After every coding session" agents/CODING.md; grep -c "AFTER ANY CODE CHANGE" agents/CODING.md
```
Expected: `0` then `1`.

C19 (X01 — optional header fields defined in ALL.md; VERIFICATION pointer resolves):
```bash
grep -c "Execution time" agents/ALL.md; grep -c "Execution time" agents/VERIFICATION.md
```
Expected: `1` then `1`.

C20 (X02 + M01 — terminology fix and trimmed example):
```bash
grep -c "(step 1)" agents/VALIDATOR.md; grep -c "(stage 1)" agents/VALIDATOR.md; grep -c "Validator reads all steps" agents/ALL.md
```
Expected: `0`, `1`, `0`.

C21 (pointer integrity — every referenced framework file exists; local.* exempt as optional):
```bash
grep -rhoE '`agents/[A-Za-z_./-]+\.md`|`AGENTS\.md`|`CLAUDE\.md`' CLAUDE.md AGENTS.md agents/*.md | tr -d '`' | grep -v '/local\.' | grep -v '<' | sort -u | while read -r f; do [ -f "$f" ] || echo "MISSING: $f"; done
```
Expected: no output.

C22 (pointer integrity — every named section referenced by a pointer exists):
```bash
grep -c '^## Task Folder Bootstrap' agents/PROTOCOL.md; grep -c '^## Before Branching' agents/GIT.md; grep -c '^## Communication Protocol Precedence' AGENTS.md; grep -c '^## Subagent Integrity Check' agents/ORCHESTRATOR.md
```
Expected: `1` on all four lines.

C23 (preservation — CLAUDE.md mandate and ALL.md pointer lines intact):
```bash
sed -n '3p' CLAUDE.md | grep -c "ALWAYS START SESSION WITH READING"; grep -c 'See `agents/PROTOCOL.md` for TASK_ID_FORMAT' agents/ALL.md; grep -c 'See `agents/GIT.md` for all git commit and push rules' agents/ALL.md; grep -c 'See `agents/TOOLS.md`' agents/ALL.md
```
Expected: `1` on all four lines (last: at least `1`).

C24 (scope — only intended files changed, patterns/local untouched):
```bash
git status --porcelain -- agents/patterns/ agents/local.ALL.md agents/AST_INDEX.md agents/reports/; git diff HEAD --name-only
```
Expected: first command shows at most ` M agents/AST_INDEX.md` (title edit) and nothing for patterns/local/reports; second lists exactly the 15 files from the table below plus the Coding step file (before commit) and nothing else. After Coding's commit: `git status --porcelain` prints nothing.

## Files Coding Will Modify — Expected Diff Shape

| # | File | Edits | Expected shape |
|---|------|-------|----------------|
| 1 | `CLAUDE.md` | CL-1 | -2 lines (control-subagents line + blank); lines 1–3 untouched |
| 2 | `AGENTS.md` | AG-1 | 1 line reworded (precedence item 1) |
| 3 | `agents/ALL.md` | AL-1..AL-5 | 1 example trimmed, +1 paragraph (D09 rule), +1 sentence (optional fields), 1 line reworded (ast-index mandate), 1 sentence reworded (rebuild exclusions) |
| 4 | `agents/SHORTCUTS.md` | SH-1, SH-2 | root bold block shrunk to one-sentence pointer; item 3 gains pointer suffix |
| 5 | `agents/ORCHESTRATOR.md` | OR-1..OR-7 | top rewritten (full root rule + bootstrap pointer); table gains Duty column; Roles Order section removed; 2 paragraphs replaced by pointers; 1 line deleted; integrity section gains D10 sentence |
| 6 | `agents/PLAN.md` | PL-1 | -2 lines at top |
| 7 | `agents/ARCHITECTURE.md` | AR-1, AR-2 | -2 lines at top; README section: 1 bullet removed, label + reference retargeted |
| 8 | `agents/CODING.md` | CO-1 | README section: 3 bullets removed, merged into AFTER ANY CODE CHANGE subsection |
| 9 | `agents/VERIFICATION.md` | VE-1 | -2 lines at top |
| 10 | `agents/VALIDATOR.md` | VA-1, VA-2 | -2 lines at top; one table cell word swap |
| 11 | `agents/ISSUES_EXECUTION.md` | IS-1..IS-4 | 4 lines reworded to pointers (steps 1, 5, 7; rules bullet) |
| 12 | `agents/PROTOCOL.md` | PR-1, PR-2 | +1 sentence (no-wipe imperative); First-Run Rule section removed |
| 13 | `agents/GIT.md` | GI-1 | 1 bullet reworded to pointer |
| 14 | `agents/TOOLS.md` | TO-1..TO-4 | caveman section shrunk to fact+pointer; scope line → pointer; mandate line → pointer; Rebuild Rule section removed |
| 15 | `agents/AST_INDEX.md` | AS-1 | title line replaced |

Plus Coding's own step file `agents/task/14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b/003-coding.md` (committed together with the 15 files per `agents/GIT.md` Coding commit scope).

## README updates

None. No feature directory or feature `README.md` is affected by this task; the dedup targets are framework instruction files only.

## Handoff to Coding

- **Model: `sonnet`** — per Planning's recorded decision (coding role priority list first entry). SHORTCUTS.md item 4's haiku routing was argued inapplicable by Planning: this is not documentation *filling* but surgical refactoring of the framework's normative program, where an imprecise deletion silently weakens rules governing every future session. Do not re-litigate; record the model used honestly in the step file header.
- Apply the 35 edits above mechanically with exact-string edits. Every Old text block is unique in its file. Do not improvise wording; if any Old text fails to match the current file byte-for-byte, STOP, record the mismatch in the step file, and hand back to Architecturing instead of adapting the edit.
- Do NOT run `ast-index rebuild` (markdown-only change). Working on `master` directly per Planning's branch decision — do not create a branch, do not push (push stays with the Orchestrator).
- Commit BOTH the step report `003-coding.md` AND the 15 modified files in one commit, normal-prose message, ending with the `Co-Authored-By: Claude <noreply@anthropic.com>` line per `agents/GIT.md`.
- After editing, run checks C01–C24 yourself as a smoke pass (Verification will re-run them as the formal gate) and record the results in `003-coding.md`.

## QUESTIONS FOR OPERATOR

None blocking. Five Low-severity deviations from the literal Planning text are recorded above (section "Deviations From the Planning Spec") with rationale; none loses or weakens a directive, none adds a directive beyond what Planning approved. The untestable-functionality flag (docs-only task) is recorded above with the sanctioned verification substitute.
