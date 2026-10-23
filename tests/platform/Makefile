P=../../src/Platform
all: test
test: test_platform
	./test_platform
test_platform: test_platform.c $(P)/wp_common.c $(P)/wp_posix.c $(P)/wp_mx.c $(P)/wims_platform.h
	gcc -Wall -O2 -o $@ test_platform.c $(P)/wp_common.c $(P)/wp_posix.c $(P)/wp_mx.c
test_platform.exe: test_platform.c $(P)/wp_common.c $(P)/wp_win32.c $(P)/wp_mx.c $(P)/wims_platform.h
	x86_64-w64-mingw32-gcc -Wall -O2 -static -o $@ test_platform.c $(P)/wp_common.c $(P)/wp_win32.c $(P)/wp_mx.c
clean:
	rm -f test_platform test_platform.exe
