<!-- GROK_V1_1 -->
# Discovery variable dump — AlgoGebra × WIMS JSON hook (Track A)

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Discovery_Variable_Dump_GROK_V1.md` (also under `D:\ALF_Agents\02-wims4win\`) |
| Version | GROK_V1_1 |
| Date | 2026-11-05 |
| Repo (source) | `D:\ALF_ZGit\02-wims4win` branch `feat/algogebra-wims-integration` |
| Runtime | `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef` |
| Companion plans | `Plan_Dev_AlgoGebra_Integration_CLAUDE_V2.md`, `Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md` |
| Verified map | `OEF_Variable_Map_GROK_V1.md` |
| Claude session resumed | `e87724f0-5dde-4283-82e0-e8ecc859e058` (usage-limit stop mid Track A) |

---

## 0. Scope and status

Track A goal: confirm real OEF variable names after `cmd=reply` on the Windows runtime, and locate a safe JSON injection point for `special_parm=algogebra_json`.

| Step | Status | Evidence |
|---|---|---|
| A1–A3 on **00006.oef** (numeric, 1 reply) | Done | CGI dumps + session `var` + template reads |
| A4 **00076.oef** (checkbox via `replytypes=checkbox`) | Done | `discovery_dump_00076/`, sessions VP*/KS* |
| Variable map refresh | Done | `OEF_Variable_Map_GROK_V1.md` |
| Track B Option A hook | Done (Windows runtime + smoke test) | `runtime_patch/`, `tests/test_algogebra_json_GROK_V1.py` **ALL PASS** |
| Track C–E | Open | encoding polish, packaging in `build-runtime-tree.sh`, Debian parity |

**Verdict for Option A (phtml hook):** implemented. Inject after the `answer_analysis` block in `scripts/oef/answer.phtml`.

**Trigger caution:** send `special_parm=algogebra_json` only on `cmd=reply`. On intro/new, OEF `select.phtml` assigns `exo=$special_parm`.

---

## 1. Template chain (corrected)

Earlier handoff / `AGENTS.md` assumed `reply.phtml` + `anscomp.phtml`. On this runtime:

```
Main.phtml
  └─ !read oef/answer.phtml          (Main.phtml ~289, cmd=reply path)
       ├─ language answer.phtml
       ├─ <div class="answer_analysis"> … </div>
       │    ├─ !read oef/answerr.phtml / answerc.phtml (per ansorder)
       │    ├─ color_legend.phtml
       │    └─ module score UI (uses $module_score)
       └─ optional solution block
