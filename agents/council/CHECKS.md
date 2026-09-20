# Council Contract Checks

## Reproducible reference predicate fixture

The following standard-library Python fixture evaluates representative positive and mutated records. It is deliberately not a runner or parser for arbitrary artifact prose. Full validation additionally checks actual schemas, provenance, launch/receipt evidence, allocation chronology, and the static scenarios above. Adapter checks supply concrete preservation baselines and local serialization. Run the fenced block in memory with Python 3; any failed assertion exits nonzero. SEALING and NEXT_ROUND are nonterminal test outcomes and never enable Coding.

```python
# Standard-library reference predicate tests; no runner invocation or repository writes.
from copy import deepcopy
from hashlib import sha256
import re

ROLES = ("architect", "programmer", "security", "designer")
IMPACTS = ("interfaces", "api", "data", "authentication", "authorization", "ui",
           "migration", "rollout", "rollback", "files", "order", "tests",
           "rationale", "alternatives", "assumptions", "confidence", "dissent")
def digest(value):
    return sha256(value.encode("utf-8")).hexdigest()

def fixture(root="artifacts", round_number=1):
    plan = "accepted implementation with mapped tests"
    brief = ("brief", digest("frozen requirements"))
    common = (brief, "instructions", "planning", "source-snapshot")
    packet = (brief, round_number, digest(plan), digest("candidate issues"))
    review_set = digest("reviews-" + repr(packet))
    resolution = digest("resolution-" + repr(packet))
    records = {
        "brief": {"bytes": "frozen requirements", "sha256": digest("frozen requirements")},
        "plan": {"bytes": plan, "sha256": digest(plan)},
        "issues": {"bytes": "candidate issues", "sha256": digest("candidate issues")},
    }
    return dict(root=root, version="council-v1", active_version="council-v1",
        required=list(ROLES), configured=list(ROLES), contracts=list(ROLES),
        omissions={}, limit=3, round=round_number, terminal_before=False,
        allocations=[(i, "writer-" + str(i)) for i in range(1, 21)],
        reserved_gaps=[], records=records, references=list(records),
        common=common, proposals={r: dict(brief=brief, common=common,
            actual=common, reads=common, receipt=True, writer=r) for r in ROLES},
        packet=packet, review_set=review_set, resolution=resolution,
        reviews=[dict(role=r, writer=r, packet=packet, verdict="AGREE",
            malformed=False) for r in ROLES],
        consents=[dict(role=r, writer=r, packet=packet, reviews=review_set,
            resolution=resolution, verdict="CONSENT", actionable=True)
            for r in ROLES],
        issues=[], effect_areas=list(IMPACTS), ambiguous=False,
        provenance=True, aggregate=True, seal=True, seal_plan=digest(plan),
        seal_extra_instructions=False, missing_fact=None, all_rounds=list(range(1, round_number+1)),
        read_phases=[("review", 1, "proposal"), ("consent", 1, "current-reviews")],
        artifacts_before_previous_validation=[], transport="local")

def evaluate(t):
    fail = "PROCESS_FAILURE"
    required = set(t["required"])
    if (t["version"] != t["active_version"] or t["terminal_before"]
        or not required or len(required) != len(t["required"])
        or required != set(t["configured"]) or not required <= set(t["contracts"])
        or not isinstance(t["limit"], int) or t["limit"] <= 0
        or t["round"] < 1 or t["round"] > t["limit"]
        or t["all_rounds"] != list(range(1, t["round"] + 1))):
        return fail
    for role in set(ROLES) - required:
        if not t["omissions"].get(role):
            return fail
    numbers = [n for n, writer in t["allocations"]]
    if (numbers != sorted(set(numbers)) or not numbers or min(numbers) < 1
        or max(numbers) > 999 or any(not writer for n, writer in t["allocations"])
        or set(t["reserved_gaps"]) & set(numbers)):
        return fail
    missing = set(range(1, max(numbers)+1)) - set(numbers)
    if missing != set(t["reserved_gaps"]):
        return fail
    for record in t["records"].values():
        if set(record) != {"bytes", "sha256"} or digest(record["bytes"]) != record["sha256"]:
            return fail
    if not set(t["references"]) <= set(t["records"]):
        return fail
    if not {"brief", "plan", "issues"} <= set(t["records"]):
        return fail
    brief = ("brief", t["records"]["brief"]["sha256"])
    packet = t["packet"]
    if (brief not in t["common"] or packet != (brief, t["round"],
            t["records"]["plan"]["sha256"], t["records"]["issues"]["sha256"])):
        return fail
    if set(t["proposals"]) != required:
        return fail
    for role, p in t["proposals"].items():
        if (p["writer"] != role or p["brief"] != t["packet"][0]
            or not p["receipt"] or p["common"] != t["common"]
            or p["actual"] != t["common"] or not set(p["reads"]) <= set(t["common"])):
            return fail
    for phase, round_number, read in t["read_phases"]:
        if phase == "blind" and read != "common":
            return fail
        if phase == "review" and read == "current-reviews":
            return fail
        if read == "previous-reviews" and round_number <= 1:
            return fail
    if any(not item["isolation"] for item in t["artifacts_before_previous_validation"]):
        return fail
    if t["missing_fact"]:
        return "NEEDS_INFORMATION"
    reviews = t["reviews"]
    if (len(reviews) != len(required) or {r["role"] for r in reviews} != required
        or any(r["writer"] != r["role"] or r["packet"] != packet or r["malformed"]
               or r["verdict"] not in ("AGREE", "AGREE_WITH_NOTES", "OBJECT") for r in reviews)):
        return fail
    blockers = False
    for issue in t["issues"]:
        if not all(issue.get(field) for field in ("id","origins","requirement","failure","evidence","mitigation")):
            return fail
        if not set(issue["origins"]) <= required:
            return fail
        if issue["status"] == "RESOLVED":
            if set(issue["closures"]) != set(issue["origins"]):
                return fail
        elif issue["blocking"]:
            blockers = True
    objections = any(r["verdict"] == "OBJECT" for r in reviews)
    if blockers or objections:
        return "IRRECONCILABLE" if t["round"] == t["limit"] else "NEXT_ROUND"
    if set(t["effect_areas"]) != set(IMPACTS) or t["ambiguous"] or not t["provenance"]:
        return fail
    consents = t["consents"]
    if len(consents) != len(required) or {c["role"] for c in consents} != required:
        return fail
    for c in consents:
        if (c["writer"] != c["role"] or c["packet"] != packet
            or c["reviews"] != t["review_set"] or c["resolution"] != t["resolution"]
            or c["verdict"] not in ("CONSENT", "WITHHOLD") or not c["actionable"]):
            return fail
        if c["verdict"] == "WITHHOLD" and not c.get("objection"):
            return fail
    if any(c["verdict"] == "WITHHOLD" for c in consents):
        return "IRRECONCILABLE" if t["round"] == t["limit"] else "NEXT_ROUND"
    if not t["aggregate"]:
        return fail
    if not t["seal"]:
        return "SEALING"
    if t["seal_plan"] != packet[2] or t["seal_extra_instructions"]:
        return fail
    return "CONSENSUS"

count = 0
def check(label, t, expected):
    global count
    actual = evaluate(t)
    assert actual == expected, (label, actual, expected)
    coding_allowed = actual == "CONSENSUS" and t["seal"]
    assert coding_allowed == (expected == "CONSENSUS")
    count += 1

def mutation(label, field, value, expected="PROCESS_FAILURE", base=None):
    t = deepcopy(base or fixture())
    t[field] = value
    check(label, t, expected)

check("D01 good", fixture(), "CONSENSUS")
mutation("D01 before seal", "seal", False, "SEALING")
check("S01 relocated evidence root", fixture("another/project/decisions"), "CONSENSUS")
t = fixture(round_number=2)
t["issues"] = [dict(id="leak", origins=["security"], requirement="privacy",
    failure="secret exposure", evidence="source snapshot", mitigation="redact",
    blocking=True, status="RESOLVED", closures=["security"])]
check("D02 revised and originator closed", t, "CONSENSUS")
mutation("D02 missing fresh review", "reviews", t["reviews"][:-1], base=t)
stale = deepcopy(t); stale["consents"][0]["packet"] = fixture()["packet"]
check("D02 stale consent", stale, "PROCESS_FAILURE")
stale = deepcopy(t); stale["reviews"][0]["packet"] = fixture()["packet"]
check("D02 stale review", stale, "PROCESS_FAILURE")
for flow in ("history", "latest-step", "diff", "index", "memory", "private-message", "live-web"):
    broken = fixture(); broken["proposals"]["security"]["reads"] += (flow,)
    check("D03 forbidden " + flow, broken, "PROCESS_FAILURE")
broken = fixture(); broken["proposals"]["architect"]["actual"] += ("different digest",)
check("D03 changed common manifest", broken, "PROCESS_FAILURE")
broken = fixture(); broken["proposals"]["architect"]["receipt"] = False
check("D03 missing receipt", broken, "PROCESS_FAILURE")
for launch_mode in ("sequential-clean", "parallel-isolated"):
    check("D03 " + launch_mode, fixture(), "CONSENSUS")
mutation("D03 sibling review", "read_phases", [("review", 1, "current-reviews")])
check("D03 prior review allowed", dict(fixture(round_number=2),
      read_phases=[("review", 2, "previous-reviews")]), "CONSENSUS")
mutation("D04 missing research", "missing_fact", "required threat model", "NEEDS_INFORMATION")
fresh = fixture(); fresh["packet"] = ("brief-2",) + fresh["packet"][1:]
check("D04 old proposals under new brief", fresh, "PROCESS_FAILURE")
omitted = fixture(); omitted["required"].remove("designer"); omitted["configured"].remove("designer")
omitted["proposals"].pop("designer")
omitted["reviews"] = [r for r in omitted["reviews"] if r["role"] != "designer"]
omitted["consents"] = [r for r in omitted["consents"] if r["role"] != "designer"]
check("D04 omission without evidence", omitted, "PROCESS_FAILURE")
omitted["omissions"]["designer"] = "explicit configured applicability evidence"
check("D04 configured omission", omitted, "CONSENSUS")
mutation("S02 added role without contract", "contracts", ["architect","programmer","designer"])
bad = deepcopy(t); bad["issues"][0]["closures"] = ["facilitator"]
check("D05 facilitator closure", bad, "PROCESS_FAILURE")
bad = deepcopy(t); bad["issues"][0]["origins"].append("programmer")
check("D05 dropped co-originator", bad, "PROCESS_FAILURE")
bad = fixture(); bad["reviews"][0]["malformed"] = True
check("D05 malformed objection", bad, "PROCESS_FAILURE")
bad["reviews"][0]["malformed"] = False; bad["reviews"][0]["verdict"] = "AGREE_WITH_NOTES"
check("D05 corrected preference", bad, "CONSENSUS")
bad = fixture(); bad["consents"][0]["writer"] = "facilitator"
check("D05 impersonated consent", bad, "PROCESS_FAILURE")
for field, value in (("resolution","wrong"), ("packet",("wrong",1,"x","y")),
                     ("reviews","stale"), ("role","unknown"), ("verdict","CONDITIONAL")):
    bad = fixture(); bad["consents"][0][field] = value
    check("D06 " + field, bad, "PROCESS_FAILURE")
mutation("D06 missing consent", "consents", fixture()["consents"][:-1])
bad = fixture(); bad["consents"][0] = deepcopy(bad["consents"][1])
check("D06 duplicate consent", bad, "PROCESS_FAILURE")
bad = fixture(); bad["consents"][0].update(verdict="WITHHOLD", objection="new evidenced issue")
check("D06 withheld with budget", bad, "NEXT_ROUND")
bad["limit"] = 1
check("D06 withheld at limit", bad, "IRRECONCILABLE")
for limit in (3, 1):
    for later in range(1, len(ROLES)):
        bad = fixture(); bad["limit"] = limit
        bad["consents"][0].update(verdict="WITHHOLD", objection="new evidenced issue")
        bad["consents"][later]["writer"] = "facilitator"
        check("D06 withheld before impersonated consent " + str((limit, later)),
              bad, "PROCESS_FAILURE")
for limit in (0, -1, None):
    mutation("D07 invalid limit", "limit", limit)
bad = fixture(); bad["reviews"][0]["verdict"] = "OBJECT"
check("D07 unresolved before limit", bad, "NEXT_ROUND")
bad["limit"] = 1
check("D07 unresolved at limit", bad, "IRRECONCILABLE")
bad["missing_fact"] = "precise operator answer"
check("D07 missing answer priority", bad, "NEEDS_INFORMATION")
mutation("D08 altered seal", "seal_plan", "changed")
mutation("D08 seal rewrite", "seal_extra_instructions", True)
mutation("D08 missing impact", "effect_areas", list(IMPACTS)[:-1])
mutation("D08 unresolved branch", "ambiguous", True)
bad = fixture(); bad["records"]["plan"]["bytes"] += "new test expectation"
check("D08 changed accepted bytes", bad, "PROCESS_FAILURE")
for record, value in (("issues", "different candidate ledger"), ("brief", "different frozen brief")):
    bad = fixture(); bad["records"][record]["bytes"] = value
    bad["records"][record]["sha256"] = digest(value)
    check("D08 locally rehashed " + record + " with stale acceptance", bad, "PROCESS_FAILURE")
mutation("D09 older closure violation", "artifacts_before_previous_validation",
         [dict(step=2, isolation=False)])
for transport in ("local-no-issue", "posting-failed"):
    bad = fixture(); bad["missing_fact"] = "answer"; bad["transport"] = transport
    check("D09 transport independent", bad, "NEEDS_INFORMATION")
mutation("D10 active override", "configured", ["architect"])
mutation("D10 rollback", "active_version", "previous-version")
mutation("D10 terminal reopen", "terminal_before", True)
mutation("S03 duplicate allocation", "allocations", [(1,"a"),(1,"b")])
mutation("S03 exhausted allocation", "allocations", [(1000,"a")])
mutation("S03 unbound reference", "references", ["absent"])
bad = fixture(); del bad["records"]["plan"]["sha256"]
check("S03 required schema field", bad, "PROCESS_FAILURE")
bad = fixture(); bad["allocations"].pop(2); bad["reserved_gaps"] = [3]
check("S03 preserved failed allocation gap", bad, "CONSENSUS")
assert int("100") > int("99")
assert re.fullmatch(r"\d{2}\.\d{2}\.\d{4}_\d{2}\.\d{2}\.\d{2}-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
    "07.07.2026_14.30.12-f47ac10b-58cc-4372-a567-0e02b2c3d479")
# Additional transition, terminal-package, delivery and adoption predicates.
def sequence_valid(events):
    allowed = {
        "PLANNING_ACCEPTED": {"BRIEF_FROZEN"},
        "BRIEF_FROZEN": {"BLIND_PROPOSALS"},
        "BLIND_PROPOSALS": {"CANDIDATE_READY"},
        "CANDIDATE_READY": {"REVIEWING"},
        "REVIEWING": {"RESOLUTION_RECORDED"},
        "RESOLUTION_RECORDED": {"CANDIDATE_READY", "CONSENTING"},
        "CONSENTING": {"CANDIDATE_READY", "SEALING"},
        "SEALING": {"CANDIDATE_READY", "CONSENSUS"},
        "CONSENSUS": set(),
    }
    if not events or events[0] != "PLANNING_ACCEPTED":
        return False
    return all(b in allowed.get(a, set()) for a,b in zip(events, events[1:]))
good_sequence = ["PLANNING_ACCEPTED","BRIEF_FROZEN","BLIND_PROPOSALS",
    "CANDIDATE_READY","REVIEWING","RESOLUTION_RECORDED","CONSENTING","SEALING","CONSENSUS"]
assert sequence_valid(good_sequence)
for missing_index in range(1,len(good_sequence)-1):
    assert not sequence_valid(good_sequence[:missing_index]+good_sequence[missing_index+1:])
assert not sequence_valid(good_sequence+["CANDIDATE_READY"])
assert sequence_valid(good_sequence[:6]+good_sequence[3:])
count += 10
terminal_fields = {"state","last_phase","last_round","affected_references","agreed_sections",
    "open_issues","positions","evidence","attempted_resolutions","required_decision",
    "consumed_rounds","remaining_rounds","coding_allowed","empty_reasons"}
package = dict(state="NEEDS_INFORMATION",last_phase="BLIND_PROPOSALS",last_round=None,
    affected_references=["brief-1"],agreed_sections=[],open_issues=["missing source"],
    positions=[],evidence=["missing-source finding"],attempted_resolutions=["intake request"],
    required_decision="supply the required source",consumed_rounds=0,remaining_rounds=3,
    coding_allowed=False,empty_reasons={"agreed_sections":"no candidate","positions":"before reviews"})
def valid_package(p):
    if set(p) != terminal_fields or p["state"] not in (
            "NEEDS_INFORMATION","IRRECONCILABLE","PROCESS_FAILURE") or p["coding_allowed"]:
        return False
    if not p["required_decision"] or p["remaining_rounds"] < 0:
        return False
    return all(p[field] or p["empty_reasons"].get(field) for field in
        ("agreed_sections","open_issues","positions","evidence","attempted_resolutions"))
assert valid_package(package)
for missing in terminal_fields:
    broken = deepcopy(package); broken.pop(missing)
    assert not valid_package(broken)
count += len(terminal_fields) + 1
def delivery(p, linked_issue, post_succeeded):
    return ("issue" if linked_issue else "local", p["state"],
            "delivery-failed" if linked_issue and not post_succeeded else "delivered")
assert delivery(package, False, False) == ("local","NEEDS_INFORMATION","delivered")
assert delivery(package, True, False) == ("issue","NEEDS_INFORMATION","delivery-failed")
def entry_gate(started_legacy, new_cycle, has_seal):
    return (started_legacy and not new_cycle) or has_seal
assert entry_gate(True, False, False)
assert not entry_gate(False, False, False)
assert not entry_gate(True, True, False)
assert entry_gate(False, True, True)
def check_mode(source_or_build_changed):
    return ("application-build","all-target-tests") if source_or_build_changed else ("static","traces")
assert check_mode(False) == ("static","traces")
assert check_mode(True) == ("application-build","all-target-tests")
count += 8
print(str(count) + " replay assertions passed; D01-D10 and S01-S03 representative predicates")
```

