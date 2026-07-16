Model: claude-fable-5 (fable)
Changed files: agents/task/14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b/001-planning.md

## Model Choice Argument

Per `agents/SHORTCUTS.md` the planning role model priority is `fable` / `opus` / `sonnet`. The best-priority model, `fable`, was available and was used: this session runs on `claude-fable-5`. No operator model override exists in `PROMPT.md`, so the priority list applies as written. Actual runtime model matches the declared model.

## Task Understanding

The operator asked (verbatim): "check current agents AI framework for repeats. The main task is to left each separated directive in one logical place."

Interpretation: this is a documentation-only refactor of the agents framework files. Every distinct directive (a normative rule an agent must follow) must end up with exactly one canonical statement in the one file where it logically belongs. Every other place that currently restates the same directive must either be deleted or reduced to a short cross-reference pointer to the canonical statement. Pointers are the desired end state, not repeats. Per-role specializations (a genuinely different rule that only applies to one role) are not repeats and stay in role files. Hard constraints: no directive may be lost or weakened; the reading-order contract must still deliver every rule to every role that needs it; `CLAUDE.md` must keep its mandatory-session-start behavior.

Files read in full for this audit: `CLAUDE.md`, `AGENTS.md`, `agents/ALL.md`, `agents/local.ALL.md`, `agents/SHORTCUTS.md`, `agents/ORCHESTRATOR.md`, `agents/PLAN.md`, `agents/ARCHITECTURE.md`, `agents/CODING.md`, `agents/VERIFICATION.md`, `agents/VALIDATOR.md`, `agents/ISSUES_EXECUTION.md`, `agents/PROTOCOL.md`, `agents/GIT.md`, `agents/TOOLS.md`, `agents/AST_INDEX.md`. All 7 files under `agents/patterns/` were skimmed for scope determination.

## Scope Decision: agents/patterns/*.md

The pattern files (`auth-ui.md`, `bearer-auth.md`, `crud-repo.md`, `full-stack-feature.md`, `local-storage.md`, `mvvm.md`, `server-routes.md`) are implementation walkthroughs, not framework directives. Each already opens with a pointer ("Read together with the hard rules in `agents/CODING.md`"), and none restates a framework directive — code snippets that happen to exemplify CODING.md rules (e.g. value-class naming in `crud-repo.md`) are instantiations, not directive copies. Decision: **patterns are OUT OF SCOPE** for the dedup edit. No changes to `agents/patterns/`.

`agents/local.ALL.md` contains one unique local directive (dev.inmo package sources location) with no duplicates. In scope but requires no change.

## Canonical-Home Principles (validated)

The Orchestrator's proposed placement principles were validated against the actual content and hold, with one adjustment:

- `agents/ALL.md` — rules binding every agent (read first by every role per SHORTCUTS). **Adjustment**: ALL.md also becomes the canonical home for tool *usage mandates* (ast-index use, rebuild trigger, caveman start), because ALL.md is the only guaranteed-read file; `TOOLS.md` is reached only through a pointer labeled "setup", so leaving behavioral mandates canonically in TOOLS.md would risk non-delivery.
- `agents/SHORTCUTS.md` — entry point, reading order, model priorities ONLY. The full root-role rule currently embedded there moves out.
- `agents/PROTOCOL.md` — task/step file formats, bootstrap, numbering invariants.
- `agents/GIT.md` — all git commit/push/branch rules.
- `agents/TOOLS.md` — tool installation, availability fallback, "pre-installed" facts. Not behavioral mandates.
- `agents/ORCHESTRATOR.md` — orchestration state machine, root-role definition, subagent enforcement duties.
- Role files (`PLAN.md`, `ARCHITECTURE.md`, `CODING.md`, `VERIFICATION.md`, `VALIDATOR.md`, `ISSUES_EXECUTION.md`) — only role-specific behavior.
- `AGENTS.md` — communication protocol precedence + AML-HIP protocol.
- `CLAUDE.md` — harness entry pointer + session-start mandate + violation-severity handling only.
- `agents/AST_INDEX.md` — pure command reference, no directives.

## Repeat Inventory (AML-HIP)

