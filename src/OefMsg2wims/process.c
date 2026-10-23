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
int prepcnt;
int ex_statement=0, ex_hint=0, ex_help=0, ex_solution=0, ex_latex=0;

const size_t MAX_KEY_LEN=128;
char vbuf_statement[MAX_LINELEN+1];
char vbuf_hint[MAX_LINELEN+1];
char vbuf_help[MAX_LINELEN+1];
char vbuf_solution[MAX_LINELEN+1];
char vbuf_latex[MAX_LINELEN+1];

/* empty processor, template. */
void empty(char *p[MAX_PARM]) {}

void p_header(char *p[MAX_PARM])
{
    p[0]=find_word_start(p[0]);
    if(strlen(p[0])>MAX_KEY_LEN) p[0][MAX_KEY_LEN]=0;
    fprintf(outf,"%s=%s\n", p[1], p[0]);
}

void p_computeanswer(char *p[MAX_PARM])
{
    p[0]=find_word_start(p[0]);
    *find_word_end(p[0])=0;
    if(strcasecmp(p[0],"yes"))
      fprintf(outf,"computeanswer=no\n");
    else fprintf(outf,"computeanswer=%s\n",p[0]);
}

void p_precision(char *p[MAX_PARM])
{
    int pr;
    pr=atoi(p[0]);
    if(pr<0 || pr>100000000) return;
    fprintf(outf,"precision=%d\n",pr);
}
void p_gen2(char *name_gen, char *p[MAX_PARM])
{
    char vbuf_gen[MAX_LINELEN+1];
    if(p==NULL) return;
    snprintf(vbuf_gen,sizeof(vbuf_gen),"%s",p[0]); subst(vbuf_gen);
    singlespace(vbuf_gen);
    fprintf(outf,"%s=%s\n", name_gen, vbuf_gen);
}
void p_css(char *p[MAX_PARM]) {p_gen2("oefcss", p);}
void p_credits(char *p[MAX_PARM]){p_gen2("credits", p);}
void p_description(char *p[MAX_PARM]){p_gen2("oefdescription", p);}
void p_observation(char *p[MAX_PARM]){p_gen2("oefobservation", p);}

void p_wims(char *p[MAX_PARM])
{
    char vbuf[MAX_LINELEN+1];
    snprintf(vbuf,sizeof(vbuf),"%s",p[0]); subst(vbuf);
/* the second condition could be removed?
 * To be very careful! */
    if(strchr(vbuf,'=')==NULL || strchr(vbuf,'!')!=NULL) return;
    fprintf(outf,"%s\n",vbuf);
}

void p_mdef(char *p[MAX_PARM])
{
    char vbuf[MAX_LINELEN+1];
    if(wordchr(mdef,p[0])==NULL) return;
    snprintf(vbuf,sizeof(vbuf),"%s",p[1]); subst(vbuf);
/* the second condition could be removed?
 * To be very careful! */
    if(strchr(vbuf,'!')!=NULL) return;
    fprintf(outf,"m_%s=%s\n",p[0],vbuf);
}

void p_range(char *p[MAX_PARM])
{
    double left, right;
    char *pp;
    pp=strstr(p[0],"..");
    if(pp==NULL) return;
    *pp=0;pp+=strlen("..");
    left=atof(p[0]); right=atof(pp);
    if(left>=right-1E-50 || left<-1E50 || right>1E50) return;
    fprintf(outf,"leftrange=%f\nrightrange=%f\n",left,right);
}

void p_language(char *p[MAX_PARM])
{
    p[0]=find_word_start(p[0]);
    *find_word_end(p[0])=0;
    if(strlen(p[0])==2) fprintf(outf,"language=%s\n",p[0]);
}

