/* Win32 implementation of wims_platform -- PHASE 2. GPL v2 or later.
 *
 * - channel: child stdin = named pipe whose parent (server) end is
 *   FILE_FLAG_OVERLAPPED, so wp_write never blocks (one write in flight,
 *   wp_poll reports writability when it completed); child stdout = anonymous
 *   pipe read after PeekNamedPipe. Parent ends are never inherited.
 * - CreateProcess(CREATE_SUSPENDED) + STARTUPINFOEX/HANDLE_LIST (only the 3
 *   std handles are inherited), AssignProcessToJobObject (CPU limit always
 *   set: PROCESS_TIME+JOB_TIME from wp_set_job or default 20s = rlimit_cpu),
 *   then ResumeThread. A watchdog thread polls job accounting (user+kernel)
 *   and wall-clock as a safety net (TerminateJobObject).
 * - wp_kill terminates the whole job (CAS + its own children).
 * - isolation:
 *    WP_ISO_DROP_PRIV: the POSIX meaning is "drop the setuid identity before
 *      exec". Windows has no setuid; the child inherits our token. Accepted
 *      only when that token is NOT elevated (nothing to drop); refused when
 *      running elevated (administrator) -> WP_E_NOISOLATION.
 *    WP_ISO_WRAPPER (ch..root sandbox): argv[0] is the wrapper; accepted
 *      only if <argv[0]>.exe exists and "ch..root.exe --selftest" succeeds
 *      (AppContainer available); otherwise refused (WP_E_NOISOLATION).
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

/* Default matches wims.c: rlimit_cpu=20 when the caller did not set a limit. */
#ifndef WP_DEFAULT_CPU_SEC
# define WP_DEFAULT_CPU_SEC 20
#endif

static long effective_cpu_sec(void)
{
  return job_cpu>0 ? job_cpu : (long)WP_DEFAULT_CPU_SEC;
}

static HANDLE make_job(void)
{
  JOBOBJECT_EXTENDED_LIMIT_INFORMATION li;
  HANDLE j=CreateJobObjectA(NULL,job_named?job_name:NULL);
  long cpu=effective_cpu_sec();
  if(j==NULL) return NULL;
  memset(&li,0,sizeof(li));
  /* Always apply a CPU limit at job creation (RLIMIT_CPU counterpart).
   * Order of use by the caller: CreateProcess(CREATE_SUSPENDED) ->
   * AssignProcessToJobObject -> ResumeThread (see spawn_core). */
  li.BasicLimitInformation.LimitFlags=JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
                                     |JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION
                                     |JOB_OBJECT_LIMIT_PROCESS_TIME
                                     |JOB_OBJECT_LIMIT_JOB_TIME;
  li.BasicLimitInformation.PerProcessUserTimeLimit.QuadPart=(LONGLONG)cpu*10000000LL;
  li.BasicLimitInformation.PerJobUserTimeLimit.QuadPart=(LONGLONG)cpu*10000000LL;
  if(job_mem>0) {
    li.BasicLimitInformation.LimitFlags|=JOB_OBJECT_LIMIT_PROCESS_MEMORY;
    li.ProcessMemoryLimit=(SIZE_T)job_mem*1024*1024;
  }
  if(!SetInformationJobObject(j,JobObjectExtendedLimitInformation,&li,sizeof(li))) {
    CloseHandle(j); return NULL;
  }
  return j;
}

/* Watchdog: poll JOBOBJECT_BASIC_ACCOUNTING_INFORMATION (user+kernel).
 * If the sum exceeds the CPU budget, TerminateJobObject. Also kill on
 * wall-clock overrun (2x budget) when accounting stays at 0 (observed on
 * some Win11 hosts for tight busy loops). */
typedef struct {
  HANDLE job; HANDLE proc; long cpu_sec; HANDLE thread;
} wp_watch_arg;

static DWORD WINAPI wp_cpu_watch(LPVOID arg)
{
  wp_watch_arg *a=(wp_watch_arg*)arg;
  LONGLONG budget=(LONGLONG)a->cpu_sec*10000000LL;
  DWORD t0=GetTickCount(), wall_ms=(DWORD)a->cpu_sec*2000; /* 2x wall grace */
  JOBOBJECT_BASIC_ACCOUNTING_INFORMATION ai;
  for(;;){
    if(WaitForSingleObject(a->proc,200)==WAIT_OBJECT_0) break;
    memset(&ai,0,sizeof(ai));
    if(QueryInformationJobObject(a->job,JobObjectBasicAccountingInformation,
                                 &ai,sizeof(ai),NULL)){
      LONGLONG used=ai.TotalUserTime.QuadPart+ai.TotalKernelTime.QuadPart;
      if(used>=budget){ TerminateJobObject(a->job,137); break; }
    }
    if(GetTickCount()-t0>=wall_ms){ TerminateJobObject(a->job,137); break; }
  }
  CloseHandle(a->proc); CloseHandle(a->job); free(a);
  return 0;
}

