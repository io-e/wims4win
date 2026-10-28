#!/bin/bash
# Build a complete WIMS runtime tree (html/, msg/, scripts/, module indexes,
# log/ defaults) on Linux, then package it for native Windows without any
# Linux ELF binary, with the Windows executables placed where wims.exe
# expects them.
#
# Usage: tools/build-runtime-tree.sh <source> [workdir]
#   source (reference, decision Q1/Q2 of 2026-10-27): an `svn export -r 19040`
#     of https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims, given
#     as a directory or a .tgz/.tar.gz of it, so that public_html, modules and
#     scripts match the 4.33a engine. Set WIMS_SVN_REV (default 19040).
#     From the Grok box Renater TLS is refused: export on the PC with SlikSVN,
#     then copy the archive over.
#   legacy: an official release tarball (wims-4.32.tgz) still works.
# Environment:
#   WIMS_SVN_REV   revision recorded in log/wims4win-provenance.txt (19040)
#   PARI_COMMAND   optional pari_command line for log/wims.conf
#   NO_MSYS2=1     skip tools/fetch-msys2-sh.sh (sh.exe + coreutils)
# Debian prerequisites:
#   build-essential flex bison libfl-dev libgd-dev libgmp-dev wget unzip
#   mingw-w64 (gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64)
# Output: <workdir>/wims-runtime.tgz
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
SRC=${1:?usage: build-runtime-tree.sh <svn-export dir|tgz> [workdir]}
SRC=$(readlink -f "$SRC")
WD=${2:-$ROOT/build/runtime}; mkdir -p "$WD"; WD=$(cd "$WD" && pwd)
# not named "src": canvasdraw locates other/bin with awk -F/src
mkdir -p "$WD/wims"; cd "$WD/wims"
[ -x public_html/wims ] || {
  if [ -d "$SRC" ]; then cp -a "$SRC"/. .
  else tar xzf "$SRC"; [ -d wims ] && { cp -a wims/. . && rm -rf wims; }; fi
  # an svn export made on Windows (SlikSVN) turns svn:special symlinks into
  # small text files "link <target>" (~225 in r19040: html/themes, msg,
  # scripts/model aliases...): turn them back into symlinks
  find . -type f -size -300c -exec grep -lI -m1 '^link ' {} + 2>/dev/null | while read -r f; do
    [ "$(wc -l < "$f")" -eq 0 ] || continue
    t=$(sed 's/^link //' "$f"); rm -f "$f"; ln -s "$t" "$f"
  done
  # an svn export made on Windows (SlikSVN) loses the exec bits: restore
  # them on every script (#!) and on configure
  find . -type f ! -name '*.[ch]' -exec sh -c 'for f; do [ "$(head -c2 "$f")" = "#!" ] && chmod +x "$f"; done; true' _ {} +
  chmod +x compile src/configure 2>/dev/null || true
  # SVN trunk ships .wims_maintainer_mode (doc/maintainer targets needing
  # ImageMagick, pdflatex): build like a release instead
  rm -f .wims_maintainer_mode
  # the upstream Makefile is not parallel-safe (mkindex before modind)
  unset MAKEFLAGS
  # --shtooka is skipped: downloadcenter.wimsedu.info is unreachable (optional audio).
  yes "" | ./compile --mathjax --geogebra > "$WD/compile.log" 2>&1
  bin/mkindex > "$WD/mkindex.log" 2>&1 || true
}
# Windows executables
for s in build-win32 build-wims-win32 build-wimslogd-win32 build-chroot-win32 build-wims-mathml-win32 build-flydraw-win32; do
  bash "$ROOT/tools/$s.sh" >/dev/null
done
OUT=$WD/wims-runtime; rm -rf "$OUT"; mkdir -p "$OUT"
# -h: Windows has no usable symlinks; WIMS relies on ~260 of them
# (html/themes -> ../themes, scripts/themes, anstype aliases...), so copy
# their targets. wims.cgi -> wims is dropped (replaced by wims.exe).
tar -C "$WD/wims" -h --exclude=./public_html/wims.cgi --exclude=./src --exclude=./other --exclude=./tmp --exclude=./sessions \
    --exclude=./s2 --exclude=./lib --exclude=./download --exclude=./dfsg --exclude=./wimstest --exclude=./localwims --exclude=./Makefile.for.debian --exclude=./mkdistsvn --exclude=./README.template --exclude='./compile*' --exclude='./*.log' --exclude='*.tgz' -cf - . | tar -C "$OUT" -xf -