void p_statement(char *p[MAX_PARM])
{
    if(ex_statement<0) return;
    if(ex_statement>0 || p==NULL) {
      out_exec(vbuf_statement,"question");
      ex_statement=-1; return;
    }
    if(p==NULL) return;
    snprintf(vbuf_statement,sizeof(vbuf_statement),"%s",p[0]);
    subst(vbuf_statement);
    if(strcmp(format,"html")!=0) {
      fprintf(outf,"question=!nosubst %s\n",vbuf_statement);
      ex_statement=-1;
    }
    else {
      fprintf(outf,"question=%s\n",executed_str);
      ex_statement=1;
    }
}
void p_latex(char *p[MAX_PARM])
{
    if(ex_latex<0) return;
    if(ex_latex>0 || p==NULL) {
      out_exec(vbuf_latex,"latex");
      ex_latex=-1; return;
    }
    snprintf(vbuf_latex,sizeof(vbuf_latex),"%s",p[0]);
    subst(vbuf_latex);
    singlespace(vbuf_latex);
    fprintf(outf,"latex=!nosubst %s\n\n", vbuf_latex);
}
void p_gen(int *ex_gen, char vbuf_gen[], char *name_gen, char *p[MAX_PARM])
{
    if(*ex_gen<0) return;
    if(*ex_gen>0 || p==NULL) {
      out_exec(vbuf_gen, name_gen);
      *ex_gen=-1; return;
    }
    snprintf(vbuf_gen,MAX_LINELEN,"%s",p[0]); subst(vbuf_gen);
    if(strchr(vbuf_gen,'\\')!=NULL) {
      fprintf(outf,"%s=%s\n", name_gen, executed_str);
      *ex_gen=1;
    }
    else {
      singlespace(vbuf_gen);
      fprintf(outf,"%s=!nosubst %s\n\n", name_gen, vbuf_gen);
    }
}

void p_hint(char *p[MAX_PARM]) {p_gen(&ex_hint, vbuf_hint, "hint", p);}
void p_help(char *p[MAX_PARM]) {p_gen(&ex_help, vbuf_help, "help", p);}
void p_solution(char *p[MAX_PARM]) {p_gen(&ex_solution, vbuf_solution, "solution", p);}

enum {typ_default, typ_num, typ_func, typ_units, typ_text,
      typ_formal,typ_matrix,typ_vector,typ_set,typ_equation,
      typ_case, typ_nocase, typ_atext, typ_wlist, typ_comp,
      typ_algexp, typ_litexp, typ_menu, typ_coord, typ_fill,
      typ_raw, typ_symtext,
      typ_java, typ_src, typ_chem
};

struct {
    char *name;
    int  type;
    char *def;
} anstype[]={
      {"algexp", typ_algexp, "algexp"},
      {"aset",  typ_set, "aset"},
      {"atext",  typ_atext, "atext"},
      {"case",  typ_case, "case"},
      {"checkbox", typ_menu, "checkbox"},
      {"chemeq",        typ_chem,       "chemeq"},
      {"chset",  typ_atext, "chset"},
      {"click",  typ_menu, "click"},
      {"clickfill", typ_fill, "clickfill"},
      {"code",  typ_src, "code"},
      {"compose", typ_comp, "compose"},
      {"coord",  typ_coord, "coord"},
      {"coordinates", typ_coord, "coord"},
      {"corresp", typ_comp, "correspond"},
      {"correspond", typ_comp, "correspond"},
      {"default", typ_default, "default"},
      {"dragfill", typ_fill, "dragfill"},
      {"equation", typ_equation, "equation"},
      {"expalg", typ_algexp, "algexp"},
      {"formal", typ_formal, "formal"},
      {"fset",  typ_set, "fset"},
      {"function", typ_func, "function"},
      {"imgcomp", typ_comp, "imgcomp"},
      {"javacurve", typ_java, "javacurve"},
      {"link",  typ_menu, "click"},
      {"litexp", typ_litexp, "litexp"},
      {"mark",  typ_menu, "mark"},
      {"matrix", typ_matrix, "matrix"},
      {"menu",  typ_menu, "menu"},
      {"nocase", typ_nocase, "nocase"},
      {"number", typ_num, "numeric"},
      {"numeric", typ_num, "numeric"},
      {"numexp", typ_algexp, "numexp"},
      {"radio",  typ_menu, "radio"},
      {"range",  typ_func, "range"},
      {"ranges", typ_func, "range"},
      {"raw",  typ_raw, "raw"},
      {"reorder", typ_comp, "reorder"},
      {"select", typ_menu, "menu"},
      {"set",  typ_set, "set"},
      {"sigunits", typ_units, "sigunits"},
      {"symtext", typ_symtext, "symtext"},
      {"text",  typ_text, "case"},
      {"textcomp", typ_comp, "textcomp"},
      {"unit",  typ_units, "units"},
      {"units",  typ_units, "units"},
      {"vector", typ_vector, "vector"},
      {"wlist",  typ_wlist, "wlist"},
      {"wordcomp", typ_comp, "textcomp"}
};

#define anstype_no (sizeof(anstype)/sizeof(anstype[0]))

