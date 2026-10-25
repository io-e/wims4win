#!/bin/bash
# Vérification légère (CI) : compile oef2wims Linux + Windows, v2/log, trace ; puis compile les
# OEF d'exemple avec oef2wims Linux et compare code retour + sha256 du .def à ci_ref.tsv.
# Usage : bash ci.sh [--update]
set -e
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../.." && pwd); OUT=${OUT:-$REPO/build/oef-validation}; export OUT
bash "$H/oef2wims/build.sh"; bash "$H/v2/build.sh"; bash "$H/trace/build_trace.sh"
python3 -m py_compile "$H/html/oef2html.py" "$H/llm/oef_llm_fix.py" "$H/html/compare_html.py" "$H/trace/mksym.py"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT; : > "$T/res.tsv"
for f in "$H"/html/exemples/sources/*.oef "$H"/corpus/erreurs/*.oef "$H"/exemples/exemples_v2/*.oef; do
  b=$(basename "$f" .oef); cp "$f" "$T/x.oef"; rm -f "$T/x.def"
  set +e; (cd "$T" && "$OUT/linux/oef2wims" x.oef x.def >/dev/null 2>&1); rc=$?; set -e
  h=-; [ -f "$T/x.def" ] && h=$(sha256sum "$T/x.def" | cut -c1-16)
  printf '%s\t%s\t%s\n' "$b" "$rc" "$h" >> "$T/res.tsv"
done
if [ "$1" = "--update" ]; then cp "$T/res.tsv" "$H/ci_ref.tsv"; echo "ci_ref.tsv mis à jour"; exit 0; fi
diff "$H/ci_ref.tsv" "$T/res.tsv" && echo "OK : $(wc -l < "$H/ci_ref.tsv") OEF identiques à la référence"
