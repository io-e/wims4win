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

/* this is part of wims server source.
 * This file does execution commands.
 */
#include "wims.h"

static void _skip_contents(int isif);

/* common routine for the two if's. */
static void _exec_if_while(char *p, int numerical, int isif)
{
  if(compare(p,numerical,0)==0) _skip_contents(isif); /* skip if false */
  else if(!isif) m_file.for_idx++;
  return;
}

/* 'if' non-numerical (unless comparisons are < or >, etc.) */
void exec_if(char *p)
{
  _exec_if_while(p,0,1);
}

/* 'if' numerical. */
void exec_ifval(char *p)
{
  _exec_if_while(p,1,1);
}

void _exec_while(char *p, int numerical)
{
  FOR_STACK *stk;
  if(m_file.for_idx>=MAX_FOR_LEVEL) module_error("too_many_fors");
  stk=&(m_file.for_stack[m_file.for_idx]);
  stk->lineno=m_file.l;
  memmove(stk->varname,"?",2);
  _exec_if_while(p,numerical,0);
}

/* 'while' non-numerical (unless comparisons are < or >, etc.) */
void exec_while(char *p)
{
  _exec_while(p,0);
}

/* 'while' numerical. */
void exec_whileval(char *p)
{
  _exec_while(p,1);
}

void exec_endwhile(char *p)
{
  FOR_STACK *stk;
  if(m_file.for_idx<=0) module_error("next_without_for");
  stk=&(m_file.for_stack[m_file.for_idx-1]);
  if(stk->varname[0]!='?') module_error("next_without_for");
  m_file.for_idx--; *p=0;
  m_file.linepointer=stk->lineno;
  executed_gotos++;
  if(executed_gotos>=GOTO_LIMIT) module_error("too_many_gotos");
}

/* Should provide a method to stop infinite loop. */
void exec_goto(char *p)
{
  char lbuf[MAX_NAMELEN+17];
  char *label_start, *line_start;
  int old_line;
  int i;
  executed_gotos++;
  if(executed_gotos>=GOTO_LIMIT) module_error("too_many_gotos");
  old_line=m_file.l;
  label_start=find_word_start(p);
  *find_word_end(label_start)=0;
  m_file.l=m_file.linepointer=0;
  for(i=0;i<m_file.linecnt;i++) {
    if(((m_file.lines[i].isstart)&4)==0) continue;
    line_start=find_word_start((m_file.lines[i]).address);
    if(line_start>(m_file.lines[i+1]).address && i<m_file.linecnt)
      continue;
    m_file.linepointer=i;
    wgetline(lbuf,MAX_NAMELEN+8,&m_file);
    line_start=find_word_start(lbuf);
    if(*line_start!=label_prefix_char) continue;
    line_start++;*find_word_end(line_start)=0;
    if(line_start[0]!='*' && strcmp(line_start,label_start)!=0) continue;
    *p=0; i++; return;
  }
  m_file.l=old_line;
  setvar(error_data_string,label_start); module_error("label_not_found");
/*    m_file.linepointer=m_file.linecnt; *p=0; return;
*/
}

/* 'else' with or without 'if'.
 * Philosophy: we implement a loosely checked grammar.
 * We cannot check 'if' grammar if we want to use 'goto'
 * together with 'if'.
 */
void exec_else(char *p)
{
  _skip_contents(1); return;
}

/* 'endif': nothing needs to be done here. */
void exec_endif(char *p)
{
  return;
}

/* find out the end of a for loop */
void goto_for_end(void)
{
  int inner;
  char buf[16];
  inner=0;
  while(m_file.linepointer<m_file.linecnt) {
    if((m_file.lines[m_file.linepointer].isstart&2)==0) {
      m_file.linepointer++; continue;
    }
    if(wgetline(buf,8,&m_file)==EOF) break;
    *find_word_end(buf)=0;
    if(strcmp(buf,"!for")==0) {
      inner++; continue;
    }
    if(strcmp(buf,"!next")==0) {
      if(inner>0) {
        inner--; continue;
      }
      else break;
    }
  }
}

/* for */
void exec_for(char *p)
{
  char *p1, *p2, *p3;
  double v1, v2, step;
  char buf[MAX_LINELEN+1];
  FOR_STACK *stk;

  if(m_file.for_idx>=MAX_FOR_LEVEL) module_error("too_many_fors");
  stk=&(m_file.for_stack[m_file.for_idx]);
  stk->lineno=m_file.l;
  p1=find_word_start(p);
  for(p2=p1; !isspace(*p2) && *p2!=0 && *p2!='=' ; p2++);
  if(*p2==0) syntax: module_error("for_syntax");
  if(p2-p1>MAX_NAMELEN) module_error("name_too_long");
  memmove(stk->varname,p1,p2-p1);
  stk->varname[p2-p1]=0;
  p1=find_word_start(p2); if(*p1==0) goto syntax;
  if(*p1=='=') {
    p1++;
    assign:
    stk->from=0;
    p2=wordchr(p1,"to");
    if(p2==NULL) p2=strstr(p1,"..");
    if(p2==NULL) goto syntax;
    *p2=0; p2+=2;
    p3=wordchr(p2,"step");
    if(p3!=NULL) {
      *p3=0; p3+=strlen("step");
      step=evalue(p3);
      if(step==0) module_error("zero_step");
    }
    else step=1;
      v1=evalue(p1); v2=evalue(p2);
      float2str(v1,buf); setvar(stk->varname, buf);
      if((step>0 && v1>v2) || (step<0 && v1<v2) ) {  /* condition not met */
  loop_out:
        goto_for_end();
      }
    else {
      stk->varval=v1; stk->varend=v2; stk->step=step;
      stk->lineno=m_file.linepointer;
      m_file.for_idx++;
    }
    *p=0; return;
  }
  if(memcmp(p1,"from",strlen("from"))==0 && isspace(*(p1+strlen("from")))) {
    p1+=strlen("from"); goto assign;
  }
  if(memcmp(p1,"in",strlen("in"))==0 && isspace(*(p1+strlen("in")))) {
    stk->from=1; p1+=strlen("in");
    p1=find_word_start(p1); if(*p1==0) goto loop_out;
    p2=xmalloc(MAX_LINELEN+1);
    mystrncpy(p2,p1,MAX_LINELEN);
    substit(p2);
    strip_trailing_spaces(p2);
    if(*p2==0) goto loop_out;
    p1=strparchr(p2,','); stk->bufpt=p2;
    if(p1!=NULL) {
      *p1=0; stk->list_pt=find_word_start(p1+1);
    }
    else stk->list_pt=NULL;
    strip_trailing_spaces(p2); setvar(stk->varname,p2);
    stk->lineno=m_file.linepointer;
    m_file.for_idx++; *p=0; return;
  }
  goto syntax;
}

/* break a for loop */
void exec_break(char *p)
{
  FOR_STACK *stk;
  if(m_file.for_idx>0) {
    stk=&(m_file.for_stack[m_file.for_idx-1]);
    if(stk->varname[0]=='?') _skip_contents(0);
    else goto_for_end();
    m_file.for_idx--;
  }
  *p=0; return;
}

/* next */
void exec_next(char *p)
{
  double v1, v2, step;
  char *p1, *p2;
  char buf[MAX_LINELEN+1];
  FOR_STACK *stk;
  if(m_file.for_idx<=0) module_error("next_without_for");
  stk=&(m_file.for_stack[m_file.for_idx-1]);
  if(stk->varname[0]=='?') module_error("next_without_for");
  if(stk->from) { /* list style */
    if(stk->list_pt==NULL) {
      free(stk->bufpt);
      m_file.for_idx--;
      *p=0; return;
    }
    p1=strparchr(stk->list_pt,',');
    if(p1!=NULL) {
      *p1=0; p2=find_word_start(p1+1);
    }
    else p2=NULL;
    strip_trailing_spaces(stk->list_pt);
    setvar(stk->varname,stk->list_pt);
    stk->list_pt=p2; goto loop_back;
  }
  v1=stk->varval; v2=stk->varend; step=stk->step;
  v1+=step; stk->varval=v1;
  float2str(v1,buf);
  setvar(stk->varname, buf);
  if((step>0 && v1<=v2) || (step<0 && v1>=v2)) { /* loop */
    loop_back:
    m_file.linepointer=stk->lineno;
    executed_gotos++;
    if(executed_gotos>=GOTO_LIMIT) module_error("too_many_gotos");
  }
  else m_file.for_idx--;
  *p=0; return;
}

/* Execution of a file in the module directory,
 * only for trusted modules.
 */
void exec_mexec(char *p)
{
  direct_exec=1;
  calc_mexec(p);
  direct_exec=0;
}

/* call shell. */
void _exec_ex(char *p, char *arg0, char *arg1, int n)
{
  char *abuf[8];
  char errorfname[MAX_FNAME+1];

  if(robot_access) {
    *p=0; return;
  }
  if(!noout || !trusted_module() || is_class_module) {
    _calc_exec(p,arg0,arg1,n); if(outputing) output0(p);
    return;
  }
  mkfname(errorfname,"%s/exec.err",tmp_dir);
  abuf[0]=arg0; abuf[1]=arg1; abuf[n]=p; abuf[n+1]=NULL;
  wrapexec=1; exportall();
  execredirected(abuf[0],NULL,NULL,errorfname,abuf);
}

/* call shell. */
void exec_sh(char *p)
{
  _exec_ex(p,"sh","-c",2);
}

/* call perl. */
void exec_perl(char *p)
{
  _exec_ex(p,"perl","-e",2);
}

/* file should not be cached */
void exec_nocache(char *p)
{    m_file.nocache|=1; *p=0; }

/*
 * Read another file.
 * Read a file in order to perform tests.
 * Relevant global variables:
 * - m_file, a WORKING_FILE structure (should be restored to its original
 *   value at the end of the procedure)
 * - untrust, an integer (should be restored to its original
 *   value at the end of the procedure)
 * - outputing, an integer; its value is juste read.
 * - readnest, an integer, which is incremented during the function
 *   and decremented when it finishes.
 * Relevant global variables used in subprograms:
 * - module_prefix, a const char * which will be preprended before
 *   the filename, (see lines.c, label = openit).
 * Relevant wims_environment parameters:
 * - wims_read_parm  (should be restored to its original
 *   value at the end of the procedure)
 * - wims_trustfile: this parameter is only read.
 * Important procedures called by this one:
 * - phtml_put(char*, int)
 * - var_proc(char*, int)
 * - module_error(const char*)
 * @param p pointer to the filename, possibly followed by parameters
 */

void exec_read(char *p)
{
  WORKING_FILE save;
  char parmsave[MAX_LINELEN+1];
  char *pp,*ps;
  int t,cache;

  strip_trailing_spaces(p);p=find_word_start(p);
  if(*p==0) return;
  pp=find_word_end(p); if(*pp) *pp++=0;
  pp=find_word_start(pp);
  ps=getvar("wims_read_parm"); if(ps==NULL) ps="";
  mystrncpy(parmsave,ps,sizeof(parmsave));
  setvar("wims_read_parm",pp);
  memmove(&save,&m_file,sizeof(WORKING_FILE));
  cache=1; if(p[0]=='.' && p[1]=='/') {p+=2; cache=0;}
  t=untrust;
  if(strncmp(p,"wimshome/",9)==0) {
    if((untrust&255)!=0 &&
        (m_file.name[0]=='/' || strncmp(m_file.name,"wimshome/",9)==0))
      module_error("Illegal_file_access");
    untrust|=0x200;
  }
  if((untrust&0x202)!=0) {
    pp=getvar("wims_trustfile");
    if(pp!=NULL && *pp!=0 && wordchr(pp,p)!=NULL)
      untrust&=0xfdfd;
  }
  readnest++; if(readnest>MAX_READNEST) module_error("too_many_nested_read");
  if(outputing) phtml_put(p,cache); else var_proc(p,cache);
  readnest--; untrust=t;
  memmove(&m_file,&save,sizeof(WORKING_FILE));
  if (trace_file){
    int i;
    putc('\n',trace_file);
    for(i=1; i<=2*trace_indent-2; i++) putc(' ',trace_file);
    fprintf(trace_file,"%s:",m_file.filepath); fflush(trace_file);
  }
  setvar("wims_read_parm",parmsave);
}

