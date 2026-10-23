#!/bin/bash
# Cross-build wims.exe (CGI) for native Windows with mingw-w64.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/wims-win}
CC=${MINGW:-x86_64-w64-mingw32-gcc}
STAGE=$OUT/stage; rm -rf "$STAGE"; mkdir -p "$STAGE" "$OUT/obj"
cp -r "$ROOT/src/." "$STAGE/"; cp "$ROOT/src/Platform/win32/config.h" "$STAGE/config.h"
CFLAGS="${WIMS_CFLAGS:--O2} -Wall -Wno-unused-result -Wno-format-truncation -D_WIN32 -DWIMS_MX_PLATFORM -D_FILE_OFFSET_BITS=64 -DNOMINMAX"
SRCS="auth.c calc.c cleaning.c compare.c config.c evalue.c exam.c exec.c files.c html.c insmath.c lines.c log.c mathfonts.c mathml.c matrix.c pedia.c rawmath.c score.c texmath.c test.c var.c variables.c wims.c Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/libwims.c Lib/math.c Lib/mt19937ar.c Lib/text.c Platform/wp_common.c Platform/wp_win32.c Platform/wp_compat.c Platform/wp_cgi_win32.c Platform/wp_mx.c Platform/wp_alarm.c Platform/wp_ipc_win32.c Platform/wp_descrypt.c Platform/wp_crypt_bcrypt.c"
objs=()
for f in $SRCS; do
  o=$OUT/obj/$(echo $f | tr / _).o
  $CC $CFLAGS -c "$STAGE/$f" -o "$o"; objs+=("$o")
done
$CC -O2 -static -o "$OUT/wims.exe" "${objs[@]}" -lws2_32 -ladvapi32 -lshell32 -lbcrypt -lm
echo "built $OUT/wims.exe ($(stat -c%s "$OUT/wims.exe") bytes)"
