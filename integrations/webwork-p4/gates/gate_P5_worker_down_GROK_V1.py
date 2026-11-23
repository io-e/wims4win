#!/usr/bin/env python3
"""GROK_V1 P4 gate P5 (worker unavailable). V1_1: message "Service de correction indisponible / grading service unavailable". (a) worker stopped (connection refused), (b) worker hung (a silent
listener on 127.0.0.1:3003 that accepts and never answers; bridge read_timeout applies). A stud3 submission must give a
clear message, score 0, a normal page (no PG exception / no 'Warning' block). The worker is restarted at the end.
Writes p4_results/P5_worker_down.json."""
import json, os, re, signal, socket, subprocess, sys, threading, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW, text_of
from oracle_GROK_V1 import db, P4

START = ["bash", "-c", WORKER_START]


def worker_pid():
    try:
        return int(WORKER_PIDFILE.read_text().strip())
    except Exception:
        return None


def stop_worker():
    """Stop the worker through its supervisor (killing only the child would let the supervisor restart it)."""
    subprocess.run(["bash", "-c", WORKER_STOP], capture_output=True)
    for _ in range(50):
        if subprocess.run(["ss", "-ltn", f"sport = :{OEF_PORT}"], capture_output=True, text=True).stdout.count("LISTEN") == 0:
            return True
        time.sleep(0.1)
    return False


def submit(label, before_submit=None):
    """View the problem (worker in its current state), then optionally break the worker, then submit."""
    w = WW("p4pilot", "stud3", "ww_student_password")
    p = w.view("P4pilot", 1)
    t_view = text_of(p)
    form, names = WW.form_of(p)
    if before_submit:
        before_submit()
    t0 = time.time()
    out = w.submit("P4pilot", 1, {names[0]: "5"} if names else {}, page=p)
    ms = int(1000 * (time.time() - t0))
    t = text_of(out)
    row = db("select status,num_incorrect from p4pilot_problem_user where user_id='stud3' and set_id='P4pilot' and problem_id=1")[0]
    r = {"case": label, "http": w.status, "ms": ms, "blanks_in_view": len(names),
         "view_message": re.findall(r"((?:Erreur|Service de correction indisponible)[^.]{0,80})", t_view)[:2],
         "submit_message": re.findall(r"((?:Erreur|Service de correction indisponible)[^.]{0,80})", t)[:3],
         "typed_kept": 'value="5"' in out,
         "pg_warning_block": "There may be something wrong with this question" in t,
         "error_flag_seen": "OEF_WORKER" in t,  # PG reports a set error_flag as an 'Evaluation error' warning
         "exception": bool(re.search(r"(Internal Server Error|Exception|died at|Can't locate)", t)),
         "result_line": (re.findall(r"(Résultat pour cette tentative[^.]{0,30})", t) or [None])[0],
         "status_db": row["status"]}
    r["pass"] = (w.status == 200 and bool(r["submit_message"]) and (r["typed_kept"] or not names) and not r["exception"] and r["error_flag_seen"]
                 and "0%" in (r["result_line"] or ""))
    print(r)
    return r


def main():
    res = []
    try:
        res.append(submit("view while up, worker stopped before submit (connection refused)", stop_worker))
        res.append(submit("view and submit while worker stopped"))
        srv = socket.socket(); srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        conns = []
        stop = threading.Event()
        subprocess.run(START, check=False); time.sleep(2)
        def hang():
            stop_worker()
            srv.bind(("127.0.0.1", OEF_PORT)); srv.listen(16)
            th.start()
        def acc():
            srv.settimeout(0.5)
            while not stop.is_set():
                try:
                    conns.append(srv.accept()[0])
                except OSError:
                    pass
        th = threading.Thread(target=acc, daemon=True)
        try:
            res.append(submit("view while up, worker hung before submit (accepts, never answers)", hang))
        finally:
            stop.set()
            if th.is_alive():
                th.join()
            [c.close() for c in conns]; srv.close()
    finally:
        subprocess.run(START, check=False)
        time.sleep(2)
    up = subprocess.run(["ss", "-ltn", f"sport = :{OEF_PORT}"], capture_output=True, text=True).stdout.count("LISTEN") == 1
    res.append(submit("worker restarted (normal again, wrong answer 5 -> 0 %, no error message)"))
    res[-1]["pass"] = (up and not res[-1]["submit_message"] and not res[-1]["exception"]
                       and not res[-1]["error_flag_seen"] and not res[-1]["pg_warning_block"])
    print("restart case pass:", res[-1]["pass"])
    ok = all(r["pass"] for r in res)
    (RESULTS / "P5_worker_down.json").write_text(json.dumps({"cases": res, "pass": ok}, indent=1, ensure_ascii=False))
    print("P5", "PASS" if ok else "FAIL")


if __name__ == "__main__":
    main()
