<!-- GROK_V2 -->
# H2 — Debian re-run of the AlgoGebra JSON hook body V2

| Field | Value |
|---|---|
| Date | 2026-11-13, evening, Europe/Paris |
| Hook | GROK_V2 body (CLAUDE_V2 F1–F3) in PR #10 @ `c188d9ff4c8fe13c37ced2458c4b3d95b97a75ce` |
| SHA-256 of `algogebra_json.phtml` as tested | `b69e71adf7b37139f2e60feae38fbe07f0daa66d9b8928d2527848cf875f9da5` |
| Runtime | new isolated tree `/workspace/mw-hook-v2-debian` (copy of `mw-algogebra/debwims`, sessions emptied; existing trees not modified) |
| CGI | `/workspace/mw-hook-v2-debian/public_html/wims.cgi` (WIMS 4.33a) |
| Injection | existing `answer.phtml` hook (`!! GROK_V1 AlgoGebra JSON hook`) left unchanged |
| Harness | Linux-adapted copy of `test_algogebra_json_corpus_CLAUDE_V1.py` plus an explicit 00006/00076 8/8 and a 00045 charset contrast |
| Live / Laragon / PC | not touched |

## Results

| Exercise | Type | Result | Notes |
|---|---|---|---|
| 00006 | existing smoke | **PASS** | 8/8: wrong `0` grade 0, right `2` grade 1, no block without `special_parm`. `expected` is `2`. Unchanged vs GROK_V1. |
| 00076 | existing smoke | **PASS** | 8/8: wrong `1` grade 0, right `3, 5` grade 1, no leak. `expected` is `3, 5`. Unchanged vs GROK_V1. |
| 00004 | radio (F1) | **PASS** | Seed 1: `expected` is the option text `blu e arancio` (not `2;blu e verde,…`). Replaying it as `reply1` gives `grade:1`. The handoff’s `blu e verde` is the format example / first form option; at this seed the correct option is `blu e arancio`. |
| 00008 | menu (F1) | **PASS** | Seed 1: `expected` is `falsa` (not `2;vera,falsa`). Replay gives `grade:1`. The handoff’s `vera` is the format example / first option; the correct option at this seed is `falsa`. |
| 00045 | checkbox, accents (F1 + charset) | **PASS** | `expected` = `Grippe, Poliomyélite, Hépatite`. Block is not pure ASCII (byte `0xE9` in `windows-1252`) and decodes under the page charset. Replay POST in `windows-1252` → `grade:1`. Replay POST in UTF-8 → `grade:0.33` (expected, spec §5.2 draft). |
| 00085 | `\steps` (F3) | **PASS** | Step 1: exactly 2 entries, `reply1` and `reply3`; `given` is `1` and `0` (not `""`). Def has 12 replies; the 10 unreached steps are omitted. Replay → `grade:1`. |
| 00021 | 5th answer free (F2) | **BLOCKED** | `cmd=new` works (4 `numexp` fields). `cmd=reply` returns HTTP 450: WIMS reports PARI is not available or not visible on this server. Not a hook-body failure; the V2 skip of empty `replygood` could not be observed. |
| 00055 / 00058 / 00072 | function / formal / algexp | **BLOCKED** | WIMS reports MAXIMA is not available or not visible (`cmd=new` 450 for 00055/00072; `cmd=reply` 450 for 00058). The box has Windows Maxima under `/workspace/cas/maxima-5.49.0` only; no Linux `maxima` or `gp` on PATH. `sudo apt-get update` failed: `deb.debian.org` HTTP 500 (same class of mirror failure as SETUP-07). |

Corpus-style checks on every non-CAS exercise above: valid JSON, schema keys, seed 1, grade matches the HTML “note de X sur 10”, `given` matches what was sent, no marker without `special_parm`, page identical outside the block after normalisation, `expected` replays to 1.0.

## Evidence paths (box)

- `/workspace/mw-hook-v2-evidence/RESULTS_H2_debian_GROK_V2.md` (this file)
- `/workspace/mw-hook-v2-evidence/out_corpus_H2_debian_GROK_V2_nocas.{md,json}` (00004, 00006, 00008, 00045, 00076, 00085)
- `/workspace/mw-hook-v2-evidence/out_corpus_H2_debian_GROK_V2.{md,json}` (first pass, includes 00021/00055 errors)
- `/workspace/mw-hook-v2-evidence/debug/00021_reply.stdout` (450 PARI message)
- `/workspace/mw-hook-v2-evidence/debug/00055_new.stdout` (450 MAXIMA message)
- `/workspace/mw-hook-v2-evidence/raw/00045_replay_cp1252_block.bin` and `00045_replay_utf8.bin`
- `/workspace/mw-hook-v2-evidence/raw/00006_*` and `00076_*` reply captures
- Test tree: `/workspace/mw-hook-v2-debian` (throwaway; not a live runtime)

00006.oef / 00076.oef are **not** in `tools/oef-validation/html/exemples/sources/` on `feat/algogebra-json-hook`. They were taken from `origin/feat/algogebra-wims-integration` and match the grok-g1 copies (SHA-256 `fb23363f…d19a3` and `57084f15…e6568`).

## What was not done

Debian Maxima/PARI could not be installed (mirrors). No tests on Arnaud’s PC, no Laragon change, no load tests, no credentials used.
