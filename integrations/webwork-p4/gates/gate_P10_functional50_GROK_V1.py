#!/usr/bin/env python3
"""GROK_V1 P4 gate P10, reduced by owner rule: 50 sequential render+check cycles through WeBWorK (stud3, the 10 pilot
problems in turn, right answers), one at a time, no concurrency. The 1,000-cycle load version is NOT RUN
(owner rule: no load tests). p50/p95 of view (render) and submit (check), worker RSS start/end, WIMS session count
start/end. Writes p4_results/P10_functional50.json."""
import json, statistics, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW
from oracle_GROK_V1 import PILOT, expected, db, P4, wims_sessions
from gate_P4_grade_GROK_V1 import values_for

N = 50


def rss():
    """RSS (kB) of the worker: prefork manager + its worker processes."""
    import subprocess
    pid = WORKER_PIDFILE.read_text().strip()
    pids = [pid] + subprocess.run(["pgrep", "-P", pid], capture_output=True, text=True).stdout.split()
    tot = 0
    for p in pids:
        for l in open(f"/proc/{p}/status"):
            if l.startswith("VmRSS"):
                tot += int(l.split()[1])
    return tot


def pct(xs, p):
    xs = sorted(xs); return xs[min(len(xs) - 1, int(round(p / 100 * (len(xs) - 1))))]


def main():
    seeds = {r["problem_id"]: r["problem_seed"] for r in
             db("select problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot' and user_id='stud3'")}
    vals = {}
    for i, (m, e) in enumerate(PILOT, 1):
        vals[i] = values_for(m, e, seeds[str(i)], expected(m, e, seeds[str(i)]))
    time.sleep(1)
    r0, s0 = rss(), wims_sessions()
    w = WW("p4pilot", "stud3", "ww_student_password")
    view_ms, sub_ms, scores = [], [], []
    for c in range(N):
        i = c % 10 + 1
        t0 = time.time(); p = w.view("P4pilot", i); t1 = time.time()
        f, names = WW.form_of(p)
        if PILOT[i - 1][1] == "ratapprox" and len(names) < 2:
            p = w.submit("P4pilot", i, {names[0]: vals[i][0]}, page=p); f, names = WW.form_of(p)
        t2 = time.time(); out = w.submit("P4pilot", i, dict(zip(names, vals[i])), page=p); t3 = time.time()
        view_ms.append(1000 * (t1 - t0)); sub_ms.append(1000 * (t3 - t2))
        scores.append("100%" in out.split("Résultat pour cette tentative")[1][:40] if "Résultat pour cette tentative" in out else False)
    r1, s1 = rss(), wims_sessions()
    res = {"cycles": N, "sequential": True,
           "view_ms": {"p50": round(statistics.median(view_ms)), "p95": round(pct(view_ms, 95)), "max": round(max(view_ms))},
           "submit_ms": {"p50": round(statistics.median(sub_ms)), "p95": round(pct(sub_ms, 95)), "max": round(max(sub_ms))},
           "cycle_ms": {"p50": round(statistics.median([a + b for a, b in zip(view_ms, sub_ms)])),
                        "p95": round(pct([a + b for a, b in zip(view_ms, sub_ms)], 95))},
           "right_answers_scored_100": f"{sum(scores)}/{N}",
           "worker_rss_kb": [r0, r1], "wims_sessions": [s0, s1],
           "load_test_1000": "NOT RUN (owner rule: no load tests)"}
    res["pass"] = sum(scores) == N
    (RESULTS / "P10_functional50.json").write_text(json.dumps(res, indent=1))
    print(json.dumps(res, indent=1))
    print("P10 (50 sequential cycles)", "PASS" if res["pass"] else "FAIL", "| 1,000-cycle load test NOT RUN (owner rule: no load tests)")


if __name__ == "__main__":
    main()
