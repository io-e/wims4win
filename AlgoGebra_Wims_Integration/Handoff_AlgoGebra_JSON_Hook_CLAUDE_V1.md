<!-- CLAUDE_V1 -->
# Handoff — L2-1 JSON output hook for AlgoGebra in wims4win

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md` |
| Version | CLAUDE_V1 |
| Date | 2026-11-04 |
| Status | **Handoff** — ready for implementation |
| Action | WA-8 in `Plan_Development_Phase2_CLAUDE_V1.md` |
| Repo | `io-e/wims4win` (private) |
| License | GPL v2+ (WIMS source); hook is WIMS-side, not AlgoGebra-side |
| Consumer spec | `Spec_WIMS_Message_Integration_CLAUDE_V1.md` (§3.2, §4.4) |
| Blocks | WA-9 (rebase mocks on hook protocol), Track 4 connected mode |

---

## 1. Goal

When a `cmd=reply` request includes `special_parm=algogebra_json`, the WIMS OEF reply page must contain an embedded JSON block with per-answer grading results, in addition to the normal HTML. AlgoGebra extracts this block and ignores the surrounding HTML.

**This is the only server-side change AlgoGebra needs from WIMS.**

---

## 2. OEF grading pipeline — how it works today

Understanding the pipeline is essential before touching it.

### 2.1 OEF compilation (oef2wims)

```
.oef source → oef2wims → .def file (WIMS internal format)
```

`oef2wims` (`src/OefMsg2wims/oef2wims.c`) translates OEF directives into WIMS variables:

| OEF directive | WIMS variable | Purpose |
|---|---|---|
| `\answer{prompt}{good_answer}` | `replyname<N>`, `replygood<N>` | Expected answer |
| `\answer{...}{...}{type=numeric}` | `replytype<N>=numeric` | Answer comparison type |
| `\answer{...}{...}{option=...}` | `replyoption<N>` | Comparison options |
| `\answer{...}{...}{weight=...}` | `replyweight<N>` | Score weight |
| `\choice{...}{...}{...}` | `choicename<N>`, `choicegood<N>` | Multiple-choice |
| `\condition{text}{expr}` | `condition<N>` | Post-evaluation conditions |

Source: `src/OefMsg2wims/process.c`, function `p_answer()` (lines 227-380).

### 2.2 Answer types

The `.def` stores `replytype<N>` — the comparison method. The full type table is in `process.c` lines 173-223:

| Type | WIMS keyword | Comparison method |
|---|---|---|
| `numeric` | `numeric` | Numerical tolerance (via `precision`) |
| `function` | `function` | Functional equivalence (variables) |
| `case` / `text` | `case` | Case-sensitive text |
| `nocase` | `nocase` | Case-insensitive text |
| `menu` / `radio` / `checkbox` | `menu` / `radio` / `checkbox` | Exact option match |
| `set` / `aset` / `fset` | `set` / `aset` / `fset` | Unordered set comparison |
| `algexp` / `litexp` | `algexp` / `litexp` | Algebraic expression |
| `units` / `sigunits` | `units` / `sigunits` | Units conversion |
| `coord` | `coord` | Coordinates |
| `matrix` / `vector` | `matrix` / `vector` | Matrix/vector equality |
| `raw` | `raw` | Pass-through (external evaluation) |
| `compose` / `reorder` / `correspond` | various | Structural matching |

### 2.3 Runtime flow: `cmd=new` → `cmd=reply`

```
Browser                           WIMS server (wims.cgi)
   |                                  |
   |--- GET cmd=new&module=...&exo=N->|
   |                                  |-- load .def, substitute random vars
   |                                  |-- render exercise HTML (phtml templates)
   |<-- HTML with form fields --------|
   |                                  |
   |--- POST cmd=reply&ans1=X&ans2=Y->|
   |                                  |-- read student answers from POST
   |                                  |-- compare each ansN vs replygoodN
   |                                  |      using replytypeN comparison
   |                                  |-- compute module_score (weighted)
   |                                  |-- render grading HTML (answer_analysis)
   |<-- HTML with score + feedback ---|
