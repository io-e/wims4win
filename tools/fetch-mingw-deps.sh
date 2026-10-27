#!/bin/bash
# Static mingw64 (msvcrt) libraries from MSYS2 for the Misc helpers, pinned:
#   gmp/gmpxx (units-filter), libgnurx = POSIX regex (mathexp).
# -> build/deps/msys2-mingw64/mingw64/{include,lib}. Sources:
#   https://github.com/msys2/MINGW-packages, https://repo.msys2.org/mingw/sources/
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); D=$ROOT/build/deps/msys2-mingw64
REPO=${MSYS2_MINGW_REPO:-https://repo.msys2.org/mingw/mingw64}
mkdir -p "$D"; cd "$D"
unzst() { if command -v zstd >/dev/null; then zstd -dc "$1"
  else python3 -c 'import sys,zstandard;zstandard.ZstdDecompressor().copy_stream(open(sys.argv[1],"rb"),sys.stdout.buffer)' "$1"; fi; }
while read -r sum f; do
  [ -f "$f" ] || curl -sSfLO "$REPO/$f"
  echo "$sum  $f" | sha256sum -c --quiet
  unzst "$f" | tar -x mingw64/include mingw64/lib
done <<'L'
8924433974c4add46cb46ea4f6ef283b5c5139d3f552375115b5580f855015cc mingw-w64-x86_64-gmp-6.3.0-2-any.pkg.tar.zst
74784a926e3ee1d91f4d6dcbbe52b909a8be8466d6fec26fadb870114ee39e02 mingw-w64-x86_64-libgnurx-2.5.1-3-any.pkg.tar.zst
L
echo "mingw deps -> $D/mingw64"
