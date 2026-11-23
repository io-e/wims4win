#!/usr/bin/env python3
# GROK_V1_1 P4
"""P4 sibling of the WeBWorK config generator (Windows webwork4win AND Debian webwork4win-mariadb). New file: the
existing generator Apply_WebWork4Win_Conf_GROK_V1.py is NOT edited. Put this file next to it and call it INSTEAD of
it (same arguments, e.g. --links). It loads the existing generator, runs its main() unchanged, and makes its two
writers also emit the P4 lines, so a regeneration never removes them:
  - write_overrides -> localOverrides.conf gets, before the final `1;`, between markers:
        push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)];      (APPEND to $pg{modules}; never replace it)
  - write_pg        -> pg_config.yml gets `- ['WeBWorK::OEFBridge']` as first item of `modules:`.
Paths are the generator's own (SRC, PG constants: D:\\... on Windows, env-driven on Debian).
Base generator: P4_BASE_GENERATOR=<path> or Apply_WebWork4Win_Conf_GROK_V1.py in this file's directory.
Not covered (not config files): PERL5LIB must also hold the P4 site lib dir and OEF_BRIDGE_CONF must be exported in
the start script environment (see INSTALL.md 3.4). Python stdlib only."""
from __future__ import annotations

import importlib.util
import os
import re
import sys
from pathlib import Path

LO_LINE = "push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)]; # GROK_V1 P4 trusted module"
LO_BLOCK = f"# BEGIN GROK_V1_1 P4 (Apply_WebWork4Win_Conf_P4_GROK_V1_1.py)\n{LO_LINE}\n# END GROK_V1_1 P4\n"
PG_ITEM = "  - ['WeBWorK::OEFBridge'] # GROK_V1 P4 trusted module"


def add_overrides(path: Path) -> str:
    t = path.read_text(encoding="utf-8")
    if "WeBWorK::OEFBridge" in t:
        return "present"
    m = list(re.finditer(r"(?m)^\s*1;.*$", t))
    if not m:
        raise SystemExit(f"{path}: no final true value")
    t = t[:m[-1].start()] + LO_BLOCK + "\n" + t[m[-1].start():]
    path.write_text(t, encoding="utf-8", newline="\n")
    return "added"


def add_pg(path: Path) -> str:
    t = path.read_text(encoding="utf-8")
    if "WeBWorK::OEFBridge" in t:
        return "present"
    m = re.search(r"(?m)^modules:[ \t]*\n", t)
    if not m:
        raise SystemExit(f"no 'modules:' key in {path}")
    t = t[:m.end()] + PG_ITEM + "\n" + t[m.end():]
    path.write_text(t, encoding="utf-8", newline="\n")
    return "added"


def load_base():
    p = Path(os.environ.get("P4_BASE_GENERATOR", Path(__file__).resolve().with_name("Apply_WebWork4Win_Conf_GROK_V1.py")))
    if not p.is_file():
        raise SystemExit(f"base generator not found: {p} (set P4_BASE_GENERATOR)")
    spec = importlib.util.spec_from_file_location("webwork4win_base_generator", p)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    for name in ("main", "write_overrides", "write_pg", "SRC", "PG"):
        if not hasattr(mod, name):
            raise SystemExit(f"base generator {p} has no {name}: update this sibling")
    return mod


def patch(base) -> None:
    orig_o, orig_p = base.write_overrides, base.write_pg

    def write_overrides(*a, **k):
        orig_o(*a, **k)
        print("P4 localOverrides.conf:", add_overrides(Path(base.SRC) / "conf" / "localOverrides.conf"))

    def write_pg(*a, **k):
        orig_p(*a, **k)
        print("P4 pg_config.yml:", add_pg(Path(base.PG) / "conf" / "pg_config.yml"))

    base.write_overrides, base.write_pg = write_overrides, write_pg  # main() looks them up in its module globals


def main() -> None:
    base = load_base()
    patch(base)
    base.main()


if __name__ == "__main__":
    main()
