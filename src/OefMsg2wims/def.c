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
#include "msg2wims.h"

int dollar_subst=1;

/* variable substitution. buffer p must have MAX_LINELEN */
void substdoc(char *p)
{
  char *pp, *pe;

  for(pp=p;pp-p<MAX_LINELEN && *pp; pp++) {
    if(*pp=='	') *pp=' ';
    if(*pp=='\n') *pp='	';
    if(*pp=='$' && dollar_subst) {
      string_modify(p,pp,pp+1,"&#36;");
      pp++; continue;
    }
    if(*pp=='!' && isalnum(*(pp+1)) && dollar_subst) {
      string_modify(p,pp,pp+1,"&#33;");
      pp++; continue;
    }
    if(*pp!='\\') continue;
    if(*(pp+1)=='\\') {
      pp++; continue;
    }
    if(!isalpha(*(pp+1))) continue;
    for(pe=pp+1;isalnum(*pe) || *pe=='_'; pe++);
    if(pe-pp<MAX_NAMELEN && *pe=='[') {
      char *pt;
      pt=find_matching(pe+1,']'); if(pt!=NULL && pt-pe<MAX_LINELEN) {
        string_modify(p,pt+1,pt+1,")");
        string_modify(p,pp,pp+1,"$(m_");
      }
      else goto nobrack;
    }
    else {
      nobrack: string_modify(p,pp,pp+1,"$m_");
    }
  }
}

void putval(char *p, char *name, int ptype)
{
  switch(ptype) {
    case pt_int: {
      fprintf(outf,"%sm_%s=$[rint(%s)]\n",setpre,name,p);
      break;
    }
    case pt_real: {
      fprintf(outf,"%sm_%s=$[%s]\n",setpre,name,p);
      break;
    }
    case pt_func: {
      fprintf(outf,"%sm_%s=!rawmath %s \n",setpre,name,p);
      break;
    }
    case pt_complex: {
      fprintf(outf,"%st_=!rawmath %s \n%st_=!exec pari print($t_)\n\
%sm_%s=!mathsubst I=i in $t_\n",
                setpre,p,setpre,setpre,name);
      break;
    }
    case pt_matrix: {
      fprintf(outf,"%stmp=!trim %s \n\
%sm_%s=!translate internal $	$ to ; in $tmp\n",setpre,p,setpre,name);
      break;
    }
    case pt_rat: {
      fprintf(outf,"%st_=!rawmath %s \n%sm_%s=!exec pari print($t_)\n",
                setpre,p,setpre,name);
      break;
    }
    default: {
      fprintf(outf,"%sm_%s=%s\n\n",setpre,name,p);
      break;
    }
  }
}

void parm(char *p[MAX_PARM], int ptype)
{
  char *pp, *p2;
  char vbuf[MAX_LINELEN+1];

  setpre="!set ";
  p[0]=find_word_start(p[0]);
  if(*p[0]=='\\') p[0]++;
/* bad name */
  if(!isalpha(*p[0])) return;
  strip_trailing_spaces(p[0]);
  for(pp=p[0];*pp;pp++) if(!isalnum(*pp) && *pp!='_') {
/* bad name and security risk */
    if(!isspace(*pp)) return;
    ovlstrcpy(pp,pp+1); pp--;
  }
  p[1]=find_word_start(p[1]);
  snprintf(vbuf,sizeof(vbuf),"%s",p[1]); substdoc(vbuf);
  fprintf(outf,"\n");
  if((pp=strparchr(vbuf,'?'))!=NULL && pp[1]!='?' && check_compare(vbuf)) {
    char buf[MAX_LINELEN+1];
    p2=strparchr(pp,':'); *pp++=0; if(p2!=NULL) *p2++=0;
    snprintf(buf,sizeof(buf),"%s",vbuf);
    prepcnt=0; fprintf(outf,"!ifval %s \n",vbuf);
    snprintf(buf,sizeof(buf),"%s",pp);
    parmprep(buf, ptype); putval(buf,p[0],ptype);
    if(p2!=NULL) {
      snprintf(buf,sizeof(buf),"%s",p2);
      fprintf(outf,"!else\n");
      parmprep(buf, ptype); putval(buf,p[0],ptype);
    }
    fprintf(outf,"!endif\n");return;
  }
  prepcnt=0; parmprep(vbuf, ptype);
  putval(vbuf,p[0],ptype);
}

void p_int(char *p[MAX_PARM]) {parm(p,pt_int);}
void p_rational(char *p[MAX_PARM]) {parm(p,pt_rat);}
void p_real(char *p[MAX_PARM]) {parm(p,pt_real);}
void p_complex(char *p[MAX_PARM]) {parm(p,pt_complex);}
void p_func(char *p[MAX_PARM]) {parm(p,pt_func);}
void p_text(char *p[MAX_PARM]) {parm(p,pt_text);}
void p_matrix(char *p[MAX_PARM]) {parm(p,pt_matrix);}

void p_parm(char *p[MAX_PARM])
{
  parm(p,pt_real);
}

struct {
    char *name;
    void (*processor) (char *p[MAX_PARM]);
} ptype[]={
  {"complex",  p_complex},
  {"function", p_func},
  {"int",  p_int},
  {"integer",  p_int},
  {"matrix",  p_matrix},
  {"parameter", p_parm},
  {"rational", p_rational},
  {"real",  p_real},
  {"text",  p_text},
  {"variable", p_parm}
};

#define ptypeno (sizeof(ptype)/sizeof(ptype[0]))

void def(char *p)
{
  char *p1,*p2,*pp[MAX_PARM];
  int i;
  p1=find_word_start(p);
  pp[1]=strchr(p1,'='); if(*pp[1]==0) return;
  *pp[1]=0; pp[1]++;
  p2=find_word_start(find_word_end(p1));
  if(*p2==0) {pp[0]=p1; p_parm(pp); return;}
  pp[0]=p2; *find_word_end(p1)=0;
  for(p2=p1; *p2; p2++) *p2=tolower(*p2);
  for(i=0;i<ptypeno;i++) if(strcmp(p1,ptype[i].name)==0) break;
  if(i<ptypeno) ptype[i].processor(pp);
  else p_parm(pp);
}

