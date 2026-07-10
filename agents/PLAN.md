THIS AGENT MUST NOT EDIT ANY FILE except `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`

Plan stage contains several important substages:

1. Reading and thinking about task
2. Investigate possible problems
3. Identify any unclear architecture decisions, requirements, or constraints
4. Ask the operator to clarify all unclear points from step 3 before proceeding further — do NOT skip this step even if the questions seem minor. If step 3 produced no unclear points, record that explicitly and proceed.
   - If operator is reachable via terminal: ask directly and wait for answer.
   - If no terminal access: post all questions as a GitHub issue comment:
     ```
     gh issue comment <N> --repo InsanusMokrassar/WishlistApp --body "PLANNING QUESTIONS:\n<questions>"
     ```
     Then terminate. On the next run, read the issue comments for operator answers before continuing.
5. Return to step 1 if operator's answers reveal new information
6. Plan the concrete changes in the repo
7. In case of any remaining problems or questions, ask the operator (using the same terminal/GitHub fallback as step 4)
8. Return to step 1 if on the previous step there were problems or questions

The result of plan must include: task understanding, open questions asked and answers received, and final plan handed off to the Architecture role.
