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
/* Routines to write log files. */

#include "wimslogd.h"

char llbuf[4*MAX_LINELEN+4], *llptr;
char *linetab[MAX_LOGLINES];
int loglinecnt;
char *logbuf, *logfname;

int llcmp(const void *c1, const void *c2)
{
  char **p1,**p2;
  p1=(char **) c1; p2=(char **) c2;
  return strcmp(*p1,*p2);
}

void onelogfile(char *fname)
{
  char namebuf[1024], b3[1024], b4[1024], *pp;
  FILE *f;
  size_t l, l2;
  struct stat st;
  int i, lim;

  if(fname[0] == 0) {
    abort:
    llptr=llbuf; return;
  }
  if(llptr<=llbuf) goto abort;
  if(strstr(fname,"..")!=NULL) goto abort;
  for(pp=fname; *pp; pp++) if(*pp<'-' || *pp>'z' || strchr(":;=<>?\\^[]",*pp)!=NULL) goto abort;
  snprintf(namebuf,sizeof(namebuf),"%s/%s",logd,fname);
  pp=strrchr(namebuf,'/'); if(pp>namebuf+strlen(logd)) {
    *pp=0; mkdirs(namebuf); *pp='/';
  }
  f=fopen(namebuf,"a"); if(f==NULL) goto abort;
  l2=ftell(f);
  fwrite(llbuf,1,llptr-llbuf,f); l=ftell(f); fclose(f); llptr=llbuf;
  if(l2==0) chmod(namebuf,S_IRUSR|S_IWUSR);
  if(strcmp(fname,"access.log")==0 || strcmp(fname,"referer.log")==0 ||
      strcmp(fname,"session.log")==0 || strcmp(fname,"post.log")==0 ||
      strcmp(fname,"user_error.log")==0 ||
      strncmp(fname,"classes/",8)==0)
    lim=GEN_LOG_LIMIT;
  else lim=MODULE_LOG_LIMIT;
  if(l>=lim) {
    for(i=OLD_LOG_FILES-1;i>0;i--) {
      snprintf(b3,sizeof(b3),"%s.old%d",namebuf,i);
      snprintf(b4,sizeof(b4),"%s.old%d",namebuf,i+1);
      if(stat(b3,&st)==0) rename(b3,b4);
    }
    if(OLD_LOG_FILES>0) {
      snprintf(b3,sizeof(b3),"%s.old1",namebuf);
      rename(namebuf,b3);
    }
    else unlink(namebuf);
  }
}

void dispatch_log(void)
{
  size_t l, l2;
  pid_t pid;
  FILE *f;
  char *p1,*p2;
  char namebuf[1024];
  int i;
  #define TEMP_LOG_2 "log/temp-2.log"

  fflush(NULL);
  pid=fork(); if(pid>0) {addfork(pid,1); return;}
  close(commsock);
  call_sh(1,"ls %s* 2>/dev/null >/dev/null || exit;\n\
  for f in %s*; do mv $f $f.bb; done;\n\
sleep 1;\n\
cat %s*.bb >%s;\n\
rm -f %s*.bb",
      &TEMP_LOG_FILE[3],&TEMP_LOG_FILE[3],&TEMP_LOG_FILE[3],TEMP_LOG_2,&TEMP_LOG_FILE[3]);
  f=fopen(TEMP_LOG_2,"r"); if(f==NULL) exit(0);
  fseek(f,0,SEEK_END); l=ftell(f);
  if(l<=0)
    {fclose(f); unlink(TEMP_LOG_2); exit(0);}
  logbuf=xmalloc(l+16); fseek(f,0,SEEK_SET);
  l2=fread(logbuf,1,l,f);fclose(f); unlink(TEMP_LOG_2);
  if(l2!=l) {free(logbuf); exit(0);}
  logbuf[l2]=0;
  for(loglinecnt=0,p1=logbuf;loglinecnt<MAX_LOGLINES && *p1;p1=p2) {
    p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
    p1=find_word_start(p1); if(*p1==0) continue;
    linetab[loglinecnt++]=p1;
  }
  qsort(linetab,loglinecnt,sizeof(linetab[0]),llcmp);
  namebuf[0]=0; llptr=llbuf;
  for(i=0;i<loglinecnt;i++) {
    p1=linetab[i]; p2=find_word_end(p1); *p2++=0;
    if(strcmp(namebuf,p1)!=0) {
      onelogfile(namebuf); snprintf(namebuf,sizeof(namebuf),"%s",p1);
    }
    if(strlen(p2)>=sizeof(llbuf)-(llptr-llbuf)-2) onelogfile(namebuf);
    snprintf(llptr,sizeof(llbuf)-(llptr-llbuf),"%s\n",p2);
    llptr+=strlen(llptr);
  }
  if(namebuf[0]) onelogfile(namebuf);
  free(logbuf);
  exit(0);
}
