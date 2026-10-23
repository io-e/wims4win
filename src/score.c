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

/* Routines treating user scores. */
#include "wims.h"

double oldfactor=0.85;     /* quality factor, should remain stable. */

enum {sr_require, sr_weight, sr_score, sr_mean, sr_remain,
  sr_last, sr_try, sr_best, sr_level, sr_new, sr_seedlast, sr_seedscorelast,
  sr_seedscores, sr_seedlastcnt};
char scorebuf[MAX_CLASSEXOS*sizeof(scoreresult)+32];
struct scoreresult *rscore;
int scorecnt;
double scoresum[MAX_SHEETS];
int sheetstart[MAX_SHEETS], shexocnt[MAX_SHEETS];
int examstart, examcnt;
char rscore_class[MAX_CLASSLEN+1];
char rscore_user[MAX_NAMELEN+1];

int totsheets=0;
int score_ispublic=0;
int score_status=-1;          /* save of score status */
int score_statussheet=-1;
int score_statusisexam=-1;
double scorerecfactor=0.9;

/* gather user score, core routine.
  rscore data is created (table of structures).
  according to the value of wims_sheet or wims_exo
*/
int getscoreuser(char *classe, char *user)
{
  int i, osh, sh;
  char *nowuser, *nowsheet, *nowexo, *nowscore;
  char *pp;
  if(user==NULL || *user==0) {
    user=getvar("wims_user");
    if(user==NULL || *user==0) return -1;
  }
  if(strcmp(classe,rscore_class)==0 && strcmp(user,rscore_user)==0) return 0;
  nowsheet=nowexo=nowscore="";
  nowuser=getvar("wims_user");
  if(nowuser!=NULL && strcmp(user,nowuser)==0) {
    nowscore=getvar("module_score");
    if(nowscore!=NULL && *nowscore!=0) {
      nowsheet=getvar("wims_sheet");
      if(nowsheet==NULL) nowsheet="";
      nowexo=getvar("wims_exo");
      if(nowexo==NULL) nowexo="";
    }
    else nowscore="";
  }
  snprintf(scorebuf+sizeof(int),sizeof(scorebuf)-sizeof(int),
       "-c%s -u%s getscore %s %s %s",
       classe,user,nowsheet,nowexo,nowscore);
  i=kerneld(scorebuf,sizeof(scorebuf));
  if(i<0)  internal_error("getscoreuser(): daemon failure.");
  if(memcmp(scorebuf+sizeof(int),"OK",2)!=0) {
    if(memcmp(scorebuf+sizeof(int),"ERROR",5)==0) {
      module_error(find_word_start(scorebuf+sizeof(int)+8));
    }
    else internal_error("getscoreuser(): communication error with wimslogd.");
  }
/* 3 is here the length of "OK " */
  pp=scorebuf+sizeof(int)+3; rscore=(struct scoreresult *) pp;
  scorecnt=(i-sizeof(int)-3)/sizeof(scoreresult);
  if(scorecnt>MAX_CLASSEXOS) module_error("too_many_exercises");
  osh=-1;
  for(i=totsheets=0;i<scorecnt;i++) {
    sh=rscore[i].sh;
    if (sh >= MAX_SHEETS) break;
    while(osh<sh)
      {++osh; scoresum[osh]=shexocnt[osh]=0; sheetstart[osh]=i;}
    scoresum[sh]+=rscore[i].require*rscore[i].weight*rscore[i].active;
    shexocnt[sh]++;
  }
  totsheets=osh+1;

  examstart=i;  examcnt=scorecnt-examstart;
  mystrncpy(rscore_class,classe,sizeof(rscore_class));
  mystrncpy(rscore_user,user,sizeof(rscore_user));
  return 0;
}
/* work : exo */
char *scorepname[]={
  "class","user","sheet","work","exam"
};
#define scorepname_no (sizeof(scorepname)/sizeof(scorepname[0]))
char score_class[MAX_CLASSLEN+1];
int score_sheet,score_exo,score_isexam,score_exam;
char score_user[256];

/* Uniformed treatment of score command parameters
 * format: class=? user=? sheet=? work=?
 * all are optional.
 */
