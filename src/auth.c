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

/* This file contains user authentification routines */
#include "wims.h"

void refuse_log(int th);
void set_module_prefix(void);

/* check machine load. 2-threshold system.
 * Threshold 1: anonymous new session refused.
 * Threshold 2: New session and anonymous request refused.
 */
void check_load(int th)
{
  int load, pload;
  char *p1, *p2, buf[64];
  char *pp;
  double dload;

  pload=0; pp=strchr(loadavg,'/'); if(pp) {
    for(;pp>loadavg && isdigit(pp[-1]); pp--);
    pload=atoi(pp);
    if(pload*12>threshold2+3) {
      pload_refuse:
      refuse_log(pload+100); user_error("threshold");
    }
  }
  if(ispriority) goto repcheck; /* priority connections will not be refused. */
  if(pload*20>threshold1+2) goto pload_refuse;
  if(th<0 || th>2) goto repcheck;
  /* Operating system load average facility */
  if(robot_access && loadavg[0]==0) goto refuse;
  if(loadavg[0]==0) goto repcheck;
  p1=find_word_start(loadavg); p2=find_word_end(p1);*p2=0;
  dload=atof(p1);
  if(robot_access && (!isfinite(dload) || dload>1000 || dload<0 || dload*200>threshold1))
    goto refuse;
  if(!isfinite(dload) || dload<=0 || dload>1000) goto repcheck; /* unreasonable */
  /* very small 1 min load average */
  if(dload*200<threshold1) goto repcheck;
  if(dload*50>threshold2) goto refuse;
  p1=find_word_start(p2+1); /* go to second average: 5 min. */
  *find_word_end(p1)=0;
  dload=atof(p1);
  if(!isfinite(dload) || dload<=0 || dload>1000) goto repcheck; /* unreasonable */
  load=dload*100;
  snprintf(buf,sizeof(buf),"%d",load);
  setvar("wims_server_load",buf);
  /* cut cpu allowance to 3/4 or half if load is high.
   * But alarm time is not changed */
  if(load*3>=threshold1*2) {
    struct rlimit rlim;
    rlimit_cpu=(3*rlimit_cpu+1)/4;
    if(load>=threshold1) rlimit_cpu=(3*rlimit_cpu+1)/4;
    rlim.rlim_cur=rlim.rlim_max=rlimit_cpu;
    setrlimit(RLIMIT_CPU,&rlim);
  }
  if((th==0 && load*2>threshold1) ||
     (th==1 && load>threshold1) || (th==2 && load>threshold2)) {
    refuse:
  if(new_session && *session_prefix!=0) {
      remove_tree(session_prefix); remove_tree(s2_prefix);
    }
    refuse_log(th); user_error("threshold");
  }
  repcheck:
  if(robot_access) return;
  if(new_session && *session_prefix!=0 && *remote_addr
     && hostcquota && strcmp(remote_addr,"127.0.0.1")!=0
     && !ispriority) {
    /* overload: */
    remove_tree(session_prefix); remove_tree(s2_prefix);
    user_error("overload");
  }
}

/* User authentification routine, obsolete */
void auth(void)
{
  check_load(1); return;
}

#define rafinfono 10

