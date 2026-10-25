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
#include "oef2wims.h"
/* these are patches for rawmath.c */
/*
char *getvar(char *p) {return NULL;}
void setvar(char *p, char *v) {return;}
*/
char *_exec_if(char *p, int type)
{
  char *p1, *p2, *p3, *p4, *p5, *p6, *pp;
  char buf[MAX_LINELEN+1];
  p1=find_word_start(p); if(*p1!='{') return p;
  p2=find_matching(p1+1,'}'); if(p2==NULL) return p;
  p3=find_word_start(p2+1); if(*p3!='{') return p;
  p4=find_matching(p3+1,'}'); if(p4==NULL) return p;
  *p2=0; snprintf(buf,sizeof(buf),"%s",p1+1);
  for(pp=strchr(buf,'\\'); pp!=NULL; pp=strchr(pp+1,'\\')) {
    if(isalnum(*(pp+1))) string_modify(buf,pp,pp+1,"$m_");
  }
  switch(type) {
    case 0: fprintf(outf,"\n!if %s \n$()",buf); break;
    case 1: fprintf(outf,"\n!ifval %s \n$()",buf);
  }
  p5=find_word_start(p4+1);
  if(*p5=='{' && (p6=find_matching(p5+1,'}'))!=NULL) {
    *p4=elsechar; *p5=' '; *p6=endifchar;
  }
  else *p4=endifchar;
  return p3+1;
}
char *exec_if(char *p) {return _exec_if(p,0);}
char *exec_ifval(char *p) {return _exec_if(p,1);}

char *exec_for(char *p)
{
  char *p1, *p2, *p3, *p4, *pp;
  char buf[MAX_LINELEN+1];
  p1=find_word_start(p); if(*p1!='{') return p;
  p2=find_matching(p1+1,'}'); if(p2==NULL) return p;
  p3=find_word_start(p2+1); if(*p3!='{') return p;
  p4=find_matching(p3+1,'}'); if(p4==NULL) return p;
  *p2=0; snprintf(buf,sizeof(buf),"%s",p1+1);
  for(pp=strchr(buf,'\\'); pp!=NULL; pp=strchr(pp+1,'\\')) {
    if(isalnum(*(pp+1))) string_modify(buf,pp,pp+1,"$m_");
  }
  fprintf(outf," \n!for m_%s \n$()",find_word_start(buf));
  *p4=nextchar;
  return p3+1;
}
/* process math formula inside \( ) or \( \) */
void process_formula(char *p)
{
  char *p3, bf[MAX_LINELEN+1];

  if(strlen(p)>=MAX_LINELEN) oef_error("formula too long");
  while((p3=strstr(p,"&lt;"))!=NULL) memmove(p3," <  ",4);
  while((p3=strstr(p,"&gt;"))!=NULL) memmove(p3," >  ",4);
  for(p3=strchr(p,'\n'); p3!=NULL; p3=strchr(p3,'\n')) *p3=' ';
  snprintf(bf,sizeof(bf),"%s",p);
  if(strchr(bf,'\\')==NULL && strchr(bf,'}')==NULL && strlen(bf)>2) {
    for(p3=strstr(bf,".."); p3!=NULL; p3=strstr(p3,"..")) {
        if(*(p3+2)=='.' || *(p3+2)==',') {
          do p3++; while(*p3=='.'); continue;
        }
        *p3=','; *(p3+1)=' ';
    }
  }
  fprintf(outf,"\n!insmath %s\n$()",bf);
}

/* deux accolades obligatoires */
int _matchit3(char **p, char **ps, char *name, int type)
{
  char *pp = find_word_start(*p+strlen(name)+1);
  if(strncmp(*p+1,name,strlen(name))==0 && *pp=='{') {
    char *pp2, *pe2, *pt;
    char *pe=find_matching(pp+1,'}');
    if(pe) pp2=find_word_start(pe+1); else return 1;
    if(pp2 && *pp2=='{') pe2=find_matching(pp2+1,'}'); else return 1;
    if(pe2) {
      pp++; pp2++; **p=*pe=*pe2=0;
      switch(type) {
     /* $val1 is $imagedir supprime s'il se trouve dans le code de draw */
          case 0: while((pt=strstr(pp2,"$val1/"))!=NULL) ovlstrcpy(pt,pt+strlen("$val1/")); break;
      }
    fprintf(outf,"%s \n\
!read oef/%s.phtml %s \\\n%s \n$()", *ps,name,pp,pp2);
    *ps=*p=pe2; (*ps)++; return 1;
    }
  }
  return 0;
}

