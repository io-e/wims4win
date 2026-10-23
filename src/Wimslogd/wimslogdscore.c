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

/* student score management */

#include "wimslogd.h"
#define MAX_TRY 60000
double oldfactor=0.85;  /* quality factor, should remain stable. */

/* User score information of an exercise.
 * size in bytes : 32 +4*MAX_REQUIRE/10 + MAX_SCORESEED*(SEEDSIZE+4)
 * information to change if struct scoredata changes
 */
typedef struct scoredata {
  unsigned short int new, allnew, try, hint, seed_scorecnt, seedlastcnt;
  float user, user2, last, best, level, high[MAX_REQUIRE/10];
  struct{
    char seed[SEEDSIZE];
    float score;
  } seed_score[MAX_SCORESEED];
} scoredata;

struct scoredata uscore[MAX_CLASSEXOS];

/* size in bytes: 200
 * information to change if scoreheader changes
 */
struct scoreheader {
  char raf[8][20];
  int sheet, exo;
  char session[32];
} scoreheader;


#define oldraf scoreheader.raf
#define oldsheet scoreheader.sheet
#define oldexo scoreheader.exo
#define oldsession scoreheader.session

/* one line of score:
  format (see log.c):
  exnowstr sess sh exo cc\tip\tse\tallow
  allow contains information about noscore
  noscore must be at the end to be taken in account
  noscore can be preceded by an explaining word for the moment maxtry
  se is the seed
  tabulation is useful in adm/class/userscore/getraw.sh
*/
void scoreline(struct classdata *cd, char *l)
{
  int i,sheet,exo,num,indnum;
  char *pm[16];
  struct scoredata *thiscore;
  struct sheetdata *ts;
  float score;

  i=cutwords(l,pm,12); if(i<6) return;
  sheet=atoi(pm[2]); exo=atoi(pm[3]);
  if(sheet<=0 || sheet>MAX_SHEETS || exo<=0 || exo>MAX_EXOS) return;
  ts=cd->sheets+sheet-1;
  num=ts->start+exo-1;
  indnum=ts->indstart + exo-1 + ts->exocnt*ts->techval;
  if(num<0) return;
  if(strcmp(pm[i-1],"noscore")==0 || strcmp(pm[i-1],"erased")==0) {
    if(strcmp(pm[1],oldsession)!=0)
      mystrncpy(oldsession,pm[1],sizeof(oldsession));
    if(strcmp(pm[4],"new")==0 || strcmp(pm[4],"renew")==0) {
      thiscore=uscore+num;
      thiscore->allnew ++;
      if(i>8) {
        /* copy-paste: seeds are kept even if the last word of the line
          contains noscore
         */
        if(thiscore->seed_scorecnt==MAX_SCORESEED) {
          int k;
          /* when one reaches the limit one kept at the end only what is necessary to
            test if the seed should be changed so MAX_SEEDSCORE
            one wants to keep the first ones (for exotrymax test)
           */
          for(k=MAX_SCORESEED-MAX_SEEDSCORE; k<MAX_SCORESEED; ++k)
            thiscore->seed_score[k-1]=thiscore->seed_score[k];
          thiscore->seed_scorecnt--;
        }
        mystrncpy(thiscore->seed_score[thiscore->seed_scorecnt].seed,pm[6],SEEDSIZE);
        thiscore->seed_score[thiscore->seed_scorecnt++].score=-2;
        thiscore->seedlastcnt=1;
        {int k;
          for(k=thiscore->seed_scorecnt-1; k>=1 &&
              strcmp(thiscore->seed_score[k].seed,thiscore->seed_score[k-1].seed)==0;k--)
            thiscore->seedlastcnt++;
        }
      }
    }
    return;
  }
  thiscore=uscore+num;
  /* line with score in word number 5*/
  if(strcmp(pm[4],"score")==0) {
    score=atof(pm[5]);
    if(!isfinite(score)) score=0;
    if(score>10) score=10;
    if(score<-10) score=-10;

    if(strcmp(pm[1],oldsession)==0 &&   /* measure to prohibit simultaneous scoring. */
        sheet==oldsheet && exo==oldexo &&
        strncmp(pm[0],oldraf[6],13)!=0   /* prohibit scores immediately after rafale */
        ) {
      thiscore->user+=score;
      thiscore->user2*=oldfactor;
      thiscore->user2+=score;
      thiscore->last=score;
      if (thiscore->high[0] < score)
      {
        int k;
        thiscore->best += (score - thiscore->high[0]);
        for (k = 1; 10*k < cd->exos[indnum].require && thiscore->high[k] < score; k++)
        thiscore->high[k-1] = thiscore->high[k];
        thiscore->high[k-1] = score;
        thiscore->level=thiscore->high[0];
      }
      if(thiscore->try<MAX_TRY) thiscore->try++;
      oldsheet=oldexo=0;
      thiscore->seed_score[thiscore->seed_scorecnt-1].score=score;
    }
  }
  /* end of line with score */
  else {
    if(strcmp(pm[4],"rafale")==0) { /* rafale punishment */
      if(strncmp(pm[0],oldraf[3],13)==0 && thiscore->new<MAX_TRY)
        { thiscore->new++; thiscore->allnew++; }
      memmove(oldraf[1],oldraf[0],sizeof(oldraf[0])*7);
      mystrncpy(oldraf[0],pm[0],sizeof(oldraf[0]));
    }
    if(strcmp(pm[4],"resume")!=0 && strcmp(pm[4],"rafale")!=0) {
      if(strcmp(pm[4],"hint")==0) thiscore->hint++;
      else if(thiscore->new<MAX_TRY) {
        thiscore->new++;
        if(thiscore->allnew<MAX_TRY) thiscore->allnew++;
      }
      /* what about resume ? */
      if((strcmp(pm[4],"new")==0 || strcmp(pm[4],"renew")==0) && i>6){
      /* the first seed is forgotten if there is already MAX_SCORESEED */
        if(thiscore->seed_scorecnt==MAX_SCORESEED) {
          int k;
          for(k=1; k<MAX_SCORESEED; ++k)
            thiscore->seed_score[k-1]=thiscore->seed_score[k];
          thiscore->seed_scorecnt--;
        }
        mystrncpy(thiscore->seed_score[thiscore->seed_scorecnt].seed,pm[6],SEEDSIZE);
        thiscore->seed_score[thiscore->seed_scorecnt++].score=-1;
        thiscore->seedlastcnt=1;
        {int k;
          for(k=thiscore->seed_scorecnt-1; k>=1 &&
              strcmp(thiscore->seed_score[k].seed,thiscore->seed_score[k-1].seed)==0;k--)
            thiscore->seedlastcnt++;
        }
      }
    }
    mystrncpy(oldsession,pm[1],sizeof(oldsession));
    oldsheet=sheet; oldexo=exo;
  }
}

