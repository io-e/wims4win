/*  Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

 /* This program translates oef format exercises into wims internal data format. */

#define MAX_TITLEN 80

#define MAX_PARM 10
#define MAX_PARAM 1024
#define MAX_DEFINE 1024
#define MAX_FLEN 199999

#define VERSION "3.22"


#include "../Lib/libwims.h"
#include "oef2wims.h"

FILE *outf;
int choicecnt=0, answercnt=0, feedbackcnt=0, varcnt=0, conditioncnt=0, condans=0, has_plot=0;
char *primitive_dir="oef";
int has_help=0;
int posttag=0, prevarcnt=-1, post=0, deftag, prevars;
int start_varcnt=0;
int step_defined=0;
int embedcnt=0;
char *badpar=NULL;
char *mdef; /* external definition */
char *format="html";

int ao[256];
int aocnt=0;

struct param param[MAX_PARAM+32];

char *inpbuf, *inpend;
char outfname[1024];

enum {t_tit, t_head, t_head2, t_lang, t_def, t_wims, t_form, t_main, t_plot, t_hint,
      t_ans, t_choi, t_cond, t_cond2, t_sol, t_feedback, t_help, t_step, t_latex};

struct {
  char *name;
  int type, parmcnt;
  void (*processor)(char *p[MAX_PARM]);
} directives[]={
  {"answer", t_ans, 5, p_answer},
  {"author", t_head2, 1, p_header},
  {"choice", t_choi, 5, p_choice},
  {"complex", t_def, 2, p_complex},
  {"computeanswer", t_head, 1, p_computeanswer},
  {"condition", t_cond, 4, p_condition},
  {"conditions", t_cond2, 1, p_conditions},
  {"credits", t_def, 2, p_credits},
  {"css", t_head, 1, p_css},
  {"description", t_head, 1, p_description},
  {"else", t_def, 0, p_else},
  {"email", t_head2, 1, p_header},
  {"endif", t_def, 0, p_endif},
  {"endwhile", t_def, 0, p_endwhile},
  {"feedback", t_feedback, 2, p_feedback},
  {"for", t_def, 1, p_for},
  {"format", t_form, 1, empty},
  {"function", t_def, 2, p_func},
  {"help", t_help, 1, p_help},
  {"hint", t_hint, 1, p_hint},
  {"if", t_def, 1, p_if},
  {"ifval", t_def, 1, p_ifval},
  {"int", t_def, 2, p_int},
  {"integer", t_def, 2, p_int},
  {"keywords", t_head2, 1, p_header},
  {"language", t_head, 1, p_language},
  {"latex", t_latex, 1, p_latex},
  {"matrix", t_def, 2, p_matrix},
  {"mdef", t_def, 2, p_mdef},
  {"next", t_def, 0, p_next},
  {"nextstep", t_step, 1, p_nextstep},
  {"observation", t_head, 1, p_observation},
  {"parameter", t_def, 2, p_parm},
  {"plot", t_plot, 3, p_plot},
  {"precision", t_head, 1, p_precision},
  {"range", t_head, 1, p_range},
  {"rational", t_def, 2, p_rational},
  {"real", t_def, 2, p_real},
  {"reply", t_ans, 5, p_answer},
  {"solution", t_sol, 1, p_solution},
  {"statement", t_main, 1, p_statement},
  {"steps", t_step, 1, p_steps},
  {"text", t_def, 2, p_text},
  {"title", t_tit, 1, empty},
  {"title_ca", t_lang, 1, p_header},
  {"title_cn", t_lang, 1, p_header},
  {"title_en", t_lang, 1, p_header},
  {"title_es", t_lang, 1, p_header},
  {"title_fr", t_lang, 1, p_header},
  {"title_it", t_lang, 1, p_header},
  {"title_nl", t_lang, 1, p_header},
  {"title_si", t_lang, 1, p_header},
  {"variable", t_def, 2, p_parm},
  {"while", t_def, 1, p_while},
  {"wims", t_wims, 1, p_wims}
};

