#ifdef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include "wp_ipc.h"

/* Map historical sock path to a local named pipe. */
static void pipe_name(char *out, size_t cap, const char *path)
{
  const char *base=path; const char *p;
  for(p=path; *p; p++) if(*p=='/'||*p=='\\') base=p+1;
  if(!*base) base="wims-logd";
  snprintf(out,cap,"\\\\.\\pipe\\wims-%s",base[0]=='.'?base+1:base);
}

static int write_all(HANDLE h, const char *buf, int n)
{
  int o=0; DWORD w;
  while(o<n){
    if(!WriteFile(h,buf+o,(DWORD)(n-o),&w,NULL) || w==0) return -1;
    o+=(int)w;
  }
  return 0;
}
static int read_all(HANDLE h, char *buf, int n)
{
  int o=0; DWORD r;
  while(o<n){
    if(!ReadFile(h,buf+o,(DWORD)(n-o),&r,NULL) || r==0) return -1;
    o+=(int)r;
  }
  return 0;
}

int wp_ipc_transact(const char *path, char *buf, int bufsize)
{
  char name[280]; HANDLE h; int l,*ip, total;
  pipe_name(name,sizeof(name),path);
  h=CreateFileA(name,GENERIC_READ|GENERIC_WRITE,0,NULL,OPEN_EXISTING,0,NULL);
  if(h==INVALID_HANDLE_VALUE){
    /* wait briefly if daemon is starting */
    if(!WaitNamedPipeA(name,2000)) return -1;
    h=CreateFileA(name,GENERIC_READ|GENERIC_WRITE,0,NULL,OPEN_EXISTING,0,NULL);
    if(h==INVALID_HANDLE_VALUE) return -1;
  }
  ip=(int*)buf; l=(int)strlen(buf+sizeof(int)); *ip=l;
  if(write_all(h,buf,l+(int)sizeof(int))<0){ CloseHandle(h); return -1; }
  if(read_all(h,buf,(int)sizeof(int))<0){ CloseHandle(h); return -1; }
  l=*ip; if(l<=0 || l>=bufsize-(int)sizeof(int)){ CloseHandle(h); return -1; }
  if(read_all(h,buf+sizeof(int),l)<0){ CloseHandle(h); return -1; }
  buf[l+(int)sizeof(int)]=0; CloseHandle(h);
  total=l+(int)sizeof(int); return total;
}

int wp_ipc_listen(wp_ipc_server *s, const char *path)
{
  char name[280];
  pipe_name(name,sizeof(name),path);
  snprintf(s->path,sizeof(s->path),"%s",name);
  /* Create the first instance; more instances created on each accept. */
  s->listen_fd=CreateNamedPipeA(name,
    PIPE_ACCESS_DUPLEX|FILE_FLAG_OVERLAPPED,
    PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT,
    PIPE_UNLIMITED_INSTANCES, 65536, 65536, 0, NULL);
  if(s->listen_fd==INVALID_HANDLE_VALUE) return -1;
  return 0;
}

wp_ipc_fd wp_ipc_accept(wp_ipc_server *s, long timeout_usec)
{
  OVERLAPPED ov; DWORD ms=(DWORD)(timeout_usec/1000); HANDLE ev;
  BOOL ok; wp_ipc_fd client;
  memset(&ov,0,sizeof(ov));
  ev=CreateEventA(NULL,TRUE,FALSE,NULL); ov.hEvent=ev;
  ok=ConnectNamedPipe(s->listen_fd,&ov);
  if(!ok){
    DWORD e=GetLastError();
    if(e==ERROR_PIPE_CONNECTED) ok=TRUE;
    else if(e==ERROR_IO_PENDING){
      if(WaitForSingleObject(ev,ms==0?1:ms)!=WAIT_OBJECT_0){
        CancelIo(s->listen_fd); CloseHandle(ev); return WP_IPC_INVALID;
      }
      ok=TRUE;
    }
  }
  CloseHandle(ev);
  if(!ok) return WP_IPC_INVALID;
  client=s->listen_fd;
  /* prepare next instance for subsequent accepts */
  s->listen_fd=CreateNamedPipeA(s->path,
    PIPE_ACCESS_DUPLEX|FILE_FLAG_OVERLAPPED,
    PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT,
    PIPE_UNLIMITED_INSTANCES, 65536, 65536, 0, NULL);
  return client;
}