unsigned int _cuttime(char ends[], char starts[], unsigned int startn)
{
  int h1,h2,m1,m2,s2, t;
  if(ends[0]==0) return 0;
  if(strncmp(ends,starts,14)<0) return 10;
  if(strncmp(ends,starts,8)>0) return 0;
  h1=atoi(ends+9);   m1=atoi(ends+12);
  h2=atoi(starts+9); m2=atoi(starts+12); s2=atoi(starts+15);
  t=((h1-h2)*60+(m1-m2))*60-s2;
  return startn+t;
}

/* Gather exam score. */
void examscorecalc(struct classdata *cd, char *uname)
{
  struct scoredata *thiscore;
  char nbuf[MAX_FNAME+1];
  char cuttimes[MAX_EXAMS][16];
  char rbuf[MAX_FILELEN+1];
  char *wlist[8];
  char *p1, *p2;
  int i, k, ecnt, num;
  double ss, sc[MAX_EXAMS], sc2[MAX_EXAMS], scb[MAX_EXAMS], scb2[MAX_EXAMS];
  unsigned int tr[MAX_EXAMS], all[MAX_EXAMS], ver[MAX_EXAMS], start[MAX_EXAMS], dure[MAX_EXAMS];
  char *ip[MAX_EXAMS], *ses[MAX_EXAMS], *cut[MAX_EXAMS];
  unsigned int start1, endtime[MAX_EXAMS];
  signed int dure1;

  ecnt=cd->examcnt;
  if(ecnt<=0) return;
  if(ecnt>MAX_EXAMS) ecnt=MAX_EXAMS;
  memset(all,0,sizeof(all)), memset(ver,0,sizeof(ver));
  for(i=0;i<ecnt;i++) {
    k=cd->exam.indstart+i;
    all[i]=cd->exos[k].require;
    cut[i]=cd->ctbuf+cd->ctptr[i];
  }
  memset(sc,0,sizeof(sc)); memset(sc2,0,sizeof(sc2));
  memset(scb,0,sizeof(scb)); memset(scb2,0,sizeof(scb2));
  memset(tr,0,sizeof(tr)); memset(cuttimes,0,sizeof(cuttimes));
  memset(dure,0,sizeof(dure)); memset(start,0,sizeof(start));
  memset(endtime,0,sizeof(endtime));
  memset(ip,0,sizeof(ip)); memset(ses,0,sizeof(ses));
  snprintf(nbuf,sizeof(nbuf),"score/%s.exam",uname);
  readfile(nbuf,rbuf,sizeof(rbuf));
  if(rbuf[0]==0) goto end;
  for(p1=rbuf; p1!=NULL && *p1; p1=p2) {
    p2=strchr(p1,'\n'); if(p2!=NULL) *p2++=0;
    i=cutwords(find_word_start(p1),wlist,7);
    if(i<6) continue;
    i=atoi(wlist[0])-1; if(i<0 || i>=ecnt) continue;
    dure1=atoi(wlist[2]); start1=atoi(wlist[3]);
    if(strcmp(wlist[1],"--")==0) {     /* session closure */
      start[i]=dure[i]=0; ip[i]=ses[i]="";
      continue;
    }
    if(strcmp(wlist[1],"00")==0) {
      if(sc2[i]<sc[i]) sc2[i]=sc[i];
      if(scb2[i]<scb[i]) scb2[i]=scb[i];
      ver[i]=1; tr[i]++; start[i]=start1; dure[i]=dure1; sc[i]=0; scb[i]=0;
      ip[i]=wlist[4]; ses[i]=wlist[5];
      if(tr[i]==1 && wlist[6]!=NULL) {
        char *pp1, *pp2, lbuf[CTBUFLEN];
        mystrncpy(lbuf,cut[i],sizeof(lbuf));
        if(lbuf[0]) {
          for(pp1=find_word_start(lbuf); *pp1; pp1=find_word_start(pp2)) {
            pp2=find_word_end(pp1); if(pp2-pp1!=14) continue;
            if(*pp2) *pp2++=0;
            pp1[8]='.'; pp1[11]=':';
            if(strcmp(pp1,wlist[6])<0) continue;
            memmove(cuttimes[i],pp1,15); break;
          }
        }
      }
      endtime[i]=_cuttime(cuttimes[i],wlist[6],start1);
    }
    else if(ver[i]==0) tr[i]++;
    if(tr[i]>all[i]) continue;
    ss=atof(wlist[1]); if(ss<=0) continue; if(ss>10) ss=10;
    /* checking conditions with ip checking*/
    if(ss!=sc[i] && (dure1>=0 || (
        start1-start[i]<dure[i]*60 &&
        dure[i]>0 && dure[i]<4096 &&
        *ses[i]!=0 && *ip[i]!=0 &&
        start[i]!=0 && start1>start[i] &&
        (endtime[i]==0 || endtime[i]>=start1) &&
        strcmp(ip[i],wlist[4])==0 &&
        strcmp(ses[i],wlist[5])==0)))
      sc[i]=ss;
    /* checking conditions without ip checking -- will be in structure best */
    if(ss!=scb[i] && (dure1>=0 ||
        (start1-start[i]<dure[i]*60 &&
         dure[i]>0 && dure[i]<4096 &&
         *ses[i]!=0 && *ip[i]!=0 &&
         start[i]!=0 && start1>start[i] &&
         (endtime[i]==0 || endtime[i]>=start1) &&
         strcmp(ses[i],wlist[5])==0)))
      scb[i]=ss;
  }
  end:
  for(i=0; i<ecnt; i++) {
    if(sc2[i]<sc[i]) sc2[i]=sc[i];
    if(scb2[i]<scb[i]) scb2[i]=scb[i];
    num=cd->exocnt - cd->examcnt+i;
    if(num<0) continue;
    thiscore=uscore+num;
    thiscore->user=sc2[i];
    thiscore->best=scb2[i];
    thiscore->try=tr[i];
    if(cuttimes[i][0] && strncmp(cuttimes[i],nowstr,14)<0) k=0; else k=1;
    thiscore->hint=k;
  }
}