void p_answer(char *p[MAX_PARM])
{
  char *pp, vbuf[MAX_LINELEN+1],nbuf[MAX_LINELEN+1];
  int i,j,k,typ;

/* look for type definition */
  typ=typ_default;
  for(i=0;i<5;i++) {
    if(p[i]==NULL || p[i][0]==0) continue;
    p[i]=find_word_start(p[i]);
    if(strncasecmp(p[i],"type",strlen("type"))==0) {
      char *tt;
      tt=find_word_start(p[i]+strlen("type"));
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        tt=find_word_start(tt+1); *find_word_end(tt)=0;
        k=search_list(anstype,anstype_no,sizeof(anstype[0]),tt);
/* unknown type is now substituted */
        if(k>=0) {
          fprintf(outf,"replytype%d=%s\n",
                      answercnt,anstype[k].def);
          typ=anstype[k].type;
        }
        else {
          snprintf(nbuf,sizeof(nbuf),"%s",tt); subst(nbuf);
          fprintf(outf,"replytype%d=%s\n\n",answercnt,nbuf);
        }
      }
      continue;
    }
    if(strncasecmp(p[i],"option",strlen("option"))==0) {
      char *tt, *tv;
      tt=p[i]+strlen("option");
      if(*tt=='s' || *tt=='S') tt++;
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(nbuf,sizeof(nbuf),"%s",tt+1); subst(nbuf);
        /* comma and semicolumn are replaced by spaces in replyoption */
        for(tv=nbuf; *tv; tv++) if(*tv==',' || *tv==';') *tv=' ';
        strip_trailing_spaces(nbuf);
        fprintf(outf,"replyoption%d=%s \n",answercnt,
              find_word_start(nbuf));
      }
      continue;
    }
    if(strncasecmp(p[i],"weight",strlen("weight"))==0) {
      char *tt;
      tt=p[i]+strlen("weight");
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(nbuf,sizeof(nbuf),"%s",tt+1); subst(nbuf);
        strip_trailing_spaces(nbuf);
        fprintf(outf,"replyweight%d=%s \n",answercnt,
              find_word_start(nbuf));
      }
      continue;
    }
  }
  p[0]=find_word_start(p[0]);
  strncpy(nbuf,p[0],MAX_LINELEN); nbuf[MAX_LINELEN]=0; subst(nbuf);
  strip_trailing_spaces(nbuf);singlespace(nbuf);
  if (strlen(nbuf)>= MAX_PROMPTLEN) oef_error("Answer prompt too long");
  nbuf[MAX_PROMPTLEN]=0;
  pp=nbuf+strlen(nbuf)-1;
  if(*pp=='=') *pp=0;
  p[1]=find_word_start(p[1]);
  if(*p[1]=='\\' && (isalnum(*(p[1]+1)) || *(p[1]+1)=='_')) {
/* check for analyzed answers */
    int i,n; char *pt;
    strncpy(vbuf,p[1]+1,MAX_LINELEN); vbuf[MAX_LINELEN]=0;
    pt=strchr(vbuf,';'); if(pt!=NULL) *pt=0;
    strip_trailing_spaces(vbuf); n=strlen(vbuf);
    if(n>=MAX_NAMELEN) goto normal;
    for(i=0;i<n && (isalnum(vbuf[i]) || vbuf[i]=='_');i++);
    if(i<n) goto normal;
    for(i=varcnt-1;i>=1 && strcmp(vbuf,param[i].name)!=0;i--);
    if(i<1) { /* unused name; the answer should be analyzed */
      char *pm;
      pm=xmalloc(MAX_NAMELEN+2);
      ovlstrcpy(pm,vbuf); param[varcnt].name=pm;
      if(pt) {
        *pt=';';
        ovlstrcpy(vbuf,pt); subst(vbuf); pt=vbuf;
      }
      else pt="";
      param[varcnt].type=pt_real;
      param[varcnt].save=1;
      fprintf(outf,"replyname%d=%s\nreplygood%d=?analyze %d%s\n",
              answercnt,nbuf,answercnt,varcnt,pt);
      condans++; answercnt++; varcnt++; return;
    }
  }
  normal:
  strncpy(vbuf,p[1],MAX_LINELEN); vbuf[MAX_LINELEN]=0;
  subst(vbuf);
  switch(typ) {
    default:
    case typ_default: {
      fprintf(outf,"replyname%d=%s\nreplygood%d=%s\n",
              answercnt,nbuf,answercnt,vbuf);
      break;
    }
    case typ_num: {
      fprintf(outf,"replyname%d=%s\nreplygood%d=$[%s]\n",
              answercnt,nbuf,answercnt,vbuf);
      break;
    }
    case typ_equation:
    case typ_func: {
      fprintf(outf,"replyname%d=%s\nreplygood%d=!rawmath %s\n",
              answercnt,nbuf,answercnt,vbuf);
      break;
    }
    case typ_units: {
      fprintf(outf,"replyname%d=%s\nreplygood%d=%s\n",
                answercnt,nbuf,answercnt,vbuf);
      break;
    }
  }
  answercnt++;
}

