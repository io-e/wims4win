#!/usr/bin/env python3
"""GROK_V1_1 P4 gate P13 (OEF drop-down menu field; test set P4menu of course p4pilot, NOT part of the pilot).
Exercise U2/arithmetic/oefrational.fr perden: reply1 = \\answer{type=menu} (multiple|diviseur), reply2 = period.
Independent oracle (no WIMS): the period of a rational whose denominator is the product of the primes shown in the
statement is the multiplicative order of 10 modulo that product; the right menu choice is "multiple".
Checks: worker render lists the 2 choices and keeps the text after the menu (V1 parsing bug) and reply2; WeBWorK page
shows both choices; right/right -> 100 %, wrong menu + right period -> 50 % (WeBWorK per-blank average = grade of record; WIMS's own
global score is reported, not asserted), wrong/wrong -> 0 %; per_field from the worker match; no leak: the period does not appear in the page served before the answer (unless the
native WIMS statement of the same seed shows it), no WIMS URL/session id, correct answers column empty.
Writes p4_results/P13_menu.json. Resets set P4menu for stud1/stud2 (own instance)."""
import json, math, re, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from ww_client_GROK_V1 import WW, text_of
from oracle_GROK_V1 import db
from oefc_GROK_V1 import call
from gate_P2_noleak_GROK_V1 import body_text

M, EXO = "U2/arithmetic/oefrational.fr", "perden"


def order10(n):
    k, x = 1, 10 % n
    while x != 1:
        x = x * 10 % n; k += 1
        if k > 10 ** 7:
            raise ValueError("no order")
    return k


def primes_of(statement_text):
    m = re.search(r"nombres premiers\s+([\d,\s]+?)\.", statement_text)
    return [int(x) for x in re.findall(r"\d+", m.group(1))] if m else []


def main():
    db("update p4pilot_problem_user set status=0,attempted=0,num_correct=0,num_incorrect=0,last_answer='',sub_status=0 where set_id='P4menu'")
    db("delete from p4pilot_past_answer where set_id='P4menu'")
    seeds = {r["user_id"]: int(r["problem_seed"]) for r in db("select user_id,problem_seed from p4pilot_problem_user where set_id='P4menu'")}
    res = {"items": []}
    ok = True
    for user, plan in (("stud1", [("wrong/wrong", ["diviseur", "987654"], 0.0), ("wrong menu/right period", ["diviseur", None], 0.5)]),
                       ("stud2", [("right/right", ["multiple", None], 1.0)])):
        seed = seeds[user]
        c, j, _, _ = call("/oef/render", {"module": M, "exo": EXO, "seed": seed, "lang": "fr"})
        fields = j["fields"]
        st_txt = re.sub(r"<[^>]+>", " ", j["statement_html"])
        pr = primes_of(st_txt)
        period = order10(math.prod(pr)) if pr else None
        render_ok = (c == 200 and [f["id"] for f in fields] == ["reply1", "reply2"] and fields[0]["type"] == "menu"
                     and sorted(ch["value"] for ch in fields[0]["choices"]) == ["diviseur", "multiple"]
                     and "{{P4:reply2}}" in j["statement_html"] and bool(pr))
        w = WW("p4pilot", user, "ww_student_password")
        page = w.view("P4menu", 1)
        form, names = WW.form_of(page)
        shown = [v for v in ("multiple", "diviseur") if re.search(rf'value="{v}"', page)]
        bt = body_text(page)
        native_has = str(period) in st_txt
        leak = (not native_has and re.search(rf"(?<![\d.]){period}(?![\d.])", bt) is not None) or re.search(r"session=[A-Z0-9]{8}|wims\.cgi", page) is not None
        for label, vals, want in plan:
            vals = [str(period) if v is None else v for v in vals]
            cj = call("/oef/check", {"module": M, "exo": EXO, "seed": seed, "lang": "fr", "answers": {"reply1": vals[0], "reply2": vals[1]}})[1]
            out = w.submit("P4menu", 1, dict(zip(names, vals)), page=page)
            row = db(f"select status from p4pilot_problem_user where user_id='{user}' and set_id='P4menu' and problem_id=1")[0]
            pa = db(f"select scores from p4pilot_past_answer where user_id='{user}' and set_id='P4menu' and problem_id=1 order by answer_id desc limit 1")
            correct_col = re.findall(r'class="[^"]*correct-ans[^"]*"[^>]*>(.*?)</', out)
            item = {"user": user, "seed": seed, "case": label, "primes": pr, "period_oracle": period, "render_ok": render_ok,
                    "choices_shown_in_webwork": shown, "blanks": len(names), "worker_score": cj.get("score"),
                    "worker_per_field": [(p["id"], p["score"]) for p in cj.get("per_field", [])],
                    "ww_status_last": float(row["status"]) if user == "stud2" or label.startswith("wrong/") else None,
                    "past_answer_scores": pa[0]["scores"] if pa else None, "leak_in_view": leak, "period_in_native_statement": native_has,
                    "correct_answer_cells": [re.sub("<[^>]+>", "", x).strip() for x in correct_col]}
            exp_pa = {0.0: "00", 0.5: "01", 1.0: "11"}[want]
            # grade of record = WeBWorK's per-blank average (past_answer scores); the WIMS global score (worker "score")
            # follows WIMS's own formula and is informative only (0.25 here for 1 of 2 right, see the report)
            exp_pf = [("reply1", int(exp_pa[0])), ("reply2", int(exp_pa[1]))]
            item["pass"] = (render_ok and len(shown) == 2 and len(names) == 2 and item["worker_per_field"] == exp_pf
                            and item["past_answer_scores"] == exp_pa and not leak and not any(item["correct_answer_cells"]))
            ok &= item["pass"]
            res["items"].append(item); print(item)
            page = w.view("P4menu", 1); form, names = WW.form_of(page)
    st = {r["user_id"]: float(r["status"]) for r in db("select user_id,status from p4pilot_problem_user where set_id='P4menu' and user_id in ('stud1','stud2')")}
    res["status_final"] = st
    ok &= st.get("stud2") == 1.0 and st.get("stud1") == 0.5
    res["pass"] = bool(ok)
    res["note"] = "WeBWorK status = best of the per-blank averages; the menu is shown as WeBWorK radio buttons."
    (RESULTS / "P13_menu.json").write_text(json.dumps(res, indent=1, ensure_ascii=False))
    print("final status", st)
    print("P13", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
