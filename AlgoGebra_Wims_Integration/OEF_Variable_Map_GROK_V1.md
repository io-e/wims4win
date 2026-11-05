<!-- GROK_V1 -->
# OEF variable map — AlgoGebra JSON hook (verified)

| Field | Value |
|---|---|
| Document | `AlgoGebra_Wims_Integration/OEF_Variable_Map_GROK_V1.md` |
| Version | GROK_V1 |
| Date | 2026-11-05 |
| Supersedes (for hook work) | `OEF_Variable_Map_CLAUDE_V1.md` (kept as archive) |
| Evidence | `Discovery_Variable_Dump_GROK_V1.md` + `tests/test_algogebra_json_GROK_V1.py` |

---

## 1. Template chain (runtime)

```
Main.phtml → answer.phtml → answerr.phtml / answerc.phtml
Grading: var.proc → ans.proc → screply.proc → module_score
```

There is **no** `reply.phtml` / `anscomp.phtml` on the Windows runtime tree used for discovery.

| File | Role |
|---|---|
| `scripts/oef/answer.phtml` | Analysis UI + **JSON hook injection** |
| `scripts/oef/algogebra_json.phtml` | Emits JSON object body |
| `scripts/oef/screply.proc` | Sets `m_sc_replyN` |
| `scripts/oef/var.proc` | Compare + `module_score` + `m_sc_choiceN` |

---

## 2. Trigger

| Item | Value |
|---|---|
| HTTP | `cmd=reply` **and** `special_parm=algogebra_json` |
| Template test | `!if $special_parm=algogebra_json` |
| Output wrapper | `<!-- ALGOGEBRA_JSON_BEGIN … ALGOGEBRA_JSON_END -->` |

**Do not send `special_parm=algogebra_json` on `cmd=new`.** OEF `select.phtml` does `!set exo=$special_parm` when `special_parm` is non-empty, which would break exercise selection. Pass the trigger only on the grading request.

---

## 3. Live phtml variables (during `answer.phtml`)

| Variable | Meaning | Range / values |
|---|---|---|
| `module_score` | Overall score | 0–10 |
| `wims_seed` | Exercise seed | integer |
| `replycnt` | Number of `\answer` fields | ≥0 |
| `choicecnt` | Number of `\choice` fields | ≥0 |
| `replyN` | Student answer | string |
| `replygoodN` | Expected (may include `;options` for checkbox) | string |
| `replytypeN` | e.g. `default`, `checkbox` | string |
| `replystyleN` | e.g. `numeric`, `mc` | string |
| `diareplyN` | `good` / `bad` | |
| `m_sc_replyN` | Per-reply score | 0 / 0.5 / 1 |
| `precreplyN` | Near-miss flag | `yes` or empty |
| `partialgoodN` | Partial credit flag | `yes` or empty |
| `choiceN` / `choicegoodN` | Choice path | |
| `diachoiceN` | `good` / `bad` | |
| `m_sc_choiceN` | 1 / 0 / -1 (“I don’t know”) | |

`m_sc_replyN` rules (`screply.proc`):

| Condition | Score |
|---|---|
| bad, not prec | 0 |
| bad + prec | 0.5 |
| good + partial | 0.5 |
| good | 1 |

---

## 4. Session file (`sessions/<sid>/var`)

Persisted names use a `w_` prefix. Useful for offline dumps; the hook should read live names above.

| Session key | Example |
|---|---|
| `w_reply1` | student value |
| `w_replygood1` | expected |
| `w_replytypes` | `checkbox` / `default` |
| `w_replystyles` | `mc` / `numeric` |
| `w_score` | 0–10 |
| `w_wims_seed` | seed |
| `w_special_parm` | `algogebra_json` when requested |

---

## 5. JSON schema emitted (V1)

```json
{
  "grade": 1.0,
  "seed": 1,
  "answers": [
    {
      "id": "reply1",
      "correct": true,
      "score": 1,
      "feedback": "",
      "expected": "2",
      "given": "2"
    }
  ],
  "feedback": ""
}
```

| Field | Source |
|---|---|
| `grade` | `module_score / 10` |
| `seed` | `wims_seed` |
| `answers[].id` | `replyN` or `choiceN` |
| `answers[].correct` | `m_sc_* >= 1` |
| `answers[].score` | `m_sc_replyN` / `m_sc_choiceN` |
| `answers[].expected` / `given` | singlespace + JSON string escapes |
| `feedback` | empty in V1 |

Known V1 limits:

- Checkbox `expected` may retain the `;optionlist` suffix from `replygoodN`.
- Per-answer and global `feedback` strings not filled yet.
- Encoding: page is `windows-1252`; keep JSON ASCII-safe.

---

## 6. Verified exercises

| Exo | Type | Wrong | Right |
|---|---|---|---|
| 00006 | numeric (`replystyles=numeric`) | grade 0, given `0`, expected `2` | grade 1 |
| 00076 | checkbox (`replytypes=checkbox`) | grade 0 | grade 1, given `3, 5` |
