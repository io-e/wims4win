#!/usr/bin/env python3
"""GROK_V1_1 P4 gate P5b (worker stopped => no score above 0 is ever recorded, the message is shown, attempts stay
unlimited). stud2 submits the RIGHT answers (WIMS oracle, independent of the worker) to every pilot problem while the
worker is stopped (connection refused). Checks, per problem: page shows "Service de correction indisponible /
grading service unavailable"; p4pilot_past_answer rows written during the outage all have score 0 (no character
other than 0 in the score string); problem_user.status not raised; max_attempts of every P4pilot problem is -1
(unlimited, global set and stud2's user rows). Control after restart: the same right answers give 100 %.
Also reports (LW review) the attempt bookkeeping: a SECOND outage submission on problem 1 (num_incorrect +1 per
attempt, one past_answer row per attempt) and what the teacher sees: the instructor "past answers" page for stud2
problem 1 (rows listed, each scored 0) and problem_user after the control (num_correct/num_incorrect/status).
No webwork2 hook. Writes p4_results/P5b_no_score_when_down.json. Resets set P4pilot for stud2 before it runs (own instance)."""
import json, re, subprocess, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from ww_client_GROK_V1 import WW, text_of
from oracle_GROK_V1 import PILOT, expected, db
from gate_P4_grade_GROK_V1 import values_for, score_line

MSG = "Service de correction indisponible / grading service unavailable"


def listening():
    return subprocess.run(["ss", "-ltn", f"sport = :{OEF_PORT}"], capture_output=True, text=True).stdout.count("LISTEN") > 0


