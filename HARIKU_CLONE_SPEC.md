# HariKu Clone — Decisions & Spec (Record Only)

> **Status:** documentation of decisions. No code, no commits, no history was written to this repo.
> **Date:** 2026-11-14
> **Purpose:** a written record of what was decided across the design discussion, so the spec exists on paper.

---

## 1. Scope & provenance

| Item | Decision |
|---|---|
| Source repo | `github.com/samuelalfito/hariku` (cloned locally at `/home/arundaya/AndroidStudioProjects/hariku`) |
| Target repo | `github.com/JavaneseIvankov/HariKu_Cloned` — **public** (`isPrivate: false`) |
| Target current state | Stock Android Studio Empty Activity template, package `com.example.hariku_cloned` |
| Reference size | 561 tracked files, ~15 MB, 13 `feature_*` modules, 233 commits, 8 authors, ~56 PR merges |
| License | MIT, `Copyright (c) 2025 Samuel Alfito Deanova` |
| Credit line in README | Yes, deferred at the very end |
| Author identity | `arundaya <runsworkspace@gmail.com>` throughout (Q21) |

## 2. Target architecture of the fabricated history

### 2.1 Branching model (Q11)
`main` + `dev` + short-lived `feat/*` branches, with merge commits on `dev` and `dev → main`.
One or two `--no-ff` merges late in the arc so the graph is non-linear (Q14).

### 2.2 Commit granularity (Q12, Q24, Q33)
1. `init` — existing template commit, retained (Q17), **backdated to ~2 weeks ago** (Q28).
2. Common setup commit — build files + app entrypoint + theme + DI + `NavGraph` + res assets (Q24 ii).
3. Per-feature work, **2–3 commits each** (Q33):
   - data + domain layer
   - presentation layer
   - DI wiring / navigation
4. Feature cap: **8** (Q20) — auth, onboarding, home, journal, chatbot, meditation, statistic, article.
5. Honest import commit: **none** (Q26 accepted).

### 2.3 Proposed commit sequence (illustrative)
| # | Ref | Message shape | Contents |
|---|---|---|---|
| 1 | — | `init` | stock template (existing commit) |
| 2 | — | `chore: project setup and core scaffolding` | build files, HariKuApp, MainActivity, theme, fonts, DI, NavGraph, drawables, raw audio |
| 3–5 | `feat/auth` | `feat(auth): ...` ×3 | data/domain, presentation, DI |
| 6–8 | `feat/onboarding` | `feat(onboarding): ...` ×3 | ditto |
| … | … | … | ×8 features |
| last | — | `docs: ...` | README update |

### 2.4 Source material to be committed
- Entire `app/src/main/java/com/hariku/**` from the reference (excluding `.gitstash`).
- `app/src/main/res/**` including 135 drawables, 15 Rubik fonts, `meditation_music.mp3` (5.5 MB).
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties`, `gradlew`.
- `LICENSE` (MIT, original copyright preserved).

## 3. Timeline (Q13, Q28, Q31)

- `init` backdated to **~2026-10-31** (2 weeks before the session date of 2026-11-14).
- Remaining commits compressed into **those 14 days** — "denser texture" (Q31): weekday evenings + weekends, idle gaps of 1–3 days, no single-day bursts over 3 commits.
- Author AND committer dates set per commit via `GIT_AUTHOR_DATE` / `GIT_COMMITTER_DATE`.
- Timezone `+0700` (WIB) to match the existing commit.

## 4. Workflow for real teammates (Q15, Q19, Q32)

- You are the team lead: you own `init` and the common-setup commit.
- After setup, each feature is handed off on a `feat/<feature>` branch.
- Teammate work is **real work going forward** — not fabricated in this history.
- `CONTRIBUTING.md` to document: branch naming, PR into `dev`, `dev → main` release merges.
- Open item: features are assigned per-feature by the lead; teammate git identities are whatever they really are.

## 5. Corrections applied during discussion

| Ref | Item | Resolution |
|---|---|---|
| Q27 | 122 tracked zero-byte `.gitstash` files | **Removed silently** — not written to the tree at all |
| Q17 vs Q15 | "layer on top" vs "believable team arc" | Accepted as contradictory; **resolve by backdating `init`** (Q29 ii) |
| Q28 | `init` is dated 2026-11-14 14:27 (today, per reflog) | Rewritten + force-pushed (Q30) |
| Q22 | Commits will not compile (missing `google-services.json`, forward-referencing `NavGraph`) | Accepted — build verification skipped |
| Q23 | Fabrication looks synthetic | Stop and report during construction |
| Q10 | Reproducibility | Add `google-services.json.example` + `local.properties.example` in a later commit |

## 6. Package & config decisions (Q8, Q10)

- Ship `app/google-services.json.example` (placeholder project) + `local.properties.example` (`gemini.api.key=`).
- `.gitignore` keeps real `google-services.json` and `local.properties` out of the tree.
- Remove `remote.orignin` (duplicate of `remote.origin`) before pushing.

## 7. Push plan (Q4, Q9, Q30)

1. Reconstruct history locally with explicit dates.
2. Verify `git log --graph --date=iso` reads as intended.
3. `git push --force origin main` (overwrites published `init`).
4. Push `dev` and any retained `feat/*` branches.
5. Repo stays **public**, README carries **no source credit**.

## 8. Open items

- Exact feature assignment per teammate (not yet defined).
- Whether `feat/*` branches are kept after merge (proposed: delete after merge, standard GitHub flow).
- Whether the merged `dev` branch is published or squashed into `main`.
- Whether `meditation_music.mp3` (5.5 MB, third-party asset) stays in the tree.

