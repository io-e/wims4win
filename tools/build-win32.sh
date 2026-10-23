#!/bin/bash
# Cross-build the WIMS pieces already ported to native Win32 (mingw-w64).
# The source tree is staged so that the hand-written Win32 config.h replaces
# the autoconf one; the POSIX tree is never touched.
# Usage: tools/build-win32.sh [OUTDIR]   (default build/win32)
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/win32}
CC=${MINGW:-x86_64-w64-mingw32-gcc}
STAGE=$OUT/stage
rm -rf "$STAGE"; mkdir -p "$STAGE" "$OUT/obj"
cp -r "$ROOT/src/." "$STAGE/"
cp "$ROOT/src/Platform/win32/config.h" "$STAGE/config.h"
CFLAGS="-O2 -Wall -Wno-unused-result -Wno-format-truncation -D_FILE_OFFSET_BITS=64"
objs=()
for f in Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/libwims.c \
         Lib/math.c Lib/mt19937ar.c Lib/text.c \
         Platform/wp_common.c Platform/wp_win32.c Platform/wp_compat.c Platform/wp_mx.c; do
  o=$OUT/obj/$(echo $f | tr / _ | sed 's/\.c$/.o/')
  $CC $CFLAGS -c "$STAGE/$f" -o "$o"; objs+=("$o")
done
x86_64-w64-mingw32-ar rcs "$OUT/libwims-w32.a" "${objs[@]}"
$CC $CFLAGS -c "$STAGE/Interfaces/common.c" -o "$OUT/obj/common.o"
for prog in pari maxima gap octave m2 scilab yacas; do
  $CC $CFLAGS -c "$STAGE/Interfaces/$prog.c" -o "$OUT/obj/$prog.o"
  $CC -static -o "$OUT/$prog.exe" "$OUT/obj/$prog.o" "$OUT/obj/common.o" "$OUT/libwims-w32.a" -lm -lpthread
done
echo "built: $(cd "$OUT" && ls *.exe)"
