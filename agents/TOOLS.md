# Tools Setup

## Caveman Mode

Caveman is pre-installed with the project — no setup needed (start rule: `agents/ALL.md`).

Scope of caveman vs normal prose: see `AGENTS.md` "Communication Protocol Precedence".

## ast-index

Usage mandate: `agents/ALL.md`. Command reference: `agents/AST_INDEX.md`.

### Installation

If `ast-index` is not available: installing Homebrew + ast-index changes the operator's machine system-wide — ASK THE OPERATOR before running the installation. If the operator declines or is unreachable, skip installation and use the grep/find fallback below.

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)"' >> ~/.bashrc
source ~/.bashrc
brew tap defendend/ast-index
brew install ast-index
```

If installation fails after one attempt: fall back to `grep`/`find` for this session and record "ast-index unavailable, used grep fallback" in the step report.