ENTITY:
entity_id=D01; type=directive; name=caveman_start_rule; state=repeated
entity_id=D02; type=directive; name=normal_prose_scope_rule; state=repeated+drifted
entity_id=D03; type=directive; name=root_main_session_rule; state=repeated+split
entity_id=D04; type=directive; name=prompt_md_first_run_fallback; state=repeated
entity_id=D05; type=directive; name=task_folder_bootstrap; state=repeated
entity_id=D06; type=directive; name=ast_index_usage_mandate; state=repeated+conflicting
entity_id=D07; type=directive; name=ast_index_rebuild_rule; state=repeated+drifted
entity_id=D08; type=directive; name=feature_readme_rule; state=repeated+role_specializations
entity_id=D09; type=directive; name=step_file_only_edit_restriction; state=repeated_x5
entity_id=D10; type=directive; name=control_subagents_rule; state=repeated+drifted
entity_id=D11; type=directive; name=step_report_write_duty; state=repeated_dual_view
entity_id=D12; type=directive; name=monotonic_numbering_no_wipe; state=repeated
entity_id=D13; type=directive; name=sync_master_before_branch; state=repeated_identical
entity_id=D14; type=directive; name=sequential_stage_execution; state=repeated
entity_id=W01; type=within_file_duplication; name=orchestrator_stage_list_x2; state=duplicated
entity_id=W02; type=within_file_duplication; name=coding_readme_update_x2; state=duplicated
entity_id=W03; type=within_file_duplication; name=agents_md_amlhip_scope_x3; state=accepted_redundancy
entity_id=X01; type=defect; name=verification_dangling_all_md_ref; state=stale_pointer
entity_id=X02; type=defect; name=stage_vs_step_terminology_drift; state=inconsistent_wording
entity_id=M01; type=minor_repeat; name=validator_step_range_example_in_all_md; state=repeated_example

CONTEXT:

* task_id=14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b; agent_id=planning-001; memory_ref=[PROMPT.md, all_framework_files_read_fully]
* constraints=[no_directive_lost, no_directive_weakened, reading_order_delivery_preserved, CLAUDE.md_session_start_intact, pointers_are_desired_end_state, patterns_out_of_scope]

DIRECTIVE RECORDS:

D01:
* occurrences=[agents/ALL.md:L23("Always starts with /caveman full"), agents/TOOLS.md:L5(sentence1 "All agents start with /caveman full"), agents/ISSUES_EXECUTION.md:L9(step1 "Run /caveman full"), AGENTS.md:L8(mention "per agents/ALL.md"=pointer_not_repeat)]
* variance=paraphrased_identical_meaning
* canonical_home=agents/ALL.md:L23; reason=behavioral_start_rule_for_every_agent+ALL.md=only_guaranteed_first_read
* treatment: agents/ALL.md:L23=keep_canonical; agents/TOOLS.md:L5=delete_start_sentence+keep_preinstalled_fact("Caveman is pre-installed — no setup needed")+add_pointer_to_ALL.md; agents/ISSUES_EXECUTION.md:L9=keep_step_for_runbook_ordering+annotate_as_pointer("(rule: agents/ALL.md)"); AGENTS.md:L8=keep_pointer

D02:
* occurrences=[AGENTS.md:L7(precedence_item1 normal-prose list), AGENTS.md:L8(precedence_item2 caveman_scope=internal_only), agents/TOOLS.md:L7(caveman_scope+prose_list restated), agents/GIT.md:L24("Use normal prose (not caveman-compressed)"), AGENTS.md:L16(SCOPE_paragraph restates prose_list=within_file), AGENTS.md:L156(CRITICAL_RULE restates=within_file)]
* variance=drifted; drift_detail: agents/TOOLS.md:L7 wording="Step reports ... must be written in normal prose"; AGENTS.md:L7 wording="step report narrative"; AGENTS.md wording=more_precise (AML-HIP blocks inside step files are NOT prose; "step report narrative" excludes AML-HIP blocks, "Step reports" wrongly implies whole file); winner=AGENTS.md wording
* conflict_detail: AGENTS.md:L7 currently cites "agents/TOOLS.md normal-prose requirements" as the rule source; agents/TOOLS.md:L7 simultaneously restates the rule → circular attribution; resolution=AGENTS.md precedence item1 becomes the self-contained canonical statement (drop citation of TOOLS.md); TOOLS.md becomes pointer
* canonical_home=AGENTS.md Communication Protocol Precedence (item1+item2); reason=principle "AGENTS.md = communication protocol precedence"; AGENTS.md=read_by_every_session_per_CLAUDE.md_mandate
* treatment: AGENTS.md:L7=keep_canonical+reword_to_remove_TOOLS.md_source_citation; agents/TOOLS.md:L7=replace_with_pointer("Scope of caveman vs prose: AGENTS.md Communication Protocol Precedence"); agents/GIT.md:L24=replace_with_pointer("Normal prose per AGENTS.md Communication Protocol Precedence"); AGENTS.md:L16+L156=keep (within-file protocol-internal redundancy, see W03)

