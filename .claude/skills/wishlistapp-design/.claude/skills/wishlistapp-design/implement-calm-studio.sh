#!/usr/bin/env bash
#
# implement-calm-studio.sh
# ─────────────────────────────────────────────────────────────────────────────
# Drives the WishlistApp web-client redesign ("Calm Studio") in your real repo,
# using Claude Code in headless mode. It:
#   1. Verifies prerequisites (claude CLI, git, clean tree).
#   2. Installs THIS design system as a Claude Code skill in your repo.
#   3. Runs the redesign as a sequence of reviewable phases — each phase is a
#      Claude Code task that reads the skill + the calm-studio reference kit and
#      edits your Kotlin/Compose-HTML web client, committed one phase at a time.
#
# This is a GUIDE you run yourself — it makes small, inspectable commits so you
# stay in control. Read it before running. Nothing here touches Desktop/Android.
#
# Usage:
#   ./implement-calm-studio.sh            # interactive, phase-by-phase, opens a PR at the end
#   ./implement-calm-studio.sh --phase 2  # run a single phase (no branch/PR side-effects)
#   ./implement-calm-studio.sh --yes      # don't pause between phases
#   ./implement-calm-studio.sh --no-pr    # do everything except open the PR
#
# Branch/PR: all phase commits land on a feature branch (default redesign/calm-studio)
# and the final step pushes it and opens a PR via the GitHub 'gh' CLI (base: master).
# Override with BRANCH=... BASE_BRANCH=... or --branch <name>.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── config ───────────────────────────────────────────────────────────────────
SKILL_NAME="wishlistapp-design"
SKILL_DIR=".claude/skills/${SKILL_NAME}"      # where the skill lands in your repo
DS_SRC="${DS_SRC:-}"                          # path to this design-system folder
WEB_MODULE="${WEB_MODULE:-features/ui}"       # Compose-HTML web views live under here
BRANCH="${BRANCH:-redesign/calm-studio}"      # feature branch all phase commits land on
BASE_BRANCH="${BASE_BRANCH:-master}"          # PR target
OPEN_PR=1                                     # set 0 with --no-pr to skip the gh PR step
AUTO_YES=0
ONLY_PHASE=""

# Claude Code invocation. We pass the prompt on stdin and allow file edits.
# Adjust flags to taste (e.g. drop --permission-mode for full manual approval).
claude_run() {
  local title="$1"; shift
  echo "──▶ ${title}"
  claude -p "$(cat)" \
    --permission-mode acceptEdits \
    --allowedTools "Read,Edit,Write,Glob,Grep,Bash(git*)" \
    || { echo "✗ phase failed: ${title}" >&2; exit 1; }
}

# ── args ─────────────────────────────────────────────────────────────────────
while [ $# -gt 0 ]; do
  case "$1" in
    --yes|-y) AUTO_YES=1 ;;
    --no-pr) OPEN_PR=0 ;;
    --phase) ONLY_PHASE="${2:-}"; shift ;;
    --src) DS_SRC="${2:-}"; shift ;;
    --branch) BRANCH="${2:-}"; shift ;;
    -h|--help) sed -n '2,40p' "$0"; exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
  shift
done

pause() {
  [ "$AUTO_YES" = "1" ] && return 0
  read -r -p "   ↳ review the diff, then press Enter for the next phase (Ctrl-C to stop) " _
}

run_phase() { [ -z "$ONLY_PHASE" ] || [ "$ONLY_PHASE" = "$1" ]; }

# ── prerequisites ─────────────────────────────────────────────────────────────
command -v claude >/dev/null || { echo "✗ Claude Code CLI 'claude' not found. Install: https://docs.claude.com/claude-code" >&2; exit 1; }
command -v git    >/dev/null || { echo "✗ git not found." >&2; exit 1; }
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "✗ run this inside your WishlistApp git repo." >&2; exit 1; }
if [ -n "$(git status --porcelain)" ]; then
  echo "✗ working tree not clean — commit or stash first (each phase makes its own commit)." >&2; exit 1
fi

# ── feature branch: all phase commits land here, so the PR is one clean series ──
if [ -z "$ONLY_PHASE" ]; then
  if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
    git switch "$BRANCH"
  else
    git switch -c "$BRANCH"
  fi
  echo "✓ on branch ${BRANCH}"
fi

