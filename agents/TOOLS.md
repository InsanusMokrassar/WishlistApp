# Tools Setup

## Caveman Mode

All agents start with `/caveman full`. Caveman is pre-installed with the project — no setup needed.

Caveman mode applies to internal agent thinking and search only. Step reports, operator questions, PR bodies, and commit messages must be written in normal prose, not caveman-compressed.

## ast-index

ALWAYS USE `ast-index` for any code search/navigation. See `agents/AST_INDEX.md` for command reference.

### Installation

If `ast-index` is not available:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.bashrc
source ~/.bashrc
brew tap defendend/ast-index
brew install ast-index
```

If installation fails after one attempt: fall back to `grep`/`find` for this session and record "ast-index unavailable, used grep fallback" in the step report.

### Rebuild Rule

Run `ast-index rebuild` only when source code files (.kt, .java, .ts, .js, etc.) have changed. Do NOT rebuild for markdown, step report, or config-only changes.