```

### 2.4 Where things happen in C

| Step | Source file | Key function / variable |
|---|---|---|
| Parse `cmd` type | `wims.c:287-291` | `commands[]` array: index 3 = `"reply"` |
| Read `special_parm` | `wims.c:274`, `config.c:831` | `getvar("special_parm")` |
| Student answers from POST | `wims.c:1059-1087` | `formdata()` |
| Variable substitution | `evalue.c:49` | `substit()` |
| Score computation | `score.c:46-98` | `getscoreuser()` — reads `module_score` |
| Score logging | `log.c:187-260` | `write_logs()` — reads `module_score` |
| Session variable save | `variables.c:1550-1589` | `save_session_vars()` — writes `module_score` to var file |
| HTML output | `wims.c:1297` | `main_phtml_put(html_file)` — renders phtml template |
| Output flush | `wims.c:1299` | `flushoutput(); close(lastout_file); putlastout()` |

### 2.5 Key insight: `module_score` is already computed

By the time WIMS reaches the HTML rendering phase (`main_phtml_put` at `wims.c:1297`), the score is already computed and stored in the `module_score` variable. The answer comparison happens inside the OEF phtml template chain (in `public_html/scripts/oef/`), not in C code.

The C code only:
1. Reads student POST data
2. Loads the `.def` file variables
3. Hands off to the OEF template scripts (phtml) for comparison
4. Reads the resulting `module_score`
5. Logs it and saves it

### 2.6 OEF template chain

The OEF templates live in `public_html/scripts/oef/` (not in the wims4win repo — they're in the upstream WIMS distribution or the runtime tree built by `build-runtime-tree.sh`).

Key templates in the reply flow:

```
scripts/oef/main.phtml        ← entry point
scripts/oef/reply.phtml       ← reply processing
scripts/oef/anscomp.phtml     ← answer comparison logic
scripts/oef/var.proc           ← variable processing
```

**`reply.phtml`** is where:
- Each `reply<N>` is compared to `replygood<N>` using `replytype<N>`
- Per-answer scores are computed
- `module_score` is set (weighted average)
- The `answer_analysis` div is rendered

### 2.7 The HTML output for `cmd=reply`

The reply page contains these CSS-classed divs (visible in `oef2html.py` extraction):

```html
<div class="answer_analysis">
  <table>
    <tr>
      <td>reply1 prompt</td>
      <td>student answer</td>
      <td><span class="oef_indgood">✓</span> or <span class="oef_indbad">✗</span></td>
      <td>La bonne réponse est: ...</td>
    </tr>
    ...
  </table>
</div>
<div class="oef_feedbacks">
  <!-- conditional feedback blocks -->
</div>
<div class="wims_widget_box clearall">
  <h2>Solution</h2>
  <div class="box_content">...</div>
</div>
```

---

## 3. The hook: what to build

### 3.1 Trigger

When `special_parm` equals `algogebra_json`, inject an HTML comment block containing structured JSON into the reply page output.

### 3.2 Injection point

**Option A (recommended): In `reply.phtml`** — after the existing `answer_analysis` div is rendered, before the page footer.

Add a conditional block:

```
!if $special_parm = algogebra_json
  <!-- ALGOGEBRA_JSON_BEGIN
  !exec algogebra_json_hook
  ALGOGEBRA_JSON_END -->
!endif
```

Where `algogebra_json_hook` is a new script that reads the already-computed per-answer variables and emits JSON.

**Option B: In `wims.c`** — after `main_phtml_put()` returns but before `flushoutput()`. Less clean (mixes C output with template output) but doesn't touch the upstream OEF templates.

**Recommendation:** Option A. The phtml template is the natural place — it has access to all OEF variables (`replygood<N>`, `replytype<N>`, `module_score`, per-answer results) without requiring C code changes.

### 3.3 JSON schema

The JSON block AlgoGebra expects (from `Spec_WIMS_Message_Integration_CLAUDE_V1.md` §3.2):

```json
{
  "grade": 0.75,
  "answers": [
    {
      "id": "reply1",
      "correct": true,
      "score": 1.0,
      "feedback": "Correct.",
      "expected": "42",
      "given": "42"
    },
    {
      "id": "reply2",
      "correct": false,
      "score": 0.0,
      "feedback": "La bonne réponse est: 2*x",
      "expected": "2*x",
      "given": "3x"
    }
  ],
  "feedback": "Vous avez obtenu 7.5/10.",
  "seed": 12345
}
```

| Field | Source | WIMS variable |
|---|---|---|
| `grade` | Overall score | `$module_score` (0-10 range → divide by 10 for 0.0-1.0) |
| `answers[i].id` | Answer field name | `reply<N>` or `choice<N>` |
| `answers[i].correct` | Per-answer result | `$sc_reply<N>` or CSS class `oef_indgood` / `oef_indbad` |
| `answers[i].score` | Per-answer weight score | Per-answer score variable |
| `answers[i].expected` | Good answer | `$replygood<N>` |
| `answers[i].given` | Student answer | `$reply<N>` |
| `answers[i].feedback` | Per-answer text | Extracted from `answer_analysis` row |
| `feedback` | Overall feedback | `$oef_feedbacks` content (text) |
| `seed` | Random seed | `$wims_seed` |

### 3.4 Per-answer score variables

After the OEF answer comparison runs, each answer's result is stored in variables. The exact variable names depend on the OEF template version, but the pattern is:

```
sc_reply1, sc_reply2, ...    — per-answer score (0 or 1, or partial)
replygood1, replygood2, ...  — expected answer
reply1, reply2, ...          — student answer
replytype1, replytype2, ...  — comparison type
replyweight1, replyweight2, ... — answer weight
```

To confirm the exact variable names, dump the session variables after a `cmd=reply` by running `oef2html.py` with `OEF2HTML_DUMP=<dir>` and inspecting the raw CGI output.

---

## 4. Implementation plan

### 4.1 Discovery step (required first)

Run `oef2html.py` with `OEF2HTML_DUMP` on a known OEF exercise and inspect:

1. The raw HTML of the `cmd=reply` response
2. The session `var` file (in `sessions/<sid>/var` or `s2/<sid>/var`) — lists all WIMS variables including per-answer scores
3. The OEF template files in `public_html/scripts/oef/` — especially `reply.phtml` and `anscomp.phtml`

This tells you the exact variable names for per-answer results and where to inject the hook.

```powershell
$RT='D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef'
$env:OEF2HTML_DUMP="D:\ALF_ZGit\02-wims4win\AlgoGebra_Wims_Integration\discovery_dump"
python tools/oef-validation/html/oef2html.py `
  --wims-home $RT `
  --oef2wims <path_to_oef2wims> `
  --cgi "$RT\public_html\wims.cgi.exe" `
  --out AlgoGebra_Wims_Integration\discovery_out `
  --json tools/oef-validation/html/exemples/sources/00076.oef
```