#define dir_no (sizeof(directives)/sizeof(directives[0]))

struct {
  char *parm[MAX_PARM];
  short int type, tag;
} define[MAX_DEFINE];
int define_no, title_no=-1, statement_no=-1;

/* Debug output */
void oef_debug(void)
{
  int i,j,n;
  for(i=0;i<define_no;i++) {
    printf("%s:	",directives[define[i].type].name);
    n=directives[define[i].type].parmcnt;
    for(j=0;j<n;j++) {
      printf("%s",define[i].parm[j]);
      if(j<n-1) printf(", ");
    }
    printf("\n");
  }
}

/* get one input file */
long int getinp(char fname[])
{
  FILE *input;
  long int siz;
  int i;

  input=fopen(fname,"r");
  if(input==NULL) return 0;
  fseek(input,0L,SEEK_END); siz=ftell(input);
  if(siz<=0 || siz>=MAX_FLEN) {
    fclose(input);
    if(siz==0) return 0; else return -1;
  }
  fseek(input,0L,SEEK_SET);
  inpbuf=xmalloc(siz+10);
  siz=fread(inpbuf,1,siz,input); fclose(input);
  if(siz<=0) return -1;
  inpend=inpbuf+siz; *inpend=0;
  for(i=0;i<siz;i++)
    if(inpbuf[i]==13 || (inpbuf[i]>=0 && inpbuf[i]<=6)) inpbuf[i]=' ';
  return siz;
}

/* Processes input file */
void process(void)
{
  char *p, *pe, *pp;
  int i,j;

  for(p=inpbuf,define_no=0;define_no<MAX_DEFINE && p<inpend;p++) {
    if(*p!='\\' && *p!='{' && (*p>6 || *p<0)) continue;
    if(*p>0 && *p<=6) {
      pe="";
      switch(*p) {
        case elsechar: pe="else"; break;
        case endifchar: pe="endif"; break;
        case nextchar: pe="next"; break;
        case whilechar: pe="endwhile"; break;
      }
      define[define_no].tag=posttag;
      define[define_no++].type=search_list(directives,dir_no,sizeof(directives[0]),pe);
      continue;
    }
    if(*p=='{') {
      if((p=find_matching(p+1,'}'))==NULL) oef_error("Unmatched parentheses???");
      continue;
    }
    p++;
    for(pe=p;isalnum(*pe) || *pe=='_'; pe++);
    pp=find_word_start(pe);
    if(*pp!='{') {
      if(pp>p) p=pp-1;
        continue;
    }
/*c=*pe; */*pe=0;
    i=search_list(directives,dir_no,sizeof(directives[0]),p);
    if(i<0) {
      if(wordchr(mdef,p)!=NULL) {
        pe=find_matching(pp+1,'}'); if(pe==NULL) oef_error("Unmatched parentheses?");
        *pe=0;define[define_no].type=
              search_list(directives,dir_no,sizeof(directives[0]),"mdef");
        replace_newline(pp+1);
        define[define_no].tag=posttag;
        define[define_no].parm[0]=p;
        define[define_no].parm[1]=pp+1;
        define_no++; p=pe;
        continue;
      }
      if((p=find_matching(pp+1,'}'))==NULL) oef_error("Unmatched parentheses?");
        continue;
    }
    define[define_no].type=i;
    define[define_no].tag=posttag;
    for(j=0;j<MAX_PARM && j<directives[i].parmcnt;j++,pp=find_word_start(pe+1)) {
      if(j>0 && *pp!='{') break;
      if((pe=find_matching(pp+1,'}'))==NULL) oef_error("Unmatched parentheses?");
      *pe=0; replace_newline(pp+1);
      define[define_no].parm[j]=pp+1;
    }
    for(;j<MAX_PARM;j++) define[define_no].parm[j]="";
    switch(directives[i].type) {
      case t_tit: {
        title_no=define_no; break;
      }
      case t_head2: case t_lang: {
        define[define_no].parm[1]=directives[i].name;
        break;
      }
      case t_ans: {
        if(aocnt<256) ao[aocnt++]=t_ans;
        answercnt++; goto checkeq;
      }
      case t_step: {
        if(step_defined) oef_error("Multiple definition of steps.");
        step_defined=1; break;
      }
      case t_def: {
        char *pt;
        if(directives[i].parmcnt<2) {
          char *pt2, *pt3;
          pt=find_word_start(pe+1); pt3="";
          if(*pt!='{') pt2=pt;
          else {
            *pt=' '; pt2=find_matching(pt,'}');
            if(pt2==NULL) oef_error("Unmatched parentheses?");
            pt3=find_word_start(pt2+1);
          }
          if(strcmp(directives[i].name,"if")==0 || strcmp(directives[i].name,"ifval")==0) {
            if(*pt3=='{') {
              *pt2=elsechar; *pt3=' ';
              pt2=find_matching(pt3,'}');
              if(pt2==NULL) oef_error("Unmatched parentheses?");
            }
            *pt2=endifchar;
          }
          else {
            if(strcmp(directives[i].name,"while")==0) *pt2=whilechar;
            else {
              varcnt++; *pt2=nextchar;
            }
          }
          break;
        }
        varcnt++;
        checkeq:
        if(*(define[define_no].parm[1])==0 &&
           (pt=strchr(define[define_no].parm[0],'='))!=NULL) {
          if(*(pt-1)==':') *(pt-1)=0;
          *pt=0; define[define_no].parm[1]=pt+1;
        }
        break;
      }
      case t_choi: {
        if(aocnt<256) ao[aocnt++]=t_choi;
        choicecnt++; break;
      }
      case t_cond: {
        conditioncnt++; break;
      }
      case t_main: {
        if(posttag) oef_error("Multiple definition of statement.");
        posttag=1; if(prevarcnt<0) prevarcnt=varcnt;
        statement_no=define_no; break;
      }
      case t_plot: {
        has_plot=1; break;
      }
      case t_form: {
        char *s=define[define_no].parm[0];
        s=find_word_start(s);*find_word_end(s)=0;
        if(strcasecmp(s,"html")==0) format="html";
        if(strcasecmp(s,"tex")==0) format="tex";
        break;
      }
      case t_feedback: {
        feedbackcnt++; break;
      }
    }
    define_no++; p=pe;
  }
}