These are reusable test specifications, not a shipped orchestration engine. Read [PROTOCOL](../PROTOCOL.md), [COMMON](COMMON.md), [MODELS](../MODELS.md) and the role files linked from [README](README.md). Adapter-specific static checks take adapter policy and baseline snapshots as inputs; the portable unit never imports those documents as execution dependencies. F01–F22 below are coverage labels for the original migration inventory, not required paths in a receiving repository. Acceptance numbers identify the fifteen original requirements; receiving repositories retain their equivalent mappings.

## Verification specifications

The tests below are acceptance specifications for Coding and Verification. These specifications express the deterministic cases using named inputs, actions, expected outcome, and evidence assertions. Verification may execute a temporary or inline standard-library checker; it must not add a runtime orchestration engine, persistent test dependency, or test file outside the authorized Markdown scope. Verification records the actual commands, assertions, results, and failing cases in its own report. A narrative assertion that a scenario "looks correct" does not substitute for evaluated trace predicates.

The replay model uses maps of immutable artifact IDs to fields and byte strings, an ordered invocation/allocation list, common manifests, and allowed-read sets. The evaluator rejects duplicate ownership, invalid schema, unresolved references, changed bytes, nonmonotonic allocation, illegal state transition, or missing evidence. Consent eligibility requires equality of the required-role set with accepting-review authors and consenting authors; all plan/brief/round/resolution identities must match; all blocking issues must have originator closure evidence; final plan bytes must match the accepted reference; and only the complete valid seal permits Coding. The evaluator is generic test machinery, not application functionality. Known-good and deliberately mutated fixture records give deterministic expected results.

