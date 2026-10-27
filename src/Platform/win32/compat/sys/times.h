/* <sys/times.h> for mingw-w64 (Misc/crossword timing). GPL v2+. */
#ifndef WP_SYS_TIMES_H
#define WP_SYS_TIMES_H
#include <windows.h>
#include <time.h>
struct tms { clock_t tms_utime, tms_stime, tms_cutime, tms_cstime; };
static inline clock_t times(struct tms *t)
{
  FILETIME c, e, k, u; ULARGE_INTEGER a, b;
  if(GetProcessTimes(GetCurrentProcess(),&c,&e,&k,&u)) {
    a.LowPart=u.dwLowDateTime; a.HighPart=u.dwHighDateTime;
    b.LowPart=k.dwLowDateTime; b.HighPart=k.dwHighDateTime;
    t->tms_utime=(clock_t)(a.QuadPart/(10000000/CLOCKS_PER_SEC));
    t->tms_stime=(clock_t)(b.QuadPart/(10000000/CLOCKS_PER_SEC));
  } else t->tms_utime=t->tms_stime=0;
  t->tms_cutime=t->tms_cstime=0;
  return clock();
}
#ifndef _SC_CLK_TCK
#define _SC_CLK_TCK 2
static inline long sysconf(int n) { return n==_SC_CLK_TCK ? (long)CLOCKS_PER_SEC : -1; }
#endif
#endif