/* deux accolades la deuxième non obligatoire */
int _matchit2(char **p, char **ps, char *name)
{
  char *pp = find_word_start(*p+strlen(name)+1);
  if(strncmp(*p+1,name,strlen(name))==0 && *pp=='{') {
    char *pp2, *pe2;
    char *pe=find_matching(pp+1,'}');
    if(pe) pp2=find_word_start(pe+1); else return 1;
    pe2=pe;
    if(*pp2=='{') {
      pe2=find_matching(++pp2,'}');
      if(pe2) *pe2=0;
    }
    else pp2="";
    pp++; **p=*pe=0;
    fprintf(outf,"%s \n\
!read oef/%s.phtml %s %s \n$()", *ps,name,pp,pp2);
    *ps=*p=pe2; (*ps)++; return 1;
  }
  return 0;
}

/* une seule accolade */
int _matchit1(char **p, char **ps, char *name)
{
  char *pp = find_word_start(*p+strlen(name)+1);
  if(strncmp(*p+1,name,strlen(name))==0 && *pp=='{') {
    char *pe=find_matching(pp+1,'}');
    if(pe) {
      pp++; **p=*pe=0;
      fprintf(outf,"%s \n\
!read oef/%s.phtml %s \n$()", *ps,name,pp);
      *ps=*p=pe; (*ps)++; embedcnt++; return 1;
    }
  }
  return 0;
}

void out_exec(char *s1, char *s2)
{
  char *ps, *p, c, *p2, *p3;
  if(s2) fprintf(outf,"\n!exit\n\n:%s\n$()",s2);
  ps=s1;
  for(p=ps;*p;p++) {
    if(*p==nextchar) {
      *p=0;
      fprintf(outf,"%s \n!next\n$()",ps); ps=p+1; continue;
    }
    if(*p==elsechar) {
      *p=0;
      fprintf(outf,"%s \n!else\n$()",ps); ps=p+1; continue;
    }
    if(*p==endifchar) {
      *p=0;
      fprintf(outf,"%s \n!endif\n$()",ps); ps=p+1; continue;
    }
    if(*p!='\\') continue;
    c=*(p+1);
    if(isalnum(c)) {
/* exit */
      if(strncmp(p+1,"exit",strlen("exit"))==0 && !isalnum(*(p+strlen("exit")+1))) {
        *p=0; fprintf(outf,"%s\n!exit\n",ps); p+=strlen("exit")+1; ps=p;
        continue;
      }
/* for */
      if(strncmp(p+1,"for",strlen("for"))==0 && *find_word_start(p+strlen("for")+1)=='{') {
        char *pt;
        *p=0; fprintf(outf,"%s",ps); p++; ps=p;
        pt=exec_for(p+strlen("for")); if(pt>p+strlen("for")) {p=pt-1;ps=pt;}
        continue;
      }
/* if */
      if(strncmp(p+1,"if",strlen("if"))==0 && *find_word_start(p+strlen("if")+1)=='{') {
        char *pt;
        *p=0; fprintf(outf,"%s",ps); p++; ps=p;
        pt=exec_if(p+2); if(pt>p+strlen("if")) {p=pt-1;ps=pt;}
        continue;
      }
/* ifval */
      if(strncmp(p+1,"ifval",strlen("ifval"))==0 && *find_word_start(p+strlen("ifval")+1)=='{') {
        char *pt;
         *p=0; fprintf(outf,"%s",ps); p++; ps=p;
        pt=exec_if(p+strlen("ifval")); if(pt>p+strlen("ifval")) {p=pt-1;ps=pt;}
        continue;
      }
/* not seen as a variable */
      if (_matchit2(&p, &ps, "img")) continue;
      if (_matchit2(&p, &ps, "audio")) continue;
      if (_matchit2(&p, &ps, "slib")) continue;
      if (_matchit1(&p, &ps, "embed")) continue;
      if (_matchit1(&p, &ps, "special")) continue;
      if (_matchit3(&p, &ps, "draw",1)) continue;
      if (_matchit3(&p, &ps, "drawtikz",1)) continue;
      if (_matchit3(&p, &ps, "canvasdraw",0)) continue;
      *p++=0; fprintf(outf,"%s$m_",ps); ps=p; continue;
    }
    if(c=='\\') {
      ovlstrcpy(p,p+1); continue;
    }
    if(c=='(') {
      p2=find_matching(p+2,')'); p3=strstr(p,"\\)");
      if((p2==NULL && p3!=NULL) ||
         (p2!=NULL && p3!=NULL && p3<p2)) p2=p3+1;
      if(p2==NULL) continue;
      *p++=0; fprintf(outf,"%s",ps);
        *p2=0; if(*(p2-1)=='\\') *(p2-1)=0;
        process_formula(p+1);
        formulaend: p=p2; ps=p+1;
        continue;
    }
    if(c=='{') {
      p2=find_matching(p+2,'}'); p3=strstr(p,"\\}");
      if((p2==NULL && p3!=NULL) ||
         (p2!=NULL && p3!=NULL && p3<p2)) p2=p3+1;
      if(p2==NULL) continue;
      *p++=0; fprintf(outf,"%s",ps);
      *p2=0; process_formula(p+1);
      goto formulaend;
    }
  }
  fprintf(outf,"%s\n$()",ps);
}
