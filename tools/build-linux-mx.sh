#!/bin/bash
# Build wims (Linux) with the OPTIONAL platform multiexec path (-DWIMS_MX_PLATFORM,
# add -DWIMS_MX_FIX_TIMEOUT via EXTRA=...). Needs a configured build tree
# (BUILD=../wims-new by default) whose src/ is overlaid with this repo's sources.
set -e; ROOT=$(cd "$(dirname "$0")/.." && pwd); BUILD=${BUILD:-$ROOT/../wims-new}
T=${OUT:-$ROOT/build/linux-mx}; rm -rf "$T"; cp -a "$BUILD" "$T"; cd "$T/src"
cp "$ROOT"/src/*.c "$ROOT"/src/*.h .; cp -r "$ROOT/src/Platform" .
(cd Platform && gcc -O2 -c wp_common.c wp_posix.c wp_mx.c)
O=$(grep '^O=' Makefile | cut -d= -f2); rm -f $O
make -j4 cflags="-g -O2 -Wall -DWIMS_MX_PLATFORM $EXTRA" $O >/dev/null
gcc -g -O2 Lib/libwims.o $O Platform/wp_common.o Platform/wp_posix.o Platform/wp_mx.o \
    -L../lib -lwims -lcrypt -lm -o wims-mx
echo "built $T/src/wims-mx"
