#!/bin/bash
# Fetch a pinned MSYS2 shell toolset (sh.exe/bash.exe + coreutils, sed, grep,
# gawk, find, tar, gzip, wget, unzip...) and install it into
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
# Layout (measured on Windows 11): msys-2.0.dll in <runtime>/other/bin makes
#   MSYS2 map "/" to <runtime> itself, so /tmp = <runtime>/tmp (WIMS tmp),
#   /etc = <runtime>/etc, and /bin = /usr/bin = <runtime>/bin (the WIMS bin/
#   scripts). sh.exe, bash.exe and env.exe are therefore also copied into
#   <runtime>/bin so that "#!/bin/sh", "#!/bin/bash" and "#!/usr/bin/env"
#   shebangs resolve. config.c puts <runtime>/other/bin first in PATH.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
RT=${1:?runtime dir}; CACHE=${2:-$ROOT/build/msys2-cache}
PINS=$ROOT/tools/msys2/msys2-pins.txt
REPO=${MSYS2_REPO:-https://repo.msys2.org/msys/x86_64}
mkdir -p "$CACHE" "$RT/other/bin" "$RT/tmp" "$RT/etc" "$RT/bin"
unzst() {  # file.tar.zst -> tar on stdout
  if command -v zstd >/dev/null; then zstd -dc "$1"
  else python3 -c 'import sys,zstandard;zstandard.ZstdDecompressor().copy_stream(open(sys.argv[1],"rb"),sys.stdout.buffer)' "$1"; fi
}
STAGE=$(mktemp -d); trap 'rm -rf "$STAGE"' EXIT
LIC=$RT/other/MSYS2-LICENSES.txt
{ echo "MSYS2 packages bundled in other/bin (pinned, sha256-verified)."
  echo "Sources: https://github.com/msys2/MSYS2-packages - https://repo.msys2.org/msys/sources/"
  echo; printf '%-18s %-14s %s\n' package version licence; } > "$LIC"
grep -v '^#' "$PINS" | while read -r name ver file sha lic; do
  [ -n "$name" ] || continue
  f=$CACHE/$file
  [ -f "$f" ] || curl -fsSL --retry 3 -o "$f" "$REPO/$file"
  echo "$sha  $f" | sha256sum -c --quiet - || { echo "sha256 mismatch: $file" >&2; exit 1; }
  unzst "$f" | tar -x -C "$STAGE" usr/bin etc 2>/dev/null || true
  printf '%-18s %-14s %s\n' "$name" "$ver" "$lic" >> "$LIC"
done
# executables and DLLs only (no docs/headers)
cp -f "$STAGE"/usr/bin/*.exe "$STAGE"/usr/bin/*.dll "$RT/other/bin/"
for f in "$STAGE"/usr/bin/*; do   # shell-script wrappers (gunzip, zcat, egrep...)
  case $f in *.exe|*.dll) continue;; esac
  [ -f "$f" ] && head -c2 "$f" | grep -q '#!' && cp -f "$f" "$RT/other/bin/"
done
[ -f "$RT/other/bin/sh.exe" ] || cp "$RT/other/bin/bash.exe" "$RT/other/bin/sh.exe"
# minimal /etc for wget (TLS CAs) ; fstab: noacl avoids NTFS ACL rewrites
[ -d "$STAGE/etc/pki" ] && cp -r "$STAGE/etc/pki" "$RT/etc/" || true
[ -d "$STAGE/etc/ssl" ] && cp -r "$STAGE/etc/ssl" "$RT/etc/" || true
printf 'none / cygdrive binary,posix=0,noacl,user 0 0\n' > "$RT/etc/fstab"
cp -f "$RT/other/bin/sh.exe" "$RT/other/bin/bash.exe" "$RT/other/bin/env.exe" "$RT/bin/"
echo "msys2: $(ls "$RT/other/bin" | wc -l) files in $RT/other/bin"
