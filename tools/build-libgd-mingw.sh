#!/bin/bash
# Static libgd 2.3.3 for mingw-w64 (GIF/core only), for flydraw.exe.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); D=$ROOT/build/deps; mkdir -p "$D"; cd "$D"
[ -d libgd-2.3.3 ] || { curl -sSL -o gd.tgz https://github.com/libgd/libgd/releases/download/gd-2.3.3/libgd-2.3.3.tar.gz; tar xzf gd.tgz; }
cd libgd-2.3.3
./configure --host=x86_64-w64-mingw32 --prefix="$D/mingw" --disable-shared --enable-static \
  --without-png --without-jpeg --without-freetype --without-fontconfig --without-xpm \
  --without-tiff --without-webp --without-heif --without-avif --without-liq --without-raqm \
  --without-x --without-zlib >/dev/null
make -j4 -C src libgd.la >/dev/null
make -C src install-libLTLIBRARIES install-includeHEADERS >/dev/null
echo "libgd -> $D/mingw"
