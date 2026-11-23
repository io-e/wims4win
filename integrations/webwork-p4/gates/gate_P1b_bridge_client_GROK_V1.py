#!/usr/bin/env python3
"""GROK_V1 P4 gate P1b: the OEFBridge HTTP client (LW_WEBWORK request: HTTP::Tiny first, homemade client only if
HTTP::Tiny fails inside Safe, and then with tests). Against a fake worker (gates/p1b/fake_worker_GROK_V1.py) on
127.0.0.1:3031, with a test token (not a secret) and a test config: both clients x every case, outside Safe, plus
the decisive case inside a Safe compartment. Writes p4_results/P1b_http_client.json."""
import json, os, socket, subprocess, sys, tempfile, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
P4 = Path(__file__).resolve().parents[1]
H = Path(__file__).parent / "p1b"
PORT = 3031
tmp = Path(tempfile.mkdtemp(prefix="p1b_GROK_V1_"))
(tmp / "token").write_text("a" * 64)
reqlog = tmp / "requests.log"


def conf(client, url=f"http://127.0.0.1:{PORT}"):
    p = tmp / f"conf_{client}"
    p.write_text(f"url={url}\ntoken_file={tmp/'token'}\nconnect_timeout=2\nread_timeout=3\nhttp_client={client}\n")
    return p


def call(client, safe=False, url=None):
    env = dict(os.environ, OEF_BRIDGE_CONF=str(conf(client, url) if url else conf(client)))
    if safe:
        env["SAFE"] = "1"
    r = subprocess.run(["perl", str(H / "bridge_call_GROK_V1.pl")], env=env, capture_output=True, text=True, timeout=60)
    try:
        return json.loads(r.stdout.strip().splitlines()[-1])
    except Exception:
        return {"result": {"harness_error": r.stderr[-300:]}, "ms": -1}


CASES = [  # mode, expectation on result.error (substring) or ok
    ("ok", "OK"), ("refused", "unreachable"), ("backlog", "unreachable|timeout"), ("silent", "timeout"),
    ("big", "too large|unreachable"), ("redirect", "redirect"), ("html", "not JSON"), ("badjson", "JSON"),
    ("garbage", "no HTTP answer|not JSON|unreachable"), ("array", "JSON"),
]


def main():
    import re
    out = {"cases": [], "safe": []}
    for client in ("builtin", "tiny"):
        for mode, want in CASES:
            fillers, srv = [], None
            if mode != "refused":
                srv = subprocess.Popen([sys.executable, str(H / "fake_worker_GROK_V1.py"), mode, str(PORT), str(reqlog)])
                time.sleep(0.4)
                if mode == "backlog":
                    for _ in range(4):
                        f = socket.socket(); f.setblocking(False)
                        try: f.connect(("127.0.0.1", PORT))
                        except BlockingIOError: pass
                        fillers.append(f)
                    time.sleep(0.2)
            before = reqlog.read_text(errors="replace").count("---") if reqlog.exists() else 0
            r = call(client)
            after = reqlog.read_text(errors="replace").count("---") if reqlog.exists() else 0
            for f in fillers: f.close()
            if srv: srv.kill(); srv.wait()
            res = r["result"]
            got = "OK" if res.get("ok") else res.get("error", json.dumps(res)[:200])
            ok = (want == "OK" and got == "OK") or (want != "OK" and not res.get("ok") and re.search(want, got or "") is not None)
            item = {"client": client, "mode": mode, "ms": r["ms"], "result": got, "transport": res.get("transport"),
                    "requests_seen_by_server": after - before, "pass": bool(ok)}
            if mode == "redirect":
                item["pass"] = item["pass"] and item["requests_seen_by_server"] == 1
            out["cases"].append(item)
            print(item)
        r = call(client, url="http://10.0.0.5:3003")
        item = {"client": client, "mode": "non-loopback url in config", "result": r["result"].get("error"), "ms": r["ms"],
                "pass": "loopback" in (r["result"].get("error") or "")}
        out["cases"].append(item); print(item)
    # request shape (no keep-alive, HTTP/1.0 for builtin)
    log = reqlog.read_text(errors="replace")
    first_builtin = log.split("---")[0]
    out["builtin_request_head"] = [l for l in first_builtin.strip().splitlines() if not l.lower().startswith("x-lumaplus-token")]
    out["builtin_no_keepalive"] = "HTTP/1.0" in first_builtin and "Connection: close" in first_builtin
    # decisive case: inside Safe
    for client in ("builtin", "tiny"):
        srv = subprocess.Popen([sys.executable, str(H / "fake_worker_GROK_V1.py"), "ok", str(PORT)]); time.sleep(0.4)
        r = call(client, safe=True)
        srv.kill(); srv.wait()
        res = r["result"]
        item = {"client": client, "inside_safe": True, "result": "OK" if res.get("ok") else res, "ms": r["ms"]}
        out["safe"].append(item); print(item)
    out["tiny_works_inside_safe"] = out["safe"][1]["result"] == "OK"
    out["builtin_works_inside_safe"] = out["safe"][0]["result"] == "OK"
    out["builtin_all_cases_pass"] = all(c["pass"] for c in out["cases"] if c["client"] == "builtin") and out["builtin_no_keepalive"]
    out["chosen_client"] = "tiny" if out["tiny_works_inside_safe"] else "builtin"
    out["pass"] = out["builtin_all_cases_pass"] and out["builtin_works_inside_safe"]
    (RESULTS / "P1b_http_client.json").write_text(json.dumps(out, indent=1))
    print("builtin request head:", out["builtin_request_head"])
    print("chosen client:", out["chosen_client"], "P1b", "PASS" if out["pass"] else "FAIL")


if __name__ == "__main__":
    main()
