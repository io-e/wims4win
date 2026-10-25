#!/bin/bash
# Cross-build other/bin/flydraw.exe (WIMS drawing engine) for Windows with a
# static libgd (GIF only, no png/jpeg/freetype: flydraw only writes GIF;
# `text ... ttf` falls back to gd bitmap fonts error as on a gd without FT).
# libgd: tools/build-libgd-mingw.sh (prefix $GD, default build/deps/mingw).
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
GD=${GD:-$ROOT/build/deps/mingw}
[ -f "$GD/lib/libgd.a" ] || bash "$ROOT/tools/build-libgd-mingw.sh"
CC=${MINGW:-x86_64-w64-mingw32-gcc}
OUT=$ROOT/build/wims-win/other/bin; OBJ=$ROOT/build/flydraw-win; mkdir -p "$OUT" "$OBJ"
STAGE=$OBJ/stage; rm -rf "$STAGE"; mkdir -p "$STAGE"; cp -r "$ROOT/src/." "$STAGE/"
cp "$ROOT/src/Platform/win32/config.h" "$STAGE/config.h"
cd "$STAGE/Flydraw"; sh ./nametab.sh >/dev/null
CFLAGS="-O2 -Wall -Wno-unused-result -Wno-format-truncation -D_WIN32 -DNOMINMAX -DNONDLL -DBGDWIN32 -I$GD/include"
objs=()
for f in Flydraw/flylines.c Flydraw/nametab.c Flydraw/vimg.c Flydraw/objects.c Flydraw/flydraw.c \
         Lib/evalue.c Lib/liblines.c Lib/libwims.c Lib/math.c Lib/mt19937ar.c Lib/text.c Lib/dir.c Lib/levelcurve.c \
         Platform/wp_compat.c; do
  o=$OBJ/$(echo $f | tr / _).o; $CC $CFLAGS -c "$STAGE/$f" -o "$o"; objs+=("$o")
done
$CC -O2 -static -o "$OUT/flydraw.exe" "${objs[@]}" -L"$GD/lib" -lgd -lm -Wl,-Bstatic -lwinpthread
echo "built $OUT/flydraw.exe"