S01, portability and copy, covers F01–F11 and acceptance criteria 1–2. Copy the documented core file set into a conceptual empty repository with a different artifact root and a different-domain evidence fixture. Resolve every relative Markdown link within the copy unit and reject mandatory imports of adapter, AGENTS, ARCHITECTURE, version-control commands, vendor names, runner APIs, application symbols, or build commands. Replay the same good trace under both roots; expect identical transition and consent results. External research citations may name vendors as sources, never normative dependencies.

S02, role completeness and routing, covers F02–F10, F12, F15–F17 and criterion 3. Assert that every default specialist has proposal, review and consent duties, input/output scope, capability requirement, evidence/test requirements, and completion/failure behavior. Assert Facilitator and Sealer have distinct duties and zero technical votes. Verify all routed paths exist. A configured added role must supply the same complete specialist contract; a missing contract is rejected.

S03, schema and artifact grammar, covers F01, F04, F12, F16–F18 and criterion 11. Validate task ID and three-digit global-number grammar; accept unique monotonic allocations with a documented failed-invocation gap, reject reuse, wraparound, overwrite, two writers, unbound references and duplicate effective responses. Verify rounds i01 through i02 and numeric handling beyond i99. A record containing a missing required field fails even if a final status says CONSENSUS. Validate a complete adapter local structured encoding example against the adapter's formatting policy, including the required sections and validation fields.