void p_choice(char *p[MAX_PARM])
{
  int i,j;
  char buf1[MAX_LINELEN+1],buf2[MAX_LINELEN+1],nbuf[MAX_LINELEN+1];
  for(i=0;i<5;i++) {
    if(p[i]==NULL || p[i][0]==0) continue;
    p[i]=find_word_start(p[i]);
    if(strncasecmp(p[i],"option",strlen("option"))==0) {
      char *tt, *tv;
      tt=p[i]+strlen("option");
      if(*tt=='s' || *tt=='S') tt++;
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(nbuf,sizeof(nbuf),"%s",tt+1); subst(nbuf);
        for(tv=nbuf; *tv; tv++) if(*tv==',' || *tv==';') *tv=' ';
        strip_trailing_spaces(nbuf);
        fprintf(outf,"choiceoption%d=%s \n",choicecnt,
              find_word_start(nbuf));
      }
      continue;
    }
    if(strncasecmp(p[i],"weight",strlen("weight"))==0) {
      char *tt;
      tt=p[i]+strlen("weight");
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(nbuf,sizeof(nbuf),"%s",tt+1); subst(nbuf);
        strip_trailing_spaces(nbuf);
        fprintf(outf,"choiceweight%d=%s \n",choicecnt,
              find_word_start(nbuf));
      }
      continue;
    }
  }
  p[0]=find_word_start(p[0]);
  snprintf(buf1,sizeof(buf1),"%s",p[1]); subst(buf1);
  snprintf(buf2,sizeof(buf2),"%s",p[2]); subst(buf2);
  snprintf(nbuf,sizeof(nbuf),"%s",p[0]); subst(nbuf);
  nbuf[MAX_PROMPTLEN]=0;
  fprintf(outf,"choicename%d=%s\nchoicegood%d=%s\nchoicebad%d=%s\n",
        choicecnt,nbuf,choicecnt,buf1,choicecnt,buf2);
  choicecnt++;
}

void putval(char *p, int n, int ptype)
{
  switch(ptype) {
    case pt_int: {
      fprintf(outf,"val%d=$[rint(%s)]\n",n,p);
      break;
    }
    case pt_real: {
      fprintf(outf,"val%d=$[%s]\n",n,p);
      break;
    }
    case pt_func: {
      fprintf(outf,"val%d=!rawmath %s\n",n,p);
      break;
    }
    case pt_complex: {
      fprintf(outf,"t_=!rawmath %s\nt_=!exec pari print($t_)\n\
val%d=!mathsubst I=i in $t_\n",p,n);
      break;
    }
    case pt_matrix: {
      fprintf(outf,"tmp=!trim %s\n\
val%d=!translate internal $	\\\n$ to ;; in $tmp\n",p,n);
      break;
    }
    case pt_rat: {
      fprintf(outf,"t_=!rawmath %s\n\
val%d=!exec pari print($t_)\n",p,n);
      break;
    }
    default: {
      fprintf(outf,"val%d=%s\n",n,p);
      break;
    }
  }
}

