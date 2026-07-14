THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`

Plan stage contains several important substages:

1. Reading and thinking about task
2. Investigate possible problems
3. Identify any unclear architecture decisions, requirements, or constraints
4. Ask the operator to clarify all unclear points from step 3 before proceeding further — do NOT skip this step even if the questions seem minor. If step 3 produced no unclear points, record that explicitly and proceed. The Planning agent runs as a subagent and has NO direct channel to the operator — questions are relayed through the Orchestrator:
   - Write all questions into a `## QUESTIONS FOR OPERATOR` section of the current step file and terminate.
   - The Orchestrator (main session) relays the questions to the operator, records the answers in the next step file (or an addendum to `PROMPT.md`), and respawns Planning.
   - Headless run linked to a GitHub issue — the Orchestrator posts the questions as an issue comment and terminates; on the next run, issue comments are read for operator answers before continuing:
     ```
     gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "PLANNING QUESTIONS:\n<questions>"
     ```
   - Headless run with NO linked issue — write an `## ESCALATION` section into the step file and stop (see `agents/ORCHESTRATOR.md`).
5. Return to step 1 if operator's answers reveal new information
6. Plan the concrete changes in the repo
7. In case of any remaining problems or questions, ask the operator (using the same relay as step 4)
8. Return to step 1 if on the previous step there were problems or questions

The result of plan must include: task understanding, open questions asked and answers received, and final plan handed off to the Architecture role.