void _scoreparm(char *p)
{
    int i;
    char *pn, *pe, *pd, *pf;
    char sav;

    score_sheet=score_exo=score_isexam=score_exam=score_ispublic=0; *score_class=0;
    score_user[0]=0;
    for(i=0;i<scorepname_no;i++) {
      pf=p;
      ahead:
      pn=strstr(pf,scorepname[i]); pf=pn+1;
      if(pn==NULL) continue;
      if(pn>p && !isspace(*(pn-1))) goto ahead;
      pe=find_word_start(pn+strlen(scorepname[i]));
      if(*pe!='=') goto ahead;
      pd=find_word_start(pe+1);
      pf=find_word_end(pd);
      if(pf<=pd) continue;
      sav=*pf; *pf=0;
      switch(i) {
        case 0: /* class */
          mystrncpy(score_class,pd,sizeof(score_class)); break;
        case 1: /* user */
          mystrncpy(score_user,pd,sizeof(score_user)); break;
        case 2: { /* sheet */
          if(*pd=='P') {pd++; score_ispublic=1;}
          score_sheet=atoi(pd);
          break;
        }
        case 3: /* work=exo */
          score_exo=atoi(pd); break;
        case 4: /* exam */
          score_isexam=1;score_exam=atoi(pd); break;
     }
     *pf=sav; ovlstrcpy(pn, pf);
    }
    *p=0;
    /* the commands are OK from a non trusted module if the user and the
    class are not precised, so it can be only the user in his class */
    if((*score_class!=0 || score_user[0]!=0) && !trusted_module()) {
      module_error("not_trusted"); return;
    }
    if(*score_class==0) {
      char *classe;
      classe=getvar("wims_class");
      if(classe==NULL || *classe==0) return;
      else mystrncpy(score_class,classe,sizeof(score_class));
    }
    if(score_user[0]==0) {
     char *user;
     user=getvar("wims_user");
     if(user!=NULL) mystrncpy(score_user,user,sizeof(score_user));
    }
}

/* gather score: relatif to some of the
 *  user class work sheet exam
 *  dtype can be require weight score mean remain best last level try
 */

void _getscore(char *p,int dtype)
{
  int i,sh,ex,osh;
  float d;
  char dc[SEEDSIZE];
  char ds[SEEDSCORES];
  char *p1;

  _scoreparm(p);
  if(*score_class==0 || *score_user==0) return;
  if(getscoreuser(score_class,score_user)<0) return;
  for(i=osh=0,p1=p;i<scorecnt && p1-p<MAX_LINELEN-32;i++) {
    sh=rscore[i].sh+1; if(sh<1 || sh>MAX_SHEETS) break;
    if(score_sheet!=0) {
      if(sh<score_sheet) continue;
      if(sh>score_sheet || sh>MAX_SHEETS) break;
    }
    ex=rscore[i].exo+1;
    if(score_exo!=0 && ex!=score_exo) continue;
    if(osh!=0 && sh!=osh) *p1++='\n';
    switch(dtype) {
      case sr_require: {d=rscore[i].require; break;}
      case sr_weight: {d=rscore[i].weight; break;}
      case sr_score: {d=rscore[i].score; break;}
      case sr_mean: {d=rscore[i].mean; break;}
      case sr_remain: {d=rscore[i].require-rscore[i].score; break;}
      case sr_last: {d=rscore[i].last; break;}
      case sr_try: {d=rscore[i].try; break;}
      case sr_new: {d=rscore[i].new; break;}
      case sr_best: {d=rscore[i].best; break;}
      case sr_level: {d=rscore[i].level; break;}
      case sr_seedscorelast: {d=rscore[i].seedscorelast; break;}
      case sr_seedlastcnt: {d=rscore[i].seedlastcnt; break;}
      case sr_seedlast: { mystrncpy(dc,rscore[i].seedlast,SEEDSIZE);
        break;
      }
      case sr_seedscores: {
        mystrncpy(ds,rscore[i].seedscores,SEEDSCORES); break;
      }
      default: {d=0; break;}
    }
    switch(dtype) {
      case sr_seedlast:
        { snprintf(p1,SEEDSIZE, "%s", dc); p1+=strlen(p1); break; }
       case sr_seedscores:
        { snprintf(p1,SEEDSCORES, "%s", ds); p1+=strlen(p1); break; }
      default:
        { p1=moneyprint(p1,d); }
    }
    *p1++=' ';
    osh=sh;
  }
  *p1++='\n'; *p1=0;
}

