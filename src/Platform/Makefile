# Platform layer: POSIX build (native) and Win32 cross-check (mingw-w64)
CC ?= gcc
CFLAGS ?= -g -O2 -Wall
MINGW ?= x86_64-w64-mingw32-gcc
OBJS = wp_common.o wp_posix.o wp_mx.o

all: libwimsplatform.a
libwimsplatform.a: $(OBJS)
	ar rcs $@ $(OBJS)
%.o: %.c wims_platform.h
	$(CC) $(CFLAGS) -c $< -o $@
win32-check:
	$(MINGW) -Wall -O2 -c wp_common.c -o wp_common.w32.o
	$(MINGW) -Wall -O2 -c wp_win32.c -o wp_win32.w32.o
	$(MINGW) -Wall -O2 -c wp_mx.c -o wp_mx.w32.o
	$(MINGW) -Wall -O2 -c wp_compat.c -o wp_compat.w32.o
	x86_64-w64-mingw32-ar rcs libwimsplatform-w32.a wp_common.w32.o wp_win32.w32.o wp_mx.w32.o wp_compat.w32.o
clean:
	rm -f *.o *.a
