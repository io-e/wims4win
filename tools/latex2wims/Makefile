# latex2wims : application autonome (Linux gcc, Windows mingw-w64 natif)
#   make           -> build/latex2wims
#   make win32     -> build/win64/latex2wims.exe (x86_64-w64-mingw32-gcc, statique)
#   make test WIMS_HOME=/chemin/runtime   (tests séquentiels)
CC      ?= gcc
MINGW   ?= x86_64-w64-mingw32-gcc
CFLAGS  ?= -O2 -Wall -Wno-misleading-indentation
SRC      = src/latex2wims.c src/mkindex.c src/util.c src/sha256.c
HDR      = src/util.h src/mkindex.h src/sha256.h
WIMS_HOME ?=

all: build/latex2wims

build/latex2wims: $(SRC) $(HDR)
	@mkdir -p build
	$(CC) $(CFLAGS) -o $@ $(SRC)

win32: build/win64/latex2wims.exe

build/win64/latex2wims.exe: $(SRC) $(HDR)
	@mkdir -p build/win64
	$(MINGW) $(CFLAGS) -static -o $@ $(SRC)

test: build/latex2wims
	@test -n "$(WIMS_HOME)" || { echo "make test WIMS_HOME=/chemin/runtime"; exit 2; }
	sh tests/run_tests.sh "$(WIMS_HOME)"

clean:
	rm -rf build

.PHONY: all win32 test clean
.NOTPARALLEL: test
