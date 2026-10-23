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

/* Caches and its management */

#include "wimslogd.h"

struct classdata classdata[MAX_CLASSCACHE];

struct classcache {
  struct classdata *ptr;
} classcache[MAX_CLASSCACHE];
int classcaches;

/* remove old cache items */
void cleancache(void)
{
  int i;
  time_t now;
  struct classdata *cd;
  now=time(NULL);
  for(i=0;i<classcaches;i++) {
    cd=classcache[i].ptr;
    if(now<cd->start+CLASSCACHE_DELAY) continue;
    cd->access=0;
    memmove(classcache+i,classcache+i+1,(classcaches-i-1)*sizeof(classcache[0]));
    classcaches--;
  }
}

void class_dump(struct classdata *cd)
{
 int i, j, k, nb;
 char *s;
 exodata *ed;
 my_debug("Classe: %s\n", cd->name);
 my_debug("Superclasse: %s\n", cd->sclass);
 my_debug("Feuilles: %i, Exercices: %i\n", cd->sheetcnt, cd->exocnt);
 for (i=0;i<cd->sheetcnt;i++)
 {
   my_debug("Feuille %i: %i exercices\n",i+1,cd->sheets[i].exocnt);
   nb=cd->sheets[i].techcnt;
   if (nb==1)
     my_debug("Pas de variable technique\n");
   else
     {
       s = cd->techs+cd->sheets[i].techoffset;
       my_debug("Variable technique: %s, %i valeurs (actuel: %i)\n",s,nb,cd->sheets[i].techval);
       s += strlen(s); s++;
   	for (j = 0; j < nb;j++)
     	{
         my_debug("%s ",s);  s += strlen(s); s++;
     	}
       my_debug("\n");
     }

   ed = cd->exos + cd->sheets[i].indstart;
   for (j = 0; j < nb; j++)
   {
     for (k = 0; k < cd->sheets[i].exocnt; k++, ed++)
       my_debug("[a: %i r: %f w:%f] ", ed->active, ed->require, ed->weight);
     my_debug("\n");
   }
 }
 if (cd->examcnt>0)
 {
    ed = cd->exos + cd->exam.indstart;
    my_debug("Examens: %i exercices\n", cd->exam.exocnt);
    for (k = 0; k < cd->examcnt; k++, ed++){
      my_debug("[a: %i r: %f w:%f] ", ed->active, ed->require, ed->weight);
      my_debug("cuttime: %s",cd->ctbuf+cd->ctptr[k]);
    }
 }
 else
   my_debug("Pas d'examen\n");
 my_debug("Fin de la classe\n");
}

