<!-- CLAUDE_V1 -->
# OEF variable map — quick reference for JSON hook implementation

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/OEF_Variable_Map_CLAUDE_V1.md` |
| Version | CLAUDE_V1 |
| Date | 2026-11-04 |
| Source pin | wims4win `io-e/wims4win`, WIMS 4.33a |
| Companion | `Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md` |

---

## 1. OEF compilation: .oef directives → .def variables

Source: `src/OefMsg2wims/process.c`

### Answer directives

```
\answer{Prompt text}{good_answer}{type=numeric, weight=2, option=noanalyze}
```

Produces (for answer N):

| Variable | Content | Source line |
|---|---|---|
| `replyname<N>` | Prompt text | process.c:330 |
| `replygood<N>` | Expected answer (substituted) | process.c:330 |
| `replytype<N>` | Comparison type (e.g., `numeric`) | process.c:247 |
| `replyoption<N>` | Comparison options | process.c:270 |
| `replyweight<N>` | Score weight | process.c:284 |

### Choice directives

```
\choice{Prompt}{good1, good2}{bad1, bad2}
```

Produces `choicename<N>`, `choicegood<N>`, `choicebad<N>`.

### Counters

| Variable | Content |
|---|---|
| `answercnt` | Total number of answers |
| `choicecnt` | Total number of choices |
| `feedbackcnt` | Total number of feedback blocks |
| `conditioncnt` | Total number of conditions |

---

## 2. Runtime: student answer submission

### POST parameters

When student submits (`cmd=reply`):

| Parameter | Content |
|---|---|
| `reply1`, `reply2`, ... | Student's text/numeric answers |
| `choice1`, `choice2`, ... | Student's menu/radio/checkbox selections |
| `session` | WIMS session ID |
| `module` | Module path |
| `cmd` | `reply` |
| `special_parm` | Trigger value (e.g., `algogebra_json`) |

### Score computation

After the OEF template (`reply.phtml` → `anscomp.phtml`) compares answers:

| Variable | Content | Range |
|---|---|---|
| `module_score` | Overall weighted score | 0 to 10 |
| `wims_seed` | Random seed for this exercise instance | integer |

### Per-answer result variables (to confirm via discovery)

These are set by the OEF reply template chain. **The exact names must be verified by dumping session variables after a `cmd=reply`.** Expected pattern:

| Variable | Content | Notes |
|---|---|---|
| `sc_reply<N>` | Per-answer score (0 or 1, or partial) | Needs verification |
| `reply<N>` | Student's submitted answer | From POST |
| `replygood<N>` | Expected answer | From .def |
| `replytype<N>` | Comparison type | From .def |

---

## 3. Answer types — full table

Source: `src/OefMsg2wims/process.c`, lines 173-223, `anstype[]` array.

| OEF keyword | Internal type | Comparison method | WIMS keyword |
|---|---|---|---|
| `algexp` | typ_algexp | Algebraic expression | `algexp` |
| `aset` | typ_set | Ordered set | `aset` |
| `atext` | typ_atext | Approximate text | `atext` |
| `case` | typ_case | Case-sensitive text | `case` |
| `checkbox` | typ_menu | Checkbox selection | `checkbox` |
| `chemeq` | typ_chem | Chemical equation | `chemeq` |
| `chset` | typ_atext | Character set | `chset` |
| `click` | typ_menu | Clickable area | `click` |
| `clickfill` | typ_fill | Click-fill | `clickfill` |
| `code` | typ_src | Source code | `code` |
| `compose` | typ_comp | Composition | `compose` |
| `coord` / `coordinates` | typ_coord | Coordinates | `coord` |
| `corresp` / `correspond` | typ_comp | Correspondence | `correspond` |
| `default` | typ_default | Default comparison | `default` |
| `dragfill` | typ_fill | Drag-fill | `dragfill` |
| `equation` | typ_equation | Equation | `equation` |
| `formal` | typ_formal | Formal expression | `formal` |
| `fset` | typ_set | Function set | `fset` |
| `function` | typ_func | Function equivalence | `function` |
| `imgcomp` | typ_comp | Image composition | `imgcomp` |
| `javacurve` | typ_java | Java curve | `javacurve` |
| `link` | typ_menu | Link click | `click` |
| `litexp` | typ_litexp | Literal expression | `litexp` |
| `mark` | typ_menu | Mark | `mark` |
| `matrix` | typ_matrix | Matrix | `matrix` |
| `menu` / `select` | typ_menu | Menu selection | `menu` |
| `nocase` | typ_nocase | Case-insensitive text | `nocase` |
| `number` / `numeric` | typ_num | Numeric tolerance | `numeric` |
| `numexp` | typ_algexp | Numeric expression | `numexp` |
| `radio` | typ_menu | Radio button | `radio` |
| `range` / `ranges` | typ_func | Range check | `range` |
| `raw` | typ_raw | Raw pass-through | `raw` |
| `reorder` | typ_comp | Reorder | `reorder` |
| `set` | typ_set | Set | `set` |
| `sigunits` | typ_units | SI units | `sigunits` |
| `symtext` | typ_symtext | Symbolic text | `symtext` |
| `text` | typ_text | Text (case-sensitive) | `case` |
| `textcomp` / `wordcomp` | typ_comp | Text composition | `textcomp` |
| `unit` / `units` | typ_units | Physical units | `units` |
| `vector` | typ_vector | Vector | `vector` |
| `wlist` | typ_wlist | Word list | `wlist` |

---

## 4. C entry points reference

### `wims.c` — command dispatch

```c
// line 287-291
char *commands[] = {
    "intro", "new", "renew", "reply", "config", "hint", "help",
    "resume", "next", "getins", "getframe", "getfile", "close", "ref"
};
// cmd_type index: reply = 3
```

### `wims.c` — main processing flow

```c
// line 1279-1301 (simplified)
set_variables();
p = getvar("module_score");                    // already computed by templates
if (p != NULL) force_setvar("wims_seed_score", p);
main_phtml_put(html_file);                     // renders the reply HTML
flushoutput(); close(lastout_file); putlastout();
write_logs(); save_session_vars();              // persist score to session
```

### `variables.c` — session variable persistence

```c
// line 1574-1589 (simplified)
// Writes session variables to file, including:
_write_var("module_score", varf, 1, 1);
_write_var("wims_scoring", varf, 1, 1);
_write_var("wims_seed", varf, 1, 1);
// ... all other vars via _write_vars(varf)
```

### Reading `special_parm`

```c
// Occurs at: wims.c:434, wims.c:729, wims.c:806, wims.c:870, wims.c:898, wims.c:1030
// config.c:831, exec.c:631, variables.c:214
char *sp = getvar("special_parm");
```

---

## 5. Discovery procedure

### Step 1: dump raw CGI output

```powershell
$RT = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef'
$env:OEF2HTML_DUMP = 'D:\ALF_ZGit\02-wims4win\AlgoGebra_Wims_Integration\discovery'
python tools/oef-validation/html/oef2html.py `
  --wims-home $RT --cgi "$RT\public_html\wims.cgi.exe" `
  --out AlgoGebra_Wims_Integration\discovery_html --json `
  tools/oef-validation/html/exemples/sources/00076.oef
```

### Step 2: inspect per-answer variables

After running, find the session directory:

```powershell
Get-ChildItem "$RT\sessions" -Recurse -Filter "var" |
  Select-String "module_score|sc_reply|replygood"
```

### Step 3: read OEF templates

```powershell
Get-Content "$RT\public_html\scripts\oef\reply.phtml"
Get-Content "$RT\public_html\scripts\oef\anscomp.phtml"
```

These reveal the exact per-answer variable names and the injection point for the JSON hook.

---

*End of `OEF_Variable_Map_CLAUDE_V1.md`.*
