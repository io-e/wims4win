/* libc compatibility for the native Win32 build. GPL v2 or later.
 * Included by includes.h only when _WIN32 is defined: the POSIX build
 * never sees this file (Linux binaries stay byte-identical). */
#ifndef WP_COMPAT_H
#define WP_COMPAT_H
#ifdef _WIN32
#include <stdlib.h>
#include <string.h>
#include <process.h>   /* _getpid */
#include <io.h>        /* _setmode, read */
#include <fcntl.h>
#include <sys/time.h>  /* gettimeofday (mingw-w64); MSVC: see wp_compat.c */

#include <direct.h>    /* _mkdir */
/* <io.h> (pulled by mingw unistd.h) declares filelength(int); libwims has its own */
#define filelength wims_filelength
/* no symlinks in the WIMS tree on Windows: lstat == stat */
#define lstat stat
#define mkdir(p,m) _mkdir(p)

int wp_setenv(const char *name, const char *value, int overwrite);
int wp_unsetenv(const char *name);
#define setenv   wp_setenv
#define unsetenv wp_unsetenv
#ifdef _MSC_VER
struct timezone;
int wp_gettimeofday(struct timeval *tv, void *tz);
#define gettimeofday wp_gettimeofday
#endif
/* POSIX wait macros are meaningless on Win32: wp_wait returns the exit code */
#ifndef WEXITSTATUS
# define WEXITSTATUS(s) (s)
# define WIFEXITED(s) 1
#endif
/* stdin/stdout must be binary, otherwise the CRT turns \n into \r\n */
void wp_stdio_binary(void);
#endif
#endif
