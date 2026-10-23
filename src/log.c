/*  Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
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
#include <sys/socket.h>
#include <sys/un.h>

#include "wims.h"

char logbuf[4*(MAX_LINELEN+1)];
char *logp=logbuf;

void write_logfile(char *fname, char *str)
{
  char *p;
  if((p=strchr(str,'\n'))!=NULL) *p=0;
  snprintf(logp,sizeof(logbuf)-(logp-logbuf),"%s %s\n",fname,str);
  logp+=strlen(logp);
}

void flushlog(void)
{
  int n, fd;
  n=logp-logbuf; logp=logbuf;
  if(n<=0) return;
  fd=open(temp_log,O_WRONLY|O_APPEND|O_CREAT,S_IRUSR|S_IWUSR);
  if(fd==-1) return;
  (void)write(fd,logbuf,n); close(fd);
}

/* Write module log file. */
void module_log(void)
{
  /* log string is limited to 100 characters. */
  char *logstr, *ip, *sess, lbuf[100], *p;

  if(robot_access || !modlog || strstr(session_prefix,"_check")!=NULL) return;
  logstr=getvar("wims_module_log");
  if(logstr==NULL || *logstr==0) return;
  ip=remote_addr;
  if(mode==mode_default) sess=getvar("wims_session");
  else sess="popup";
  if(ip==NULL || sess==NULL) return;
  mystrncpy(lbuf,logstr,sizeof(lbuf));
  p=strchr(sess,'_'); if(p==NULL) p=sess+strlen(sess);
  if(p<sess+4) p=sess; else p=p-4;
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %.6s %s\11%s",
   nowstr, p, ip, lbuf);
  p=getvar(ro_name[ro_module]);
  if(p==NULL || *p==0) return;
  write_logfile(mkfname(NULL,"%s/%s",module_dir,p),tmplbuf);
}

/* log http referers */
void referer_log(void)
{
  char *c,*s,*ip,*r,*h,refstr[256];

  if(robot_access) return;
  c=getvar(ro_name[ro_cmd]);
  if(c==NULL) c="";
  if(mode==mode_default) s=getvar("wims_session"); else s="popup";
  if(s==NULL) s="----";
  else if(!new_session && strcmp(c,"intro")!=0) return;
  if(strlen(s)<4) s="----";
  if(strchr(s,'_')!=NULL) return;
  ip=remote_addr; if(*ip==0) return;
  r=getenv("HTTP_REFERER");
  if(r==NULL || *r==0) r="??";
  /* skip some useless referers */
  else {
    if(strstr(r,cgi_name)!=NULL) return;
    if(strstr(r,"file:")!=NULL || strchr(r,'.')==NULL) r="??";
    if(strstr(r,"http")==NULL && strchr(r,'.')==NULL &&
     strstr(r,"bookmark")!=NULL) r="??bookmark";
  }
  if(strncmp(r,"http://",strlen("http://"))==0) r+=strlen("http://");
  if(strncmp(r,"https://",strlen("https://"))==0) r+=strlen("https://");
  /* Take references from the same site or not? No. */
  h=getenv("HTTP_HOST");
  if(h!=NULL && *h!=0 && strncmp(r,h,strlen(h))==0) return;
    /* stop before '#' */
  mystrncpy(refstr,r,sizeof(refstr));
  r=strchr(refstr,'#'); if(r!=NULL) *r=0;
  for(r=refstr;r<refstr+strlen(refstr);r++) {
    if(*r=='%' && *(r+1)=='7' && *(r+2)=='E') {
    *r='~'; ovlstrcpy(r+1,r+3);
    }
  }
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %s %s\11%s",
     nowstr,s+strlen(s)-4,ip,refstr);
  write_logfile("referer.log",tmplbuf);
}

/* Log new creation of sessions. For server counting use. */
void session_log(char *c)
{
  int i;
  char *ip, *p, *agent, *s, *sess;

  ip=remote_addr; if(*ip==0) return;
  if(mode==mode_default) {
    sess=getvar("wims_session");
    if(sess==NULL) return;
    if(strchr(sess,'_')!=NULL) return;
  }
  else sess="popup";
  p=getenv("REMOTE_HOST"); if(p==NULL) p="";
  i=strlen(p); if(i>40) p+=i-40;
  agent=getenv("HTTP_USER_AGENT"); if(agent==NULL) agent="";
  s=strchr(sess,'_'); if(s==NULL) s=sess+strlen(sess);
  if(s<sess+4) s=sess; else s=s-4;
    /* limit agent name to 80 chars */
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %s %s\11%s\11%.80s",
     nowstr, s, ip, p, agent);
  write_logfile("session.log",tmplbuf);
}

