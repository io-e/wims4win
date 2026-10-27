/* Windows helpers: binary stdio (LF output identical to Linux), before main(). */
#include <stdio.h>
#include <io.h>
#include <fcntl.h>
#include <stdlib.h>
__attribute__((constructor)) static void wp_binmode(void){ _fmode=_O_BINARY; _setmode(0,_O_BINARY); _setmode(1,_O_BINARY); _setmode(2,_O_BINARY); }
