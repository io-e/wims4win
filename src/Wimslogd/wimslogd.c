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

/* WIMS log daemon */

#include "wimslogd.h"
/* variables shared with wims.c */
#include "../commun.h"
extern char **environ;

char cwd[MAX_FNAME+1];
char pidstr[32];
char keepdate[32]="0";
char mupdate[32]="0";
char backdate[32]="0";
char loadavg[MAX_LINELEN+1];
char qbuf[MAX_LINELEN+1];       /* quota buffer */
time_t nowtime, starttime, lastcleantime=0;
time_t thismin, lastmin, startmin;
struct tm *now;
int nowsec, nowmin, nowhr, nowday, nowwday,nowmon,nowyear;
int startdate;
char nowstr[64];
pid_t mypid;
int anti_time=3600*24;      /* antidate tolerance */
int backup_hour=-1;
int site_accounting=0;
int modupdatetime=0;
int rshift;      /* shift minute start */
int commsock;
int answerlen;
int debugging;
char ipbuf[64];
char nodeipbuf[MAX_LINELEN+1];
char commbuf[BUFFERLEN+1];
char *textptr;

int cwdtype;

/* check whether there is anything to execute */
void logexec(void)
{
  struct stat st;
  pid_t pid;
  if(stat("log/wimslogd.exec",&st)) return;
  fflush(NULL);
  pid=fork(); if(pid>0) {addfork(pid,1); return;}
  close(commsock); msleep(100);
  call_ssh(1,"sh log/wimslogd.exec >tmp/log/wimslogdexec.out 2>tmp/log/wimslogdexec.err");
  unlink("log/wimslogd.exec"); exit(0);
}

void local(void)
{
  struct stat st;
  if(stat("log/wimslogd.local",&st)) return;
  if(!(S_IXUSR&st.st_mode)) return;
  call_ssh(0,"sh log/wimslogd.local");
}

void getnow(void)
{
  nowtime=time(NULL); thismin=(nowtime-rshift)/MINLENGTH;
  now=localtime(&nowtime);
  nowsec=now->tm_sec;
  nowmin=now->tm_min; nowhr=now->tm_hour;
  nowday=now->tm_mday; nowwday=now->tm_wday;
  nowmon=now->tm_mon+1; nowyear=now->tm_year+1900;
  snprintf(nowstr,sizeof(nowstr),"%04d%02d%02d.%02d:%02d:%02d",
         nowyear,nowmon,nowday,nowhr,nowmin,nowsec);
}

void parms(void)
{
  char *p, *p1, *p2, *parm[16];
  char buf[16];
  int t,r;
  p=getenv("wimslogd");
  if(p==NULL || *p==0) return;
  for(t=0, p1=find_word_start(p); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    parm[t++]=p1;
  }
  idle_time=atoi(parm[0]); if(idle_time<=10) idle_time=5000;
  idle_time2=atoi(parm[1]); if(idle_time2<=10) idle_time2=idle_time;
  idle_time3=atoi(parm[2]); if(idle_time3<=10) idle_time3=idle_time2;
  if(idle_time2>idle_time) idle_time2=idle_time;
  if(idle_time3>idle_time2) idle_time3=idle_time2;
  OLD_LOG_FILES=atoi(parm[3]);
  if(OLD_LOG_FILES>100) OLD_LOG_FILES=100;
  LOG_DELETE=atoi(parm[9]);
  if(LOG_DELETE>1000) LOG_DELETE=1000;
  if(parm[4]) GEN_LOG_LIMIT=atoi(parm[4]);
  if(parm[5]) MODULE_LOG_LIMIT=atoi(parm[5]);
  if(parm[6]) backup_hour=atoi(parm[6]);
  if(parm[7]) site_accounting=atoi(parm[7]);
  if(parm[8]) r=atoi(parm[8])+1; else r=8;
  if(r<2) r=2;
  if(r>100) r=100;
  snprintf(buf,sizeof(buf),"%d",r); setenv("examlog_lim2",buf,1);
  if(site_accounting>0) setenv("site_accounting","yes",1);
}

/* This is run only when manually invoking the program.
 * Verifies the orderedness of various list tables.
 */
int verify_tables(void)
{
  if(verify_order(cmdlist,cmdcnt,sizeof(cmdlist[0]))) return -1;
  return 0;
}