D03:
* occurrences=[agents/SHORTCUTS.md:L7(bold block: root=main_session, never_subagent, subagents_cannot_spawn_subagents, never_role_work, creates_task_folder+PROMPT.md, spawns_one_role_subagent_per_stage_sequentially, reads_step_file_between_stages), agents/ORCHESTRATOR.md:L1(partial restatement+pointer_to_SHORTCUTS), agents/ISSUES_EXECUTION.md:L24(step7: act_as_root_yourself, do_NOT_spawn_root_subagent, subagents_cannot_spawn_subagents, create_task_folder+PROMPT.md, spawn_one_role_subagent_per_stage_sequentially)]
* variance=paraphrased; fullest_wording=agents/SHORTCUTS.md:L7 (contains rationale "subagents cannot spawn subagents" + between-stage step-file reads); winner=SHORTCUTS_block_text, relocated
* canonical_home=agents/ORCHESTRATOR.md (top section); reason=principle "ORCHESTRATOR.md = orchestration state machine"; SHORTCUTS=entry_point+reading_order_only; root_reads_ORCHESTRATOR.md_per_SHORTCUTS_routing; issue-executor_reads_ORCHESTRATOR.md_per_ISSUES step7_pointer
* treatment: agents/ORCHESTRATOR.md:L1=expand_into_canonical_full_root_rule(text from SHORTCUTS block, pointer_direction_reversed); agents/SHORTCUTS.md:L7=reduce_to_routing_line+one_sentence_pointer("ROOT IS THE MAIN SESSION, NEVER A SUBAGENT — full root rule: agents/ORCHESTRATOR.md"); agents/ISSUES_EXECUTION.md:L24=reduce_to("Resolve the issue: act as root (Orchestrator) yourself per agents/ORCHESTRATOR.md — the main session is the root role; do NOT spawn a root subagent")+delete_restated_bootstrap_and_spawn_details

D04:
* occurrences=[agents/ALL.md:L2("If no step files exist yet, read PROMPT.md in the task folder instead"), agents/PROTOCOL.md:L37-39(section "## First-Run Rule", identical semantics)]
* variance=paraphrased_identical_meaning
* canonical_home=agents/ALL.md:L2; reason=fallback_is_one_clause_of_the_read-latest-step_directive_at_ALL.md:L1; splitting_one_clause_into_PROTOCOL.md_forces_file_hop; ALL.md=guaranteed_read
* treatment: agents/ALL.md:L2=keep_canonical; agents/PROTOCOL.md:L37-39=delete_section

D05:
* occurrences=[agents/PROTOCOL.md:L30-35(section "## Task Folder Bootstrap": generate_TASK_ID, mkdir, write_PROMPT.md_with_source_prompt_or_issue_text), agents/ORCHESTRATOR.md:L3-5(TASK_ID_generation+folder "as described in agents/PROTOCOL.md" + "Add PROMPT.md with source prompt or issue raw text"), agents/SHORTCUTS.md:L7(root_block: "creates the task folder, places PROMPT.md"), agents/ISSUES_EXECUTION.md:L24(step7: "create the task folder + PROMPT.md")]
* variance=paraphrased_identical_meaning
* canonical_home=agents/PROTOCOL.md:L30-35; reason=principle "PROTOCOL.md = task/step file formats and bootstrap"
* treatment: agents/PROTOCOL.md:L30-35=keep_canonical; agents/ORCHESTRATOR.md:L3-5=merge_into_single_pointer_line("Bootstrap the task folder per agents/PROTOCOL.md 'Task Folder Bootstrap'"); agents/SHORTCUTS.md_occurrence=dissolves_via_D03_treatment; agents/ISSUES_EXECUTION.md_occurrence=dissolves_via_D03_treatment

D06:
* occurrences=[agents/ALL.md:L40(full mandate: ALWAYS_use+NEVER_grep_find+pointer_AST_INDEX.md+pointer_TOOLS.md+applies_to_ALL_roles), agents/TOOLS.md:L11("ALWAYS USE ast-index for any code search/navigation. See agents/AST_INDEX.md"), agents/AST_INDEX.md:L1(title "# USE ast-index IF IT IS AVAILABLE IN CLASSPATH")]
* variance=CONFLICTING; conflict_detail: agents/ALL.md:L40="NEVER use grep/find"+"ALL roles without exceptions"(absolute); agents/AST_INDEX.md:L1="if it is available in classpath"(conditional); agents/TOOLS.md:L15-25=fallback_procedure(ask_operator_before_install; operator_declines_or_install_fails→grep/find_fallback_allowed+record_in_step_report); three_statements_mutually_inconsistent_when_ast-index_unavailable
* conflict_resolution: composite_wins=mandate_when_available(agents/ALL.md wording)+explicit_fallback_reference(agents/TOOLS.md procedure); agents/AST_INDEX.md_title_loses_directive_status
* canonical_home=agents/ALL.md:L40(usage mandate, rewritten: ALWAYS use ast-index for code search/navigation; NEVER grep/find when ast-index can do the job; applies to all roles; if ast-index is unavailable follow installation/fallback in agents/TOOLS.md; command reference agents/AST_INDEX.md); fallback_procedure_canonical_home=agents/TOOLS.md Installation section(unchanged)
* treatment: agents/ALL.md:L40=keep_canonical+add_explicit_fallback_clause; agents/TOOLS.md:L11=delete_mandate_sentence(section keeps Installation+fallback only); agents/AST_INDEX.md:L1=retitle_to("# ast-index Command Reference")

