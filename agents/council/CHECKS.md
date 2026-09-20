# Council Completion Checks

Preparation uses this reading and reasoning checklist before publishing the council outcome. It is not a script, executable fixture or parser. Read actual reports and referenced evidence; record observed results and cited paths in the final Preparation report. Hypothetical walkthroughs assess the instructions, while an executed council needs actual invocation/vote evidence. Do not claim a scenario was executed merely because the instructions describe it.

## Requirements and delivered result

- Map every prompt/issue/review requirement to planned changes and test specifications in the final Preparation report. State gaps explicitly; do not claim proposed implementation or tests have been executed.
- Trace **every** council comment, objection and note through candidate revisions and originating-role dispositions to the final implementation instructions. Nonblocking notes may remain only with an explicit disposition and rationale; no lost or administratively erased dissent.
- Compare the accepted candidate's implementation, test, research and README sections with the final Preparation report. Additional design decisions require another full vote.
- Confirm the final Preparation report contains complete implementation instructions without relying on council internals.
- Confirm the final report carries complete requirement and comment dispositions with evidence.
- Distinguish proposed test specifications from observed test execution; a passing build is not proof that every requirement or comment is satisfied.

## Process evidence

- Compare the actual full roles-directory contents and frozen instructions with the recorded roster. Every role must appear in proposals and **every voting cycle**, with no omitted, invented or duplicate voters.
- Read actual subagent identities and dispatch evidence: one fresh independent subagent per role, all launched as a parallel wave before waiting, no previous-cycle context reuse or sequential fallback. Inspect tool evidence for differing common packets, forbidden current-wave peer-output exposure and unexpected writes. Independent repository/task-history reads are allowed, not packet or isolation failures.
- Confirm all role models prefer HL and any ML fallback is justified; no LL council reasoning.
- Confirm each participant receives only council-shared instructions and its own role contract. No ordinary root-role file, other role contract or future-workflow disclosure is read, including copies in searches/history. Repository evidence cannot override this boundary.
- Confirm all participants can inspect repository source/configuration/documentation and permitted task history directly. Check cited paths/revisions and discovered evidence; do not require a separate architecture/context file or restrict reads to packet excerpts. Current-wave peer proposal/review/vote artifacts must be excluded even when reachable through history or searches.
- Check same-brief/cycle/candidate/issues identity for every vote. A copied response, impersonated role, missing role or altered immutable packet cannot count.
- Check that all accepting votes are unconditional, every blocking issue has originator-backed closure, revisions trigger all-role parallel re-voting, and the finite cycle budget is honored.
- Confirm the process needs only native agent/subagent/file/research tools; no repository script or executable evaluator controls decisions.
- Confirm separate final reports retain complete content and all historical task records remain untouched.

## Adversarial walkthroughs

| Situation | Required result |
|---|---|
| Every discovered role accepts the same unchanged candidate with no blockers | CONSENSUS; publish a self-contained Preparation result |
| Add a fifth role file before an attempt | Include the fifth role in proposals and every parallel vote without dispatcher edits |
| Add/remove/edit a role file between cycles or before final publication | New brief and full current-roster proposals; old votes cannot carry |
| A role has no domain-specific concern | Still invoke the role; require an explicit whole-plan vote |
| A role is missing, repeated, impersonated, timed out, or uses another brief/candidate/issues packet | PROCESS_FAILURE; blocked result |
| The first vote is DISAGREE but a later vote is invalid | Inspect the whole set; PROCESS_FAILURE, not an early successful retry path |
| A role says “agree after changing X” | DISAGREE; revise and re-invoke all roles in parallel |
| A DISAGREE omits the violated requirement/invariant, exact problem/failure mode, evidence, or correction/operator question | Invalid response; inspect the whole wave and record PROCESS_FAILURE, not a veto or implicit agreement |
| A participant receives root routing, another role's instructions or a report excerpt exposing future workflow | PROCESS_FAILURE; root must provide a correctly isolated invocation |
| Only disagreeing roles are re-polled after a revision | Invalid cycle; every role needs a fresh parallel invocation |
| Prior-cycle agent conversations are reused or current sibling votes leak | PROCESS_FAILURE; do not count contaminated votes |
| A participant reads repository files, ordinary task history or completed prior-cycle reports and cites new evidence | Allowed; Preparation reconciles the evidence without changing the active packet in place |
| A history/search result exposes another participant's current-wave proposal, review or vote | PROCESS_FAILURE; history access does not bypass peer isolation |
| The host offers fewer concurrent agents than the full roster | PROCESS_FAILURE; no sequential batches or simulated roles |
| HL unavailable but adequate ML available | Recorded ML fallback for that role; role remains in the wave |
| Only LL available | PROCESS_FAILURE; no omitted or LL voter |
| Candidate or issue contents are overwritten under the same path | PROCESS_FAILURE even if a new self-reported identity is supplied |
| Preparation changes accepted implementation text during final collation | New candidate and complete parallel vote required |
| A mitigation is marked resolved without each originator's acceptance | Remains blocking; record the defect |
| Precise external information is missing | NEEDS_INFORMATION with a specific question |
| Valid incompatible objections remain at the limit | IRRECONCILABLE; no forced unanimity |
| Build/tests pass but a requirement or council comment is unfulfilled | Unresolved requirement/comment; passing mechanical checks do not waive it |
| Old reports describe v1 scripts/reference seals | Preserve history; never use them as proof of a new v2 run |

Preparation records council outcomes and evidence, returns its allocated report and stops.
