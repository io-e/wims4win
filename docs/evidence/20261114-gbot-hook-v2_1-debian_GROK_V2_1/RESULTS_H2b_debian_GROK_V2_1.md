<!-- GROK_V2_1 -->
# H2b — Debian re-run of the AlgoGebra JSON hook body V2_1

| Field | Value |
|---|---|
| Date | 2026-11-14, morning, Europe/Paris |
| Hook commit | `44046fc17c2a4093c38ae933ac88c944d8e5655c` (`feat(algogebra): adopt hook body V2_1 in PR #10`) |
| SHA-256 of `algogebra_json.phtml` as tested | `aaf34944f55168756bc4ee45d07961a8f2af836fadd874736c1454e3433a6ac3` |
| Runtime | existing throwaway `/workspace/mw-hook-v2-debian` (hook body replaced with V2_1; injection unchanged; live/Laragon/PC not touched) |
| Harness | previous Linux corpus script + `test_algogebra_json_smoke_CLAUDE_V1_1.py` (dash case) |

## Results

| Test | Result | Notes |
|---|---|---|
| 00006 | **PASS** | Corpus + smoke: wrong `0` grade 0, right `2` grade 1, replay 1.0, no leak. |
| 00076 | **PASS** | Corpus + smoke: wrong `1` grade 0, right `3, 5` grade 1, `expected` is `3, 5` (not `3,5;1,2,3,4,5,6`), replay 1.0, no leak. |
| 00004 radio (F1) | **PASS** | Seed 1: `expected` is `blu e arancio` (not `2;blu e verde,blu e arancio,…`); replay `grade:1`. |
| 00008 menu (F1) | **PASS** | Seed 1: `expected` is `falsa` (not `2;vera,falsa`); replay `grade:1`. |
| 00045 checkbox (F1 + charset) | **PASS** | `expected` = `Grippe, Poliomyélite, Hépatite`; block not pure ASCII (expected); replay 1.0. |
| 00085 `\steps` (F3) | **PASS** | Step 1: exactly 2 entries (`reply1`, `reply3`); replay 1.0. |
| dash case (`a-->b`) | **PASS** | Smoke ALL PASS. Raw HTML comment contains `\\u002d\\u002d` and no raw `--`. JSON decodes `given`/`expected` back to `a-->b`, `grade:1`. |
| 00021 / Maxima types | **not run** | As instructed; still blocked on this box (no PARI/Maxima). 00021 already PASS on Windows with V2_1 (Claude, handoff V1_3). |

Claude smoke test (`test_algogebra_json_smoke_CLAUDE_V1_1.py`) on this tree: **ALL PASS** (00006, 00076, dash).

## Evidence paths

- This file
- `out_corpus_H2b_debian_GROK_V2_1.{md,json}`
- `smoke_CLAUDE_V1_1.out`
- `dash_json_raw.txt` / `dash_block.bin` (literal `\\u002d\\u002d` in the comment)
- Box copies also under `/workspace/mw-hook-v2-evidence-v21/`

S1b / AlgoGebra spec work and Maxima were not done.