/* read a variable processing file */
void exec_readproc(char *p)
{
  int o=outputing; outputing=0; exec_read(p); outputing=o;
}

void exec_defread(char *p)
{
  int t,o;
  secure_exec();
  t=untrust; untrust|=0x400;
  o=outputing; outputing=0;
  exec_read(p); untrust=t; outputing=o;
}

/* Change to another file (no return) */
void exec_changeto(char *p)
{
  m_file.linepointer=m_file.linecnt;
  exec_read(p);
}

int header_executed=0, tail_executed=0;

/* internal routine: get other language versions */
void other_langs(void)
{
  int i,j,k;
  char *p, lbuf[4], pbuf[MAX_FNAME+1], *phtml;
  char namebuf[MAX_FNAME+1], listbuf[4*MAX_LANGUAGES];
  static int otherlangs_got=0;

  if(otherlangs_got) return;
  mystrncpy(namebuf,module_prefix,sizeof(namebuf));
  listbuf[0]=0;j=strlen(namebuf)-3; p=listbuf;
  if(j>=2 && strcmp("light",namebuf+j-2)==0) {
    setvar("wims_light_module","yes");
    phtml=getvar("phtml");
    if(phtml==NULL || *phtml==0) return;
    mystrncpy(pbuf,phtml,sizeof(pbuf));
    j=strlen(pbuf)-3;
    if(pbuf[j]!='.') return;
    j++; ovlstrcpy(lbuf,pbuf+j); pbuf[j]=0;
    j=strlen(namebuf);
    snprintf(namebuf+j,MAX_FNAME-j-10,"/pages/%s",pbuf);
    j=strlen(namebuf);
    for(i=k=0;i<available_lang_no;i++) {
      if(strcmp(lbuf,available_lang[i])==0) continue;
      mystrncpy(namebuf+j,available_lang[i],MAX_FNAME-j);
      if(ftest(namebuf)<0) continue;
      if(k>0) *p++=',';
      mystrncpy(p,available_lang[i],sizeof(listbuf)-4-(p-listbuf));
      p+=strlen(p);
      k++;
    }
  goto end;
  }
  if(j>0 && namebuf[j]=='.') {
    setvar("wims_light_module","");
    j++; ovlstrcpy(lbuf,namebuf+j); namebuf[j]=0;
    for(i=k=0;i<available_lang_no;i++) {
      if(strcmp(lbuf,available_lang[i])==0) continue;
      snprintf(namebuf+j,MAX_FNAME-j,"%s/main.phtml",
           available_lang[i]);
      if(ftest(namebuf)<0) continue;
      if(k>0) *p++=',';
      mystrncpy(p,available_lang[i],sizeof(listbuf)-4-(p-listbuf));
      p+=strlen(p);
      k++;
    }
  }
  end: setvar("wims_otherlangs",listbuf); otherlangs_got=1;
}

/* Standardised reference to wims home */
void exec_homeref(char *p)
{
  char *ref, *user;

  if(ismhelp || tail_executed) return;
  setvar("wims_homeref_parm",p); *p=0;
  user=getvar("wims_user");
  if(user==NULL) user="";
  if(*user==0 || robot_access) ref=home_referer;
  else {
    if(strcmp(user,"supervisor")==0) ref=home_referer_supervisor;
    else ref=home_referer_user;
  }
  if(user[0]==0 && !robot_access) other_langs();
  phtml_put_base(ref,0); tail_executed=1;
}

/* Standardised header menu */
void exec_headmenu(char *p)
{
  char *ref, *user;

  if(header_executed) return;
  setvar("wims_headmenu_parm",p); *p=0;
  user=getvar("wims_user");
  if(user==NULL) user="";
  if(*user==0 || robot_access) ref=header_menu;
  else {
    if(strcmp(user,"supervisor")==0) ref=header_menu_supervisor;
    else ref=header_menu_user;
  }
  if(user[0]==0 && !robot_access) other_langs();
  phtml_put_base(ref,0); header_executed=1;
}

/* uniformized title */
void exec_title(char *p)
{
  char *s;
  *p=0;
  if(!outputing) return;
  s=getvar("wims_title_title");
  if(s==NULL || *s==0) {
    s=getvar("module_title");
    if(s==NULL || *s==0) return;
    force_setvar("wims_title_title",s);
  }
  phtml_put_base(title_page,0);
}

/* standardized html tail */
void exec_tail(char *p)
{
  if(!outputing || tail_executed) {
    *p=0; return;
  }
  if(!ismhelp) exec_homeref(p);
  *p=0;
  _output_("</body></html>");
  tail_executed=1;
}

void exec_formend(char *p)
{
  _output_("\n</div></form>");
}

void determine_font(char *l);

/* see changelog wims_mathml for history */
void _headmathjax (char *p)
{
  _output_("\n<script>/*<![CDATA[*/\
function wims_mathml_zoom(id){\
var math = document.getElementById(id);\
if(math.getAttribute(\"mathsize\") == \"100%\"){\
  math.setAttribute(\"mathsize\",\"200%\");}else{\
  math.setAttribute(\"mathsize\",\"100%\");};\
};\
window.addEventListener(\"load\", function() {\
  var ua = navigator.userAgent.toLowerCase();\
  var gecko = ua.indexOf(\"gecko\") > -1 && ua.indexOf(\"khtml\") === -1 && ua.indexOf(\"trident\") === -1;\
  if(! gecko ){\
    var script = document.createElement(\"script\");\
    script.src  = \"scripts/js/mathjax/mml-chtml.js\";\
    document.head.appendChild(script);\
  };\
});/*]]>*/</script>\n");

  // Safari/MathJax bugfix : refresh display when document ready.
  _output_("\n<script>\
reload_mathML=function(){var t=document.getElementsByClassName(\"wims_mathml\");Array.prototype.filter.call(t,function(t){var e=t.style.display;t.style.display=\"none\",t.style.opacity=\"0\",setTimeout(function(){t.style.display=e},1e3),setTimeout(function(){t.style.opacity=\"1\"},1100)})},document.onreadystatechange=function(){\"complete\"===document.readyState&&(/^((?!chrome|android).)*safari/i.test(navigator.userAgent)&&reload_mathML())};\
</script>");
}

/* standardized header */
void _header(char *p, int option)
{
  char *s1, *s2, hbuf[MAX_LINELEN+1], *ws="", *ws2="", *bo, *ol;
  char wsbuf[MAX_LINELEN+1],wsbuf2[MAX_LINELEN+1];
  setvar("wims_header_parm",p); *p=0;
  if(!outputing || header_executed) return;
  s1=getvar("wims_window");
  if(mode==mode_popup) {
    if(s1!=NULL && *s1!=0) {
      char *p1, *p2;
      int t1,t2/*,t3,t4*/;
      p1=find_word_start(s1);
      for(p2=p1; myisdigit(*p2); p2++);
      *p2=0; t1=atoi(p1); p1=p2+1;
      while(!myisdigit(*p1) && *p1) p1++;
      for(p2=p1; myisdigit(*p2); p2++);
      *p2=0; t2=atoi(p1); p1=p2+1;
/*        while(!myisdigit(*p1) && *p1) p1++;
        for(p2=p1; myisdigit(*p2); p2++);
        *p2=0; t3=atoi(p1); p1=p2+1;
        while(!myisdigit(*p1) && *p1) p1++;
        for(p2=p1; myisdigit(*p2); p2++);
        *p2=0; t4=atoi(p1); p1=p2+1;
        while(!myisdigit(*p1) && *p1) p1++;
        for(p2=p1; myisdigit(*p2); p2++);
        if(t3<5) t3=5; if(t4<20) t4=20;
*/     snprintf(wsbuf,sizeof(wsbuf),
          "window.focus();window.resizeTo(%d,%d);",t1,t2); ws=wsbuf;
/*      snprintf(wsbuf,sizeof(wsbuf),
             "window.focus();window.resizeto(%d,%d);window.moveto(%d,%d);",
             t1,t2,t3,t4); ws=wsbuf;
*/   }
  }
  else {
    if(s1!=NULL && strcmp(s1,"new")==0)
      ws="window.focus();window.resizeTo(800,640);window.moveTo(15,35);";
  }
  if(strstr(session_prefix,"_exam")!=NULL) {
/*    char buf[64]; */
    if(*ws==0) ws="window.focus();";
    else ws= "window.focus();window.moveTo(5,70);";
/*    snprintf(buf,sizeof(buf),"name.phtml.%s",lang);
      phtml_put_base(buf);
      phtml_put_base("jsclock.phtml"); */
  }
  s1=getvar("wims_html_header"); if(s1==NULL) s1="";
  determine_font(getvar("module_language"));
  s2=getvar("module_title"); if(s2!=NULL && *s2!=0) {
    mystrncpy(hbuf,s2,sizeof(hbuf)); calc_detag(hbuf);
    setvar("module_title2",hbuf);
  }
  mystrncpy(hbuf,s1,sizeof(hbuf)); substit(hbuf);
  s2=getvar("wims_htmlbody"); if(s2==NULL) s2="";
  bo=getvar("wims_html_bodyoption"); if(bo==NULL) bo="";
  ws2=getvar("wims_html_onload"); if(ws2==NULL) ws2="";
  snprintf(wsbuf2,sizeof(wsbuf2),"%s%s",ws,ws2);
  setvar("wims_html_onload",wsbuf2);
  if(wsbuf2[0]) ol=" onload="; else ol="";

  output("<!DOCTYPE html>\
    \n<html lang=\"%s\"><head>\n\
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0\">\n\
    %s\n", lang, hbuf);
  if(robot_access){
    output("<link rel=\"stylesheet\" href=\"html/themes/_css/robots.css\">");
  }

  _headmathjax(p);

  if(ol[0]) {
    output("</head>\n<body %s %s\"%s\" %s>", s2, ol, wsbuf2, bo);}
  else {
    output("</head>\n<body %s %s%s %s>", s2, ol, wsbuf2, bo);
  }
  exec_headmenu(p);
  if(option) exec_title(p);
  if(cmd_type==cmd_help) {
    char *s=getvar("special_parm");
    if(s==NULL) s="";
    m_file.linepointer=m_file.linecnt;
    if(strcmp(s,"about")==0) ovlstrcpy(hbuf,"about.phtml");
    else ovlstrcpy(hbuf,"help.phtml");
    exec_read(hbuf); exec_tail(p); /* param of exec_...() must be readable */
    return;
  }
  header_executed=1;
}

void exec_header(char *p) { _header(p,1);}
void exec_header1(char *p) { _header(p,0);}
void exec_headmathjax(char *p) { _headmathjax(p);}

char href_target[128];
char jsbuf[512];
#define jsstr " onclick=\"%s=window.open('','%s','status=no,toolbar=no,location=no,menubar=no,scrollbars=yes,resizable=yes')\""
int ref_mhelp=0;
int follow_list[]={
  ro_session, ro_lang, ro_useropts, ro_module
};
#define follow_no (sizeof(follow_list)/sizeof(follow_list[0]))

