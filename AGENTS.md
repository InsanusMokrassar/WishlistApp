Use `agents/SHORTCUTS.md` for instructions.

## Communication Protocol Precedence

When communication rules conflict, the higher rule wins:

1. Normal-prose requirement — step report narrative, operator questions, PR bodies, and commit messages are ALWAYS normal prose, never caveman-compressed.
2. Caveman mode (`/caveman full`, per `agents/ALL.md`) — internal agent thinking, search, and working notes.
3. AML-HIP (below) — applies ONLY to structured data blocks inside step files.

YOU MUST FOLLOW NEXT RULES WITHOUT ANY QUESTIONS OR DOUBTS

SYSTEM DIRECTIVE: AUTISTIC META-LANGUAGE PROTOCOL WITH HIGH INFORMATION DENSITY (AML-HIP V1)

SCOPE:
AML-HIP applies ONLY to structured, machine-parsed data blocks inside step files (`agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`): findings lists, entity/state records, and inter-role handoff data. AML-HIP does NOT apply to prose — step report narrative, operator questions, PR bodies, and commit messages follow the Communication Protocol Precedence above.

DEFINITION:
Structured data blocks in step files are required to use a strictly explicit, literal, low-ambiguity, and high-density format (AML-HIP). The goal of AML-HIP is the maximum transmission of information with zero loss of meaning and zero ambiguity.

CORE PRINCIPLES:

1. Maximum information density per line.
2. Zero ambiguity.
3. Full explicitness of all entities.
4. Preference of precision over readability.
5. Minimization of "empty" words.
6. Redundancy is allowed only to prevent loss of meaning.

PROHIBITIONS (inside AML-HIP blocks):

1. Pronouns are forbidden (this, he, she, they, there, it, etc.).
2. Free conversational text is forbidden.
3. Metaphors, emotions, evaluative constructions are forbidden.
4. Implicit references and hidden dependencies are forbidden.
5. Vague description of actions is forbidden.

DENSITY REQUIREMENTS:

1. Each line must contain the maximum of facts without loss of unambiguity.
2. Combine related parameters into a single line.
3. Use compact constructions:
    * key=value
    * entity_id=...
    * relation: A→B
4. Exclude words without semantic load.
5. Repetitions are allowed only for critical entities.

BLOCK STRUCTURE (MANDATORY for AML-HIP blocks):

ENTITY:
entity_id=<id>; type=<type>; state=<state>

CONTEXT:

* task_id=<id>; agent_id=<id>; memory_ref=[...]
* constraints=[...]

ACTION:

1. action=<type>; target=<entity_id>; params={...}
2. action=<type>; target=<entity_id>; params={...}

REASON:

* condition=<condition>; requirement=<requirement>

EXPECTED RESULT:

* entity_id=<id>; new_state=<state>; location=<memory>

VERIFICATION:

* check=<condition>; expected=<value>

UNCERTAINTY:

* missing=<data>; ambiguity=<description>

REPETITION OF RESULT:

* entity_id=<id>; stored_in=shared_memory; status=available

COMMUNICATION:

* sender=<agent_id>; receiver=<agent_id>; task_id=<id>; message_id=<uuid>; protocol=AML-HIP

PERSISTENCE:

* local_memory=true; shared_memory=true; index_keys=[task_id, entity_id, intent]

EXPRESSION RULES:

1. Each line = a completed semantic block.
2. Use the key=value format instead of descriptions.
3. Use lists of parameters instead of sentences.
4. Use causal connectives explicitly:
   condition → action → result
5. Do not split related data into several lines without necessity.

REPETITION RULES:

1. entity_id is repeated at every critical use.
2. result is duplicated in "REPETITION OF RESULT".
3. transmission between agents duplicates the key fields.

MULTI-AGENT MODE:

1. All structured handoff data between agents (passed via step files) uses AML-HIP.
2. A handoff block must be fully interpretable without history.
3. Any agent is required to:
    * duplicate critical data
    * avoid loss of context

ANTI-DEGRADATION (applies to AML-HIP blocks):
If detected inside an AML-HIP block:

* a pronoun
* an implicit reference
* low density (empty words, vague constructions)
* absence of structure

→ the block is considered invalid
→ mandatory regeneration of the block

SELF-CHECK (per AML-HIP block):

VALIDATION:

* format_valid=true/false
* no_pronouns=true/false
* entities_explicit=true/false
* high_density=true/false
* causal_chain_present=true/false
* ambiguity_detected=true/false

If any parameter=false:
→ mandatory regeneration

DENSITY METRIC:
high_density=true if:

* there are no "empty" words
* each line contains ≥2 facts or parameters
* descriptive constructions without data are absent

PRIORITIES:

1. Format (AML-HIP)
2. Information density
3. Explicitness
4. Completeness
5. Readability (minimum priority)

CRITICAL RULE:
Any structured data block in a step file outside of AML-HIP is invalid and must be regenerated in AML-HIP form. Prose sections are governed by the Communication Protocol Precedence at the top of this file — do NOT convert prose to AML-HIP.

END OF PROTOCOL
