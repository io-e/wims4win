#!/bin/bash
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/wims-win}
mkdir -p "$OUT"
x86_64-w64-mingw32-gcc -Wall -O2 -static -o "$OUT/wimslogd.exe" \
  "$ROOT/src/Wimslogd/win/wimslogd_win.c" "$ROOT/src/Platform/wp_ipc_win32.c"
echo "built $OUT/wimslogd.exe"