void _httpfollow(char b1[], char *wn, int new)
{
  int i;
  char *p1, *s, *ss, sb[MAX_LINELEN+1], qbuf[MAX_LINELEN+1];

  sb[0]=0;
  for(i=0;i<follow_no;i++) {
    if(robot_access && follow_list[i]==ro_session) continue;
    if(!new && follow_list[i]!=ro_session
        && follow_list[i]!=ro_module && follow_list[i]!=ro_lang)
      continue;
    if(follow_list[i]==ro_module) {
      char *pp;
      if(new) continue;
      pp=strstr(b1,"cmd=");
      if(pp==NULL) continue;
      pp+=strlen("cmd=");
      if(memcmp(pp,"intro",strlen("intro"))==0 ||
         memcmp(pp,"new",strlen("new"))==0) continue;
    }
    s=getvar(ro_name[follow_list[i]]);
    ss=strstr(b1,ro_name[follow_list[i]]);
    if(s!=NULL && *s!=0 &&
      (ss==NULL || (ss>b1 && *(ss-1)!='&')
       || *(ss+strlen(ro_name[follow_list[i]]))!='=')) {
      if(follow_list[i]==ro_session && memcmp(href_target,"wims_",5)==0) {
        char st[MAX_LINELEN+1];
        char *s1;
        s1=getvar("wims_session");
        if(s1==NULL) internal_error("exec_href() error.\n");
        if(ref_mhelp) {
          if(strstr(s1,"_mhelp")!=0)
            snprintf(st,sizeof(st),"%s",s1);
          else
            snprintf(st,sizeof(st),"%s_mhelp",s1);
        }
        else snprintf(st,sizeof(st),"%.10s%s",s1,href_target+4);
          s=st;
      }
      snprintf(sb+strlen(sb),MAX_LINELEN-strlen(sb),"%s=%s&",
             ro_name[follow_list[i]],s);
      if(ismhelp && follow_list[i]==ro_session &&
           strstr(sb,"_mhelp")==NULL)
        snprintf(sb+strlen(sb)-1,MAX_LINELEN-strlen(sb)+1,"_mhelp&");
      }
  }
  snprintf(qbuf,MAX_LINELEN,"%s%s%s",wn,sb,b1);
  /* cleaning up query string */
  for(p1=qbuf;*p1;p1++) {
    if(*p1=='"') string_modify(qbuf,p1,p1+1,"%22");
    if(*p1=='&' && isalpha(*(p1+1))) {
      p1++; string_modify(qbuf,p1,p1,"+");
    }
  }
  mystrncpy(b1,qbuf,MAX_LINELEN);
}

/* Restart with a new module, using http code 302. */
void exec_restart(char *p)
{
  /* buf will contain all the url parameters*/
  /* buf2 will be the final URL */
  char buf[MAX_LINELEN+1], *rfn, buf2[MAX_LINELEN+1];
  /* long int t; */

  if(robot_access || outputing || !trusted_module() || is_class_module) return;
  /* accessfile(buf,"r","%s/restart.time",s2_prefix);
    t=atoi(buf); if(t==nowtime) return;    */ /* possible looping */
  mystrncpy(buf,find_word_start(p),sizeof(buf));
  *find_word_end(buf)=0;
  _httpfollow(buf,"",0);
  nph_header(302);
  rfn=strchr(ref_name,':');
  if(rfn==NULL) {
    usual: snprintf(buf2,sizeof(buf2),"%s?%s",ref_name,buf);
  }
  else {
    char *p;
    p=getvar("wims_protocol");
    if(p!=NULL && strcmp(p,"https")==0) {
      snprintf(buf2,sizeof(buf2),"https%s?%s",rfn,buf);
    }
    else goto usual;
  }
  printf("Location: %s\r\n\r\n\
<!DOCTYPE html>\
<html><body><a href=\"%s\">%s</a></body></html>",buf2,buf2,buf2);
  close_working_file(&m_file,0); write_logs();
  snprintf(buf,sizeof(buf),"%ld",(long)nowtime);
/*    accessfile(buf,"w","%s/restart.time",s2_prefix);*/
  delete_pid(); exit(0);
}

/*  extract target tag from parm string. */
void href_find_target(char *p)
{
  char *pp, *pe,buf1[MAX_LINELEN+1];
  href_target[0]=0; jsbuf[0]=0; ref_mhelp=0;
  for(pp=find_word_start(p);*pp!=0;pp=find_word_start(pe)) {
    pe=find_word_end(pp);
    if(strncasecmp(pp,"target=wims_",strlen("target=wims_"))!=0) continue;
    memmove(buf1,pp,pe-pp); buf1[pe-pp]=0; substit(buf1);
    if(strncasecmp(buf1,"target=wims_mhelp",strlen("target=wims_mhelp"))==0) {
      if(*pe!=0) *pe++=0;
      ovlstrcpy(href_target,"wims_help");
      ref_mhelp=1;
    }
    else {
      if(*pe!=0) *pe++=0;
        mystrncpy(href_target,buf1+strlen("target="),sizeof(href_target));
    }
    snprintf(jsbuf,sizeof(jsbuf),jsstr,href_target,href_target);
    ovlstrcpy(pp,pe);return;
  }
  pp=getvar("module_help");
  if(href_target[0]==0 && pp!=NULL && strcmp(pp,"popup")==0 &&
     (pe=strstr(p,"cmd=help"))!=NULL) {
    if(pe==p || *(pe-1)=='&') {
      ovlstrcpy(href_target,"wims_help"); ref_mhelp=1;
      snprintf(jsbuf,sizeof(jsbuf),jsstr,href_target,href_target);
    }
  }
}

void _href_getdef(char src[], char vname[], char buf[], int buflen)
{
  char *p1, *p2, *p3;
  buf[0]=0;
  for(p1=strstr(src,vname); p1; p1=strstr(p2,vname)) {
    p2=p1+strlen(vname); if(*p2!='=') continue;
    if(p1>src && *(p1-1)!='&') continue;
    p2++; p3=strchr(p2,'&'); if(p3==NULL) p3=p2+strlen(p2);
    if(p3-p2>=buflen) return; /* too long */
    memmove(buf,p2, p3-p2); buf[p3-p2]=0; return;
  }
}

/* Create href to wims requests. subst() is not done. */
void exec_href(char *p)
{
  char *s, st[128], sti[128], stc[128], stt[128], *p1, *p2, *p3, *wn="";
  char *U="<span class=\"disabled_link\">%s</span>";
  char b1[MAX_LINELEN+1], b2[MAX_LINELEN+1];
  int new=0;
  if(!outputing) return;
  href_find_target(p);
  p1=find_word_start(p);
  p2=find_word_end(p1); if(*p2) *(p2++)=0;
  mystrncpy(b1,p1,sizeof(b1));
  mystrncpy(b2,find_word_start(p2),sizeof(b2));
  substit(b1); substit(b2);
  /* standard reference */
  if(*b2==0 && strchr(b1,'=')==NULL) {
    char b[MAX_LINELEN+1], *ll;
    p1=find_word_start(b1); *find_word_end(p1)=0;
    if(*p1==0 || strlen(p1)>64) return;
    ll=lang;
    if(ll==NULL || *ll==0 || *(ll+1)==0 || *(ll+2)!=0) ll=getvar("module_language");
    accessfile(b,"r","html/href.%s",ll);
    memmove(p1+1,p1,64); *p1='\n'; strcat(p1,"	");
    p2=strstr(b,p1); if(p2==NULL) return;
    p1=find_word_start(p2+strlen(p1)); p2=find_word_end(p1);
    if(*p2) *(p2++)=0;
    p3=strchr(p2,'\n'); if(p3!=NULL) *p3=0;
    mystrncpy(b1,p1,sizeof(b1));
    mystrncpy(b2,find_word_start(p2),sizeof(b2));
    substit(b1); substit(b2);
  }
  /* for robots: only references without defining cmd. */
  if(robot_access && strstr(b1,"cmd=")!=NULL &&
      strstr(b1,"module=adm/doc")==NULL) {
    _output_(b2); return;
  }
  if(robot_access && strstr(aliased_cgi,"yes")!=NULL) {
    char mbuf[256], lbuf[16];
    _href_getdef(b1,"module",mbuf,sizeof(mbuf));
    if(mbuf[0]==0) mystrncpy(mbuf,home_module,sizeof(mbuf));
    _href_getdef(b1,"lang",lbuf,sizeof(lbuf));
    if(strlen(lbuf)!=2) {mystrncpy(lbuf,lang,4);lbuf[2]=0;}
    if(strncmp(mbuf,"adm/doc",strlen("adm/doc"))==0) {
      char dbuf[256], bbuf[256];
      _href_getdef(b1,"doc",dbuf,sizeof(dbuf));
      _href_getdef(b1,"block",bbuf,sizeof(bbuf));
      if(!myisdigit(dbuf[0])) dbuf[0]=0;
      if(dbuf[0]!=0 && bbuf[0]==0) snprintf(bbuf,sizeof(bbuf),"main");
      if(dbuf[0]==0)
        output("<a href=\"%s%s_doc~.html\">%s</a>", ref_base,lbuf,b2);
      else
        output("<a href=\"%s%s_doc~%s~%s.html\">%s</a>",
           ref_base,lbuf,dbuf,bbuf,b2);
    }
    else {
      for(s=strchr(mbuf,'/'); s!=NULL; s=strchr(s+1,'/')) *s='~';
      output("<a href=\"%s%s_%s.html\">%s</a>", ref_base,lbuf,mbuf,b2);
    }
    return;
  }
  s=getvar("wims_ref_id");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(sti,sizeof(sti)," id=\"%s\"",s);
  }
  else sti[0]=0;

  s=getvar("wims_ref_class");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    // If class contains "button", add "role=button" for accessibility
    if(strstr(s,"button")) {
      snprintf(stc,sizeof(stc)," class=\"%s\" role=\"button\"",s);
    } else {
      snprintf(stc,sizeof(stc)," class=\"%s\"",s);
    }
  }
  else stc[0]=0;

  s=getvar("wims_ref_title");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(stt,sizeof(stt)," title=\"%s\"",s);
  }
  else stt[0]=0;

  s=getvar("wims_ref_target");
  if(href_target[0]!=0) s=href_target;
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(st,sizeof(st)," target=\"%s\"",s);
    if(strcmp(s,"_parent")!=0) {
      new=1; wn="wims_window=new&";
    }
  }
  else st[0]=0;
  _httpfollow(b1,wn,new);
  tohttpquery(b1);
  if(strstr(session_prefix,"_check")!=NULL) {
    if(*b2) output(U,b2);
  else _output_("<a id=\"0\"></a>");
  return;
  }
  if(jsbuf[0]==0 && st[0]==0 && strstr(session_prefix,"_exam")!=NULL) {
    p1=strstr(b1,"cmd=");
    if(p1!=NULL) {
      p1+=strlen("cmd=");
      if(strncmp(p1,"new",3)==0 || strncmp(p1,"renew",5)==0 ||
          strncmp(p1,"intro",5)==0) {
        if(*b2) output(U,b2);
        else _output_("<a id=\"#\"></a>");
        return;
      }
    }
  }
  if(*b2)
    output("<a href=\"%s?%s\"%s%s %s %s %s>%s</a>",
       ref_name, b1, st, jsbuf,sti,stc,stt,b2);
  else
    output("<a href=\"%s?%s\"%s%s %s %s %s>",ref_name, b1, st, jsbuf,sti,stc,stt);
  setvar("wims_ref_id","");
  setvar("wims_ref_class","");
  setvar("wims_ref_title","");
}

