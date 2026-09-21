# Step File Protocol

## TASK_ID_FORMAT

Format: `dd.MM.yyyy_HH.mm.ss-<uuid4>`

- `dd` = day (2 digits, zero-padded)
- `MM` = month (2 digits, zero-padded)
- `yyyy` = year (4 digits)
- `HH` = hour in 24h (2 digits, zero-padded)
- `mm` = minutes (2 digits, zero-padded)
- `ss` = seconds (2 digits, zero-padded)
- `<uuid4>` = random UUID v4

Example: `07.07.2026_14.30.12-f47ac10b-58cc-4372-a567-0e02b2c3d479`

**No slashes, no spaces.** Dots and underscores only.

## STEP_NUMBER_FORMAT

Format: `<NNN>-<role-name>`

- `NNN` = 3-digit zero-padded integer (001, 002, ... 999)
- `<role-name>` = the role name supplied in the invocation; do not inspect a workflow catalog to determine it.

Example: `001-preparation`.

**Numbering is monotonic and never resets** — root allocates the next unused number, including on retries. No agent may wipe, rename, or overwrite an existing step file.

## Task Folder Bootstrap

When starting a new task:
1. Generate TASK_ID using current UTC time + uuid4.
2. Create folder: `mkdir -p agents/task/<TASK_ID>`
3. Write the source prompt or issue text into `agents/task/<TASK_ID>/PROMPT.md`

## Reading the Latest Step

To find the latest step: use only root's permitted completed input set, exclude `PROMPT.md`, sort by filename and take the last one. Never discover inputs by unrestricted traversal of the task folder. Root supplies the prompt when no completed input exists.

## Legacy Task Folders

Task folders created before this protocol (dashed dates like `06-07-2026_07-28-28-…`, bare UUID names, non-padded step names like `1-Planning.md`) are legacy: they are exempt from format validation. Never rename or restructure them, and never use them as format examples.