void parm(char *p[MAX_PARM], int ptype)
{
  char *pp, *p2;
  char vbuf[MAX_LINELEN+1];
  int i;

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
  for(i=1;i<varcnt && strcmp(p[0],param[i].name)!=0;i++);
  p[1]=find_word_start(p[1]);
  snprintf(vbuf,sizeof(vbuf),"%s",p[1]); subst(vbuf);
  if(deftag) repsubst(vbuf);
  if((pp=strparchr(vbuf,'?'))!=NULL && pp[1]!='?') {
    char buf[MAX_LINELEN+1];
    if(check_compare(vbuf)==0) goto noif;
    p2=strparchr(pp,':'); *pp++=0; if(p2!=NULL) *p2++=0;
    snprintf(buf,sizeof(buf),"%s",vbuf);
    prepcnt=0; parmprep(buf,ptype);
    fprintf(outf,"\n!ifval %s\n",buf);
    snprintf(buf,sizeof(buf),"%s",pp);
    parmprep(buf,ptype); putval(buf,i,ptype);
    if(p2!=NULL) {
      fprintf(outf,"!else\n");
      snprintf(buf,sizeof(buf),"%s",p2);
      parmprep(buf,ptype); putval(buf,i,ptype);
    }
    fprintf(outf,"!endif\n");
  }
  else {
noif:
    prepcnt=0; parmprep(vbuf, ptype);
    putval(vbuf,i,ptype);
  }
  if(i>=varcnt && i<MAX_PARAM) {
    param[varcnt].name=p[0];
    param[varcnt].type=ptype;
    param[varcnt].save=0;
    varcnt++;
  }
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

void _p_if(char *p[MAX_PARM], int type)
{
  char vbuf[MAX_LINELEN+1];
  snprintf(vbuf,sizeof(vbuf),"%s",p[0]); subst(vbuf);
  if(deftag) repsubst(vbuf);
  prepcnt=0; parmprep(vbuf, pt_real);
  switch(type) {
    case 0: fprintf(outf,"!if %s \n",vbuf); break;
    case 1: fprintf(outf,"!ifval %s \n",vbuf);
  }
}
void p_if(char *p[MAX_PARM]) { return _p_if(p, 0);}
void p_ifval(char *p[MAX_PARM]) { return _p_if(p, 1);}

void p_else(char *p[MAX_PARM])
{
  fprintf(outf,"!else\n");
}

void p_endif(char *p[MAX_PARM])
{
  fprintf(outf,"!endif\n");
}


void p_while(char *p[MAX_PARM])
{
  char vbuf[MAX_LINELEN+1];
  snprintf(vbuf,sizeof(vbuf),"%s",p[0]); subst(vbuf);
  if(deftag) repsubst(vbuf);
  prepcnt=0; parmprep(vbuf, pt_real);
  fprintf(outf,"!while %s \n",vbuf);
}

void p_endwhile(char *p[MAX_PARM])
{
  fprintf(outf,"!endwhile\n");
}

void p_for(char *p[MAX_PARM])
{
  char *p1, *p2, buf[256];
  char vbuf[MAX_LINELEN+1];
  int i;

  p1=find_word_start(p[0]);
  if(!isalpha(*p1)) return;
  for(p2=p1; isalnum(*p2); p2++);
  if(p2-p1>64) return;
  memmove(buf,p1,p2-p1); buf[p2-p1]=0;
  for(i=1;i<varcnt && strcmp(buf,param[i].name)!=0;i++);
  if(i>=varcnt && i<MAX_PARAM) {
    param[varcnt].name=p1;
    param[varcnt].type=pt_real;
    param[varcnt].save=0;
    varcnt++;
  }
  snprintf(vbuf,sizeof(vbuf),"%s",p2); subst(vbuf); *p2=0;
  if(deftag) repsubst(vbuf);
  prepcnt=0; parmprep(vbuf, pt_real);
  fprintf(outf,"!for val%d %s \n", i, vbuf);
}

void p_next(char *p[MAX_PARM])
{
  fprintf(outf,"!next\n");
}

void p_plot(char *p[MAX_PARM])
{
  int i, f, xr, yr;
  char *pp, *p2;
  char buf[MAX_LINELEN+1];
  f=xr=yr=-1;
  for(i=0;i<3;i++) {
    if(*p[i]==0) continue;
    if((pp=strchr(p[i],'='))==NULL) f=i;
    else {
      *pp=0; pp++;
      p2=find_word_start(p[i]);
      if(*p2=='x' || *p2=='X') xr=i;
      else if (*p2=='y' || *p2=='Y') yr=i;
      ovlstrcpy(p[i],pp);
    }
  }
/*    if(xr>=0 && (pp=strstr(p[xr],".."))!=NULL) {

    }
*/    if(f<0) return;
  ovlstrcpy(buf, p[f]);
  prepcnt=0; parmprep(buf,pt_func);
  fprintf(outf,"plot_fn=!rawmath %s\n",buf);

}

void p_condition(char *p[MAX_PARM])
{
  int i,j;
  char buf1[MAX_LINELEN+1],buf2[MAX_LINELEN+1];
  for(i=0;i<5;i++) {
    if(p[i]==NULL || p[i][0]==0) continue;
    p[i]=find_word_start(p[i]);
    if(strncasecmp(p[i],"option",strlen("option"))==0) {
      char *tt, *tv;
      tt=p[i]+strlen("option");
      if(*tt=='s' || *tt=='S') tt++;
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(buf1,sizeof(buf1),"%s",tt+1); subst(buf1);
        for(tv=buf1; *tv; tv++) if(*tv==',' || *tv==';') *tv=' ';
        strip_trailing_spaces(buf1);
        fprintf(outf,"condoption%d=%s \n",conditioncnt,
        find_word_start(buf1));
      }
      continue;
    }
    if(strncasecmp(p[i],"weight",strlen("weight"))==0) {
      char *tt;
      tt=p[i]+strlen("weight");
      tt=find_word_start(tt);
      if(*tt=='=') {
        for(j=i;j<6;j++) p[j]=p[j+1];
        i--;
        snprintf(buf1,sizeof(buf1),"%s",tt+1); subst(buf1);
        strip_trailing_spaces(buf1);
        fprintf(outf,"condweight%d=%s \n",conditioncnt,
              find_word_start(buf1));
      }
      continue;
    }
  }
  if(p[1][0]==0) {p[1]=p[0]; p[0]="";}
  snprintf(buf1,sizeof(buf1),"%s",p[0]); subst(buf1);
  snprintf(buf2,sizeof(buf2),"%s",p[1]); subst(buf2);
  prepcnt=0; parmprep(buf2, pt_real);
  repsubst(buf2);
  fprintf(outf,"\n!ifval %s\n condtest%d=1\n!else\n condtest%d=0\n!endif\n\
condname%d=%s\n", buf2,conditioncnt,conditioncnt,conditioncnt,buf1);
    conditioncnt++;
}

