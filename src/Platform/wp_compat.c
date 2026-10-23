/* libc compatibility for the native Win32 build. GPL v2 or later. */
#ifdef _WIN32
#include <stdio.h>
#include "wp_compat.h"
#include <windows.h>

int setenv(const char *name, const char *value, int overwrite)
{
  if(name==NULL || *name==0 || strchr(name,'=')) return -1;
  if(!overwrite && getenv(name)!=NULL) return 0;
  /* _putenv_s refuses an empty value as "unset": keep POSIX semantics */
  if(value==NULL) value="";
  /* Limitation: the Windows CRT cannot hold an empty variable; setenv(x,"")
   * removes it, so getenv() returns NULL instead of "". WIMS code tests
   * (p==NULL || *p==0) everywhere, so both are equivalent for it. */
  return _putenv_s(name,value)==0?0:-1;
}

int unsetenv(const char *name)
{
  if(name==NULL || *name==0) return -1;
  return _putenv_s(name,"")==0?0:-1;
}

#ifdef _MSC_VER
int wp_gettimeofday(struct timeval *tv, void *tz)
{
  FILETIME ft; unsigned long long t; (void)tz;
  GetSystemTimeAsFileTime(&ft);
  t=((unsigned long long)ft.dwHighDateTime<<32)|ft.dwLowDateTime;
  t-=116444736000000000ULL; t/=10;
  tv->tv_sec=(long)(t/1000000); tv->tv_usec=(long)(t%1000000);
  return 0;
}
#endif

void wp_stdio_binary(void)
{
  _setmode(0,_O_BINARY); _setmode(1,_O_BINARY); _setmode(2,_O_BINARY);
}
#endif