/* Create form refering to the page. */
void exec_form(char *p)
{
  char *s, *p1, *p2, *a, *m, *opt, st[128], *wn="", *form_id;
  char abuf[128], idbuf[128];
  int i, new=0;
  if(!outputing) return;
  href_find_target(p);
  s=getvar("wims_ref_target");
  if(href_target[0]!=0) s=href_target;
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(st,sizeof(st)," target=\"%s\"",s);
    if(strcmp(s,"_parent")!=0) {
      new=1; wn="<input type=\"hidden\" name=\"wims_window\" value=\"yes\">\n";
    }
  }
  else st[0]=0;
  a=getvar("wims_ref_anchor"); if(a==NULL) a="";
  opt=find_word_start(find_word_end(find_word_start(p)));
  m=getvar("wims_form_method");
  if(m!=NULL) {
    m=find_word_start(m);
    if(strncasecmp(m,"post",4)==0) m="post";
    else if(strncasecmp(m,"get",3)==0) m="get";
      else if(strncasecmp(m,"file",4)==0) {
       m="post\" enctype=\"multipart/form-data";
      snprintf(abuf,sizeof(abuf),"?form-data%ld%s",random(),a); a=abuf;
      force_setvar("wims_form_method","");
    }
    else m=default_form_method;
  }
  else m=default_form_method;

  if(strstr(session_prefix,"_check")!=NULL) {
    output("<form action=\"NON_EXISTING_PAGE\" onsubmit=\"window.close();\" %s>\n",
           opt);
    return;
  }

  form_id=getvar("wims_form_id");
  if(form_id==NULL || *form_id==0 || isspace(*form_id)){
    form_id="";
  } else {
    snprintf(idbuf,sizeof(idbuf),"id=\"%s\"",find_word_start(form_id));form_id=idbuf;
  }
  force_setvar("wims_form_id","");

  output("<form action=\"%s%s\"%s method=\"%s\" %s %s>\n<div class='wims_form'>%s",ref_name,a,st,m,opt,form_id,wn);
  if(a!=abuf && a[0]) force_setvar("wims_ref_anchor","");
  for(i=0;i<follow_no;i++) {
    if(robot_access && follow_list[i]==ro_session) continue;
    if(!new && follow_list[i]!=ro_session
     && follow_list[i]!=ro_module && follow_list[i]!=ro_lang)
      continue;
    if(follow_list[i]==ro_module) continue;
    s=getvar(ro_name[follow_list[i]]);
    if(s!=NULL && *s!=0) {
      if(follow_list[i]==ro_session && memcmp(href_target,"wims_",5)==0) {
        char st[MAX_LINELEN+1];
        char *s1;
        s1=getvar("wims_session");
        if(s1==NULL) internal_error("exec_form() error.\n");
        snprintf(st,sizeof(st),"%.10s%s",s1,href_target+4);
        s=st;
      }
      output("<input type=\"hidden\" name=\"%s\" value=\"%s\">\n",
           ro_name[follow_list[i]],s);
    }
  }
  p1=find_word_start(p);p2=find_word_end(p1);
  if(p2>p1) {
    char buf[64];
    int i;
    i=p2-p1; if(i>60) i=60;
    memmove(buf,p1,i);buf[i]=0;
    for(i=0;i<CMD_NO && strcmp(buf,commands[i]);i++);
    if(i<CMD_NO) {
      output("<input type=\"hidden\" name=\"cmd\" value=\"%s\">\n",buf);
      if(i!=cmd_intro && i!=cmd_new)
        output("<input type=\"hidden\" name=\"module\" value=\"%s\">\n",
            getvar(ro_name[ro_module]));
    }
  }
}

/* Creat link to trap robot access, an internal command
 * which should not be documented
 */
void exec_robottrap(char *p)
{
  char buf[MAX_LINELEN+1];
  if(robot_access) return;
  ovlstrcpy(buf,"session=$wims_session.1&module=adm/trap");
  _output_("<!-- "); exec_href(buf);_output_("Robot trapper, do not click!</a> -->");
  _output_("<div class='wimstrap hide' aria-hidden='true'>");exec_href(buf); _output_("<span lang='en'>Robot trapper, do not go there!</span></a></div>");
}

/* set definitions in a file. Trusted modules only. */
void exec_setdef(char *p)
{
   char *p1, *pp;
   char nbuf[MAX_LINELEN+1], fbuf[MAX_LINELEN+1], tbuf[MAX_LINELEN+1];
   if(robot_access || !trusted_module() || is_class_module) return;
   p1=wordchr(p,"in"); if(p1==NULL) module_error("syntax_error");
   *p1=0; p1=find_word_start(p1+strlen("in"));
   ovlstrcpy(nbuf,p);
   mystrncpy(tbuf,p1,sizeof(tbuf));
   substit(nbuf); substit(tbuf);
   if(find_module_file(tbuf,fbuf,1)) return;
   pp=find_word_start(nbuf); p1=find_word_start(fbuf); *find_word_end(p1)=0;
   strip_trailing_spaces(pp);
   setdef(p1,pp);
}

/* Set a variable. */
void exec_set(char *name)
{
  char *p, *defn, *parm;
  char tbuf2[MAX_LINELEN+1], namebuf[MAX_LINELEN+1];
  int i;

  p=strchr(name,'=');
  if(p==NULL) return; /* warning or error! */
  *p=0; defn=find_word_start(p+1);
  *find_word_end(name)=0;
  mystrncpy(namebuf,find_word_start(name),sizeof(namebuf));
  /* we allow substit in names, to implement array */
  substit(namebuf); *find_word_end(namebuf)=0;
  if(*defn!=calc_prefix_char) {
  /* substitute by default */
    mystrncpy(tbuf2,defn,sizeof(tbuf2));
    substit(tbuf2); setvar(namebuf,tbuf2); return;
  }
  /* called from !readdef */
  if((untrust&4)!=0) module_error("not_trusted");
  /* definition is a command  */
  parm=find_word_end(defn+1);
  if( *parm != 0 ) { *parm=0; parm=find_word_start(parm+1); }
  i=m_file.lines[m_file.l].varcode;
  if(i<0) {
    i=search_list(calc_routine,CALC_FN_NO,sizeof(calc_routine[0]),defn+1);
    m_file.lines[m_file.l].varcode=i;
  }
  if(i<0) {
      /* replace by warning? */
    setvar(error_data_string,defn+1); module_error("bad_cmd");
    return;
  }
  mystrncpy(tbuf2,parm,sizeof(tbuf2)); execnt++;
  if(calc_routine[i].tag==0) substit(tbuf2);
  tbuf2[sizeof(tbuf2)-1]=0; calc_routine[i].routine(tbuf2);
      /* remove trailing new line */
  tbuf2[sizeof(tbuf2)-1]=0;
  if(tbuf2[strlen(tbuf2)-1]=='\n') tbuf2[strlen(tbuf2)-1]=0;
    setvar(namebuf,tbuf2);
}

/* set but do not overwrite. */
void exec_default(char *p)
{
  char *start, *end, c, *pp;
  char namebuf[MAX_LINELEN+1];
  start=find_word_start(p);
  for(end=start;*end!=0 && !isspace(*end) && *end!='='; end++);
  c=*end; *end=0;
  if(end-start<=MAX_LINELEN-1) {
    memmove(namebuf,start,end-start+1); substit(namebuf);
    pp=getvar(namebuf);
    if(pp!=NULL && *pp!=0) return;
  }
  *end=c; exec_set(p);
}

/* Does nothing; just a comment. */
void exec_comment(char *p)
{
  return;
}

/* Exit the file under interpretation */
void exec_exit(char *p)
{
  m_file.linepointer=m_file.linecnt;
  return;
}

/* output a file. Aliases:
     * getfile, outfile, fileout */
void exec_getfile(char *p)
{
  char *s, sti[128], stc[128], stt[128], sgf[128], *p1, url[MAX_LINELEN+1];
  char *prompt;

  p=find_word_start(p); prompt=find_word_end(p);
  if(*prompt!=0) *prompt++=0;
  prompt=find_word_start(prompt);
  if(*p==0 || !outputing) return;
  if(!trusted_module() || is_class_module) return;
  s=getvar(ro_name[ro_session]);
  if(s==NULL || *s==0 || strstr(s,"robot")!=NULL) return;
  mystrncpy(url,ref_name,sizeof(url));
  for(p1=url+strlen(url);p1>url && *(p1-1)!='/'; p1--);
  if(good_httpd) snprintf(p1,sizeof(url)+p1-url,
              "getfile/%s?&+session=%s&+modif=%ld",
              p,s,(long)nowtime);
  else snprintf(url,sizeof(url),
      "%s?cmd=getfile&+session=%s&+special_parm=%s&+modif=%ld",
          ref_name,s,p,(long)nowtime);
  snprintf(jsbuf,sizeof(jsbuf),jsstr,"wims_file","wims_file");

  s=getvar("wims_ref_id");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(sti,sizeof(sti)," id=\"%s\"",s);
  }
  else sti[0]=0;

  s=getvar("wims_ref_class");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(stc,sizeof(stc)," class=\"%s\"",s);
  }
  else stc[0]=0;

  s=getvar("wims_ref_title");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(stt,sizeof(stt)," title=\"%s\"",s);
  }
  else stt[0]=0;

  s=getvar("wims_getfile_fname");
  if(s!=NULL && *s!=0 && !isspace(*s)) {
    snprintf(sgf,sizeof(sgf)," download=\"%s\"",s);
  }
  else sgf[0]=0;

  if(*prompt) output("<a href=\"%s\" %s %s %s %s>%s</a>\n", url,sti,stc,stt,sgf,prompt);
  else output("<a href=\"%s\" %s %s %s></a>",url,sti,stc,stt);

  setvar("wims_ref_id","");
  setvar("wims_ref_class","");
  setvar("wims_ref_title","");
  setvar("wims_getfile_fname","");
}

/* internal */
void count_insert(void)
{
  insert_no++;
  if(insert_no>=INS_LIMIT) module_error("too_many_ins");
}

int animated_ins=0;
int grouped_ins=0;