/* calculate score from raw data, core routine. */
void rawscorecalc(struct classdata *cd, char *uname)
{
  char fbuf[MAX_FILELEN+1];
  char *p1, *p2;
  char namebuf[MAX_FNAME+1];
  /* initialize everything to zero */
  memset(uscore,0,sizeof(uscore[0])*cd->exocnt);
  memset(&scoreheader,0,sizeof(scoreheader));
  snprintf(namebuf,sizeof(namebuf),"score/%s",uname);
  readfile(namebuf,fbuf,sizeof(fbuf));
  if(fbuf[0]!=0) {
    oldsession[0]=oldsheet=oldexo=0;
    for(p1=fbuf; *p1; p1=p2) {
      p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
      if(myisdigit(*p1)) scoreline(cd,p1);
    }
  }
  examscorecalc(cd,uname);
}

/* size of the file *.bin:
 * 200 + (28+4*MAX_REQUIRE/10)*(number_exos_in_sheets + number_exams)
 * information to change if struct scoredata or scoreheader change
 */
void savescorebin(struct classdata *cd, char *uname)
{
  int fd, cnt;
  char fname[MAX_FNAME+1];
  snprintf(fname,sizeof(fname),"score/%s.bin",uname);
  cnt=cd->exocnt;
  fd=creat(fname,S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH);
  if(fd==-1) return;
  (void)write(fd,&scoreheader,sizeof(scoreheader));
  (void)write(fd,uscore,sizeof(uscore[0])*cnt);
  close(fd);
}

