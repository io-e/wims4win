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

#include "wims.h"

char *relation_type[]={
    "sametext","samecase",
    "in",  "wordof","itemof","lineof","varof","variableof"
};
#define total_relations (sizeof(relation_type)/sizeof(relation_type[0]))

/* Compares two strings. Returns 1 if yes, 0 if no, -1 if error.
 * In fact, -1 will occur only if module_error() is modified to
 * return instead of abort.
 */
/* TODO: quoted string. */
int compare(char *p, int numeric, int lvl)
{
  char *p1, *p2, *pp, *pt;
  char *r1, *r2;
  int i, l, k, r, neg, gotl;

/* Check global pairs of parentheses */
  p2=strip_trailing_spaces(p);
  p1=find_word_start(p);
  if(lvl==0 && p2-p1>=MAX_LINELEN-1) module_error("parm_too_long");
  while(*p1=='(' && *p2==')' && p2==parend(p1)) {
    lvl=0; p1=find_word_start(++p1);
    do p2--; while(p2>=p1 && myisspace(*p2));
    p2[1]=0;
  }
  gotl=100; r1=r2=p1; r=-1; neg=0;
  for(pp=p1; *pp; pp++) {
    if(*pp==')') {badpar: module_error("unmatched_parentheses"); return -1;}
    if(*pp=='(') {
      pp=parend(pp); if(pp==NULL) goto badpar;
      continue;
    }
    if(gotl>3) {
      switch(*pp) {
        case '<': {
          gotl=3; r1=pp; r2=r1+1; r=102; neg=0;
          if(*r2=='=') {r2++; r=103; neg=1;}
          if(*r2=='>') {
            r2++; neg=1;
            if(numeric) r=101; else r=0;
          }
          break;
        }
        case '>': {
          gotl=3; r1=pp; r2=r1+1; r=103; neg=0;
          if(*r2=='=') {r2++; r=102; neg=1;}
          break;
        }
        case '=': {
          gotl=3; neg=0; r1=pp; r2=r1+1; if(*r2=='=') r2++;
          if(numeric) r=101; else r=0;
          break;
        }
        case '!': {
          if(pp[1]=='=') {
            gotl=3; r1=pp; r2=pp+2; neg=1;
            if(numeric) r=101; else r=0;
          }
          break;
        }
      }
      if(r2>p1) {
        if(lvl==2) break;
        pp=r2-1; continue;
      }
    }
    if(!myisspace(*pp)) continue;
    pp=find_word_start(pp);
    if(gotl>3) {
      if(pp[0]=='i' && pp[1]=='s') {k=2; neg=0; goto isnot;}
      if(pp[0]=='n' && pp[1]=='o' && pp[2]=='t') {k=3; neg=1; goto isnot;}
      goto rel;
      isnot:
      if(strchr("siwlv",pp[k])==NULL) goto rel;
      pt=pp; pp+=k; l=0;
      for(i=0;i<total_relations;i++) {
        l=strlen(relation_type[i]);
        if(strncmp(pp, relation_type[i], l)==0 &&
           ((!myisalnum(pp[l]) && pp[l]!='_') || pp[l]==0)) break;
        }
        if(i>=total_relations) {pp--; continue;}
        gotl=3; r=i+1; pp+=l; r1=pt; r2=pp;
        if(lvl==2) break; else {pp--; continue;}
    }
    rel:
    if(*pp!='|' && *pp!='&' && *pp!='a' && *pp!='o')
      {pp--; continue;}
    if(gotl>2 &&
        ((myisspace(pp[3]) && strncmp(pp,"and",3)==0) ||
       (myisspace(pp[2]) && strncmp(pp,"&&",2)==0))) {
      gotl=2; r1=pp; pp=r2=find_word_end(r1);
      if(lvl==1) break; else {pp--;continue;}
    }
    if(gotl>1 && myisspace(pp[2]) &&
      (strncmp(pp,"or",2)==0 || strncmp(pp,"||",2)==0)) {
      gotl=1; r1=pp; r2=pp=r1+2; break;
    }
  }
  if(gotl>20) {
    setvar(error_data_string,"relation not defined");
    module_error("comp_syntax"); return -1;
  }

  switch(gotl) {
    case 1: { /* or */
      *r1=0; i=compare(p1,numeric,1);
      if(i) return i;
      else return compare(r2,numeric,0);
    }
    case 2: { /* and */
      *r1=0; i=compare(p1,numeric,2); if(i<=0) return i;
      else return compare(r2,numeric,1);
    }
    case 3: { /* atomic comparison */
      if(r<100) { /* textual comparison */
        char buf1[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
        while(r1>p1 && myisspace(r1[-1])) r1--;
        memmove(buf1,p1,r1-p1); buf1[r1-p1]=0;
        r2=find_word_start(r2);
        while(p2>=r2 && myisspace(*p2)) p2--;
        memmove(buf2,r2,p2+1-r2); buf2[p2+1-r2]=0;
        substitute(buf1); substitute(buf2);
        switch(r) {
          case 0: { /* equal */
            if(strcmp(buf1,buf2)==0) return 1^neg; else return neg;
          }
          case 1: { /* sametext */
            deaccent(buf1); deaccent(buf2);
            if(strcasecmp(bufprep(buf1),bufprep(buf2))==0)
              return 1^neg;
            else return neg;
          }
          case 2: { /* samecase */
            if(strcmp(bufprep(buf1),bufprep(buf2))==0)
              return 1^neg;
            else return neg;
          }
          case 3: { /* in */
            if(strstr(buf2,buf1)!=NULL) return 1^neg; else return neg;
          }
          case 4: { /* wordof */
            if(wordchr(buf2,buf1)!=NULL) return 1^neg; else return neg;
          }
          case 5: { /* itemof */
            if(itemchr(buf2,buf1)!=NULL) return 1^neg; else return neg;
          }
          case 6: { /* lineof */
            if(linechr(buf2,buf1)!=NULL) return 1^neg; else return neg;
          }
          case 7:
          case 8: { /* varof */
            if(varchr(buf2,buf1)!=NULL) return 1^neg; else return neg;
          }
        }
      }
      else { /* numerical comparison */
        double d1, d2, sum, diff, prec;
        *r1=0;
        d1=evalue(p1); d2=evalue(r2);
        sum=d1+d2; if(sum<0) sum=-sum;
        diff=d1-d2; if(diff<0) diff=-diff;
        prec=evalue(getvar("wims_compare_precision"));  /* Move string name to header! */
        diff=diff*prec;
        if(prec>0 && prec<1E10) sum=sum+1/prec;
        switch(r) {
          case 101: { /* = */
            if(sum>=diff) return 1^neg; else return neg;
          }
          case 102: { /* < */
            if(d1<d2) return 1^neg; else return neg;
          }
          case 103: { /* > */
            if(d1>d2) return 1^neg; else return neg;
          }
          default: break; /* should never occur */
        }
      }
    }
  }
  internal_error("compare(): this should never happen.");
  return -1;
}