void p_conditions(char *p[MAX_PARM])
{
  char buf[MAX_LINELEN+1];
  snprintf(buf,sizeof(buf),"%s",p[0]); subst(buf);
  prepcnt=0; parmprep(buf, pt_real);
  repsubst(buf);
  fprintf(outf,"\ncondlist=%s\n",buf);
}

void p_feedback(char *p[MAX_PARM])
{
  char buf1[MAX_LINELEN+1],buf2[MAX_LINELEN+1];
  char *cmpstr="ifval";

  snprintf(buf1,sizeof(buf1),"%s",p[0]); subst(buf1);
  snprintf(buf2,sizeof(buf2),"%s",p[1]); subst(buf2);
  repsubst(buf1); repsubst(buf2);
  if(strstr(buf1,"$m_choice")!=NULL) cmpstr="if";
  prepcnt=0; setpre="!set "; parmprep(buf1, pt_real); setpre="";
  fprintf(outf,"!%s %s\n <div class='oef_feedbacks'>",cmpstr, buf1);
  out_exec(buf2,NULL);
  fprintf(outf,"</div>\n!endif\n");
}

/* definition of steps */
void p_steps(char *p[MAX_PARM])
{
  char vbuf[MAX_LINELEN+1];
  char *pp, *p2;

  snprintf(vbuf,sizeof(vbuf),"%s",find_word_start(p[0])); subst(vbuf);
  strip_trailing_spaces(vbuf);
  if(vbuf[0]==0) return;
  if((pp=strparchr(vbuf,'?'))!=NULL && pp[1]!='?') {
    char buf[MAX_LINELEN+1];
    if(check_compare(vbuf)==0) goto noif;
    p2=strparchr(pp,':'); *pp++=0; if(p2!=NULL) *p2++=0;
    snprintf(buf,sizeof(buf),"%s",vbuf);
    prepcnt=0; parmprep(buf,pt_text);
    fprintf(outf,"\n!ifval %s \n",buf);
    snprintf(buf,sizeof(buf),"%s",pp);
    parmprep(buf,pt_text);
    fprintf(outf,"oefsteps=%s \n",buf);
    if(p2!=NULL) {
      fprintf(outf,"!else\n");
      snprintf(buf,sizeof(buf),"%s",p2);
      parmprep(buf,pt_text);
      fprintf(outf,"oefsteps=%s \n",buf);
    }
    fprintf(outf,"!endif\n");
  }
  else {
noif:
    prepcnt=0; parmprep(vbuf, pt_text);
    fprintf(outf,"oefsteps=%s \nnextstep=!nosubst %s \n",vbuf,vbuf);
  }
  fprintf(outf,"!readproc oef/steps.proc\n");
}

/* dynamic steps */
void p_nextstep(char *p[MAX_PARM])
{
  fprintf(outf,"dynsteps=yes\n");
  p_steps(p);
}
