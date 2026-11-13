# CLAUDE_V1  (supersedes tests/test_algogebra_json_GROK_V1.py, kept as archive)
"""Smoke test for the AlgoGebra JSON hook on a WIMS runtime (Windows or Linux).

Calls the CGI directly: cmd=new, then cmd=reply&special_parm=algogebra_json,
extracts the HTML-comment JSON block and checks it against the approved
contract (spec GROK_V1_2 draft, rules [V1_2], 2026-11-13):

  * schema keys incl. feedback, JSON types, seed == fixed seed
  * grade == the score printed by answer.phtml ("note de X sur 10")
  * answers[] count == analysis rows displayed
  * given == what was sent (singlespaced)
  * expected is REPLAYABLE: sent back as the answer it yields grade 1.0  (F1)
  * form values are percent-encoded in the charset of the cmd=new page   (F6)
  * no block without special_parm, page otherwise unchanged
  * empty submission: HTTP page with class="wims_msg alert" and no block  (F4)

Fixes vs GROK_V1: quote() used UTF-8 (WIMS reads the page charset, accented
answers were mis-graded); expected was never replayed, so the GROK_V1 hook
body ("3,5;1,2,3,4,5,6" for a checkbox) passed although unusable; def/ dir
read-only attribute broke oef2wims on Windows; Windows-only CGI name.

stdlib only, Python 3.8+. Run with:  python -X utf8 -I <this file>
Env: WIMS_HOME (runtime root), OEF2WIMS (compiler), OEF_SOURCES (dir with
00006.oef / 00076.oef; default: <repo>/tools/oef-validation/html/exemples/sources)
"""
from __future__ import annotations

