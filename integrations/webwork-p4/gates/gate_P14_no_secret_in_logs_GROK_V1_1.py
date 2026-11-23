#!/usr/bin/env python3
"""GROK_V1_1 P4 gate P14 (D4: no secret in our logs, after a full run). Every secret value of the dev instance is read
from the secrets folder (P4_SECRETS) and searched, as bytes, in every log of OUR instance:
  - every *.log file and every file of a logs/ folder under P4_ROOT (dev-cgi WIMS log, worker, Morbo, launcher,
    WeBWorK course and transaction logs, gate logs), every file of p4_results/;
  - every file of the WIMS runtime log/ folder except log/p4_auth/ (the WIMS copy of the P4 token, by design) and
    log/classes/ (connection config: WIMS keeps ident_password there, by design; not a log).
  - P14_EXTRA_DIRS (default /tmp/p10b_GROK_V1, gate P10b's worker run dir).
WIMS session folders (sessions/) are reported separately: WIMS saves adm/raw request variables there (P11 deletes its
own sessions afterwards); they must hold 0 secrets too.
Positive control: the search finds the token in log/p4_auth/p4_secret.proc. Only file counts are printed, never a value. Folders of other agents (LW_WEBWORK) are NOT read.
Writes p4_results/P14_no_secret_in_logs.json."""
import json, os, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa

NAMES = ["wims_p4grok_password", "wims_p4_token", "oef_token", "mojolicious_secret", "mariadb_webworkWrite_password",
         "ww_admin_password", "ww_prof_password", "ww_student_password"]


def secrets():
    out = {}
    for n in NAMES:
        p = SECRETS / n
        if p.is_file():
            v = p.read_text().strip()
            if len(v) >= 8:
                out[n] = v.encode()
    return out


def log_files():
    seen = set()
    skip = {SECRETS.resolve(), (WIMS_RUNTIME / "sessions").resolve(), (WIMS_RUNTIME / "log/p4_auth").resolve(),
            (WIMS_RUNTIME / "log/classes").resolve(), (P4 / "archive_stage").resolve(),
            (P4 / "archive_stage_V1_1").resolve()}
    for d, dirs, fs in os.walk(P4):
        dp = Path(d).resolve()
        dirs[:] = [x for x in dirs if (dp / x).resolve() not in skip and not (dp / x).is_symlink()]
        in_logs = dp.name == "logs" or dp == RESULTS.resolve() or str(dp).startswith(str((WIMS_RUNTIME / "log").resolve()))
        for f in fs:
            p = dp / f
            if (in_logs or f.endswith(".log")) and p.is_file() and not p.is_symlink():
                seen.add(p)
    for extra in os.environ.get("P14_EXTRA_DIRS", "/tmp/p10b_GROK_V1").split(":"):  # P10b's own worker run dir
        if extra and Path(extra).is_dir():
            seen.update(p for p in Path(extra).rglob("*") if p.is_file() and not p.is_symlink())
    return sorted(seen)


def count(files, sec):
    res = {n: 0 for n in sec}
    for p in files:
        try:
            b = p.read_bytes()
        except OSError:
            continue
        for n, v in sec.items():
            if v in b:
                res[n] += 1
    return res


def main():
    sec = secrets()
    files = log_files()
    logs = count(files, sec)
    sess_files = [Path(d, f) for d, _, fs in os.walk(WIMS_RUNTIME / "sessions") for f in fs]
    sess = count(sess_files, sec)
    # positive control: the same search finds the token in WIMS's own copy of it (excluded above by design)
    ctrl = count([WIMS_RUNTIME / "log/p4_auth/p4_secret.proc"], {"wims_p4_token": sec.get("wims_p4_token", b"\0" * 64)})
    ok = ctrl["wims_p4_token"] == 1 and len(sec) >= 6 and all(v == 0 for v in logs.values()) and all(v == 0 for v in sess.values())
    out = {"secrets_checked": sorted(sec), "log_files_scanned": len(files),
           "log_files_scanned_bytes": sum(p.stat().st_size for p in files if p.exists()),
           "files_containing_secret_logs": logs, "session_files_scanned": len(sess_files),
           "files_containing_secret_sessions": sess,
           "positive_control_token_found_in_log/p4_auth/p4_secret.proc": ctrl["wims_p4_token"], "pass": ok}
    (RESULTS / "P14_no_secret_in_logs.json").write_text(json.dumps(out, indent=1))
    print(json.dumps(out))
    print("P14", "PASS" if ok else "FAIL")


if __name__ == "__main__":
    main()