D07:
* occurrences=[agents/ALL.md:L42(rebuild_on_source_change; NOT_for "markdown or step report changes"), agents/TOOLS.md:L27-29(section "### Rebuild Rule"; NOT_for "markdown, step report, or config-only changes")]
* variance=drifted; drift_detail: agents/TOOLS.md exclusion_list_broader(+config-only); winner=agents/TOOLS.md wording (deliberate refinement, more precise)
* canonical_home=agents/ALL.md:L42; reason=behavioral_mandate_for_every_role_that_changes_code; ALL.md=guaranteed_read; TOOLS.md_pointer_from_ALL.md_labeled_"setup"→rebuild_rule_left_only_in_TOOLS.md_risks_non-delivery(BREAKAGE_avoided)
* treatment: agents/ALL.md:L42=keep_canonical+adopt_TOOLS_wording("markdown, step report, or config-only changes"); agents/TOOLS.md:L27-29=delete_section

D08:
* occurrences=[agents/ALL.md:L46-87(full rule: README_mandatory_per_feature, read_before_work, Operator_Notes_immutable+operator_only, stop_and_ask_on_conflict, required_structure), agents/ARCHITECTURE.md:L13-18(pointer "See ALL.md for the full rule"+3 bullets), agents/CODING.md:L21-37(pointer "See ALL.md for the full rule"+3 bullets+section "### AFTER ANY CODE CHANGE"), agents/SHORTCUTS.md:L14(reading_order_item3: feature README before touching code)]
* variance=mixed; true_repeat_bullets=[ARCHITECTURE bullet"Read the feature's README ... before making any architectural decisions"(=ALL.md read-before rule), CODING bullet"Read the feature's README ... before touching any code"(=ALL.md read-before rule), CODING bullet"Never modify the ## Operator Notes section"(=ALL.md immutability rule)]; legitimate_specializations=[ARCHITECTURE: README-delta-written-into-step-report+Coding-applies-delta+delta-must-not-touch-Operator-Notes(role-specific because Architecture cannot edit files), CODING: update-README-after-code-change+apply-Architecture-delta(role-specific duty)]
* canonical_home=agents/ALL.md:L46-87; reason=rule_binds_every_agent; already_declared_canonical_by_both_role_files
* treatment: agents/ALL.md:L46-87=keep_canonical; agents/ARCHITECTURE.md:L13-18=keep_pointer_line+keep_delta-in-step-report_bullets(role-specific)+delete_read-before_bullet; agents/CODING.md:L21-37=keep_pointer_line+delete_read-before_bullet+delete_Operator-Notes_bullet+merge_remaining_update-README_bullet_with_"AFTER ANY CODE CHANGE"_checklist_into_one_subsection(see W02); agents/SHORTCUTS.md:L14=keep(reading-order_contract_instantiation)+append_pointer("(rule: agents/ALL.md)")

D09:
* occurrences=[agents/PLAN.md:L1, agents/ARCHITECTURE.md:L3, agents/VERIFICATION.md:L3, agents/VALIDATOR.md:L1(four_identical_sentences "THIS AGENT MUST NOT EDIT ANY FILE except agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md"), agents/ORCHESTRATOR.md:L41(paraphrase inside Subagent Integrity Check: "most roles may touch ONLY their step file; Coding may touch source files plus its step file")]
* variance=identical_x4+paraphrase_x1
* canonical_home=agents/ALL.md(new general rule); reason=rule_covers_4_of_5_roles+Coding_exception=one_general_rule_with_exception; ALL.md=guaranteed_first_read_by_every_role→salience_not_weakened; current_state_lacks_any_statement_for_Coding_scope_in_role_files→general_rule_adds_coverage
* canonical_wording_requirement: "Every role subagent MUST NOT edit any file except its current step file. Exception: Coding may additionally edit the source, resource, and feature README.md files the task requires. (Orchestrator/root additionally creates the task folder and PROMPT.md per agents/PROTOCOL.md.)"
* treatment: agents/ALL.md=add_canonical_rule; agents/PLAN.md:L1=delete; agents/ARCHITECTURE.md:L3=delete+fix_internal_reference_at_L17("file-edit restriction above"→"file-edit restriction in agents/ALL.md"); agents/VERIFICATION.md:L3=delete; agents/VALIDATOR.md:L1=delete; agents/ORCHESTRATOR.md:L41=keep_check_mechanics+replace_restriction_paraphrase_with_pointer("restriction defined in agents/ALL.md")