/* generic insertion */
void _exec_ins(char *p, char *script_name,char *format)
{
  char *s, *b, *at, *tag, *tag2, *al, *fmt, *mh, *buftex=NULL;
  char *p1, *pt;
  char buf[1024],buf2[1024],url[MAX_LINELEN+1],altbuf[1024];
  char outbuf[1024];
  int border, middle, vspace;
  long int tel;

  if(robot_access) return;
  count_insert(); outbuf[0]=0;
  setenv("ins_source",p,1); /* value kept from user tamper */
  if(animated_ins) fmt=getvar("anim_format"); else fmt=format;
  if(fmt==NULL) fmt="gif";
  if(ismhelp) mh="mh"; else mh="";
  snprintf(buf,sizeof(buf),"%s/insert%s-%d.%s",s2_prefix,mh,insert_no,fmt);
  if(grouped_ins) {unlink(buf); goto grouped;}
  exportall();
  call_ssh("%s/%s %d %s >%s/ins.out 2>%s/ins.err",
      bin_dir,script_name,insert_no,tmp_dir,tmp_dir,tmp_dir);
  unlink(buf); wrapexec=1;
  if(trusted_module()) setenv("trusted_module","yes",1);
  else if(untrust) setenv("trusted_module","no",1);
  call_ssh("mv %s/insert%s-%d.%s %s >/dev/null 2>/dev/null",
       tmp_dir,mh,insert_no,fmt,s2_prefix);
  tel=filelength("%s", buf);
  if(tel<=5) {
    char bbuf[MAX_LINELEN+1];
    accessfile(bbuf,"r","%s/ins.err",tmp_dir);
    snprintf(url,sizeof(url),"gifs/badins.gif");
    for(p1=bbuf;p1<bbuf+512 && *p1;p1++)
      if(*p1=='<' || *p1=='>') *p1='?';
    *p1=0;
    if(bbuf[0]==0) snprintf(bbuf,sizeof(bbuf),"Fail");
    snprintf(outbuf+strlen(outbuf),sizeof(outbuf)-strlen(outbuf),
       " <img src=\"%s\" alt=\"Error\"> <small><pre>%s</pre></small> <br> ",
       url,bbuf);
    setvar("ins_warn","fail");
    setvar("ins_cnt","0");
    goto reset;
  }
  grouped:
  s=getvar(ro_name[ro_session]);
  b=getvar("ins_border"); at=getvar("ins_attr");
  tag=getvar("ins_tag");  al=getvar("ins_align");
  if(at==NULL) at="";
  if(tag==NULL) tag="";
  if(al==NULL) al="";
  al=find_word_start(al);
  if(*al!=0) snprintf(buf2,sizeof(buf2),"vertical-align:%s",al); else buf2[0]=0;
  if(strcasecmp(al,"middle")==0) middle=1; else middle=0;
  tag2=""; vspace=0;
  if(*tag!=0) {
    mystrncpy(buf,tag,sizeof(buf)); tag=find_word_start(buf);
    tag2=find_word_end(tag);
    if(*tag2!=0) *tag2++=0;
    tag2=find_word_start(tag2);
  }
  if(b==NULL || *b==0) border=0;
  else border=atoi(b);
  if(border<0) border=0;
  if(border>100) border=100;
  if(middle) {
    snprintf(outbuf+strlen(outbuf),
       sizeof(outbuf)-strlen(outbuf),"%s",mathalign_sup1);
    vspace=2;
  }
  mystrncpy(url,ref_name,sizeof(url));
  for(p1=url+strlen(url);p1>url && *(p1-1)!='/'; p1--);
  snprintf(p1,sizeof(url)+p1-url,
         "wims.%s?cmd=getins&+session=%s&+special_parm=insert%s-%d.%s&+modif=%ld",
         fmt,s,mh,insert_no,fmt,(long)nowtime);
  if(strchr(ins_alt,'"')!=NULL || strlen(ins_alt)>256) ins_alt[0]=0;
  pt=getvar("wims_ins_alt"); if(pt==NULL) pt="";
  if(strcmp(pt,"empty")==0) altbuf[0]=0;
  else
    if(ins_alt[0] && strcmp(pt,"none")!=0)
      snprintf(altbuf,sizeof(altbuf)," alt=\"\"");
    else
      snprintf(altbuf,sizeof(altbuf)," alt=\"%s\"",ins_alt);
  if(strcasecmp(tag,"form")!=0) {
    if(strcasecmp(fmt,"tex")==0){
      snprintf(buf,sizeof(buf), "%s/insert%s-%d.%s",s2_prefix,mh,insert_no,fmt);
      buftex=readfile(buf,NULL,MAX_LINELEN);
    }
    else
      snprintf(outbuf+strlen(outbuf),sizeof(outbuf)-strlen(outbuf),
       "<img src=\"%s\" class=\"wims_mathimg\" style=\"border-width:%dpx;margin-bottom:%dpx;%s\" %s %s>",
       url, border, vspace, buf2, at, altbuf);
  }
  else {
    char *n, *nend;
/* fix: add quotes at name=" " */
    if(*tag2!=0) {n="name=\"" ; nend="\"";} else {n=""; nend="";}
    snprintf(outbuf+strlen(outbuf),sizeof(outbuf)-strlen(outbuf),
        "<input type=\"image\" %s%s%s src=\"%s\" style=\"border:solid;border-width:%dpx;margin-bottom:%dpx;%s\" %s %s>",
        n,tag2,nend,url,border,vspace,buf2,at,altbuf);
  }
  if(middle) snprintf(outbuf+strlen(outbuf),
           sizeof(outbuf)-strlen(outbuf),"%s",mathalign_sup2);
  setvar("ins_warn",""); ins_alt[0]=0;
  setvar("ins_cnt",int2str(insert_no));
  reset:
  if(outputing) _output_(outbuf);
  setvar("ins_out",outbuf);
  setvar("ins_attr",""); setvar("ins_tag","");
  setvar("ins_url",url);
  setvar("ins_tikz",buftex?buftex:"");
  snprintf(buf2,sizeof(buf2),"insert%s-%d.%s",mh,insert_no,fmt);
  setvar("ins_filename",buf2);
  animated_ins=0;
  if (buftex) free(buftex);
}

/* instex: dynamically insert tex outputs */
void exec_instex(char *p)
{
  char *ts, *tc, *f, *mh, buf[MAX_FNAME+1];

  if(robot_access) {
    *p=0; return;
  }
  f=instex_check_static(p); substit(p);
  if(f==NULL) {
/* Use static instex if there is no real substitution
 * and the source file is not in sessions directory.
 */
    calc_instexst(p); if(outputing) _output_(p);
    return;
  }
  if(ismhelp) mh="mh"; else mh="";
  fix_tex_size(); f="gif";
  setenv("texgif_style",instex_style,1);
  tc=getvar("instex_texheader"); if (tc) { setenv("texgif_texheader",tc,1);}
  setenv("texgif_tmpdir",tmp_dir,1);
  setenv("texgif_src",p,1);
  if(ins_alt[0]==0) mystrncpy(ins_alt,p,sizeof(ins_alt));
  mkfname(buf,"%s/insert%s-%d.gif",tmp_dir,mh,insert_no+1);
  setenv("texgif_outfile",buf,1);
  ts=getvar("wims_texsize"); tc=getvar("instex_color");
  if(lastout_file!=-1 && (tc==NULL || *tc==0) &&
      (ts==NULL || *ts==0 || strcmp(ts,"0")==0) &&
     strstr(p,"\\begin{")==NULL) {
    int ls, ln;
    char *pagebreak;
    ls=strlen(instex_src); ln=strlen(instex_fname);
    if(ls+strlen(p)>=MAX_LINELEN-256 ||
     ln+strlen(buf)>=MAX_LINELEN-16) {
        instex_flush(); ls=ln=0;
    }
    if(instex_cnt>0) pagebreak="\\pagebreak\n"; else pagebreak="";
    snprintf(instex_src+ls,MAX_LINELEN-ls,"%s %s %s %s\n",
         pagebreak,instex_style,p,instex_style);
    snprintf(instex_fname+ln,MAX_LINELEN-ln,"%s\n",buf);
      grouped_ins=1;
  }
  mkfname(buf,"%s/texgif.dvi",tmp_dir); unlink(buf);
  wrapexec=0; _exec_ins(p,instex_processor,f);
  if(grouped_ins) instex_cnt++;
  grouped_ins=0;
}

/* patches the gnuplot integer division (mis)feature. */
void gnuplot_patch(char *p,int oneline)
{
  char *pp;
  for(pp=strchr(p,'/');pp!=NULL;pp=strchr(pp+1,'/')) {
    char *p1;
    if(pp<=p || !myisdigit(*(pp-1)) || !myisdigit(*(pp+1))) continue;
    for(p1=pp-2;p1>=p && myisdigit(*p1);p1--);
    if(p1>=p && *p1=='.') continue;
    for(p1=pp+2;*p1 && myisdigit(*p1);p1++);
    if(*p1=='.') continue;
    string_modify(p,p1,p1,".0");
  }
  for(pp=strchr(p,'^');pp!=NULL;pp=strchr(pp+1,'^'))
    string_modify(p,pp,pp+1,"**");
/* disallow new lines and ';' */
  if(oneline)
    for(pp=p;*pp!=0;pp++) if(*pp==';' || *pp=='\n') *pp=' ';
}

/* This is to disable pipe in the gnuplot plotting function.
 * We do not allow ' followed by < .
 */
void prepare_insplot_parm(char *p)
{
  int i,j,multanim; char *pp, *s;
  double d;
  char setbuf[MAX_LINELEN+10],buf[MAX_LINELEN+1];

  j=strlen(p);
/* pipe in plot command */
  for(i=0;i<j;i++) {
    if(*(p+i)!='\'' && *(p+i)!='"') continue;
    pp=find_word_start(p+i+1); if(*pp=='<') module_error("illegal_plot_cmd");
  }
  gnuplot_patch(p,1);
/* multiplot */
  multanim=0;
  pp=getvar("insplot_split");
  if(pp!=NULL) i=linenum(pp); else i=0;
/* arbitrary limit: 16 multiplots */
  if(i>16) i=16;
  if(i>1) {
    char tbuf[MAX_LINELEN*(i+1)+100], bbuf[MAX_LINELEN+1];
    tbuf[0]=0;
    if(*p!=0) snprintf(tbuf,sizeof(tbuf),"%s\n",p);
    snprintf(buf,sizeof(buf),"%d",i); setenv("multiplot",buf,1);
    for(j=1;j<=i;j++) {
      snprintf(buf,sizeof(buf),"insplot_parm_%d",j);
      pp=getvar(buf);
      if(pp==NULL || *pp==0) {
        if(j==1 && *p!=0) continue;
        pp="";
      }
      else {
        mystrncpy(bbuf,pp,sizeof(bbuf));
        gnuplot_patch(bbuf,1);
      }
      strcat(tbuf,bbuf);strcat(tbuf,"\n");
    }
    setenv("insplot_source",tbuf,1);
    if(varchr(tbuf,"s")!=NULL) multanim=1;
  }
/* no illegal chaining */
  pp=getvar("insplot_font"); if(pp!=NULL) {
    for(s=pp;s<pp+MAX_LINELEN && *s;s++)
    if(*s==';' || *s=='\n' || *s==' ') *s=0;
    if(s>=pp+MAX_LINELEN) *s=0;
    setvar("insplot_font",pp);
  }
  pp=getvar("insplot_set");
  if(pp!=NULL) {
    char tbuf[MAX_LINELEN+1];
    mystrncpy(tbuf,pp,sizeof(tbuf));
    i=strlen(tbuf)-1;
    while(i>0 && isspace(tbuf[i])) i--;
    if(tbuf[i]==';') tbuf[i]=0;
    gnuplot_patch(tbuf,0);pp=tbuf;
    ovlstrcpy(setbuf,"set "); j=strlen("set ");
    for(i=0; *(pp+i)!=0 && j<MAX_LINELEN; i++) {
      if(*(pp+i)=='\n') {setbuf[j++]=' '; continue;}
      if(*(pp+i)!=';') {setbuf[j++]=*(pp+i); continue;}
      ovlstrcpy(setbuf+j,"\nset "); j+=strlen("\nset ");
    }
    setbuf[j]=0;
    setenv("insplot_set",setbuf,1);
  }
  else setenv("insplot_set","",1);
/* frames of animation */
  pp=getvar("ins_anim_frames");
  if(pp!=NULL) i=evalue(pp); else i=1;
  if(i>=ANIM_LIMIT) i=ANIM_LIMIT-1;
  if(i<1) i=1;
  if(strstr(setbuf,"step")==NULL && strstr(p,"step")==NULL
     && varchr(setbuf,"s")==NULL && varchr(p,"s")==NULL && !multanim) i=1;
  setenv("ins_anim_frames",int2str(i),1);
  setvar("ins_anim_frames","");
  if(i>1) {setvar("ins_animation","yes");animated_ins=1;}
  else setvar("ins_animation","no");
/* delay of animation */
  pp=getvar("ins_anim_delay");
  if(pp!=NULL) d=evalue(pp); else d=0;
  if(d>=10) d=10;
  if(d<0) d=0;
  setenv("ins_anim_delay",int2str(d*100),1);
}

/* Insert dynamic 2d plot */
void exec_insplot(char *p)
{
  char *fmt;
  if(robot_access) {
    *p=0; return;
  }
  fmt=getvar("ins_format"); if(fmt==NULL || *fmt==0) fmt=DEFAULT_INS_FORMAT;
  prepare_insplot_parm(p); setenv("insplot_method","2D",1);
  _exec_ins(p,insplot_processor,fmt);
  wrapexec=1;
/*    call_ssh("mv %s/insplot_cmd %s 2>/dev/null",tmp_dir,s2_prefix); */
   unsetenv("multiplot"); setvar("insplot_split","");
}

