#!/bin/bash
# GROK_V1 - Linux (Debian route) build of a fresh WIMS copy for the adm/raw test.
# Same Linux steps as tools/build-runtime-tree.sh of io-e/wims4win master b21702a (copy svn r19040 export,
# ./compile --mathjax --geogebra, bin/mkindex, gen-theme-assets.sh, AlgoGebra hook), without the Windows packaging.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$HERE/wims-master-b21702a
WD=$HERE/build; mkdir -p "$WD/wims"; cd "$WD/wims"
cp -a /workspace/svn-r19040/. .
find . -type f ! -name '*.[ch]' -exec sh -c 'for f; do [ "$(head -c2 "$f")" = "#!" ] && chmod +x "$f"; done; true' _ {} +
chmod +x compile src/configure 2>/dev/null || true
rm -f .wims_maintainer_mode
unset MAKEFLAGS
yes "" | ./compile --mathjax --geogebra > "$WD/compile.log" 2>&1
bin/mkindex > "$WD/mkindex.log" 2>&1 || true
bash "$ROOT/tools/gen-theme-assets.sh" "$WD/wims" > "$WD/theme.log" 2>&1 || true
bash "$ROOT/tools/algogebra-hook/install-algogebra-hook.sh" "$WD/wims" "$ROOT/tools/algogebra-hook"
echo BUILD_DONE
