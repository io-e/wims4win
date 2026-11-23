#!/usr/bin/env python3
"""GROK_V1 P4 gate P11 (WIMS hardening). myself refuses; p4grok allows only gettime/listmodules/getmodule;
ident_site enforced (temporary test connection p4grokip with ident_site=10.9.9.9, removed at the end: the V1_1 test);
WIMS and lightpopup unreachable from a non-loopback address (bind check + connection attempt on the box's own
non-loopback IPs). Passwords read from secrets, never printed.
V1_1 (D4): every call that carries ident/passwd is a multipart/form-data POST to wims.cgi?form-data, so the password
is never in a URL (the dev-cgi access log records only "POST /wims/wims.cgi?form-data"; WIMS post.log records only
the content type). WIMS itself saves the adm/raw request variables (passwd included) in the var file of the
session it opens for the call: at the end the gate deletes those session folders of this dev WIMS (only folders whose
files contain the password; count recorded, value never printed). Gate P14 checks that the password is absent from every log afterwards. Writes p4_results/P11_wims_hardening.json."""
import json, os, random, re, socket, subprocess, sys, urllib.parse, urllib.request
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
P4 = Path(__file__).resolve().parents[1]
CONN = WIMS_RUNTIME / "log/classes/.connections"
WIMS = WIMS_URL
PW = (SECRETS / "wims_p4grok_password").read_text().strip()


def get(params, agent="P4-OEF-GROK"):
    req = urllib.request.Request(WIMS + "?" + urllib.parse.urlencode(params), headers={"User-Agent": agent})
    b = urllib.request.urlopen(req, timeout=60).read()
    return b.decode("utf-8", "replace").replace(PW, "<redacted>")


def post(params, agent="P4-OEF-GROK"):
    """multipart/form-data POST (WIMS parses it only when QUERY_STRING starts with "form-data")."""
    bnd = "----p4grok%016x" % random.getrandbits(64)
    body = b"".join(f'--{bnd}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
                    for k, v in params.items()) + f"--{bnd}--\r\n".encode()
    req = urllib.request.Request(WIMS + "?form-data%d" % random.randrange(10**9), data=body, headers={
        "User-Agent": agent, "Content-Type": f"multipart/form-data; boundary={bnd}"})
    b = urllib.request.urlopen(req, timeout=60).read()
    return b.decode("utf-8", "replace").replace(PW, "<redacted>")


def raw(job, ident="p4grok", passwd=None, agent="P4-OEF-GROK", **kw):
    p = {"module": "adm/raw", "job": job, "ident": ident, "passwd": PW if passwd is None else passwd,
         "code": str(random.randint(10**5, 10**6)), **kw}
    return post(p, agent)


def _has(p, v):
    try:
        return v.encode() in p.read_bytes()
    except OSError:
        return False