void readscorebin(char *fname,int cnt)
{
  int fd;
  fd=open(fname,O_RDONLY);
  if(fd==-1) return;
  (void)read(fd,&scoreheader,sizeof(scoreheader));
  (void)read(fd,uscore,sizeof(uscore[0])*cnt);
  close(fd);
}
/* copie provisoire  dans wimslogd venant de wims */
/*  Get variable definition from a file.
 * Result stored in buffer value of length MAX_LINELEN.
 */
void _getdef(char buf[], char *name, char value[])
{
  char *p1, *p2, *p3, *p4;

  if(*name==0) goto nothing;      /* this would create segfault. */
  for(p1=strstr(buf,name); p1!=NULL; p1=strstr(p1+1,name)) {
    p2=find_word_start(p1+strlen(name));
    if((p1>buf && !isspace(*(p1-1))) || *p2!='=') continue;
    p3=p1; while(p3>buf && *(p3-1)!='\n') p3--;
    p3=find_word_start(p3);
    if(p3<p1 && *p3!='!') continue;
    if(p3<p1) {
      p3++; p4=find_word_end(p3);
      if(find_word_start(p4)!=p1) continue;
      if(p4-p3!=3 || (strncmp(p3,"set",3)!=0 &&
           strncmp(p3,"let",3)!=0 &&
           strncmp(p3,"def",3)!=0)) {
        if(p4-p3!=6 || strncmp(p3,"define",6)!=0) continue;
      }
    }
    p2++;p3=strchr(p2,'\n'); if(p3==NULL) p3=p2+strlen(p2);
    p2=find_word_start(p2);
    if(p2>p3) goto nothing;
    /*if(p3-p2>=MAX_LINELEN) user_error("cmd_output_too_long");*/
    memmove(value,p2,p3-p2); value[p3-p2]=0;
    strip_trailing_spaces(value); return;
  }
nothing:
  value[0]=0;
}

