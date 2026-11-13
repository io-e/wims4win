# CLAUDE_V1
"""Corpus review of the AlgoGebra JSON hook on the Windows WIMS runtime.

For every .oef given (default: the sample corpus under tools/oef-validation),
drives wims.cgi.exe through cmd=new / cmd=reply and checks the hook output
against the HTML the engine renders in the same page:

  W  wrong-ish dummy answers + special_parm=algogebra_json
       - marker present, body is valid JSON, schema keys present
       - len(answers) == replycnt of the compiled .def
       - grade == "note de X sur 10" / 10 printed by answer.phtml
       - answers[i].correct == oef_indgood on row i of answer_analysis
       - answers[i].given == what was sent (after WIMS !singlespace)
  R  replay: the JSON "expected" values sent back as answers (new session,
       same seed) -> should grade 1.0 with every answer correct. A miss here is
       a representation finding (menu index vs text, checkbox ";options" tail...).
  N  same answers as W without special_parm -> no marker, and the page equals
       W with the comment block removed (session ids / mathml ids normalised).
  E  encoding: is the JSON block pure ASCII? does it decode under the page charset?

stdlib only. Nothing is written outside the test module dir of the runtime
(modules/H6/test/oef2html.fr) and its sessions/, like oef2html.py does.

  python AlgoGebra_Wims_Integration/tests/test_algogebra_json_corpus_CLAUDE_V1.py
  python ... --sources tools/oef-validation/html/exemples/sources --only 00085 00008
"""
from __future__ import annotations

import argparse
import html as htmlmod
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
sys.path.insert(0, str(ROOT / "tools" / "oef-validation" / "html"))
from oef2html import find_div, form_fields, parse_rows, strip_tags  # noqa: E402

