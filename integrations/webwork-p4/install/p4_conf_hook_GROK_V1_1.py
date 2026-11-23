#!/usr/bin/env python3
"""GROK_V1_1 P4 config hook (I3): run it AFTER the WeBWorK config generator, every time the generator runs
(Debian: start_debian_mariadb.sh runs Apply_WebWork4Win_Conf_GROK_V1.py at each start and rewrites
localOverrides.conf and pg_config.yml from their .dist files, which removes hand-added lines).
Idempotent: adds the P4 lines once, between markers, and never removes or reorders anything else.
  - $WEBWORK_ROOT/conf/localOverrides.conf: `push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)];` before the final `1;`
    (APPEND to $pg{modules}; under webwork2 defaults.config's $pg{modules} is what PG uses);
  - $PG_ROOT/conf/pg_config.yml: `- ['WeBWorK::OEFBridge']` as first item of `modules:` (PG runs without webwork2).
Usage: p4_conf_hook_GROK_V1_1.py [--webwork-root DIR] [--pg-root DIR] [--check]
  (defaults: $WEBWORK_ROOT, $PG_ROOT or <webwork-root>/pg). --check: exit 1 if a line is missing, change nothing.
Python stdlib only. Same script on Windows (paths from the environment of the webwork4win start script)."""
import argparse, os, re, sys
from pathlib import Path

LO_LINE = "push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)]; # GROK_V1 P4 trusted module"
LO_BLOCK = f"# BEGIN GROK_V1_1 P4 (p4_conf_hook_GROK_V1_1.py)\n{LO_LINE}\n# END GROK_V1_1 P4\n"
PG_ITEM = "  - ['WeBWorK::OEFBridge'] # GROK_V1 P4 trusted module"

ap = argparse.ArgumentParser()
ap.add_argument("--webwork-root", default=os.environ.get("WEBWORK_ROOT"))
ap.add_argument("--pg-root", default=os.environ.get("PG_ROOT"))
ap.add_argument("--check", action="store_true")
a = ap.parse_args()
if not a.webwork_root:
    sys.exit("set --webwork-root or WEBWORK_ROOT")
ww = Path(a.webwork_root); pg = Path(a.pg_root) if a.pg_root else ww / "pg"
lo, pc = ww / "conf/localOverrides.conf", pg / "conf/pg_config.yml"
for f in (lo, pc):
    if not f.is_file():
        sys.exit(f"missing {f} (run the config generator first)")
status = {}
t = lo.read_text(encoding="utf-8")
if "WeBWorK::OEFBridge" in t:
    status["localOverrides.conf"] = "present"
elif a.check:
    status["localOverrides.conf"] = "MISSING"
else:
    m = list(re.finditer(r"(?m)^\s*1;.*$", t))
    t = (t[:m[-1].start()] + LO_BLOCK + "\n" + t[m[-1].start():]) if m else (t.rstrip("\n") + "\n" + LO_BLOCK + "1;\n")
    lo.write_text(t, encoding="utf-8", newline="\n"); status["localOverrides.conf"] = "added"
t = pc.read_text(encoding="utf-8")
if "WeBWorK::OEFBridge" in t:
    status["pg_config.yml"] = "present"
elif a.check:
    status["pg_config.yml"] = "MISSING"
else:
    m = re.search(r"(?m)^modules:[ \t]*\n", t)
    if not m:
        sys.exit(f"no 'modules:' key in {pc}")
    t = t[:m.end()] + PG_ITEM + "\n" + t[m.end():]
    pc.write_text(t, encoding="utf-8", newline="\n"); status["pg_config.yml"] = "added"
print(" ".join(f"{k}={v}" for k, v in status.items()))
sys.exit(1 if "MISSING" in status.values() else 0)