/* gather user score. */
void calc_getscore(char *p)
{
  _getscore(p,sr_score);
}

/* gather user score average. */
void calc_getscoremean(char *p)
{
  _getscore(p,sr_mean);
}

/* gather remaining of score to get for user. */
void calc_getscoreremain(char *p)
{
  _getscore(p,sr_remain);
}

/* Require score table. */
void calc_getscorerequire(char *p)
{
  _getscore(p,sr_require);
}

/* Score weight table. */
void calc_getscoreweight(char *p)
{
  _getscore(p,sr_weight);
}
/* user last score */
void calc_getscorelast(char *p)
{
  _getscore(p,sr_last);
}
/* gather user score try numbers with score activated. */
void calc_getscoretry(char *p)
{
  _getscore(p,sr_try);
}
/* gather all user try numbers (terminated or not, but with notation activated)*/
void calc_getscorealltries(char *p)
{
  _getscore(p,sr_new);
}

/* if the required points are 10* N, gather N user best scores */
void calc_getscorebest(char *p)
{
  _getscore(p,sr_best);
}

/* gather user score average. */
void calc_getscorelevel(char *p)
{
  _getscore(p,sr_level);
}

/* percentage of work done for each sheet:
 * score mean best level
 * as follows
 * score=100*cumulative_points/required_points (< 100)
 * mean=quality (<10)
 * best=10*(required/10 best_scores)/required_points (< 100)
 * level=minimum of the required/10 best_scores (< 100)
 */

 void calc_getscorepercent(char *p)
{
  int i,j,jend;
  double tot, mean, totb, totl, totw, d;
  char *p1;

  _scoreparm(p);
  if(*score_class==0 || *score_user==0) return;
  if(getscoreuser(score_class,score_user)<0) return;
  for(p1=p,i=0;i<totsheets && p1-p<MAX_LINELEN-32;i++) {
    if(scoresum[i]==0) {
      ovlstrcpy(p1,"0 0 0 0\n"); p1+=strlen(p1); continue;
    }
    if(score_sheet!=0 && i!=score_sheet-1) continue;
    if(scoresum[i]<=0) *p1++='\n';
    tot=mean=totb=totl=totw=0; jend=sheetstart[i]+shexocnt[i];
    for(j=sheetstart[i];j<jend;j++) {
      /* if mean<1 then ignore score.
      * if mean<2 then half score.
      */
      if(rscore[j].mean>=1) {
        double dt=rscore[j].score;
        float db=rscore[j].best;
        float dl=rscore[j].level;
        if(rscore[j].mean<2) {dt=dt/2; db=db/2; dl=dl/2;}
        d=dt*rscore[j].weight*rscore[j].active;
        /* quality; barycenter with coefficients d */
        mean+=rscore[j].mean*d;
        /* cumulative score */
        tot+=d;
        /* best */
        totb+=db*rscore[j].weight*rscore[j].active;
        /* level */
        totl+=dl*rscore[j].weight*rscore[j].active;
        totw+=rscore[j].weight*rscore[j].active;
      }
    }
    if(tot>0) {d=mean/tot;} else d=0;
    /* cumulative score */
    p1=moneyprint(p1,rint(100*tot/scoresum[i])); *p1++=' ';
    /* quality */
    p1=moneyprint(p1,d); *p1++=' ';
    /* best */
    p1=moneyprint(p1,rint(100*totb/scoresum[i])); *p1++=' ';
    /* level */
    p1=moneyprint(p1,rint(10*totl/totw));

   *p1++='\n';
  }
  *p1=0;
}
/* [seed1,score1;seed2,score2;...] at most MAX_SCORESEED */
void calc_getseedscores(char *p)
{
  _getscore(p,sr_seedscores);
}
/* last seed (0 if there is no seed)*/
void calc_getseedlast(char *p)
{
  _getscore(p,sr_seedlast);
}
/* last score (-1 if does not exist) */
void calc_getseedscorelast(char *p)
{
  _getscore(p,sr_seedscorelast);
}

