#!/usr/bin/env python3
"""GROK_V1 P4 gate P10b (worker recycling, functional, not a load test): a second worker on 127.0.0.1:3014 with
OEF_MAX_REQUESTS=5 under the supervisor; 23 sequential renders with NO client retry. Each worker process must be
replaced without any refused/reset connection. Writes p4_results/P10b_recycling.json."""
import json, os, subprocess, sys, time, urllib.request
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from oefc_GROK_V1 import TOKEN

RUN = Path("/tmp/p10b_GROK_V1"); PORT = 3014
START = os.environ.get("P10B_START", str(P4 / "start/start_oef_worker_GROK_V1_1.sh"))  # V1_1 supervisor


def main():
    env = dict(os.environ, OEF_WORKER=str(WORKER_SCRIPT), OEF_TOKEN_FILE=str(SECRETS / "oef_token"), OEF_WIMS_TOKEN_FILE=str(WIMS_TOKEN_FILE),
               OEF_MODULES=os.environ.get("OEF_MODULES", "H3/analysis/oeflinf.fr U2/arithmetic/oefrational.fr"),
               OEF_MODULE_ROOTS=str(WIMS_RUNTIME / "public_html/modules"), OEF_WIMS_BASE=WIMS_URL,
               OEF_LISTEN=f"127.0.0.1:{PORT}", OEF_ALLOWED_ORIGIN=OEF_ORIGIN_OK, OEF_RUN_DIR=str(RUN), OEF_MAX_REQUESTS="5")
    subprocess.run(["bash", START, "start"], env=env, check=True, capture_output=True)
    time.sleep(1.5)
    codes, pids, ms = [], [], []
    try:
        for i in range(23):
            req = urllib.request.Request(f"http://127.0.0.1:{PORT}/oef/render", method="POST",
                                         data=json.dumps({"module": "H3/analysis/oeflinf.fr", "exo": "antecedant", "seed": i}).encode(),
                                         headers={"Content-Type": "application/json", "X-LumaPlus-Token": TOKEN, "Connection": "close"})
            t0 = time.time()
            try:
                with urllib.request.urlopen(req, timeout=30) as r:
                    codes.append(r.status); r.read()
            except Exception as e:
                codes.append(f"{e.__class__.__name__}")
            ms.append(round(1000 * (time.time() - t0)))
            ping = json.loads(urllib.request.urlopen(urllib.request.Request(f"http://127.0.0.1:{PORT}/oef/ping",
                              headers={"X-LumaPlus-Token": TOKEN}), timeout=30).read())
            pids.append(ping["pid"])
    finally:
        subprocess.run(["bash", START, "stop"], env=env, capture_output=True)
    res = {"max_requests": 5, "calls": len(codes), "http_codes": codes, "ms": ms, "distinct_worker_pids": len(set(pids)),
           "no_client_retry": True}
    res["pass"] = all(c == 200 for c in codes) and len(set(pids)) >= 3
    (RESULTS / "P10b_recycling.json").write_text(json.dumps(res, indent=1))
    print(json.dumps(res)); print("P10b", "PASS" if res["pass"] else "FAIL")


if __name__ == "__main__":
    main()