```

Grading itself runs earlier in **`var.proc`** (reads `oef/ans.proc`, then `oef/screply.proc`, aggregates into `module_score`).

| Assumed name | Actual on runtime | Role |
|---|---|---|
| `reply.phtml` | **missing** | — |
| (reply UI / analysis) | `answer.phtml` | Analysis + score display; **hook injection host** |
| per-reply row | `answerr.phtml` | Reply good/bad presentation |
| per-choice row | `answerc.phtml` | Choice good/bad presentation |
| per-reply score | `screply.proc` | Sets `m_sc_reply<N>` |
| score aggregate | `var.proc` | Sets `module_score` (0–10) |

---

## 2. Discovery runs (00006.oef)

Tooling: `oef2html.py` CGI mode against `wims-runtime-oef`.

Artifacts (repo, currently untracked):

| File | Meaning |
|---|---|
| `AlgoGebra_Wims_Integration/discovery_dump/001.html` | `cmd=new` |
| `…/002.html` | `cmd=reply` wrong (`reply1=0`) |
| `…/003.html` | `cmd=new` again |
| `…/004.html` | `cmd=reply` correct (`reply1=2`) |
| `…/discovery_out/00006_s1.json` | Parsed exercise (seed 1) |
| `…/discovery_out/oef2html_rapport.json` | CGI call log |

### 2.1 CGI calls (from rapport)

| # | Query (abbrev.) | Result |
|---|---|---|
| 1 | `cmd=new&exo=00006&…` | session start |
| 2 | `session=WM01C1D019.1&cmd=reply&reply1=0` | wrong |
| 3 | `cmd=new&exo=00006&…` | new session |
| 4 | `session=XY01C3BA85.1&cmd=reply&reply1=2` | correct |

### 2.2 HTML outcomes

| Dump | Indicator class | Visible score text |
|---|---|---|
| 002 (wrong) | `oef_indbad` | « Vous avez obtenu une note de **0** sur 10. » |
| 004 (correct) | `oef_indgood` | « Vous avez obtenu une note de **10** sur 10. » |

Expected answer shown in analysis: `<div class="inline tt">2</div>` (matches `replygood1`).

### 2.3 Encoding

Both CGI `Content-type` and `<meta charset>`:

```text
charset=windows-1252
```

JSON hook contract: emit ASCII with `\uNNNN` escapes (or document an explicit encoding agreement with AlgoGebra).

---

## 3. Session `var` dump (persisted `w_*`)

`m_sc_reply*` / `diareply*` are **phtml-runtime** names. They are used while templates run; the session file stores the **`w_`-prefixed** snapshot.

### 3.1 Wrong answer — `sessions/WM01C1D019/var`

| Key | Value | Notes |
|---|---|---|
| `w_cmd` | `reply` | |
| `w_exo` | `00006` | |
| `w_wims_seed` | `1` | |
| `w_replycnt` / `w_reply_cnt` | `1` | |
| `w_choicecnt` | `0` | numeric-only exercise |
| `w_reply1` | `0` | student given |
| `w_replygood1` | `2` | expected |
| `w_replynames` | `Valore della tangente di \(a\)` | |
| `w_replystyles` | `numeric` | |
| `w_replytypes` | `default` | |
| `w_replyoptions` | `absolute` | |
| `w_score` | `0` | 0–10 |
| `w_score_got` | `0` | |
| `w_score_should` | `1` | |
| `w_wims_seed_score` | `0` | |
| `w_special_parm` | *(empty)* | discovery did not pass `algogebra_json` |

### 3.2 Correct answer — `sessions/XY01C3BA85/var`

| Key | Value |
|---|---|
| `w_reply1` | `2` |
| `w_replygood1` | `2` |
| `w_score` | `10` |
| `w_score_got` | `1` |
| `w_score_should` | `1` |
| `w_wims_seed_score` | `10` |
| `w_wims_seed` | `1` |
| `w_choicecnt` | `0` |

**Implication for the hook:** during `answer.phtml`, prefer live names (`replyN`, `replygoodN`, `m_sc_replyN`, `module_score`). Session `w_*` is what remains after the request for offline inspection.

---

## 4. Runtime scoring variables (from templates)

### 4.1 Per-reply — `screply.proc`

```text
diareplyN=bad  + precreplyN!=yes  → m_sc_replyN=0
diareplyN=bad  + precreplyN=yes   → m_sc_replyN=0.5
diareplyN=good + partialgoodN=yes → m_sc_replyN=0.5
diareplyN=good                    → m_sc_replyN=1
```

Called from `var.proc` after `ans.proc` comparison (~367, ~381) and from `step.proc`.

### 4.2 Per-choice — `var.proc` (~425–441)

| Condition | `diachoiceN` | `m_sc_choiceN` |
|---|---|---|
| Selected item in `choicegoodN` (or QCM “good”) | `good` | `1` |
| Wrong | `bad` | `0` |
| “I don’t know” slot (`choiceitemsN+1`) | `bad` | `-1` |

**Not exercised** by 00006 (`choicecnt=0`). Confirm on A4.

### 4.3 Overall — `var.proc` (~484–487)

```text
score = min(10, rint(100 * (score_got/score_should)^scorepower) / 10)
module_score = score   # 0–10 scale
```

Optional time penalty can zero or shrink `module_score` when `delay1`/`delay2` apply.

### 4.4 UI indicator CSS classes (`answer.phtml`)

| Class | Meaning |
|---|---|
| `oef_indgood` | good |
| `oef_indbad` | bad |
| `oef_indpartial` | partial good |
| `oef_indprec` | precision / near-miss |
| `oef_modulescore` | wraps localized score sentence |

---

## 5. Recommended JSON field mapping (for Track B)

Draft mapping for Option A, based on verified names:

| JSON field (concept) | Primary source in phtml | Fallback / notes |
|---|---|---|
| `seed` | `$wims_seed` | session `w_wims_seed` |
| `grade` / overall | `$module_score` / 10 | or `$score` |
| `answers[].index` | loop `1..$replycnt` | also walk `$ansorder` |
| `answers[].name` | `$replynameN` | |
| `answers[].given` | `$replyN` / `$m_replyN` | |
| `answers[].expected` | `$replygoodN` | may contain analyze tokens |
| `answers[].type` | `$replytypeN` / `$replystyleN` | |
| `answers[].score` | `$m_sc_replyN` | 0 / 0.5 / 1 |
| `answers[].status` | `$diareplyN` | `good` / `bad` |
| `choices[].…` | `$choicenameN`, `$choiceN`, `$diachoiceN`, `$m_sc_choiceN` | A4 required |
| trigger | `$special_parm` equals `algogebra_json` | keep silent otherwise |

Emit inside HTML comment:

```html
<!-- ALGOGEBRA_JSON_BEGIN
{ ... ASCII / \u escapes ... }
ALGOGEBRA_JSON_END -->
```

### Injection point

In `scripts/oef/answer.phtml`, **after** line `!!end of .answer_analysis` / closing `</div>` of `answer_analysis` (~115–116), **before** the solution block (~117+):

```text
!if $special_parm=algogebra_json
  !read oef/algogebra_json.phtml
