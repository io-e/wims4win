#ifdef _WIN32
#include "win32/posix_shim.h"   /* winsock2 before windows.h */
#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
static wp_sighandler_t alrm_handler=NULL;
static HANDLE alrm_timer=NULL;
static VOID CALLBACK alrm_cb(PVOID a, BOOLEAN f)
{ (void)a;(void)f; if(alrm_handler) alrm_handler(SIGALRM); }
wp_sighandler_t wp_signal(int sig, wp_sighandler_t h)
{
  wp_sighandler_t old=alrm_handler;
  if(sig!=SIGALRM) return SIG_ERR;
  alrm_handler=h; return old;
}
unsigned wp_alarm(unsigned sec)
{
  if(alrm_timer){ DeleteTimerQueueTimer(NULL,alrm_timer,NULL); alrm_timer=NULL; }
  if(sec==0) return 0;
  if(!CreateTimerQueueTimer(&alrm_timer,NULL,alrm_cb,NULL,sec*1000,0,0)) return 0;
  return sec;
}
int wp_no_fork(void){ fprintf(stderr,"wims: fork() unavailable; use platform spawn\n"); return -1; }
int wp_symlink(const char *t, const char *l)
{ return CreateSymbolicLinkA(l,t,0)?0:-1; }
int getrlimit(int r, struct rlimit *l){ (void)r; l->rlim_cur=l->rlim_max=~0UL; return 0; }
int setrlimit(int r, const struct rlimit *l){ (void)r;(void)l; return 0; }
int kill(int pid, int sig)
{
  HANDLE h; BOOL ok;
  if(pid<=0) return -1;
  h=OpenProcess(PROCESS_TERMINATE,FALSE,(DWORD)pid);
  if(!h) return -1;
  ok=TerminateProcess(h, sig==SIGKILL?137:1);
  CloseHandle(h);
  return ok?0:-1;
}
int getppid(void){ return 0; }
int getrusage(int who, struct rusage *u)
{
  FILETIME c,e,k,uft; (void)who;
  memset(u,0,sizeof(*u));
  if(!GetProcessTimes(GetCurrentProcess(),&c,&e,&k,&uft)) return -1;
  { ULARGE_INTEGER t; t.LowPart=uft.dwLowDateTime; t.HighPart=uft.dwHighDateTime;
    u->ru_utime.tv_sec=(long)(t.QuadPart/10000000ULL);
    u->ru_utime.tv_usec=(long)((t.QuadPart/10ULL)%1000000ULL); }
  return 0;
}
int pipe(int fildes[2])
{
  return _pipe(fildes,4096,O_BINARY);
}
int nanosleep(const struct timespec *req, struct timespec *rem)
{
  DWORD ms; (void)rem;
  if(!req) return -1;
  ms=(DWORD)(req->tv_sec*1000 + req->tv_nsec/1000000);
  if(ms) Sleep(ms);
  return 0;
}
#endif
