#!/bin/bash
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/tests-win}
mkdir -p "$OUT"
x86_64-w64-mingw32-gcc -Wall -O2 -D_WIN32_WINNT=0x0602 -static \
  -o "$OUT/ch..root.exe" "$ROOT/src/Platform/sandbox/chroot_win.c" -luserenv -ladvapi32
echo "built $OUT/ch..root.exe"
