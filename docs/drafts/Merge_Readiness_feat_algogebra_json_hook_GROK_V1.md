<!-- GROK_V1 -->
# Merge readiness — `feat/algogebra-json-hook` (no merge yet)

| Field | Value |
|---|---|
| Document | `docs/drafts/Merge_Readiness_feat_algogebra_json_hook_GROK_V1.md` |
| Version | GROK_V1 |
| Date | 2026-11-06 |
| Branch | `feat/algogebra-json-hook` @ `6764f31` (+ packaging from `6748e98`) |
| Related master tip | `0bb1553` — Laragon Maxima/Octave/CGI-08 docs (not yet on this branch) |
| Policy | **Checklist only — do not merge** without owner OK |

---

## 1. What this branch delivers

| Item | Status |
|---|---|
| Vendored hook `tools/algogebra-hook/` | Done |
| `build-runtime-tree.sh` always installs hook (`ALGOGEBRA_HOOK=0` opt-out) | Done |
| Install/uninstall docs `docs/09_Hook_JSON_AlgoGebra.md` | Done |
| GPL provenance note under `tools/algogebra-hook/NOTICE.md` | Done |
| Linux smoke 00006/00076 | 8/8 (GBot) |
| Debian HTTP smoke | 8/8 (GBot evidence pack) |
| Windows sibling runtime on PC | `wims-runtime-r19040-grok-hook-20261106` |
| Laragon `WIMS_RT` cutover | Done (owner) |
| Laragon Maxima / Octave / CGI-08 | PASS (`0bb1553` on **master**) |
| Issue #3 | Closed |
| Hook body GROK_V2_1 (F1–F3, `--` escape restored) | Adopted 2026-11-14; Debian re-run of previous set + dash case pending |

---

## 2. Pre-merge checklist

| # | Check | Owner / who | Status |
|---|---|---|---|
| M1 | Fast-forward or merge `master` (`0bb1553`+) into this branch so Suivi/test-plan stay consistent | GBot / Grok | **Open** |
| M2 | CI green on branch (linux-platform, mingw-cross, oef-validation) | GBot | **Verify** |
| M3 | Confirm `ALGOGEBRA_HOOK=0` build still produces tree **without** injection | GBot | **Verify once** |
| M4 | Confirm Linux object identity unchanged (phtml-only → expect N/A / skip) | GBot | Note in PR |
| M5 | PR description: hook trigger = reply-only `special_parm=algogebra_json`; fields `replyN` | Author | Draft below |
| M6 | No force-push; no merge to `master` without owner | Everyone | Standing rule |
| M7 | AlgoGebra client WA-9 can proceed in parallel (does not block merge of WIMS hook) | Codex | Unblocked |
| M8 | body V2 adopted, marker bumped, docs/09 recipe extended (00004, 00008, 00045, 00085, 00021) and Debian re-run (handoff « M4 » delta, 2026-11-13) | GBot | Partial: 00006/00076 + 00004/00008/00045/00085 PASS; 00021 and Maxima types BLOCKED (PARI/Maxima not on the box; apt mirrors HTTP 500). Evidence: docs/evidence/20261113-gbot-hook-v2-debian_GROK_V2/ |
| M9 | body V2_1 adopted (`--` escaped as `\\u002d\\u002d`), marker bumped, Debian re-run of 00006/00076/00004/00008/00045/00085 plus dash case | GBot | Pending Debian re-run |

---

## 3. Suggested PR title / body (draft)

**Title:** `feat: AlgoGebra JSON grading hook in OEF runtime trees`

**Body (draft):**

```markdown
## Summary
Vendors `tools/algogebra-hook/` and installs it from `build-runtime-tree.sh` into
Linux and Windows runtime trees (`algogebra_json.phtml` + `answer.phtml` injection).
Opt-out: `ALGOGEBRA_HOOK=0`.

Trigger: `cmd=reply&special_parm=algogebra_json` only (never on `cmd=new`).
Output: HTML comment `ALGOGEBRA_JSON_BEGIN`…`END` with `replyN` / `choiceN` answers.

## Test plan
- [x] Linux 00006/00076 smoke 8/8
- [x] Debian HTTP 8/8 (docs/evidence/20261106-…)
- [x] Windows sibling runtime + Laragon cutover
- [x] Laragon Maxima / Octave / CGI-08 (see master `0bb1553` — merge master first)
- [ ] CI jobs green on this PR
- [ ] `ALGOGEBRA_HOOK=0` negative install check

## Notes
- GPL-2 hook; AlgoGebra client only consumes JSON over HTTP (see NOTICE).
- Does not close client WA-9 (Codex); unblocks it.
```

---

## 4. Explicit non-goals for this merge

- Merging AlgoGebra `#40–#42` client PRs  
- Closing crossword P2 failure (unrelated)  
- Replacing `modupdate` / full module banks (Issue #6)  

---

## 5. Recommended next agent moves

1. **GBot:** M1 merge/rebase `master` onto `feat/algogebra-json-hook`; run M2–M4; open PR **without** merging.  
2. **Codex:** WA-9 mock/client rebase (see AlgoGebra `Handoff_WA9_Evidence_Links_GROK_V1.md`).  
3. **Owner:** review PR → merge when M2–M4 green.

---

*End.*