void getdef(char *fname, char *name, char value[])
{
  FILE *f;
  char *buf;
  int l;

  value[0]=0;
  f=fopen(fname,"r"); if(f==NULL) return;
  fseek(f,0,SEEK_END); l=ftell(f); fseek(f,0,SEEK_SET);
  buf=xmalloc(l+256); l=fread(buf,1,l,f);
  fclose(f);
  if(l<=0) return; else buf[l]=0;
  _getdef(buf,name,value);
  free(buf);
}
/* define number of the technical variable associated to user by sheet */
void techvals (struct classdata *cd, char *user)
{
  char fname[MAX_FNAME+1];
  char techname[MAX_FNAME+1], techval[MAX_FNAME+1];
  char *p1;
  int i, j;

  // snprintf(fname,sizeof(fname),".users/%s",user);
  if(cd->sclass[0] != 0 ){
    snprintf(fname,sizeof(fname),"%s/%s/%s/.users/%s",cwd,classd,cd->sclass,user);
  } else {
    snprintf(fname,sizeof(fname),"%s/%s/%s/.users/%s",cwd,classd,cd->name,user);
  }
  for (i = 0; i < cd->sheetcnt; ++i){
    cd->sheets[i].techval=0;
    if (cd->sheets[i].techcnt>1) {
      p1 = cd->techs + cd->sheets[i].techoffset;
      snprintf(techname,sizeof(techname),"user_techvar_%s", p1);
      getdef (fname, techname, techval);
      if (techval[0])
        for (j = 0; j < cd->sheets[i].techcnt; j++){
          p1 += strlen(p1); p1++;
          if (!strcmp(techval, p1)) {cd->sheets[i].techval=j; break;}
        }
      }
  }
}

void getscore(struct classdata *cd, char *user)
{
  struct stat st[3];
  int i, cnt, non[3];
  char buf[3][MAX_FNAME+1];

  snprintf(buf[0],sizeof(buf[0]),"score/%s",user);
  snprintf(buf[1],sizeof(buf[1]),"score/%s.exam",user);
  snprintf(buf[2],sizeof(buf[2]),"score/%s.bin",user);
  cnt=cd->exocnt; if(cnt<=0) return;
  for(i=0;i<3;i++) non[i]=stat(buf[i],st+i);
  if(non[0] && non[1]) {
    memset(uscore,0,sizeof(uscore[0])*cnt);
    memset(&scoreheader,0,sizeof(scoreheader));
    return;
  }
  if(!non[2] &&
      st[2].st_size==sizeof(scoreheader)+sizeof(uscore[0])*cnt &&
      (non[0] || st[2].st_mtime>=st[0].st_mtime) &&
      st[2].st_mtime>=cd->modif) {
    readscorebin(buf[2],cnt);
    if(!non[1] && st[2].st_mtime<st[1].st_mtime) {
      examscorecalc(cd,user);
      savescorebin(cd,user);
    }
    return;
  }
  rawscorecalc(cd,user);
  savescorebin(cd,user);
}

