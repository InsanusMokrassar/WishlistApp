# Agent-Performed Council Validation

This is a reading and reasoning checklist for the **Validation agent**, not a script, executable fixture, parser or extra Verification gate. Read the actual active reports and referenced evidence; record observed results and cited paths in the Validation report. Hypothetical walkthroughs assess the framework, while an executed council needs actual invocation/vote evidence. Do not claim a scenario was executed merely because the instructions describe it.

## Requirements and delivered result

- Map every prompt/issue/review requirement to the final Architecturing report, implemented changes and test evidence. State gaps explicitly.
- Trace **every** council comment, objection and note through candidate revisions and originating-role dispositions to the final implementation. Nonblocking notes may remain only with an explicit disposition and rationale; no lost or administratively erased dissent.
- Compare the accepted candidate's implementation, test, research and README sections with the final Architecturing report. Additional design decisions require another full vote.
- Confirm Coding can implement from the final Architecturing report without reading council internals.
- Keep project build/test execution in Verification. A passing build is not proof that requirements or comments were satisfied.

## Process evidence

- Compare the actual full roles-directory contents and frozen instructions with the recorded roster. Every role must appear in proposals and **every voting cycle**, with no omitted, invented or duplicate voters.
- Read actual subagent identities and dispatch evidence: one fresh independent subagent per role, all launched as a parallel wave before waiting, no previous-cycle context reuse or sequential fallback. Inspect tool evidence for differing packets, peer-output exposure and unexpected writes.
- Confirm all role models prefer HL and any ML fallback is justified; no LL council reasoning.
- Check same-brief/cycle/candidate/issues identity for every vote. A copied response, impersonated role, missing role or altered immutable packet cannot count.
- Check that all accepting votes are unconditional, every blocking issue has originator-backed closure, revisions trigger all-role parallel re-voting, and the finite cycle budget is honored.
- Confirm the process needs only native agent/subagent/file/research tools; no repository script or executable evaluator controls decisions.
- Confirm separate final reports retain complete content and all historical task records remain untouched.

## Adversarial walkthroughs

| Situation | Required result |
|---|---|
| Every discovered role accepts the same unchanged candidate with no blockers | CONSENSUS; publish a self-contained Architecturing result |
| Add a fifth role file before an attempt | Include the fifth role in proposals and every parallel vote without dispatcher edits |
| Add/remove/edit a role file between cycles or before final publication | New brief and full current-roster proposals; old votes cannot carry |
| A role has no domain-specific concern | Still invoke the role; require an explicit whole-plan vote |
| A role is missing, repeated, impersonated, timed out, or uses another brief/candidate/issues packet | PROCESS_FAILURE; no Coding |
| The first vote objects but a later vote is invalid | Inspect the whole set; PROCESS_FAILURE, not an early successful retry path |
| A role says “agree after changing X” | OBJECT; revise and re-invoke all roles in parallel |
| Only objectors are re-polled after a revision | Invalid cycle; every role needs a fresh parallel invocation |
| Prior-cycle agent conversations are reused or current sibling votes leak | PROCESS_FAILURE; do not count contaminated votes |
| The host offers fewer concurrent agents than the full roster | PROCESS_FAILURE; no sequential batches or simulated roles |
| HL unavailable but adequate ML available | Recorded ML fallback for that role; role remains in the wave |
| Only LL available | PROCESS_FAILURE; no omitted or LL voter |
| Candidate or issue contents are overwritten under the same path | PROCESS_FAILURE even if a new self-reported identity is supplied |
| Architecture changes accepted implementation text during final collation | New candidate and complete parallel vote required |
| A mitigation is marked resolved without each originator's acceptance | Remains blocking; Validation reports the defect |
| Precise external information is missing | NEEDS_INFORMATION with a specific question |
| Valid incompatible objections remain at the limit | IRRECONCILABLE; no forced unanimity |
| Build/tests pass but a requirement or council comment is unfulfilled | Validation finding; mechanical Verification does not waive it |
| Old reports describe v1 scripts/reference seals | Preserve history; never use them as proof of a new v2 run |

The Validator assigns severity and routes findings through [VALIDATOR](../VALIDATOR.md). Architecture records council outcomes and evidence; it does not replace this independent audit.