D10:
* occurrences=[CLAUDE.md:L5("YOU MUST CONTROL THAT ALL YOUR SUBAGENTS FOLLOWING THEIR INSTRUCTIONS IF OTHER IS NOT SAID IN USER PROMPT WITHOUT ANY EXCEPTIONS"), agents/ISSUES_EXECUTION.md:L24(tail: "YOU MUST CONTROL THAT ALL SUBAGENTS FOLLOWING THEIR INSTRUCTIONS FROM `agents` FOLDER IF OTHER IS NOT SAID IN USER PROMPT")]
* variance=drifted; drift_detail: ISSUES copy adds "FROM `agents` FOLDER"(more precise scope); CLAUDE copy adds "WITHOUT ANY EXCEPTIONS"(stronger); winner=merged_wording(both qualifiers kept)
* canonical_home=agents/ORCHESTRATOR.md(new sentence adjacent to Subagent Integrity Check); reason=directive_applies_only_to_spawning_agents(root, issue-executor); both_read_ORCHESTRATOR.md; principle "CLAUDE.md = harness entry pointer only"(operator-provided principle sanctions relocation); Subagent Integrity Check=the_enforcement_mechanism_of_the_same_duty→co-location
* canonical_wording_requirement: "The Orchestrator MUST control that all spawned subagents follow their instructions from the `agents` folder, unless the user prompt says otherwise, without any exceptions."
* treatment: agents/ORCHESTRATOR.md=add_canonical_sentence; CLAUDE.md:L5=delete; agents/ISSUES_EXECUTION.md:L24_tail=delete

D11:
* occurrences=[agents/ALL.md:L4("THIS AGENT MUST WRITE <step file> WITH RESULTS REPORT ABOUT ITS WORK"), agents/ORCHESTRATOR.md:L33("EACH STEP MUST BE FORCED TO MAKE REPORT ABOUT ITS RESULTS IN <step file>")]
* variance=dual_view(agent_duty vs orchestrator_enforcement); underlying_directive=identical
* canonical_home=agents/ALL.md:L4(agent duty); reason=every_agent_reads_ALL.md; enforcement_view_belongs_to_ORCHESTRATOR_as_a_check_not_a_second_statement
* treatment: agents/ALL.md:L4=keep_canonical; agents/ORCHESTRATOR.md:L33=reword_to_enforcement_pointer("After each stage the Orchestrator verifies the role wrote its step report (duty defined in agents/ALL.md); step numbering per agents/PROTOCOL.md")

D12:
* occurrences=[agents/PROTOCOL.md:L28(monotonic numbering, never resets, ensures no step file ever overwritten), agents/ORCHESTRATOR.md:L33(parenthetical "monotonically — never reset across restarts"), agents/ORCHESTRATOR.md:L37("None of the steps must be wiped during work of some other step")]
* variance=paraphrased; note: ORCHESTRATOR:L37=imperative_no-wipe_rule; PROTOCOL:L28=rationale_phrasing("ensures ... never overwritten"); imperative_form_must_not_be_lost
* canonical_home=agents/PROTOCOL.md STEP_NUMBER_FORMAT section; reason=numbering_invariant=file_format_concern
* treatment: agents/PROTOCOL.md:L28=keep_canonical+add_explicit_imperative("No agent may wipe, rename, or overwrite an existing step file"); agents/ORCHESTRATOR.md:L33_parenthetical=replace_with_pointer(merged into D11 treatment); agents/ORCHESTRATOR.md:L37=delete

D13:
* occurrences=[agents/GIT.md:L5-10(section "## Before Branching": git checkout master && git pull origin master), agents/ISSUES_EXECUTION.md:L22(step5: identical command)]
* variance=identical
* canonical_home=agents/GIT.md; reason=principle "GIT.md = all git rules"
* treatment: agents/GIT.md:L5-10=keep_canonical; agents/ISSUES_EXECUTION.md:L22=replace_with_pointer("Sync with master per agents/GIT.md '## Before Branching'")