/* number of occurences of the last seed */
void calc_getseedlastcnt(char *p)
{
  _getscore(p,sr_seedlastcnt);
}

/* Returns the status of a sheet, or -1 if error */
int getsheetstatus(char *classe, int sheet)
{
  char *p, *st, buf[MAX_LINELEN+1], namebuf[MAX_FNAME+1];
  int i,j,res,lnb;
  char *classp, *classp2, *scp;
  char *p1,buf1[MAX_LINELEN],buf2[MAX_LINELEN];
  char buf3[MAX_LINELEN],buf4[MAX_LINELEN];
  if(isexam || score_isexam) {st="exam"; lnb=8;} else {st="sheet";lnb=9;};
  mkfname(namebuf,"%s/%s/%ss/.%ss",class_base,classe,st,st);
  direct_datafile=1;datafile_fnd_record(namebuf,sheet,buf);direct_datafile=0;
  p=find_word_start(buf); if(*p==0) return -1;
  i=*p-'0'; if(i>5 || i<0) i=-1;
  if((isexam || score_isexam) && i==0) {
    p=getvar("wims_user"); if(p!=NULL && strcmp(p,"supervisor")==0) i=1;
  }
  res=i;
  if (res == 0 || res > 2)
    goto ret;
  if (strcmp(score_user,"supervisor")==0 || score_user[0]==0)
    goto ret;
  fnd_line(buf,lnb,buf1);
  rows2lines(buf1);
  classp=getvar("wims_class"); scp=getvar("wims_superclass");
  if(scp!=NULL && *scp!=0) classp2=scp; else classp2=classp;
  mkfname(namebuf,"%s/%s/.users/%s",class_base,classp2,score_user);
  for(i=1;;++i){
    fnd_line(buf1,i,buf);
    if(*buf==0) break;
    for (j=1;;++j){
	    fnd_item(buf,j,buf2);
	    if (buf2[0]==0) {res = 3; goto ret;}
	    p1=strchr(buf2,'=');
	    if(p1==NULL) break;
	    *p1=0;
	    snprintf(buf3,sizeof(buf3),"%s_%s","user_techvar",buf2);
	    getdef(namebuf,buf3,buf4);
	    if(strcmp(buf4,p1+1)) break;
	  }
  }
  ret:
  return res;
}

/* return 1 if a word of bf2 is a substring of host
  and if time restrictions are verified for wims_user
 * Content of bf2 is destroyed.
 */
int _subword(char bf2[],char *ftbuf)
{
  char *p1, *p2;
  for(p1=strchr(bf2,'\\'); p1!=NULL; p1=strchr(p1+1,'\\')) {
    char buf[MAX_LINELEN+1], buf2[MAX_LINELEN+1], fbuf[MAX_FNAME+1];
    char *classp, *classp2, *userp, *scp;
    classp=getvar("wims_class"); userp=getvar("wims_user");
    if(classp==NULL || userp==NULL || *classp==0 || *userp==0) break;
    scp=getvar("wims_superclass");
    if(scp!=NULL && *scp!=0) classp2=scp; else classp2=classp;
    if(p1>bf2 && !isspace(*(p1-1))) continue;
    if(!isalnum(*(p1+1))) continue;
    p2=find_word_end(p1); if(p2>=p1+MAX_NAMELEN) continue;
    memmove(buf2, p1+1, p2-p1-1); buf2[p2-p1-1]=0;
    /* get value of technical variable for user */
    snprintf(buf,sizeof(buf),"user_techvar_%s",buf2);
    if(strcmp(userp,"supervisor")==0)
      mkfname(fbuf,"%s/%s/supervisor",class_base,classp);
    else
      mkfname(fbuf,"%s/%s/.users/%s",class_base,classp2,userp);
    getdef(fbuf,buf,buf2);
    /* end value for the user */
    if(buf2[0]==0) ovlstrcpy(buf2,"EMPTY"); /* in case of no value defined for the user*/
    /* get time restriction for this value */
    snprintf(buf,sizeof(buf),"techvar_%s",buf2);
    /* mkfname(fbuf,"%s/%s/.E%s",class_base,classp,sheet);
     * read time restriction corresponding to the value of
     * technical variable in the file of the sheet or exam
    */
    getdef(ftbuf,buf,buf2);
    /* value for techvar */
    if(buf2[0]==0) ovlstrcpy(buf2,"none");
    /*string_modify(bf2,p1,p2,buf2);*/
    bf2=buf2;
    p1+=strlen(buf2);
  }
  if((isexam || score_isexam) && bf2[0]=='#') return 1;
  if(wordchr(bf2,"none")!=NULL) return 0;
  if(wordchr(bf2,"all")!=NULL) return 1;
  p1=find_word_start(bf2); if(*p1==0) return 1;
  /* check host and time */
  return checkhostt(p1);
}

