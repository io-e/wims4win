<!-- CLAUDE_V1 -->
# Review — AlgoGebra JSON hook (PR #10 body, GROK_V1) on the Windows runtime

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Review_Hook_JSON_Windows_CLAUDE_V1.md` |
| Version | CLAUDE_V1 |
| Date | 2026-11-13 |
| Reviewed | `tools/algogebra-hook/algogebra_json.phtml` GROK_V1 (`feat/algogebra-json-hook` @ `00eaa91`, PR #10) — same body as `runtime_patch/algogebra_json.phtml` on this branch |
| Spec | `io-e/AlgoGebra` `02-Cdc/Spec_WIMS_Message_Integration_GROK_V1_1.md` §3.2, §4.4, §5, §6 |
| Bench | `wims-runtime-oef` (wims4win 4.33a CGI, PARI only), 15 sample exercises of `tools/oef-validation/html/exemples/sources/` on `feat/algogebra-json-hook`, seed 1 |
| Tool | `tests/test_algogebra_json_corpus_CLAUDE_V1.py` (stdlib; `python -X utf8 -I …`) |
| Evidence | `tests/out_corpus_GROK_V1_hook_CLAUDE_V1.{md,json}` · `tests/out_corpus_CLAUDE_V2_hook_CLAUDE_V1.{md,json}` · `tests/out_series_qnum2_CLAUDE_V1.{md,json}` |
| Proposal | `runtime_patch/algogebra_json_CLAUDE_V2.phtml` (same schema, no new field) |
| Status | For Arnaud / GBot — **not** posted on GitHub, nothing merged, live runtimes untouched (GROK_V1 body restored on the bench, sha256 `8174221D…7B0A` verified) |

---

## 1. Method

For each exercise the script compiles the `.oef`, installs it in the test module, fixes the seed and:

| Case | What is sent | What is checked |
|---|---|---|
| **W** | dummy answers + `special_parm=algogebra_json` | marker present · body parses as JSON · schema keys · `seed` · `grade` == « note de X sur 10 » / 10 printed by `answer.phtml` · `correct` == indicator class of the analysis row · `correct ⇔ score ≥ 1` · `given` == what was sent (after `!singlespace`) · number of answers == analysis rows |
| **R** | the JSON `expected` values sent back as answers (new session, same seed) | grade 1.0 and every `correct=true` — i.e. `expected` is usable; else retried with `expected` cut at `;` |
| **N** | same answers as W, no `special_parm` | no marker, and the page is identical to W once the comment block, session ids and the `special_parm` echo are normalised |
| **E** | — | block pure ASCII? decodes under the page charset? numbers are JSON numbers? |

The POST is percent-encoded in the charset of the `cmd=new` page (`windows-1252`), as a browser does.

## 2. Results

Types covered: `default` ×2, `radio`, `menu`, `clickfill` ×2, `numexp` ×5, `checkbox` (text options, accents), `case` ×2, `dragfill`, `numeric`, and a 12-reply `\steps` exercise (`checkbox, default, set, fset, vector, function, algexp, formal, matrix`).

| Check | GROK_V1 (PR #10) | CLAUDE_V2 proposal |
|---|---|---|
| Marker, valid JSON, schema, `seed`, numeric types | 10/10 graded exercises | 10/10 |
| `grade` == HTML score | 10/10 | 10/10 |
| `given` == sent | 10/10 | 10/10 |
| `correct` == HTML indicator | 7/7 decidable | 9/9 decidable |
| answers == analysis rows | **8/10** (00021, 00085) | 10/10 |
| `expected` replays to 1.0 | **4/10** (00004, 00008, 00014, 00021, 00045, 00085 fail) | **10/10** |
| no block without `special_parm` | 10/10 | 10/10 |
| page identical outside the block | 10/10 | 10/10 |
| block pure ASCII | 9/10 (00045) | 9/10 (00045) — by design, see F6 |

Not gradable on this bench (same with both bodies): 00055, 00058, 00072 (`function`/`formal` → `bad_exo`, no Maxima on `wims-runtime-oef`, 10 s timeout), 00027 (`canvasdraw` helper absent), 00352 (`too_many_gotos`, known).

## 3. Findings

### F1 — `expected` is not the answer for menu / radio / checkbox / clickfill (high)

The form posts the **option text** (`<input type="radio" name="reply1" value="blu e verde">`, `<option value="vera">`, `<input type="checkbox" … value="Grippe">`), whereas `replygood<N>` holds `<index>;<option list>` (`2;blu e verde,blu e arancio,…`, `2;vera,falsa`, `2,5,6;Toxoplasmose,…`, clickfill `a,n,g,l,e;a,b,c,d,e`). GROK_V1 emits that raw string. Sent back, WIMS answers « La réponse … n'est pas compréhensible » even for the bare index `2`.

WIMS resolves this itself in `scripts/anstype/{menu,radio,checkbox,…}`: `good=!rows2lines $(replygood$i)` → `good=!line 1 of $good` → `replyGood$i=!item $good of $menulist` — the text it prints as « la bonne réponse est ». `replyGood<N>` is still set when `answer.phtml` runs (`answerr.phtml` line 50 relies on it). 19 handlers use the first-row idiom (`atext case checkbox click clickfill compose coord dragfill flashcard imgcomp javacurve mark menu multipleclick nocase radio reorder textcomp wlist`); `matrix` and friends keep `;` as row separator.

**V2:** menu family → `!detag $(replyGood<N>)`; fill/text family → first row of `replygood<N>`; others unchanged. Replay 10/10.

### F2 — replies with empty `replygood` reported as wrong (medium)

00021: 5 `\answer`, the 5th has an empty good answer; `var.proc` counts it as a free point (`!advance freegot`) and `answerr.phtml` prints no row. GROK_V1 lists `{"id":"reply5","correct":false,"score":0,"expected":"","given":""}` while `grade` is 1.0 — a client deriving "all correct" from `answers[]` contradicts `grade`. **V2:** omitted.

### F3 — stepped exercises list every reply (medium)

00085 (`\steps`): step 1 grades `reply1` and `reply3`; GROK_V1 emits 12 answers, 10 of them `correct:false, score:0, given:""`. **V2:** `r<N> isitemof $gotsteps` when `$oefsteps` is set, exactly as `answer.phtml` line 31.

### F4 — no block on engine error pages (medium, spec side)

`answer.phtml` exits on `bad_exo` (line 6) *before* the injection point, and is not read at all on `bad_answer` / `empty_data` (form redisplayed, `status=waiting`, `<div class="wims_msg alert">`). HTTP is 200. Spec §6 then yields `NO_JSON_BLOCK` → « The JSON hook may not be installed on this server », which is wrong for a mistyped answer. Owner decision recorded in the GB-W7/W8 probe: no new JSON status field. **Proposal (spec GROK_V1_2 §6):** `200` + no block + `class="wims_msg alert"` → `INVALID_ANSWER` (retry, show the WIMS message); `200` + no block + no alert → `NO_JSON_BLOCK`.

### F5 — `grade` is 0 on intermediate questions of a series (low)

`out_series_qnum2`: with `qnum=2`, after question 1 `module_score` is unset (set only when `reply_cnt ≥ qnum`), `!default module_score=0` → `grade:0` with `correct:true`, and no score sentence in the HTML. Spec §3.1 does not send `qnum`, so WIMS defaults to 1 and this does not occur today. **Proposal:** state `qnum` must stay 1 in the spec; if series are wanted later, emit a provisional grade from `$allgot/$alltot` (both set in `var.proc` before `status=done`).

### F6 — encoding: the block is in the page charset, and so must be the POST (high, client side)

Spec §3.2 says the block is "ASCII-safe": false whenever an option text or a text answer has accents (00045: `Lèpre, Poliomyélite, Hépatite`). It is `windows-1252` like the page; the client already decodes by `Content-Type` (§5.2), so parsing works — only the wording is wrong. Converting to `\uNNNN` in WIMS script would need a per-character loop; not worth it.

Conversely spec §5.2 prescribes **UTF-8** for the POST body. WIMS reads form data as bytes in the page charset: `Poliomyélite` sent as UTF-8 arrives as `PoliomyÃ©lite` and is graded wrong (reproduced: replay grade 0.33 before the test encoded in `windows-1252`, 1.0 after). A browser submits in the page charset; `HttpWimsClient` must URL-encode answers with the charset of the `cmd=new` response.

### Observations (no action)

- WIMS echoes `special_parm=algogebra_json` into every navigation link of the reply page (`cmd=intro&+special_parm=algogebra_json…`); harmless for a client that never follows links, but following one would hit the known caution (`select.phtml` uses `exo=$special_parm` on intro).
- `given` is `!singlespace`d; `score` can be `0.5` (precision / partial) — both as documented in the spec caveats.
- `grade`, `seed`, `score` are emitted as JSON numbers; `correct` as a JSON boolean.

## 4. V2 body — what changed (`runtime_patch/algogebra_json_CLAUDE_V2.phtml`)

```
!if $(replygood$i)=$empty                      → skip (F2)
!if $oefsteps!=$empty and r$i notitemof $gotsteps → skip (F3)
menu radio checkbox click mark multipleclick flashcard, replyGood set
                                               → expected = !detag $(replyGood$i)   (F1)
