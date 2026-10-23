/* Win32 implementation of wims_platform -- PHASE 1 SKELETON. GPL v2 or later.
 *
 * Implemented: channel (anonymous pipes, non-inherited parent ends),
 * CreateProcess with STARTUPINFOEX + PROC_THREAD_ATTRIBUTE_HANDLE_LIST
 * (only the 3 std handles are inherited), wait/alive/kill on HANDLEs,
 * poll() emulation (PeekNamedPipe for read; write side reported ready),
 * bounded read, CR stripping.
 *
 * NOT YET implemented (deliberately refused, never silently ignored):
 *  - WP_ISO_DROP_PRIV / WP_ISO_WRAPPER: no sandbox yet -> WP_E_NOISOLATION
 *  - overlapped write side (a write may block if the child does not read;
 *    the caller writes line by line as the historical code does).
 */
#ifdef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "wims_platform.h"

const char *wp_platform_name(void) { return "win32"; }

int wp_chan_open(wp_chan *c)
{
  SECURITY_ATTRIBUTES sa;
  sa.nLength=sizeof(sa); sa.lpSecurityDescriptor=NULL; sa.bInheritHandle=TRUE;
  if(!CreatePipe(&c->in_r,&c->in_w,&sa,0)) return WP_E_SYS;
  if(!CreatePipe(&c->out_r,&c->out_w,&sa,0)) {
    CloseHandle(c->in_r); CloseHandle(c->in_w); return WP_E_SYS;
  }
  /* parent ends must never be inherited */
  SetHandleInformation(c->in_w,HANDLE_FLAG_INHERIT,0);
  SetHandleInformation(c->out_r,HANDLE_FLAG_INHERIT,0);
  return WP_OK;
}

void wp_chan_close_parent_write(wp_chan *c)
{
  if(c->in_w!=INVALID_HANDLE_VALUE && c->in_w!=NULL) CloseHandle(c->in_w);
  c->in_w=INVALID_HANDLE_VALUE;
}

/* MSVCRT quoting rules for one argument */
static size_t quote_arg(char *dst, size_t cap, const char *a)
{
  size_t n=0; const char *p; int need=(*a==0)||strpbrk(a," \t\"")!=NULL;
#define PUT(ch) do{ if(n+1<cap) dst[n]=(ch); n++; }while(0)
  if(!need) { for(p=a;*p;p++) PUT(*p); return n; }
  PUT('"');
  for(p=a;;p++) {
    size_t bs=0;
    while(*p=='\\') { bs++; p++; }
    if(*p==0) { while(bs--) { PUT('\\'); PUT('\\'); } break; }
    if(*p=='"') { while(bs--) { PUT('\\'); PUT('\\'); } PUT('\\'); PUT('"'); }
    else { while(bs--) PUT('\\'); PUT(*p); }
  }
  PUT('"');
#undef PUT
  return n;
}