D14:
* occurrences=[agents/SHORTCUTS.md:L7(root_block: "spawns ONE role subagent per stage ... sequentially"), agents/ISSUES_EXECUTION.md:L38("Role subagents run SEQUENTIALLY, never in parallel (avoid branch/index conflicts)")]
* variance=paraphrased
* canonical_home=agents/ORCHESTRATOR.md(inside relocated root rule, via D03); reason=sequencing=orchestration_behavior
* treatment: agents/SHORTCUTS.md_occurrence=dissolves_via_D03; agents/ISSUES_EXECUTION.md:L38=reduce_to("One issue at a time. Stage subagents run sequentially per agents/ORCHESTRATOR.md (avoids branch/index conflicts)")

W01:
* location=agents/ORCHESTRATOR.md:L7-15(table "## Stage State Machine") + agents/ORCHESTRATOR.md:L25-31(list "## Roles Order"); both enumerate 5 stages with descriptions; restart-on-High/Critical stated in both
* treatment=merge: single "## Stage State Machine" table gains a Duty column (text from Roles Order items); "## Roles Order" section deleted; restart_rule_stated_once_in_table

W02:
* location=agents/CODING.md:L21-37; bullet "After every coding session that changes a feature: update its README.md (routes, models, behavior, deps)" duplicates section "### AFTER ANY CODE CHANGE" checklist (same four items)
* treatment=merge_into_one_subsection: keep detailed checklist + "Apply any README delta the Architecture step specified" line; delete duplicate bullet (see D08 treatment)

W03:
* location=AGENTS.md:L7+L16+L156; prose-artifact list (step report narrative, operator questions, PR bodies, commit messages) appears 3x within one file
* treatment=keep_unchanged_except_D02_item1_rewording; reason: AML-HIP protocol internally permits redundancy to prevent loss of meaning; one file = one logical place; operator goal targets cross-file repeats; restructuring the AML-HIP protocol body carries risk without dedup benefit

X01:
* location=agents/VERIFICATION.md:L45("(`Execution time` / `Tokens used` are optional rough estimates per `agents/ALL.md`.)"); agents/ALL.md contains NO mention of Execution time or Tokens used → dangling reference; the optionality directive currently exists only in VERIFICATION.md with false attribution
* treatment: agents/ALL.md_header_spec(L29-36)=add_optional_fields_note("Optionally: `Execution time` and `Tokens used` as rough estimates"); agents/VERIFICATION.md:L45=keep_short_parenthetical(now-correct pointer) or delete; recommended=keep_pointer

X02:
* location=agents/VALIDATOR.md:L15(High action "Restart full cycle from Planning (step 1)") vs agents/ORCHESTRATOR.md:L15+L31("stage 1"); word "step" collides with step-file numbering terminology
* treatment: agents/VALIDATOR.md:L15=change "(step 1)"→"(stage 1)"; no other change; VALIDATOR_severity_table_row_actions=legitimate_dual_view_with_state_machine(kept, see L03)

M01:
* location=agents/ALL.md:L1 parenthetical example "(e.g. the Validator reads all steps since the last validation)" duplicates agents/VALIDATOR.md:L27 Process rule
* canonical_home=agents/VALIDATOR.md:L27
* treatment: agents/ALL.md:L1=trim_example_to("or the full step range the agent's role file specifies"); agents/VALIDATOR.md:L27=keep_canonical