/* Insert dynamic 3d plot */
void exec_insplot3d(char *p)
{
  char *fmt;
  if(robot_access) {
    *p=0; return;
  }
  fmt=getvar("ins_format"); if(fmt==NULL || *fmt==0) fmt=DEFAULT_INS_FORMAT;
  prepare_insplot_parm(p); setenv("insplot_method","3D",1);
  _exec_ins(p,insplot_processor,fmt);
  wrapexec=1;
/*     call_ssh("mv %s/insplot_cmd %s 2>/dev/null",tmp_dir,s2_prefix); */
  unsetenv("multiplot");setvar("insplot_split","");
}

/* Insert dynamic gif draw. The parm preparation is specific to fly. */
void exec_insdraw(char *p)
{
  char *pp, *fmt;
  int i;
  double d;

  if(robot_access) {
    *p=0; return;
  }
/*    calc_tolower(p);*/
  fmt=getvar("ins_format"); if(fmt==NULL || *fmt==0) fmt=DEFAULT_INS_FORMAT;
  while((pp=wordchr(p,"output"))!=NULL) memmove(pp,"zqkwfx",6);
/* frames of animation */
  pp=getvar("ins_anim_frames");
  if(pp!=NULL) i=evalue(pp); else i=1;
  if(i>=ANIM_LIMIT) i=ANIM_LIMIT-1;
  if(i<1) i=1;
  if(i>1 && varchr(p,"s")==NULL && varchr(p,"animstep")==NULL
     && varchr(p,"step")==NULL) i=1;
  setenv("ins_anim_frames",int2str(i),1);
  setvar("ins_anim_frames","");
  if(i>1) {setvar("ins_animation","yes");animated_ins=1;}
  else setvar("ins_animation","no");
/* delay of animation */
  pp=getvar("ins_anim_delay");
  if(pp!=NULL) d=evalue(pp); else d=0;
  if(d>=10) d=10;
  if(d<0) d=0;
  setenv("ins_anim_delay",int2str(d*100),1);
  pp=getvar("insdraw_filebase");
  if(pp!=NULL && strstr(pp,parent_dir_string)!=NULL)
    setvar("insdraw_filebase","");
  _exec_ins(p,insdraw_processor,fmt);
}

void exec_increase(char *p)
{
  char *p1, *p2;
  p1=find_word_start(p); p2=find_word_end(p1);
  if(p2<=p1) {
    *p=0; return;
  }
  *p2=0;p2=getvar(p1);
  if(p2==NULL) p2="";
  setvar(p1,int2str(atoi(p2)+1)); *p=0;
}

void exec_setseed(char *p)
{
  char buf[64];
  snprintf(buf,sizeof(buf),"%s",p);
  force_setvar("wims_seed",buf);
  double d=evalue(p);
  srandom(d); *p=0;
}

/* bound a variable */
void exec_bound(char *p)
{
  char *p1, *p2, *p3;
  int doub,i,bcnt,defaulted;
  double d1,d2,dd,val;
  char nbuf[MAX_LINELEN+1],lbuf[MAX_LINELEN+1],dbuf[MAX_LINELEN+1];
  char vbuf[MAX_LINELEN+1];
  char *blist[2048];

  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2==0) {
    syntax: module_error("syntax_error");
  }
  *p2=0; ovlstrcpy(nbuf,p1);substit(nbuf); p1=find_word_start(p2+1);
  p2=getvar(nbuf);if(p2==NULL) p2="";
  mystrncpy(vbuf,find_word_start(p2),sizeof(vbuf));
  strip_trailing_spaces(vbuf);
  p2=find_word_end(p1); if(*p2==0) goto syntax;
  *p2=0;p2++;
  p3=wordchr(p2,"default");
  if(p3!=NULL) {
    *p3=0; defaulted=1;
    p3=find_word_start(p3+strlen("default"));
    ovlstrcpy(dbuf,p3); substit(dbuf);
  }
  else defaulted=0;
  if(strcmp(p1,"between")==0) {
    p1=find_word_start(p2);
    i=strlen("integer");
    if(strncmp(p1,"integer",i)==0 &&
        (isspace(*(p1+i)) || (*(p1+i)=='s' && isspace(*(p1+i+1))))) {
      doub=0; p1=find_word_start(find_word_end(p1));
      val=rint(evalue(vbuf));
      if(vbuf[0]) float2str(val,vbuf);
    }
    else {
      doub=1;val=evalue(vbuf);
    }
    p2=wordchr(p1,"and"); p3=p2+strlen("and");
    if(p2==NULL) {
      p2=strchr(p1,','); p3=p2+1;
    }
    if(p2==NULL) goto syntax;
   *p2=0;p2=find_word_start(p3);
    if(*p1==0 || *p2==0) goto syntax;
    d1=evalue(p1);d2=evalue(p2);
    if(!isfinite(d1) || !isfinite(d2) ||
      fabs(d1)>(double)(1E10) || fabs(d2)>(double)(1E10)) goto syntax;
    if(d1>d2) {
      dd=d1;d1=d2;d2=dd;
    }
    if(vbuf[0] && val<=d2 && val>=d1) {
      if(!doub) setvar(nbuf,vbuf);
      *p=0; return;
    }
    if(defaulted) ovlstrcpy(p,dbuf);
    else {
      if(!doub) {
        d1=ceil(d1);d2=floor(d2);
      }
      if(vbuf[0]==0 || val<d1) val=d1;
      else val=d2;
      float2str(val,p);
    }
    setvar(nbuf,p); *p=0; return;
  }
  else {
    if(strcmp(p1,"within")==0 || strcmp(p1,"among")==0) {
      ovlstrcpy(lbuf,p2);substit(lbuf);
      bcnt=cutitems(lbuf,blist,2048);
      if(bcnt<=0) {
        *p=0; return;
      }
      for(i=0;i<bcnt;i++) {
        if(strcmp(blist[i],vbuf)==0) {
          *p=0; return;
        }
      }
      if(defaulted) ovlstrcpy(p,dbuf); else ovlstrcpy(p,blist[0]);
      setvar(nbuf,p); *p=0;
      return;
    }
  else goto syntax;
  }
}

/* detrust the module. */
void exec_detrust(char *p)
{
  untrust|=1; *p=0;
}

void exec_warn(char *p)
{
  char *p1,*p2;
  char buf[MAX_FNAME+1];
  WORKING_FILE save;

  if(!outputing) goto end;
  p1=find_word_start(p);p2=find_word_end(p1);
  if(p2<=p1) goto end;
  *p2=0;
  snprintf(buf,sizeof(buf),"wims_warn_%s",p1);
  p2=getvar(buf);
  if(p2==NULL || *p2==0) goto end;
  p2=getvar("module_language");if(p2==NULL) p2="en";
  mkfname(buf,"msg/warn_%s.phtml.%s",p1,p2);
  memmove(&save,&m_file,sizeof(WORKING_FILE));
  if(open_working_file(&m_file,buf)==0) phtml_put(NULL,0);
  memmove(&m_file,&save,sizeof(WORKING_FILE));
  end:
  *p=0; return;
}

/* write an error message. */
void exec_msg(char *p)
{
  char *p1,*p2, buf[64], *l;
  secure_exec();
  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2) {
    *p2=0; p2=find_word_start(p2+1);
  }
  force_setvar("wims_error",p1); force_setvar("wims_error_parm",p2);
  l=getvar("module_language");
  if(l!=NULL && strlen(l)==2) {
    snprintf(buf,sizeof(buf),"msg.phtml.%s",l);
    phtml_put_base(buf,0);
  }
  *p=0;
}

struct distr_cmd distr_cmd[]={
  {"char",      NULL},
  {"charof",    NULL},
  {"chars",     NULL},
  {"charsof",   NULL},
  {"item",      cutitems},
  {"itemof",    cutitems},
  {"items",     cutitems},
  {"itemsof",   cutitems},
  {"line",      cutlines},
  {"lineof",    cutlines},
  {"lines",     cutlines},
  {"linesof",   cutlines},
  {"list",      cutitems},
  {"word",      cutwords},
  {"wordof",    cutwords},
  {"words",     cutwords},
  {"wordsof",   cutwords}
};

int distr_cmd_no=(sizeof(distr_cmd)/sizeof(distr_cmd[0]));

/* distribute a number of lines, items, etc. into a list of vars. */
void exec_distribute(char *p)
{
  int i,k,n;
  char *p1, *p2;
  char bf1[MAX_LINELEN+1],bf2[MAX_LINELEN+1];
  char *names[4096],*vals[4096];
  p1=find_word_start(p); p2=find_word_end(p1);
  if(p2<=p1 || *p2==0) module_error("syntax_error");
  *p2++=0;
  i=search_list(distr_cmd,distr_cmd_no,sizeof(distr_cmd[0]),p1);
  if(i<0) module_error("syntax_error");
  p2=find_word_start(p2); p1=wordchr(p2,"into");
  if(p1==NULL) module_error("syntax_error");
  *p1=0;mystrncpy(bf1,p2,sizeof(bf1));
  p1=find_word_start(p1+strlen("into"));
  mystrncpy(bf2,p1,sizeof(bf2));
  substit(bf1);substit(bf2);
  strip_trailing_spaces(bf1);
  items2words(bf2); n=cutwords(bf2,names,4096);
  if(distr_cmd[i].routine!=NULL) {
    k=distr_cmd[i].routine(bf1,vals,n);
    for(i=0;i<k;i++) setvar(names[i],vals[i]);
    for(;i<n;i++) setvar(names[i],"");
  }
  else {
    char buf[2];
    buf[1]=0;
    for(p1=bf1,i=0;i<n;i++) {
      buf[0]=*p1; if(*p1) p1++;
      setvar(names[i],buf);
    }
  }
}

/* reset variables - the syntax name[10] is allowed*/
void exec_reset(char *p)
{
  char *p1, *p2, *p3, *p4, buf[100];
  int i, n;

  items2words(p);
  for(p1=find_word_start(p); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    substit(p1);
    p3=strchr(p1,'[');
    if (p3 != NULL) {
      *p3++ = 0;
      p4 = find_matching(p3, ']');
      if (p4 == NULL) module_error("nomatching");
      *p4=0;
      n = atoi(p3);
      for (i = 1; i<=n; ++i) {
        snprintf(buf, sizeof(buf),"%s%d",p1,i);
        setvar(buf,"");
      }
    }
    else setvar(p1,"");
  }
}

/* exchange the values of two variables */
void exec_exchange(char *p)
{
  char buf[MAX_LINELEN+1],b1[MAX_LINELEN+1],b2[MAX_LINELEN+1];
  char *p1,*p2,*pb;
  p1=wordchr(p,"and");
  if(p1!=NULL) {
    *p1=0; p2=find_word_start(p1+strlen("and"));
  }
  else {
    p1=strchr(p,',');
    if(p1==NULL) module_error("syntax_error");
    *p1=0; p2=find_word_start(p1+1);
  }
  p1=find_word_start(p);
  mystrncpy(b1,p1,sizeof(b1)); substit(b1); *find_word_end(b1)=0;
  mystrncpy(b2,p2,sizeof(b2)); substit(b2); *find_word_end(b2)=0;
  if(*b1==0 || *b2==0) module_error("syntax_error");
  pb=getvar(b1);if(pb==NULL) pb="";
  mystrncpy(buf,pb,sizeof(buf));
  pb=getvar(b2);if(pb==NULL) pb="";
  setvar(b1,pb); setvar(b2,buf);
}

/**
 * Send a mail
 * p contains 4 lines :
    $To\
    $from\
    $subject\
    $body\
   The first line ($mailheader) must contain the recipient address.
 */
