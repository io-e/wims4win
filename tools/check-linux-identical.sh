#!/bin/bash
# Prove that the Linux objects are byte-identical between a git revision
# (default HEAD) and the working tree, for every file touched by the port.
# Needs a configured config.h (CONFIG_H=..., default ../wims-new/src/config.h).
ROOT=$(cd "$(dirname "$0")/.." && pwd); REV=${1:-HEAD}
CONFIG_H=${CONFIG_H:-$ROOT/../wims-new/src/config.h}
FILES=${FILES:-"Interfaces/common.c Interfaces/pari.c Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/libwims.c Lib/math.c Lib/mt19937ar.c Lib/text.c calc.c lines.c Platform/wp_posix.c Platform/wp_common.c"}
T=$(mktemp -d); mkdir -p $T/a $T/b
git -C "$ROOT" archive "$REV" src | tar -x -C $T/a
cp -r "$ROOT/src" $T/b/
cp "$CONFIG_H" $T/a/src/config.h; cp "$CONFIG_H" $T/b/src/config.h
rc=0
for f in $FILES; do
  for v in a b; do
    [ -f $T/$v/src/$f ] || { echo "MISSING $v $f"; continue; }
    (cd $T/$v/src/$(dirname $f) && gcc -O2 -c $(basename $f) -o $T/$v.o 2>/dev/null) || echo "COMPILE-FAIL $v $f"
    objcopy --remove-section .comment $T/$v.o 2>/dev/null
  done
  if cmp -s $T/a.o $T/b.o; then echo "IDENTICAL $f"; else echo "DIFFERENT $f"; rc=1; fi
done
rm -rf $T; exit $rc