/* Log user information. */
void user_log(char *c)
{
  char fname[MAX_FNAME+1], logbuf[MAX_LINELEN+1], cbuf[256], sbuf[32], shbuf[32];
  char *user, *classe, *sh, *sess, *exo, *cc, *ip, *allow, *pend, *se;
  char *ex;
  double sc,Sc;
  int i, scorelog, testmax;

  if(robot_access || strstr("session_prefix","_check")!=NULL) return;
  user=getvar("wims_user"); classe=getvar("wims_class");
  sc=0;
  if(isexam) {
    if(user==NULL || classe==NULL || *user==0 || *classe==0) return;
    sh=getvar("worksheet"); if(sh==NULL) return;
    mystrncpy(shbuf,sh,sizeof(shbuf));
    exo=strchr(shbuf,'.'); if(exo==NULL) return;
    *exo++=0; sh=shbuf;
    if(mode==mode_default) sess=getvar("wims_session");
    else sess="popup";
    if(sess==NULL) return;
    mystrncpy(sbuf,sess,sizeof(sbuf));
    sess=strchr(sbuf,'_'); if(sess==NULL) return;
    *sess=0; sess=sbuf; ex="E";
    accessfile(logbuf,"r","%s/.E%s",class_dir,sh);
    if(strchr(logbuf,'#')!=NULL || strcmp(user,"supervisor")==0) simuxam=1;
    else {
      accessfile(logbuf,"r","%s/%s/examsimu.%d", session_dir,sess,sh);
      if(strstr(logbuf,"yes")!=NULL) user_error("expired_exam");
    }
    mkfname(examlogd,"%s/examlog/%s/%s",class_dir,user,sess);
    mkfname(examlogf,"%s/%s.%s",examlogd,sh,exo);
  }
  else {
    sh=getvar("wims_sheet");
    if(sh==NULL || *sh==0) {
      sh=getvar("wims_fwnumber");
      if(sh==NULL || *sh==0) return;
      ex="F";
      exo=getvar("wims_fwexo");
    }
    else {
      exo=getvar("wims_exo"); if(exo==NULL) return;
      ex="";
    }
    sess=getvar("wims_session");
    if(sess==NULL) return;
  }
  if(strcmp(c,"new")!=0 && strcmp(c,"renew")!=0
     && strcmp(c,"rafale")!=0
     && strcmp(c,"hint")!=0 && parm_restore==0) {
    char *s;
    s=getvar("module_score");
    if(s==NULL || *s==0) return;
    sc=atof(s); if(!isfinite(sc)) {sc=0; return;}
    snprintf(cbuf,sizeof(cbuf),"score %s",s);
    cc=cbuf;
  }
  else cc=c;
  if(classe==NULL || *classe==0) i=1;
  else i=getscorestatus(classe,atoi(sh));
  testmax=gettrycheck(classe,user,atoi(sh),atoi(exo))==1;
  pend=getvar("wims_scoring"); if(pend==NULL) pend="";
  if((i==0 || !exodepOK || strcmp(pend,"pending")!=0 || testmax) && strcmp(cc,"rafale")!=0 && *ex!='F')
    if(testmax) allow="maxtry noscore"; else allow="noscore";
  else allow="";
  ip=remote_addr; if(*ip==0) ip="-"; scorelog=0;
  if(user==NULL || *user==0) {
    classe="0"; allow="";
    mkfname(fname,"../sessions/%s/.score",sess);
  }
  else {
    char *pp;
    if(classe==NULL || *classe==0) return;
    pp=getvar("wims_scorereg"); /* contains suspend or exotrymax */
    if((allow[0]==0 || i!=0 || (pp!=NULL && strcmp(pp,"suspend")==0)) && *ex!='E' && *ex!='F')
      scorelog=1;
    else {
      if (*ex=='F') {
       char *s;
       /* freework activity logged to different file */
        mkfname(fname,"classes/%s/freeworksdata/%s.freework",classe,user);
        /* log exercise activity in case of score is enough */        
        s=getvar("module_score");
        if(s!=NULL && *s!=0) {
          char *ms;
          int ims;
          ms=getvar("wims_fwminscore");
          ims=atof(ms);
          if (ims<0 && -1*atof(s)<=ims) {
            char nbuf[MAX_FNAME+1];
            snprintf(nbuf,sizeof(nbuf),"%s/freeworksdata/%s/work/%s-wimsexo/%s",class_dir,sh,user,exo);
            if (ftest(mkfname(NULL,"%s",nbuf))!=is_file) {
              mkfname(freeworklogd,"%s/freeworksdata/%s/work/%s-wimsexo",class_dir,sh,user);
              mkfname(freeworklogf,"%s/%s",freeworklogd,exo);
            }
          } 
        }
      } 
      else mkfname(fname,"classes/%s/noscore/%s",classe,user); 
    }
  }
  if(isexam && user!=NULL && *user!=0) {
    allow=exam_sheetexo;
    snprintf(logbuf,sizeof(logbuf),":%s %2s %s  \t%s%s\n",
    nowstr,exo,cc,ip,allow);
    accessfile(logbuf,"a","%s/%s/examscore.%s", session_dir,sess,sh);
    Sc=currexamscore(atoi(sh));
    accessfile(logbuf,"r","%s/.E%s",class_dir,sh);
    if(simuxam==0) {   /* not simulation */
      if(sc>0) {
        snprintf(logbuf,sizeof(logbuf),
        "%s %.5f -1 %u %s %s\n",
        sh,Sc,(unsigned int) nowtime,ip,sess);
        accessfile(logbuf,"a","%s/score/%s.exam", class_dir,user);
      }
    }
    else snprintf(exam_sheetexo+strlen(exam_sheetexo),
        sizeof(exam_sheetexo)-strlen(exam_sheetexo), "\tS");
  }
  if(strcmp(c,"new")==0 || strcmp(c,"renew")==0)
    se=getvar("wims_seed"); else se="";
  snprintf(logbuf,sizeof(logbuf),"%s%s %s %2s %2s %s  \t%s\t%s\t%s",
      ex,nowstr,sess,sh,exo,cc,ip,se,allow);
  if(scorelog) {
    snprintf(tmplbuf,sizeof(tmplbuf),"-c%s -u%s scorelog %s",
       classe,user,logbuf);
    _daemoncmd(tmplbuf);
  }
  else write_logfile(fname,logbuf);
}

