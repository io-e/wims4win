#!/usr/bin/env python3
"""GROK_V1 P4 gate P2 (no leak of expected answers). For stud1 (only wrong answers submitted, by P4) and each pilot
item: the known right text values for stud1's seed (oracle, WIMS directly) are searched, with number/word boundaries, in
 (a) worker render + check (wrong answers) responses, (b) WeBWorK HTML: student view, instructor view acting as stud1,
 instructor view with "show correct answers" requested, instructor past-answers page, (c) DB rows past_answer and
 problem_user for stud1, (d) worker log and Morbo log.
Positive control: the same search must find stud2's right values in stud2's past_answer (stud2 typed them).
Choice fields (radio) are not grep-able (every label is on screen by design); they are checked via the response
schema only (no key that could hold an expected value).
V1_1 (D1, LW rule): a value already printed by the native WIMS page for the same seed (oracle.native_statement) is
exercise data, listed as visible_in_native_wims_statement and not judged.
V1_1 (LW review item 5): P2 also checks that no SECRET value (WIMS p4grok password, WIMS P4 token, worker token,
WeBWorK passwords, Mojolicious secret, DB password) is in any WIMS, WeBWorK or worker log (same scan as P14). Writes p4_results/P2_noleak.json."""
import json, re, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW, BASE
from oracle_GROK_V1 import native_statement, PILOT, expected, db, P4
from oefc_GROK_V1 import call

BAD_KEYS = re.compile(r'"(expected|correct_ans|correct_answer|replygood|goodanswer|good|solution|answer)"\s*:', re.I)


def pat(v):
    v = v.strip()
    alts = {v, v.replace(".", ","), v.replace(",", ".")}
    return re.compile(r"(?<![\w.,/])(?:" + "|".join(re.escape(a) for a in alts) + r")(?![\w/]|[.,]\d)")


def hits(text, v):
    p = pat(v)
    return [text[max(0, m.start() - 50): m.end() + 50].replace("\n", " ") for m in p.finditer(text)]


def body_text(page):
    """Visible text of the problem body + feedback only (for 1-2 character values, page chrome gives false hits)."""
    import html as H
    i = page.find('id="problem_body"')
    if i < 0:
        i = page.find('class="problem-content')
    j = page.find('id="problemMainForm"')
    seg = page[i:] if i >= 0 else page
    seg = re.sub(r"<script.*?</script>", " ", seg, flags=re.S)
    k = seg.find('class="submit-buttons-container')
    seg = seg[:k] if k > 0 else seg
    fb = " ".join(H.unescape(x) for x in re.findall(r'data-bs-content="([^"]*)"', page))
    return H.unescape(re.sub(r"<[^>]+>", " ", seg)) + " " + re.sub(r"<[^>]+>", " ", fb)


def log_clean(t):
    """Logs carry only module/exo/seed/status/ms by design; drop the numeric metadata (timestamps, pids, request ids,
    ms=, seed=, status=, problem numbers in URL paths, Morbo timings) so an expected value cannot match them."""
    t = re.sub(r"\[[^\]\n]*\]", " ", t)
    t = re.sub(r"\b(ms|seed|status|done|pid|line)[= ]\d+", " ", t)
    t = re.sub(r"/\d+(?=/)", "/", t)
    t = re.sub(r"\(\d+(\.\d+)?s, [\d.?]+/s\)", " ", t)
    t = re.sub(r"\b\d{3} (OK|Found|Bad Request|Forbidden|Not Found)\b", " ", t)
    t = re.sub(r"line \d+|:\d{2,5}\b", " ", t)
    return t


