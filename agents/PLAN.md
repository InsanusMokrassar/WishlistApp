Plan stage contains several important substages:

1. Reading and thinking about task
2. Investigate possible problems
3. Identify any unclear architecture decisions, requirements, or constraints
4. Ask the operator to clarify all unclear points from step 3 before proceeding further — do NOT skip this step even if the questions seem minor. If step 3 produced no unclear points, record that explicitly and proceed. The Planning agent runs as a subagent and has NO direct channel to the operator — questions are relayed through the Orchestrator:
   - Write all questions into a `## QUESTIONS FOR OPERATOR` section of the current step file and terminate.
   - The Orchestrator relays questions via [Operator delivery and escalation](COUNCIL_ADAPTER.md#operator-delivery-and-escalation), records answers in a new immutable operator-input artifact, and respawns Planning. Never append to or overwrite `PROMPT.md`.
   - Headless and no-linked-issue delivery use the same adapter policy; Planning never sends external messages directly.
5. Return to step 1 if operator's answers reveal new information
6. Identify affected areas, evidence and credible alternatives without selecting the implementation design
7. In case of any remaining problems or questions, ask the operator (using the same relay as step 4)
8. Return to step 1 if on the previous step there were problems or questions

The accepted result contains task understanding, stable acceptance IDs, constraints, known facts, repository evidence/revision, questions and answers, remaining unknowns, excluded scope, and applicability/configuration recommendations. The Orchestrator transcribes those facts into a frozen council brief under [PROTOCOL](PROTOCOL.md). Planning must not preselect architecture or supply an expected specialist answer. Required current research enters the common evidence packet before freezing. Questions or genuinely missing material evidence stop the handoff; no-question cases are stated explicitly.