/* Output one category */
void _out(int typ)
{
  int i, t;
  for(i=0;i<define_no;i++) {
    t=define[i].type;
    if(directives[t].type!=typ) continue;
    if(typ==t_def && define[i].tag!=deftag) continue;
    directives[t].processor(define[i].parm);
  }
}

/* Output the result */
void output(void)
{
  int i,k;
/* no statement, nothing to do */
  if(statement_no<0) oef_error("No statement defined.");
  outf=fopen(outfname,"w"); if(outf==NULL) oef_error("Unable to open output file.");
  if(title_no>=0 && *(define[title_no].parm[0])!=0) {
    char *p=define[title_no].parm[0];
    if(strlen(p)>MAX_TITLEN) *(p+MAX_TITLEN)=0;
    fprintf(outf,"!set title=%s\n",p);
  }
  else {
    fprintf(outf,"!set title=No title\n");
  }
  fprintf(outf,"!if $wims_read_parm!=$empty\n\
 !goto $wims_read_parm\n\
!endif\n");
  fprintf(outf,"oef2wims_version=%s\n",VERSION);
  _out(t_head2);
  _out(t_head);
  _out(t_lang);
  if(aocnt>0) {
    int i;
    fprintf(outf,"\nansorder=");
    for(i=0;i<aocnt;i++) {
      if(ao[i]==t_ans) fprintf(outf,"r");
      else fprintf(outf,"c");
      if(i<aocnt-1) fprintf(outf,",");
    }
  }
  if(prevarcnt<varcnt) post=varcnt-prevarcnt; else post=0;
  fprintf(outf,"\n\
varcnt=%d\n\
prevarcnt=%d\n\
postvarcnt=%d\n\
replycnt=%d\n\
choicecnt=%d\n\
conditioncnt=%d\n\
feedbackcnt=%d\n\
format=%s\n\n\
val1=$imagedir\n\
val2=$confparm1\n\
val3=$confparm2\n\
val4=$confparm3\n\
val5=$confparm4\n\n\
!if $testcondition=yes\n\
 !goto test\n\
!endif\n\
!if $status=waiting\n\
 !exit\n\
!endif\n\n",varcnt+1,prevarcnt,post,answercnt,choicecnt,
          conditioncnt,feedbackcnt,format);
  varcnt=start_varcnt; deftag=0;
  _out(t_def);
  _out(t_step);
/*    _out(t_wims); */
  _out(t_plot);
  answercnt=1; _out(t_ans);
  choicecnt=1; _out(t_choi);
  deftag=1; prevars=varcnt;
  _out(t_main);
  _out(t_hint);
  _out(t_help);
  _out(t_sol);
  _out(t_latex);
  fprintf(outf,"\n!goto stat\n");
/* second run to output execution codes */
  p_statement(NULL);
  p_hint(NULL);
  p_help(NULL);
  p_solution(NULL);
  p_latex(NULL);
  if(post) {
    fprintf(outf,"\n!exit\n\n:postdef\n"); _out(t_def);
  }
  fprintf(outf,"\n!exit\n\n:feedback\n");
  _out(t_feedback);
  fprintf(outf,"\n!exit\n\n:test\n");
  _out(t_cond2); conditioncnt=1; _out(t_cond);
  fprintf(outf,"\n!exit\n\n:stat\nvsavelist=");
  for(k=0,i=1;i<prevars;i++) {
    if(param[i].save==0) continue;
    if(k>0) fprintf(outf,",");
    k++;
    fprintf(outf,"%d",i);
  }
  fprintf(outf,"\nembedcnt=%d\n",embedcnt);
  fclose(outf);
}

