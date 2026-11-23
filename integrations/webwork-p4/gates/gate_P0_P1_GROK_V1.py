#!/usr/bin/env python3
"""GROK_V1 P4 gates P0 (ports/bindings) and P1 (worker security). V1_1: F1 cases (OEF modules only, def/<exo>.def
required; V1_1 refined: exact site allowlist OEF_MODULES, empty names, traversal, absolute path), listen-refusal test on 127.0.0.1:3015, P0 wording from the environment (I6). Writes p4_results/P0_ports.txt, P1_security.json."""
import json, os, subprocess, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from oefc_GROK_V1 import call, TOKEN, P4

MINE = {WW_PORT: "WeBWorK", str(OEF_PORT): "OEF worker", DB_PORT: "MariaDB", str(WIMS_PORT): "WIMS"}


def p0():
    out = subprocess.run(["ss", "-ltnp"], capture_output=True, text=True).stdout
    lines = [l for l in out.splitlines() if any(f":{p} " in l for p in list(MINE) + ["3000", "3307"])]
    rows, ok = [], True
    for p, what in MINE.items():
        ls = [l for l in lines if f":{p} " in l]
        addrs = sorted({l.split()[3] for l in ls})
        good = bool(addrs) and all(a.startswith("127.0.0.1:") for a in addrs)
        ok &= good
        rows.append(f"{p:5} {what:28} {', '.join(addrs) or 'NOT LISTENING'}  {'OK' if good else 'FAIL'}")
    lw = [l for l in lines if ":3000 " in l or ":3307 " in l]
    txt = ("# P0 ports and bindings (ss -ltnp), " + time.strftime("%Y-%m-%d %H:%M:%S %Z") + "\n"
           + f"# Instance under test: WeBWorK {WW_PORT}, MariaDB {DB_PORT}, OEF worker {OEF_PORT}, WIMS {WIMS_PORT}"
           + (" (builder's dev instance; 3000/3307 belong to LW_WEBWORK and are only observed)" if WW_PORT != "3000" else "") + "\n"
           + "\n".join(rows) + ("\n\n# 3000/3307 (other instance, only observed):\n" + "\n".join(lw) if WW_PORT != "3000" else "") + "\n\n# raw ss lines\n"
           + "\n".join(lines) + f"\n\nP0 {'PASS' if ok else 'FAIL'}\n")
    (RESULTS / "P0_ports.txt").write_text(txt)
    print(txt)
    return ok


def wims_hits():
    """Lines in the WIMS HTTP server log (if any) + WIMS's own access/session logs: any WIMS call adds a line."""
    n = 0
    for f in (WIMS_ACCESS_LOG, WIMS_RUNTIME / "log/access.log", WIMS_RUNTIME / "log/session.log"):
        if Path(f).is_file():
            n += sum(1 for _ in open(f, errors="replace"))
    return n


NOT_LISTED = WIMS_RUNTIME / "public_html/modules/H2/algebra/oefcalcul.fr/def"
NOT_LISTED_EXO = sorted(p.stem for p in NOT_LISTED.glob("*.def"))[0] if NOT_LISTED.is_dir() else "x"


