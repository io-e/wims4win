/* oef2wims Windows portability shim (force-included with -include).
 * Does NOT change any WIMS logic: only maps POSIX names to MinGW equivalents. */
#pragma once
#include <io.h>
#include <direct.h>
#include <sys/stat.h>
/* MinGW's <io.h> already declares an unrelated filelength(int); rename WIMS's own function. */
#define filelength wims_filelength
/* No symlinks on Windows: lstat == stat. */
#define lstat stat
/* POSIX mkdir(path, mode) -> Windows _mkdir(path) (mode ignored). */
#define mkdir(p,m) _mkdir(p)