/* check rapidfire information */
void checkrafale(void)
{
  char *p, *p1, *p2, *sh, *u;
  char rbuf[MAX_LINELEN+1];
  time_t rr, rafinfo[rafinfono];
  int i, t, mm, rafinfocnt;
  double coef=0.23;

  if(rafalvl<=0) return;
  p=getvar("module_scoring"); if(p==NULL || strcasecmp(p,"yes")!=0) return;
  u=getvar("wims_user"); if(u!=NULL && strcmp(u,"supervisor")==0) return;
  p=getvar("wims_developer"); if(p!=NULL && *p!=0) return;
  p=getenv("REMOTE_ADDR");if(p!=NULL && strcmp(p,"127.0.0.1")==0) return;
  p=getvar("session"); if(p!=NULL && strstr(p,"_exam")!=NULL) return;
  sh=getvar("wims_sheet"); if(sh!=NULL && *sh>'0') coef*=3;
  p=getvar("wims_rafale"); if(p==NULL) p="";
  mm=0;
  for(p1=find_word_start(p),i=0;i<rafinfono && *p1;p1=find_word_start(p2)) {
   p2=find_word_end(p1); if(*p2) *p2++=0;
   rr=atol(p1); if(rr<=0 || rr>nowtime) continue;
   t=coef*rafalvl*pow(i,1+rafalvl*0.05)-(nowtime-rr); if(t>mm) mm=t;
   rafinfo[i++]=rr;
  }
  if(mm>0) {
    if(u!=NULL && *u!=0) user_log("rafale");
    user_error("rafale");
  }
  rafinfocnt=i;
  { char *rp=rbuf;
    size_t avail=sizeof(rbuf);
    int w=snprintf(rp,avail,"%lu",(unsigned long)nowtime);
    if(w>0 && (size_t)w<avail){rp+=w; avail-=(size_t)w;}
    for(i=0;i<rafinfocnt && avail>2;i++) {
      w=snprintf(rp,avail," %lu",(unsigned long)rafinfo[i]);
      if(w>0 && (size_t)w<avail){rp+=w; avail-=(size_t)w;}
    }
  }
  force_setvar("wims_rafale",rbuf);
}

/* when score is got: erase 2 rafale information. */
void lessrafale(void)
{
  char *p;
  double s;
  int i;
  p=getvar("module_score"); if(p==NULL) return;
  s=atof(p); if(s<3) return;
  p=getvar("wims_rafale"); if(p==NULL || *p==0) return;
  for(i=0;i<2;i++) p=find_word_end(find_word_start(p));
  p=find_word_start(p);
  force_setvar("wims_rafale",p);
}

#define ac_class 0x1  /* class access */
#define ac_exo   0x2  /* access to exercises */
#define ac_tool  0x4  /* access to tools */
#define ac_recre 0x8  /* access to recreations */
#define ac_doc  0x10  /* access to documents */
#define ac_local 0x20 /* access to local modules */
#define ac_com   0x40 /* access to commercial modules */
#define ac_hint  0x80 /* hint command */
#define ac_sheet 0x100/* use within a worksheet */
#define ac_exam  0x200/* work during an exam */
#define ac_freew  0x400/* work during a freework */
#define ac_gloss  0x800/* consulting a glossary */