clickfill dragfill atext case nocase compose coord imgcomp javacurve reorder textcomp wlist
                                               → expected = !line 1 of !rows2lines $(replygood$i)
otherwise                                      → expected = $(replygood$i)
choices: given = $(m_choice$i) (option text, as graded), same step filter
```

Schema, markers, escaping (`\`, `"`, `--`), header licence and the `answer.phtml` injection are unchanged; `install-algogebra-hook.*` works as is.

## 5. Recommendations

| # | Action | Who |
|---|---|---|
| 1 | Replace the body in `tools/algogebra-hook/algogebra_json.phtml` (PR #10) by V2, bump marker, extend `docs/09` recipe with one menu, one checkbox and 00085 | GBot |
| 2 | Re-run Debian 00006/00076 + the three above with V2 | GBot |
| 3 | Spec GROK_V1_2: §3.2 (`expected` = option text; block in page charset), §5.2 (POST in response charset), §6 (`INVALID_ANSWER`), §3.1 (`qnum` stays 1) | Claude drafts, Arnaud decides |
| 4 | WA-9 client: encode POST in page charset; fixtures = the 10 real W/R blocks in `out_corpus_CLAUDE_V2_hook_CLAUDE_V1.json` | Codex |
| 5 | Cover `function`/`formal`/`algexp` on a runtime with Maxima (Laragon sibling or Debian) — untested here | GBot |

## 6. Reproduce

```powershell
cd D:\ALF_ZGit\02-wims4win-claude-wt
python -X utf8 -I AlgoGebra_Wims_Integration\tests\test_algogebra_json_corpus_CLAUDE_V1.py `
  --sources D:\ALF_ZGit\02-wims4win\tools\oef-validation\html\exemples\sources `
  --out AlgoGebra_Wims_Integration\tests\out_corpus_<label>
# series probe: add --only 00002 somme_demo --qnum 2
```

Defaults: runtime `wims-runtime-oef`, `oef2wims.exe` of `wims-runtime-r19040-grok`; override with `--wims-home` / `--oef2wims`. To test another hook body, swap `public_html/scripts/oef/algogebra_json.phtml` on a bench tree you own and restore it afterwards.

---

*End of `Review_Hook_JSON_Windows_CLAUDE_V1.md`.*