/* Returns 1 if score registration is open for the user
 * in variable wims_user, 0 otherwise.
 */
int _getscorestatus(char *classe, int sheet)
{
  char nbuf[MAX_LINELEN+1], gbuf[MAX_LINELEN+1];
  char ftbuf[MAX_FNAME+1]="";
  char *es;

  if(classe==NULL || *classe==0 || sheet<=0) return 1;
  if(getsheetstatus(classe,sheet)!=1) return 0;
  if(*remote_addr==0) return 0;
  if(isexam || score_isexam) {     /* exam simulation */
    accessfile(nbuf,"r","%s/%s/.E%d",class_base,classe,sheet);
    if(nbuf[0]=='#') return 1;
  }
  /* Global restriction data */
  accessfile(nbuf,"r","%s/%s/.security",class_base,classe);
  if(nbuf[0]) {
    _getdef(nbuf,"allow",gbuf);
    if(*find_word_start(gbuf)!=0 && _subword(gbuf,ftbuf)==0)
      return 0;
    _getdef(nbuf,"except",gbuf);
    if(*find_word_start(gbuf)!=0 && _subword(gbuf,ftbuf)==1)
      return 0;
  }
  /* Sheet restriction data; take in account the technical variables */
  if(isexam || score_isexam) es="E"; else es="";
  accessfile(nbuf,"r","%s/%s/.%s%d",class_base,classe,es,sheet);
  if(*find_word_start(nbuf)==0) return 1;
  /* preparing score restriction file name :*/
  mkfname(ftbuf,"%s/%s/.%s%d",class_base,classe,es,sheet);
  return _subword(nbuf,ftbuf);
}

/* Returns 1 if score registration is open for wims_user, 0 otherwise.*/
int getscorestatus(char *classe, int sheet)
{
  if(score_status<0 || score_statussheet!=sheet
     || score_statusisexam!=isexam) {
    score_statussheet=sheet; score_statusisexam=isexam;
    score_status=_getscorestatus(classe,sheet); score_isexam=0;
    if(score_status!=0 && (cmd_type==cmd_new || cmd_type==cmd_renew
               || isexam)) {
      char *p;
      p=getvar("wims_scorereg");
      if(p==NULL || strcmp(p,"suspend")!=0)
         setvar("wims_scoring","pending");
      else setvar("wims_scoring","");
    }
  }
  if(isexam && score_status==0) {
    char *p;
    p=getvar("wims_user");
    if(p==NULL || strcmp(p,"supervisor")!=0)
      user_error("exam_closed");
  }
  return score_status;
}

/* Whether score registering is open */
void calc_getscorestatus(char *p)
{
  _scoreparm(p);
  if(*score_class==0 || score_sheet==0 || *score_user==0) {
    *p=0; return;
  }
  if(getscorestatus(score_class, score_sheet))
    ovlstrcpy(p,"yes");
  else ovlstrcpy(p,"no");
}

/* get status of a sheet in a class: can be 0, 1, 2 or 3
  (in preparation, active, expired or hidden) */
void calc_getsheetstatus(char *p)
{
  _scoreparm(p);
  if(*score_class==0 || (score_sheet==0 && score_isexam==0)) {
    *p=0; return;
  }
  snprintf(p,20,"%d",getsheetstatus(score_class, score_sheet?score_sheet:score_exam));
}