static void start_cpu_watch(HANDLE job, HANDLE proc, long cpu_sec)
{
  wp_watch_arg *a; HANDLE th; DWORD id;
  if(!job || !proc || cpu_sec<=0) return;
  a=(wp_watch_arg*)malloc(sizeof(*a)); if(!a) return;
  a->job=NULL; a->proc=NULL; a->cpu_sec=cpu_sec;
  if(!DuplicateHandle(GetCurrentProcess(),job,GetCurrentProcess(),&a->job,
                      0,FALSE,DUPLICATE_SAME_ACCESS) ||
     !DuplicateHandle(GetCurrentProcess(),proc,GetCurrentProcess(),&a->proc,
                      0,FALSE,DUPLICATE_SAME_ACCESS)){
    if(a->job) CloseHandle(a->job);
    if(a->proc) CloseHandle(a->proc);
    free(a); return;
  }
  a->cpu_sec=cpu_sec;
  th=CreateThread(NULL,0,wp_cpu_watch,a,0,&id);
  if(th) CloseHandle(th); /* thread frees a and duplicated handles */
  else { CloseHandle(a->job); CloseHandle(a->proc); free(a); }
}

int wp_sandbox_available(const char *wrapper);
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

/* Common Win32 spawn on explicit handles (owned by the caller). */
/* "<name>.exe" when <name> itself is not a file but <name>.exe is. */
int wp_resolve_exe(const char *name, char *out, size_t cap)
{
  size_t l; DWORD a;
  if(name==NULL) return 0;
  l=strlen(name);
  if(l>=4 && _stricmp(name+l-4,".exe")==0) return 0;
  a=GetFileAttributesA(name);
  if(a!=INVALID_FILE_ATTRIBUTES && !(a&FILE_ATTRIBUTE_DIRECTORY)) return 0;
  if(l+5>cap) return 0;
  snprintf(out,cap,"%s.exe",name);
  a=GetFileAttributesA(out);
  return a!=INVALID_FILE_ATTRIBUTES && !(a&FILE_ATTRIBUTE_DIRECTORY);
}

/* "#!" scripts (module .sh, other/bin scripts): CreateProcess cannot run
 * them, so return the interpreter basename ("sh", "perl", "bash"...), found
 * through PATH (other/msys2/usr/bin), and its optional single argument. */
int wp_script_interp(const char *name, char *interp, size_t cap,
                     char *arg, size_t acap)
{
  FILE *f; char line[256], *p, *q, *b;
  if(name==NULL) return 0;
  f=fopen(name,"rb"); if(f==NULL) return 0;
  if(fgets(line,sizeof(line),f)==NULL) { fclose(f); return 0; }
  fclose(f);
  if(line[0]!='#' || line[1]!='!') return 0;
  p=line+2; while(*p==' '||*p=='\t') p++;
  q=p; while(*q && *q!=' ' && *q!='\t' && *q!='\r' && *q!='\n') q++;
  if(q==p) return 0;
  b=q; while(b>p && b[-1]!='/' && b[-1]!='\\') b--;
  if((size_t)(q-b)+1>cap) return 0;
  memcpy(interp,b,q-b); interp[q-b]=0;
  if(arg && acap) {
    arg[0]=0;
    while(*q==' '||*q=='\t') q++;
    p=q; while(*q && *q!=' ' && *q!='\t' && *q!='\r' && *q!='\n') q++;
    if(q>p && (size_t)(q-p)+1<=acap) { memcpy(arg,p,q-p); arg[q-p]=0; }
  }
  /* "#!/usr/bin/env perl": run the named program directly */
  if(strcmp(interp,"env")==0 && arg && arg[0]) {
    snprintf(interp,cap,"%s",arg); arg[0]=0;
  }
  return 1;
}

/* Shell utilities WIMS calls through call_ssh() ("mv a b", "rm -f a"),
 * done in-process on Windows (no sh/coreutils). Returns 1 if handled. */
