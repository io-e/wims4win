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

#include "../Lib/libwims.h"

char *relation_type[]={
  "sametext","samecase",
  "in",  "wordof","itemof","lineof","varof","variableof"
};
#define total_relations (sizeof(relation_type)/sizeof(relation_type[0]))

/* Here we only check syntax correctness. Returns 1 if OK. */
int _check_compare(char *p, int lvl)
{
  char *p1, *p2, *pp, *pt;
  char *r1, *r2;
  int i, l, k, gotl;

/* Check global pairs of parentheses */
  p2=strip_trailing_spaces(p);
  p1=find_word_start(p);
  while(*p1=='(' && *p2==')' && p2==parend(p1)) {
    lvl=0; p1=find_word_start(++p1);
    do p2--; while(p2>=p1 && myisspace(*p2));
    p2[1]=0;
  }
  gotl=100; r1=r2=p1;
  for(pp=p1; *pp; pp++) {
    if(*pp==')') return 0;
    if(*pp=='(') {
        pp=parend(pp); if(pp==NULL) return 0;
        continue;
    }
    if(gotl>3) {
      switch(*pp) {
        case '<': {
          gotl=3; r1=pp; r2=r1+1;
          if(*r2=='=') {r2++;break;}
          if(*r2=='>') r2++;
          break;
        }
        case '>': {
          gotl=3; r1=pp; r2=r1+1;
          if(*r2=='=') r2++;
          break;
        }
        case '=': {
          gotl=3; r1=pp; r2=r1+1; if(*r2=='=') r2++;
          break;
        }
        case '!': {
          if(pp[1]=='=') {
            gotl=3; r1=pp; r2=pp+2;
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
      if(pp[0]=='i' && pp[1]=='s') {k=2; goto isnot;}
      if(pp[0]=='n' && pp[1]=='o' && pp[2]=='t') {k=3; goto isnot;}
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
      gotl=3; pp+=l; r1=pt; r2=pp;
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
  switch(gotl) {
    case 1: { /* or */
      *r1=0;
      if(!_check_compare(p1,1)) return 0;
      if(!_check_compare(r2,0)) return 0;
      return 1;
    }
    case 2: { /* and */
      *r1=0;
      if(!_check_compare(p1,2)) return 0;
      if(!_check_compare(r2,1)) return 0;
    }
    case 3: {
      return 1; /* this is considered as OK. */
    }
  }
  return 0;
}

int check_compare(char *p)
{
  char buf[MAX_LINELEN+1];
  char *pp;
  snprintf(buf,sizeof(buf),"%s",p);
  pp=strparchr(buf,'?'); if(pp) *pp=0;
  return _check_compare(buf,0);
}


