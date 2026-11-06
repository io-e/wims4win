#!/bin/bash
# Remove the AlgoGebra JSON hook from a WIMS runtime tree.
# Prefers restoring answer.phtml from answer.phtml.bak_pre_algogebra;
# otherwise strips the injected block. Deletes algogebra_json.phtml.
#
# Usage: uninstall-algogebra-hook.sh <wims_root>
#
# Copyright (C) 2026 wims4win contributors. GNU GPL v2 (see NOTICE.md).
set -e
W=${1:?usage: uninstall-algogebra-hook.sh <wims_root>}
OEF=$W/public_html/scripts/oef
A=$OEF/answer.phtml
[ -f "$A" ] || { echo "missing answer.phtml: $A" >&2; exit 1; }
if [ -f "$A.bak_pre_algogebra" ]; then
  cp -p "$A.bak_pre_algogebra" "$A"
  echo "restored $A from answer.phtml.bak_pre_algogebra"
else
  python3 - "$A" <<'PY'
import re, sys
p = sys.argv[1]
t = open(p, "rb").read()
pat = re.compile(
    br"!! GROK_V1 AlgoGebra JSON hook \(special_parm=algogebra_json\)\r?\n"
    br"!if \$special_parm=algogebra_json\r?\n"
    br"<!-- ALGOGEBRA_JSON_BEGIN\r?\n"
    br"!read oef/algogebra_json.phtml\r?\n"
    br"ALGOGEBRA_JSON_END -->\r?\n"
    br"!endif\r?\n"
)
n, c = pat.subn(b"", t, count=1)
if c != 1 and b"GROK_V1 AlgoGebra JSON hook" in t:
    sys.exit("could not strip hook block from answer.phtml")
open(p, "wb").write(n)
print("stripped hook block from", p)
PY
fi
rm -f "$OEF/algogebra_json.phtml"
echo "removed $OEF/algogebra_json.phtml"