void exec_mailto(char *p)
{
  char *from,*subject,*body,*pp,*charset;
  char mail_content[MAX_LINELEN+1],buf[MAX_LINELEN+1];
  /*int status;
  FILE *mailer;// need stdio.h */

  if(!trusted_module() || is_class_module) return;
  // searches for the first occurrence of \n
  from=strchr(p,'\n'); if(from==NULL) return;
  // delimitate the 'To' string.
  *from++=0; p=find_word_start(p);
  if(*p==0) return;

  subject=strchr(from,'\n'); if(subject==NULL) return;
  // delimitate the 'From' string.
  *subject++=0; from=find_word_start(from);
  if(*from!=0) snprintf(buf,sizeof(buf),"From:%s\n",from);
    else buf[0]=0;
  body=strchr(subject,'\n'); if(body==NULL) return;
  // delimitate the 'Subject' string.
  *body++=0; subject=find_word_start(subject);

  for(pp=subject;*pp;pp++) if(*pp=='"' || *pp=='\n') *pp=' ';

  charset=getvar("wims_main_font");if(charset==NULL) charset="windows-1252";
  snprintf(mail_content,sizeof(mail_content),
          "To:%s\n%sSubject:%s\nContent-Type: text/html;charset=\"%s\"\n\n%s\n",p,buf,subject,charset,body);

  accessfile(mail_content,"w","%s/mail.eml",tmp_dir);
  wrapexec=1;
  // you can add option '-vv' to make sendmail more verbose.
  call_sh("cat %s/mail.eml | /usr/sbin/sendmail -t 2> %s/sendmail.log",tmp_dir,tmp_dir);

  /*mailer = popen("/usr/sbin/sendmail -tvv", "w");
  if (mailer == NULL){module_error("sendmail_popen_error");}
  fprintf(mailer, "To:%s\nSubject:%s\nContent-Type: text/html;charset=\"windows-1252\"\n\n%s",p,subject,body);
  status = pclose(mailer);
  if (status == -1) {
    // Error reported by pclose()
    module_error("sendmail_error");
  }*/

  //call_sh("iconv -f %s -t UTF-8//TRANSLIT %s/mail.eml > %s/mail.eml", charset, tmp_dir, tmp_dir);
  /*call_sh("mail %s -s \" %s \nContent-Type: text/html\" %s <%s/mail.eml; chmod og-rwx %s/mail.eml",
      mail_opt, p1,p,tmp_dir,tmp_dir);*/

  mail_log(p);
  *p=0;
}

/* Generates a user error. Internal and undocumented. */
void exec_usererror(char *p)
{
  if(trusted_module()) user_error(p);
}

/* stop output. */
void exec_directout(char *p)
{
  if(outputing || !trusted_module()) return;
  printf("%s",p);
  noout=1;
}