/* whether there are too much tries for the exo return yes or no */
void calc_getscoremaxexotry(char *p)
{
  _scoreparm(p);
  if(*score_class==0 || *score_user==0 || score_sheet==0 || score_exo==0) return;
  if(gettrycheck(score_class, score_user, score_sheet, score_exo)==1)
    ovlstrcpy(p,"yes");
  else ovlstrcpy(p,"no");
}

double exam_scoredata[MAX_EXAMS];

/* get current exam score */
void exam_currscore(int esh)
{
  char *p, *bf, pb[MAX_FNAME+1];
  char *s, *p1, *p2, *e1, *e2;
  int i;
  for(i=0;i<MAX_EXAMS;i++) exam_scoredata[i]=-1000;
  /* session_prefix is not yet defined here */
  s=getvar("wims_session"); if(s==NULL || *s==0) return;
  mystrncpy(pb,s,sizeof(pb));
  p=strchr(pb,'_'); if(p!=NULL) *p=0;
  bf=readfile(mkfname(NULL,"%s/%s/examscore.%d",session_dir,pb,esh),NULL,WORKFILE_LIMIT);
  if(bf==NULL) return;
  for(p1=bf;*p1;p1=p2) {
    p2=strchr(p1,'\n'); if(p2!=NULL) *p2++=0;
    else p2=p1+strlen(p1);
    p1=find_word_start(find_word_end(find_word_start(p1)));
    e1=find_word_end(p1); if(*e1) *e1++=0;
    e1=find_word_start(e1); e2=find_word_start(find_word_end(e1));
    *find_word_end(e1)=0;
    i=atoi(p1);
    if(i>=1 && i<=MAX_EXAMS &&
       exam_scoredata[i-1]==-1000 && strcmp(e1,"score")==0) {
      *find_word_end(e2)=0;
      exam_scoredata[i-1]=atof(e2);
    }
  }
  free(bf);
}

/* Gather exam score. */
void calc_examscore(char *p)
{
  char *p1;
  int i;
  char *withoutip;

  _scoreparm(p); *p=0;
  withoutip=getvar("wims_examscore_withoutip");

  if(*score_class==0 || *score_user==0) return;
  if(getscoreuser(score_class,score_user)<0) return;
  p1=p;
  if(withoutip!=NULL && strcmp(withoutip,"yes")==0) {
    for(i=0; i<examcnt && p1-p<MAX_LINELEN-32; i++) {
    p1=moneyprint(p1,rscore[examstart+i].best); *p1++=' ';
    }
  } else {
    for(i=0; i<examcnt && p1-p<MAX_LINELEN-32; i++) {
      p1=moneyprint(p1,rscore[examstart+i].score); *p1++=' ';
    }
  }
  *p1++='\n';
  for(i=0; i<examcnt && p1-p<MAX_LINELEN-32; i++) {
    p1=moneyprint(p1,rscore[examstart+i].require); *p1++=' ';
    p1=moneyprint(p1,floor(rscore[examstart+i].mean/2)); *p1++=' ';
    p1=moneyprint(p1,(int) rscore[examstart+i].mean%2); *p1++='\n';
  }
  *p1=0;
}

/* check score dependency.
 * returns 1 if requirements are met.
 */
int _depcheck(char *ds, struct scoreresult *rs, int ecnt)
{
  char *p1, *p2, *p3, *p4, *pp;
  int perc, t, sum;
  double tgot, ttot, tmean;

  for(p1=ds; *p1; p1=p3) {
    p2=strchr(p1,':'); if(p2==NULL) break;
    *p2++=0; p2=find_word_start(p2);
    for(p3=p2; myisdigit(*p3); p3++);
    if(p3<=p2) break;
    if(*p3) *p3++=0;
    perc=atoi(p2);
    if(perc<=0 || perc>100) break;
    for(pp=p1; *pp; pp++) if(!myisdigit(*pp)) *pp=' ';
    tgot=ttot=tmean=0; sum=0;
    for(pp=find_word_start(p1); *pp; pp=find_word_start(p4)) {
      p4=find_word_end(pp); if(*p4) *p4++=0;
      t=atoi(pp); if(t<=0 || t>ecnt) goto lend;
      t--;
      ttot+=rs[t].require; tgot+=rs[t].score; tmean+=rs[t].mean;
      sum++;
    }
    if(ttot<10) continue;
    if(tgot/ttot*sqrt(tmean/(sum*10))*100<perc) {
      for(pp=p1;pp<p2-1;pp++) if(!*pp) *pp=',';
      *pp=0; setvar("dep_list",p1);
      return 0;
    }
  lend: ;
  }
  return 1;
}

