/* Hand-written config.h for the native Win32 (mingw-w64 / MSVC) build.
 * Replaces the autoconf-generated src/config.h; only what Windows really has. */
#ifndef WIMS_WIN32_CONFIG_H
#define WIMS_WIN32_CONFIG_H
#define HAVE_DIRENT_H 1
#define HAVE_FCNTL_H 1
#define HAVE_FLOOR 1
#define HAVE_GETCWD 1
#define HAVE_GETTIMEOFDAY 1
#define HAVE_INTTYPES_H 1
#define HAVE_LIBM 1
#define HAVE_MALLOC 1
#define HAVE_MALLOC_H 1
#define HAVE_MEMMOVE 1
#define HAVE_MEMORY_H 1
#define HAVE_MEMSET 1
#define HAVE_MKDIR 1
#define HAVE_POW 1
#define HAVE_PUTENV 1
#define HAVE_REALLOC 1
#define HAVE_RINT 1
#define HAVE_RMDIR 1
#define HAVE_SQRT 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRCHR 1
#define HAVE_STRDUP 1
#define HAVE_STRERROR 1
#define HAVE_STRING_H 1
#define HAVE_STRPBRK 1
#define HAVE_STRRCHR 1
#define HAVE_STRSTR 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1
#define HAVE_UTIME 1
#define HAVE_UTIME_H 1
#define PACKAGE_BUGREPORT "wimsdev@groupes.renater.fr"
#define PACKAGE_NAME "WIMS"
#define PACKAGE_STRING "WIMS4.33a"
#define PACKAGE_TARNAME "wims"
#define PACKAGE_URL ""
#define PACKAGE_VERSION "4.33a"
#define RETSIGTYPE void
#define STDC_HEADERS 1
#define TIME_WITH_SYS_TIME 1
#define YYTEXT_POINTER 1
#endif
