# Instruction startup probe

Invocation class: ordinary worker. Assigned role: instruction-probe. Worker name: instruction_probe. Model: gpt-6.1-sol with high reasoning (Sol HL), selected explicitly for this bounded validation.

Inspect the automatic instruction text already present in the initial context. State whether the automatically injected workspace guidance includes a role roster or workflow routing. Compare that shared text with the permitted repository AGENTS.md if needed. Identify the explicitly assigned instruction sources and report any additional initial instructions, distinguishing platform instructions from repository instructions. Report observed text without reconstructing unavailable context.

Read this prompt first because no completed step exists. The complete permitted repository read set is AGENTS.md, agents/ALL.md, agents/local.ALL.md, agents/PROTOCOL.md, agents/GIT.md, agents/TOOLS.md, this PROMPT.md and the allocated report. The shared communication policy is the Communication Protocol Precedence and AML-HIP policy in the automatically loaded AGENTS.md. Use normal prose for the report narrative and commit message; use AML-HIP only for structured data blocks in the report. Use /caveman full only for internal working notes.

The only repository write allocation is agents/task/04.10.2026_19.57.23-5b01fa81-71e5-4a99-8297-d5b79ae9266f/001-instruction-probe.md. Do not inspect application source, run code searches or source tests, traverse history, read unassigned instructions or delegate. Do not discover additional roles, workflow stages or consumers. A git status check and a commit containing only the allocated report are authorized. Preserve all existing files. Do not push or send external messages. If excluded input is exposed, report the exact exposure and stop dependent work.

Write a self-contained report beginning with Model and Changed files, explain the requested model selection, state the initial-context observations and their limits, commit only the allocated report under agents/GIT.md and stop. No completed step report is supplied; this initial task prompt is the latest permitted input.
