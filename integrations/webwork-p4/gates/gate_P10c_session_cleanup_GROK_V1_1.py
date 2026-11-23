#!/usr/bin/env python3
"""GROK_V1_1 P4 gate P10c (WIMS session cleanup on the dev WIMS). Usage:
  gate_P10c_session_cleanup_GROK_V1_1.py --baseline   (before P10: records the session count)
  gate_P10c_session_cleanup_GROK_V1_1.py              (after P10's 50 cycles: waits for wimslogd's cleaning)
Cleanup is WIMS's own: the wimslogd daemon (must run for this WIMS) cleans sessions at most every 300 s; idle
lightpopup/popup sessions (the worker's and the oracle's) go after 50 s idle, other sessions after idle_time*
(wims.conf). PASS when the count is back to <= the baseline within P10C_WAIT seconds (default 480) and wimslogd runs.
No load: polls the sessions folder every 10 s, no HTTP request. Writes p4_results/P10c_session_cleanup.json."""
import json, os, subprocess, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from oracle_GROK_V1 import wims_sessions

BASE = RESULTS / "P10c_baseline.json"


def wimslogd_running():
    """a wimslogd process whose working directory is this WIMS runtime."""
    for pid in subprocess.run(["pgrep", "-x", "wimslogd"], capture_output=True, text=True).stdout.split():
        try:
            if Path(os.readlink(f"/proc/{pid}/cwd")).resolve() == WIMS_RUNTIME.resolve():
                return int(pid)
        except OSError:
            pass
    return None


def main():
    if "--baseline" in sys.argv:
        b = {"t": time.time(), "sessions": wims_sessions(), "wimslogd_pid": wimslogd_running()}
        BASE.write_text(json.dumps(b)); print(json.dumps(b)); print("P10c baseline recorded (PASS)"); return
    b = json.loads(BASE.read_text())
    wait = int(os.environ.get("P10C_WAIT", "480"))
    t0 = time.time(); after = wims_sessions(); trace = [(0, after)]
    while time.time() - t0 < wait:
        n = wims_sessions()
        if trace[-1][1] != n:
            trace.append((round(time.time() - t0), n))
        if n <= b["sessions"]:
            break
        time.sleep(10)
    final = wims_sessions()
    pid = wimslogd_running()
    res = {"baseline_before_P10": b["sessions"], "right_after_P10": after, "final": final,
           "seconds_waited": round(time.time() - t0), "trace_s_count": trace, "wimslogd_pid": pid,
           "pass": bool(pid) and final <= b["sessions"]}
    (RESULTS / "P10c_session_cleanup.json").write_text(json.dumps(res, indent=1))
    print(json.dumps(res))
    print("P10c", "PASS" if res["pass"] else "FAIL")


if __name__ == "__main__":
    main()
