/* Pre-included (-include) when building Misc/canvasdraw for Windows:
 * windows.h is parsed first with its own names, then the canvasdraw keyword
 * enum members that collide with Win32 typedefs/macros are renamed. */
#include <winsock2.h>
#include <windows.h>
#undef TRANSPARENT
#undef OPAQUE
#undef ERROR
#undef DELETE
#undef IN
#undef OUT
#define SIZE    CD_SIZE
#define INPUT   CD_INPUT
#define RECT    CD_RECT
#define POINT   CD_POINT
#define LINE    CD_LINE
#define STRING  CD_STRING
#define COPY    CD_COPY
#define POLYLINE CD_POLYLINE
#define ARC     CD_ARC
#define NEW     CD_NEW
#define STYLE   CD_STYLE
#define MOUSE   CD_MOUSE
#define GRID    CD_GRID
#define POINTS  CD_POINTS
#define GROUP   CD_GROUP
/* obabel (molecule drawing) needs fork(): reported as "fork" error on
 * Windows by canvasdraw itself; wait() is only reached after fork */
#define wait(x) (0)
#include <string.h>
#include <ctype.h>
static inline char *strcasestr(const char *h, const char *n)
{
  size_t l=strlen(n);
  for(; *h; h++) if(_strnicmp(h,n,l)==0) return (char*)h;
  return l?NULL:(char*)h;
}
#include <io.h>
#include <fcntl.h>
static inline int pipe(int fd[2]) { return _pipe(fd,4096,_O_BINARY); }
int wp_no_fork(void);
