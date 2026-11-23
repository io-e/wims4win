#!/usr/bin/env python3
"""GROK_V1_1 P4 gate P12 (WIMS-side authentication of the worker's paths, patch p4_wims_GROK_V1_1.diff).
  - adm/raw job=p4render (the worker's render entry) without / with a wrong / truncated / upper-case token -> refused
    ("p4_auth_refused", no redirect, no session marked); emod=adm/... -> refused; with the token (multipart POST)
    -> 302 to the exercise and the session's var.stat has wims_p4_auth=yes;
  - special_parm=p4_json in sessions NOT opened by p4render (lightpopup session, direct module session, forged
    wims_p4_auth=yes in the query) -> the P4 block is exactly {"p4":1,"error":"auth"} (no grade, no per_field);
    in a p4render session -> the grading JSON;
  - the worker (which sends the token) renders and grades (right answer 1, wrong 0);
  - normal WIMS use not broken: lightpopup of an OEF module, a direct anonymous exercise session (new + reply with the
    normal feedback page), adm/raw listmodules (no auth job), the home page and the tool module wcalc;
  - 127.0.0.1 binding of WIMS kept (ss).
The token is read from the secrets dir and never printed; refused responses are checked not to contain it.
Writes p4_results/P12_wims_auth.json."""
import json, re, subprocess, sys, urllib.parse, urllib.request, uuid
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa
from oefc_GROK_V1 import call
from oracle_GROK_V1 import expected

TOK = WIMS_TOKEN_FILE.read_text().strip()
M, EXO = "H3/analysis/oeflinf.fr", "antecedant"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


def req(url, data=None, multipart=None):
    headers = {}
    body = None
    if multipart is not None:
        b = uuid.uuid4().hex
        parts = [f'--{b}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n' for k, v in multipart]
        body = ("".join(parts) + f"--{b}--\r\n").encode("latin-1")
        headers["Content-Type"] = f"multipart/form-data; boundary={b}"
        url = WIMS_URL + "?form-data"
    elif data is not None:
        url = WIMS_URL + "?" + urllib.parse.urlencode(data)
    r = urllib.request.Request(url, body, headers)
    try:
        resp = OPENER.open(r, timeout=60)
    except urllib.error.HTTPError as e:
        resp = e
    return resp.status if hasattr(resp, "status") else resp.code, resp.headers.get("Location", ""), resp.read().decode("latin-1")


def sid_of(loc_or_html):
    m = re.search(r"session=([A-Z0-9]{8,})", loc_or_html)
    return m.group(1) if m else None


def p4block(h):
    m = re.search(r"P4_JSON_BEGIN(.*?)P4_JSON_END", h, re.S)
    return m.group(1).strip() if m else None


def marked(sid):
    f = WIMS_RUNTIME / "sessions" / sid / "var.stat"
    return f.is_file() and "wims_p4_auth=yes" in f.read_text(errors="replace")


def p4render(token, emod=M):
    kv = [("module", "adm/raw"), ("job", "p4render")] + ([("p4_auth", token)] if token is not None else []) + \
         [("emod", emod), ("parm", f"cmd=new;exo={EXO};qnum=1;seed=1")]
    return req(None, multipart=kv)