S04, active framework consistency, covers F12–F22 and criteria 9, 12 and 14. Resolve all new/changed local links; check every mentioned artifact suffix and role against PROTOCOL; ensure no active route invokes a standalone Architecture worker for a new task. Search active instructions, excluding historical tasks, for contradictory unconditional latest-step reads, old single-Agent architecture gates, unconditional documentation application-build requirements, direct role external messaging, and in-place PROMPT/step edits. Explicit historical descriptions and compatibility exceptions are allowed only when clearly scoped.

S05, preservation and scope, covers F13–F22 and criteria 12 and 15. Compare the adapter's project-evidence document from its first `## Overview` onward byte-for-byte against the pre-Coding version. Compare unchanged sections of the adapter's Coding contract and the existing build failure behavior in the adapter's Verification contract. Use the pre-Coding commit as the diff baseline; assert all changes are in F01–F22 plus Coding's own report. Assert no prior task report, PROMPT, application source, ignored-file policy, feature README or Operator Notes changed. Run the adapter's whitespace check over both unstaged/staged work and the completed change range; expect exit 0. Application build and source-index rebuild are not applicable.

D01, first-round consensus, covers F01–F10, F12–F13, F20–F21 and criteria 4, 6, 9–10. Input a frozen brief with four roles and limit three; four isolated proposals; one complete plan/ledger; four same-revision AGREE reviews; a resolution with no blockers; four final CONSENT records; an aggregate; and an identity-preserving seal. Replay all guards. Expect CONSENSUS, four required technical votes, and Coding enabled only after sealing; before sealing expect Coding disabled.