def p1():
    big = json.dumps({"module": "H3/analysis/oeflinf.fr", "exo": "antecedant", "seed": 1, "pad": "x" * 70000}).encode()
    ok_body = {"module": "H3/analysis/oeflinf.fr", "exo": "antecedant", "seed": 1}
    cases = [
        ("no token", dict(path="/oef/render", body=ok_body, token=None), 403),
        ("bad token", dict(path="/oef/render", body=ok_body, token="0" * 64), 403),
        ("bad token same length prefix", dict(path="/oef/render", body=ok_body, token=TOKEN[:-1] + ("0" if TOKEN[-1] != "0" else "1")), 403),
        ("bad Host", dict(path="/oef/render", body=ok_body, headers={"Host": f"evil.example:{OEF_PORT}"}), 403),
        ("bad Host localhost alias", dict(path="/oef/render", body=ok_body, headers={"Host": f"localhost:{OEF_PORT}"}), 403),
        ("bad Origin", dict(path="/oef/render", body=ok_body, headers={"Origin": "http://evil.example"}), 403),
        ("non-JSON content type", dict(path="/oef/render", raw=b"module=x", ctype="application/x-www-form-urlencoded"), 415),
        ("body over 64 KiB", dict(path="/oef/render", raw=big), 413),
        ("JSON not an object", dict(path="/oef/render", raw=b"[1,2]"), 400),
        ("module with ..", dict(path="/oef/render", body={**ok_body, "module": "H3/../../../etc"}), 400),
        ("absolute module", dict(path="/oef/render", body={**ok_body, "module": "/etc/passwd"}), 400),
        ("module outside roots", dict(path="/oef/render", body={**ok_body, "module": "H9/none/nothing.fr"}), 400),
        # F1 (V1_1): only OEF exercise modules, and only exercises that have def/<exo>.def
        ("F1 module adm/raw", dict(path="/oef/render", body={**ok_body, "module": "adm/raw"}), 400),
        ("F1 module adm/raw (check)", dict(path="/oef/check", body={**ok_body, "module": "adm/raw", "answers": {}}), 400),
        ("F1 module adm/class", dict(path="/oef/render", body={**ok_body, "module": "adm/class"}), 400),
        ("F1 module home", dict(path="/oef/render", body={**ok_body, "module": "home"}), 400),
        ("F1 module classes/fr", dict(path="/oef/render", body={**ok_body, "module": "classes/fr"}), 400),
        ("F1 module tool/wcalc.fr", dict(path="/oef/render", body={**ok_body, "module": "tool/wcalc.fr"}), 400),
        ("F1 module moduletest", dict(path="/oef/render", body={**ok_body, "module": "moduletest"}), 400),
        ("F1 area dir H3/analysis (no def/)", dict(path="/oef/render", body={**ok_body, "module": "H3/analysis"}), 400),
        ("F1 exo without def/<exo>.def", dict(path="/oef/render", body={**ok_body, "exo": "nosuchexo"}), 400),
        ("F1 exo name of a module file (INDEX)", dict(path="/oef/render", body={**ok_body, "exo": "INDEX"}), 400),
        # F1 refined (LW review): exact site allowlist OEF_MODULES; empty names; traversal through an allowed module
        ("F1 OEF module present on disk but not in the site allowlist (H2/algebra/oefcalcul.fr)",
         dict(path="/oef/render", body={**ok_body, "module": "H2/algebra/oefcalcul.fr", "exo": NOT_LISTED_EXO}), 400),
        ("F1 empty module name", dict(path="/oef/render", body={**ok_body, "module": ""}), 400),
        ("F1 empty exo name", dict(path="/oef/render", body={**ok_body, "exo": ""}), 400),
        ("F1 traversal from an allowed module", dict(path="/oef/render",
         body={**ok_body, "module": "U2/arithmetic/oefrational.fr/../../../adm/raw"}), 400),
        ("F1 allowed module + trailing slash", dict(path="/oef/render", body={**ok_body, "module": "H3/analysis/oeflinf.fr/"}), 400),
        ("F1 absolute path to an allowed module", dict(path="/oef/render",
         body={**ok_body, "module": str(WIMS_RUNTIME / "public_html/modules/H3/analysis/oeflinf.fr")}), 400),
        ("F1 exo without def (check)", dict(path="/oef/check", body={**ok_body, "exo": "nosuchexo", "answers": {}}), 400),
        ("bad exo name (dash)", dict(path="/oef/render", body={**ok_body, "exo": "img-ant"}), 400),
        ("bad exo name (shell)", dict(path="/oef/render", body={**ok_body, "exo": "x;cat /etc/passwd"}), 400),
        ("bad seed", dict(path="/oef/check", body={**ok_body, "seed": "1;x", "answers": {}}), 400),
        ("bad answer key", dict(path="/oef/check", body={**ok_body, "answers": {"session": "X"}}), 400),
        ("answer too long", dict(path="/oef/check", body={**ok_body, "answers": {"reply1": "9" * 600}}), 400),
        ("unknown path", dict(path="/wims/wims.cgi", body=ok_body), 404),
    ]
    before_ping = call("/oef/ping")[1]["wims_calls"]
    before_log = wims_hits()
    res = []
    for name, kw, want in cases:
        c, j, txt, hdrs = call(**kw)
        cors = [h for h in hdrs if h.lower().startswith("access-control-")]
        res.append({"case": name, "http": c, "want": want, "error": (j or {}).get("error"), "cors_headers": cors,
                    "pass": c == want and not cors})
        print(f"{'PASS' if res[-1]['pass'] else 'FAIL'} {name}: {c} (want {want}) {(j or {}).get('error')}")
    after_ping = call("/oef/ping")[1]["wims_calls"]
    after_log = wims_hits()
    # refuses a non-loopback listen address
    env = dict(os.environ, OEF_TOKEN_FILE=str(SECRETS / "oef_token"), OEF_WIMS_TOKEN_FILE=str(WIMS_TOKEN_FILE), OEF_LISTEN="0.0.0.0:3015",
               OEF_MODULE_ROOTS=str(WIMS_RUNTIME / "public_html/modules"),
               OEF_MODULES="H3/analysis/oeflinf.fr U2/arithmetic/oefrational.fr")
    r = subprocess.run(["perl", str(WORKER_SCRIPT)], env=env, capture_output=True, text=True, timeout=20)
    nl = {"case": "start with OEF_LISTEN=0.0.0.0:3015", "exit": r.returncode, "stderr": r.stderr.strip()[:200],
          "pass": r.returncode != 0 and "loopback" in r.stderr}
    env2 = dict(env, OEF_LISTEN="127.0.0.1:3015", OEF_WIMS_BASE="http://10.0.0.5/wims/wims.cgi")
    r2 = subprocess.run(["perl", str(WORKER_SCRIPT)], env=env2, capture_output=True, text=True, timeout=20)
    nl2 = {"case": "start with non-loopback OEF_WIMS_BASE", "exit": r2.returncode, "stderr": r2.stderr.strip()[:200],
           "pass": r2.returncode != 0 and "loopback" in r2.stderr}
    starts = [nl, nl2]
    for label, mods in (("start without OEF_MODULES", None), ("start with adm/raw in OEF_MODULES", "adm/raw"),
                        ("start with ../ in OEF_MODULES", "H3/../adm/raw")):
        e3 = dict(env, OEF_LISTEN="127.0.0.1:3015")
        e3.pop("OEF_MODULES", None)
        if mods:
            e3["OEF_MODULES"] = mods
        r3 = subprocess.run(["perl", str(WORKER_SCRIPT)], env=e3, capture_output=True, text=True, timeout=20)
        starts.append({"case": label, "exit": r3.returncode, "stderr": r3.stderr.strip()[:200],
                       "pass": r3.returncode != 0 and "OEF_MODULES" in r3.stderr})
    print(*starts, sep="\n")
    out = {"cases": res, "listen_refusal": starts, "not_listed_module_exists_with_def": NOT_LISTED.is_dir(), "wims_calls_worker_counter": [before_ping, after_ping],
           "wims_dev_server_log_lines": [before_log, after_log],
           "note": "wims_calls is per worker process (prefork): informative only; the logs are the evidence",
           "no_wims_call": before_log == after_log}
    out["pass"] = (all(x["pass"] for x in res) and all(x["pass"] for x in starts) and out["no_wims_call"]
                   and NOT_LISTED.is_dir())
    (RESULTS / "P1_security.json").write_text(json.dumps(out, indent=1))
    print("no WIMS call during P1:", out["no_wims_call"], out["wims_calls_worker_counter"], out["wims_dev_server_log_lines"])
    print("P1", "PASS" if out["pass"] else "FAIL")
    return out["pass"]


if __name__ == "__main__":
    a = p0(); b = p1()
    sys.exit(0 if a and b else 1)