/* Log class information. */
void class_log(char *cl, char *l, char *ip)
{
  char logbuf[1024];

  if(robot_access) return;
  snprintf(logbuf,sizeof(logbuf),"%s %s   \t%s",
      nowstr,ip,l);
  write_logfile(mkfname(NULL,"classes/%s/.log",cl),logbuf);
}

/* Log accesses to modules. For server counting use. */
void access_log(char *c)
{
  int i;
  char *ip, *p, *sess, *s, *agent, *u, *cl;
  time_t logtime;
  char ag[128], tm[64];

  ip=remote_addr;
  if(*ip==0) ip="????????";
  if(mode==mode_default) sess=getvar("wims_session");
  else sess="popup";
  if(sess==NULL) sess="----------";
  p=getvar(ro_name[ro_module]);
  if(p==NULL || *p==0) p="-";
/* limit module name to 40 chars */
  i=strlen(p); if(i>40) p+=i-40;
  if(robot_access) {
   agent=getenv("HTTP_USER_AGENT"); if(agent==NULL) agent="-";
   snprintf(ag,sizeof(ag)," %s",agent);
  }
  else {
   u=getvar("wims_user");
   if(u!=NULL && *u!=0) snprintf(ag,sizeof(ag)," %s,%s",u,getvar("wims_class"));
   else ag[0]=0;
  }
  s=strchr(sess,'_'); if(s==NULL) s=sess+strlen(sess);
  if(s<sess+4) s=sess; else s=s-4;
  tm[0]=0; logtime=time(0); if(logtime>nowtime+2) {
   snprintf(tm,sizeof(tm)," (%lus)", logtime-nowtime);
  }
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %.6s %s\11%s\11%s%s%s",
      nowstr, s, ip, c, p, tm, ag);
  write_logfile("access.log",tmplbuf);
  user_log(c);
  cl=getvar("wims_class");
  if(cl!=NULL && *cl!=0) {
    char *l;
    l=getvar("wims_class_log");
    if(l!=NULL && *l!=0) class_log(cl, l, ip);
  }
}