D02, revised-round agreement, covers F01, F04–F10, F13, F18, F20–F21 and criteria 5–6 and 10. Security objects to a concrete data-leak scenario in i01 while three roles agree. A sourced mitigation produces a different i02 plan; all four roles freshly review; Security explicitly closes its issue; the final ledger retains the original objection and evidence; all four freshly consent to i02 and its final ledger. Expect CONSENSUS only for i02. Delete any i02 review, replace any i02 consent with i01 consent, or carry three old reviews forward: expect PROCESS_FAILURE and Coding disabled. Retain a valid Security objection through i02: no consensus even with three accepts.

D03, equal blind inputs and forbidden flow, covers F01, F04–F09, F12, F15–F18 and criteria 4 and 13. Accept both a sequential clean-context trace and a parallel isolated-output trace with identical common manifests. Then mutate one input digest, inject a sibling proposal through inherited history, automatic latest-step loading, a version-control diff, shared search index, memory, a private message or a live web result. Each trace must fail isolation and end PROCESS_FAILURE before synthesis. A role's self-declaration cannot override the contradictory receipt. A missing receipt also fails. Allow reading all proposals only after the barrier; permit prior-round reviews in i02 while denying current-round sibling reviews until that review set is complete.

D04, evidence and roster changes, covers F01–F08, F12–F15 and criteria 3–4 and 11. During a blind proposal request a missing source or operator answer; expect NEEDS_INFORMATION and a complete terminal package. Supply the answer in a new immutable input/evidence artifact and create a higher-numbered brief; expect all roles to propose again with an equal new manifest. Reject changing the original brief or reusing old proposals as valid new proposals. Accept an explicitly configured omitted role with recorded applicability evidence before freezing; reject omission by nonresponse, documentation-only assumption, or Facilitator choice mid-run.

