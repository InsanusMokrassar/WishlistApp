Model: OpenAI GPT-6 (Codex; HL)
Changed files: agents/council/CHECKS.md, agents/task/20.09.2026_08.29.44-18128984-e5c6-4d02-bc47-7f401b72fbe7/007-coding.md

# Coding repair: V86-M1 reference identity and consent validation

## Authority, inputs, and scope

This is the bounded return to Coding allocated by the main Orchestrator following `005-validating.md` and `006-orchestrator.md`, not a fresh council attempt or a retrospective claim of CONSENSUS. The starting checkout was clean at `7b28ae0761f6d89abaa56127b254bb78d70857c9` on `fix/issue-86-universal-agent-council`, in `/tmp/wishlist-issue86-HBHeR8`. The existing OpenAI GPT-6 runtime satisfies the Coding role's bounded implementation capability; no alternate-provider runtime or delegated helper was used.

The complete applicable instructions were read: AGENTS, SHORTCUTS, ALL, MODELS, CODING, GIT, PROTOCOL, council CHECKS and README, and TOOLS. The active artifacts from PROMPT through 006-orchestrator were read. No applicable local override exists. The surgical-patch skill kept the change at the failing predicate layer with regression proof; repository caveman style applies only to internal working notes, while the report and commit use normal prose. No application implementation pattern applies. The workspace quota-preview command was unavailable, so no quota result is claimed.

Only the reference fixture inside CHECKS and this newly allocated report changed. Normative protocol text, prior reports, PROMPT, application files, feature READMEs, Operator Notes, and historical commits remain untouched. No runner, parser, dependency, or persistent test file was added. The accepted V86-L1 and V86-L2 historical exceptions in 006 remain unchanged.

## Reproduction and repair

Before editing, the complete original suite reported 94 passing replay checks. The exact three adversarial mutations from Validation then produced `CONSENSUS`, `CONSENSUS`, and `NEXT_ROUND`, respectively, confirming V86-M1 in the assigned starting tree.

The brief now has a content-bearing reference consisting of its logical record key and SHA-256 digest. That reference is shared by the common input manifest, proposals, and review/consent packet. The evaluator verifies that the required brief, plan, and candidate-ledger records exist, checks their local byte hashes, and binds the packet to the current brief reference, round, plan digest, and ledger digest. Recomputing a changed record's local hash no longer makes stale acceptance valid.

The consent loop now validates every required record's author, packet, review-set identity, resolution identity, verdict, actionability, and required WITHHOLD objection before selecting a disagreement outcome. Only after the full loop passes can any WITHHOLD produce NEXT_ROUND or IRRECONCILABLE. A later invalid record therefore takes precedence as PROCESS_FAILURE.

Eight regression cases were added: locally rehashed brief and candidate-ledger mutations, plus an initial valid WITHHOLD followed by impersonation at each of the three later consent positions under both remaining-budget and exhausted-budget configurations. Existing positive consensus, revised consensus, valid WITHHOLD, and all original negative cases remain in the suite and pass.

## Executed replay and exact adversarial probes

All commands below run from the checkout root. No temporary files are written.

The complete published replay command exited 0:

```sh
sed -n '/^```python$/,/^```$/p' agents/council/CHECKS.md | sed '1d;$d' | python3
```

Output: `102 replay assertions passed; D01-D10 and S01-S03 representative predicates`.

The following command reran the complete fixture and the three exact adversarial probes, with explicit required-result assertions. Exit 0; all three outcomes were PROCESS_FAILURE, followed by `3 exact adversarial probe assertions passed`.

```sh
python3 - <<'PY'
from pathlib import Path
import re
source = re.search(r'^```python\n(.*?)^```$', Path('agents/council/CHECKS.md').read_text(), re.M | re.S).group(1)
ns = {}
exec(compile(source, 'CHECKS.md:replay', 'exec'), ns)
case = ns['fixture']()
case['records']['issues']['bytes'] = 'different candidate ledger'
case['records']['issues']['sha256'] = ns['digest'](case['records']['issues']['bytes'])
assert ns['evaluate'](case) == 'PROCESS_FAILURE'
print('candidate-ledger probe: PROCESS_FAILURE')
case = ns['fixture']()
case['records']['brief']['bytes'] = 'different frozen brief'
case['records']['brief']['sha256'] = ns['digest'](case['records']['brief']['bytes'])
assert ns['evaluate'](case) == 'PROCESS_FAILURE'
print('frozen-brief probe: PROCESS_FAILURE')
case = ns['fixture']()
case['consents'][0].update(verdict='WITHHOLD', objection='new evidenced issue')
case['consents'][1]['writer'] = 'facilitator'
assert ns['evaluate'](case) == 'PROCESS_FAILURE'
print('WITHHOLD then impersonation probe: PROCESS_FAILURE')
print('3 exact adversarial probe assertions passed')
PY
```