int wp_builtin(char *const arg[], int *status)
{
  int i, k;
  if(arg==NULL || arg[0]==NULL) return 0;
  if(strcmp(arg[0],"mv")==0) {
    char dst[MAX_PATH]; DWORD a;
    for(k=0; arg[k]; k++); /* argc */
    i=1; while(arg[i] && arg[i][0]=='-' && arg[i][1]) i++;
    if(k-i<2) { *status=1; return 1; }
    *status=0;
    a=GetFileAttributesA(arg[k-1]);
    for(; i<k-1; i++) {
      if(a!=INVALID_FILE_ATTRIBUTES && (a&FILE_ATTRIBUTE_DIRECTORY)) {
        const char *b=arg[i]+strlen(arg[i]);
        while(b>arg[i] && b[-1]!='/' && b[-1]!='\\') b--;
        snprintf(dst,sizeof(dst),"%s/%s",arg[k-1],b);
      } else snprintf(dst,sizeof(dst),"%s",arg[k-1]);
      if(!MoveFileExA(arg[i],dst,MOVEFILE_REPLACE_EXISTING|MOVEFILE_COPY_ALLOWED)) *status=1;
    }
    return 1;
  }
  if(strcmp(arg[0],"rm")==0) {
    *status=0;
    for(i=1; arg[i]; i++) if(arg[i][0]!='-') DeleteFileA(arg[i]);
    return 1;
  }
  return 0;
}

static int spawn_core(char *const argv[], HANDLE hin, HANDLE hout, HANDLE herr,
                      wp_isolation iso, int lowprio, const char *progname,
                      wp_pid *pid)
{
  char cmd[32768]; size_t n=0; int i, ok, nl;
  HANDLE list[3], job;
  STARTUPINFOEXA si; PROCESS_INFORMATION pi; SIZE_T asz=0;
  if(argv==NULL || argv[0]==NULL || pid==NULL) return WP_E_ARG;
  if(iso==WP_ISO_WRAPPER && !wp_sandbox_available(argv[0])) return WP_E_NOISOLATION;
  if(iso==WP_ISO_DROP_PRIV && token_elevated()) return WP_E_NOISOLATION;
  for(i=0;argv[i];i++) {
    const char *a=argv[i]; char exe0[MAX_PATH];
    if(i) { if(n+1<sizeof(cmd)) cmd[n]=' '; n++; }
    /* WIMS names helpers without extension (bin/pari, bin/insdraw..processor):
     * CreateProcess only appends ".exe" when the name has no dot. */
    if(i==0 && wp_resolve_exe(a,exe0,sizeof(exe0))) a=exe0;
    else if(i==0) {
      char in[MAX_PATH], ia[MAX_PATH];
      if(wp_script_interp(a,in,sizeof(in),ia,sizeof(ia))) {
        n+=quote_arg(cmd+n, n<sizeof(cmd)?sizeof(cmd)-n:0, in);
        if(n+1<sizeof(cmd)) cmd[n]=' '; n++;
        if(ia[0]) {
          n+=quote_arg(cmd+n, n<sizeof(cmd)?sizeof(cmd)-n:0, ia);
          if(n+1<sizeof(cmd)) cmd[n]=' '; n++;
        }
      }
    }
    n+=quote_arg(cmd+n, n<sizeof(cmd)?sizeof(cmd)-n:0, a);
  }
  if(n>=sizeof(cmd)) return WP_E_ARG;
  cmd[n]=0;
  if(hin==INVALID_HANDLE_VALUE || hout==INVALID_HANDLE_VALUE
     || herr==INVALID_HANDLE_VALUE) return WP_E_SYS;
  nl=0; list[nl++]=hin;
  if(hout!=hin) list[nl++]=hout;
  if(herr!=hin && herr!=hout) list[nl++]=herr;
  memset(&si,0,sizeof(si)); si.StartupInfo.cb=sizeof(si);
  si.StartupInfo.dwFlags=STARTF_USESTDHANDLES;
  si.StartupInfo.hStdInput=hin; si.StartupInfo.hStdOutput=hout;
  si.StartupInfo.hStdError=herr;
  InitializeProcThreadAttributeList(NULL,1,0,&asz);
  si.lpAttributeList=(LPPROC_THREAD_ATTRIBUTE_LIST)malloc(asz);
  if(!si.lpAttributeList) return WP_E_SYS;
  InitializeProcThreadAttributeList(si.lpAttributeList,1,0,&asz);
  UpdateProcThreadAttribute(si.lpAttributeList,0,PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
                            list,nl*sizeof(HANDLE),NULL,NULL);
  job=make_job();
  ok=job!=NULL && CreateProcessA(NULL,cmd,NULL,NULL,TRUE,
                    CREATE_NO_WINDOW|EXTENDED_STARTUPINFO_PRESENT|CREATE_SUSPENDED
                    |(lowprio?BELOW_NORMAL_PRIORITY_CLASS:0),
                    NULL,NULL,&si.StartupInfo,&pi);
  if(ok && !AssignProcessToJobObject(job,pi.hProcess)) {
    /* never run a CAS outside its job (no limits, no cleanup) */
    TerminateProcess(pi.hProcess,127); CloseHandle(pi.hThread);
    CloseHandle(pi.hProcess); ok=0;
  }
  DeleteProcThreadAttributeList(si.lpAttributeList); free(si.lpAttributeList);
  if(!ok) {
    if(job) CloseHandle(job);
    /* historical behaviour of the forked child when exec fails */
    if(progname) { fprintf(stderr,"%s not_INStalled",progname); fflush(stderr); }
    return WP_E_SYS;
  }
  /* Limits are already on the job; child was suspended until Assign. */
  ResumeThread(pi.hThread); CloseHandle(pi.hThread);
  start_cpu_watch(job, pi.hProcess, effective_cpu_sec());
  pid->h=pi.hProcess; pid->id=pi.dwProcessId; pid->job=job;
  return WP_OK;
}