def main():
    R = []
    def rec(name, ok, body):
        R.append({"test": name, "pass": bool(ok), "body": re.sub(r"\s+", " ", body)[:300]}); print("PASS" if ok else "FAIL", name, "::", R[-1]["body"][:140])
    pwd_state = {f.name: ("DISABLED(*)" if re.search(r"(?m)^ident_password=\*\s*$", f.read_text(errors="replace")) else "set")
                 for f in CONN.iterdir() if f.is_file()}
    rec("connection files: only p4grok has a password", all(v == "DISABLED(*)" for k, v in pwd_state.items() if k != "p4grok")
        and pwd_state.get("p4grok") == "set", json.dumps(pwd_state))
    for pw in ("tata", "", "*"):
        b = raw("gettime", ident="myself", passwd=pw, agent="WIMS-webget")
        rec(f"myself refused (passwd {'empty' if not pw else repr(pw) if pw == '*' else 'example default'})", '"OK"' not in b and "ERROR" in b.upper(), b)
    b = raw("gettime"); rec("p4grok gettime allowed", '"OK"' in b, b)
    b = raw("listmodules"); rec("p4grok listmodules allowed", '"OK"' in b or "ERROR" not in b[:200].upper(), b[:300])
    b = raw("getmodule", option="title", emod="H3/analysis/oeflinf.fr"); rec("p4grok getmodule allowed", '"OK"' in b, b)
    for job in ("listclasses", "addclass", "getclass", "authuser", "getscores", "delclass", "addexo", "getcsv"):  # lightpopup handled below
        b = raw(job, qclass="1234567", rclass="x")
        rec(f"p4grok {job} refused", "illegal job" in b or ("ERROR" in b.upper() and '"OK"' not in b) or job == "lightpopup" and "illegal" in b, b)
    b = raw("lightpopup", emod="H3/analysis/oeflinf.fr", option="noabout", parm="cmd=new;exo=antecedant;qnum=1;seed=1")
    rec("INFO lightpopup is a no-auth job: it answers with or without ident (ident_allow does not apply to it)",
        "<html" in b.lower(), b[:120])
    b = raw("gettime", passwd="0" * 32); rec("p4grok bad password refused", "bad login" in b or '"OK"' not in b, b)
    b = raw("gettime", agent="curl/8"); rec("p4grok bad user agent refused", '"OK"' not in b, b)
    # ident_site: temporary connection with a non-loopback ident_site
    tmp = CONN / "p4grokip"
    body = (CONN / "p4grok").read_text().replace("ident_site=127.0.0.1", "ident_site=10.9.9.9")
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600); os.write(fd, body.encode()); os.close(fd)
    try:
        b = raw("gettime", ident="p4grokip"); rec("ident_site enforced (p4grokip ident_site=10.9.9.9, call from 127.0.0.1)", "bad IP" in b or '"OK"' not in b, b)
    finally:
        tmp.unlink()
    rec("temporary p4grokip removed", not tmp.exists(), "")
    # bind and reachability from non-loopback addresses
    ss = subprocess.run(["ss", "-ltn", f"sport = :{WIMS_PORT}"], capture_output=True, text=True).stdout
    rec("WIMS bound to 127.0.0.1 only", f"127.0.0.1:{WIMS_PORT}" in ss and f"0.0.0.0:{WIMS_PORT}" not in ss and f"*:{WIMS_PORT}" not in ss, ss)
    ips = re.findall(r"inet (\d+\.\d+\.\d+\.\d+)/", subprocess.run(["ip", "-4", "addr"], capture_output=True, text=True).stdout)
    for ip in [i for i in ips if not i.startswith("127.")]:
        for port in (WIMS_PORT, OEF_PORT):
            s = socket.socket(); s.settimeout(2)
            try:
                s.connect((ip, port)); got = "CONNECTED"
            except OSError as e:
                got = f"refused/unreachable ({e.__class__.__name__})"
            finally:
                s.close()
            rec(f"{ip}:{port} unreachable (lightpopup / worker from non-loopback)", got != "CONNECTED", got)
    b = get({"module": "adm/raw", "job": "lightpopup", "emod": "H3/analysis/oeflinf.fr", "option": "noabout",
             "parm": "cmd=new;exo=antecedant;qnum=1;seed=1"})
    rec("note: lightpopup needs no ident (allowed_jobs_without_auth) -> loopback bind is the control", "oef" in b.lower(), "lightpopup on 127.0.0.1 answers without ident")
    import shutil
    sess = WIMS_RUNTIME / "sessions"
    hit = set()
    for d, _, fs in os.walk(sess):
        for f in fs:
            try:
                if PW.encode() in Path(d, f).read_bytes():
                    hit.add(Path(d).relative_to(sess).parts[0])
            except OSError:
                pass
    for h in hit:
        shutil.rmtree(sess / h, ignore_errors=True)
    left = sum(1 for d, _, fs in os.walk(sess) for f in fs if _has(Path(d, f), PW))
    rec(f"adm/raw sessions holding the password removed ({len(hit)} folders), files still holding it: {left}", left == 0, "")
    ok = all(r["pass"] for r in R)
    (RESULTS / "P11_wims_hardening.json").write_text(json.dumps({"tests": R, "pass": ok}, indent=1))
    print("P11", "PASS" if ok else "FAIL", f"{sum(r['pass'] for r in R)}/{len(R)}")


if __name__ == "__main__":
    main()
