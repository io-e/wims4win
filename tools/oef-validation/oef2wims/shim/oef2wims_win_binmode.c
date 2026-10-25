/* oef2wims Windows shim: force binary I/O (LF line endings, no CRLF translation)
 * so that stdout and the .def output are byte-identical to the Linux build.
 * Runs before main(); does not touch any WIMS logic. */
#include <stdio.h>
#include <stdlib.h>
#include <io.h>
#include <fcntl.h>
__attribute__((constructor)) static void oef2wims_binmode(void)
{
  _fmode = _O_BINARY;
  _setmode(_fileno(stdin), _O_BINARY);
  _setmode(_fileno(stdout), _O_BINARY);
  _setmode(_fileno(stderr), _O_BINARY);
}
