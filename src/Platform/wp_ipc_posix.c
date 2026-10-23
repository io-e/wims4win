#ifndef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include "wp_ipc.h"
extern char **environ;

int wp_ipc_transact(const char *path, char *buf, int bufsize)
{
  int sock, t, t1, l, *ip;
  struct sockaddr_un sun;
  struct timeval tv; fd_set rset;
  sock=socket(PF_UNIX,SOCK_STREAM,0); if(sock<0) return -1;
  memset(&sun,0,sizeof(sun)); sun.sun_family=AF_UNIX;
  snprintf(sun.sun_path,sizeof(sun.sun_path),"%s",path);
  if(connect(sock,(struct sockaddr*)&sun,sizeof(sun))!=0) { close(sock); return -1; }
  ip=(int*)buf; l=(int)strlen(buf+sizeof(int)); *ip=l;
  if(write(sock,buf,l+(int)sizeof(int))!=l+(int)sizeof(int)) { close(sock); return -1; }
  for(t=0,l=bufsize-1; t<l+(int)sizeof(int);) {
    tv.tv_sec=2; tv.tv_usec=0; FD_ZERO(&rset); FD_SET(sock,&rset);
    if(select(sock+1,&rset,NULL,NULL,&tv)<=0) { close(sock); return -1; }
    t1=(int)read(sock,buf+t,l+(int)sizeof(int)-t);
    if(t1+(t)<(int)sizeof(int) || t1<0) { close(sock); return -1; }
    l=*ip; if(l<=0) { close(sock); return -1; }
    t+=t1;
  }
  buf[l+(int)sizeof(int)]=0; close(sock);
  return l+(int)sizeof(int);
}

int wp_ipc_listen(wp_ipc_server *s, const char *path)
{
  struct sockaddr_un sun; struct stat st;
  snprintf(s->path,sizeof(s->path),"%s",path);
  if(stat(path,&st)==0) unlink(path);
  s->listen_fd=socket(PF_UNIX,SOCK_STREAM,0);
  if(s->listen_fd<0) return -1;
  memset(&sun,0,sizeof(sun)); sun.sun_family=AF_UNIX;
  snprintf(sun.sun_path,sizeof(sun.sun_path),"%s",path);
  if(bind(s->listen_fd,(struct sockaddr*)&sun,sizeof(sun))<0) return -1;
  if(listen(s->listen_fd,32)<0) return -1;
  return 0;
}

wp_ipc_fd wp_ipc_accept(wp_ipc_server *s, long timeout_usec)
{
  fd_set rset; struct timeval tv; int t;
  tv.tv_sec=timeout_usec/1000000; tv.tv_usec=timeout_usec%1000000;
  FD_ZERO(&rset); FD_SET(s->listen_fd,&rset);
  t=select(s->listen_fd+1,&rset,NULL,NULL,&tv);
  if(t<=0) return WP_IPC_INVALID;
  return accept(s->listen_fd,NULL,NULL);
}

int wp_ipc_read_msg(wp_ipc_fd fd, char *buf, int bufsize)
{
  int t,t2,l,*ip; struct timeval tv; fd_set rset;
  t=(int)read(fd,buf,bufsize-(int)sizeof(int)); if(t<(int)sizeof(int)) return -1;
  ip=(int*)buf; l=*ip;
  if(l<=0 || l>=bufsize-(int)sizeof(int)) return -1;
  while(t<l+(int)sizeof(int)) {
    tv.tv_sec=0; tv.tv_usec=20000; FD_ZERO(&rset); FD_SET(fd,&rset);
    if(select(fd+1,&rset,NULL,NULL,&tv)<=0) return -1;
    t2=(int)read(fd,buf+t,l+(int)sizeof(int)-t);
    if(t2<=0) return -1;
    t+=t2;
  }
  buf[l+(int)sizeof(int)]=0; return t;
}

int wp_ipc_write_msg(wp_ipc_fd fd, const char *buf, int total)
{
  return (int)write(fd,buf,total)==total?0:-1;
}
void wp_ipc_close(wp_ipc_fd fd){ if(fd>=0) close(fd); }
void wp_ipc_close_server(wp_ipc_server *s)
{ if(s->listen_fd>=0){ close(s->listen_fd); s->listen_fd=-1; } unlink(s->path); }

void wp_loadavg(char *out, size_t cap)
{
  FILE *f=fopen("/proc/loadavg","r");
  if(!f){ snprintf(out,cap,"0.00 0.00 0.00 1/1 1"); return; }
  if(!fgets(out, (int)cap, f)) snprintf(out,cap,"0.00 0.00 0.00 1/1 1");
  fclose(f);
}

int wp_daemon_spawn(char *const argv[], const char *pidfile)
{
  pid_t p1,p2; char buf[32]; int st;
  if(!argv||!argv[0]) return -1;
  p1=fork(); if(p1<0) return -1;
  if(p1>0){ waitpid(p1,&st,0); return 0; }
  p2=fork(); if(p2>0){
    snprintf(buf,sizeof(buf),"%u",(unsigned)p2);
    if(pidfile){ FILE *f=fopen(pidfile,"w"); if(f){ fputs(buf,f); fclose(f);} }
    _exit(0);
  }
  if(p2<0) _exit(1);
  execve(argv[0],argv,environ);
  _exit(127);
}
#endif