/* Log of mails. */
void mail_log(char *c)
{
  int i;
  char *ip, *p, *sess, *s, *cl;

  ip=remote_addr;
  if(*ip==0) ip="????????";
  if(mode==mode_default) sess=getvar("wims_session");
  else sess="popup";
  if(sess==NULL) sess="----------";
  p=getvar(ro_name[ro_module]);
  if(p==NULL || *p==0) p="-";
/* limit module name to 40 chars */
  i=strlen(p); if(i>40) p+=i-40;
  s=strchr(sess,'_'); if(s==NULL) s=sess+strlen(sess);
  if(s<sess+4) s=sess; else s=s-4;
  cl=getvar("wims_class"); if(cl==NULL) cl="";
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %.6s %s\11%s\11%s\11%s",
      nowstr, s, ip, c, cl, p);
  write_logfile("mail.log",tmplbuf);
}

/* log posted data */
void post_log(void)
{
  char *h, *l, logstr[2*MAX_LINELEN+2];
  char *authpwd, *p, *ll, *l1 ;
  static char *obliterate[]={"auth_password=","passwd=","passsup=","password="};
  int i, oblicnt=sizeof(obliterate)/sizeof(char*);;

  h=remote_addr;
  if(mpboundary[0]!=0) l="multipart/form-data"; else l=stdinbuf;

  ll=strdup(l);
  for(i=0;i<oblicnt;++i){
    authpwd=obliterate[i];
    if((p=strstr(ll,authpwd))!=NULL ) {
      l1=strdup(ll);
      mystrncpy(ll,l1,p-ll+strlen(authpwd)+1);
      strcat(ll,"xxxx");
      l1+=(p-ll)+strlen(authpwd);
      if((p=strstr(l1,"&"))!=NULL) strcat(ll,p);
    }
  }
  snprintf(logstr,sizeof(logstr),"%s %s\t%s",
     nowstr, h, ll);
  write_logfile("post.log",logstr);
}

/* It is this routine which is called by main(). */
void write_logs(void)
{
  char *p;
  p=getvar(ro_name[ro_cmd]); if(p==NULL || *p==0) p="no_cmd";
  access_log(p);
  if(strstr(session_prefix,"_check")!=NULL) return;
  module_log(); referer_log();
  if(new_session) session_log(p);
}

void user_error_log(char msg[])
{
  char *s, *m, *c, *h, *q, *r, *sess, logstr[512];
  if(mode==mode_default) sess=getvar("wims_session");
  else sess="popup";
  if(sess==NULL) sess="----------";
  m=getvar(ro_name[ro_module]);if(m==NULL) m="";
  c=getvar(ro_name[ro_cmd]);if(c==NULL) c="";
  h=remote_addr;
  q=getenv("QUERY_STRING");if(q==NULL) q="";
  r=getenv("HTTP_REFERER");
  if(r==NULL || strstr(r,cgi_name)!=NULL) r="";
  s=strchr(sess,'_'); if(s==NULL) s=sess+strlen(sess);
  if(s<sess+4) s=sess; else s=s-4;
  snprintf(logstr,sizeof(logstr),"%s %.5s %s %s, module=%s cmd=%s: %s %s",
     nowstr, s, h, msg, m, c, r, q);
  write_logfile("user_error.log",logstr);
  if(user_error_nolog) return;
  access_log("user_error");referer_log();
}

void module_error_log(char msg[])
{
  char *s, *m, *c, logstr[256];
  if(strstr(msg,"debug")!=NULL || strstr(msg,"timeup")!=NULL) return;
  if(strstr(m_file.name,"sessions/")!=NULL) return;
  s=getvar(ro_name[ro_module]);
  if(s!=NULL) {
   if(strncmp(s,"classes/",strlen("classes/"))==0 ||
    strncmp(s,"devel/",strlen("devel/"))==0) return;
  }
  s=getvar("wims_session"); if(s==NULL) s="  ";
  m=getvar(ro_name[ro_module]);if(m==NULL) m="";
  c=getvar(ro_name[ro_cmd]);if(c==NULL) c="";
  snprintf(logstr,sizeof(logstr),"%s %.10s %s in %s/%s, line %d",
     nowstr, s+2, msg, m, m_file.name, m_file.l+1);
  write_logfile("module_error.log",logstr);
  access_log("module_error");
}