def main():
    res = []

    def case(name, ok, **info):
        res.append({"case": name, "pass": bool(ok), **info}); print("PASS" if ok else "FAIL", name, info)

    wrong = ("0" if TOK[0] != "0" else "1") + TOK[1:]
    for name, t in [("p4render without token", None), ("p4render empty token", ""), ("p4render wrong token (same length)", wrong),
                    ("p4render truncated token", TOK[:-1]), ("p4render upper-case token", TOK.upper() if TOK.upper() != TOK else "Z" + TOK[1:]),
                    ("p4render token + 1 char", TOK + "0")]:
        c, loc, h = p4render(t)
        case(name, c == 200 and not loc and "p4_auth_refused" in h and TOK not in h and "oefstatement" not in h, http=c, redirect=bool(loc))
    c, loc, h = req(None, data={"module": "adm/raw", "job": "p4render", "emod": M, "p4_auth": wrong, "parm": f"cmd=new;exo={EXO}"})
    case("p4render GET with wrong token", "p4_auth_refused" in h and not loc, http=c)
    c, loc, h = p4render(TOK, emod="adm/raw")
    case("p4render with token, emod=adm/raw", "p4_bad_module" in h and not loc, http=c)
    c, loc, h = p4render(TOK)
    sid = sid_of(loc)
    case("p4render with token -> 302 to the exercise, session marked", c == 302 and sid and M in loc and marked(sid) and TOK not in loc,
         http=c)
    # p4_json in sessions not opened by p4render
    seed = 1
    exp = expected(M, EXO, seed)
    good = exp.get("reply1", "")

    def reply_in(s, extra=None):
        req(None, data={"session": s, "module": M, "cmd": "new", "exo": EXO, "qnum": 1, "seed": seed, **(extra or {})})
        return req(None, multipart=[("session", s), ("module", M), ("cmd", "reply"), ("special_parm", "p4_json"),
                                    ("reply1", good)] + list((extra or {}).items()))[2]

    c, loc, h = req(None, data={"module": "adm/raw", "job": "lightpopup", "emod": M, "parm": f"cmd=new;exo={EXO};qnum=1;seed={seed}"})
    s_lp = sid_of(loc)
    b = p4block(reply_in(s_lp))
    case("p4_json in a lightpopup session -> auth error only", b == '{"p4":1,"error":"auth"}', block=b)
    c, loc, h = req(None, data={"module": M, "cmd": "new", "exo": EXO, "qnum": 1, "seed": seed})
    s_direct = sid_of(h)
    b = p4block(reply_in(s_direct))
    case("p4_json in a direct module session -> auth error only", b == '{"p4":1,"error":"auth"}', block=b)
    b = p4block(reply_in(s_lp, {"wims_p4_auth": "yes"}))
    case("p4_json with forged wims_p4_auth=yes -> auth error only", b == '{"p4":1,"error":"auth"}' and not marked(s_lp), block=b)
    b = p4block(reply_in(sid))
    d = json.loads(b) if b and b.startswith("{") else {}
    case("p4_json in the p4render session -> grading JSON (right answer 1)", d.get("kind") == "final" and d.get("grade") == 1, block_keys=sorted(d))
    # worker path (sends the token)
    c1, j1, _, _ = call("/oef/check", {"module": M, "exo": EXO, "seed": seed, "lang": "fr", "answers": {"reply1": good}})
    c2, j2, _, _ = call("/oef/check", {"module": M, "exo": EXO, "seed": seed, "lang": "fr", "answers": {"reply1": "987654"}})
    case("worker with token: right 1 / wrong 0", c1 == 200 and j1.get("score") == 1 and c2 == 200 and j2.get("score") == 0,
         scores=[j1.get("score"), j2.get("score")])
    # normal WIMS use
    c, loc, h = req(None, data={"module": "adm/raw", "job": "lightpopup", "emod": M, "option": "noabout", "parm": f"cmd=new;exo={EXO};qnum=1;seed=2"})
    c2, _, h2 = req(loc) if loc else (0, "", "")
    case("normal: lightpopup still opens an OEF exercise", c == 302 and "oefstatement" in h2, http=[c, c2])
    c, _, h = req(None, data={"module": M, "cmd": "new", "exo": "image", "qnum": 1})
    s2 = sid_of(h)
    c3, _, h3 = req(None, data={"session": s2, "module": M, "cmd": "reply", "reply1": "1"})
    case("normal: anonymous exercise new + reply (normal feedback page, no P4 block)", c == 200 and "oefstatement" in h and c3 == 200
         and ("oefanswer" in h3 or "oef_indbad" in h3 or "oefreply" in h3 or "score" in h3.lower()) and "P4_JSON_BEGIN" not in h3, http=[c, c3])
    c, _, h = req(None, data={"module": "adm/raw", "job": "listmodules"})
    case("normal: adm/raw listmodules (no-auth job)", c == 200 and '"status" : "OK"' in h, http=c)
    c, _, h = req(None, data={"module": "home"})
    case("normal: home page", c == 200 and "<html" in h.lower(), http=c)
    c, _, h = req(None, data={"module": "tool/wcalc.fr"})
    case("normal: tool module wcalc.fr", c == 200 and "<html" in h.lower() and "ERROR" not in h[:200], http=c)
    ss = subprocess.run(["ss", "-ltn", f"sport = :{WIMS_PORT}"], capture_output=True, text=True).stdout
    addrs = re.findall(r"LISTEN\s+\d+\s+\d+\s+(\S+)", ss)
    case("WIMS bound to 127.0.0.1 only", addrs and all(a.startswith("127.0.0.1:") for a in addrs), addrs=addrs)
    ok = all(r["pass"] for r in res)
    (RESULTS / "P12_wims_auth.json").write_text(json.dumps({"cases": res, "pass": ok}, indent=1))
    print("P12", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
