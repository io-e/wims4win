#!/bin/bash
# Build a complete WIMS runtime tree (html/, msg/, scripts/, module indexes,
# log/ defaults) on Linux, then package it for native Windows without any
# Linux ELF binary, with the Windows executables placed where wims.exe
# expects them.
#
# Usage: tools/build-runtime-tree.sh [wims-4.xx.tgz] [workdir]
#   tarball: official release from SourceSup, e.g.
#     https://sourcesup.renater.fr/frs/download.php/file/6765/wims-4.32.tgz
#   (svn export of trunk/wims/public_html is an alternative but very slow;
#    from the Grok box Renater TLS is refused, so the tarball was fetched
#    on the PC and copied over).
# Debian prerequisites:
#   build-essential flex bison libfl-dev libgd-dev libgmp-dev wget unzip
#   mingw-w64 (gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64)
# Output: <workdir>/wims-runtime.tgz
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TGZ=${1:-wims-4.32.tgz}
WD=${2:-$ROOT/build/runtime}
mkdir -p "$WD/src"; cd "$WD/src"
[ -x public_html/wims ] || {
  tar xzf "$TGZ"
  # --shtooka is skipped: downloadcenter.wimsedu.info is unreachable (optional audio).
  yes "" | ./compile --mathjax --geogebra > "$WD/compile.log" 2>&1
  bin/mkindex > "$WD/mkindex.log" 2>&1 || true
}
# Windows executables
for s in build-win32 build-wims-win32 build-wimslogd-win32 build-chroot-win32 build-wims-mathml-win32; do
  bash "$ROOT/tools/$s.sh" >/dev/null
done
OUT=$WD/wims-runtime; rm -rf "$OUT"; mkdir -p "$OUT"
# -h: Windows has no usable symlinks; WIMS relies on ~260 of them
# (html/themes -> ../themes, scripts/themes, anstype aliases...), so copy
# their targets. wims.cgi -> wims is dropped (replaced by wims.exe).
tar -C "$WD/src" -h --exclude=./public_html/wims.cgi --exclude=./src --exclude=./other --exclude=./tmp --exclude=./sessions \
    --exclude=./s2 --exclude=./lib --exclude=./download --exclude='./compile*' --exclude='./*.log' --exclude='*.tgz' -cf - . | tar -C "$OUT" -xf -
# drop every Linux ELF binary (and the wims.cgi symlink)
find "$OUT" -type f -exec sh -c 'head -c4 "$1" | grep -q ELF && rm -f "$1"' _ {} \;
find "$OUT" -type l -delete   # broken links only (tar -h kept the rest)
mkdir -p "$OUT/tmp/log" "$OUT/tmp/sessions" "$OUT/sessions" "$OUT/s2"
cp "$ROOT/build/wims-win/wims.exe" "$OUT/public_html/wims.exe"
cp "$ROOT/build/wims-win/wims.exe" "$OUT/public_html/wims.cgi.exe"
cp "$ROOT/build/wims-win/wimslogd.exe" "$OUT/bin/"
cp "$ROOT/build/wims-win/bin/wims_mathml.exe" "$OUT/bin/"
cp "$ROOT/build/tests-win/ch..root.exe" "$OUT/public_html/bin/"
for i in gap maxima octave pari m2 scilab yacas; do
  [ -f "$ROOT/build/win32/$i.exe" ] && cp "$ROOT/build/win32/$i.exe" "$OUT/public_html/bin/"
done
tar -C "$WD" -czf "$WD/wims-runtime.tgz" wims-runtime
echo "packaged $WD/wims-runtime.tgz ($(du -h "$WD/wims-runtime.tgz" | cut -f1))"
