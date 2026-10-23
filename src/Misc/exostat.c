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

/* This is an internal program,
 * used to show statistics of frequentation, module by module.
*/

/* TODO : lorsqu'on selectionne score et noscore il y a une double lecture 
   des fichiers de log (une premiere lecture pour score et une autre pour noscore
   surement qu'une seule lecture serait possible A voir */


#include "../Lib/libwims.h"
#include "../wimsdef.h"

#define MAX_SCORE      512*1024

typedef struct {
    short int dure,score;
    long int next;
} statscoredata;
statscoredata scores[MAX_SCORE];
int sccnt=0;

typedef struct {
    int newcnt,scorecnt,lasttime,firstscore,lastscore;
    char lastnew[12];
} statexodata;
statexodata shdata[MAX_SHEETS*MAX_EXOS],examdata[MAX_EXAMS*MAX_EXOS];

/* cid: combined index of difficulty */
double scsum, scavg, scdeviat, scmin, scmax, cid;
double dursum, duravg, durdeviat, durmin, durmax;
double best[10];
int filecnt=0, fcind;
char *dirbase, *sdata, user[256];

int str2time(char *p)
{
  int sec,min,hr;
  sec=atoi(p+15); p[14]=0;
  min=atoi(p+12); p[11]=0;
  hr=atoi(p+9);
  if(sec<0 || min<0 || hr<0 || sec>59 || min>59 || hr>23) return -1;
  return hr*3600+min*60+sec;
}

/*  traitement d'une ligne des fichiers score/noscore d'un utilisateur 
 les donnees des sheet sont stockees dans shdata 
  les donnees des exam sont stockees dans examdata */
static
void oneline(char *p, char *typ)
{
  int i, j, sh, ex, t;
  char *data[64];
  statexodata *tab;
  char *pp, *pe;
  char *ltype;
  for(i=0, pp=find_word_start(p); i<9  && *pp; pp=find_word_start(pe),i++) {
    pe=find_word_end(pp); if(*pe) *pe++=0;
    data[i]=pp;
  }
  if(i<6) return;
  sh=atoi(data[2]); ex=atoi(data[3]);
  if(sh<=0 || ex<=0 || ex>MAX_EXOS || strlen(data[1])>10) return;
  if(data[0][0]=='E') {
    tab=examdata; data[0]++; if(sh>MAX_EXAMS) return;
    ltype="S";
  }
  else {
    tab=shdata; if(sh>MAX_SHEETS) return;
    ltype="noscore";
  }
  tab+=(sh-1)*MAX_EXOS+(ex-1);
  t=str2time(data[0]); if(t==-1) return;
  if(strstr(data[4],"new")!=NULL) {
    if(strcmp(typ,"score")==0)
	    for (j = 6; j < i; ++j) if (strcmp(data[j],ltype)==0) return;
    if(strcmp(typ,"noscore")==0) {
            int test=0;
	    for (j = 7; j < i; ++j) if (strcmp(data[j],ltype)==0) test=1;
            if (test==0) return;
    }
    snprintf(tab->lastnew,12,"%s",data[1]);
    tab->newcnt++; tab->lasttime=t;
    fcind++;
    return;
  }
  if(strcmp(data[4],"score")==0) {
    if(strcmp(tab->lastnew,data[1])!=0) return;
    if(sccnt>=MAX_SCORE) return;
    if(tab->lasttime==-1) return;
    if(strcmp(typ,"score")==0)
	    for (j = 6; j < i; ++j) if (strcmp(data[j],ltype)==0) return;
    if(strcmp(typ,"noscore")==0) {
            int test=0;
	    for (j = 7; j < i; ++j) if (strcmp(data[j],ltype)==0) test=1;
            if (test==0) return;
    }
    t-=tab->lasttime; tab->lasttime=-1; if(t<0) t+=24*3600;
    if(t<0) t=0;
    if(t>5*3600) t=5*3600;
    scores[sccnt].dure=t; scores[sccnt].next=-1;
    scores[sccnt].score=(double) atof(data[5])*100+0.5;
    if(tab->scorecnt>0) scores[tab->lastscore].next=sccnt;
    else tab->firstscore=sccnt;
    tab->lastscore=sccnt; sccnt++; tab->scorecnt++;
  }
}


/* traitement d'un fichier de log particulier 
 typ prend deux valeurs score ou noscore */