/* Check site or class access policy. */
void access_check(int isclass)
{
  char *p, *p1, *p2, *p3, *pp1, *pp2;
  char msg[200], *name="";
  char cbuf[MAX_LINELEN+1];
  char* nonstr[]={"","non"};
  long int thisaccess, lineaccess, linepol, thispol;
  int non, refuse;

  if(manageable>=2 || robot_access) return;
  thisaccess=0;
  p=getvar(ro_name[ro_module]); if(p==NULL || *p==0) return;
  if(strncmp(p,"adm/doc",7)==0) thisaccess|=ac_doc;
  else if(strncmp(p,"adm/class/freeworks",17)==0) thisaccess|=ac_freew;
  else if(strncmp(p,"adm/tool/glossary",17)==0) thisaccess|=ac_gloss;
  else if(strncmp(p,"adm/",4)==0 || strcmp(p,home_module)==0) return;
  if(strncmp(p,"local/",6)==0) thisaccess|=ac_local;
  if(strncmp(p,"com/",4)==0) thisaccess|=ac_com;
  p=getvar("wims_user");
  if(p!=NULL && *p!=0) {
    if(!isclass && strcmp(p,"supervisor")!=0) access_check(1);
    thisaccess|=ac_class;
  }
  if(isclass) {
    if(class_dir[0]==0) return;
    accessfile(cbuf,"r","%s/access.conf",class_dir);
  }
  else accessfile(cbuf,"r",ACCESS_CONF);
  if(cbuf[0]==0) return;
  /* access limited */
  if(cmd_type==cmd_hint) thisaccess|=ac_hint;
  p1=getvar("wims_accessright"); if(p1!=NULL && *p1!=0) {
    p=getvar(ro_name[ro_module]);
    for(p1=find_word_start(p1);*p1; p1=find_word_start(p2)) {
      p2=find_word_end(p1);
      if(strncmp(p,p1,p2-p1)==0) return;
    }
  }
  p=getvar("module_category");
  if(p) {
    if(strstr(p,"exercise")!=NULL) thisaccess|=ac_exo;
    if(strstr(p,"tool")!=NULL) thisaccess|=ac_tool;
    if(strstr(p,"recre")!=NULL) thisaccess|=ac_recre;
    if(strstr(p,"doc")!=NULL) thisaccess|=ac_doc;
    if(strstr(p,"glossary")!=NULL) thisaccess|=ac_gloss;
  }
  for(p1=find_word_start(cbuf);*p1;p1=find_word_start(p2)) {
    p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
    if(!myisalpha(*p1)) continue;
    p3=strchr(p1,':'); if(p3==NULL) continue;
    *p3++=0; p3=find_word_start(p3); strip_trailing_spaces(p3);
    refuse=0;
    if(*p3=='!') { p3=find_word_start(p3+1); refuse=1;}
    if(*p3 && checkhostt(p3)==0) continue;
    for(p=p1; *p; p++) {
      if(myisalpha(*p)) *p=tolower(*p); else *p=' ';
    }
    lineaccess=thisaccess; linepol=0;
    for(pp1=find_word_start(p1); *pp1; pp1=find_word_start(pp2)) {
      pp2=find_word_end(pp1); if(*pp2) *pp2++=0;
      if(strncmp(pp1,"non",3)==0) {
        pp1=find_word_start(pp1+3); non=1;
      }  else non=0;
      thispol=0;
      if(strcmp(pp1,"class")==0) {thispol=ac_class; name="class"; goto nxt;}
      if(strcmp(pp1,"exo")==0) {thispol=ac_exo; name="exo"; goto nxt;}
      if(strcmp(pp1,"exercise")==0) {thispol=ac_exo; name="exo"; goto nxt;}
      if(strcmp(pp1,"tool")==0) {thispol=ac_tool; name="tool"; goto nxt;}
      if(strcmp(pp1,"recre")==0) {thispol=ac_recre; name="recre"; goto nxt;}
      if(strcmp(pp1,"recreation")==0) {thispol=ac_recre; name="recre"; goto nxt;}
      if(strcmp(pp1,"doc")==0) {thispol=ac_doc; name="doc"; goto nxt;}
      if(strcmp(pp1,"document")==0) {thispol=ac_doc; name="doc"; goto nxt;}
      if(strcmp(pp1,"freework")==0) {thispol=ac_freew; name="freework"; goto nxt;}
      if(strcmp(pp1,"glossary")==0) {thispol=ac_gloss; name="glossary"; goto nxt;}
      if(strcmp(pp1,"local")==0) {thispol=ac_local; name="local"; goto nxt;}
      if(strcmp(pp1,"com")==0) {thispol=ac_com; name="com"; goto nxt;}
      if(strcmp(pp1,"hint")==0) {thispol=ac_hint; name="hint"; goto nxt;}
      nxt:
      if(thispol==0) continue;
      if(non) lineaccess^=thispol;
      linepol|=thispol;
    }
    if(linepol==0 || (linepol&lineaccess)!=linepol) continue;
    snprintf(msg,sizeof(msg),"no_access_class %s %s",name,nonstr[non]);
    if(refuse)
      if (isclass) user_error(msg); else user_error("no_access");
    else return;
  }
  /* end of limited access */
}
