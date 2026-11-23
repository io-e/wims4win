#!/usr/bin/env python3
"""GROK_V1 P4 gate P4 (grade) + P7 (\\steps). Right answers -> 100 %, wrong -> 0 %, per blank for multi-blank;
problem_user/past_answer written by WeBWorK; no WIMS class created. Writes p4_results/P4_grade.json."""
import json, re, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW, text_of
from oracle_GROK_V1 import PILOT, expected, db, P4
from oefc_GROK_V1 import call

CLASSES = WIMS_RUNTIME / "log/classes"
n_classes = lambda: len([p for p in CLASSES.iterdir() if p.is_dir() and not p.name.startswith(".")])


def values_for(module, exo, seed, exp, wrong=False):
    """WeBWorK blank values in blank order (fields of the last step view)."""
    c, j, _, _ = call("/oef/render", {"module": module, "exo": exo, "seed": int(seed), "lang": "fr"})
    fields = j["fields"]
    ids = [f["id"] for f in fields]
    # steps: the oracle knows later fields too, in order
    for k in exp:
        if k not in ids:
            ids.append(k)
    out = []
    for fid in ids:
        f = next((x for x in fields if x["id"] == fid), {"type": "text"})
        e = exp.get(fid, "")
        if f.get("type") == "radio":
            vals = [ch for ch in f["choices"]]
            good = next((ch["value"] for ch in vals if re.sub("<[^>]+>", "", ch["label"]).strip() == e), vals[0]["value"])
            out.append(next(ch["value"] for ch in vals if ch["value"] != good) if wrong else good)
        else:
            out.append("987654" if wrong else e)
    return out


def score_line(page):
    t = text_of(page)
    m = re.search(r"(?:Votre score|Your score)[^.]*?(\d+\s?%)", t)
    return m.group(1) if m else None


def main():
    # fresh state for the pilot set (own instance only)
    db("update p4pilot_problem_user set status=0,attempted=0,num_correct=0,num_incorrect=0,last_answer='',sub_status=0 "
       "where set_id='P4pilot'")
    db("delete from p4pilot_past_answer where set_id='P4pilot'")
    res = {"items": [], "classes_before": n_classes()}
    seeds = {(r["user_id"], r["problem_id"]): r["problem_seed"] for r in
             db("select user_id,problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot'")}
    for user, wrong in (("stud1", True), ("stud2", False)):
        w = WW("p4pilot", user, "ww_student_password")
        for i, (m, e) in enumerate(PILOT, 1):
            seed = seeds[(user, str(i))]
            exp = expected(m, e, seed)
            vals = values_for(m, e, seed, exp, wrong)
            item = {"user": user, "n": i, "module": m, "exo": e, "seed": int(seed), "mode": "wrong" if wrong else "right",
                    "n_blanks": len(vals)}
            page = w.view("P4pilot", i)
            form, names = WW.form_of(page)
            t0 = time.time()
            if e == "ratapprox" and not wrong:
                # P7: step 1 alone, then step 1 + step 2
                p1 = w.submit("P4pilot", i, {names[0]: vals[0]}, page=page)
                item["step1_text"] = re.findall(r"(Étape \d/\d[^<]{0,60})", p1)[:1]
                item["step1_banner"] = score_line(p1)
                f2, n2 = WW.form_of(p1)
                item["step1_blanks_after"] = n2
                out = w.submit("P4pilot", i, dict(zip(n2, vals)), page=p1)
            else:
                out = w.submit("P4pilot", i, dict(zip(names, vals)), page=page)
            item["ms"] = int(1000 * (time.time() - t0))
            item["banner"] = score_line(out)
            row = db(f"select status,attempted,num_correct,num_incorrect,last_answer from p4pilot_problem_user "
                     f"where user_id='{user}' and set_id='P4pilot' and problem_id={i}")[0]
            pa = db(f"select count(*) n from p4pilot_past_answer where user_id='{user}' and set_id='P4pilot' and problem_id={i}")[0]["n"]
            item.update(status=float(row["status"]), num_correct=int(row["num_correct"]), num_incorrect=int(row["num_incorrect"]),
                        past_answers=int(pa))
            item["pass"] = (item["status"] == 0.0) if wrong else (item["status"] == 1.0)
            res["items"].append(item)
            print(json.dumps({k: item[k] for k in ("user", "n", "exo", "mode", "n_blanks", "banner", "status",
                                                    "num_correct", "num_incorrect", "past_answers", "pass", "ms")}))
    # per-blank partial: stud3, prob2 (3 blanks) with 2 right, 1 wrong; perdecimal1 (2 blanks) 1 right 1 wrong
    w = WW("p4pilot", "stud3", "ww_student_password")
    seeds3 = {r["problem_id"]: r["problem_seed"] for r in
              db("select problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot' and user_id='stud3'")}
    for i, (m, e) in enumerate(PILOT, 1):
        if e not in ("prob2", "perdecimal1"):
            continue
        exp = expected(m, e, seeds3[str(i)])
        vals = values_for(m, e, seeds3[str(i)], exp)
        vals[-1] = "987654"
        page = w.view("P4pilot", i); form, names = WW.form_of(page)
        out = w.submit("P4pilot", i, dict(zip(names, vals)), page=page)
        row = db(f"select status from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id={i}")[0]
        want = round((len(vals) - 1) / len(vals), 4)
        item = {"user": "stud3", "n": i, "exo": e, "mode": "partial", "n_blanks": len(vals), "banner": score_line(out),
                "status": float(row["status"]), "expected_status": want, "pass": abs(float(row["status"]) - want) < 0.01}
        res["items"].append(item)
        print(json.dumps(item))
    res["classes_after"] = n_classes()
    res["no_wims_class_created"] = res["classes_after"] == res["classes_before"]
    res["pass"] = all(i["pass"] for i in res["items"]) and res["no_wims_class_created"]
    (RESULTS / "P4_grade.json").write_text(json.dumps(res, indent=1, ensure_ascii=False))
    print("P4", "PASS" if res["pass"] else "FAIL", f"{sum(i['pass'] for i in res['items'])}/{len(res['items'])}",
          "classes", res["classes_before"], "->", res["classes_after"])


if __name__ == "__main__":
    main()