/* Locate the cache number of a class */
struct classdata *getclasscache(char *cl)
{
  int i,j=0,k,l,m,n, oldest, offset;
  struct stat st;
  struct classdata *cd;
  char buf[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char spc[MAX_FNAME+1];
  char *p1, *p2, *p3, *q1, *q2;
  time_t tt;
  tt=0, oldest=0;
  for(i=0;i<classcaches;i++) {
    cd=classcache[i].ptr;
    if(tt>cd->start) {tt=cd->start; oldest=i;}
    if(strcmp(cd->name,cl)==0) {
      cd->access++;
      return cd;
    }
  }
  if(classcaches>=MAX_CLASSCACHE) {
    i=oldest;cd=classcache[i].ptr;
    cd->access=0;
    memmove(classcache+i,classcache+i+1,(classcaches-i-1)*sizeof(classcache[0]));
    classcaches--;
  }
  for(i=0;i<MAX_CLASSCACHE && classdata[i].access>0; i++);
  if(i>classcaches) return NULL;
  cd=classdata+i; cd->access=1;
  classcache[classcaches++].ptr=cd;
  snprintf(cd->name,sizeof(cd->name),"%s",cl);
  cd->techs[0]=0; offset=1;
  cd->start=time(NULL); cd->exocnt=0;
  /* get superclass */
  // getdef is defined in wimslogdscore.c
  getdef (".def","class_superclass",spc);
  strcpy(cd->sclass,spc);
  /* Now get the exo data */
  wlogdaccessfile(buf,"r","sheets/.require");
  p1=strchr (buf,':');
  if (p1==NULL) {
    /*old format*/
    for(i=k=0,p1=buf; *p1; i++,p1=p2) {
      cd->sheets[i].start=cd->sheets[i].indstart=k;
      p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
      for(j=0,q1=find_word_start(p1); *q1 && k<MAX_CLASSEXOS; j++,q1=find_word_start(q2)) {
        q2=find_word_end(q1); if(*q2) *q2++=0;
        cd->exos[k].require=atof(q1);
        cd->exos[k].weight=0;
        cd->exos[k].active=1;
        k++;
      }
      cd->sheets[i].exocnt=j;
      cd->exocnt +=j;
      cd->sheets[i].techcnt=1;
   }
    cd->sheetcnt=i;
  }
  else {
  /* i: sheet, k: exo numero counted with multiplicity, l: tech version*/
    for (i=k=0,p1++; *p1; i++,p1=p2) {
      cd->sheets[i].indstart=k; /* numero of the first exo of the i sheet with multiplicity */
      cd->sheets[i].start=cd->exocnt;
      p2=strchr(p1,':'); if(p2) *p2++=0; else p2=p1+strlen(p1);
      for (l=0; *p1; l++,p1=p3) {
        p3=strchr(p1,'\n'); if(p3) *p3++=0; else p3=p1+strlen(p1);
        for(j=0,q1=find_word_start(p1); *q1 && k<MAX_CLASSEXOS; j++,q1=find_word_start(q2)) {
          q2=find_word_end(q1); if(*q2) *q2++=0;
          cd->exos[k].require=atof(q1);
          /* default values, will be changed after reading the appropriate file */
          cd->exos[k].weight=0;
          cd->exos[k].active=1;
          k++;
        }
      }
      cd->sheets[i].exocnt=j; /* number of exos in the sheet i */
      cd->exocnt+=j; /* total number of exos sans multiplicité */
      cd->sheets[i].techcnt=l; //nombre de lignes=nombre de variables techniques,
    }
    cd->sheetcnt=i; /* number of sheets */
  }
  if(k>=MAX_CLASSEXOS) return NULL;
  cd->examstart=cd->exocnt; // nb exos sans multipl. dans les feuilles précédant l'examen
  cd->examcnt=0;
  cd->exam.indstart=k;
  cd->exam.start=cd->exocnt;
  cd->exam.exocnt=0;
  cd->modif=0;
  wlogdaccessfile(buf,"r","sheets/.weight");
  p1=strchr (buf,':');
  /*old format*/
  if (p1==NULL)
    for(i=k=0,p1=buf; *p1; i++,p1=p2) {
      p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
      for(j=0,q1=find_word_start(p1); *q1 && k<MAX_CLASSEXOS; j++,q1=find_word_start(q2)) {
        q2=find_word_end(q1); if(*q2) *q2++=0;
        cd->exos[k].weight=atof(q1);
        k++;
      }
    }
  else
    for (i=k=0,p1++; *p1; i++,p1=p2) {
      p2=strchr(p1,':'); if(p2) *p2++=0; else p2=p1+strlen(p1);
      for (; *p1; p1=p3) {
        p3=strchr(p1,'\n'); if(p3) *p3++=0; else p3=p1+strlen(p1);
        for(q1=find_word_start(p1); *q1 && k<MAX_CLASSEXOS;q1=find_word_start(q2)) {
          q2=find_word_end(q1); if(*q2) *q2++=0;
          cd->exos[k].weight=atof(q1);
          k++;
        }
      }
    }
  if(stat("sheets/.vars",&st)==0) {
    wlogdaccessfile(buf,"r","sheets/.vars");
    p1=strchr (buf,':');
    if (p1!=NULL) {
      for (i=0; i < cd->sheetcnt; i++, p1=p2){
        p1++;
        cd->sheets[i].techoffset=0;
        p2=strchr(p1,'\n'); if(p2) *p2++=0; else p2=p1+strlen(p1);
        if (cd->sheets[i].techcnt == 0) continue;
        cd->sheets[i].techoffset=offset;
        for(l=0; l <= cd->sheets[i].techcnt;l++,p1=q2) {
          p1=find_word_start(p1);
          q2=find_word_end(p1); if(*q2) *q2++=0;
          strcpy(cd->techs+offset,p1);
          offset+=strlen(p1); offset++;
        }
      }
    }
    wlogdaccessfile(buf,"r","sheets/.active");
    p1=strchr (buf,':');
    if (p1!=NULL) {
      for (i=k=0,p1++; *p1; i++,p1=p2) {
        p2=strchr(p1,':'); if(p2) *p2++=0; else p2=p1+strlen(p1);
        for (; *p1; p1=p3) {
          p3=strchr(p1,'\n'); if(p3) *p3++=0; else p3=p1+strlen(p1);
          for(q1=find_word_start(p1); *q1 && k<MAX_CLASSEXOS;q1=find_word_start(q2)) {
            q2=find_word_end(q1); if(*q2) *q2++=0;
            cd->exos[k].active=strcmp(q1,"0");
            k++;
          }
        }
      }
    }
  }
  if(stat("exams/.exams",&st)==0) cd->modif=st.st_mtime; else return cd;
  wlogdaccessfile(buf,"r","exams/.exams");
  if(buf[0]==0) return cd;
  if(buf[0]==':') p1=buf-1; else p1=strstr(buf,"\n:");
  for(n=m=0,k=cd->exam.indstart; p1 && k<MAX_CLASSEXOS && m<MAX_EXAMS; p1=p2,m++,k++) {
    p1+=2;
    p2=strstr(p1,"\n:"); if(p2) *p2=0;
    //      if(*find_word_start(p1)<'1') continue;      /* status */
    fnd_line(p1,3,buf2); if(buf2[0]==0) continue;
    q1=find_word_start(buf2); q2=find_word_end(q1);
    if(*q2) *q2++=0;
    q2=find_word_start(q2); *find_word_end(q2)=0;
    i=atoi(q1); j=atoi(q2); if(i<=0 || j<=0) continue;
    cd->exos[k].weight=i; cd->exos[k].require=j;      /* weight: duration. require: retries */
    fnd_line(p1,6,buf2); q1=find_word_start(buf2);
    singlespace(q1); strip_trailing_spaces(q1);
    cd->ctptr[m]=n; cd->ctbuf[n]=0;
    if(n+strlen(q1)>CTBUFLEN-MAX_EXAMS-16) *q1=0;      /* silent truncation */
    l=strlen(q1)+1; memmove(cd->ctbuf+n,q1,l); n+=l;
  }
  cd->examcnt=m; // number of exos in all exams
  cd->exocnt+=m; // number of all exos without multiplicity
  cd->exam.exocnt=m;
  cd->sheets[cd->sheetcnt]=cd->exam;
  //class_dump(cd);
  return cd;
}