def main():
    seeds = {(r["user_id"], r["problem_id"]): r["problem_seed"] for r in
             db("select user_id,problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot'")}
    s1 = WW("p4pilot", "stud1", "ww_student_password")
    prof = WW("p4pilot", "p4prof", "ww_prof_password")
    logs = log_clean(WORKER_LOG.read_text(errors="replace") + MORBO_LOG.read_text(errors="replace"))
    # V1_1 fix: the past-answers page is /<course>/show_answers/ (V1 queried /instructor/show_answers/, a 404 page)
    pa_page = prof.get(f"{BASE}/p4pilot/show_answers/", {"selected_users": "stud1", "selected_sets": "P4pilot",
                                                         "selected_problems": [str(k) for k in range(1, 11)], "action": "Afficher"})
    pa_ok = prof.status == 200 and "past-answer-table" in pa_page
    # surface = the answer cells of the past-answer tables (what the teacher reads); the page chrome (problem list
    # 1..10, headings) would match 1-character values
    pa_page = "\n".join(re.findall(r'<td class="px-3 (?:correct|incorrect)">(.*?)</td>', pa_page, re.S))
    items, ok = [], True
    for i, (m, e) in enumerate(PILOT, 1):
        seed = int(seeds[("stud1", str(i))])
        exp = expected(m, e, seed)
        c1, r1, rt, _ = call("/oef/render", {"module": m, "exo": e, "seed": seed, "lang": "fr"})
        types = {f["id"]: f["type"] for f in r1["fields"]}
        c2, r2, ct, _ = call("/oef/check", {"module": m, "exo": e, "seed": seed, "lang": "fr",
                                            "answers": {k: ("1" if k.startswith("choice") else "987654") for k in exp}})
        for f in (r2.get("view") or {}).get("fields", []):
            types.setdefault(f["id"], f["type"])
        sv = s1.view("P4pilot", i)
        iv = prof.view("P4pilot", i, effectiveUser="stud1")
        form, names = WW.form_of(iv)
        form.update(effectiveUser="stud1", showCorrectAnswers="1", checkAnswers="1")
        ic = prof.get(prof.url("P4pilot", i), form)
        dbrows = json.dumps(db(f"select * from p4pilot_past_answer where user_id='stud1' and set_id='P4pilot' and problem_id={i}")
                            + db(f"select * from p4pilot_problem_user where user_id='stud1' and set_id='P4pilot' and problem_id={i}"),
                            ensure_ascii=False)
        surfaces = {"worker_render": rt, "worker_check": ct, "ww_student_view": sv, "ww_instructor_view": iv,
                    "ww_instructor_show_correct": ic, "ww_instructor_past_answers": pa_page, "db_rows": dbrows, "logs": logs}
        native = native_statement(m, e, seed)
        item = {"n": i, "exo": e, "seed": seed, "expected_text_fields": {}, "choice_fields": [], "leaks": {},
                "visible_in_native_wims_statement": [],
                "bad_keys": [k for k, t in (("render", rt), ("check", ct)) if BAD_KEYS.search(t)]}
        for fid, v in exp.items():
            if types.get(fid, "text") != "text" or fid.startswith("choice"):
                item["choice_fields"].append(fid); continue
            item["expected_text_fields"][fid] = "<redacted len=%d>" % len(v)
            if hits(native, v):  # D1 (LW rule): printed by WIMS itself for this seed -> data, not judged as a leak
                item["visible_in_native_wims_statement"].append(fid); continue
            for sname, txt in surfaces.items():
                if len(v.strip()) <= 2 and sname.startswith("ww_"):
                    txt = body_text(txt)  # documented: short values searched in problem body/feedback text only
                h = hits(txt, v)
                if h:
                    item["leaks"].setdefault(sname, []).append({"field": fid, "n": len(h), "context": h[:3]})
        item["show_correct_has_correct_column"] = bool(re.search(r"(Bonne réponse|Correct Answer)", ic))
        item["pass"] = not item["leaks"] and not item["bad_keys"]
        ok &= item["pass"]
        items.append(item)
        print(("PASS" if item["pass"] else "CHECK"), i, e, "text fields", len(item["expected_text_fields"]),
              "choice", item["choice_fields"], "leaks", {k: len(v) for k, v in item["leaks"].items()}, item["bad_keys"])
    # positive control on stud2 (typed the right answers in P4)
    ctrl = []
    for i, (m, e) in enumerate(PILOT, 1):
        seed = int(seeds[("stud2", str(i))])
        exp = expected(m, e, seed)
        pa = json.dumps(db(f"select answer_string from p4pilot_past_answer where user_id='stud2' and set_id='P4pilot' and problem_id={i}"),
                        ensure_ascii=False).replace("\\t", " ")
        tv = [v for k, v in exp.items() if not k.startswith("choice") and not re.fullmatch(r"\d+", k) and k.startswith("reply")]
        found = [bool(hits(pa, v)) for v in tv]
        ctrl.append({"n": i, "exo": e, "values_found": f"{sum(found)}/{len(found)}"})
    ctrl_ok = all(c["values_found"].split("/")[0] == c["values_found"].split("/")[1] for c in ctrl
                  if c["values_found"] != "0/0")
    from gate_P14_no_secret_in_logs_GROK_V1_1 import secrets as _sec, log_files as _lf, count as _cnt
    sec = _sec(); lf = _lf(); sec_hits = _cnt(lf, sec)
    sec_ok = len(sec) >= 6 and not any(sec_hits.values())
    print("secrets in logs (files per secret, values never printed):", sec_hits, "files scanned:", len(lf))
    out = {"items": items, "positive_control_stud2_past_answer": ctrl, "positive_control_ok": ctrl_ok,
           "secrets_in_logs": {"files_scanned": len(lf), "files_containing": sec_hits, "pass": sec_ok},
           "past_answers_page_ok": pa_ok, "pass": ok and ctrl_ok and sec_ok and pa_ok}
    (RESULTS / "P2_noleak.json").write_text(json.dumps(out, indent=1, ensure_ascii=False))
    print("positive control:", ctrl, ctrl_ok)
    print("P2", "PASS" if out["pass"] else "CHECK (see contexts)")


if __name__ == "__main__":
    main()
