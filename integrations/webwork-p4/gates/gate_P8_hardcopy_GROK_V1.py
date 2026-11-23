#!/usr/bin/env python3
"""GROK_V1 P4 gate P8 (hardcopy). WeBWorK PDF hardcopy of set P4pilot for stud1 (only wrong answers submitted):
(a) as stud1, (b) as instructor p4prof for stud1 with correct answers / solutions / hints requested.
Checks: HTTP 200 + a PDF, the 10 statements present (a key phrase of each, from the worker render for stud1's seed),
no answer inputs, no expected value (boundary search; 1-character values are not grep-able in a PDF of numbers and
are reported, not judged).
V1_1: (D1) an expected value that is printed by the native WIMS statement itself for the same seed is exercise data,
not a leak (reported as visible_in_statement); (P8 HOLD) no "[figure]" anywhere, and every number written inside a
formula of the statement (worker statement_tex, WIMS MathML -> TeX) is in that problem's PDF text.
Writes p4_results/P8_hardcopy.json and P8_hardcopy_*.pdf/.txt (no secrets inside)."""
import json, re, subprocess, sys, html as H
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import *  # noqa: settings (env-overridable)
sys.path.insert(0, str(Path(__file__).parent))
from ww_client_GROK_V1 import WW, BASE
from oracle_GROK_V1 import PILOT, expected, db, P4
from oefc_GROK_V1 import call
from gate_P2_noleak_GROK_V1 import hits


def pdf_of(w, extra):
    form = {"user": w.user, "effectiveUser": w.user, "in_hc_form": "1", "selected_sets": "P4pilot",
            "selected_users": "stud1", "hardcopy_format": "pdf", "generate_hardcopy": "1", **extra}
    import urllib.request, urllib.parse
    req = urllib.request.Request(f"{BASE}/p4pilot/hardcopy/", urllib.parse.urlencode(form).encode())
    req.add_header("Cookie", "; ".join(f"{k}={v}" for k, v in w.cookies.items()))
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.status, r.headers.get("Content-Type"), r.read()


def columns_text(pdf):
    """Text of each page, left half then right half (pdftotext's reading order interleaves the two columns)."""
    info = subprocess.run(["pdfinfo", str(pdf)], capture_output=True, text=True).stdout
    pages = int(re.search(r"Pages:\s+(\d+)", info).group(1))
    w, h = [float(x) for x in re.search(r"Page size:\s+([\d.]+) x ([\d.]+)", info).groups()]
    out = []
    for p in range(1, pages + 1):
        for x0 in (0, w / 2):
            out.append(subprocess.run(["pdftotext", "-r", "72", "-f", str(p), "-l", str(p), "-x", str(int(x0)), "-y", "0",
                                       "-W", str(int(w / 2) + 1), "-H", str(int(h) + 1), str(pdf), "-"],
                                      capture_output=True, text=True).stdout)
    return "\n".join(out)


def key_phrase(st):
    t = H.unescape(re.sub(r"<[^>]+>", " ", re.sub(r"\{\{P4:\w+\}\}", " ", st)))
    t = re.sub(r"\\\(.*?\\\)", " ", t, flags=re.S)  # V1_1: formulas are TeX \(...\) (MathJax); key words from the text only
    words = [x for x in re.findall(r"[A-Za-zÀ-ÿ']{4,}", t)]
    return words[:4]


def main():
    seeds = {r["problem_id"]: int(r["problem_seed"]) for r in
             db("select problem_id,problem_seed from p4pilot_problem_user where set_id='P4pilot' and user_id='stud1'")}
    res, ok = {"runs": []}, True
    runs = [("stud1", WW("p4pilot", "stud1", "ww_student_password"), {"printStudentAnswers": "0"}),
            ("p4prof_for_stud1", WW("p4pilot", "p4prof", "ww_prof_password"),
             {"showCorrectAnswers": "1", "showSolutions": "1", "showHints": "1", "printStudentAnswers": "0"})]
    for label, w, extra in runs:
        code, ctype, body = pdf_of(w, extra)
        pdf = RESULTS / f"P8_hardcopy_{label}.pdf"
        is_pdf = body[:5] == b"%PDF-"
        run = {"who": label, "http": code, "content_type": ctype, "is_pdf": is_pdf, "bytes": len(body), "problems": []}
        if is_pdf:
            pdf.write_bytes(body)
            txt = columns_text(pdf)  # V1_1: left column then right column of each page (WeBWorK prints 2 columns)
            (RESULTS / f"P8_hardcopy_{label}.txt").write_text(txt)
            import unicodedata
            norm = unicodedata.normalize("NFKC", txt).replace("\u2019", "'")
            norm = re.sub(r"-\n", "", norm)
            flat = re.sub(r"\s+", " ", norm)
            seg = {}
            parts = re.split(r"Problem (\d+)\. \(\d+ points?\)", flat)
            for k in range(1, len(parts) - 1, 2):
                seg[int(parts[k])] = parts[k + 1]
            run["pdf_pages"] = int(re.search(r"Pages:\s+(\d+)", subprocess.run(["pdfinfo", str(pdf)], capture_output=True, text=True).stdout).group(1))
            run["acroform_fields"] = body.count(b"/AcroForm")
            run["oef_error_in_pdf"] = "OEF:" in flat
            for i, (m, e) in enumerate(PILOT, 1):
                rj = call("/oef/render", {"module": m, "exo": e, "seed": seeds[str(i)]})[1]
                st = rj["statement_html"]
                st_txt = H.unescape(re.sub(r"<[^>]+>", " ", st))
                math_nums = sorted({n for f in re.findall(r"\\\((.*?)\\\)", rj.get("statement_tex", ""), re.S)
                                    for n in re.findall(r"\d+(?:\.\d+)?", f)})
                part_digits = re.sub(r"\s+", "", seg.get(i, ""))
                math_missing = [n for n in math_nums if n not in part_digits]
                kp = key_phrase(st)
                part = seg.get(i, "")
                present = bool(part) and all(k in part for k in kp)
                exp = expected(m, e, seeds[str(i)])
                leak, unjudged, visible = [], [], []
                for fid, v in exp.items():
                    if fid.startswith("choice"):
                        continue
                    if len(v.strip()) <= 1:
                        unjudged.append(fid); continue
                    if hits(part, v):  # searched in this problem's own text (other problems print other numbers)
                        (visible if hits(st_txt, v) else leak).append(fid)  # D1: printed by WIMS's own statement
                run["problems"].append({"n": i, "exo": e, "statement_key_words": kp, "statement_present": present,
                                        "expected_found": leak, "visible_in_statement": visible, "not_judged_1char": unjudged,
                                        "math_numbers": math_nums, "math_numbers_missing": math_missing,
                                        "figure_placeholder": "[figure]" in part})
            run["figure_anywhere"] = "[figure]" in flat
            run["pass"] = (code == 200 and all(p["statement_present"] and not p["expected_found"] and not p["math_numbers_missing"]
                                               and not p["figure_placeholder"] for p in run["problems"]) and not run["figure_anywhere"]
                           and run["acroform_fields"] == 0 and not run["oef_error_in_pdf"])
        else:
            run["body_head"] = body[:300].decode("utf-8", "replace")
            run["pass"] = False
        ok &= run["pass"]
        res["runs"].append(run)
        print(json.dumps(run, ensure_ascii=False)[:1500])
    res["pass"] = ok
    (RESULTS / "P8_hardcopy.json").write_text(json.dumps(res, indent=1, ensure_ascii=False))
    print("P8", "PASS" if ok else "FAIL")


if __name__ == "__main__":
    main()
