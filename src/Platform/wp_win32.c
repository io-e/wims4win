/* Win32 implementation of wims_platform -- PHASE 2. GPL v2 or later.
 *
 * - channel: child stdin = named pipe whose parent (server) end is
 *   FILE_FLAG_OVERLAPPED, so wp_write never blocks (one write in flight,
 *   wp_poll reports writability when it completed); child stdout = anonymous
 *   pipe read after PeekNamedPipe. Parent ends are never inherited.
 * - CreateProcess(CREATE_SUSPENDED) + STARTUPINFOEX/HANDLE_LIST (only the 3
 *   std handles are inherited), child assigned to a Job Object
 *   (KILL_ON_JOB_CLOSE, optional CPU / memory limits, optional name so that
 *   wims can kill a whole session: replaces exec.pid), then resumed.
 * - wp_kill terminates the whole job (CAS + its own children).
 * - isolation:
 *    WP_ISO_DROP_PRIV: the POSIX meaning is "drop the setuid identity before
 *      exec". Windows has no setuid; the child inherits our token. Accepted
 *      only when that token is NOT elevated (nothing to drop); refused when
 *      running elevated (administrator) -> WP_E_NOISOLATION.
 *    WP_ISO_WRAPPER (ch..root sandbox): no Win32 sandbox yet -> refused.
 */
#ifdef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "wims_platform.h"

const char *wp_platform_name(void) { return "win32"; }

static char job_name[200];
static int job_named=0;
static long job_cpu=0, job_mem=0;

void wp_set_job(const char *name, long cpu_sec, long mem_mb)
{
  int i=0; const char *p;
  job_named=0; job_cpu=cpu_sec; job_mem=mem_mb;
  if(name==NULL || *name==0) return;
  i=snprintf(job_name,sizeof(job_name),"Local\\wims-job-");
  for(p=name; *p && i<(int)sizeof(job_name)-1; p++)
    job_name[i++]=(isalnum((unsigned char)*p)||*p=='-'||*p=='_')?*p:'_';
  job_name[i]=0; job_named=1;
}

int wp_job_kill(const char *name)
{
  HANDLE j; char save[sizeof(job_name)]; int named=job_named; long c=job_cpu, m=job_mem;
  memcpy(save,job_name,sizeof(save));
  wp_set_job(name,0,0);
  j=job_named?OpenJobObjectA(JOB_OBJECT_TERMINATE,FALSE,job_name):NULL;
  memcpy(job_name,save,sizeof(save)); job_named=named; job_cpu=c; job_mem=m;
  if(j==NULL) return 0;
  TerminateJobObject(j,137); CloseHandle(j);
  return 1;
}

static HANDLE make_job(void)
{
  JOBOBJECT_EXTENDED_LIMIT_INFORMATION li;
  HANDLE j=CreateJobObjectA(NULL,job_named?job_name:NULL);
  if(j==NULL) return NULL;
  memset(&li,0,sizeof(li));
  li.BasicLimitInformation.LimitFlags=JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
                                     |JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION;
  if(job_cpu>0) {
    li.BasicLimitInformation.LimitFlags|=JOB_OBJECT_LIMIT_PROCESS_TIME;
    li.BasicLimitInformation.PerProcessUserTimeLimit.QuadPart=(LONGLONG)job_cpu*10000000;
  }
  if(job_mem>0) {
    li.BasicLimitInformation.LimitFlags|=JOB_OBJECT_LIMIT_PROCESS_MEMORY;
    li.ProcessMemoryLimit=(SIZE_T)job_mem*1024*1024;
  }
  if(!SetInformationJobObject(j,JobObjectExtendedLimitInformation,&li,sizeof(li))) {
    CloseHandle(j); return NULL;
  }
  return j;
}

static int token_elevated(void)
{
  HANDLE t; TOKEN_ELEVATION e; DWORD n=0; int r=1; /* unknown = elevated (refuse) */
  if(!OpenProcessToken(GetCurrentProcess(),TOKEN_QUERY,&t)) return 1;
  if(GetTokenInformation(t,TokenElevation,&e,sizeof(e),&n)) r=e.TokenIsElevated!=0;
  CloseHandle(t);
  return r;
}

int wp_chan_open(wp_chan *c)
{
  SECURITY_ATTRIBUTES sa; char name[96]; static LONG seq=0;
  memset(c,0,sizeof(*c));
  sa.nLength=sizeof(sa); sa.lpSecurityDescriptor=NULL; sa.bInheritHandle=TRUE;
  snprintf(name,sizeof(name),"\\\\.\\pipe\\wims-%lu-%ld",
           GetCurrentProcessId(),(long)InterlockedIncrement(&seq));
  c->in_w=CreateNamedPipeA(name,PIPE_ACCESS_OUTBOUND|FILE_FLAG_OVERLAPPED
                           |FILE_FLAG_FIRST_PIPE_INSTANCE,
                           PIPE_TYPE_BYTE|PIPE_WAIT|PIPE_REJECT_REMOTE_CLIENTS,
                           1,65536,65536,0,NULL);   /* not inheritable */
  if(c->in_w==INVALID_HANDLE_VALUE) return WP_E_SYS;
  c->in_r=CreateFileA(name,GENERIC_READ,0,&sa,OPEN_EXISTING,0,NULL);
  if(c->in_r==INVALID_HANDLE_VALUE) { CloseHandle(c->in_w); return WP_E_SYS; }
  if(!CreatePipe(&c->out_r,&c->out_w,&sa,0)) {
    CloseHandle(c->in_r); CloseHandle(c->in_w); return WP_E_SYS;
  }
  SetHandleInformation(c->out_r,HANDLE_FLAG_INHERIT,0);
  c->wov.hEvent=CreateEventA(NULL,TRUE,TRUE,NULL);
  c->wbuf=NULL; c->wpending=0;
  return WP_OK;
}