Then inspect `discovery_dump/002.html` (the `cmd=reply` response) and `$RT/sessions/*/var`.

### 4.2 Template approach

Create `public_html/scripts/oef/algogebra_json.phtml`:

```
!comment This hook emits a JSON block for AlgoGebra consumption.
!comment Triggered only when special_parm=algogebra_json.
!comment It reads the already-computed OEF answer variables.

!set json_grade = $[$module_score / 10]
!set json_seed  = $wims_seed
!set json_buf   = {"grade": $json_grade, "seed": $json_seed, "answers": [

!for i = 1 to $answercnt
  !if $i > 1
    !set json_buf = $json_buf,
  !endif
  !set sc_i = $sc_reply$i
  !set correct_i = !if $sc_i >= 1 then true else false
  !set json_buf = $json_buf {"id": "reply$i", "correct": $correct_i, "score": $sc_i, "expected": "$replygood$i", "given": "$reply$i"}
!next i

!set json_buf = $json_buf ], "feedback": "$oef_global_feedback"}
!print $json_buf
```

(This is **pseudo-WIMS script** — the exact syntax depends on the WIMS template engine features. The discovery step reveals the actual variable names and available control structures.)

### 4.3 Hook injection in `reply.phtml`

After the answer analysis block and before the page footer, add:

```
!if $wims_special_parm = algogebra_json
  !print <!-- ALGOGEBRA_JSON_BEGIN
  !changeto oef/algogebra_json.phtml
  !print ALGOGEBRA_JSON_END -->
!endif
```

### 4.4 C-side alternative (Option B)