int wp_spawn(wp_chan *c, char *const argv[], const char *inf,
             const char *outf, const char *errf, wp_isolation iso,
             const char *progname, wp_pid *pid)
{
  HANDLE hin, hout, herr; int r;
  if(argv==NULL || argv[0]==NULL || pid==NULL) return WP_E_ARG;
  hin = inf ? open_redirect(inf,0) : c->in_r;
  hout= outf? open_redirect(outf,1): c->out_w;
  herr= errf? open_redirect(errf,1): GetStdHandle(STD_ERROR_HANDLE);
  r=spawn_core(argv,hin,hout,herr,iso,0,progname,pid);
  if(inf && hin!=INVALID_HANDLE_VALUE) CloseHandle(hin);
  if(outf && hout!=INVALID_HANDLE_VALUE) CloseHandle(hout);
  if(errf && herr!=INVALID_HANDLE_VALUE) CloseHandle(herr);
  return r;
}

int wp_spawn_mx(wp_chan *c, char *const argv[], wp_fd err_w, wp_isolation iso,
                int lowprio, const char *progname, wp_pid *pid)
{
  return spawn_core(argv,c->in_r,c->out_w,err_w,iso,lowprio,progname,pid);
}

int wp_pipe_open(wp_fd *r, wp_fd *w)
{
  SECURITY_ATTRIBUTES sa;
  sa.nLength=sizeof(sa); sa.lpSecurityDescriptor=NULL; sa.bInheritHandle=TRUE;
  if(!CreatePipe(r,w,&sa,0)) return WP_E_SYS;
  SetHandleInformation(*r,HANDLE_FLAG_INHERIT,0);
  return WP_OK;
}

long wp_fd_read(wp_fd fd, char *buf, long cap)
{
  DWORD got=0;
  if(cap<=0) return 0;
  if(!ReadFile(fd,buf,(DWORD)cap,&got,NULL))
    return GetLastError()==ERROR_BROKEN_PIPE?0:-1;
  return (long)got;
}

/* Sandbox wrapper (ch..root.exe) is usable iff it exists and its self-test
 * (AppContainer creation + restricted launch) succeeds on this machine. */
int wp_sandbox_available(const char *wrapper)
{
  static int cached=-1; char cmd[1024]; STARTUPINFOA si; PROCESS_INFORMATION pi;
  DWORD code=1; char path[MAX_PATH]; size_t l;
  if(cached>=0) return cached;
  cached=0;
  if(wrapper==NULL) return 0;
  snprintf(path,sizeof(path),"%s",wrapper); l=strlen(path);
  if(l<4 || _stricmp(path+l-4,".exe")!=0) snprintf(path+l,sizeof(path)-l,".exe");
  if(GetFileAttributesA(path)==INVALID_FILE_ATTRIBUTES) return 0;
  snprintf(cmd,sizeof(cmd),"\"%s\" --selftest",path);
  memset(&si,0,sizeof(si)); si.cb=sizeof(si);
  if(!CreateProcessA(NULL,cmd,NULL,NULL,FALSE,CREATE_NO_WINDOW,NULL,NULL,&si,&pi)) return 0;
  if(WaitForSingleObject(pi.hProcess,15000)==WAIT_OBJECT_0) GetExitCodeProcess(pi.hProcess,&code);
  else TerminateProcess(pi.hProcess,1);
  CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
  cached=(code==0);
  return cached;
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