# Windows-only script fixes (scripts otherwise unchanged, D17):
#  bin/mkindex: pass mlist/slist/glist to modind as files (32767-char env limit)
patch -s -d "$OUT" -p1 < "$ROOT/tools/patches/mkindex-wims4win.patch"
# drop every Linux ELF binary (and the wims.cgi symlink)
find "$OUT" -type f -exec sh -c 'head -c4 "$1" | grep -q ELF && rm -f "$1"' _ {} \;
find "$OUT" -type l -delete   # broken links only (tar -h kept the rest)
mkdir -p "$OUT/tmp/log" "$OUT/tmp/sessions" "$OUT/sessions" "$OUT/s2"
cp "$ROOT/build/wims-win/wims.exe" "$OUT/public_html/wims.exe"
cp "$ROOT/build/wims-win/wims.exe" "$OUT/public_html/wims.cgi.exe"
cp "$ROOT/build/wims-win/wimslogd.exe" "$OUT/bin/"
cp "$ROOT/build/wims-win/bin/wims_mathml.exe" "$OUT/bin/"
cp "$ROOT/build/tests-win/ch..root.exe" "$OUT/public_html/bin/"
# CAS sandbox on (#8): Interfaces/common.c runs every CAS through
# bin/ch..root.exe (AppContainer + Job Object) when this marker exists; wims
# refuses the CAS if the sandbox selftest fails. Each CAS install dir must be
# readable by AppContainers: ch..root.exe --grant-rx <dir> (see docs).
mkdir -p "$OUT/chroot/tmp/sessions"; : > "$OUT/chroot/tmp/sessions/.chroot"
# other/bin scripts from the tree (src2def, imgindex, findnoexec), not the ELF
mkdir -p "$OUT/other/bin"
for f in "$WD/wims/other/bin/"*; do
  [ -f "$f" ] && ! head -c4 "$f" | grep -q ELF && cp "$f" "$OUT/other/bin/"
done
# C helpers of Misc/OefMsg2wims built for Windows (translator, dicsort,
# modind, oef2wims, msg2wims, webget...)
bash "$ROOT/tools/build-misc-win32.sh" "$ROOT/build/misc-win" >/dev/null
cp "$ROOT"/build/misc-win/public_html/bin/*.exe "$OUT/public_html/bin/"
cp "$ROOT"/build/misc-win/bin/*.exe "$OUT/bin/"
# the other Misc programs (canvasdraw, chemeq, units-filter, wrap..exec...)
bash "$ROOT/tools/build-misc-extra-win32.sh" "$ROOT/build/misc-win" >/dev/null
cp "$ROOT"/build/misc-win/other/bin/*.exe "$OUT/other/bin/"
# drawing: flydraw.exe + native insdraw processor (replaces the sh script)
cp "$ROOT/build/wims-win/other/bin/flydraw.exe" "$OUT/other/bin/"
${MINGW:-x86_64-w64-mingw32-gcc} -O2 -Wall -static -o "$OUT/public_html/bin/insdraw..processor.exe" \
   "$ROOT/src/Platform/tools/insdraw_win.c"
rm -f "$OUT/public_html/bin/insdraw..processor"
# site configuration (persistent): MathML formulas, wims4win settings
grep -q '^default_texposition=' "$OUT/log/wims.conf" 2>/dev/null || \
  printf 'default_texposition=2\n' >> "$OUT/log/wims.conf"
cp "$ROOT/docs/wims4win.yaml" "$OUT/log/wims4win.yaml"
[ -n "$PARI_COMMAND" ] && { sed -i '/^pari_command=/d' "$OUT/log/wims.conf"; \
  printf 'pari_command=%s\n' "$PARI_COMMAND" >> "$OUT/log/wims.conf"; }
# sh.exe + coreutils (MSYS2, pinned) for call_sh and the bin/ scripts
[ -n "$NO_MSYS2" ] || bash "$ROOT/tools/fetch-msys2-sh.sh" "$OUT"
# provenance of the tree
{ echo "source: $(basename "$SRC")"
  [ -f "$SRC" ] && echo "source_sha256: $(sha256sum "$SRC" | cut -d' ' -f1)"
  echo "svn_url: https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims"
  echo "svn_revision: ${WIMS_SVN_REV:-19040}"
  echo "wims_version: $(cat "$WD/wims/version" 2>/dev/null)"
  echo "wims4win_commit: $(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null)"
  echo "built: $(date -u +%Y-%m-%dT%H:%MZ)"
} > "$OUT/log/wims4win-provenance.txt"
for i in gap maxima octave pari m2 scilab yacas; do
  [ -f "$ROOT/build/win32/$i.exe" ] && cp "$ROOT/build/win32/$i.exe" "$OUT/public_html/bin/"
done
tar -C "$WD" -czf "$WD/wims-runtime.tgz" wims-runtime
echo "packaged $WD/wims-runtime.tgz ($(du -h "$WD/wims-runtime.tgz" | cut -f1))"