int main(int argc, char *argv[])
{
  char *p;
  struct stat st;
  uid_t myid;
  int /*mfd,mincnt,*/rsock;
  char buf[MAX_LINELEN+1];
  forkcnt=0; exec_wait=1; /*mincnt=0;*/
  classcaches=0;
  (void)freopen("/dev/null","r",stdin);
  (void)freopen("../tmp/log/wimslogd.out","w",stdout);
  (void)freopen("../tmp/log/wimslogd.err","w",stderr);
  /*    mfd=shm_open(SHM_NAME,O_RDWR|O_CREAT|O_TRUNC,S_IRUSR|S_IWUSR);
    write(mfd,buf,SHM_SIZE);
    shmptr=mmap(0,SHM_SIZE,PROT_READ|PROT_WRITE,MAP_SHARED,mfd,0);
    if(shmptr==MAP_FAILED) {
      fprintf(stderr,"wimslogd: mmap() failure. %s\n",
            strerror(errno));
      exit(1);
    }
  */
  verify_tables();
  init_random();
  modupdatetime=(double) random()*350/RAND_MAX;
  rshift=(double) random()*MINLENGTH/RAND_MAX;
  parms();
  if(getcwd(cwd,sizeof(cwd))==NULL) {      /* directory missing */
    fprintf(stderr,"wimslogd: getcwd() failure. %s\n",
          strerror(errno));
    return 1;
  }
  p=strstr(cwd,"/public_html");
  if(p!=NULL && *(p+strlen("/public_html"))==0) {
    *p=0;
    if(chdir(cwd)<0) {      /* strong error */
      fprintf(stderr,"wimslogd: Unable to change directory. %s\n",
                strerror(errno));
      return 1;
    }
  }
  opensock();
  mypid=getpid();
  myid=geteuid(); if(setreuid(myid,myid)) {fprintf(stderr,"wimslogd: setreuid failed. %s\n",strerror(errno)); return 1;}
  myid=getegid(); if(setregid(myid,myid)) {fprintf(stderr,"wimslogd: setregid failed. %s\n",strerror(errno)); return 1;}
  stat("/sysmask/notice/init-end",&st);
  snprintf(pidstr,sizeof(pidstr),"%u",mypid);
  getnow(); printf("wimslogd %s started at %s.\n",pidstr,nowstr);
  startdate=nowday;
  fflush(NULL);
  starttime=nowtime; startmin=lastmin=thismin;
  wlogdaccessfile(qbuf,"r","log/cquota/lim.host");
  wlogdaccessfile(buf,"r","log/myip"); mystrncpy(ipbuf,find_word_start(buf),sizeof(ipbuf));
  wlogdaccessfile(buf,"r","tmp/log/wimslogd.relax"); /* if yes then it is a cluster child */
  if(strstr(buf,"yes")!=NULL) {      /* register my real IP */
    wlogdaccessfile(nodeipbuf,"r","/etc/myip");
    wlogdaccessfile(nodeipbuf,"w","tmp/log/myip");
  }
  do {
    fd_set rset;
    struct timeval tv;
    int t, selectcnt;

    if(getpid()!=mypid) return 0;      /* leaked child */
    /* creer le fichier debugfile défini dans wimslogd.h pour voir apparaitre des debug*/
    if(stat(debugfile,&st)==0 && st.st_size<MAX_DEBUGLENGTH) debugging=1;
    else debugging=0;
    wlogdaccessfile(loadavg,"r","/proc/loadavg");
    for(selectcnt=0; selectcnt<100; selectcnt++) {
      tv.tv_sec=0; tv.tv_usec=50000; /* a pause every 50 ms. */
      FD_ZERO(&rset); FD_SET(commsock,&rset);
      t=select(commsock+1,&rset,NULL,NULL,&tv);
        if(t==0) {forkman(0); continue;}
        if(t<0) {wimslogd_error("select() error."); continue;}
        rsock=accept(commsock,NULL,NULL);
        if(rsock==-1) {wimslogd_error("accept() error."); continue;}
        answer(rsock);
    }
    forkman(1);
    getnow();
    if(thismin==lastmin) continue;
    /*mincnt++;  if(mincnt>MAX_MIN) return 0; Refreshment. */
    if(nowday!=startdate) return 0; /* Daily refreshment. */
    lastmin=thismin;
    wlogdaccessfile(buf,"r",pidfile); strip_trailing_spaces(buf);
    if(strcmp(buf,pidstr)!=0) {      /* wrong pid: abandon. */
      wait_children();
      return 0;
    }

    if(getpid()!=mypid) return 0;      /* leaked child */
    wlogdaccessfile(qbuf,"r","log/cquota/lim.host");
    wlogdaccessfile(buf,"r","log/myip"); mystrncpy(ipbuf,find_word_start(buf),sizeof(ipbuf));
    cleancache();
    if((thismin%127)==6) homedir();      /* update home directory setup */
    wlogdaccessfile(buf,"r","tmp/log/wimslogd.relax"); /* if yes then no housekeeping */
    if(strstr(buf,"yes")==NULL) {
      dispatch_log();
      if((thismin%2)==1) local();
      /* if((thismin%9)==0) */ cleaning(1);       /* clean up session directories */
      if((thismin%5)==0 && nowmin>15) housekeep();      /* daily housekeeping */
      if(getpid()!=mypid) return 0;      /* leaked child */
      if(nowhr*60+nowmin>modupdatetime && (thismin%17)==11) modupdate();
      if(backup_hour>0 && backup_hour<23 && (thismin%17)==3 && nowhr>=backup_hour)
        backup();      /* daily backup */
      fflush(NULL);
      logexec();
    }
    else {      /* cluster child */
      if((thismin%9)==0) cleaning(0);       /* clean up session directories */
    }
  }
  while(1==1);
  return 0;
}

