#!/usr/bin/env python3
# GROK_V1 P4 copy (P9): copied read-only from LW_WEBWORK's webwork4win-mariadb; only the default paths changed to
# /workspace/mw-p4-20261123/ww (own instance). LW's original is untouched.
# GROK_V1_1 (I7): WW_COURSES and WEBWORK_ROOT must be set explicitly (no silent builder default); the paths used are
# printed on stderr at start.
"""MARIADB_1 multi-seed grading baseline (reproducible).

Sample: from the course's BPL_all set, sorted by path, take N problems per collection
(BdeB, CCDMD, CCDMD-EN, Mathema-TIC) at an even stride. Seeds: 1234 1 42 2026 31415.
For each problem and seed the worker reads correct_ans, submits it (must score 100%),
then submits a perturbed copy (must score < 100%). When some correct answers are a single
decimal number, it also submits them with a decimal comma (score_comma; 1 means accepted). Output: CSV, one row per problem x seed.

Gate: compare a new CSV with the stored baseline using --compare BASELINE.csv; any row whose
status or scores changed is a regression to explain. Exit code 1 when rows differ.
"""
import argparse, csv, json, os, subprocess, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
for _v in ("WW_COURSES", "WEBWORK_ROOT"):
    if not os.environ.get(_v):
        sys.exit(f"Grade_Baseline: set {_v} (I7: no default; e.g. WW_COURSES=<webwork courses dir>, WEBWORK_ROOT=<webwork2 dir>)")
COURSES = Path(os.environ["WW_COURSES"])
print(f"Grade_Baseline: WW_COURSES={COURSES} WEBWORK_ROOT={os.environ['WEBWORK_ROOT']} "
      f"PG_ROOT={os.environ.get('PG_ROOT', os.environ['WEBWORK_ROOT'] + '/pg')}", file=sys.stderr)
COLLECTIONS = ["BdeB", "CCDMD", "CCDMD-EN", "Mathema-TIC"]
SEEDS = [1234, 1, 42, 2026, 31415]
GATE = ["status", "score_correct", "score_perturbed"]  # add "score_comma" with --gate-comma
FIELDS = ["path", "seed", "status", "n_answers", "score_correct", "score_perturbed", "n_decimal_blanks", "score_comma", "failing_blanks", "correct", "perturbed", "detail"]


def sample(course: str, set_file: str, per: int) -> list[str]:
    paths = []
    for line in (COURSES / course / "templates" / set_file).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("source_file") and "=" in line:
            paths.append(line.split("=", 1)[1].strip())
    paths = sorted(set(paths))
    out = []
    for col in COLLECTIONS:
        group = [p for p in paths if p.split("/")[1:2] == [col]]
        if not group:
            continue
        stride = max(1, len(group) // per)
        out += group[::stride][:per]
    return out


def run(course: str, problems: list[str], chunk: int) -> list[dict]:
    rows = []
    jobs = [(p, s) for p in problems for s in SEEDS]
    env = os.environ.copy()
    for start in range(0, len(jobs), chunk):  # fresh worker per chunk to bound memory
        part = jobs[start:start + chunk]
        stdin = "".join(f"{course}\t{p}\t{s}\n" for p, s in part)
        proc = subprocess.run(["perl", str(HERE / "Grade_Baseline_Worker_GROK_V1.pl")], input=stdin,
                              capture_output=True, text=True, env=env, timeout=3600)
        got = [json.loads(l) for l in proc.stdout.splitlines() if l.startswith("{")]
        if len(got) != len(part):
            print(f"chunk {start}: expected {len(part)} got {len(got)}; stderr tail: {proc.stderr[-400:]}", file=sys.stderr)
        rows += got
        print(f"{min(start + chunk, len(jobs))}/{len(jobs)}", flush=True)
    return rows


def key(r):
    return (r["path"], str(r["seed"]))


def compare(new_csv: Path, base_csv: Path) -> int:
    load = lambda f: {key(r): r for r in csv.DictReader(open(f, encoding="utf-8"))}
    a, b = load(base_csv), load(new_csv)
    diffs = []
    for k in sorted(set(a) | set(b)):
        ra, rb = a.get(k), b.get(k)
        if not ra or not rb:
            diffs.append((k, "missing", ra and ra["status"], rb and rb["status"]))
        elif tuple(ra.get(c, "") for c in GATE) != tuple(rb.get(c, "") for c in GATE):
            diffs.append((k, "changed", f'{ra["status"]} {ra["score_correct"]}/{ra["score_perturbed"]}', f'{rb["status"]} {rb["score_correct"]}/{rb["score_perturbed"]}'))
    for d in diffs:
        print("\t".join(map(str, d)))
    print(f"rows {len(b)} vs baseline {len(a)}; differing {len(diffs)}")
    return 1 if diffs else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--course", default="bpltest")
    ap.add_argument("--set-file", default="setBPL_all.def")
    ap.add_argument("--per-collection", type=int, default=25)
    ap.add_argument("--chunk", type=int, default=50)
    ap.add_argument("--out", default=str(HERE / "tmp" / "grade_baseline.csv"))
    ap.add_argument("--compare", default="", help="Baseline CSV to compare --out against (no run if --no-run).")
    ap.add_argument("--no-run", action="store_true")
    ap.add_argument("--gate-comma", action="store_true", help="Also require score_comma to match the baseline.")
    ap.add_argument("--extra-list", default="", help="Text file of extra BPL/... paths appended to the sample (e.g. the 6 non-macro failures).")
    a = ap.parse_args()
    out = Path(a.out)
    if not a.no_run:
        problems = sample(a.course, a.set_file, a.per_collection)
        if a.extra_list:
            problems += [l.strip() for l in open(a.extra_list, encoding="utf-8") if l.strip() and l.strip() not in problems]
        (out.parent / (out.stem + "_sample.txt")).write_text("\n".join(problems) + "\n", encoding="utf-8")
        rows = run(a.course, problems, a.chunk)
        with open(out, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=FIELDS, extrasaction="ignore")
            w.writeheader()
            for r in sorted(rows, key=lambda r: (r["path"], int(r["seed"]))):
                w.writerow(r)
        counts = {}
        for r in rows:
            counts[r["status"]] = counts.get(r["status"], 0) + 1
        print("summary", json.dumps(counts, sort_keys=True))
    if a.gate_comma:
        GATE.append("score_comma")
    return compare(out, Path(a.compare)) if a.compare else 0


if __name__ == "__main__":
    sys.exit(main())