def main():
    db("update p4pilot_problem_user set status=0,attempted=0,num_correct=0,num_incorrect=0,last_answer='',sub_status=0 "
       "where set_id='P4pilot' and user_id='stud2'")
    db("delete from p4pilot_past_answer where set_id='P4pilot' and user_id='stud2'")
    seeds = {r["problem_id"]: r["problem_seed"] for r in
             db("select problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot' and user_id='stud2'")}
    plan = []
    for i, (m, e) in enumerate(PILOT, 1):   # oracle + blank values while the worker is up
        exp = expected(m, e, seeds[str(i)])
        plan.append((i, m, e, values_for(m, e, seeds[str(i)], exp)))
    gl = db("select problem_id,max_attempts from p4pilot_problem where set_id='P4pilot'")
    us = db("select problem_id,max_attempts from p4pilot_problem_user where set_id='P4pilot' and user_id='stud2'")
    unlimited = all(r["max_attempts"] == "-1" for r in gl) and all(r["max_attempts"] in ("-1", "NULL") for r in us)
    w = WW("p4pilot", "stud2", "ww_student_password")
    pages = {i: w.view("P4pilot", i) for i, m, e, vals in plan}   # viewed while the worker is up (blanks present)
    subprocess.run(["bash", "-c", WORKER_STOP], capture_output=True)
    for _ in range(50):
        if not listening():
            break
        time.sleep(0.1)
    down = not listening()
    items = []
    try:
        for i, m, e, vals in plan:
            page = pages[i]
            form, names = WW.form_of(page)
            out = w.submit("P4pilot", i, dict(zip(names, vals)), page=page)
            t = text_of(out)
            row = db(f"select status,num_correct,num_incorrect from p4pilot_problem_user where user_id='stud2' and set_id='P4pilot' and problem_id={i}")[0]
            pas = db(f"select scores from p4pilot_past_answer where user_id='stud2' and set_id='P4pilot' and problem_id={i}")
            items.append({"n": i, "exo": e, "blanks": len(names), "message_submit": MSG in t,
                          "banner": score_line(out), "status": float(row["status"]), "num_correct": int(row["num_correct"]),
                          "num_incorrect": int(row["num_incorrect"]), "past_answer_scores": [p["scores"] for p in pas]})
            it = items[-1]
            it["pass"] = (it["message_submit"] and it["status"] == 0 and it["num_correct"] == 0
                          and all(set(s) <= {"0"} for s in it["past_answer_scores"]) and bool(it["past_answer_scores"]))
            print(it)
        # attempt bookkeeping: a second outage submission on problem 1
        i, m, e, vals = plan[0]
        page = w.view("P4pilot", i)
        form, names = WW.form_of(page)
        out2 = w.submit("P4pilot", i, dict(zip(names, vals)), page=page) if names else ""
        r2 = db(f"select status,num_correct,num_incorrect from p4pilot_problem_user where user_id='stud2' and set_id='P4pilot' and problem_id={i}")[0]
        pa2 = db(f"select scores from p4pilot_past_answer where user_id='stud2' and set_id='P4pilot' and problem_id={i}")
        second = {"n": i, "blanks_on_view_while_down": len(names), "message": MSG in text_of(out2) if out2 else None,
                  "num_incorrect_after_1st": items[0]["num_incorrect"], "num_incorrect_after_2nd": int(r2["num_incorrect"]),
                  "past_answer_rows_after_2nd": len(pa2), "scores": [p["scores"] for p in pa2], "status": float(r2["status"])}
        print("second outage attempt:", second)
        # a page viewed while the worker is down shows the message too
        view_down = MSG in text_of(w.view("P4pilot", 1))
    finally:
        subprocess.run(["bash", "-c", WORKER_START], capture_output=True); time.sleep(2)
    # control: worker back, right answers to problem 1 and 4 give 100 %
    ctrl = []
    for i, m, e, vals in plan:
        if i not in (1, 4):
            continue
        page = w.view("P4pilot", i)
        form, names = WW.form_of(page)
        out = w.submit("P4pilot", i, dict(zip(names, vals)), page=page)
        row = db(f"select status from p4pilot_problem_user where user_id='stud2' and set_id='P4pilot' and problem_id={i}")[0]
        ctrl.append({"n": i, "exo": e, "banner": score_line(out), "status": float(row["status"]), "message": MSG in text_of(out)})
    ctrl_ok = all(c["status"] == 1.0 and not c["message"] for c in ctrl)
    # what the teacher sees (instructor pages, p4prof)
    prof = WW("p4pilot", "p4prof", "ww_prof_password")
    pa_html = prof.get(f"{WW_BASE}/p4pilot/show_answers/", {"selected_users": "stud2", "selected_sets": "P4pilot",
                       "selected_problems": "1", "action": "Afficher"})
    pa_rows = re.findall(r'<td class="px-3 (correct|incorrect)"', pa_html)  # one row per attempt, colour = WeBWorK's judgement
    r_end = db("select status,num_correct,num_incorrect,attempted from p4pilot_problem_user where user_id='stud2' and set_id='P4pilot' and problem_id=1")[0]
    teacher = {"past_answers_page_http": prof.status, "past_answers_page_rows_problem1": pa_rows,
               "past_answer_rows_problem1": len(db("select answer_id from p4pilot_past_answer where user_id='stud2' and set_id='P4pilot' and problem_id=1")),
               "problem_user_problem1_after_control": r_end,
               "reading": "past answers page: one row per attempt with the typed answer; outage attempts are marked "
                          "incorrect (red) even when the typed answer is right, with no outage note; num_incorrect counts "
                          "them; the final status is the best score (1.0 after the worker came back)"}
    teacher_ok = prof.status == 200 and pa_rows == ["incorrect", "incorrect", "correct"]
    print("teacher view:", teacher)
    second_ok = (second["num_incorrect_after_2nd"] == second["num_incorrect_after_1st"] + 1
                 and second["past_answer_rows_after_2nd"] == 2 and all(set(x) <= {"0"} for x in second["scores"])
                 and second["status"] == 0)
    ok = down and unlimited and view_down and all(x["pass"] for x in items) and ctrl_ok and second_ok and teacher_ok
    out = {"worker_down_during_test": down, "message_on_view_while_down": view_down, "unlimited_attempts": unlimited, "max_attempts_global": gl,
           "items": items, "second_outage_attempt": second, "second_ok": second_ok, "teacher_view": teacher,
           "control_after_restart": ctrl, "control_ok": ctrl_ok, "pass": ok,
           "note": "WeBWorK still counts each outage submission as an attempt (num_incorrect +1, score 0): unlimited "
                   "attempts make it harmless; WeBWorK cannot cancel an attempt without a webwork2 patch."}
    (RESULTS / "P5b_no_score_when_down.json").write_text(json.dumps(out, indent=1, ensure_ascii=False))
    print("unlimited:", unlimited, "down:", down, "control:", ctrl)
    print("P5b", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
