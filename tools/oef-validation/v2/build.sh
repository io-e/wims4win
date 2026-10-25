#!/bin/bash
# Compile oef2wims-v2.exe (assistant de correction) et oef2wims-log.exe (journal) — Windows uniquement.
set -e
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CC_WIN=${CC_WIN:-x86_64-w64-mingw32-gcc}; mkdir -p "$OUT/win"
$CC_WIN -municode -O2 -static -I"$H" -o "$OUT/win/oef2wims-v2.exe" "$H/oef2wims_v2.c" -lbcrypt
$CC_WIN -municode -O2 -static -o "$OUT/win/oef2wims-log.exe" "$H/../wrapper/oef2wims_log.c" -lbcrypt
ls -la "$OUT"/win/oef2wims-v2.exe "$OUT"/win/oef2wims-log.exe
