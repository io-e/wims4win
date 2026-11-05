# GROK_V1
"""Smoke test for AlgoGebra JSON hook on Windows WIMS runtime.

Calls wims.cgi.exe with cmd=new then cmd=reply&special_parm=algogebra_json,
extracts the HTML-comment JSON block, and checks schema basics.

stdlib only. Example:

  python AlgoGebra_Wims_Integration/tests/test_algogebra_json_GROK_V1.py
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_RT = Path(
    r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef"
)
DEFAULT_OEF2WIMS = Path(
    r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok"
    r"\wims-runtime-r19040-grok\bin\oef2wims.exe"
)
MODULE = "H6/test/oef2html.fr"
BEGIN = "ALGOGEBRA_JSON_BEGIN"
END = "ALGOGEBRA_JSON_END"


def run_cgi(cgi: Path, home: Path, query: str) -> str:
    env = dict(
        os.environ,
        REQUEST_METHOD="GET",
        QUERY_STRING=query,
        REMOTE_ADDR="127.0.0.1",
        SERVER_NAME="localhost",
        SERVER_PORT="80",
        SCRIPT_NAME="/wims/wims.cgi",
        HTTP_HOST="localhost",
        HTTP_ACCEPT_LANGUAGE="fr",
        HTTP_USER_AGENT="test_algogebra_json",
    )
    p = subprocess.run(
        [str(cgi)],
        cwd=str(home / "public_html"),
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=120,
    )
    raw = p.stdout
    m = re.search(rb"charset=([\w-]+)", raw[:600])
    enc = m.group(1).decode() if m else "utf-8"
    return raw.decode(enc, "replace")


def extract_json(html: str) -> dict:
    m = re.search(
        r"<!--\s*" + BEGIN + r"\s*(.*?)\s*" + END + r"\s*-->",
        html,
        re.S,
    )
    if not m:
        raise AssertionError("JSON comment block not found")
    body = m.group(1).strip()
    return json.loads(body)


def install_def(oef2wims: Path, oef: Path, mod_def_dir: Path) -> None:
    import time

    mod_def_dir.mkdir(parents=True, exist_ok=True)
    out = mod_def_dir / (oef.stem + ".def")
    tmp = mod_def_dir / (oef.stem + ".def.tmp")
    if tmp.exists():
        try:
            tmp.unlink()
        except OSError:
            pass
    p = subprocess.run(
        [str(oef2wims), str(oef), str(tmp)],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if p.returncode != 0 or not tmp.exists():
        raise RuntimeError("oef2wims failed: " + p.stdout.decode("utf-8", "replace"))
    last_err = None
    for _ in range(10):
        try:
            os.replace(tmp, out)
            last_err = None
            break
        except OSError as e:
            last_err = e
            time.sleep(0.2)
    if last_err is not None:
        raise last_err


def set_seed(mod_var_proc: Path, seed: int) -> None:
    t = mod_var_proc.read_text(encoding="latin-1")
    t = re.sub(r"^!setseed .*\n", "", t, flags=re.M)
    t = t.replace(
        "!changeto oef/var.proc",
        "!setseed %d\n!changeto oef/var.proc" % seed,
        1,
    )
    mod_var_proc.write_text(t, encoding="latin-1")


def set_exindex(mod_dir: Path, name: str) -> None:
    """Minimal Exindex like oef2html.Wims.install — one exercise only."""
    (mod_dir / "Exindex").write_text(
        "!set exolist  =!char 1 to -2 of %s,\n"
        "!set titlelist=!char 1 to -2 of %s,\n"
        "!set exototal =!itemcnt $exolist\n" % (name, name),
        encoding="latin-1",
    )


def check_payload(data: dict, expect_grade: float | None = None) -> None:
    assert "grade" in data and "seed" in data and "answers" in data
    assert isinstance(data["answers"], list) and data["answers"]
    for a in data["answers"]:
        for k in ("id", "correct", "score", "expected", "given"):
            assert k in a, "missing %s in %r" % (k, a)
        assert isinstance(a["correct"], bool)
        assert a["id"].startswith(("reply", "choice"))
    if expect_grade is not None:
        assert abs(float(data["grade"]) - expect_grade) < 1e-6, data["grade"]


def exercise(
    cgi: Path,
    home: Path,
    exo: str,
    reply_fields: dict[str, str],
    expect_grade: float,
    mod_dir: Path | None = None,
) -> dict:
    if mod_dir is not None:
        set_exindex(mod_dir, exo)
    page = run_cgi(
        cgi,
        home,
        "module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % (MODULE, exo),
    )
    sm = re.search(r'name="session" value="([^"]+)"', page)
    assert sm, "no session in cmd=new"
    q = "session=%s&cmd=reply&module=%s&special_parm=algogebra_json" % (
        sm.group(1),
        MODULE,
    )
    for k, v in reply_fields.items():
        q += "&%s=%s" % (k, quote(v))
    html = run_cgi(cgi, home, q)
    assert BEGIN in html, "hook marker missing (is answer.phtml patched?)"
    data = extract_json(html)
    check_payload(data, expect_grade=expect_grade)
    return data


def main() -> int:
    rt = Path(os.environ.get("WIMS_HOME", DEFAULT_RT))
    cgi = rt / "public_html" / "wims.cgi.exe"
    oef2wims = Path(os.environ.get("OEF2WIMS", DEFAULT_OEF2WIMS))
    assert cgi.is_file(), cgi
    assert oef2wims.is_file(), oef2wims

    mod = rt / "public_html" / "modules" / "H6" / "test" / "oef2html.fr"
    sources = ROOT / "tools" / "oef-validation" / "html" / "exemples" / "sources"
    for name in ("00006", "00076"):
        install_def(oef2wims, sources / ("%s.oef" % name), mod / "def")
    set_seed(mod / "var.proc", 1)

    # regression: no special_parm → no JSON block
    set_exindex(mod, "00006")
    page = run_cgi(
        cgi,
        rt,
        "module=%s&cmd=new&exo=00006&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % MODULE,
    )
    sm = re.search(r'name="session" value="([^"]+)"', page)
    assert sm, "no session in regression cmd=new"
    html = run_cgi(
        cgi,
        rt,
        "session=%s&cmd=reply&module=%s&reply1=2" % (sm.group(1), MODULE),
    )
    assert BEGIN not in html, "JSON leaked without special_parm"
    assert "answer_analysis" in html, "regression reply did not grade"

    d1 = exercise(cgi, rt, "00006", {"reply1": "0"}, 0.0, mod_dir=mod)
    d2 = exercise(cgi, rt, "00006", {"reply1": "2"}, 1.0, mod_dir=mod)
    d3 = exercise(cgi, rt, "00076", {"reply1": "1"}, 0.0, mod_dir=mod)
    d4 = exercise(cgi, rt, "00076", {"reply1": "3, 5"}, 1.0, mod_dir=mod)

    print("OK numeric wrong", json.dumps(d1, ensure_ascii=True))
    print("OK numeric right", json.dumps(d2, ensure_ascii=True))
    print("OK checkbox wrong", json.dumps(d3, ensure_ascii=True))
    print("OK checkbox right", json.dumps(d4, ensure_ascii=True))
    print("ALL PASS")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print("FAIL:", e, file=sys.stderr)
        sys.exit(1)