int depcheck(char *sh, int exo, char *deps)
{
  char buf[MAX_LINELEN+1];
  char *s, sbuf[64];
  int i, is;

  s=getvar("wims_session");
  if(s==NULL || *s==0 || strstr(s,"robot")!=NULL) return 0;
  mystrncpy(sbuf,s,sizeof(sbuf));
  s=strchr(sbuf,'_'); if(s) *s=0;
  accessfile(buf,"r","../sessions/%s/exodep.%s",sbuf,sh);
  if(buf[0]==0) {     /* no dep file found */
    is=atoi(sh); if(is<=0 || is>totsheets) return 0;
    s=getvar("wims_class"); if(s==NULL || *s==0) return 0;
    getscoreuser(s,"");
    return _depcheck(deps,rscore+sheetstart[is-1],shexocnt[is-1]);
  }
  for(i=1,s=strchr(buf,':'); s && i<exo; i++, s=strchr(s+1,':'));
  if(s==NULL) return 0;     /* bad file or exo number */
  if(myisdigit(*++s)) return 0; else return 1;
}

int exam_depcheck(char *deps, int exam)
{
  static struct scoreresult esc[MAX_EXAMS];
  int i;
  exam_currscore(exam);
  for(i=0;i<MAX_EXAMS;i++) {
    esc[i].require=esc[i].mean=10;
    if(exam_scoredata[i]==-1000) esc[i].score=0;
    else esc[i].score=exam_scoredata[i];
  }
  return _depcheck(deps,esc,MAX_EXAMS);
}

/* public sheet gives she=0 */
/* return 1 if score is no more taken in account because of exotrymax */
int gettrycheck(char *classe, char *user, int she, int exo) {
  char *s;
  int sh, ex, i;
  if(classe==NULL || *classe==0 || user==NULL || *user==0) return 0;
  if (she<=0) return 0;
    if(strcmp(user,"supervisor")==0) return 0;
  s=getvar("exotrymax");
  if(s==NULL || *s==0) return 0;
  getscoreuser(classe,user);
  char *p=getvar("wims_scorereg");
  if(p==NULL) p="";
  for(i=0;i<scorecnt;i++) {
    sh=rscore[i].sh+1;
    if (she!=sh) continue;
    ex=rscore[i].exo+1;
    if(exo!=ex) continue;
    if(cmd_type==cmd_new || cmd_type==cmd_renew)
      if(rscore[i].new >= atoi(s)) {
        if(strcmp(p,"suspend")!=0) setvar("wims_scorereg","exotrymax");
        return 1;
    }
    if(rscore[i].new > atoi(s)){
      if(strcmp(p,"suspend")!=0) setvar("wims_scorereg","exotrymax");
      return 1;
    }
  }
  return 0;
}

/* return seed if the seed should be kept and NULL if not */
char *getseedscore(char *classe, char *user, int she, int exo) {
  char *s;
  int sh, ex, i;
  if (she<=0) return NULL;
  if(strcmp(user,"supervisor")==0) return NULL;
  s=getvar("seedrepeat");
  if(s==NULL || *s==0 || atoi(s)<=0) return NULL;
  getscoreuser(classe,user);
  for(i=0;i<scorecnt;i++) {
    sh=rscore[i].sh+1;
    if (she!=sh) continue;
    ex=rscore[i].exo+1;
    if(exo!=ex) continue;
    if(atoi(rscore[i].seedlast)==0) {
      char *seed=getvar("wims_seed");
      return seed;
    }
    if(rscore[i].seedlastcnt > MAX_SEEDSCORE) return NULL;
    if(atoi(s)>0 && rscore[i].seedlastcnt >= atoi(s) && cmd_type!=cmd_next) return NULL;
    if(rscore[i].seedscorelast < 10) return rscore[i].seedlast;
  };
  return NULL;
}