void onefile(char *fname, char *typ)
{
  FILE *f;
  char *buf, *pp, *pe;
  long int l;
  f=fopen(fname,"r"); if(f==NULL) return;
  fseek(f,0,SEEK_END); l=ftell(f); fseek(f,0,SEEK_SET);
  if(l<=0) {fclose(f); return;}
  buf=xmalloc(l+16); (void)fread(buf,1,l,f); fclose(f); buf[l]=0;
  fcind=0;
  for(pp=buf; pp; pp=pe) {
    pe=strchr(pp,'\n'); if(pe!=NULL) *pe++=0;
    oneline(pp,typ);
  }
  free(buf);
  if(fcind>0) filecnt++;
}


/* traitement de tous les fichiers d'un repertoire 
qui est soit /score soit /noscore */

void onedir_ (char buf[MAX_LINELEN+1], char *typ)
{
  char buf2[MAX_LINELEN+1];
  DIR *dir;
  struct dirent *ff;
  dir=opendir(buf); if(dir==NULL) return;
  while((ff=readdir(dir))!=NULL) {
    /* les fichiers commençant par . sont ceux des eleves supprimes ; les fichiers avec 
    une extension .exam .bin ne sont pas des fichiers de scores 
    le decompte d'activite n'est pas fait sur supervisor */
    if(strstr(ff->d_name,".")!=NULL || strcmp(ff->d_name,"supervisor")==0) continue;
    snprintf(buf2,sizeof(buf2),"%s/%s",buf,ff->d_name);
    onefile(buf2,typ);
  }
  closedir(dir);
}

void onedir(char *dirname)
{
  char buf[MAX_LINELEN+1];
  char *t1, *t2, types[256];
  snprintf(types,sizeof(types),"%s",sdata);
  for(t1=find_word_start(types); *t1; t1=find_word_start(t2)) {
    t2=find_word_end(t1); if(*t2) *t2++=0;
    snprintf(buf,sizeof(buf),"%s/%s/score",dirbase,dirname);
    onedir_(buf,t1);
    snprintf(buf,sizeof(buf),"%s/%s/noscore",dirbase,dirname);
    onedir_(buf,t1);
  }
}

/* traitement des donnees seulement pour un utilisateur 
  appel de la fonction onefile pour chaque fichier */

