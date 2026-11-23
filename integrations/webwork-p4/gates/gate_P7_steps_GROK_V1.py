#!/usr/bin/env python3
"""GROK_V1 P4 gate P7 (\\steps), from the P4 run (run gate_P4 first): ratapprox (2 steps). Right answers: step 1
alone -> 'Etape 2/2' note, the step-2 blank appears, attempt recorded at 0; then step 1 + step 2 -> 100 %.
Wrong step 1 -> 0 %. Plus a worker-level trace of each step. Writes p4_results/P7_steps.json."""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from oracle_GROK_V1 import expected, R, db
from oefc_GROK_V1 import call


def main():
    d = json.loads((RESULTS / "P4_grade.json").read_text())
    items = [i for i in d["items"] if i["exo"] == "ratapprox"]
    right = next(i for i in items if i["mode"] == "right"); wrong = next(i for i in items if i["mode"] == "wrong")
    seed = right["seed"]; exp = expected(R, "ratapprox", seed)
    trace = []
    for label, ans in (("step 1 only", [exp["reply1"]]), ("step 1 + step 2", [exp["reply1"], exp["reply2"]]),
                       ("step 1 wrong", ["987654"]), ("step 1 right, step 2 wrong", [exp["reply1"], "987654"])):
        j = call("/oef/check", {"module": R, "exo": "ratapprox", "seed": seed, "answers_ordered": ans})[1]
        trace.append({"case": label, "done": j["done"], "step": j["step"], "steps": j["steps"], "score": j["score"],
                      "fields_in_view": [f["id"] for f in j["view"]["fields"]]})
    res = {"webwork": {"right": {k: right.get(k) for k in ("step1_text", "step1_blanks_after", "status", "num_correct", "num_incorrect", "past_answers")},
                       "wrong": {k: wrong.get(k) for k in ("status", "num_correct", "num_incorrect")}},
           "worker_trace": trace}
    res["pass"] = (right["status"] == 1.0 and bool(right.get("step1_text")) and len(right.get("step1_blanks_after") or []) == 2
                   and wrong["status"] == 0.0 and not trace[0]["done"] and trace[1]["done"] and trace[1]["score"] == 1
                   and trace[2]["done"] and trace[2]["score"] < 1 and trace[3]["done"] and trace[3]["score"] < 1)
    (RESULTS / "P7_steps.json").write_text(json.dumps(res, indent=1, ensure_ascii=False))
    print(json.dumps(res, indent=1, ensure_ascii=False)); print("P7", "PASS" if res["pass"] else "FAIL")


if __name__ == "__main__":
    main()