!endif
```

Rationale: all grading vars are already set; analysis UI is complete; solution may still be omitted depending on `givesol`.

---

## 6. Corrections to prior docs

| Document claim | Correction |
|---|---|
| Inject into `reply.phtml` | Use **`answer.phtml`**; `reply.phtml` absent |
| `anscomp.phtml` as compare host | Comparison is `var.proc` + `ans.proc` + `screply.proc` |
| Session always holds `sc_replyN` | Session holds **`w_reply*` / `w_score*`**; `m_sc_replyN` is live phtml |
| AGENTS.md “reply.phtml, anscomp.phtml” paths | Stale for this runtime tree |

Leave historical `CLAUDE_*` files as archives; prefer a new `OEF_Variable_Map_GROK_V1.md` (or bump sibling) when updating the map.

---

## 7. A4 results — 00076.oef (checkbox)

Artifacts: `discovery_dump_00076/`, `discovery_out_00076/`.

| Case | Session | `reply1` | HTML score | Notes |
|---|---|---|---|---|
| Wrong | `VP1579AAF9` | `1` | 0 / 10 | `oef_indbad` |
| Right | `KS157BC075` | `3, 5` | 10 / 10 | `oef_indgood` |

Session highlights (right): `w_replytypes=checkbox`, `w_replystyles=mc`, `w_replygood1=3,5;1,2,3,4,5,6`, `w_choicecnt=0`.

Checkbox exercises in this corpus still use the **`replyN` + `m_sc_replyN`** path (not `\choice` / `m_sc_choiceN`). The choice loop in the hook is reserved for true `\choice` directives.

---

## 8. Track B implementation (done on Windows)

| Piece | Location |
|---|---|
| Hook body | `runtime_patch/algogebra_json.phtml` → runtime `scripts/oef/` |
| Injection | `answer.phtml` after `answer_analysis` (snippet + install script) |
| Installer | `runtime_patch/install_algogebra_hook_GROK_V1.ps1` |
| Smoke test | `tests/test_algogebra_json_GROK_V1.py` (numeric + checkbox, with/without trigger) |

Verified JSON examples (seed 1):

- 00006 wrong: `grade=0`, `given=0`, `expected=2`
- 00006 right: `grade=1`, `correct=true`
- 00076 wrong/right: same schema; expected may include `;optionlist`

---

## 9. Open work

1. Wire hook copy into `tools/build-runtime-tree.sh` (Debian packaging path).
2. Track C — richer escaping / `\uNNNN`, trim checkbox `expected` before `;`, fill feedback strings.
3. True `\choice` exercise discovery (nonzero `choicecnt`).
4. GBot Debian parity when requested.

---

## 10. Uncertainty / limits

- Claude transcript recovery skipped many records; this dump trusts **disk artifacts + template source + smoke test**.
- `m_sc_replyN` verified via `screply.proc` and via emitted JSON scores in the smoke test.
- V1 feedback fields are empty strings.
- Checkbox `expected` currently passes through full `replygoodN` including the option list after `;`.
)