# ── step 0: install the skill ──────────────────────────────────────────────────
if run_phase 0; then
  echo "━━ Phase 0 — install the '${SKILL_NAME}' skill ━━"
  if [ -z "$DS_SRC" ]; then
    echo "   Set DS_SRC to the design-system folder you downloaded, e.g.:"
    echo "     DS_SRC=~/Downloads/wishlistapp-design ./implement-calm-studio.sh"
    exit 1
  fi
  mkdir -p "$(dirname "$SKILL_DIR")"
  rm -rf "$SKILL_DIR"
  cp -R "$DS_SRC" "$SKILL_DIR"
  # the skill doesn't need the generated bundle or this script inside the repo
  rm -f "$SKILL_DIR/_ds_bundle.js" "$SKILL_DIR/_ds_manifest.json" \
        "$SKILL_DIR/_adherence.oxlintrc.json" "$SKILL_DIR/implement-calm-studio.sh" 2>/dev/null || true
  git add "$SKILL_DIR"
  git commit -q -m "chore(design): add ${SKILL_NAME} skill (Calm Studio spec)"
  echo "   ✓ skill installed at ${SKILL_DIR}"
  pause
fi

# Shared preamble prepended to every implementation phase.
PREAMBLE="You are implementing the WishlistApp WEB CLIENT redesign called \"Calm Studio\".
The design spec and a complete interactive reference implementation are in the skill
at ${SKILL_DIR}. ALWAYS read these before editing:
  - ${SKILL_DIR}/README.md                     (brand + the Calm Studio target-spec section)
  - ${SKILL_DIR}/tokens/calm-studio.css        (the --cs-* design tokens)
  - ${SKILL_DIR}/ui_kits/calm-studio/           (reference: styles.css, components.jsx, app.jsx, data.js)
The web client is Kotlin/JS Compose HTML; web views are under ${WEB_MODULE}/**/src/jsMain.
Keep the product's copy voice (functional, second-person, Title Case actions,
sentence-case messages, question-style confirms, no emoji). Do NOT touch the Android
or Desktop (Material 3) clients. Match the reference's structure and class names so the
CSS in the next phases lines up. Make ONE git commit at the end of this phase with a
clear conventional-commit message. If something is ambiguous, prefer the reference."

# ── phase 1: design tokens + global stylesheet ─────────────────────────────────
if run_phase 1; then
  echo "━━ Phase 1 — tokens + global stylesheet ━━"
  claude_run "Phase 1: Calm Studio tokens & base CSS" <<EOF
${PREAMBLE}

TASK (phase 1 of 5): Establish the Calm Studio visual foundation in the web client.
1. Add the --cs-* custom properties from ${SKILL_DIR}/tokens/calm-studio.css into the
   web client's global stylesheet (the CSS served by client/src/jsMain/resources, or
   create a calm-studio.css and link it from index.html). Keep Bootstrap loaded for now.
2. Load the Manrope webfont (weights 400–800) the way the reference does.
3. Port the base/reset + shell-level rules (.app, .sidebar, .main, .topbar, .content,
   buttons, .card, .row, .pill, forms, modal, toast) from
   ${SKILL_DIR}/ui_kits/calm-studio/styles.css, expressed against the --cs-* tokens.
Do not change any Kotlin views yet — CSS + font + token wiring only. Commit.
EOF
  pause
fi

# ── phase 2: app shell — sidebar nav + top bar ─────────────────────────────────
if run_phase 2; then
  echo "━━ Phase 2 — app shell (sidebar nav + top bar + search) ━━"
  claude_run "Phase 2: shell & navigation" <<EOF
${PREAMBLE}

TASK (phase 2 of 5): Replace the breadcrumb-only chrome with the Calm Studio shell.
Reference: ${SKILL_DIR}/ui_kits/calm-studio/components.jsx (Sidebar, TopBar) and the
ScaffoldView / TopBarView in ${WEB_MODULE}/scaffold and ${WEB_MODULE}/topBar.
1. Implement a persistent left Sidebar (Compose HTML) with sections: My Lists,
   Discover, Reserved (with a live count), Settings; the signed-in user's lists
   pinned below; a "New list" affordance; and the profile row at the bottom.
2. Implement the top bar with a global search field (people / lists / items) and the
   auth action (Log in / Log out). Use the Lucide icon set (lucide.dev) for nav glyphs.
3. Wire the existing router so the sidebar items navigate; keep a small breadcrumb in
   content for depth. Default landing route becomes "My Lists", NOT the users list.
