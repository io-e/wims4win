#!/usr/bin/env python3
"""GROK_V1 P4 gate P6 (decimal comma). prob2 (H3/analysis/oeflinf.fr, numexp prix/poids + algexp P(x)) and approx
(U2/arithmetic/oefrational.fr, type=set, where the comma is a separator). First what WIMS does alone (worker
normalisation bypassed by sending the raw WIMS call through the oracle path), then the worker, then WeBWorK end to end
(stud3, problem 4). Writes p4_results/P6_comma.json."""
import json, re, sys, urllib.parse
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from oefc_GROK_V1 import call
from oracle_GROK_V1 import expected, db, P4, _get, L, R
from ww_client_GROK_V1 import WW


def wims_raw(module, exo, seed, answers):
    """Direct WIMS reply with p4_json, no worker (what WIMS itself does with a comma)."""
    h = _get({"module": "adm/raw", "job": "lightpopup", "emod": module, "option": "noabout", "lang": "fr",
              "parm": f"cmd=new;exo={exo};qnum=1;seed={seed}"})
    sid = re.search(r'name="session" value="([A-Z0-9]+)', h).group(1)
    q = {"session": sid, "module": module, "cmd": "reply", "special_parm": "p4_json", "lang": "fr", **answers}
    out = _get(q)
    m = re.search(r"P4_JSON_BEGIN(.*?)P4_JSON_END", out, re.S)
    return json.loads(m.group(1)) if m else None


def main():
    seed = int(db("select problem_seed from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id=4")[0]["problem_seed"])
    exp = expected(L, "prob2", seed)
    dot = [exp["reply1"], exp["reply2"], exp["reply3"]]
    com = [v.replace(".", ",") for v in dot]
    res = {"prob2_seed": seed, "wims_alone": [], "worker": [], "webwork": []}
    for label, a in (("dots", dot), ("commas", com)):
        d = wims_raw(L, "prob2", seed, dict(zip(("reply1", "reply2", "reply3"), a)))
        res["wims_alone"].append({"case": label, "kind": d and d.get("kind"), "grade": d and d.get("grade")})
    num = exp["reply2"]
    cases = [("dots", dot, 1, "final"), ("commas everywhere (algexp + numexp)", com, 1, "final"),
             ("trailing zero with comma", [dot[0], num.replace(".", ",") + "0", com[2]], 1, "final"),
             ("comma, one digit short (must be read as a number, wrong)", [dot[0], num.replace(".", ",")[:-1], com[2]], None, "final"),
             ("double comma (left alone, WIMS refuses)", [dot[0], num.replace(".", ",,"), com[2]], 0, "input")]
    for label, a, want_score2, want_kind in cases:
        j = call("/oef/check", {"module": L, "exo": "prob2", "seed": seed, "answers_ordered": a})[1]
        pf = {p["id"]: p["score"] for p in j.get("per_field", [])}
        kind = "final" if j.get("done") else ("input" if j.get("error") == "input" else "step")
        ok = kind == want_kind and (want_score2 is None and pf.get("reply2") == 0 or want_score2 is not None and
                                    (kind == "input" or pf.get("reply2") == want_score2))
        res["worker"].append({"case": label, "kind": kind, "grade": j.get("score"), "per_field": pf, "pass": ok})
    seed6 = int(db("select problem_seed from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id=6")[0]["problem_seed"])
    e6 = expected(R, "approx", seed6)["reply1"]
    j = call("/oef/check", {"module": R, "exo": "approx", "seed": seed6, "answers_ordered": [e6]})[1]
    res["worker"].append({"case": "type=set keeps its comma separator (approx, expected has a comma)", "kind": "final" if j.get("done") else j.get("error"),
                          "grade": j.get("score"), "comma_in_expected": "," in e6, "pass": j.get("score") == 1})
    w = WW("p4pilot", "stud3", "ww_student_password")
    p = w.view("P4pilot", 4)
    f, names = WW.form_of(p)
    out = w.submit("P4pilot", 4, dict(zip(names, com)), page=p)
    st = db("select status from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id=4")[0]["status"]
    res["webwork"].append({"case": "stud3 problem 4, all answers with decimal commas", "status": float(st), "pass": float(st) == 1.0})
    for x in res["wims_alone"] + res["worker"] + res["webwork"]:
        print(x)
    res["pass"] = all(x["pass"] for x in res["worker"] + res["webwork"])
    (RESULTS / "P6_comma.json").write_text(json.dumps(res, indent=1, ensure_ascii=False))
    print("P6", "PASS" if res["pass"] else "FAIL")


if __name__ == "__main__":
    main()
