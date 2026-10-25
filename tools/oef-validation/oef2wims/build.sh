#!/bin/bash
# Compile oef2wims (WIMS 4.31b, sources non modifiées dans src/) pour Linux (référence) et Windows (MinGW-w64, statique).
# Usage : bash build.sh            -> $OUT/linux/oef2wims, $OUT/win/oef2wims.exe
# Variables : OUT (défaut <dépôt>/build/oef-validation), CC_WIN (défaut x86_64-w64-mingw32-gcc), NO_LINUX=1, NO_WIN=1
set -e
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CC_WIN=${CC_WIN:-x86_64-w64-mingw32-gcc}
SRCS="Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/math.c Lib/text.c Lib/mt19937ar.c Lib/libwims.c OefMsg2wims/oeflines.c OefMsg2wims/compare.c OefMsg2wims/outexec.c OefMsg2wims/process.c OefMsg2wims/sp.c OefMsg2wims/oef2wims.c"
VD='-DVERSION_DATE="2026-10-25"'
cd "$H/src"
if [ -z "$NO_LINUX" ]; then
  mkdir -p "$OUT/linux"; rm -f "$OUT"/linux/*.o
  for f in $SRCS; do gcc -g -O2 -Wall -DGNU_SOURCE "$VD" -c $f -o "$OUT/linux/$(basename $f .c).o"; done
  gcc "$OUT"/linux/*.o -lm -o "$OUT/linux/oef2wims"; echo "linux : $OUT/linux/oef2wims"
fi
if [ -z "$NO_WIN" ]; then
  mkdir -p "$OUT/win"; rm -f "$OUT"/win/*.o
  # config.h Windows (configure ne tourne pas pour MinGW) : copie des sources + shim/config_win.h
  rm -rf "$OUT/winsrc"; cp -r "$H/src" "$OUT/winsrc"; cp "$H/shim/config_win.h" "$OUT/winsrc/config.h"; cd "$OUT/winsrc"
  for f in $SRCS; do $CC_WIN -g -O2 -Wall -I"$H/shim" -include oef2wims_win_compat.h -DGNU_SOURCE "$VD" -c $f -o "$OUT/win/$(basename $f .c).o"; done
  $CC_WIN -O2 -Wall -c "$H/shim/oef2wims_win_binmode.c" -o "$OUT/win/zz_binmode.o"
  $CC_WIN -static -o "$OUT/win/oef2wims.exe" "$OUT"/win/*.o -lm -Wl,-Bstatic -lpthread
  ${CC_WIN%gcc}strip "$OUT/win/oef2wims.exe"; sha256sum "$OUT/win/oef2wims.exe"
fi
