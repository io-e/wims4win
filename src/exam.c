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

#include <utime.h>
#include "wims.h"

/* Subroutines to process examinations */

char eparm[MAX_LINELEN+1];

void exam_parm(void)
{
  char *p1, *p2, *p3;
  int v;
  setvar("module_init_parm",eparm);
  for(v=0,p1=eparm; *p1; p1=p2) {
    while(*p1=='&') p1++;
    p2=strchr(p1,'&');
    if(p2==NULL) p2=p1+strlen(p1); else *p2++=0;
    p3=strchr(p1,'='); if(p3==NULL) continue; else *p3++=0;
    p1=find_word_start(p1); *find_word_end(p1)=0;
    p3=find_word_start(p3); strip_trailing_spaces(p3);
    user_variable[v].name=p1; user_variable[v].value=p3;
    v++;
  }
  user_var_no=v;
}

/* prepare examination situation */
void check_exam(void)
{
  char *p, *p2, *e1, *e2, *pc=NULL;
  char ps[64], pb[64], cbuf[64], dbuf[64], vbuf[MAX_FNAME+1];
  int t1, t2;
  int start, duration, remain;
  double prime;
  struct stat st;

  p=getvar(ro_name[ro_session]);
  if(p==NULL || (p2=strstr(p,"_exam"))==NULL) return;
  mystrncpy(pb,p,sizeof(pb)); p=pb;
  p2=strstr(p,"_exam"); if(p2==NULL) return; else *p2=0;

  if(strchr(p2+1,'t')==NULL) {
    e1=getvar(ro_name[ro_worksheet]);
    if(e1==NULL || *e1==0) {
      bad: user_error("bad_exam"); return;
    }
    e2=strchr(e1,'.'); if(e2==NULL) goto bad;
    e2++; t1=atoi(e1); t2=atoi(e2);
  }
  else {
    char *pt1,*pt2;
    pt1=strchr(p2+1,'t'); pt2=strchr(pt1+1,'t');
    *pt1++=0; *pt2++=0; t1=atoi(pt1); t2=atoi(pt2);
  }
  if(t1<1 || t1>99 || t2<1 || t2>99) goto bad;
  snprintf(ps,sizeof(ps),"%s_examt%dt%d",p,t1,t2);
  isexam=1; setsesdir(ps); force_setvar("wims_isexam","1");
  force_setvar(ro_name[ro_session],ps);
  /* get exam setup data */
  accessfile(cbuf,"r","%s/%s/examdata.%d", session_dir,pb,t1);
  if(cbuf[0]==0) goto bad;
  p=find_word_start(cbuf); p2=find_word_end(p); if(*p2) *p2++=0;
  duration=atoi(p)*60; if(duration==0) goto bad;
  if(*p2) prime=atof(p2); else prime=0;
  if (strstr(ps,parent_dir_string)!=NULL) user_error("wrong_session");
  /* Whether the exercise is already done */
  p=getvar(ro_name[ro_cmd]); if(p!=NULL) {
    if(strcmp(p,"getins")==0 || strcmp(p,"getframe")==0) goto skipped;
  }
  exam_currscore(t1);
  if(exam_scoredata[t2-1]!=-1000) {
    snprintf(dbuf,sizeof(dbuf),"%.1f",exam_scoredata[t2-1]);
    setvar("wims_exo_lastscore",dbuf);
    /* Do we need to erase subsession? */
    /* cannot see the page of the statement because of the user_error */
    user_error("exam_exo_finished");
  }
  skipped:
  snprintf(session_prefix,sizeof(session_prefix),"%s/%s",session_dir,ps);
  snprintf(vbuf,sizeof(vbuf),"%s/var",session_prefix);
  if(stat(vbuf,&st)==0) { /* exists */
    unsetvar("module");
    p=getvar(ro_name[ro_cmd]);
    if(p==NULL) {
      recmd:
      force_setvar(ro_name[ro_cmd],commands[cmd_resume]);
      user_var_no=0;
    }
    else {
      int i;
      for(i=0;i<CMD_NO && strcmp(p,commands[i]);i++);
      if(i>=CMD_NO || i==cmd_intro || i==cmd_new || i==cmd_renew || i==cmd_hint)
        goto recmd;
    }
  }
  else { /* new */
    char *pt, cbuf[MAX_FNAME+1], ebuf[MAX_FNAME+1];
    char buf[2][MAX_LINELEN+1];
    int w1,w2;

    force_setvar(ro_name[ro_cmd],commands[cmd_new]);
    get_static_session_var();

    pc=getvar("wims_class"); if(pc==NULL) pc="";
    snprintf(cbuf,sizeof(cbuf),"%s/%s",class_base,pc);
    snprintf(ebuf,sizeof(ebuf),"%s/exams/.exam%d",cbuf,t1);
    direct_datafile=1;
    datafile_fnd_record(ebuf,t2,buf[0]);
    direct_datafile=0;
    if(buf[0][0]==0) goto bad;
    fnd_line(buf[0],4,buf[1]);
    if(buf[1][0]!=0) { /* dependencies */
      if(!exam_depcheck(buf[1],t1)) {
        user_error("exam_dep");
      }
    }
    fnd_line(buf[0],2,buf[1]); if(buf[1][0]==0) goto bad;
    calc_randitem(buf[1]);
    pt=strchr(buf[1],'.');
    if(pt==NULL) {
      bad2: module_error("bad_exam_2"); return;
    }
    snprintf(exam_sheetexo,sizeof(exam_sheetexo),"	%s",buf[1]);
    *pt++=0; w1=atoi(buf[1]); w2=atoi(pt);
    if(w1<1 || w1>99 || w2<1 || w2>99) goto bad2;
    snprintf(ebuf,sizeof(ebuf),"%s/sheets/.sheet%d",cbuf,w1);
    direct_datafile=1;
    datafile_fnd_record(ebuf,w2,buf[0]);
    direct_datafile=0;
    if(buf[0][0]==0) goto bad2;
    fnd_line(buf[0],1,buf[1]); if(buf[1][0]==0) goto bad2;
    force_setvar(ro_name[ro_module],buf[1]);
    fnd_line(buf[0],2,eparm); /* if(eparm[0]==0) goto bad2; */
    exam_parm();
  }
  get_static_session_var();
 /* detect open exam in other session and forbidden open a new */
  char *pu, pbuf1[MAX_LINELEN+1];
  if(pc==NULL) pc=getvar("wims_class");
  if(pc==NULL) pc="";
  pu=getvar("wims_user"); if(pu==NULL) pu="";
 /* line not needed at this step make directory is done later
  snprintf(pbuf1,sizeof(pbuf1),"%s/%s/.parmreg", class_base, pc);
  mkdirs(pbuf1);*/
  snprintf(pbuf1,sizeof(pbuf1),"%s/%s/.parmreg/%s.exam",class_base, pc, pu);
  if(stat(pbuf1,&st)==0) { /* search for other session ?? */
    /* read file pbuf1 */
    char pbuf2[MAX_LINELEN+1], *pf1, *pf2;
    accessfile(pbuf2,"r",pbuf1);
    pf1=find_word_start(find_word_end(find_word_start(pbuf2)));pf2=find_word_end(pf1);
    memmove(pbuf2,pf1,pf2-pf1);pbuf2[pf2-pf1]=0;
    if( strcmp(pbuf2,pb)!=0 && strcmp("supervisor",pu)!=0 ) { /* other exam session open */
        user_error("other_exam_session"); return;
    }
  }
  /* end of add for detect */

  /* Register start time */
  snprintf(vbuf,sizeof(vbuf),"%s/%s/examreg.%d",
        session_dir,pb,t1);
  if(stat(vbuf,&st)==0) { /* reg exists */
    char tbuf[MAX_LINELEN+1];
    struct utimbuf ut;
    accessfile(tbuf,"r",vbuf); start=atoi(tbuf);
    if(start<=nowtime-duration) {
      user_error("expired_exam"); return;
    }
    /* refresh session times */
    ut.actime=ut.modtime=nowtime;
    utime(mkfname(NULL,"%s/%s",session_dir,pb),&ut);
  }
  else { /* First time call: register starting time */
    char *p1, tbuf[MAX_LINELEN+1];
    snprintf(dbuf,sizeof(dbuf),"%u",(unsigned int) nowtime);
    accessfile(dbuf,"w","%s",vbuf); start=nowtime;
/*  variable initializing before
    if(pc==NULL) pc=getvar("wims_class");
    if(pc==NULL) pc="";
    pu=getvar("wims_user"); if(pu==NULL) pu="";*/
    snprintf(vbuf,sizeof(vbuf),"%s/%s/.parmreg", class_base, pc);
    mkdirs(vbuf);
    snprintf(vbuf,sizeof(vbuf),"%s/%s/.parmreg/%s.exam",
         class_base, pc, pu);
    p1=remote_addr;
    snprintf(dbuf,sizeof(dbuf),"%s %s %u %d",
         p1, pb, (unsigned int) start+duration, t1);
    accessfile(dbuf,"w","%s",vbuf);
    accessfile(tbuf,"r","%s/%s/.E%d",class_base,pc,t1);
    if(strchr(tbuf,'#')==NULL && strcmp(pu,"supervisor")!=0) {  /* not for simulation */
      snprintf(tbuf,sizeof(tbuf),"%d 00 %d %u %s %s %s\n",
             t1, duration/60, (unsigned int) start,p1,pb,
             nowstr);
      if(prime!=0) {
        int n=strlen(tbuf);
          snprintf(tbuf+n,sizeof(tbuf)-n,"%d %.2f -1 %u %s %s\n",
                t1,prime,(unsigned int) start,p1,pb);
      }
      accessfile(tbuf,"a","%s/%s/score/%s.exam",
                 class_base,pc,pu);
    }
    else accessfile("yes","w","%s/%s/examsimu.%d",
          session_dir,pb,t1);
  }
  /* set up wims_scoring + wims_time_remain */
  if(pc==NULL) pc=getvar("wims_class");
  remain=duration+start-nowtime;
  if(pc) {
    int maxremain;
    maxremain=getscorestatus(pc,t1);
    /* negative return is score retriction timeleft */
    if (maxremain<0 && -maxremain < remain) {
      remain=-maxremain;
    }
    snprintf(vbuf,sizeof(vbuf),"%s/%s/examdepend.%d",session_dir,pb,t1);
    snprintf(dbuf,sizeof(dbuf),"%u",(unsigned int) nowtime+remain);
    accessfile(dbuf,"w","%s",vbuf);
  }
  if(remain<0) remain=0;
  snprintf(dbuf,sizeof(dbuf),"%d",remain);
  setvar("wims_exam_remain",dbuf);
}