Commit.
EOF
  pause
fi

# ── phase 3: screens ───────────────────────────────────────────────────────────
if run_phase 3; then
  echo "━━ Phase 3 — screens (My Lists, Discover, List, Item, Edit) ━━"
  claude_run "Phase 3: screens" <<EOF
${PREAMBLE}

TASK (phase 3 of 5): Rebuild the core screens to match the reference app.jsx, reusing
your existing view-models / data layer (do not fake data — bind to the real stores):
  - My Lists (home): list cards with cover, visibility, item + reserved counts.
  - Discover: people grid → profile (a user's public lists).
  - List detail: grid/list toggle, filter (All/Available/Reserved), sort
    (Priority/Cost/Title), Share + (owner) Add item / (visitor) Copy to my profile.
  - Item detail: gallery, priority pill, price, links, owner Edit vs visitor actions.
  - Item add/edit: title, description, price+amount, priority segmented control,
    delete with a "Delete item?" confirm modal.
Match the reference's CSS class names so Phase 1 styles apply. Commit.
EOF
  pause
fi

# ── phase 4: reservations feature ──────────────────────────────────────────────
if run_phase 4; then
  echo "━━ Phase 4 — surface the reservation feature ━━"
  claude_run "Phase 4: reservations" <<EOF
${PREAMBLE}

TASK (phase 4 of 5): Surface gift reservations as a first-class feature (reference:
the reservation model in ${SKILL_DIR}/ui_kits/calm-studio/data.js and the Item/Reserved
screens in app.jsx). Use the existing server endpoints if present; otherwise stub the
client store and leave a clearly-marked TODO for the API.
  - On a visitor's view of an item: "Reserve this gift" / "Cancel reservation".
  - A "Reserved" screen listing gifts the signed-in user has reserved (the sidebar
    count reflects it live).
  - PRIVACY: the list owner must see only THAT an item is reserved, never WHO reserved
    it. Reserved items are visually marked but their reserver is never exposed to the owner.
Commit.
EOF
  pause
fi

# ── phase 5: polish + retire legacy ────────────────────────────────────────────
if run_phase 5; then
  echo "━━ Phase 5 — polish, empty states, retire Bootstrap ━━"
  claude_run "Phase 5: polish" <<EOF
${PREAMBLE}

TASK (phase 5 of 5): Finish and clean up.
  - Empty states (no lists / no items / nothing reserved), toasts for async actions,
    login & register modal, settings screen — all per the reference.
  - Once every screen renders against --cs-* styling, remove the Bootstrap stylesheet
    link and delete now-dead Bootstrap class usages from the web views. Verify nothing
    on the page still depends on Bootstrap.
  - Run the project's web build/format/lint and fix any breakage you introduced.
Commit. Then print a short summary of remaining manual follow-ups (API gaps, etc.).
EOF
  pause
fi

# ── final step: push the branch and open the PR ────────────────────────────────
if [ -z "$ONLY_PHASE" ] && [ "$OPEN_PR" = "1" ]; then
  echo "━━ Opening pull request ━━"
  if command -v gh >/dev/null 2>&1; then
    git push -u origin "$BRANCH"
    gh pr create \
      --base "$BASE_BRANCH" \
      --head "$BRANCH" \
      --title "Web client redesign: Calm Studio" \
      --body "$(cat <<'PRBODY'
Redesigns the **web client** to the "Calm Studio" design language: persistent
left-sidebar navigation, global search, surfaced gift **Reservations** (with
owner-privacy), and a near-monochrome single-accent visual system on the Manrope
typeface. Implemented phase-by-phase — see the commit series.

Design source: the `wishlistapp-design` skill committed under `.claude/skills/`
(`tokens/calm-studio.css` + the `ui_kits/calm-studio/` reference implementation).

Scope: web (`features/ui/**/src/jsMain`) only — Android & Desktop (Material 3) untouched.

Generated with Claude Code via implement-calm-studio.sh.
PRBODY
)"
    echo "✓ PR opened against ${BASE_BRANCH}."
  else
    git push -u origin "$BRANCH" || true
    echo "⚠ 'gh' CLI not found — branch pushed (if a remote exists). Open the PR manually:"
    echo "   https://github.com/InsanusMokrassar/WishlistApp/compare/${BASE_BRANCH}...${BRANCH}?expand=1"
  fi
fi

echo "✓ Calm Studio implementation run complete. Review the commit series with: git log --oneline"