/* 1 if the in-flight write (if any) is finished */
static int write_done(wp_chan *c, DWORD ms)
{
  DWORD n;
  if(!c->wpending) return 1;
  if(WaitForSingleObject(c->wov.hEvent,ms)!=WAIT_OBJECT_0) return 0;
  GetOverlappedResult(c->in_w,&c->wov,&n,FALSE);
  free(c->wbuf); c->wbuf=NULL; c->wpending=0;
  return 1;
}

void wp_chan_close_parent_write(wp_chan *c)
{
  if(c->in_w==INVALID_HANDLE_VALUE || c->in_w==NULL) return;
  if(!write_done(c,5000)) { CancelIoEx(c->in_w,&c->wov); write_done(c,INFINITE); }
  CloseHandle(c->in_w);
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
  HANDLE job;
  if(iso==WP_ISO_WRAPPER) return WP_E_NOISOLATION;          /* no sandbox yet */
  if(iso==WP_ISO_DROP_PRIV && token_elevated()) return WP_E_NOISOLATION;
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
  job=make_job();
  ok=job!=NULL && CreateProcessA(NULL,cmd,NULL,NULL,TRUE,
                    CREATE_NO_WINDOW|EXTENDED_STARTUPINFO_PRESENT|CREATE_SUSPENDED,
                    NULL,NULL,&si.StartupInfo,&pi);
  if(ok && !AssignProcessToJobObject(job,pi.hProcess)) {
    /* never run a CAS outside its job (no limits, no cleanup) */
    TerminateProcess(pi.hProcess,127); CloseHandle(pi.hThread);
    CloseHandle(pi.hProcess); ok=0;
  }
  DeleteProcThreadAttributeList(si.lpAttributeList); free(si.lpAttributeList);
  if(inf) CloseHandle(hin);
  if(outf) CloseHandle(hout);
  if(errf) CloseHandle(herr);
  if(!ok) {
    if(job) CloseHandle(job);
    /* historical behaviour of the forked child when exec fails */
    if(progname) { fprintf(stderr,"%s not_INStalled",progname); fflush(stderr); }
    return WP_E_SYS;
  }
  ResumeThread(pi.hThread); CloseHandle(pi.hThread);
  pid->h=pi.hProcess; pid->id=pi.dwProcessId; pid->job=job;
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

int wp_kill(wp_pid *pid)
{
  if(pid->job && TerminateJobObject(pid->job,137)) return 0;
  return TerminateProcess(pid->h,137)?0:-1;
}

int wp_poll(wp_chan *c, int want_read, int want_write, long timeout_usec,
            int *can_read, int *can_write)
{
  DWORD avail=0, waited=0, step=10, limit=(DWORD)(timeout_usec/1000);
  int r=0, w=0;
  if(step>limit && limit>0) step=limit;
  for(;;) {
    r=0;
    if(want_read) {
      if(!PeekNamedPipe(c->out_r,NULL,0,NULL,&avail,NULL)) avail=1; /* EOF/err: let read report it */
      if(avail) r=1;
    }
    w=want_write && write_done(c,0);
    if(r || w || waited>=limit) break;
    Sleep(step); waited+=step;
  }
  if(can_read) *can_read=r;
  if(can_write) *can_write=w;
  return r || w;
}

long wp_read(wp_chan *c, char *buf, long cap)
{
  DWORD got=0;
  if(cap<=0) return 0;
  if(!ReadFile(c->out_r,buf,(DWORD)cap,&got,NULL))
    return GetLastError()==ERROR_BROKEN_PIPE?0:-1;
  return (long)got;
}

/* Overlapped write: the data is copied and handed to the kernel; the call
 * returns at once with len (accepted). If a previous write is still in
 * flight, returns 0 (nothing accepted, poll again) -- never blocks. */
long wp_write(wp_chan *c, const char *buf, long len)
{
  if(len<=0) return 0;
  if(!write_done(c,0)) return 0;
  c->wbuf=(char*)malloc(len); if(!c->wbuf) return -1;
  memcpy(c->wbuf,buf,len);
  ResetEvent(c->wov.hEvent);
  c->wov.Offset=c->wov.OffsetHigh=0;
  if(WriteFile(c->in_w,c->wbuf,(DWORD)len,NULL,&c->wov)
     || GetLastError()==ERROR_IO_PENDING) {
    c->wpending=1; return len;
  }
  free(c->wbuf); c->wbuf=NULL;
  return -1;
}
int wp_fd_readable(wp_fd fd, long timeout_usec)
{
  DWORD avail=0, waited=0, limit=(DWORD)(timeout_usec/1000);
  for(;;) {
    if(!PeekNamedPipe(fd,NULL,0,NULL,&avail,NULL)) return 1; /* EOF/error */
    if(avail) return 1;
    if(waited>=limit) return 0;
    Sleep(10); waited+=10;
  }
}
#endif