/* Refused users due to threshold excess */
void refuse_log(int th)
{
  char *load, *h;

  load=getvar("wims_server_load"); if(load==NULL) load="??";
  h=remote_addr;
  snprintf(tmplbuf,sizeof(tmplbuf),"%s %s\11%d:%s",
      nowstr, h, th, load);
  write_logfile("refuse.log",tmplbuf);
}

#define logdpid "../tmp/log/wimslogd.pid"
#define newlogd "../tmp/log/wimslogd.new"

void bringuplogd(void)
{
  char *arg[]={"../bin/wimslogd",NULL};
  struct stat st;
  pid_t pid;

/* need to update wimslogd? */
  if(stat(newlogd,&st)==0) {
   if((S_IXUSR&st.st_mode)!=0 && st.st_size>40000 && st.st_size<200000)
     call_ssh("mv %s %s",newlogd,arg[0]);
   else call_ssh("rm -f %s",newlogd);
  }
  pid=fork(); if(pid) return;   /* parent */
/* double fork to escape sysmask orphan. */
  pid=fork(); if(pid) {   /* secondary parent */
   snprintf(tmplbuf,sizeof(tmplbuf),"%u",pid);
   mkdirs("../tmp/log");
   chmod("../tmp/log",S_IRUSR|S_IWUSR|S_IXUSR);
   accessfile(tmplbuf,"w",logdpid);
   exit(0);
  }
// don't change the order: use in Wimslogd/wimslogd.c in parms()
  if(setreuid(geteuid(),geteuid()) || setregid(getegid(),getegid())) _exit(1);
  snprintf(tmplbuf,sizeof(tmplbuf),"%u %u %u %u %u %u %d %d %d %d",
      idle_time,idle_time2,idle_time3,
      OLD_LOG_FILES,GEN_LOG_LIMIT,
      MODULE_LOG_LIMIT,backup_hour,site_accounting,
      examlog_limit,LOG_DELETE);

  setenv("wimslogd",tmplbuf,1);
  execve(arg[0],arg,environ);
  fprintf(stderr,"Unable to execute wimslogd: %s\n",strerror(errno));
  exit(1);
}

void checklogd(void)
{
  int i,t;
  char *p1, *p2, *p, buf[MAX_LINELEN+1];
  sun.sun_family=PF_UNIX;
  snprintf(sun.sun_path,sizeof(sun.sun_path),"%s",ksockfile);
  p=getenv("REMOTE_ADDR"); if(p==NULL) p="";
  snprintf(buf+sizeof(int),sizeof(buf)-sizeof(int),"ping %s",p);
  t=kerneld(buf,sizeof(buf)); if(t<0) {
    bringuplogd();
    for(i=0; i<10 && t<0; i++) {
      msleep(100);
        snprintf(buf+sizeof(int),sizeof(buf)-sizeof(int),"ping %s",p);
     t=kerneld(buf,sizeof(buf));
    }
  }
  if(t<0) internal_error("Unable to bring up wimslogd.");
  p1=find_word_start(buf+sizeof(int)); p2=find_word_end(p1);
  if(*p2) *p2++=0;
  if(strcmp(p1,"OK")!=0) internal_error("wimslogd error.");
  if(*p2=='1') hostcquota=1; else hostcquota=0;
  p1=find_word_start(find_word_end(p2));
  p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
  mystrncpy(loadavg,p1,sizeof(loadavg));
  p2=find_word_start(p2);
  p1=find_word_end(p2); if(*p1) *p1++=0;

  p=getenv("SERVER_ADDR");
  if(*p2!=0 && memcmp(p,"10.",3)==0) p=p2;
  if(p!=NULL) {
    i=strlen(cookieheader);
    snprintf(cookieheader+i, sizeof(cookieheader)-i,"%s/",p);
  }
  p=getenv("HTTP_COOKIE"); cookiegot[0]=0;
  if(p!=NULL && (p2=strstr(p,cookieheader))!=NULL) {
    mystrncpy(cookiegot,find_word_start(p+strlen(cookieheader)),sizeof(cookiegot));
    *find_word_end(cookiegot)=0;
  }
}
