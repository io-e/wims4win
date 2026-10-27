#!/bin/bash
# Cross-build wimslogd.exe for native Windows (mingw-w64): upstream daemon
# sources + src/Wimslogd/win/wimslogd_win.c (named pipe, no fork).
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/wims-win}
CC=${MINGW:-x86_64-w64-mingw32-gcc}
STAGE=$OUT/stage-wimslogd; rm -rf "$STAGE"; mkdir -p "$STAGE" "$OUT/obj-wimslogd"
cp -r "$ROOT/src/." "$STAGE/"; cp "$ROOT/src/Platform/win32/config.h" "$STAGE/config.h"
CFLAGS="${WIMS_CFLAGS:--O2} -Wall -Wno-unused-result -Wno-format-truncation -Wno-unused-variable -Wno-unused-but-set-variable -D_WIN32 -DWIMS_MX_PLATFORM -D_FILE_OFFSET_BITS=64 -DNOMINMAX"
SRCS="Wimslogd/cache.c Wimslogd/cleaning.c Wimslogd/cmd.c Wimslogd/files.c Wimslogd/homedir.c Wimslogd/housekeep.c Wimslogd/log.c Wimslogd/options.c Wimslogd/wimslogd.c Wimslogd/wimslogdlines.c Wimslogd/wimslogdscore.c Wimslogd/win/wimslogd_win.c Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/libwims.c Lib/math.c Lib/mt19937ar.c Lib/text.c Platform/wp_common.c Platform/wp_win32.c Platform/wp_compat.c Platform/wp_ipc_win32.c Platform/wp_alarm.c"
objs=()
for f in $SRCS; do
  o=$OUT/obj-wimslogd/$(echo $f | tr / _).o
  $CC $CFLAGS -c "$STAGE/$f" -o "$o"; objs+=("$o")
done
# MAX_FILELEN (2 MB) buffers live on the stack (files.c): Linux has 8 MB, Windows 1 MB
$CC -O2 -static -Wl,--stack,33554432 -o "$OUT/wimslogd.exe" "${objs[@]}" -lws2_32 -ladvapi32 -lshell32 -lbcrypt -lm
echo "built $OUT/wimslogd.exe ($(stat -c%s "$OUT/wimslogd.exe") bytes)"
