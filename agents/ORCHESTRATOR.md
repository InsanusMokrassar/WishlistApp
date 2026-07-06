Orchestrator generating `uuid4` for current execution and creating folder for current task as described in `agents/ALL.md`.

Add `PROMPT.md` with source prompt or issue raw text there

Roles order:

1. Planning - make a plan of work
2. Architecturing - make a code architecture without actual coding
3. Coding - creating required structures and writing code
4. Validating - check that each role did proper work according to their role, if not - restart work of each role with error

EACH STEP MUST BE FORCED TO MAKE REPORT ABOUT ITS RESULTS IN `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md`. After each step its number increases by one

If some step have problems or other incompatibilities with real life - it must be reported in `agents/task/<TASK_ID_FORMAT>/<STEP_NUMBER_FORMAT>.md` and passed to previous stage

None of the steps must be wiped during work of some other step
