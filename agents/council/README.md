# Expert Council inside Architecturing

Architecture is the nonvoting council coordinator. It returns one self-contained Architecturing report with concrete design, implementation order, tests, rationale, README updates and the recorded council outcome.

Participants read [COMMON](COMMON.md), [PROTOCOL](PROTOCOL.md), [MODELS](MODELS.md), their assigned role file and the explicit frozen packet. No ordinary root-role instructions apply to a council participant.

All voting contracts live in [roles/](roles/). Discover every Markdown role file there, including nested files, before every cycle and final publication. The default roster must contain [Architect](roles/ARCHITECT.md), [Programmer](roles/PROGRAMMER.md), [Security](roles/SECURITY.md) and [Designer](roles/DESIGNER.md); this list is not the dispatch roster. Adding a role file includes it automatically. Every role runs as a fresh independent subagent in parallel for proposals and every voting cycle, with HL preferred and ML fallback.

Keep shared documents and [CHECKS](CHECKS.md) outside the roles directory. Use only native agent, file and research tools for council execution. No script, executable fixture, parser or separate Facilitator/Sealer role controls council decisions. Architecture retains all votes and comments, checks the complete attempt, and publishes only unchanged unanimously accepted implementation instructions. Missing parallel capacity or independent contexts is a process failure.

Architecture stores immutable council artifacts in a separate attempt subdirectory and publishes the final report at the root-allocated ordinary stage path. Participants write only their allocated outputs and never perform Git writes. Existing task reports remain untouched.
