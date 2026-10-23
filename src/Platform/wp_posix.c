/* POSIX implementation of wims_platform. GPL v2 or later.
 * Behaviour mirrors the historical code of Interfaces/common.c exactly. */
#ifndef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <sys/select.h>
#include "wims_platform.h"

const char *wp_platform_name(void) { return "posix"; }

int wp_chan_open(wp_chan *c)
{
  int pi[2], po[2];
  if(pipe(pi)<0) return WP_E_SYS;
  if(pipe(po)<0) { close(pi[0]); close(pi[1]); return WP_E_SYS; }
  c->in_r=pi[0]; c->in_w=pi[1]; c->out_r=po[0]; c->out_w=po[1];
  return WP_OK;
}

void wp_chan_close_parent_write(wp_chan *c)
{
  if(c->in_w>=0) close(c->in_w);
  c->in_w=-1;
}

int wp_spawn(wp_chan *c, char *const argv[], const char *inf,
             const char *outf, const char *errf, wp_isolation iso,
             const char *progname, wp_pid *pid)
{
  pid_t p;
  if(argv==NULL || argv[0]==NULL || pid==NULL) return WP_E_ARG;
  fflush(NULL); /* otherwise buffered output is doubled */
  p=fork(); if(p==-1) return WP_E_SYS;
  if(p==0) { /* child */
    if(!inf) { dup2(c->in_r,0); close(c->in_w); }
    else if(freopen(inf,"r",stdin)==NULL) fprintf(stderr,"freopen failed");
    if(!outf) { dup2(c->out_w,1); close(c->out_r); }
    else if(freopen(outf,"w",stdout)==NULL) fprintf(stderr,"freopen failed");
    if(errf && freopen(errf,"w",stderr)==NULL) fprintf(stderr,"freopen failed");
    if(iso==WP_ISO_DROP_PRIV) {
      if(setreuid(getuid(),getuid()) || setregid(getgid(),getgid())) _exit(127);
    }
    if(strchr(argv[0],'/')) execv(argv[0],argv);
    else execvp(argv[0],argv);
    fprintf(stderr,"%s not_INStalled",progname?progname:"");
    exit(127);
  }
  pid->id=p;
  return WP_OK;
}

int wp_wait(wp_pid *pid, int *status)
{
  int st=0;
  if(waitpid(pid->id,&st,0)==pid->id) { if(status) *status=st; return 1; }
  return 0;
}

int wp_alive(wp_pid *pid)
{
  if(kill(pid->id,0)<0 && errno==ESRCH) return 0;
  return 1;
}

int wp_kill(wp_pid *pid) { return kill(pid->id,SIGKILL); }

int wp_poll(wp_chan *c, int want_read, int want_write, long timeout_usec,
            int *can_read, int *can_write)
{
  fd_set rset, wset;
  struct timeval t;
  int n=-1, i;
  t.tv_sec=timeout_usec/1000000; t.tv_usec=timeout_usec%1000000;
  FD_ZERO(&rset); FD_ZERO(&wset);
  if(want_read) { FD_SET(c->out_r,&rset); if(c->out_r>n) n=c->out_r; }
  if(want_write) { FD_SET(c->in_w,&wset); if(c->in_w>n) n=c->in_w; }
  i=select(n+1,&rset,&wset,NULL,&t);
  if(can_read) *can_read = (i>0 && want_read && FD_ISSET(c->out_r,&rset));
  if(can_write) *can_write = (i>0 && want_write && FD_ISSET(c->in_w,&wset));
  return i;
}

long wp_read(wp_chan *c, char *buf, long cap) { return read(c->out_r,buf,cap); }
long wp_write(wp_chan *c, const char *buf, long len) { return write(c->in_w,buf,len); }
#endif
