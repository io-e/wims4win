#!/bin/bash
# GROK_V1 — install AlgoGebra JSON hook into a WIMS runtime tree (Linux/macOS/Git Bash).
# Usage:
#   bash AlgoGebra_Wims_Integration/runtime_patch/install_algogebra_hook_GROK_V1.sh [RUNTIME_ROOT]
# Default RUNTIME_ROOT: first arg, or $WIMS_HOME, or ./wims-runtime
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=${1:-${WIMS_HOME:-}}
if [ -z "${ROOT}" ]; then
  echo "usage: $0 /path/to/wims-runtime" >&2
  exit 2
fi
OEF="$ROOT/public_html/scripts/oef"
ANSWER="$OEF/answer.phtml"
HOOK_SRC="$HERE/algogebra_json.phtml"
HOOK_DST="$OEF/algogebra_json.phtml"
MARKER='GROK_V1 AlgoGebra JSON hook'

[ -f "$ANSWER" ] || { echo "answer.phtml not found: $ANSWER" >&2; exit 1; }
[ -f "$HOOK_SRC" ] || { echo "missing $HOOK_SRC" >&2; exit 1; }

cp "$HOOK_SRC" "$HOOK_DST"
echo "Installed $HOOK_DST"

if grep -qF "$MARKER" "$ANSWER"; then
  echo "answer.phtml already contains AlgoGebra hook"
  exit 0
fi

BAK="${ANSWER}.bak_pre_algogebra"
[ -f "$BAK" ] || cp "$ANSWER" "$BAK"

python3 - "$ANSWER" <<'PY'
import pathlib, re, sys
path = pathlib.Path(sys.argv[1])
text = path.read_text(encoding="latin-1")
pattern = re.compile(
    r"(!!end of \.answer_analysis\r?\n</div>\r?\n)(!!presentsol=1 deprecated)",
    re.M,
)
inject = (
    r"\1!! GROK_V1 AlgoGebra JSON hook (special_parm=algogebra_json)\n"
    r"!if $special_parm=algogebra_json\n"
    r"<!-- ALGOGEBRA_JSON_BEGIN\n"
    r"!read oef/algogebra_json.phtml\n"
    r"ALGOGEBRA_JSON_END -->\n"
    r"!endif\n"
    r"\2"
)
new, n = pattern.subn(inject, text, count=1)
if n != 1:
    sys.stderr.write("injection pattern did not match in answer.phtml\n")
    sys.exit(1)
path.write_text(new, encoding="latin-1", newline="")
print(f"Patched {path}")
PY

echo "Done."