D05, objection quality and non-authority, covers F01, F04–F10, F18 and criteria 6–7. A review with a concrete violated invariant, failure, evidence and mitigation is blocking until the originator dispositions it. A bare preference cannot become a valid technical veto; request correction and accept a later explicit AGREE_WITH_NOTES plus consent, preserving history. If correction never arrives, expect PROCESS_FAILURE rather than consensus. Attempt majority override, model-prestige override, Facilitator-written role consent, duplicate removal that drops an originator, or Facilitator-only closure; each must be rejected. Compatible normalization with complete source references is allowed.

D06, final-consent integrity, covers F01, F04–F10, F18, F20 and criteria 5–7 and 13. Start with four accepting reviews and a zero-blocker resolution. Supply only three CONSENT records, a wrong-ledger digest, wrong brief, wrong round, duplicate role, unknown author, conditional assent, or a stale signature. Expect PROCESS_FAILURE and no Coding. A valid WITHHOLD containing a new objection returns to a new round when budget remains, followed by a complete fresh review and consent set; it never permits aggregate success. At the limit, classify the valid unresolved technical case as IRRECONCILABLE, not a process error merely because a role disagrees.

D07, bounded termination, covers F01, F09–F13, F18 and criteria 8–9. With limit one, keep two valid incompatible requirements unresolved after complete reviews: expect IRRECONCILABLE, all positions preserved and Coding disabled. If an exact operator fact is missing, expect NEEDS_INFORMATION even before the final round. If a required artifact or isolation guarantee is invalid/missing, expect PROCESS_FAILURE. Reject zero/negative/absent limits before proposal dispatch. Assert every terminal package includes agreed sections, open issues, positions, evidence, attempted resolutions, and the exact required decision/repair; a terminal brief cannot reopen in place.