The fixture's printed count is its established case counter, not the literal number of executed Python assertions: each `check` executes two assertions, and two grammar assertions are outside the counter. The following read-only instrumentation independently measured **173 executed Python assertions** in one complete replay, all passing. Exit 0; output includes `173 executed Python assert statements`.

```sh
python3 - <<'PY'
from pathlib import Path
import ast, re
source = re.search(r'^```python\n(.*?)^```$', Path('agents/council/CHECKS.md').read_text(), re.M | re.S).group(1)
class CountAssertions(ast.NodeTransformer):
    def visit_Assert(self, node):
        return [ast.copy_location(ast.Expr(ast.Call(ast.Name('count_assertion', ast.Load()), [], [])), node), node]
executed = []
ns = {'count_assertion': lambda: executed.append(1)}
exec(compile(ast.fix_missing_locations(CountAssertions().visit(ast.parse(source))), 'CHECKS.md:counted-replay', 'exec'), ns)
print(str(len(executed)) + ' executed Python assert statements')
PY
```

## Stage-scoped repository static checks

The immutable 003 checker is reused in memory. Its full baseline-to-current inventory check now explicitly allows the later allocated reports 004 through 007; no content or semantic assertion is removed. A separate repair-baseline check restricts this invocation to exactly the two authorized paths, and byte comparisons prove that PROMPT and reports 001 through 006 remain unchanged. This avoids interpreting later legitimate role reports as unauthorized original-Coding output while retaining the narrow repair boundary.

The following command exited 0 and printed `220 static assertions passed; S01-S05` and `9 repair-scope and immutable-artifact assertions passed; 1 harness assertion passed`.

```sh
python3 - <<'PY'
from pathlib import Path
import re, subprocess
folder = Path('agents/task/20.09.2026_08.29.44-18128984-e5c6-4d02-bc47-7f401b72fbe7')
source = re.search(r'^```python\n(.*?)^```$', (folder / '003-coding.md').read_text(), re.M | re.S).group(1)
marker = 'verify("S05 exact scope", (modified | untracked) <= allowed)'
assert source.count(marker) == 1
later = [str(folder / name) for name in ('004-verification.md', '005-validating.md', '006-orchestrator.md', '007-coding.md')]
source = source.replace(marker, 'allowed.update(' + repr(later) + ')\n' + marker)
exec(compile(source, 'stage-scoped-static-check', 'exec'), {})
baseline = '7b28ae0761f6d89abaa56127b254bb78d70857c9'
changed = set(subprocess.check_output(['git', 'diff', '--name-only', baseline]).decode().splitlines())
untracked = set(subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard']).decode().splitlines())
allowed = {'agents/council/CHECKS.md', str(folder / '007-coding.md')}
assert (changed | untracked) <= allowed
assert 'agents/council/CHECKS.md' in changed
for path in [folder / 'PROMPT.md', *sorted(folder.glob('00[1-6]-*.md'))]:
    assert path.read_bytes() == subprocess.check_output(['git', 'show', baseline + ':' + str(path)])
print('9 repair-scope and immutable-artifact assertions passed; 1 harness assertion passed')
PY
```

The static checker also contains one initial inventory assertion outside its printed 220 count. Consequently one complete replay, the three probe assertions, and one stage-scoped static execution total **407 actual successful Python assertions**: 173 + 3 + 221 + 9 + 1. Repeated suite execution is not counted again in that total.

## Whitespace, preservation, and handoff

The following whitespace commands passed with exit 0, including after staging both authorized files:

```sh
git diff --check
git diff --cached --check
git diff --check 7b28ae0761f6d89abaa56127b254bb78d70857c9
git diff --check bcb9636
```

The complete repair diff was inspected: only fixture identity binding, full consent validation, the eight regressions, and this report are present. Application compilation and ast-index rebuild are not applicable to this documentation-only change; no application symbol navigation was required. No feature README change is needed because no application behavior, API, ownership, or dependency changed.

V86-M1 is repaired at the reference predicate layer. Independent Verification and Validation remain required before completion or publication. The results establish the documented representative predicates and preservation properties, not arbitrary artifact parsing, hidden runner isolation, or model reasoning correctness. No push or external publication was performed.
