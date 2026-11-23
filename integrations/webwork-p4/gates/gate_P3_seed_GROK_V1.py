#!/usr/bin/env python3
"""GROK_V1 P4 gate P3 (seed stability). Same WeBWorK problemSeed -> same statement in two separate login sessions
(and in the worker for two calls); changing problem_user.problem_seed (own DB) changes the draw; seed restored after.
Writes p4_results/P3_seed.json."""
import json, re, sys, hashlib
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW
from gate_P2_noleak_GROK_V1 import body_text
from oracle_GROK_V1 import PILOT, db, P4
from oefc_GROK_V1 import call

h = lambda s: hashlib.sha256(re.sub(r"\s+", " ", s).encode()).hexdigest()[:16]


def stmt(page):
    if 'id="problem_body"' not in page:
        return "NO PROBLEM BODY (login page?)"
    t = body_text(page)
    i = page.find('id="problem_body"'); body = page[i:page.find('id="problemMainForm"', i) if False else i + 400000]
    imgs = [hashlib.sha256(u.encode()).hexdigest()[:8] for u in re.findall(r'src="(data:image/[^"]+)"', body)]
    t = " ".join(imgs) + " | " + t
    return t.split("You Entered")[0].split("Vous avez saisi")[0]


def main():
    items, ok = [], True
    for i, (m, e) in enumerate(PILOT, 1):
        seed = int(db(f"select problem_seed from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id={i}")[0]["problem_seed"])
        # WeBWorK keeps one session per user: log in, view, then a second fresh login and view
        a = WW("p4pilot", "stud3", "ww_student_password"); sa = stmt(a.view("P4pilot", i))
        b = WW("p4pilot", "stud3", "ww_student_password"); sb = stmt(b.view("P4pilot", i))
        a = b
        w1 = call("/oef/render", {"module": m, "exo": e, "seed": seed})[1]["statement_html"]
        w2 = call("/oef/render", {"module": m, "exo": e, "seed": seed})[1]["statement_html"]
        new = seed + 7919
        db(f"update p4pilot_problem_user set problem_seed={new} where user_id='stud3' and set_id='P4pilot' and problem_id={i}")
        try:
            sc = stmt(a.view("P4pilot", i))
        finally:
            db(f"update p4pilot_problem_user set problem_seed={seed} where user_id='stud3' and set_id='P4pilot' and problem_id={i}")
        sd = stmt(a.view("P4pilot", i))
        it = {"n": i, "exo": e, "seed": seed, "session_a": h(sa), "session_b": h(sb), "worker_1": h(w1), "worker_2": h(w2),
              "reseeded": new, "after_reseed": h(sc), "restored": h(sd)}
        it["same_seed_same"] = sa == sb and w1 == w2
        it["reseed_changes"] = sc != sa
        it["restore_same"] = sd == sa
        it["valid_pages"] = all('id="problem_body"' in x for x in (sa, sb, sc, sd))
        it["pass"] = it["valid_pages"] and it["same_seed_same"] and it["reseed_changes"] and it["restore_same"]
        ok &= it["pass"]; items.append(it)
        print(it)
    (RESULTS / "P3_seed.json").write_text(json.dumps({"items": items, "pass": ok}, indent=1))
    print("P3", "PASS" if ok else "FAIL")


if __name__ == "__main__":
    main()
