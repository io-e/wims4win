#!/bin/bash
# Cross-build bin/wims_mathml.exe (MathML helper) for Windows.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT/src/Mathml"
if [ ! -f y.tab.c ] || [ ! -f lex.yy.c ]; then
  bison -y -v -p wims_mathml_yy -d wims_mathml.y
  flex -Pwims_mathml_yy -olex.yy.c wims_mathml.l
fi
x86_64-w64-mingw32-gcc -Wall -O2 -c -o y.tab.o y.tab.c
x86_64-w64-mingw32-gcc -Wall -O2 -c -o lex.yy.o lex.yy.c
x86_64-w64-mingw32-g++ -Wall -O2 -static -o wims_mathml.exe lex.yy.o y.tab.o wims_mathml.cc
mkdir -p "$ROOT/build/wims-win/bin" "$ROOT/build/tests-win"
cp -f wims_mathml.exe "$ROOT/build/wims-win/bin/wims_mathml.exe"
cp -f wims_mathml.exe "$ROOT/build/tests-win/wims_mathml.exe"
echo "built $ROOT/build/wims-win/bin/wims_mathml.exe"
