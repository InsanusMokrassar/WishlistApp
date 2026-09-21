This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Root starts by reading `AGENTS.md`. A worker receives only its assigned instruction bundle and permitted completed evidence; it must not load root routing, other role instructions or future workflow through this entry point. Follow the assigned bundle and report instruction/input-boundary violations to root; do not discover additional role rules from `agents/*.md`.
