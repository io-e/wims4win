<!-- GROK_V1 -->
# Next for GBot — MW-N1 + MW-N3 (owner selected 2026-11-06)

| Field | Value |
|---|---|
| Branch | `feat/algogebra-json-hook` (tip includes `647fb83` Issue #3 EN twin) |
| Done just now | **MW-N2** — Issue #3 comment posted (FR+EN): https://github.com/io-e/wims4win/issues/3#issuecomment-6681266477 |
| Still open | **MW-N1**, **MW-N3** |
| Policy | No merge. Ask before overwriting any PC runtime path. |

---

## MW-N1 — Deliver rebuilt Windows runtime to the PC

**Goal:** Produce `wims-runtime.tgz` (or equivalent tree) from `feat/algogebra-json-hook` with the vendored hook installed, stage it for Shadow PC, **ask before replacing** the live recipe tree.

Suggested target (owner confirms before copy):

```text
D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef
```

or a sibling folder e.g. `wims-runtime-oef-hook-YYYYMMDD` for side-by-side smoke first.

**Done when:**

1. Archive built from this branch (`build-runtime-tree.sh`, hook on by default).
2. Staged path + SHA256 / size reported.
3. Owner OK received before any overwrite of the current PC recipe tree.
4. After install (when OK): quick local check that `scripts/oef/algogebra_json.phtml` exists and `answer.phtml` contains `ALGOGEBRA_JSON_BEGIN` injection.
5. Optional: run Windows smoke `00006`/`00076` on that tree (8/8 target) — can be owner or GBot with wine/native CGI.

**Do not:** silently replace `wims-runtime-oef` or `wims-runtime-r19040-grok`.

---

## MW-N3 — Debian live HTTP smoke (GB-W3 / GB-W4)

**Goal:** Against real Debian `wims-full` HTTP (GB-R5 URL), with hook installed:

| Case | Expect |
|---|---|
| 00006 wrong + `special_parm=algogebra_json` | JSON block, `grade` 0, `id`=`reply1` |
| 00006 right + hook | `grade` 1 |
| 00076 wrong + hook | `grade` 0 |
| 00076 right + hook | `grade` 1 |
| Same reply **without** `special_parm` | **no** `ALGOGEBRA_JSON_BEGIN` |

**Done when:** evidence pack under e.g.  
`docs/evidence/YYYYMMDD-gbot-algogebra-json-debian_GROK_V1/`  
(raw HTML or extracts + one PASS/FAIL table).

**Wire rules:** `special_parm` on **reply only**; fields `replyN` (not `ansN`).

---

## Paste brief — GBot

> **MW-N2 done** (Issue #3 FR+EN posted). **Next:** (1) **MW-N1** build Windows runtime from `feat/algogebra-json-hook`, stage for PC, **ask before overwrite**. (2) **MW-N3** Debian HTTP smoke 00006/00076 × wrong/right + no-leak; evidence pack. No merge. Tip ≥ `647fb83`.

---

*End.*