DEFAULT_RT = Path(r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef")
DEFAULT_OEF2WIMS = Path(r"D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok"
                        r"\wims-runtime-r19040-grok\bin\oef2wims.exe")
MODULE = "H6/test/oef2html.fr"
BEGIN, END = "ALGOGEBRA_JSON_BEGIN", "ALGOGEBRA_JSON_END"
BLOCK_RE = re.compile(r"<!--\s*" + BEGIN + r"\s*(.*?)\s*" + END + r"\s*-->", re.S)
NEW_QUERY = ("module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes"
             "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr")


# ----------------------------------------------------------------- CGI driver
def run_cgi(cgi: Path, home: Path, query: str) -> tuple[bytes, str, str]:
    env = dict(os.environ, REQUEST_METHOD="GET", QUERY_STRING=query,
               REMOTE_ADDR="127.0.0.1", SERVER_NAME="localhost", SERVER_PORT="80",
               SCRIPT_NAME="/wims/wims.cgi", HTTP_HOST="localhost",
               HTTP_ACCEPT_LANGUAGE="fr", HTTP_USER_AGENT="algogebra_corpus_review")
    p = subprocess.run([str(cgi)], cwd=str(home / "public_html"), env=env,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
    raw = p.stdout
    m = re.search(rb"charset=([\w-]+)", raw[:800], re.I)
    enc = m.group(1).decode() if m else "utf-8"
    return raw, enc, raw.decode(enc, "replace")


def make_writable(path: Path) -> None:
    for p in [path, *path.rglob("*")]:
        try:
            os.chmod(p, stat.S_IWRITE | stat.S_IREAD | (stat.S_IEXEC if p.is_dir() else 0))
        except OSError:
            pass


def compile_oef(oef2wims: Path, oef: Path, dest: Path) -> bytes:
    tmp = dest.with_suffix(".def.tmp")
    if tmp.exists():
        tmp.unlink()
    p = subprocess.run([str(oef2wims), str(oef), str(tmp)],
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if p.returncode != 0 or not tmp.exists():
        raise RuntimeError("oef2wims: " + p.stdout.decode("utf-8", "replace").strip())
    for _ in range(10):
        try:
            os.replace(tmp, dest)
            break
        except OSError:
            time.sleep(0.2)
    return dest.read_bytes()


def set_seed(var_proc: Path, seed: int) -> None:
    t = var_proc.read_text(encoding="latin-1")
    t = re.sub(r"^!setseed .*\n", "", t, flags=re.M)
    t = t.replace("!changeto oef/var.proc", "!setseed %d\n!changeto oef/var.proc" % seed, 1)
    var_proc.write_text(t, encoding="latin-1")


def set_exindex(mod: Path, name: str) -> None:
    (mod / "Exindex").write_text(
        "!set exolist  =!char 1 to -2 of %s,\n!set titlelist=!char 1 to -2 of %s,\n"
        "!set exototal =!itemcnt $exolist\n" % (name, name), encoding="latin-1")


def def_counts(deftext: bytes) -> dict:
    t = deftext.decode("latin-1")
    g = lambda k: int(re.search(r"^%s=(\d+)" % k, t, re.M).group(1)) if re.search(r"^%s=(\d+)" % k, t, re.M) else 0
    types = re.findall(r"^replytype(\d+)=(\S*)", t, re.M)
    return {"replycnt": g("replycnt"), "choicecnt": g("choicecnt"),
            "feedbackcnt": g("feedbackcnt"), "types": dict((int(a), b) for a, b in types)}


# ----------------------------------------------------------------- page facts
def html_score(page: str):
    sc = find_div(page, "answer_analysis")
    if not sc:
        return None
    m = re.search(r"note de\s*([0-9]+(?:[.,][0-9]+)?)\s*sur\s*10", strip_tags(sc[2]))
    return float(m.group(1).replace(",", ".")) if m else None


def html_rows(page: str) -> list:
    an = find_div(page, "answer_analysis")
    return parse_rows(an[2]) if an else []


def html_row_flags(page: str) -> list:
    """Per analysis row: True (only oef_indgood), False (a bad/partial/prec indicator),
    None when undecidable (fill types tag every expected tile oef_indgood)."""
    an = find_div(page, "answer_analysis")
    if not an:
        return []
    flags = []
    for tr in re.findall(r"<tr>(.*?)</tr>", an[2], re.S):
        if len(re.findall(r"<td", tr)) < 2:
            continue
        good = "oef_indgood" in tr
        bad = any(c in tr for c in ("oef_indbad", "oef_indpartial", "oef_indprec"))
        flags.append(True if good and not bad else False if bad and not good else None)
    return flags


def singlespace(s: str) -> str:
    return re.sub(r"\s+", " ", s)


# session ids are 10 alphanumerics (e.g. 6YA0C4A131.1, YPA11AB25D_mhelp), as in compare_html.py
SESSION_RE = re.compile(r"\b[0-9A-Z]{10}(?:\.\d+|_[a-z]+)?\b")
NORMALISERS = [
    (SESSION_RE, "SESSION"),
    # WIMS echoes special_parm into every navigation link of the page (documented finding)
    (re.compile(r"special_parm=algogebra_json"), "special_parm="),
    (re.compile(r"wims_mathml\d+"), "wims_mathml#"),
    (re.compile(r"modif=\d+"), "modif=#"),
    (re.compile(r"\d{4}-\d{2}-\d{2}[ .T]\d{2}:\d{2}(:\d{2})?"), "DATE"),
    (re.compile(r"[ \t]+\n"), "\n"),
    (re.compile(r"\n\s*\n+"), "\n"),   # removing the comment block leaves blank lines
]


def normalise(page: str) -> str:
    page = BLOCK_RE.sub("", page)
    for rx, rep in NORMALISERS:
        page = rx.sub(rep, page)
    return page


def first_diff(a: str, b: str) -> str:
    la, lb = a.splitlines(), b.splitlines()
    for i, (x, y) in enumerate(zip(la, lb)):
        if x != y:
            return "line %d:\n  W: %s\n  N: %s" % (i + 1, x.strip()[:160], y.strip()[:160])
    return "length %d vs %d lines" % (len(la), len(lb))


# ----------------------------------------------------------------- one exercise
def dummy_answers(fields: dict) -> dict:
    out = {}
    for name, vals in fields.items():
        out[name] = (vals[0] if vals and vals[0] != "" else "0") if vals is not None else "0"
    return out


def reply_query(session: str, answers: dict, hook: bool) -> str:
    q = "session=%s&cmd=reply&module=%s" % (session, MODULE)
    if hook:
        q += "&special_parm=algogebra_json"
    for k, v in answers.items():
        q += "&%s=%s" % (k, quote(v, encoding=POST_CHARSET, errors="replace"))
    return q


QNUM = 1
POST_CHARSET = "windows-1252"   # a browser submits a form in the page charset; so must a client


def new_session(cgi, rt, exo):
    global POST_CHARSET
    _, enc, page = run_cgi(cgi, rt, (NEW_QUERY % (MODULE, exo)).replace("qnum=1", "qnum=%d" % QNUM))
    POST_CHARSET = enc
    m = re.search(r'name="session" value="([^"]+)"', page)
    if not m:
        msg = find_div(page, "wims_msg") or find_div(page, "wimsbody")
        raise RuntimeError("no session in cmd=new: " + (strip_tags(msg[2])[:200] if msg else "?"))
    return m.group(1), page


def replay(cgi, rt, exo, answers: dict) -> dict:
    """Grade `answers` in a fresh session (same seed) with the hook on."""
    session, _ = new_session(cgi, rt, exo)
    _, _, page = run_cgi(cgi, rt, reply_query(session, answers, True))
    m = BLOCK_RE.search(page)
    if not m:
        err = find_div(page, "wims_msg")
        return {"ok": False, "no_block": True,
                "msg": strip_tags(err[2])[:160] if err else "no wims_msg"}
    d = json.loads(m.group(1).strip())
    bad = [(a["id"], a["expected"], a["given"]) for a in d["answers"] if not a["correct"]]
    return {"ok": float(d["grade"]) >= 0.999 and not bad, "grade": d["grade"],
            "correct": [a["correct"] for a in d["answers"]], "html_score": html_score(page), "bad": bad}


def review_exercise(cgi: Path, rt: Path, oef2wims: Path, mod: Path, oef: Path, seed: int) -> dict:
    name = oef.stem
    res = {"exo": name, "checks": {}, "findings": [], "errors": []}
    counts = def_counts(compile_oef(oef2wims, oef, mod / "def" / (name + ".def")))
    res["def"] = counts
    set_exindex(mod, name)
    set_seed(mod / "var.proc", seed)

    # ---- W: dummy answers with hook
    session, new_page = new_session(cgi, rt, name)
    fields = form_fields(new_page)
    if not fields:
        res["errors"].append("no reply/choice fields in cmd=new form")
        return res
    answers = dummy_answers(fields)
    res["sent"] = answers
    raw_w, enc_w, page_w = run_cgi(cgi, rt, reply_query(session, answers, True))
    res["charset"] = enc_w
    m = BLOCK_RE.search(page_w)
    res["checks"]["W.marker"] = bool(m)
    if not m:
        res["errors"].append("hook marker missing on cmd=reply (answer_analysis=%s)" %
                             ("answer_analysis" in page_w))
        res["html_excerpt"] = strip_tags(find_div(page_w, "wimsbody")[2] if find_div(page_w, "wimsbody") else page_w)[:300]
        return res
    body = m.group(1).strip()
    res["json_raw"] = body
    try:
        data = json.loads(body)
        res["checks"]["W.json_valid"] = True
    except json.JSONDecodeError as e:
        res["checks"]["W.json_valid"] = False
        res["errors"].append("JSON invalid: %s" % e)
        return res
    res["json"] = data
    keys_ok = {"grade", "seed", "answers", "feedback"} <= set(data) and all(
        {"id", "correct", "score", "feedback", "expected", "given"} <= set(a) for a in data["answers"])
    res["checks"]["W.schema_keys"] = keys_ok
    res["checks"]["W.seed"] = data.get("seed") == seed
    n_json = len(data["answers"])
    res["n_def"] = counts["replycnt"] + counts["choicecnt"]
    if n_json != res["n_def"]:
        res["findings"].append("answers=%d, def replycnt=%d choicecnt=%d (ungraded replies omitted?)" %
                               (n_json, counts["replycnt"], counts["choicecnt"]))
    # grade vs HTML sentence
    hs = html_score(page_w)
    res["html_score"] = hs
    res["checks"]["W.grade_matches_html"] = (hs is not None and abs(float(data["grade"]) - hs / 10) < 1e-6)
    if hs is None:
        res["findings"].append("no 'note de X sur 10' sentence (status/qnum?); grade=%s" % data["grade"])
    elif not res["checks"]["W.grade_matches_html"]:
        res["findings"].append("grade %s != html %s/10" % (data["grade"], hs))
    # per-answer correct vs HTML indicator rows
    rows = html_rows(page_w)
    flags = html_row_flags(page_w)
    res["html_rows"] = [[r[0], r[1], f, r[3]] for r, f in zip(rows, flags)]
    res["checks"]["W.count_matches_html_rows"] = (len(flags) == n_json)
    if len(flags) == n_json:
        mism = [i + 1 for i, (a, f) in enumerate(zip(data["answers"], flags))
                if f is not None and bool(a["correct"]) != f]
        res["checks"]["W.correct_matches_html"] = (not mism) if any(f is not None for f in flags) else None
        if mism:
            res["findings"].append("correct flag differs from the HTML indicator on rows %s" % mism)
    else:
        res["checks"]["W.correct_matches_html"] = None
        res["findings"].append("html analysis rows=%d vs json answers=%d: the hook reports answers the "
                               "engine did not grade (step not reached / empty replygood)" % (len(flags), n_json))
    # correct <=> score>=1 ; given == sent
    res["checks"]["W.correct_iff_score1"] = all(bool(a["correct"]) == (float(a["score"]) >= 1) for a in data["answers"])
    gm = []
    for a in data["answers"]:
        sent = singlespace(answers.get(a["id"], "")).strip()
        if singlespace(str(a["given"])).strip() != sent:
            gm.append("%s: given=%r sent=%r" % (a["id"], a["given"], sent))
    res["checks"]["W.given_matches_sent"] = not gm
    if gm:
        res["findings"].append("given != sent: " + "; ".join(gm))
    # ---- E: encoding
    res["checks"]["E.block_ascii"] = all(b < 0x80 for b in body.encode(enc_w, "replace"))
    res["checks"]["E.types_numeric"] = isinstance(data["grade"], (int, float)) and all(
        isinstance(a["score"], (int, float)) for a in data["answers"])

    # ---- R: replay expected as answers (then once more with the ";optionlist" tail cut)
    exp = {a["id"]: str(a["expected"]) for a in data["answers"]}
    base = dict(answers)                  # keep fields the JSON did not cover
    base.update(exp)
    r1 = replay(cgi, rt, name, base)
    res["replay"] = r1
    res["checks"]["R.expected_replays_to_1"] = r1["ok"]
    if not r1["ok"]:
        if r1.get("no_block"):
            res["findings"].append("replaying expected: engine rejected the answer, no hook block: %s" % r1["msg"])
        else:
            res["findings"].append("replaying expected does not grade 1.0 (grade=%s): %s" % (r1["grade"], r1["bad"]))
        if any(";" in v for v in exp.values()):
            trimmed = dict(answers)
            trimmed.update({k: v.split(";", 1)[0] for k, v in exp.items()})
            r2 = replay(cgi, rt, name, trimmed)
            res["replay_trimmed"] = r2
            res["checks"]["R.replays_after_semicolon_trim"] = r2["ok"]
            res["findings"].append("after cutting expected at ';': %s" %
                                   ("grades 1.0" if r2["ok"] else "still not 1.0: %s" % (r2.get("bad") or r2.get("msg"))))

    # ---- N: no special_parm -> no marker, same page
    session3, _ = new_session(cgi, rt, name)
    _, _, page_n = run_cgi(cgi, rt, reply_query(session3, answers, False))
    res["checks"]["N.no_marker"] = BEGIN not in page_n and "algogebra" not in page_n.lower()
    nw, nn = normalise(page_w), normalise(page_n)
    res["checks"]["N.page_identical_outside_block"] = (nw == nn)
    if nw != nn:
        res["findings"].append("page differs outside the JSON block: " + first_diff(nw, nn))
    return res


# ----------------------------------------------------------------- report
def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--wims-home", default=os.environ.get("WIMS_HOME", str(DEFAULT_RT)))
    ap.add_argument("--oef2wims", default=os.environ.get("OEF2WIMS", str(DEFAULT_OEF2WIMS)))
    ap.add_argument("--sources", default=str(ROOT / "tools/oef-validation/html/exemples/sources"))
    ap.add_argument("--only", nargs="*", default=None, help="exercise stems to run")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--qnum", type=int, default=1, help="questions per series (>1 probes intermediate grade)")
    ap.add_argument("--out", default=str(Path(__file__).with_name("out_corpus_report_CLAUDE_V1")))
    a = ap.parse_args()

    global QNUM
    QNUM = a.qnum
    rt, oef2wims = Path(a.wims_home), Path(a.oef2wims)
    cgi = rt / "public_html" / "wims.cgi.exe"
    assert cgi.is_file(), cgi
    assert oef2wims.is_file(), oef2wims
    mod = rt / "public_html" / "modules" / "H6" / "test" / "oef2html.fr"
    if not mod.exists():
        import shutil
        shutil.copytree(rt / "public_html" / "modules" / "template" / "oef.fr", mod)
    make_writable(mod)
    (mod / "def").mkdir(exist_ok=True)

    oefs = sorted(Path(a.sources).glob("*.oef"))
    if a.only:
        oefs = [o for o in oefs if o.stem in set(a.only)]
    results = []
    for oef in oefs:
        t0 = time.time()
        try:
            r = review_exercise(cgi, rt, oef2wims, mod, oef, a.seed)
        except Exception as e:  # infra failure: keep going, report it
            r = {"exo": oef.stem, "checks": {}, "findings": [], "errors": ["exception: %r" % e]}
        r["seconds"] = round(time.time() - t0, 1)
        results.append(r)
        flags = " ".join("%s=%s" % (k.split(".", 1)[1], "ok" if v else ("?" if v is None else "FAIL"))
                         for k, v in r["checks"].items())
        print("%-14s %5.1fs  %s%s" % (r["exo"], r["seconds"], flags,
                                       ("  ERR " + "; ".join(r["errors"])) if r["errors"] else ""))
        for f in r["findings"]:
            print("    - " + f)

    out = Path(a.out)
    out.with_suffix(".json").write_text(json.dumps(results, indent=1, ensure_ascii=False), encoding="utf-8")
    lines = ["<!-- CLAUDE_V1 -->", "# AlgoGebra JSON hook — corpus review (Windows runtime)", "",
             "| exo | types | W marker | JSON | n==rows | grade==html | correct==html | given==sent | ASCII | replay 1.0 | replay ;-trim | no leak | page== | findings |",
             "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|"]
    sym = lambda v: "ok" if v else ("?" if v is None else "**FAIL**")
    for r in results:
        c = r["checks"]
        types = ",".join(sorted(set(r.get("def", {}).get("types", {}).values()))) if r.get("def") else "-"
        lines.append("| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
            r["exo"], types, sym(c.get("W.marker")), sym(c.get("W.json_valid")), sym(c.get("W.count_matches_html_rows")),
            sym(c.get("W.grade_matches_html")), sym(c.get("W.correct_matches_html")), sym(c.get("W.given_matches_sent")),
            sym(c.get("E.block_ascii")), sym(c.get("R.expected_replays_to_1")),
            ("-" if "R.replays_after_semicolon_trim" not in c else sym(c["R.replays_after_semicolon_trim"])),
            sym(c.get("N.no_marker")),
            sym(c.get("N.page_identical_outside_block")),
            "<br>".join(htmlmod.escape(x) for x in (r["findings"] + r["errors"])) or ""))
    out.with_suffix(".md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\nreport: %s.{md,json}" % out)
    hard = sum(1 for r in results if r["errors"])
    return 1 if hard else 0


if __name__ == "__main__":
    sys.exit(main())
