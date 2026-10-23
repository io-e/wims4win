/*    Copyright (C) 2002-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
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

#include "mathexp.h"

int nopart=0, nonesting=0, nocomma=0, nocoord=0, _estart;

void extrout(char *pb, char *pe, int dist, int commas[], int *commacnt)
{
    int i;
    if(pe-pb<thislinelen && nopart) return;
    while(pe>pb && isspace(*(pe-1))) pe--;
    if(pe<=pb) return;
    if(regexcnt>0 && pe-pb<MAX_LINELEN) {
     char buf[MAX_LINELEN+1];
     memmove(buf,pb,pe-pb); buf[pe-pb]=0;
     if(!checkregex(buf)) return;
    }
    if(!nocoord)
      printf("%d %d %d, ",thisobjline,dist+_estart,(int)(pe-pb)+dist+_estart);
    while(*commacnt>0 && commas[*commacnt-1]>pe-pb) (*commacnt)--;
    if(*commacnt>0 && !nocomma) {
     fwrite(pb,1,commas[0],stdout); putchar(',');
     for(i=1;i<*commacnt;i++) {
         fwrite(pb+commas[i-1],1,commas[i]-commas[i-1],stdout); putchar(',');
     }
     pb+=commas[*commacnt-1];
    }
    fwrite(pb,1,pe-pb,stdout); printf("\n");
    *commacnt=0;
}

void _extract(char *p, int dist)
{
    int i,l,got,lt;
    char *p1, *p2, *p3, *p4;
    char buf[MAX_LINELEN+1];
    int commas[MAX_COMMAS];
    int commacnt;

    if(dist>0 && (nonesting || nopart)) return;
    commacnt=0;
    if(expl1<=0) got=1; else got=0;
    for(p1=find_word_start(p), p2=p1; *p2; p2=find_word_start(p3)) {
     if(*p2=='.' || isdigit(*p2)) {
         (void)strtod(p2,&p3); continue;
     }
     if(*p2=='(') {
         p3=find_matching(p2+1,')'); lt=exp_paren;
         paren: if(p3==NULL) error("Unmatched parentheses.");
         p2++; memmove(buf,p2,p3-p2); buf[p3-p2]=0;
         _extract(buf,p2-p+dist);
         if(expl2==lt) extrout(p2,p3,dist+p2-p,commas,&commacnt);
         p3++; continue;
     }
     if(*p2=='[') {
         p3=find_matching(p2+1,']'); lt=exp_matrix; goto paren;
     }
     if(*p2=='{') {
         p3=find_matching(p2+1,'}'); lt=exp_set; goto paren;
     }
     if(isalpha(*p2)) {
         for(p3=p2; *p3=='_' || isalnum(*p3); p3++);
         if(p3-p2>=16) goto notdefined;
         memmove(buf,p2,p3-p2); buf[p3-p2]=0;
         for(i=0;i<opalphano && strcmp(buf,opalpha[i].name)!=0; i++);
         if(i<opalphano) {
          l=opalpha[i].lvl; if(l>expl2) {
              if(got) extrout(p1,p2,dist+p1-p,commas,&commacnt);
              if(expl1>0) got=0;
              p1=find_word_start(p3);
          }
          if(l>=expl1 && l<=expl2) {
              got=1;
              if(l>0 && commacnt<MAX_COMMAS-2) {
               commas[commacnt++]=p2-p1;
               commas[commacnt++]=p3-p1;
              }
          }
          continue;
         }
         notdefined: p4=find_word_start(p3);
         if(*p4=='(') {
          p3=find_matching(p4+1,')');
          if(p3==NULL) error("Unmatched parentheses.");
          p4++; memmove(buf,p4,p3-p2); buf[p3-p4]=0;
          _extract(buf,p2-p+dist);
          p3++;
          if(expl2==exp_fn) extrout(p2,p3,dist+p2-p,commas,&commacnt);
         }
         else if(expl2==exp_variable) extrout(p2,p3,dist+p2-p,commas,&commacnt);
         continue;
     }
     for(i=0;i<oppunctno && strncmp(p2,oppunct[i].name,strlen(oppunct[i].name))!=0; i++);
     if(i>=oppunctno) error("Unknown operator.");
     p3=p2+strlen(oppunct[i].name); l=oppunct[i].lvl;
     if(l>expl2) {
         if(got) extrout(p1,p2,dist+p1-p,commas,&commacnt);
         if(expl1>0) got=0;
	 p1=find_word_start(p3);
     }
     if(l>=expl1 && l<=expl2) {
         got=1;
         if(l>0 && commacnt<MAX_COMMAS-2) {
          commas[commacnt++]=p2-p1;
          commas[commacnt++]=p3-p1;
         }
     }
  }
  if(got) extrout(p1,p1+strlen(p1),dist+p1-p,commas,&commacnt);
}

void req_extract(void)
{
    int i;
    char *p;

    if(objlinecnt<2) return;
    if(*reqtype==0) error("Missing extraction type.");
    for(i=0;i<exptypeno && strcmp(reqtype,exptype[i].name)!=0; i++);
    if(i>=exptypeno) error("Bad extraction type.");
    if(logdir!=0) nopart=1;
    while((p=wordchr(objline[0],"nopart"))!=NULL) {
     nopart=1; ovlstrcpy(p,p+strlen("nopart"));
    }
    while((p=wordchr(objline[0],"nonesting"))!=NULL) {
     nonesting=1; ovlstrcpy(p,p+strlen("nonesting"));
    }
    while((p=wordchr(objline[0],"nocomma"))!=NULL) {
     nocomma=1; ovlstrcpy(p,p+strlen("nocomma"));
    }
    getregex(objline[0]);
    expl1=exptype[i].lvl1; expl2=exptype[i].lvl2;
    for(i=1;i<objlinecnt;i++) {
     thisobjline=i; p=find_word_start(objline[i]);
     linelogdir=0;
     if(*p=='>') {
         if(logdir<0) continue;
         p=find_word_start(p+1); linelogdir=1;
     }
     else if(*p=='<') {
         if(logdir>0) continue;
         p=find_word_start(p+1); linelogdir=-1;
     }
     thislinelen=strlen(p); if(thislinelen<=0) continue;
     _estart=p-objline[i]; _extract(p, 0);
    }
}

void req_cut(void)
{
    nocoord=1; req_extract();
}