D08, sealing identity and actionability, covers F01, F05–F10, F18, F20–F21 and criteria 9–10. Accept an immutable-reference seal when the plan contains all effect areas, implementation order, files, tests, rationale, alternatives, assumptions, confidence and dissent. Reject changed plan bytes, a seal containing rewritten instructions, omitted impact areas without rationale, a new test expectation added during sealing, or an unresolved branch requiring Coding to choose the design. A proposed substantive change consumes a new round with all reviews and consents; exhausted budget blocks handoff. A post-seal implementation redesign cannot use the old seal.

D09, full-chain validation and transport, covers F12–F14, F18, F20–F22 and criteria 13–14. Place a referenced blind proposal before a previous validation report and an isolation violation inside that proposal's receipt. Validator must traverse and detect it despite the historical cutoff. Remove the linked-issue destination: expect the same terminal package surfaced locally, no invented target. Simulate failed external posting: expect reported delivery failure and unchanged non-consensus council state. Verify roles never directly send messages. A docs-only Coding/Verification trace runs S01–S05 and D01–D10 with real pass/fail assertions and no application-build invocation; an application-change fixture retains the existing build/test gate.

D10, instruction/version compatibility, covers F01–F03, F12–F22 and criteria 1, 11–12 and 14–15. Keep an old five-stage task readable and unchanged, including its architecturing suffix. Permit finishing the already-started framework-adoption cycle under its recorded contract. Start a fresh task after adoption: reject a bare architecturing report as a Coding gate and require a council seal. Add an override that weakens unanimous consent or allows sibling reads: reject before freezing. Exhaust step 999: expect PROCESS_FAILURE with a linked-new-task requirement, never renumbering. Roll back documentation version while a council exists: preserve history and require an explicit new attempt under the chosen protocol rather than recycling signatures.

These mappings cover every planned file and all fifteen acceptance criteria in issue order. No new functions/classes/endpoints exist to test. The significant observable behavior is the Markdown contract and whether representative records satisfy its deterministic transition and information-flow predicates. Tests do not prove real-world model reasoning quality or an unexamined runner's hidden state, and successful test output must not make either claim.