void oneuser(char *dirname, char fname[64])
{
  char buf[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char *t1, *t2, types[256];
  snprintf(types,sizeof(types),"%s",sdata);
  for(t1=find_word_start(types); *t1; t1=find_word_start(t2)) {
    t2=find_word_end(t1); if(*t2) *t2++=0;
    snprintf(buf,sizeof(buf),"%s/%s/score/%s",dirbase,dirname,fname);
    onefile(buf,t1);
    snprintf(buf2,sizeof(buf),"%s/%s/noscore/%s",dirbase,dirname,fname);
    onefile(buf2,t1);
  }
}

/* traitement de plusieurs utilisateurs (appel de oneuser
  pour chacun d'entre eux */
void multiuser(char *dirname, char *user)
{
  char buf[MAX_LINELEN+1];
  char *u1, *u2;
  snprintf(buf,sizeof(buf),"%s",user);
  for(u1=find_word_start(buf); *u1; u1=find_word_start(u2)) {
    u2=find_word_end(u1); if(*u2) *u2++=0;
    oneuser(dirname, u1);
  }
}

/* fonction de generation des statistiques en utilisant les donnees
   recoltees dans les fichiers de log avec oneuser onefile etc...
   ne realise les statistiques que sur une feuille ou exam
   en meme temps */
void stati(statexodata *dat)
{
  int i,j;
  double s,d;
  scsum=scavg=scdeviat=dursum=duravg=durdeviat=cid=0;
  scmin=10; scmax=0; durmin=24*3600; durmax=0;
  {int k; for(k=0; k<10; k++) best[k]=0 ;}
  for(i=0,j=dat->firstscore; i<dat->scorecnt; i++) {
    s=(double) scores[j].score/100; d=(double) scores[j].dure/60;
    scsum+=s; dursum+=d;
    if(scmin>s) scmin=s;
    if(scmax<s) scmax=s;
    if(durmin>d) durmin=d;
    if(durmax<d) durmax=d;
    {
     int k, l = 0;
     for (k = 0; k < 10; k++) if (best[k] < best[l]) l = k;
     if (best[l] < s) best[l] = s;
    }
    j=scores[j].next;
  }
  if(i<=0) {scmin=durmin=0; return;}
  scavg=scsum/i; duravg=dursum/i;
  if(scsum>1) cid=min(4*sqrt(dursum*dat->newcnt)/scsum,99);
  else cid=0;
  if(i>=2) {
    for(i=0,j=dat->firstscore; i<dat->scorecnt; i++) {
      s=(double) scores[j].score/100; d=(double) scores[j].dure/60;
      scdeviat+=(s-scavg)*(s-scavg); durdeviat+=(d-duravg)*(d-duravg);
      j=scores[j].next;
    }
    scdeviat=sqrt(scdeviat/i); durdeviat=sqrt(durdeviat/i);
  }
}

/* Output line format:
 * type sh exo newcnt scorecnt scsum dursum scavg duravg scmin durmin scmax durmax scdeviat durdeviat cid
 */

void  outsheetexo (int i, int flag)
{
  if(shdata[i].newcnt<=0) return;
  stati(shdata+i);
  switch(flag) {
     case 0: printf(":S %2d %2d %4d %4d \
%4.0f %4.0f %5.2f %5.2f \
%5.2f %4.1f %5.2f %5.1f \
%5.2f %5.2f %4.1f\n",
             i/MAX_EXOS+1,i%MAX_EXOS+1,
             shdata[i].newcnt, shdata[i].scorecnt,
             scsum, dursum,
             scavg, duravg,
             scmin,durmin,scmax,durmax,
             scdeviat, durdeviat,
             cid); break;
     case 1: printf("%d_%d=%d,%d,\
%.0f,%.0f,%.2f,%.2f,\
%.2f,%.1f,%.2f,%.1f,\
%.2f,%.2f,%.1f;",
             i/MAX_EXOS+1,i%MAX_EXOS+1,
             shdata[i].newcnt, shdata[i].scorecnt,
             scsum, dursum,
             scavg, duravg,
             scmin,durmin,scmax,durmax,
             scdeviat, durdeviat,
             cid);
            int k; for (k=0; k<10; k++) printf("%.2f,", best[k]);
            printf("\n");
            break;
  }
}

void outexamexo (int i)
{
  if(examdata[i].newcnt<=0) return;
  stati(examdata+i);
  printf(":E %2d %2d %4d %4d \
%4.0f %4.0f %5.2f %5.2f \
%5.2f %4.1f %5.2f %5.1f \
%5.2f %5.2f %4.1f\n",
             i/MAX_EXOS+1,i%MAX_EXOS+1,
             examdata[i].newcnt, examdata[i].scorecnt,
             scsum, dursum,
             scavg, duravg,
             scmin,durmin,scmax,durmax,
             scdeviat, durdeviat,
             cid);
}


/* ecriture de la sortie */
void output(void)
{
  int i;
  for(i=0;i<MAX_SHEETS*MAX_EXOS;i++) { outsheetexo(i, 0);}
  for(i=0;i<MAX_EXAMS*MAX_EXOS;i++) { outexamexo(i);}
}

int main()
{
  char cla[MAX_LINELEN+1], user[256];
  char *c1, *c2;
  char *cdata, *udata, *sh;

  memset(shdata,0,sizeof(shdata)); memset(examdata,0,sizeof(examdata));
  dirbase=getenv("exostat_dirbase");
  if(dirbase==NULL || *dirbase==0) dirbase="../log/classes";
  sdata=getenv("exostat_types");
  if(sdata==NULL || *sdata==0) sdata="score noscore";
  cdata=getenv("exostat_classes");
  if(cdata==NULL || *cdata==0) cdata=getenv("w_wims_class");
  if(cdata==NULL || *cdata==0) return -1;
  snprintf(cla,sizeof(cla),"%s",cdata);
  udata=getenv("exostat_user");
  if(udata==NULL || *udata==0) {
    for(c1=cla; *c1; c1++) if(!isalnum(*c1) && *c1!='/') *c1=' ';
    for(c1=find_word_start(cla); *c1; c1=find_word_start(c2)) {
      c2=find_word_end(c1); if(*c2) *c2++=0;
      onedir(c1);
    }
    output();
  } else {
    snprintf(user,sizeof(user),"%s",udata);
    multiuser(cla, user);
    sh=getenv("exostat_sheet");
    if( sh==NULL || *sh==0 ) output();
    else {
      int s=atoi(sh);
      s=(s-1)*MAX_EXOS;
      int i;
      for(i=s;i < s + MAX_EXOS;i++) {
        outsheetexo(i, 1);
      }
    };
  }
  return 0;
}