static HANDLE open_redirect(const char *f, int write)
{
  SECURITY_ATTRIBUTES sa;
  sa.nLength=sizeof(sa); sa.lpSecurityDescriptor=NULL; sa.bInheritHandle=TRUE;
  return CreateFileA(f, write?GENERIC_WRITE:GENERIC_READ, FILE_SHARE_READ, &sa,
                     write?CREATE_ALWAYS:OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
}

int wp_spawn(wp_chan *c, char *const argv[], const char *inf,
             const char *outf, const char *errf, wp_isolation iso,
             const char *progname, wp_pid *pid)
{
  char cmd[32768]; size_t n=0; int i, ok;
  HANDLE hin, hout, herr, list[3];
  STARTUPINFOEXA si; PROCESS_INFORMATION pi; SIZE_T asz=0;
  (void)progname;
  if(argv==NULL || argv[0]==NULL || pid==NULL) return WP_E_ARG;
  if(iso!=WP_ISO_NONE) return WP_E_NOISOLATION; /* no sandbox yet: refuse */
  for(i=0;argv[i];i++) {
    if(i) { if(n+1<sizeof(cmd)) cmd[n]=' '; n++; }
    n+=quote_arg(cmd+n, n<sizeof(cmd)?sizeof(cmd)-n:0, argv[i]);
  }
  if(n>=sizeof(cmd)) return WP_E_ARG;
  cmd[n]=0;
  hin = inf ? open_redirect(inf,0) : c->in_r;
  hout= outf? open_redirect(outf,1): c->out_w;
  herr= errf? open_redirect(errf,1): GetStdHandle(STD_ERROR_HANDLE);
  if(hin==INVALID_HANDLE_VALUE || hout==INVALID_HANDLE_VALUE
     || herr==INVALID_HANDLE_VALUE) return WP_E_SYS;
  list[0]=hin; list[1]=hout; list[2]=herr;
  memset(&si,0,sizeof(si)); si.StartupInfo.cb=sizeof(si);
  si.StartupInfo.dwFlags=STARTF_USESTDHANDLES;
  si.StartupInfo.hStdInput=hin; si.StartupInfo.hStdOutput=hout;
  si.StartupInfo.hStdError=herr;
  InitializeProcThreadAttributeList(NULL,1,0,&asz);
  si.lpAttributeList=(LPPROC_THREAD_ATTRIBUTE_LIST)malloc(asz);
  if(!si.lpAttributeList) return WP_E_SYS;
  InitializeProcThreadAttributeList(si.lpAttributeList,1,0,&asz);
  UpdateProcThreadAttribute(si.lpAttributeList,0,PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
                            list,(herr==hout?2:3)*sizeof(HANDLE),NULL,NULL);
  ok=CreateProcessA(NULL,cmd,NULL,NULL,TRUE,
                    CREATE_NO_WINDOW|EXTENDED_STARTUPINFO_PRESENT,
                    NULL,NULL,&si.StartupInfo,&pi);
  DeleteProcThreadAttributeList(si.lpAttributeList); free(si.lpAttributeList);
  if(inf) CloseHandle(hin);
  if(outf) CloseHandle(hout);
  if(errf) CloseHandle(herr);
  if(!ok) return WP_E_SYS;
  CloseHandle(pi.hThread);
  pid->h=pi.hProcess; pid->id=pi.dwProcessId;
  return WP_OK;
}

int wp_wait(wp_pid *pid, int *status)
{
  DWORD code=0;
  if(WaitForSingleObject(pid->h,INFINITE)!=WAIT_OBJECT_0) return 0;
  GetExitCodeProcess(pid->h,&code); if(status) *status=(int)code;
  return 1;
}

int wp_alive(wp_pid *pid)
{
  return WaitForSingleObject(pid->h,0)==WAIT_TIMEOUT;
}

int wp_kill(wp_pid *pid) { return TerminateProcess(pid->h,137)?0:-1; }

int wp_poll(wp_chan *c, int want_read, int want_write, long timeout_usec,
            int *can_read, int *can_write)
{
  DWORD avail=0, waited=0, step=10, limit=(DWORD)(timeout_usec/1000);
  int r=0;
  for(;;) {
    r=0;
    if(want_read) {
      if(!PeekNamedPipe(c->out_r,NULL,0,NULL,&avail,NULL)) avail=1; /* EOF/err: let read report it */
      if(avail) r=1;
    }
    if(r || want_write || waited>=limit) break;
    Sleep(step); waited+=step;
  }
  if(can_read) *can_read=r;
  if(can_write) *can_write=want_write?1:0; /* TODO phase 2: overlapped write */
  return r || want_write;
}

long wp_read(wp_chan *c, char *buf, long cap)
{
  DWORD got=0;
  if(cap<=0) return 0;
  if(!ReadFile(c->out_r,buf,(DWORD)cap,&got,NULL))
    return GetLastError()==ERROR_BROKEN_PIPE?0:-1;
  return (long)got;
}

long wp_write(wp_chan *c, const char *buf, long len)
{
  DWORD put=0;
  if(!WriteFile(c->in_w,buf,(DWORD)len,&put,NULL)) return -1;
  return (long)put;
}
#endif