If modifying the OEF phtml templates is problematic (they're upstream), the hook can be added in `wims.c` between `main_phtml_put()` and `flushoutput()`:

```c
/* wims.c, after line ~1299 */
if(cmd_type == cmd_reply) {
    char *sp = getvar("special_parm");
    if(sp != NULL && strcmp(sp, "algogebra_json") == 0) {
        algogebra_json_output();  /* new function */
    }
}
```

Where `algogebra_json_output()` reads the session variables and emits the JSON comment block. This would go in a new file `algogebra.c` to minimize changes to upstream sources.

**Tradeoff:** Option B doesn't touch upstream templates (cleaner for wims4win patching), but it means the C code must know the OEF variable naming scheme (coupling).

---

## 5. Files to study in the runtime tree

These are in the installed WIMS runtime, not in the wims4win source repo:

| Path | Purpose | Why study it |
|---|---|---|
| `public_html/scripts/oef/main.phtml` | OEF module entry | Understand template dispatch |
| `public_html/scripts/oef/reply.phtml` | Reply processing | Where to inject the hook |
| `public_html/scripts/oef/anscomp.phtml` | Answer comparison | Per-answer score variable names |
| `public_html/scripts/oef/var.proc` | Variable processing | Seed, score variables |
| `public_html/scripts/oef/mkindex` | Module index builder | Exercise listing |
| `sessions/<sid>/var` | Session variables after reply | Actual per-answer variable dump |

Runtime location: `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef`

---

## 6. Constraints

1. **GPL v2+:** All code in `wims4win` is GPL. The hook is WIMS-side code, licensed under WIMS's GPL. It is NOT part of AlgoGebra (EUPL). No license conflict.

2. **No `adm/raw` for grading:** The JSON hook triggers on `cmd=reply` + `special_parm=algogebra_json` — the stock OEF grading path. Never route through `adm/raw` (see D-WIMS-G01).

3. **Backward compatibility:** When `special_parm != algogebra_json`, the reply page is unchanged. The hook is purely additive.

4. **Encoding:** WIMS outputs `windows-1252` by default. The JSON block should use ASCII with `\uNNNN` escapes for non-ASCII, or the AlgoGebra client must handle encoding (already specified in `Spec_WIMS_Message_Integration_CLAUDE_V1.md` §5.3).

5. **No upstream merge target:** This hook is specific to AlgoGebra/wims4win. It's a patch on the local fork, not an upstream contribution.

6. **`common` is untouchable:** This hook only touches WIMS server code. The GeoGebra `common` module is not involved.

---

## 7. Test plan

### 7.1 Unit test: OEF exercise with known answers

1. Take exercise `00076.oef` (numeric, function answers — known to work in oef2html)
2. Compile with oef2wims
3. `cmd=new` → extract session + form fields
4. `cmd=reply` with correct answers + `special_parm=algogebra_json`
5. Verify `ALGOGEBRA_JSON_BEGIN` block exists
6. Parse JSON → verify `grade` is 1.0 (all correct)
7. Verify each answer has `correct: true`

### 7.2 Unit test: wrong answers

1. Same exercise, submit wrong answers
2. Verify `grade` < 1.0
3. Verify `correct: false` for wrong answers
4. Verify `expected` contains the correct answer
5. Verify `given` contains the student answer

### 7.3 Unit test: mixed answer types

1. Take exercise `00006.oef` (menu/choice type)
2. Submit correct and wrong choices
3. Verify JSON reflects per-choice results

### 7.4 Regression test: no hook without `special_parm`

1. `cmd=reply` WITHOUT `special_parm=algogebra_json`
2. Verify response does NOT contain `ALGOGEBRA_JSON`
3. Verify normal grading HTML is unchanged

### 7.5 Integration test: oef2html comparison

1. Run `oef2html.py` on sample exercises WITH and WITHOUT the hook
2. Verify HTML output (excluding the JSON block) is identical
3. Verify JSON extraction matches `oef2html.py`'s own answer parsing

### 7.6 End-to-end (Phase 2, after Codex builds HttpWimsClient)

1. AlgoGebra desktop → `cmd=new` → display exercise
2. Student answers → `cmd=reply` + `special_parm=algogebra_json`
3. AlgoGebra extracts JSON → displays per-answer feedback
4. Verify on Debian (Grok) and Windows (Codex)

---

## 8. Deliverables

| # | File | Description |
|---|---|---|
| 1 | `algogebra_json.phtml` or `algogebra.c` | The hook implementation |
| 2 | `reply.phtml.patch` or `wims.c.patch` | Minimal patch for injection point |
| 3 | `test_algogebra_json.py` | Automated test (extend oef2html.py pattern) |
| 4 | Variable dump from discovery step | Evidence of actual variable names |
| 5 | Evidence report | Per test plan, with dates, commands, results |

---

## 9. Quick reference: existing tooling

| Tool | Location | Use |
|---|---|---|
| `oef2wims` | `src/OefMsg2wims/` (C source) | Compile .oef → .def |
| `oef2html.py` | `tools/oef-validation/html/` | End-to-end HTML generation via CGI |
| `OEF2HTML_DUMP` | env var | Dumps raw CGI responses for debugging |
| `wims.cgi.exe` | Runtime tree, `public_html/` | WIMS engine (CGI mode) |
| Test corpus | `tools/oef-validation/html/exemples/sources/` | Known-good OEF exercises |
| oef2wims test | `tools/oef-validation/oef2wims/` | Compilation parity tests |

---

## 10. Agent assignment

| Step | Agent | Notes |
|---|---|---|
| Discovery (§4.1) | **Grok** | Has access to Debian runtime + wims4win runtime on Windows |
| Implementation (§4.2-4.4) | **Grok** | WIMS-side code, GPL, Grok's lane |
| Tests (§7.1-7.5) | **Grok** | Uses oef2html.py pattern |
| Integration test (§7.6) | **Grok + Codex** | Grok = WIMS server, Codex = AlgoGebra client |
| Review | **Claude** | Verify JSON schema matches spec |

---

*End of `Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md`.*
