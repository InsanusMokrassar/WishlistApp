Orchestrator generating `uuid4` for current execution and creating folder `agents/task/<uuid>/`.

Each step MUST complete its work with file writing in `agents/task/<uuid>/<STEP_NUMBER>.md`. `STEP_NUMBER` is the `STEP_0`/`STEP_1`/etc. steps titles

Steps order:

1. Planning - make a plan of work
2. Architecturing - make a code architecture without actual coding
3. Coding - creating required structures and writing code

EACH STEP MUST BE FORCED TO MAKE REPORT ABOUT ITS RESULTS IN `agents/task/<uuid>/<STEP_NUMBER>.md`. After each step its number increases by one

If some step have problems or other incompatibilities with real life - it must be reported in `agents/task/<uuid>/<STEP_NUMBER>.md` and passed to previous stage
