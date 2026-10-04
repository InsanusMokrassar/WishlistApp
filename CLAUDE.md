This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Shared repository guidance is in `AGENTS.md`. A main session starts with the explicit root invocation in `agents/ROOT_PROMPT.md`. A worker follows only its assigned instruction bundle and permitted completed evidence; it must not load another instruction bundle through this entry point. Follow the invocation's access restrictions and report instruction/input-boundary violations to the assigning session; do not discover additional role rules from `agents/*.md`.
