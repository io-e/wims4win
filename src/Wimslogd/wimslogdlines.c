/*    Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/* line input / output / translation routines
 * and error routines
 */
#include "wimslogd.h"

int exec_wait;

int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *arg[])
{
  pid_t pid;
  int status, t;

  fflush(NULL);      /* flush all output streams before forking
                      * otherwise they will be doubled */
  pid=fork(); if(pid==-1) return -1;
  if(!pid) {      /* child */
    char buf[4096]; int k;
    if(inf!=NULL) (void)freopen(inf,"r",stdin);
    if(outf!=NULL) (void)freopen(outf,"w",stdout);
    if(errf!=NULL) (void)freopen(errf,"w",stderr);
      /* This is to patch LinuxPPC uid wrapping
       * for scripts */
    t=0; if(strchr(cmdf,'/')) {
      FILE *tf;
      char buf[16];
      tf=fopen(cmdf,"r"); (void)fread(buf,1,10,tf); fclose(tf);
      if(memcmp(buf+1,"ELF",3)!=0) t=1;
    }
    errno=0;
    if(strchr(cmdf,'/')) execve(cmdf,arg,environ);
    else execvp(cmdf,arg);
    snprintf(buf,sizeof(buf),"Failed to execute");
    for(k=0;arg[k];k++) {
      t=strlen(buf);
      snprintf(buf+t,sizeof(buf)-t," %s",arg[k]);
    }
    t=strlen(buf);
    snprintf(buf+t,sizeof(buf)-t,"\n	%s\n",strerror(errno));
    wlogdaccessfile(buf,"a","%s/exec.fail",tmpd);
    exit(127);
  }
  else {      /* parent */
    status=0;
    if(exec_wait) {
      waitpid(pid,&status,0);
      return WEXITSTATUS(status);
    }
    else {
      exec_wait=1; addfork(pid,0); return 0;
    }
  }
}

/* my system(), but with variable parms
 * More secure than system(), and direct fork.
 */
int call_ssh(int wait,char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  char *arg[1024];
  char *inf=NULL, *outf=NULL, *errf=NULL;
  char *cmdf, *p, *p2;
  int i, d;

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  p=find_word_start(buf); if(*p==0) return 0;
  cmdf=p;
  for(i=0;*p!=0 && i<1000; p=find_word_start(p2)) {
    switch(*p) {
      case '\'': {
        p++; p2=strchr(p,'\''); if(p2==NULL) p2=p+strlen(p);
        d=0; break;
      }
      case '"': {
        p++; p2=strchr(p,'"'); if(p2==NULL) p2=p+strlen(p);
        d=0; break;
      }
      default: d=1; p2=find_word_end(p); break;
    }
    if(*p2) *p2++=0;
    if(!d) {arg[i++]=p; continue;}
    switch(*p) {
      case '<': inf=++p; break;
      case '>': {
        p++;
        if(*p=='&') {
          merge: p++; errf=outf=p; break;
        }
        else outf=p;
        break;
      }
      case '&': {
        p++; if(*p=='>') goto merge;
        else break;
      }
      case '2': {
        if(*(p+1)=='>') {errf=p+2; break;}
      }
      default: arg[i++]=p; break;
    }
  }
  arg[i]=NULL;
  exec_wait=wait;
  return execredirected(cmdf,inf,outf,errf,arg);
}

/* Read/write to a file with variable parms to print filename */
void wlogdaccessfile(char *content, char *type, char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  FILE *f;
  int l;

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  f=fopen(buf,type);
  if(f==NULL) {
    if(*type=='r') content[0]=0;
    return;
  }
  switch(*type) {
    case 'a':
    case 'w': {
      l=strlen(content); fwrite(content,1,l,f); break;
    }
    case 'r': {
      l=fread(content,1,MAX_LINELEN-1,f);
      if(l>0 && l<MAX_LINELEN) content[l]=0;
      else content[0]=0;
      _tolinux(content);
      break;
    }
    default: {
      content[0]=0; break;
    }
  }
  fclose(f);
}

/* system(), but with variable parms
 * Uses sh to execute command.
 */
int call_sh(int wait,char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  char *abuf[8];

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  abuf[0]="sh"; abuf[1]="-c"; abuf[2]=buf; abuf[3]=NULL;
  exec_wait=wait;
  return execredirected(abuf[0],NULL,NULL,NULL,abuf);
}

void wimslogd_error(char *msg)
{
  fprintf(stderr,"%s %s\n",nowstr, msg);
}

void debug(char *p,...)
{
  char lbuf[MAX_LINELEN+1];
  char *pp;
  va_list vp;

  snprintf(lbuf,sizeof(lbuf),"%s: ",nowstr);
  pp=lbuf+strlen(lbuf);
  va_start(vp,p);
  vsnprintf(pp,sizeof(lbuf)-(pp-lbuf),p,vp);
  va_end(vp);
  pp=strchr(lbuf,'\n'); if(pp) *pp=0;
  strip_trailing_spaces(lbuf); strcat(lbuf,"\n");
  wlogdaccessfile(lbuf,"a",debugfile);
}

void my_debug(char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
// better with zsh, but not always install
 call_sh(1,"echo -n '%s' >> /tmp/debug",buf);
}