import json
import os
import re
import stat
import subprocess
import sys
import time
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_RT = Path(r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef")
DEFAULT_OEF2WIMS = Path(
    r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok"
    r"\wims-runtime-r19040-grok\bin\oef2wims.exe"
)
MODULE = "H6/test/oef2html.fr"
BEGIN, END = "ALGOGEBRA_JSON_BEGIN", "ALGOGEBRA_JSON_END"
BLOCK_RE = re.compile(r"<!--\s*" + BEGIN + r"\s*(.*?)\s*" + END + r"\s*-->", re.S)
SEED = 1
NEW_QUERY = (
    "module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes"
    "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr"
)
CASES = [  # (exo, type, wrong, right, expected_grade_wrong)
    ("00006", "numeric", "0", "2", 0.0),
    ("00076", "checkbox", "1", "3, 5", 0.0),
]

FAILS: list[str] = []


def check(cond: bool, msg: str) -> None:
    if not cond:
        FAILS.append(msg)
        print("  FAIL", msg)


# ----------------------------------------------------------------- runtime --
def find_cgi(home: Path) -> Path:
    for name in ("wims.cgi.exe", "wims.cgi"):
        p = home / "public_html" / name
        if p.is_file():
            return p
    raise FileNotFoundError("no wims.cgi(.exe) under %s/public_html" % home)


def run_cgi(cgi: Path, home: Path, query: str) -> tuple[str, str]:
    """GET request through the CGI. Returns (page, charset)."""
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
        HTTP_USER_AGENT="test_algogebra_json_smoke",
    )
    p = subprocess.run([str(cgi)], cwd=str(home / "public_html"), env=env,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
    raw = p.stdout
    m = re.search(rb"charset=([\w-]+)", raw[:600])
    enc = m.group(1).decode() if m else "windows-1252"
    return raw.decode(enc, "replace"), enc


def make_writable(path: Path) -> None:
    """Windows: the module def/ dir comes back read-only between runs."""
    if not path.exists():
        return
    for p in [path, *path.rglob("*")]:
        try:
            p.chmod(p.stat().st_mode | stat.S_IWRITE)
        except OSError:
            pass


def install_def(oef2wims: Path, oef: Path, mod_def_dir: Path) -> None:
    mod_def_dir.mkdir(parents=True, exist_ok=True)
    make_writable(mod_def_dir)
    out = mod_def_dir / (oef.stem + ".def")
    tmp = mod_def_dir / (oef.stem + ".def.tmp")
    if tmp.exists():
        tmp.unlink()
    p = subprocess.run([str(oef2wims), str(oef), str(tmp)],
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode != 0 or not tmp.exists():
        raise RuntimeError("oef2wims failed on %s: %s" % (oef.name, p.stdout.decode("utf-8", "replace")))
    last = None
    for _ in range(10):
        try:
            os.replace(tmp, out)
            last = None
            break
        except OSError as e:
            last = e
            time.sleep(0.2)
    if last is not None:
        raise last


def set_seed(mod_var_proc: Path, seed: int) -> None:
    make_writable(mod_var_proc)
    t = mod_var_proc.read_text(encoding="latin-1")
    t = re.sub(r"^!setseed .*\n", "", t, flags=re.M)
    t = t.replace("!changeto oef/var.proc", "!setseed %d\n!changeto oef/var.proc" % seed, 1)
    mod_var_proc.write_text(t, encoding="latin-1")


def set_exindex(mod_dir: Path, name: str) -> None:
    make_writable(mod_dir / "Exindex")
    (mod_dir / "Exindex").write_text(
        "!set exolist  =!char 1 to -2 of %s,\n"
        "!set titlelist=!char 1 to -2 of %s,\n"
        "!set exototal =!itemcnt $exolist\n" % (name, name),
        encoding="latin-1",
    )


# -------------------------------------------------------------------- page --
def html_score(page: str) -> float | None:
    m = re.search(r"note de\s*([0-9]+(?:[.,][0-9]+)?)\s*sur\s*10", page)
    return float(m.group(1).replace(",", ".")) / 10 if m else None


def html_rows(page: str) -> int:
    """Rows of the answer_analysis block (div or fieldset), like oef2html.parse_rows."""
    m = re.search(r'<(div|fieldset)\b[^>]*class="[^"]*\banswer_analysis\b[^"]*"[^>]*>', page, re.I)
    if not m:
        return 0
    tag, depth, pos = m.group(1), 1, m.end()
    pat = re.compile(r"<(/?)%s\b[^>]*>" % tag, re.I)
    while depth:
        n = pat.search(page, pos)
        if not n:
            pos = len(page)
            break
        depth += -1 if n.group(1) else 1
        pos = n.end()
    inner = page[m.end():pos]
    return sum(1 for tr in re.findall(r"<tr>(.*?)</tr>", inner, re.S) if len(re.findall(r"<td", tr)) >= 2)


def normalise(page: str) -> str:
    page = BLOCK_RE.sub("", page)
    page = re.sub(r"\b[0-9A-Z]{10}(?:\.\d+|_[a-z]+)?\b", "SESSION", page)
    page = page.replace("special_parm=algogebra_json", "special_parm=")
    page = re.sub(r"wims_mathml\d+", "wims_mathml#", page)
    page = re.sub(r"[ \t]+\n", "\n", page)
    return re.sub(r"\n\s*\n+", "\n", page)


def singlespace(s: str) -> str:
    return re.sub(r"\s+", " ", s).strip()


# ----------------------------------------------------------------- session --
class Session:
    def __init__(self, cgi: Path, home: Path, mod_dir: Path, exo: str):
        self.cgi, self.home = cgi, home
        set_exindex(mod_dir, exo)
        page, self.charset = run_cgi(cgi, home, NEW_QUERY % (MODULE, exo))
        m = re.search(r'name="session" value="([^"]+)"', page)
        if not m:
            raise RuntimeError("no session in cmd=new for %s" % exo)
        self.sid = m.group(1)

    def reply(self, fields: dict[str, str], hook: bool = True) -> str:
        q = "session=%s&cmd=reply&module=%s" % (self.sid, MODULE)
        if hook:
            q += "&special_parm=algogebra_json"
        for k, v in fields.items():  # F6: encode in the page charset, as a browser does
            q += "&%s=%s" % (k, quote(v, encoding=self.charset, errors="replace"))
        return run_cgi(self.cgi, self.home, q)[0]


def extract_json(page: str) -> dict:
    m = BLOCK_RE.search(page)
    if not m:
        raise AssertionError("JSON comment block not found")
    return json.loads(m.group(1))


def check_payload(exo: str, label: str, data: dict, page: str, sent: dict[str, str],
                  expect_grade: float) -> None:
    tag = "%s %s:" % (exo, label)
    check(set(data) == {"grade", "seed", "answers", "feedback"}, tag + " top-level keys %r" % sorted(data))
    check(isinstance(data.get("grade"), (int, float)), tag + " grade is not a number")
    check(data.get("seed") == SEED, tag + " seed %r != %d" % (data.get("seed"), SEED))
    check(isinstance(data.get("feedback"), str), tag + " feedback not a string")
    answers = data.get("answers")
    check(isinstance(answers, list) and len(answers) > 0, tag + " answers empty")
    for a in answers or []:
        check(set(a) == {"id", "correct", "score", "feedback", "expected", "given"},
              tag + " answer keys %r" % sorted(a))
        check(isinstance(a.get("correct"), bool), tag + " correct not boolean")
        check(isinstance(a.get("score"), (int, float)), tag + " score not a number")
        check(a.get("correct") == (float(a.get("score", 0)) >= 1), tag + " correct != (score>=1)")
        check(str(a.get("id", "")).startswith(("reply", "choice")), tag + " bad id %r" % a.get("id"))
        if a.get("id") in sent:
            check(a.get("given") == singlespace(sent[a["id"]]),
                  tag + " given %r != sent %r" % (a.get("given"), sent[a["id"]]))
    check(abs(float(data.get("grade", -1)) - expect_grade) < 1e-6,
          tag + " grade %r != expected %r" % (data.get("grade"), expect_grade))
    hs = html_score(page)
    check(hs is not None and abs(hs - float(data.get("grade", -1))) < 1e-6,
          tag + " grade %r != HTML score %r" % (data.get("grade"), hs))
    check(len(answers or []) == html_rows(page),
          tag + " %d answers != %d analysis rows" % (len(answers or []), html_rows(page)))


# -------------------------------------------------------------------- main --
def main() -> int:
    rt = Path(os.environ.get("WIMS_HOME", DEFAULT_RT))
    cgi = find_cgi(rt)
    oef2wims = Path(os.environ.get("OEF2WIMS", DEFAULT_OEF2WIMS))
    sources = Path(os.environ.get("OEF_SOURCES", ROOT / "tools" / "oef-validation" / "html" / "exemples" / "sources"))
    assert oef2wims.is_file(), oef2wims
    mod = rt / "public_html" / "modules" / "H6" / "test" / "oef2html.fr"
    assert (mod / "var.proc").is_file(), "test module missing: %s" % mod

    for exo, *_ in CASES:
        install_def(oef2wims, sources / (exo + ".oef"), mod / "def")
    set_seed(mod / "var.proc", SEED)
    print("runtime %s  cgi %s" % (rt, cgi.name))

    for exo, typ, wrong, right, grade_wrong in CASES:
        print("[%s %s]" % (exo, typ))
        # W: wrong answer with hook
        s = Session(cgi, rt, mod, exo)
        page_w = s.reply({"reply1": wrong})
        check(BEGIN in page_w, "%s: hook marker missing (is answer.phtml patched?)" % exo)
        if BEGIN in page_w:
            d_w = extract_json(page_w)
            check_payload(exo, "wrong", d_w, page_w, {"reply1": wrong}, grade_wrong)
            print("  wrong:", json.dumps(d_w, ensure_ascii=True))
            # R: replay `expected` as the answer -> must grade 1.0 (F1)
            exp = {a["id"]: a["expected"] for a in d_w["answers"]}
            s_r = Session(cgi, rt, mod, exo)
            page_r = s_r.reply(exp)
            if BEGIN in page_r:
                d_r = extract_json(page_r)
                check(abs(float(d_r.get("grade", 0)) - 1.0) < 1e-6 and all(a["correct"] for a in d_r["answers"]),
                      "%s: replaying expected=%r graded %r, not 1.0 (F1: expected not usable as an answer)"
                      % (exo, exp, d_r.get("grade")))
            else:
                check(False, "%s: replaying expected=%r was rejected by WIMS (no block; F1)" % (exo, exp))
        # right answer with hook
        s = Session(cgi, rt, mod, exo)
        page_ok = s.reply({"reply1": right})
        check(BEGIN in page_ok, "%s: hook marker missing on right answer" % exo)
        if BEGIN in page_ok:
            d_ok = extract_json(page_ok)
            check_payload(exo, "right", d_ok, page_ok, {"reply1": right}, 1.0)
            print("  right:", json.dumps(d_ok, ensure_ascii=True))
        # N: no special_parm -> no block, page otherwise identical
        s = Session(cgi, rt, mod, exo)
        page_n = s.reply({"reply1": right}, hook=False)
        check(BEGIN not in page_n, "%s: JSON leaked without special_parm" % exo)
        check("answer_analysis" in page_n, "%s: reply without special_parm did not grade" % exo)
        check(normalise(page_n) == normalise(page_ok), "%s: page differs outside the JSON block" % exo)
        # F4: empty submission -> alert, no block, HTTP 200 (CGI exit)
        s = Session(cgi, rt, mod, exo)
        page_e = s.reply({"reply1": ""})
        check(BEGIN not in page_e, "%s: block emitted on empty submission" % exo)
        check('class="wims_msg alert"' in page_e, "%s: empty submission shows no wims_msg alert" % exo)

    if FAILS:
        print("FAILED %d check(s)" % len(FAILS))
        return 1
    print("ALL PASS")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:  # setup errors
        print("FAIL:", e, file=sys.stderr)
        sys.exit(2)
