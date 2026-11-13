<!-- CLAUDE_V1_1 -->
# Hand-off to GBot — adopt hook body V2 in PR #10 (`feat/algogebra-json-hook`)

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Handoff_GBot_Hook_V2_PR10_CLAUDE_V1_1.md` |
| Version | CLAUDE_V1_1 (V1 + corrected smoke test, §3b) |
| Date | 2026-11-13 |
| From / to | Claude (Windows lane) → GBot (Debian lane, owner of PR #10) |
| Decision | Arnaud, 2026-11-13: "GBot takes the V2 hook into PR #10 from commit `bece517` and re-runs Debian before merge. Claude will not touch that branch." |
| Source commit | `feat/algogebra-wims-integration` @ `bece517` (local, not pushed; Arnaud decides when/if it is pushed) |
| Evidence | `Review_Hook_JSON_Windows_CLAUDE_V1.md` (findings F1–F6), `tests/out_corpus_CLAUDE_V2_hook_CLAUDE_V1.{md,json}` |

This note is the only channel: no commit on `feat/algogebra-json-hook` by Claude, no GitHub comment, no change to the Laragon runtime (`wims-runtime-r19040-grok-hook-20261106`) or to `wims-runtime-oef`.

---

## 1. What to take

| Item | Path on `feat/algogebra-wims-integration` | Action on PR #10 |
|---|---|---|
| Hook body V2 | `AlgoGebra_Wims_Integration/runtime_patch/algogebra_json_CLAUDE_V2.phtml` | Copy **the body** over `tools/algogebra-hook/algogebra_json.phtml`. Keep your licence header as is; bump the in-file marker `GROK_V1 → GROK_V2` (or keep both tags, your call). Schema, markers, escaping and the `answer.phtml` injection are unchanged, so `install-algogebra-hook.*`, `uninstall-*` and `build-runtime-tree.sh` need no change. |
| Review | `AlgoGebra_Wims_Integration/Review_Hook_JSON_Windows_CLAUDE_V1.md` | Reference from `docs/09_Hook_JSON_AlgoGebra.md` (one line) so the "why" of V2 is findable. |
| **Smoke test, corrected** | `AlgoGebra_Wims_Integration/tests/test_algogebra_json_smoke_CLAUDE_V1.py` | Replaces `test_algogebra_json_GROK_V1.py` (kept as archive). Runs on Windows **and** Linux (`wims.cgi` or `wims.cgi.exe`), stdlib only. Use it for H2, see §3b. |
| Corpus tool (optional) | `AlgoGebra_Wims_Integration/tests/test_algogebra_json_corpus_CLAUDE_V1.py` | Not for CI (needs a Windows runtime). Document the command in `docs/09` as a manual recipe if you want it; otherwise ignore. |

## 2. What V2 changes (and why)

| # | GROK_V1 behaviour | V2 behaviour | Why |
|---|---|---|---|
| F1 | `expected` = raw `replygood<N>`: `2;vera,falsa` (radio), `2,5,6;Toxoplasmose,…` (checkbox), `a,n,g,l,e;a,b,c,d,e` (clickfill) | menu / radio / checkbox / click / mark / multipleclick / flashcard → `!detag $(replyGood<N>)` (the option text WIMS prints and the value the form posts); clickfill / dragfill / case / nocase / atext / compose / coord / imgcomp / javacurve / reorder / textcomp / wlist → first row of `replygood<N>` (`rows2lines` + `line 1`, the anstype idiom); other types unchanged | Sending GROK_V1 `expected` back gets « pas compréhensible ». Replay: 4/10 → **10/10**. |
| F2 | reply with empty `replygood<N>` emitted as `correct:false, score:0` while `grade` is 1.0 | omitted (WIMS counts it as a free point and prints no analysis row) | `answers[]` contradicted `grade` (exercise 00021). |
| F3 | `\steps` exercise: all 12 replies emitted, 10 of them `given:""` | only `r<N> isitemof $gotsteps` when `$oefsteps` is set, same test as `answer.phtml` line 31 | exercise 00085: 12 → 2 entries, equal to the rows displayed. |

No new JSON field (owner decision). Strings stay in the page charset (`windows-1252`), as before.

## 3. Debian recipe to re-run before merge (H2)

Same as `docs/09` plus three exercises that exercise F1–F3. All are in `tools/oef-validation/html/exemples/sources/` **on your branch** (the corpus differs per branch; on `feat/algogebra-wims-integration` only 00006/00076 exist).

| Exercise | Type | What to check in the block |
|---|---|---|
| 00006, 00076 | your existing smoke | unchanged result |
| 00004 | `radio` | `expected` is the option text (`blu e verde`), not `2;blu e verde,…`; replaying it as `reply1` gives `grade:1` |
| 00008 | `menu` | `expected` is the option text (`vera`), not `2;vera,falsa`; replay gives `grade:1` |
| 00045 | `checkbox`, accented options | `expected` = comma-separated option texts; block decodes under `windows-1252`; replay in `windows-1252` gives `grade:1` (UTF-8 POST gives 0.33, this is expected, see spec §5.2 draft) |
| 00085 | `\steps`, 12 answers | step 1: exactly 2 entries (`reply1`, `reply3`), none with `given:""` |
| 00021 | 5 answers, 5th free | 4 entries, `grade` consistent |

Expected totals on Windows for reference (seed 1, dummy answers then replay): 10/10 graded exercises, `expected` replays to 1.0 on all 10, page identical outside the block, no block without `special_parm`. Numbers are in `tests/out_corpus_CLAUDE_V2_hook_CLAUDE_V1.md`.

## 3b. Corrected smoke test (2026-11-13)

`test_algogebra_json_GROK_V1.py` passes on **both** hook bodies, so it cannot gate H1: it never replays `expected`, percent-encodes the POST in UTF-8 (WIMS reads the page charset), ignores the `feedback` key, does not compare `answers[]` with the analysis rows, and only knows `wims.cgi.exe`. `test_algogebra_json_smoke_CLAUDE_V1.py` adds, on the same 00006 / 00076 cases: schema incl. `feedback` and JSON types, `seed`, `grade` == « note de X sur 10 », `answers` == analysis rows, `given` == sent, **`expected` replayed → 1.0** (F1), POST in the `cmd=new` charset (F6), no leak and identical page without `special_parm`, empty submission → `wims_msg alert` and no block (F4).

Result on `wims-runtime-oef` (seed 1):

| Hook body | Result |
|---|---|
| GROK_V1 (PR #10 today) | **FAIL 1**: `00076: replaying expected={'reply1': '3,5;1,2,3,4,5,6'} graded 0, not 1.0` — everything else passes |
| CLAUDE_V2 | **ALL PASS**; checkbox `expected` is `3, 5` |

Bench restored to GROK_V1 afterwards (sha256 `8174221d…` verified). Debian invocation:

```bash
WIMS_HOME=/path/to/runtime OEF2WIMS=/path/to/bin/oef2wims OEF_SOURCES=tools/oef-validation/html/exemples/sources python3 -X utf8 -I AlgoGebra_Wims_Integration/tests/test_algogebra_json_smoke_CLAUDE_V1.py
```

It needs the test module `modules/H6/test/oef2html.fr` (copy of `modules/template/oef.fr`, as in `docs/09`). Expected: FAIL 1 before H1, ALL PASS after.

## 4. Known gaps (not blocking PR #10, per Arnaud)

- `function` / `formal` / `algexp` (00055, 00058, 00072): `bad_exo` on `wims-runtime-oef` (PARI only). V2 treats them as "other types" (raw `replygood`), same as GROK_V1. Please include at least one in your Debian run since Maxima is available there.
- `matrix`, `set`, `fset`, `vector` keep `;` as row separator in `expected` (intended; the form posts rows the same way).
- Error pages (`bad_exo`, `bad_answer`, `empty_data`) carry no block: spec-side fix (`INVALID_ANSWER`), nothing to do in the hook.

## 5. Merge checklist delta

Add to `docs/drafts/Merge_Readiness_feat_algogebra_json_hook_GROK_V1.md`: M4 "body V2 adopted, marker bumped, docs/09 recipe extended (00004, 00008, 00045, 00085, 00021) and re-run green".

---

*End of `Handoff_GBot_Hook_V2_PR10_CLAUDE_V1.md`.*