double currexamscore(int sh)
{
  char buf[MAX_FNAME+1], sbuf[MAX_LINELEN+1];
  char *p, *p1, *cl;
  int i, excnt;
  double sc, w, ww, prime;

  cl=getvar("wims_class"); if(cl==NULL || *cl==0) return 0;
  mystrncpy(sbuf,session_prefix,sizeof(sbuf));
  p=strstr(sbuf,"_exam"); if(p) *p=0;
  accessfile(buf,"r","%s/examdata.%d",sbuf,sh);
  p=find_word_start(find_word_end(find_word_start(buf)));
  if(*p) prime=atof(p); else prime=0;
  exam_currscore(sh);
  snprintf(buf,sizeof(buf),"%s/%s/exams/.exam%d",class_base,cl,sh);
  direct_datafile=1;
  excnt=datafile_recordnum(buf);
  w=sc=0;
  for(i=0;i<excnt;i++) {
    sbuf[0]=0; datafile_fnd_record(buf,i+1,sbuf);
    p1=find_word_start(sbuf); *find_word_end(p1)=0;
    ww=atof(p1); if(ww<=0) continue;
    w+=ww; if(exam_scoredata[i]>-1000) sc+=exam_scoredata[i]*ww;
  }
  direct_datafile=0; if(sc<0) sc=0;
  if(w==0) return 0; else return (1-prime/10)*sc/w+prime;
}

void calc_examdep(char *p)
{
  char *p1, *p2;
  int t;
  if(!trusted_module()) {abt: *p=0; return;}
  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2==0) goto abt;
  *p2++=0; p2=find_word_start(p2);
  t=atoi(p1);
  snprintf(p,MAX_LINELEN, exam_depcheck(p2,t)? "yes" : "no");
}