void cmd_getscore(char *p)
{
  struct classdata *cd;
  struct scoreresult tscore[MAX_CLASSEXOS];
  char *cut[4];
  int i, j, sheet, exo, snew, stry, thissheet, thisexo;
  double score, score2, quality, tt, ts, thisscore, sbest;
  float slevel=0;

  if(cwdtype!=dir_class) {
    sockerror(2,"getscore_no_class"); return;
  }
  if(*opt_user==0) {
    sockerror(2,"getscore_no_user"); return;
  }
  cd=getclasscache(opt_class);
  if(cd==NULL) {
    sockerror(2,"getscore_bad_class"); return;
  }
  if(cutwords(p,cut,3)==3) {
    thissheet=atoi(cut[0])-1; thisexo=atoi(cut[1])-1; thisscore=atof(cut[2]);
    if(!isfinite(thisscore)) thisscore=0;
    if(thisscore<-10) thisscore=-10;
    if(thisscore>10) thisscore=10;
  }
  else {thissheet=thisexo=-1;thisscore=0;}
  techvals(cd,opt_user);
  getscore(cd,opt_user);
  for(i=sheet=0;sheet<=cd->sheetcnt;sheet++){
    for(exo=0;exo<cd->sheets[sheet].exocnt;exo++,i++) {
      /* j <-> i is the correspondance between the numerotation of
        exercises with all versions and with one version */
      j=cd->sheets[sheet].indstart+cd->sheets[sheet].techval*cd->sheets[sheet].exocnt+exo;
      tscore[i].require=cd->exos[j].require;
      if(cd->exos[j].require==0) {tscore[i].weight=0;}
        else {tscore[i].weight=cd->exos[j].weight;}
      tscore[i].active=cd->exos[j].active;
      tscore[i].sh=sheet;
      tscore[i].exo=exo;
      score=uscore[i].user;
      stry=uscore[i].try;
      score2=uscore[i].user2;
      sbest=uscore[i].best;
      slevel=uscore[i].level;
      /* case of one exo in a sheet */
      if(sheet==thissheet && exo==thisexo) {
        score+=thisscore;
        stry++;
        score2*=oldfactor; score2+=thisscore;
        /* one compute the new sbest and slevel */
        if (uscore[i].high[0] < thisscore) {
          sbest += (thisscore - uscore[i].high[0]);
          int k;
          for (k = 1; 10*k < tscore[i].require && uscore[i].high[k] < thisscore; k++)
            uscore[i].high[k-1] = uscore[i].high[k];
          uscore[i].high[k-1] = thisscore;
          slevel=uscore[i].high[0];
        }
      }
      if(sheet==cd->sheetcnt) { /* examens */
        tscore[i].score=score;
        tscore[i].mean=stry*2+uscore[i].hint;
        tscore[i].try=stry;
        tscore[i].best=sbest;
        tscore[i].level=slevel;
        tscore[i].sh=MAX_SHEETS;
        continue;
      }
      if(score>tscore[i].require) score=tscore[i].require;
      if(stry>0) {
      snew=uscore[i].new; if(uscore[i].hint>0) snew++;
        /* here we give up to 1 time unsuccessful tries.
         * Together with a premium of 5 uncounted tries.
         */
        if(snew<stry*2+5) tt=1;
        else tt=(double) (snew-4)/(2*stry); /* tt>=1 */
        ts=(1-pow(oldfactor,stry))/(1-oldfactor);
        quality=score2/(ts*tt);
      }
      else {
        score=quality=stry=sbest=slevel=0;
      }
      tscore[i].score=score;
      tscore[i].mean=quality;
      tscore[i].try=stry;
      tscore[i].best=sbest;
      tscore[i].level=slevel;
      tscore[i].last=uscore[i].last;
      tscore[i].new=uscore[i].allnew;
      mystrncpy(tscore[i].seedlast,
        uscore[i].seed_score[uscore[i].seed_scorecnt-1].seed,SEEDSIZE);
      tscore[i].seedscorelast=uscore[i].seed_score[uscore[i].seed_scorecnt-1].score;
      p=tscore[i].seedscores;
      *p++ = '[';
      for(j=0;j<uscore[i].seed_scorecnt;j++){
        if (j) *p++=';';
        mystrncpy(p,uscore[i].seed_score[j].seed,SEEDSIZE);
        p+=strlen(p); *p++=',';
        p=moneyprint(p,uscore[i].seed_score[j].score);
      }
      *p++ = ']'; *p=0;
      tscore[i].seedlastcnt=uscore[i].seedlastcnt;
    }
  }
  answerlen=cd->exocnt*sizeof(tscore[0]);
  memmove(textbuf+3,tscore,answerlen);
  answerlen+=3;
}

void cmd_scorelog(char *p)
{
  struct classdata *cd;
  char buf[MAX_LINELEN+1];

  if(cwdtype!=dir_class) {
    sockerror(2,"scorelog_no_class"); return;
  }
  if(*opt_user==0) {
    sockerror(2,"scorelog_no_user"); return;
  }
  cd=getclasscache(opt_class);
  if(cd==NULL) {
    sockerror(2,"scorelog_bad_class"); return;
  }
  getscore(cd,opt_user);
  p=find_word_start(p); strip_trailing_spaces(p);
  snprintf(buf,sizeof(buf),"%s\n",p);
  wlogdaccessfile(buf,"a","score/%s",opt_user);
  if(myisdigit(*p)) scoreline(cd,p);
  savescorebin(cd,opt_user);
}