VALIDATED NON-REPEATS (keep unchanged; classification=legitimate):
* L01: agents/GIT.md commit-scope-per-role (what to COMMIT) vs D09 edit restriction (what to EDIT) — two distinct directives; both kept in canonical homes (GIT.md / ALL.md)
* L02: agents/VERIFICATION.md fail→hand-back-to-Coding steps vs ORCHESTRATOR state machine "On failure: Return to Coding" — dual view (role duty vs orchestration transition); table cell required for state machine completeness
* L03: agents/VALIDATOR.md severity-action column vs ORCHESTRATOR restart/Medium rules — severity definitions canonical in VALIDATOR.md (CLAUDE.md points there); Medium action already pointer to ORCHESTRATOR.md Medium Findings Decision Rule; kept
* L04: gh-issue-comment escalation templates (ORCHESTRATOR cycle-limit "AGENT ESCALATION", PLAN "PLANNING QUESTIONS", ISSUES "AGENT BLOCKED", ISSUES 3-attempts "AGENT ESCALATION") — distinct message contracts per context; no-issue "## ESCALATION" fallback canonical in ORCHESTRATOR.md "Escalation Without a Linked Issue"; PLAN.md:L15+VALIDATOR.md:L16+L22 already pointer-style; kept
* L05: agents/CODING.md KDoc model priority (haiku/sonnet/opus) vs SHORTCUTS item4 (md fillings→haiku) vs role model priorities — three distinct scopes (KDoc subtask / md-file fillings / role sessions); kept
* L06: agents/SHORTCUTS.md:L10 coding bullet pattern-file clause — already pointer to CODING.md Pattern Library section; kept
* L07: agents/VERIFICATION.md step1 (read latest report + extract test cases) — role-specific specialization of ALL.md:L1; kept
* L08: agents/ISSUES_EXECUTION.md step8 push vs GIT.md push-exception — GIT.md already cross-references ISSUES_EXECUTION.md as the documented exception; kept
* L09: agents/PROTOCOL.md role-name list in STEP_NUMBER_FORMAT — instantiation with existing pointer ("role as named in ORCHESTRATOR.md"); kept
* L10: agents/patterns/*.md — out of scope (implementation walkthroughs; headers already point to CODING.md hard rules)

EXPECTED RESULT:

* entity_id=D01..D14+W01+W02+X01+X02+M01; new_state=each_directive_single_canonical_statement+pointers_elsewhere; location=agents_framework_files_per_treatment_lines
* changed_files_projection=[CLAUDE.md, AGENTS.md, agents/ALL.md, agents/SHORTCUTS.md, agents/ORCHESTRATOR.md, agents/PLAN.md, agents/ARCHITECTURE.md, agents/CODING.md, agents/VERIFICATION.md, agents/VALIDATOR.md, agents/ISSUES_EXECUTION.md, agents/PROTOCOL.md, agents/GIT.md, agents/TOOLS.md, agents/AST_INDEX.md]; unchanged=[agents/local.ALL.md, agents/patterns/*]

VERIFICATION:

* check=every_deleted_occurrence_has_canonical_statement_reachable_via_reading_order; expected=true
* check=ALL.md_pointer_lines_L6-L8_to_PROTOCOL/GIT/TOOLS_preserved; expected=true
* check=CLAUDE.md_lines_1-3_(session_start_mandate+severity_handling)_unchanged; expected=true
* check=no_new_circular_pointer(TOOLS→AGENTS→TOOLS eliminated by D02); expected=true
* check=grep_for_directive_keywords_after_edit_finds_exactly_one_normative_statement_per_directive; expected=true

UNCERTAINTY:

* missing=none_blocking; ambiguity=agents/ALL.md:L25("Always look at the project work rules first")=vague_unique_directive_with_unclear_referent; out_of_dedup_scope; recorded_as_observation_only

REPETITION OF RESULT:

* entity_id=D01..D14; stored_in=agents/task/14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b/001-planning.md; status=available_for_architecturing_stage

COMMUNICATION:

* sender=planning-001; receiver=architecturing-002; task_id=14.07.2026_11.59.13-33c1194b-bf4c-4eb0-af13-101a464f859b; message_id=8f4e2c1a-7b3d-4e9f-a2c6-5d0b9e8a1f47; protocol=AML-HIP

PERSISTENCE:

* local_memory=false; shared_memory=step_file_only; index_keys=[task_id, D01-D14, W01-W03, X01-X02, M01]

## Conflict Decisions Summary (prose)

Five places showed semantic drift between copies; the winning wording and reasoning:

1. **ast-index mandate (D06)** — `ALL.md` says "ALWAYS ... NEVER grep/find ... without exceptions"; `AST_INDEX.md`'s title says "if it is available in classpath"; `TOOLS.md` defines an operator-gated install and a grep/find fallback. These conflict when ast-index is absent. Winner: a composite — the `ALL.md` mandate stays absolute *when the tool is available*, with an explicit clause pointing to the `TOOLS.md` fallback procedure for unavailability. `AST_INDEX.md` stops carrying any directive (retitled to a pure command reference).
2. **ast-index rebuild exclusions (D07)** — `TOOLS.md`'s broader "markdown, step report, or config-only changes" wins over `ALL.md`'s "markdown or step report changes"; the config-only exclusion is a deliberate refinement and dropping it would reintroduce pointless rebuilds.
3. **Caveman/prose scope (D02)** — `AGENTS.md`'s "step report *narrative*" wins over `TOOLS.md`'s "Step reports": AML-HIP blocks inside step files are explicitly not prose, so "narrative" is the correct scope. Additionally, the current circular attribution (AGENTS.md cites TOOLS.md as the source of a rule TOOLS.md merely restates) is resolved by making the AGENTS.md precedence item self-contained.
4. **Control-subagents rule (D10)** — merged wording wins: keep "from the `agents` folder" (ISSUES copy, more precise) and "without any exceptions" (CLAUDE.md copy, stronger).
5. **"stage 1" vs "step 1" (X02)** — "stage" wins; "step" is reserved for step files, and `VALIDATOR.md`'s "(step 1)" could be misread as step file 001.

## Breakage Checks (reading-order delivery)

Delivery chain for every agent: harness-injected `CLAUDE.md` → mandatory `AGENTS.md` → `agents/SHORTCUTS.md` → `agents/ALL.md` (+ role file per routing) → `PROTOCOL.md`/`GIT.md`/`TOOLS.md`/`AST_INDEX.md` via ALL.md pointer lines (L6–L8, L40).

- Every canonical home chosen (`ALL.md`, `AGENTS.md`, `ORCHESTRATOR.md`, `PROTOCOL.md`, `GIT.md`, `TOOLS.md`, role files) is on the reading path of every role that needs the directive. No directive moves to a file outside any consumer's path.
- The deliberately avoided breakage: usage mandates (ast-index use, rebuild, caveman start) are kept canonical in `ALL.md`, NOT moved to `TOOLS.md`, because ALL.md's pointer to TOOLS.md is labeled "setup" and a role could legitimately skip it when the tool already works. `TOOLS.md` keeps only installation/fallback facts.
- Root rule and control-subagents rule move to `ORCHESTRATOR.md`: the only spawning agents (root, issue-executor) both read it — root via SHORTCUTS routing, issue-executor via the retained pointer in ISSUES step 7.
- Edit constraints for the Coding stage must preserve: ALL.md pointer lines L6–L8 and L40; the SHORTCUTS routing table; the one-line root pointer left in SHORTCUTS; the ISSUES step 7 pointer to ORCHESTRATOR.md; CLAUDE.md lines 1–3 verbatim.
- `CLAUDE.md` mandatory-session-start behavior remains intact (only line 5 moves, per operator-supplied principle "CLAUDE.md = harness entry pointer only").

## Coding-Stage Model Decision

Recommendation: **sonnet** (coding role priority list, first entry), NOT haiku despite SHORTCUTS item 4.

Reasoning: SHORTCUTS item 4 ("All fillings of documentations and other *.md files must be done with `haiku` agent") targets routine documentation *filling* — generating prose content for READMEs, KDoc-style text. This task is not filling: the markdown files ARE the framework's normative program, and the edit is surgical cross-file refactoring where an imprecise deletion silently weakens a rule that governs every future agent session. The task demands judgment about directive equivalence and exact preservation of pointer chains — failure modes are high-severity and hard to detect. The coding role priority (sonnet/opus/fable) is the governing rule for the coding *stage*; item 4 governs delegated doc-fill subtasks. Architecturing will produce an exact per-file edit spec, and sonnet executes it with the reliability this rule-bearing content requires.

## Branch Strategy Decision

Work **directly on `master`**, following repo precedent 497af56 ("improve (rework) agents framework" — committed straight to master) and the Orchestrator's default in PROMPT.md. The operator prompt contains no branch/PR requirement. Each stage commits its own scope per `agents/GIT.md`; the Orchestrator pushes after all stages complete. Not a blocking question — no operator escalation raised.

## QUESTIONS FOR OPERATOR

None. Substage 3 of the planning process produced **no unclear points**: the operator's goal (one logical place per directive), the scope (framework files; patterns excluded as walkthroughs), the constraints (no loss/weakening, reading-order preserved), the model choice, and the branch strategy are all resolvable from PROMPT.md, repo precedent, and the framework's own placement principles. This is recorded explicitly per `agents/PLAN.md` substage 4.

## Handoff to Architecturing

Turn the inventory above into an exact per-file edit specification. For each of the 15 files in the changed-files projection, specify the precise text to delete, the precise replacement/pointer text, and the canonical insertions (D09 general edit-restriction rule in ALL.md; D10 control sentence in ORCHESTRATOR.md; D03 relocated root rule in ORCHESTRATOR.md; X01 optional-header-fields note in ALL.md; D12 imperative no-wipe sentence in PROTOCOL.md). Honor every "must preserve" item in the Breakage Checks section. Note for the test-planning requirement: this is a documentation-only task with no compilable surface — the appropriate verification is textual (each directive greps to exactly one normative statement; all pointers resolve; build remains unaffected). Architecturing should flag the absence of automated tests explicitly per its untestable-functionality rule and propose the grep-based checklist as the verification substitute for the Verification stage.

## Observations (out of scope, no action in this task)

- `agents/ALL.md:L25` ("Always look at the project work rules first.") is vague and has no clear referent; unique, so not a dedup target. Candidate for a future clarification task.
- SHORTCUTS item 4's scope ("fillings of documentations") vs framework-rule editing could be clarified in a future task to prevent repeated model-choice deliberation.
