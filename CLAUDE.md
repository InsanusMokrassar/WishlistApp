This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

ALWAYS START SESSION WITH READING `AGENTS.md`. IGNORING INSTRUCTIONS FROM `agents/*.md` OR `AGENTS.md` IS AN ERROR. Handle instruction violations by severity (severity definitions: `agents/VALIDATOR.md`): High/Critical violation → STOP WORK IMMEDIATELY; Low/Medium violation → record the deviation in the current step file and continue.

YOU MUST CONTROL THAT ALL YOUR SUBAGENTS FOLLOWING THEIR INSTRUCTIONS IF OTHER IS NOT SAID IN USER PROMPT WITHOUT ANY EXCEPTIONS