int main(int argc, char *argv[])
{
  int t;
  sp_error=oef_error;
  substitute=substit;
  if(argc<=1) return 0; /* no input file */
  if(argc==2 && strcmp(argv[1],"table")==0) {
    if(verify_order(directives, dir_no, sizeof(directives[0]))) return -1;
    if(verify_order(specialfn, specialfn_no, sizeof(specialfn[0]))) return -1;
    puts("Table orders OK."); return 0;
  }
  if(argc>2) snprintf(outfname,sizeof(outfname),"%s",argv[2]);
  else {
    char *fe;
    snprintf(outfname,sizeof(outfname)-10,"%s",argv[1]);
    fe=outfname+strlen(outfname)-strlen(".oef");
    if(strcasecmp(fe,".oef")==0) *fe=0;
    strcat(fe,".def");
  }
  mdef=getenv("oef2wims_mdef"); if(mdef==NULL) mdef="";
  printf("%s..",argv[1]);
  t=getinp(argv[1]);
  if(t<0) oef_error("Source file bad or too long.");
  if(t==0) oef_error("Empty source file.");
  if(checkparentheses(inpbuf,1)!=0) oef_error("Unmatched parentheses");
  outf=fopen(outfname,"w"); if(outf==NULL) oef_error("Unable to open output file.");
  fclose(outf); remove(outfname);
  vbuf_statement[0]=vbuf_hint[0]=vbuf_help[0]=vbuf_solution[0]=vbuf_latex[0]=0;
  param[1].name="imagedir";param[1].type=pt_text;
  param[2].name="confparm1";param[2].type=pt_text;
  param[3].name="confparm2";param[3].type=pt_text;
  param[4].name="confparm3";param[4].type=pt_text;
  param[5].name="confparm4";param[5].type=pt_text;
  start_varcnt=6;
  process();
  output();
  printf(" -> %s\n",outfname);
  return 0;
}
