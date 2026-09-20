# Expert Council inside Architecturing

[Architecture](../ARCHITECTURE.md) is the small nonvoting council orchestrator. It returns the familiar self-contained `<NNN>-architecturing.md` report: concrete design, implementation order, tests, rationale and README updates. The other workflow stages retain their responsibilities.

All voting contracts live in [roles/](roles/). Architecture discovers **every Markdown role file** there, including nested files, for every cycle. Each role gets its own fresh independent subagent; **all roles vote in parallel on every cycle**, using HL first and ML as fallback. By default, it must contains [Architect](roles/ARCHITECT.md), [Programmer](roles/PROGRAMMER.md), [Security](roles/SECURITY.md) and [Designer](roles/DESIGNER.md) roles. This list is descriptive, not the dispatcher roster. Adding a role file must include it automatically; non-domain work never skips a voter.

Read [PROTOCOL](../PROTOCOL.md), [COMMON](COMMON.md), [MODELS](../MODELS.md) and the complete role packet before council work. README, common instructions and [agent validation scenarios](CHECKS.md) stay outside the roles directory so no documentation file is mistaken for a voter.

The process uses only agents and native subagent/file/research tools. The Architecture coordinator performs administrative collation; there are no separate Facilitator or Sealer roles. It cannot force agreement or alter accepted implementation instructions. Requirements and comment-resolution validation belongs to [Validation](../VALIDATOR.md), not mechanical [Verification](../VERIFICATION.md).

Existing task reports are immutable history, not current protocol examples.
