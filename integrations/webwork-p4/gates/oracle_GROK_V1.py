#!/usr/bin/env python3
"""GROK_V1 P4 test oracle: the known right answers of a pilot item for a seed, read directly from WIMS through the
unchanged algogebra_json hook. TEST HARNESS ONLY (gates P2/P4/P7); the worker and WeBWorK never call it.
Also the pilot list, DB helpers and WIMS session counting."""
import json, re, subprocess, urllib.parse, urllib.request
from pathlib import Path

P4 = Path(__file__).resolve().parents[1]
import sys as _s; _s.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import WIMS_URL as WIMS, MARIADB_DEFAULTS, MARIADB_DB, WIMS_RUNTIME, RESULTS
L, R = "H3/analysis/oeflinf.fr", "U2/arithmetic/oefrational.fr"
PILOT = [(L, "antecedant"), (L, "fnctlin2"), (L, "image"), (L, "prob2"), (R, "ratapprox"), (R, "approx"),
         (R, "decprod"), (R, "perdecimal1"), (R, "decimal2rat0"), (R, "perdecimal2")]


def _get(q):
    import time
    for attempt in range(20):  # WIMS answers 450 "threshold" (load above wims.conf threshold) -> wait and retry
        try:
            with urllib.request.urlopen(WIMS + "?" + urllib.parse.urlencode(q), timeout=30) as r:
                b = r.read()
            break
        except urllib.error.HTTPError as e:
            if e.code != 450 or attempt == 19:
                raise
            time.sleep(3)
    try:
        return b.decode("utf-8")
    except UnicodeDecodeError:
        return b.decode("latin-1")


def _ag(html):
    m = re.search(r"ALGOGEBRA_JSON_BEGIN(.*?)ALGOGEBRA_JSON_END", html, re.S)
    return json.loads(m.group(1)) if m else None


def fields_of(html):
    out = []
    for n in re.findall(r'name="((?:reply|choice)\d+)"', html):
        if n not in out:
            out.append(n)
    return out


def expected(module, exo, seed, lang="fr"):
    """{field_id: expected} for every field of the exercise (all \\steps), via WIMS directly."""
    known = {}
    for _ in range(6):
        h = _get({"module": "adm/raw", "job": "lightpopup", "emod": module, "option": "noabout", "lang": lang,
                  "parm": f"cmd=new;exo={exo};qnum=1;seed={seed}"})
        sid = re.search(r'name="session" value="([A-Z0-9]+)', h).group(1)
        fields = fields_of(h)
        while True:
            q = {"session": sid, "module": module, "cmd": "reply", "special_parm": "algogebra_json", "lang": lang}
            for f in fields:
                q[f] = known.get(f, "1" if f.startswith("choice") else "0")
            h = _get(q)
            ag = _ag(h)
            if ag:
                new = False
                for a in ag["answers"]:
                    if a["id"] not in known:
                        known[a["id"]] = a["expected"]; new = True
                if not new:
                    return known
                break  # restart with the newly known values
            nf = [f for f in fields_of(h) if f not in fields]
            if not nf:
                return known
            fields = nf
    return known


def native_statement(module, exo, seed, lang="fr"):
    """GROK_V1_1 (D1 rule): visible text of the native WIMS page (first step, same seed, MathML math = useropts=32)
    before any answer. A value printed there is exercise data shown to every WIMS student, not a bridge leak."""
    import html as _H
    h = _get({"module": "adm/raw", "job": "lightpopup", "emod": module, "option": "noabout", "lang": lang,
              "parm": f"cmd=new;exo={exo};qnum=1;seed={seed};useropts=32"})
    h = re.sub(r"(?is)<(script|style)\b.*?</\1>", " ", h)
    return _H.unescape(re.sub(r"<[^>]+>", " ", h))


def db(sql):
    out = subprocess.run(["mariadb", f"--defaults-file={MARIADB_DEFAULTS}", MARIADB_DB, "-B", "-e", sql],
                         capture_output=True, text=True, check=True).stdout
    rows = [l.split("\t") for l in out.splitlines()]
    return [dict(zip(rows[0], r)) for r in rows[1:]] if rows else []


def wims_sessions():
    d = WIMS_RUNTIME / "sessions"
    return len([p for p in d.iterdir() if p.is_dir()]) if d.is_dir() else 0