int wp_ipc_read_msg(wp_ipc_fd fd, char *buf, int bufsize)
{
  int l,*ip;
  if(read_all(fd,buf,(int)sizeof(int))<0) return -1;
  ip=(int*)buf; l=*ip;
  if(l<=0 || l>=bufsize-(int)sizeof(int)) return -1;
  if(read_all(fd,buf+sizeof(int),l)<0) return -1;
  buf[l+(int)sizeof(int)]=0; return l+(int)sizeof(int);
}

int wp_ipc_write_msg(wp_ipc_fd fd, const char *buf, int total)
{ return write_all(fd,buf,total); }

void wp_ipc_close(wp_ipc_fd fd)
{
  if(fd!=INVALID_HANDLE_VALUE){
    FlushFileBuffers(fd); DisconnectNamedPipe(fd); CloseHandle(fd);
  }
}
void wp_ipc_close_server(wp_ipc_server *s)
{ if(s->listen_fd!=INVALID_HANDLE_VALUE){ CloseHandle(s->listen_fd); s->listen_fd=INVALID_HANDLE_VALUE; } }

void wp_loadavg(char *out, size_t cap)
{
  /* Approximate 1/5/15 load with a single busy-ness ratio from idle ticks. */
  static unsigned long long pIdle=0,pKernel=0,pUser=0;
  FILETIME id,kn,us; unsigned long long i,k,u,di,dk,du,dt; double load=0;
  if(GetSystemTimes(&id,&kn,&us)){
    i=((unsigned long long)id.dwHighDateTime<<32)|id.dwLowDateTime;
    k=((unsigned long long)kn.dwHighDateTime<<32)|kn.dwLowDateTime;
    u=((unsigned long long)us.dwHighDateTime<<32)|us.dwLowDateTime;
    if(pKernel){
      di=i-pIdle; dk=k-pKernel; du=u-pUser; dt=(dk+du);
      if(dt>0) load=1.0 - (double)di/(double)dt;
      if(load<0) load=0; if(load>64) load=64;
    }
    pIdle=i; pKernel=k; pUser=u;
  }
  snprintf(out,cap,"%.2f %.2f %.2f 1/1 %lu",load,load,load,GetCurrentProcessId());
}

int wp_daemon_spawn(char *const argv[], const char *pidfile)
{
  char cmd[32768]; size_t n=0; int i;
  STARTUPINFOA si; PROCESS_INFORMATION pi; BOOL ok;
  if(!argv||!argv[0]) return -1;
  for(i=0;argv[i];i++){
    if(i){ if(n+1<sizeof(cmd)) cmd[n++]=' '; }
    { size_t L=strlen(argv[i]); if(n+L+3>=sizeof(cmd)) return -1;
      if(strpbrk(argv[i]," \t")){ cmd[n++]='"'; memcpy(cmd+n,argv[i],L); n+=L; cmd[n++]='"'; }
      else { memcpy(cmd+n,argv[i],L); n+=L; } }
  }
  cmd[n]=0;
  memset(&si,0,sizeof(si)); si.cb=sizeof(si);
  si.dwFlags=STARTF_USESTDHANDLES;
  si.hStdInput=GetStdHandle(STD_INPUT_HANDLE);
  si.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE);
  si.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  ok=CreateProcessA(NULL,cmd,NULL,NULL,TRUE,CREATE_NO_WINDOW|DETACHED_PROCESS,NULL,NULL,&si,&pi);
  if(!ok) return -1;
  if(pidfile){
    char b[32]; FILE *f; snprintf(b,sizeof(b),"%lu",(unsigned long)pi.dwProcessId);
    f=fopen(pidfile,"w"); if(f){ fputs(b,f); fclose(f); }
  }
  CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
  return 0;
}
#endif