enum {
  EXEC_IF, EXEC_JUMP, EXEC_ELSE, EXEC_ENDIF, EXEC_EXEC,
  EXEC_WHILE, EXEC_ENDWHILE,
  EXEC_FOR, EXEC_VAR, EXEC_DEBUG, EXEC_DAEMON,
  EXEC_SET, EXEC_DEFAULT, EXEC_COMMENT, EXEC_READ, EXEC_HREF,
  EXEC_INS, EXEC_STRING, EXEC_PEDIA, EXEC_DIR,
  EXEC_TRUST, EXEC_WARN, EXEC_ERROR, EXEC_SQL, EXEC_SCORE,
  EXEC_MAIL, EXEC_OTHER
} EXEC_TYPES;
#define EXEC_SUBST 0x1000
#define EXEC_USECALC 0x2000
#define EXEC_PROCTOO 0x4000
MYFUNCTION exec_routine[]={
  {"!",        EXEC_COMMENT,        exec_comment},
  {"TeXmath",  EXEC_STRING|EXEC_SUBST|EXEC_USECALC,texmath},
  {"add",      EXEC_STRING|EXEC_USECALC,calc_sum},
  {"advance",  EXEC_VAR|EXEC_SUBST,   exec_increase},
  {"append",   EXEC_STRING|EXEC_USECALC,calc_append},
  {"appendfile",EXEC_DIR|EXEC_SUBST,    fileappend},
  {"bound",     EXEC_STRING,       exec_bound},
  {"break",     EXEC_FOR,        exec_break},
  {"call",      EXEC_EXEC|EXEC_SUBST|EXEC_PROCTOO|EXEC_USECALC,    calc_exec},
  {"changeto",  EXEC_READ|EXEC_SUBST,    exec_changeto},
  {"char",      EXEC_STRING|EXEC_USECALC,calc_charof},
  {"chars",     EXEC_STRING|EXEC_USECALC,calc_charof},
  {"checkhost", EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_checkhost},
  {"column",    EXEC_STRING|EXEC_USECALC,calc_columnof},
  {"columns",   EXEC_STRING|EXEC_USECALC,calc_columnof},
  {"comment",   EXEC_COMMENT,        exec_comment},
  {"daemon",    EXEC_DAEMON|EXEC_USECALC|EXEC_SUBST,calc_daemon},
  {"date",      EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_date},
  {"deaccent",  EXEC_STRING|EXEC_USECALC|EXEC_SUBST,deaccent},
  {"debug",     EXEC_DEBUG,        calc_debug},
  {"declosing", EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_declosing},
  {"def",       EXEC_SET,        exec_set},
  {"default",   EXEC_SET,        exec_default},
  {"define",    EXEC_SET,        exec_set},
  {"definitionof", EXEC_SCORE|EXEC_USECALC,calc_defof},
  {"defof",     EXEC_SCORE|EXEC_USECALC,calc_defof},
  {"defread",   EXEC_READ|EXEC_SUBST,    exec_defread},
  {"detag",     EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_detag},
  {"detrust",   EXEC_TRUST,        exec_detrust},
/*      {"dictionary",EXEC_STRING|EXEC_USECALC,calc_dictionary},    */
  {"dir",       EXEC_DIR|EXEC_SUBST|EXEC_USECALC,calc_listfile},
  {"distribute",EXEC_STRING,        exec_distribute},
  {"distrust",  EXEC_TRUST,        exec_detrust},
  {"else",      EXEC_ELSE,        exec_else},
  {"embraced",  EXEC_STRING|EXEC_USECALC,calc_embraced},
  {"encyclo",   EXEC_PEDIA|EXEC_SUBST|EXEC_USECALC,pedia},
  {"encyclopedia",EXEC_PEDIA|EXEC_SUBST|EXEC_USECALC,pedia},
  {"endif",     EXEC_ENDIF,        exec_endif},
  {"endwhile",  EXEC_ENDWHILE,        exec_endwhile},
  {"evalsubst", EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"evalsubstit",EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"evalsubstitute",EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"evaluesubst",EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"evaluesubstit",EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"evaluesubstitute",EXEC_STRING|EXEC_USECALC,calc_evalsubst},
  {"examscore", EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_examscore},
  {"exchange",  EXEC_STRING,        exec_exchange},
  {"exec",      EXEC_EXEC|EXEC_SUBST|EXEC_PROCTOO|EXEC_USECALC,    calc_exec},
  {"execute",   EXEC_EXEC|EXEC_SUBST|EXEC_PROCTOO|EXEC_USECALC,    calc_exec},
  {"exit",      EXEC_JUMP,        exec_exit},
  {"fileappend",EXEC_DIR|EXEC_SUBST,    fileappend},
  {"fileexists",EXEC_STRING|EXEC_USECALC, calc_fileexists},
  {"filelist",  EXEC_DIR|EXEC_SUBST|EXEC_USECALC,calc_listfile},
  {"fileout",   EXEC_HREF|EXEC_SUBST,    exec_getfile},
  {"filewrite", EXEC_DIR|EXEC_SUBST,    filewrite},
  {"filexists",EXEC_STRING|EXEC_USECALC, calc_fileexists},
  {"for",       EXEC_FOR,        exec_for},
  {"form",      EXEC_HREF|EXEC_SUBST,    exec_form},
  {"formbar",   EXEC_HREF,        exec_formbar},
  {"formcheckbox",EXEC_HREF,        exec_formcheckbox},
  {"formend",   EXEC_STRING,    exec_formend},
  {"formradio", EXEC_HREF,        exec_formradio},
  {"formradiobar",EXEC_HREF,        exec_formbar},
  {"formselect",EXEC_HREF,        exec_formselect},
  {"getdef",    EXEC_SCORE|EXEC_USECALC,calc_defof},
  {"getfile",   EXEC_HREF|EXEC_SUBST,    exec_getfile},
  {"getscore",  EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscore},
  {"getscorebest",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscorebest},
  {"getscorelast",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscorelast},
  {"getscorelevel",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscorelevel},
  {"getscoremean",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscoremean},
  {"getscorepercent",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscorepercent},
  {"getscorequality",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscoremean},
  {"getscoreremain",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscoreremain},
  {"getscorerequire",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscorerequire},
  {"getscoretry",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscoretry},
  {"getscoreweight",EXEC_SCORE|EXEC_SUBST|EXEC_USECALC,calc_getscoreweight},
  {"goto",      EXEC_JUMP|EXEC_SUBST,    exec_goto},
  {"header",    EXEC_HREF,        exec_header},
  {"header1",   EXEC_HREF,        exec_header1},
  {"headmathjax",EXEC_HREF,  exec_headmathjax},
  {"headmenu",  EXEC_HREF,        exec_headmenu},
  {"hex",       EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_hex},
  {"homeref",   EXEC_HREF,        exec_homeref},
  {"href",      EXEC_HREF,        exec_href},
  {"html2iso",  EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_html2iso},
  {"htmlbar",   EXEC_HREF,        exec_formbar},
  {"htmlcheckbox",EXEC_HREF,        exec_formcheckbox},
  {"htmlheader",EXEC_HREF,        exec_header},
  {"htmlmath",  EXEC_STRING|EXEC_SUBST|EXEC_USECALC,htmlmath},
  {"htmlradio", EXEC_HREF,        exec_formradio},
  {"htmlradiobar",EXEC_HREF,        exec_formbar},
  {"htmlselect",EXEC_HREF,        exec_formselect},
  {"htmltail",  EXEC_HREF,        exec_tail},
  {"htmltitle", EXEC_HREF,        exec_title},
  {"if",        EXEC_IF,        exec_if},
  {"ifval",     EXEC_IF,        exec_ifval},
  {"ifvalue",   EXEC_IF,        exec_ifval},
  {"imgrename", EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_imgrename},
  {"include",   EXEC_READ|EXEC_SUBST,   exec_read},
  {"increase",  EXEC_VAR|EXEC_SUBST,    exec_increase},
  {"input",     EXEC_READ|EXEC_SUBST,    exec_read},
  {"insdraw",   EXEC_INS|EXEC_SUBST,    exec_insdraw},
  {"insmath",   EXEC_INS,        insmath},
  {"inspaint",  EXEC_INS|EXEC_SUBST,    exec_insdraw},
  {"insplot",   EXEC_INS|EXEC_SUBST,    exec_insplot},
  {"insplot3d", EXEC_INS|EXEC_SUBST,    exec_insplot3d},
  {"instex",    EXEC_INS,        exec_instex},
  {"instexst",  EXEC_INS|EXEC_USECALC,    calc_instexst},
  {"instexstatic",EXEC_INS|EXEC_USECALC,    calc_instexst},
  {"item",        EXEC_STRING|EXEC_USECALC,calc_itemof},
  {"items",     EXEC_STRING|EXEC_USECALC,calc_itemof},
  {"items2lines", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2lines},
  {"items2words",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2words},
  {"itemstolines",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2lines},
  {"itemstowords",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2words},
  {"let",        EXEC_SET,        exec_set},
  {"leveldata",  EXEC_STRING|EXEC_USECALC,calc_leveldata},
  {"levelpoints",EXEC_STRING|EXEC_USECALC,calc_leveldata},
  {"line",       EXEC_STRING|EXEC_USECALC,calc_lineof},
  {"lines",      EXEC_STRING|EXEC_USECALC,calc_lineof},
  {"lines2items",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2items},
  {"lines2list", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2items},
  {"lines2words",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2words},
  {"linestoitems",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2items},
  {"linestolist",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2items},
  {"linestowords",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,lines2words},
  {"list2lines", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2lines},
  {"list2words", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2words},
  {"listfile",   EXEC_DIR|EXEC_SUBST|EXEC_USECALC,calc_listfile},
  {"listfiles",  EXEC_DIR|EXEC_SUBST|EXEC_USECALC,calc_listfile},
  {"listintersect",EXEC_STRING|EXEC_USECALC,calc_listintersect},
  {"listintersection", EXEC_STRING|EXEC_USECALC,calc_listintersect},
  {"listtolines",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2lines},
  {"listtowords",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,items2words},
  {"listunion",  EXEC_STRING|EXEC_USECALC,calc_listunion},
  {"listuniq",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_listuniq},
  {"listunique", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_listuniq},
  {"listvar",    EXEC_STRING|EXEC_SUBST|EXEC_USECALC,mathvarlist},
  {"lookup",     EXEC_STRING|EXEC_USECALC,    calc_lookup},
  {"lower",      EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_tolower},
  {"lowercase",  EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_tolower},
  {"ls",         EXEC_DIR|EXEC_SUBST|EXEC_USECALC,calc_listfile},
  {"mailto",     EXEC_MAIL|EXEC_SUBST,    exec_mailto},
  {"mailurl",    EXEC_MAIL|EXEC_SUBST|EXEC_USECALC,calc_mailurl},
  {"makelist",   EXEC_STRING|EXEC_USECALC,calc_makelist},
  {"math2html",  EXEC_STRING|EXEC_SUBST|EXEC_USECALC,htmlmath},
  {"math2mathml",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,mathmlmath},
  {"math2tex",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,texmath},
  {"mathmlmath", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,mathmlmath},
  {"mathsubst",  EXEC_STRING|EXEC_USECALC,calc_mathsubst},
  {"mathsubstit",EXEC_STRING|EXEC_USECALC,calc_mathsubst},
  {"mathsubstitute",EXEC_STRING|EXEC_USECALC,calc_mathsubst},
  {"mexec",     EXEC_EXEC|EXEC_SUBST,    exec_mexec},
  {"module",    EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_module},
  {"msg",       EXEC_EXEC|EXEC_SUBST,exec_msg},
  {"multiply",  EXEC_STRING|EXEC_USECALC,calc_product},
  {"next",      EXEC_FOR,        exec_next},
  {"nocache",   EXEC_READ,        exec_nocache},
  {"non_empty", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_nonempty},
  {"nonempty",  EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_nonempty},
  {"nospace",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,nospace},
  {"outfile",   EXEC_HREF|EXEC_SUBST,    exec_getfile},
  {"pedia",     EXEC_PEDIA|EXEC_SUBST|EXEC_USECALC,pedia},
  {"perl",      EXEC_EXEC|EXEC_PROCTOO|EXEC_SUBST,exec_perl},
  {"position",  EXEC_STRING|EXEC_USECALC,calc_pos},
  {"positionof",EXEC_STRING|EXEC_USECALC,calc_pos},
  {"positions", EXEC_STRING|EXEC_USECALC,calc_pos},
  {"prod",      EXEC_STRING|EXEC_USECALC,calc_product},
  {"product",   EXEC_STRING|EXEC_USECALC,calc_product},
  {"rawmath",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,rawmath},
  {"rawmatrix", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,rawmatrix},
  {"reaccent",  EXEC_STRING|EXEC_USECALC|EXEC_SUBST,reaccent},
  {"read",      EXEC_READ|EXEC_SUBST,    exec_read},
  {"readdef",   EXEC_READ|EXEC_SUBST,    exec_defread},
  {"readproc",  EXEC_READ|EXEC_SUBST,    exec_readproc},
  {"record",    EXEC_STRING|EXEC_USECALC,calc_recordof},
  {"records",   EXEC_STRING|EXEC_USECALC,calc_recordof},
  {"recursion", EXEC_STRING|EXEC_USECALC,calc_recursion},
  {"reinput",   EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_reinput},
  {"rem",       EXEC_COMMENT,        exec_comment},
  {"remark",    EXEC_COMMENT,        exec_comment},
  {"replace",   EXEC_STRING|EXEC_USECALC,calc_replace},
  {"reset",     EXEC_SET|EXEC_SUBST,    exec_reset},
  {"restart",   EXEC_JUMP|EXEC_SUBST,    exec_restart},
  {"return",    EXEC_JUMP,        exec_exit},
  {"robotrap",  EXEC_HREF|EXEC_SUBST,    exec_robottrap},
  {"robottrap", EXEC_HREF|EXEC_SUBST,    exec_robottrap},
  {"rootof",    EXEC_STRING|EXEC_USECALC,calc_solve},
  {"row",       EXEC_STRING|EXEC_USECALC,calc_rowof},
  {"rows",      EXEC_STRING|EXEC_USECALC,calc_rowof},
  {"rows2lines",EXEC_STRING|EXEC_USECALC|EXEC_SUBST,calc_rows2lines},
  {"run",       EXEC_EXEC|EXEC_SUBST|EXEC_PROCTOO|EXEC_USECALC,    calc_exec},
  {"select",    EXEC_STRING|EXEC_USECALC,calc_select},
  {"set",       EXEC_SET,        exec_set},
  {"setdef",    EXEC_OTHER,        exec_setdef},
  {"setseed",   EXEC_STRING|EXEC_SUBST,        exec_setseed},
  {"sh",        EXEC_EXEC|EXEC_PROCTOO|EXEC_SUBST,exec_sh},
  {"shortout",  EXEC_JUMP|EXEC_SUBST,    exec_directout},
  {"singlespace",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,singlespace},
  {"solve",     EXEC_STRING|EXEC_USECALC,calc_solve},
  {"sort",      EXEC_STRING|EXEC_USECALC, calc_sort},
/*      {"sql",     EXEC_SQL|EXEC_SUBST|EXEC_USECALC, calc_sql}, */
  {"staticinstex",EXEC_INS|EXEC_USECALC,    calc_instexst},
  {"stinstex",  EXEC_INS|EXEC_USECALC,    calc_instexst},
  {"sum",       EXEC_STRING|EXEC_USECALC,calc_sum},
  {"system",    EXEC_EXEC|EXEC_PROCTOO|EXEC_SUBST,exec_sh},
  {"tail",      EXEC_HREF,        exec_tail},
  {"test",      EXEC_DEBUG,        exec_test},
  {"texmath",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,texmath},
  {"text",      EXEC_STRING|EXEC_USECALC,text},
  {"title",     EXEC_HREF,        exec_title},
  {"tohex",     EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_hex},
  {"tolower",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_tolower},
  {"toupper",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_toupper},
  {"translate", EXEC_STRING|EXEC_USECALC,calc_translate},
  {"trim",      EXEC_STRING|EXEC_USECALC,calc_trim},
  {"upper",     EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_toupper},
  {"uppercase", EXEC_STRING|EXEC_SUBST|EXEC_USECALC,calc_toupper},
  {"usererror", EXEC_WARN|EXEC_SUBST,    exec_usererror},
  {"values",    EXEC_STRING|EXEC_USECALC,calc_values},
  {"varlist",   EXEC_STRING|EXEC_SUBST|EXEC_USECALC,mathvarlist},
  {"warn",      EXEC_WARN|EXEC_SUBST,    exec_warn},
  {"warning",   EXEC_WARN|EXEC_SUBST,    exec_warn},
  {"while",     EXEC_WHILE,        exec_while},
  {"whileval",  EXEC_WHILE,        exec_whileval},
  {"whilevalue",EXEC_WHILE,        exec_whileval},
  {"wimsheader",EXEC_HREF,        exec_header},
  {"wimsref",   EXEC_HREF,        exec_homeref},
  {"wimstail",  EXEC_HREF,        exec_tail},
  {"wimstitle", EXEC_HREF,        exec_title},
  {"word",      EXEC_STRING|EXEC_USECALC,calc_wordof},
  {"words",     EXEC_STRING|EXEC_USECALC,calc_wordof},
  {"words2items",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2items},
  {"words2lines",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2lines},
  {"words2list",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2items},
  {"wordstoitems",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2items},
  {"wordstolines",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2lines},
  {"wordstolist",EXEC_STRING|EXEC_SUBST|EXEC_USECALC,words2items},
  {"writefile", EXEC_DIR|EXEC_SUBST,    filewrite},
};
int EXEC_FN_NO=(sizeof(exec_routine)/sizeof(exec_routine[0]));

/* internal: to skip the content of a false if/while. */
static void _skip_contents(int isif)
{
  char buf[MAX_NAMELEN+8], *p1;
  int i,j,loop;
  loop=0;
  while(m_file.linepointer<m_file.linecnt) {
    j=m_file.linepointer;
    if((m_file.lines[j].isstart&2)==0) {
      m_file.linepointer++; continue;
    }
    i=m_file.lines[j].execcode;
    if(i<0) {
      if(wgetline(buf,MAX_NAMELEN+4,&m_file)==EOF) return;
      p1=buf+1; if(*p1!='i' && *p1!='e' && *p1!='w') continue;
      *find_word_end(p1)=0;
      i=search_list(exec_routine,EXEC_FN_NO,sizeof(exec_routine[0]),p1);
      if(i>=0) m_file.lines[j].execcode=i;
    }
    else m_file.linepointer++;
    if(i<0) continue;
    switch(exec_routine[i].tag & 0xffff) {
      case EXEC_WHILE:
        if(!isif) loop++;
        break;
      case EXEC_IF:
        if(isif) loop++;
        break;
      case EXEC_ELSE: {
        if(!isif) break;
        if(loop<=0) return; else break;
      }
      case EXEC_ENDIF: {
        if(!isif) break;
        if(loop>0) {
          loop--; break;
        }
        else return;
      }
      case EXEC_ENDWHILE: {
        if(isif) break;
        if(loop>0) {
          loop--; break;
        }
        else return;
      }
      default: break;
    }
  }
}

/* Execute a command defined by !. Returns 0 if OK. */
void exec_main(char *p)
{
  int i,j;
  char *pp;
  char tbuf2[MAX_LINELEN+1];

  pp=find_word_end(p);
  if(*pp!=0) {
    *(pp++)=0; pp=find_word_start(pp);
  }
  i=m_file.lines[m_file.l].execcode;
  if(i<0) {
    i=search_list(exec_routine,EXEC_FN_NO,sizeof(exec_routine[0]),p);
    m_file.lines[m_file.l].execcode=i;
  }
  if(i<0) {
    setvar(error_data_string,p); module_error("bad_cmd");
  }
/* called from !readdef, no right other than set; bail out */
  execnt++;
  if((untrust&4)!=0 && (j=(exec_routine[i].tag&0xffff))!=EXEC_SET) {
    tbuf2[0]=0; exec_exit(tbuf2);
  }
  ovlstrcpy(tbuf2,pp); j=exec_routine[i].tag;
  if(j&EXEC_SUBST) substit(tbuf2);
  if(j&EXEC_USECALC) {
    if(!outputing && (j&EXEC_PROCTOO)==0) return;
    exec_routine[i].routine(tbuf2); if(outputing) output0(tbuf2);
  }
  else exec_routine[i].routine(tbuf2);
  return;
}
