#!/bin/bash
# Install the AlgoGebra JSON hook into a WIMS runtime tree (Linux or
# packaged Windows tree on a POSIX host). Idempotent.
# Keeps public_html/scripts/oef/answer.phtml.bak_pre_algogebra for uninstall.
#
# Usage: install-algogebra-hook.sh <wims_root> [hook_dir]
#   hook_dir defaults to the directory of this script.
#
# Copyright (C) 2026 wims4win contributors. GNU GPL v2 (see NOTICE.md).
set -e
W=${1:?usage: install-algogebra-hook.sh <wims_root> [hook_dir]}
P=${2:-$(cd "$(dirname "$0")" && pwd)}
OEF=$W/public_html/scripts/oef
A=$OEF/answer.phtml
[ -f "$A" ] || { echo "missing answer.phtml: $A" >&2; exit 1; }
[ -f "$P/algogebra_json.phtml" ] || { echo "missing hook: $P/algogebra_json.phtml" >&2; exit 1; }
cp "$P/algogebra_json.phtml" "$OEF/algogebra_json.phtml"
if grep -q 'GROK_V1 AlgoGebra JSON hook' "$A"; then
  echo "already patched $A"
  exit 0
fi
[ -f "$A.bak_pre_algogebra" ] || cp -p "$A" "$A.bak_pre_algogebra"
python3 - "$A" <<'PY'
import re, sys
p = sys.argv[1]
t = open(p, "rb").read()
pat = re.compile(rb"(!!end of \.answer_analysis\r?\n</div>\r?\n)(!!presentsol=1 deprecated)")
nl = b"\r\n" if b"\r\n" in t else b"\n"
ins = nl.join([
    b"!! GROK_V1 AlgoGebra JSON hook (special_parm=algogebra_json)",
    b"!if $special_parm=algogebra_json",
    b"<!-- ALGOGEBRA_JSON_BEGIN",
    b"!read oef/algogebra_json.phtml",
    b"ALGOGEBRA_JSON_END -->",
    b"!endif",
]) + nl
n, c = pat.subn(lambda m: m.group(1) + ins + m.group(2), t, count=1)
if c != 1:
    sys.exit("injection pattern did not match in answer.phtml")
open(p, "wb").write(n)
PY
echo "patched $A"
