# GROK_V1
"""GB-W7/W8 report probes for AlgoGebra JSON hook (Windows CGI).

Owner report requirements:
- invalid-answer: distinguish WIMS retry/incomplete UI from a missing hook
  (no new JSON status field)
- encoding: page charset + JSON decode; accented answer best-effort
- grade type: observed JSON numbers 0 and 1
- given: WIMS-normalized (!singlespace) — not exact keystrokes
- wimslogd: record stopped; do not restart
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
sys.path.insert(0, str(Path(__file__).resolve().parent))
from test_algogebra_json_GROK_V1 import (  # noqa: E402
    BEGIN,
    DEFAULT_OEF2WIMS,
    DEFAULT_RT,
    MODULE,
    END,
    exercise,
    extract_json,
    install_def,
    run_cgi,
    set_exindex,
    set_seed,
)


def page_charset(raw: bytes) -> str:
    m = re.search(rb"charset=([\w-]+)", raw[:800], re.I)
    return m.group(1).decode() if m else "unknown"


def run_cgi_raw(cgi: Path, home: Path, query: str) -> bytes:
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
        HTTP_USER_AGENT="probe_gbot_p1",
    )
    p = subprocess.run(
        [str(cgi)],
        cwd=str(home / "public_html"),
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=120,
    )
    return p.stdout


def wimslogd_running() -> bool:
    if os.name == "nt":
        p = subprocess.run(
            ["tasklist", "/FI", "IMAGENAME eq wimslogd.exe"],
            capture_output=True,
        )
        out = p.stdout.decode("utf-8", "replace") + p.stderr.decode("utf-8", "replace")
        return "wimslogd.exe" in out
    p = subprocess.run(["pgrep", "-a", "wimslogd"], capture_output=True)
    out = p.stdout.decode("utf-8", "replace")
    return p.returncode == 0 and bool(out.strip())


def classify_reply_html(html: str) -> dict:
    has_json = BEGIN in html and END in html
    has_analysis = "answer_analysis" in html
    sure = bool(
        re.search(r"Êtes-vous\s+sûr|Etes-vous\s+sur|exo_formCheck", html, re.I)
    )
    return {
        "json_block_present": has_json,
        "answer_analysis_present": has_analysis,
        "sure_or_incomplete_ui": sure,
    }


def main() -> int:
    rt = Path(os.environ.get("WIMS_HOME", DEFAULT_RT))
    cgi = rt / "public_html" / "wims.cgi.exe"
    oef2wims = Path(os.environ.get("OEF2WIMS", DEFAULT_OEF2WIMS))
    report: dict = {"host": "windows-cgi-direct", "runtime": str(rt)}

    report["wimslogd_running"] = wimslogd_running()
    report["wimslogd_policy"] = (
        "STOPPED — recorded; do not restart unless owner confirms needed"
        if not report["wimslogd_running"]
        else "RUNNING — leave as found; this probe does not start/stop wimslogd"
    )

    mod = rt / "public_html" / "modules" / "H6" / "test" / "oef2html.fr"
    sources = ROOT / "tools" / "oef-validation" / "html" / "exemples" / "sources"
    for name in ("00006", "00076"):
        install_def(oef2wims, sources / ("%s.oef" % name), mod / "def")
    set_seed(mod / "var.proc", 1)

    set_exindex(mod, "00006")
    raw_new = run_cgi_raw(
        cgi,
        rt,
        "module=%s&cmd=new&exo=00006&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % MODULE,
    )
    charset = page_charset(raw_new)
    report["page_charset"] = charset
    report["json_decode"] = (
        "1) Decode CGI body with Content-Type charset (observed windows-1252). "
        "2) Locate <!-- ALGOGEBRA_JSON_BEGIN ... ALGOGEBRA_JSON_END -->. "
        "3) json.loads the inner text (ASCII keys/numbers; strings may use \\u escapes)."
    )

    d0 = exercise(cgi, rt, "00006", {"reply1": "0"}, 0.0, mod_dir=mod)
    d1 = exercise(cgi, rt, "00006", {"reply1": "2"}, 1.0, mod_dir=mod)
    report["grade_wrong"] = {
        "python_type": type(d0["grade"]).__name__,
        "value": d0["grade"],
        "json_fragment": json.dumps({"grade": d0["grade"]}),
    }
    report["grade_right"] = {
        "python_type": type(d1["grade"]).__name__,
        "value": d1["grade"],
        "json_fragment": json.dumps({"grade": d1["grade"]}),
    }
    report["grade_client_rule"] = (
        "Preserve observed integer values 0 and 1. Client must accept JSON "
        "numbers (int or float) — do not require decimal literals (0.0) and do "
        "not require string \"0\"/\"1\"."
    )
    assert d0["grade"] == 0 and d1["grade"] == 1
    assert isinstance(d0["grade"], int) and isinstance(d1["grade"], int)

    d_sp = exercise(cgi, rt, "00006", {"reply1": "  2  "}, 1.0, mod_dir=mod)
    given = d_sp["answers"][0]["given"]
    report["given"] = {
        "student_typed": "  2  ",
        "json_given": given,
        "rule": (
            "WIMS normalizes via !singlespace (and escape passes) before emit. "
            "Do not present answers[].given as the exact text the student typed."
        ),
    }
    assert given == "2"

    # Invalid / non-numeric with special_parm — often incomplete/"sure" UI, no JSON
    set_exindex(mod, "00006")
    page = run_cgi(
        cgi,
        rt,
        "module=%s&cmd=new&exo=00006&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % MODULE,
    )
    sm = re.search(r'name="session" value="([^"]+)"', page)
    raw_bad = run_cgi_raw(
        cgi,
        rt,
        "session=%s&cmd=reply&module=%s&special_parm=algogebra_json&reply1=%s"
        % (sm.group(1), MODULE, quote("not-a-number-xyz")),
    )
    html_bad = raw_bad.decode(
        charset if charset != "unknown" else "latin-1", "replace"
    )
    cls_bad = classify_reply_html(html_bad)

    # Missing-hook simulation: completed wrong numeric WITHOUT special_parm
    set_exindex(mod, "00006")
    page = run_cgi(
        cgi,
        rt,
        "module=%s&cmd=new&exo=00006&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % MODULE,
    )
    sm = re.search(r'name="session" value="([^"]+)"', page)
    html_no = run_cgi(
        cgi,
        rt,
        "session=%s&cmd=reply&module=%s&reply1=0" % (sm.group(1), MODULE),
    )
    cls_no = classify_reply_html(html_no)

    report["invalid_answer_vs_missing_hook"] = {
        "invalid_or_incomplete_with_special_parm": cls_bad,
        "graded_reply_without_special_parm": cls_no,
        "client_distinction_without_new_status": (
            "Do NOT add a JSON status field without approval. "
            "1) If ALGOGEBRA_JSON_BEGIN present → hook fired (use grade/answers). "
            "2) If absent and answer_analysis present → reply graded but hook "
            "missing/off (special_parm absent or not installed). "
            "3) If absent and sure/incomplete UI (e.g. h1 Êtes-vous sûr / "
            "exo_formCheck) → WIMS did not finish grading this POST; treat as "
            "retry/incomplete page, not as a missing-hook grade result."
        ),
        "no_new_status_block": True,
    }

    # Accented: same numeric exercise → typically incomplete UI; correct accented OEF unverified
    set_exindex(mod, "00006")
    page = run_cgi(
        cgi,
        rt,
        "module=%s&cmd=new&exo=00006&qnum=1&qcmlevel=1&intro_expert=yes"
        "&intro_sol=1&intro_good=1&intro_feed=1&lang=fr" % MODULE,
    )
    sm = re.search(r'name="session" value="([^"]+)"', page)
    # Send UTF-8 percent-encoding of été
    raw_acc = run_cgi_raw(
        cgi,
        rt,
        "session=%s&cmd=reply&module=%s&special_parm=algogebra_json&reply1=%s"
        % (sm.group(1), MODULE, quote("été")),
    )
    html_acc = raw_acc.decode(
        charset if charset != "unknown" else "latin-1", "replace"
    )
    cls_acc = classify_reply_html(html_acc)
    report["accented_probe"] = {
        "typed": "été",
        "query_encoding": "urllib.parse.quote UTF-8 percent-encoding",
        "page_charset": charset,
        "classification": cls_acc,
        "status": (
            "accented_correct_answer_UNVERIFIED — fixtures 00006/00076 are "
            "numeric/checkbox; non-numeric reply hits incomplete/sure UI and "
            "emits no JSON. Encoding path for successful JSON remains: decode "
            "page as windows-1252 then json.loads comment body."
        ),
    }

    report["suites_21_23"] = {
        "windows_this_run": "hook CGI probes only (not Playwright full cycle)",
        "live_debian": "DEFERRED to GBot box — run suites 21-23 vs Debian+hook; keep mocks green separately",
        "mock_baseline": "prior GBot GV residual: suite21/22 PASS on #40/#41; suite23 env-sensitive",
    }

    out = Path(
        os.environ.get(
            "PROBE_OUT",
            str(
                Path(__file__).resolve().parent
                / "out_probe_gbot_p1_report_GROK_V1.json"
            ),
        )
    )
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print("WROTE", out)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        print("FAIL:", e, file=sys.stderr)
        sys.exit(1)
