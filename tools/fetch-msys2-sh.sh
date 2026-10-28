#!/bin/bash
# Fetch a pinned MSYS2 shell toolset (sh.exe/bash.exe + coreutils, sed, grep,
# gawk, find, tar, gzip, wget, unzip, perl, cpp...) and install it into
# <runtime>/other/bin, so WIMS shell scripts (call_sh, oefcompile, mkindex,
# modupdate, backup...) run on Windows without cmd.exe and without rewriting.
#
# Usage: tools/fetch-msys2-sh.sh <runtime-dir> [cache-dir]
# Pins: tools/msys2/msys2-pins.txt (name version file sha256 license).
#   Every package is verified against its sha256 before extraction.
#   Regenerate pins only deliberately: tools/msys2/resolve-pins.py.
# Licences: GPL/LGPL and others, listed in <runtime>/other/MSYS2-LICENSES.txt.
#   Corresponding sources: https://github.com/msys2/MSYS2-packages (and
#   https://repo.msys2.org/msys/sources/ for the exact pinned versions).
# Layout: MSYS2 has its own root, <runtime>/other/msys2 (= "/" for sh), with
#   the programs in other/msys2/usr/bin (= /usr/bin = /bin). The runtime is
#   reached through its real path (/d/.../runtime), so scripts computing
#   WIMS_HOME with `pwd` (mkindex, oefcompile, modupdate) get a usable path.
#   config.c (Windows) puts other/bin then other/msys2/usr/bin first in PATH.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
RT=${1:?runtime dir}; CACHE=${2:-$ROOT/build/msys2-cache}
PINS=$ROOT/tools/msys2/msys2-pins.txt
REPO=${MSYS2_REPO:-https://repo.msys2.org/msys/x86_64}
MS=$RT/other/msys2
mkdir -p "$CACHE" "$MS/usr/bin" "$MS/tmp" "$MS/etc"
unzst() {  # file.tar.zst -> tar on stdout
  if command -v zstd >/dev/null; then zstd -dc "$1"
  else python3 -c 'import sys,zstandard;zstandard.ZstdDecompressor().copy_stream(open(sys.argv[1],"rb"),sys.stdout.buffer)' "$1"; fi
}
STAGE=$(mktemp -d); trap 'rm -rf "$STAGE"' EXIT
LIC=$MS/MSYS2-LICENSES.txt
{ echo "MSYS2 packages bundled in other/bin (pinned, sha256-verified)."
  echo "Sources: https://github.com/msys2/MSYS2-packages - https://repo.msys2.org/msys/sources/"
  echo; printf '%-18s %-14s %s\n' package version licence; } > "$LIC"
grep -v '^#' "$PINS" | while read -r name ver file sha lic; do
  [ -n "$name" ] || continue
  f=$CACHE/$file
  [ -f "$f" ] || curl -fsSL --retry 3 -o "$f" "$REPO/$file"
  echo "$sha  $f" | sha256sum -c --quiet - || { echo "sha256 mismatch: $file" >&2; exit 1; }
  if [ "$name" = gcc ]; then
    # only the C preprocessor (cpp.exe + cc1.exe) for scripts/oef/mkindex
    # ("cpp -P -C -nostdinc ..." on src/cpp/*.cpp OEF models, bin/mkindex)
    unzst "$f" | tar -x -C "$STAGE" usr/bin/cpp.exe "usr/lib/gcc/x86_64-pc-cygwin/${ver%-*}/cc1.exe"
  else
    unzst "$f" | tar -x -C "$STAGE" usr/bin usr/lib/perl5 usr/share/perl5 etc 2>/dev/null || true
  fi
  printf '%-18s %-14s %s\n' "$name" "$ver" "$lic" >> "$LIC"
done
# executables and DLLs only (no docs/headers)
cp -f "$STAGE"/usr/bin/*.exe "$STAGE"/usr/bin/*.dll "$MS/usr/bin/"
for f in "$STAGE"/usr/bin/*; do   # shell-script wrappers (gunzip, zcat, egrep...)
  case $f in *.exe|*.dll) continue;; esac
  [ -f "$f" ] && head -c2 "$f" | grep -q '#!' && cp -f "$f" "$MS/usr/bin/"
done
[ -f "$MS/usr/bin/sh.exe" ] || cp "$MS/usr/bin/bash.exe" "$MS/usr/bin/sh.exe"
# minimal /etc for wget (TLS CAs) ; fstab: noacl avoids NTFS ACL rewrites
[ -d "$STAGE/etc/pki" ] && cp -r "$STAGE/etc/pki" "$MS/etc/" || true
[ -d "$STAGE/etc/ssl" ] && cp -r "$STAGE/etc/ssl" "$MS/etc/" || true
printf 'none / cygdrive binary,posix=0,noacl,user 0 0\n' > "$MS/etc/fstab"
[ -d "$STAGE/usr/lib/gcc" ] && { mkdir -p "$MS/usr/lib"; cp -r "$STAGE/usr/lib/gcc" "$MS/usr/lib/"; }
[ -d "$STAGE/usr/lib/perl5" ] && { mkdir -p "$MS/usr/lib"; cp -r "$STAGE/usr/lib/perl5" "$MS/usr/lib/"; }
[ -d "$STAGE/usr/share/perl5" ] && { mkdir -p "$MS/usr/share"; cp -r "$STAGE/usr/share/perl5" "$MS/usr/share/"; }
echo "msys2: $(ls "$MS/usr/bin" | wc -l) files in $MS/usr/bin"
