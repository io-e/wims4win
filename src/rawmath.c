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

/* internal variable */
int rawmath_easy=0;

struct hmname hmname[]={
      {"CC",  "", ""},
      {"Delta", "", ""},
      {"Gamma","", ""},
      {"Inf",  "$(m_infty)","\\infty"},
      {"Lambda","", ""},
      {"NN",  "", ""},
      {"Omega",  "", ""},
      {"Phi",  "", ""},
      {"Pi",  "", ""},
      {"Psi",  "", ""},
      {"QQ",  "",""},
      {"RR",  "",""},
      {"Sigma",  "",""},
      {"Xi",  "",""},
      {"ZZ",  "",""},
      {"alpha",  "", ""},
      {"beta",  "", ""},
      {"cap",  "",""},
      {"chi",  "",""},
      {"cup",  "",""},
      {"delta",  "",""},
      {"div",  "&divide;", "&divide;"},
      {"divide","&divide;","&divide;"},
      {"epsilon","$(m_varepsilon)","\\varepsilon"},
      {"eta",  "",""},
      {"exist",  "$(m_exists)","\\exists"},
      {"exists","", ""},
      {"forall","",""},
      {"gamma",  "",""},
      {"in",  "",""},
      {"inf",  "$(m_infty)","\\infty"},
      {"infinity","$(m_infty)","\\infty"},
      {"infty",  "",""},
      {"intersect","$(m_cap)", "\\cap"},
      {"intersection","$(m_cap)", "\\cap"},
      {"iota",  "",""},
      {"kappa",  "",""},
      {"lambda","", ""},
      {"mu",  "",""},
      {"nabla",  "",""},
      {"neq",  "",""},
      {"nu",  "",""},
      {"omega",  "",""},
      {"pi",  "",""},
      {"pm",  "",""},
      {"psi",  "",""},
      {"rho",  "",""},
      {"sigma",  "",""},
      {"subset","", ""},
      {"subseteq","",""},
      {"tau",  "",""},
      {"theta",  "",""},
      {"times",  "&times;", "\\times"},
      {"union",  "$(m_cup)", "\\cup"},
      {"varepsilon","",""},
      {"varphi","", ""},
      {"x",  "","x"},
      {"xi","",""},
      {"y",  "","y"},
      {"z",  "","z"},
      {"zeta","",""},
};
int hmname_no=(sizeof(hmname)/sizeof(hmname[0]));

enum {RM_UNKNOWN, RM_FN, RM_VAR, RM_PREFIX};

struct mathname mathname[]={
      {"Arc",     RM_PREFIX, "arc"},
      {"Arg",     RM_PREFIX, "arg"},
      {"Ci",      RM_FN,        ""},
      {"E",       RM_VAR,       ""},
      {"Euler",   RM_VAR,       ""},
      {"I",       RM_VAR,       ""},
      {"Int",     RM_FN,        ""},
      {"PI",      RM_VAR,       ""},
      {"Pi",      RM_VAR,       ""},
      {"Prod",    RM_FN,        ""},
      {"Si",      RM_FN,        ""},
      {"Sum",     RM_FN,        ""},
      {"arc",     RM_PREFIX,    ""},
      {"arg",     RM_PREFIX,    ""},
      {"binomial",RM_FN,        ""},
      {"diff",    RM_FN,        ""},
      {"e",       RM_VAR,       ""},
      {"erf",     RM_FN,        ""},
      {"euler",   RM_VAR,       ""},
      {"i",       RM_VAR,       ""},
      {"infinity",RM_VAR,       ""},
      {"int",     RM_FN,        ""},
      {"integrate",RM_FN,       ""},
      {"neq",     RM_VAR,       ""},
      {"pi",      RM_VAR,       ""},
      {"prod",    RM_FN,        ""},
      {"product", RM_FN,        ""},
      {"psi",     RM_FN,        ""},
      {"sum",     RM_FN,        ""},
      {"theta",   RM_FN,        ""},
      {"x",       RM_VAR,       ""},
      {"y",       RM_VAR,       ""},
      {"z",       RM_VAR,       ""},
      {"zeta",    RM_FN,        ""},
};
int mathname_no=(sizeof(mathname)/sizeof(mathname[0]));
char rm_vbuf[MAX_LINELEN+1],rm_fbuf[MAX_LINELEN+1];
char *rm_uservar[MAX_LINELEN+1],*rm_userfn[MAX_LINELEN+1];
int  rm_uservars,rm_userfns;

/* add user-defined variables and function names,
 * internal, only called by rawmath().
 */
void getuservar(void)
{
  char *p1, *p2, *p;
  rm_uservars=rm_userfns=0;
  p=getvar("wims_rawmath_variables");
  if(p!=NULL && *p!=0) {
    ovlstrcpy(rm_vbuf,p);
    for(p=rm_vbuf;*p;p++) if(*p==',') *p=' ';
    for(p1=find_word_start(rm_vbuf);*p1;p1=find_word_start(p2)) {
      rm_uservar[rm_uservars++]=p1;
      p2=find_word_end(p1);
      if(*p2!=0) *(p2++)=0;
    }
  }
  p=getvar("wims_rawmath_functions");
  if(p!=NULL && *p!=0) {
    ovlstrcpy(rm_fbuf,p);
    for(p=rm_fbuf;*p;p++) if(*p==',') *p=' ';
    for(p1=find_word_start(rm_fbuf);*p1;p1=find_word_start(p2)) {
      rm_userfn[rm_userfns++]=p1;
      p2=find_word_end(p1);
      if(*p2!=0) *(p2++)=0;
    }
  }
}

/* Try to split a word into recognizable variables */
char *mathname_split(char *p)
{
  int c,i,j,type;

  c=0;
  beg: for(i=get_evalcnt()-1;
  i>=0 && strncmp(p,get_evalname(i),strlen(get_evalname(i)))!=0;
  i--);
  if(i>=0 && get_evaltype(i)>0) {
    type=RM_FN;
    j=strlen(get_evalname(i));
    gotit:
    if(!*(p+j)) return p;
    if(myisdigit(*(p+j)) && type!=RM_FN) return NULL;
    if(!c) {string_modify(p,p+j,p+j," "); p+=j+1;}
    else p+=j;
    c++; goto beg;
  }
  for(i=mathname_no-1;
      i>=0 &&
      (strncmp(p,mathname[i].name,strlen(mathname[i].name))!=0
      || mathname[i].style==RM_PREFIX);
    i--);
  if(i>=0) {
    type=mathname[i].style;
    j=strlen(mathname[i].name);
    goto gotit;
  }
  for(i=0;i<rm_uservars &&
    strncmp(rm_uservar[i],p,strlen(rm_uservar[i]));i++);
  if(i<rm_uservars) {
    type=RM_VAR;
    j=strlen(rm_uservar[i]); goto gotit;
  }
  for(i=0;i<rm_userfns &&
      strncmp(p,rm_userfn[i],strlen(rm_userfn[i]));i++);
  if(i<rm_userfns) {
    type=RM_FN;
    j=strlen(rm_userfn[i]); goto gotit;
  }
  return NULL;
}

int __replace_badchar (char *p, char *old, char *new)
{
  int cnt = 0;
  char *p1 ;
  while((p1=strstr(p,old))!=NULL) {
    string_modify(p,p1,p1+strlen(old),"%s",new);
    cnt++;
  }
  return cnt ;
}

/* translate |x| into abs(x)*/
int __replace_abs ( char *p )
{
  char *p1, *p2 ;
  while((p1=strchr(p,'|'))!=NULL) {
    p2=find_matching(p1+1,'|');
    if(p2==NULL) { return 1; break;} /* error; drop it. */
    *p2=')'; string_modify(p,p1,p1+1,"abs(");
  }
  return 0;
}

/* signs: translate ++, +-, -+, ... into one sign. */
void __replace_plusminus ( char *p )
{
  char *p1, *p2;
  for(p1=p;*p1!=0;p1++) {
    int sign, redundant;
    if(*p1!='+' && *p1!='-') continue;
    if(*p1=='+') sign=1; else sign=-1;
    redundant=0;
    for(p2=find_word_start(p1+1);*p2=='+' || *p2=='-';
    p2=find_word_start(p2+1)) {
      if(*p2=='-') sign*=-1;
      redundant=1;
    }
    if(redundant && *p2!='>' && strncmp(p2,"&gt;",4)!=0) {
      if(sign==1) *p1='+'; else *p1='-';
      ovlstrcpy(p1+1,p2);
    }
  }
}

/* dangling decimal points
 * 4. --> 4.0  4.x -> 4.0x
 * .5 -> 0.5
 * another treatment is done in insmath (will replace .. by , )
 */
void __treat_decimal(char *p)
{
  char *p1 ;
  for(p1=strchr(p,'.'); p1!=NULL; p1=strchr(p1+1,'.')) {
        /* multiple .. is conserved */
    if(*(p1+1)=='.') {
      do p1++; while(*p1=='.'); continue;
    }
    if(p1>p && myisdigit(*(p1-1)) && myisdigit(*(p1+1))) continue;
        /* Non-digit dangling '.' is removed */
    if((p1<=p || !myisdigit(*(p1-1))) && !myisdigit(*(p1+1))) {
      ovlstrcpy(p1,p1+1); p1--; continue;
    }
    if(p1==p || !myisdigit(*(p1-1))) { // nondigit.digit
      string_modify(p,p1,p1,"0"); p1++; //add zero before point
    }
    if(!myisdigit(*(p1+1))) string_modify(p,p1+1,p1+1,"0"); //add zero after point
  }
}

/* replace new-lines, tabs, " */
void __replace_space(char *p)
{
  char *p1 ;
  for(p1=p;*p1!=0; p1++) {
    if(*p1=='\\' || isspace(*p1)) *p1=' ';// replace \ and all spaces by a simple space -
    if(*p1=='\"') { string_modify(p,p1,p1+1,"''"); p1++;} // replace " by ''
  }
}

/* Error-tolerant raw math translation routine
 * Translate error-laden raw math into machine-understandable form.
 * do nothing if there is some { or \\
 */
void rawmath(char *p)
{
  char *p1, *p2, *p3, *p4;
  char warnbuf[1024];
  int ambiguous=0,unknown=0,flatpower=0,badprec=0,unmatch=0;// for warning

/* looks like a TeX source : do nothing */
  if( (strchr(p,'\\')!=NULL || strchr(p,'{')!=NULL)) return;
  if(strchr(p,'^')==NULL) flatpower=-1;
  if(strlen(p)>=MAX_LINELEN) {*p=0; return;}
  p1=find_word_start(p);if(*p1==0) return;
  while(*p1=='+') p1++;
  if(p1>p) ovlstrcpy(p,p1);
  (void)__replace_badchar(p,"**", "^");
  (void)__replace_badchar(p,"\xa0", " ");

/*if (__replace_badchar(p,"²", "^2 ")) flatpower=1;
  if (__replace_badchar(p,"³", "^3 ")) flatpower=1;
*/
  if (__replace_badchar(p,"\xB2", "^2 ")) flatpower=1;
  if (__replace_badchar(p,"\xB3", "^3 ")) flatpower=1;
  unmatch=__replace_abs(p);
  __replace_plusminus(p) ;
  __replace_space(p);
  __treat_decimal(p);
  if (rawmath_easy) return;

/* Principal translation: justapositions to multiplications */
  if(strstr(p,"^1/")!=NULL) badprec=1;
  getuservar();
  for(p1=p;*p1;p1++) {
    if(!isalnum(*p1) && *p1!=')' && *p1!=']') continue;
    if(*p1==')' || *p1==']') {
      p2=find_word_start(++p1);
      add_star:
      if(isalnum(*p2) || *p2=='(' || *p2=='[') {
        if(*p2=='(' && *p1==')') ambiguous=1;
        if(p2>p1) *p1='*';
        else string_modify(p,p1,p1,"*");
      }
      p1--;continue;
    }
    p2=find_mathvar_end(p1); p3=find_word_start(p2);
    if(myisdigit(*p1)) {
      p1=p2; p2=p3; goto add_star;
    }
    else {
      char buf[MAX_LINELEN+1];
      int i;
      if(p2-p2>30) goto ambig;
      memcpy(buf,p1,p2-p1);buf[p2-p1]=0;
      i=search_evaltab(buf);
      if(i>=0 && get_evaltype(i)>0) {
        fnname1:
        p1=p2;p2=p3;
/*fnname:*/
        if(*p2 && *p2!='(' && *p2!='*' && *p2!='/') {
          char hatbuf[MAX_LINELEN+1];
          hatbuf[0]=')'; hatbuf[1]=0;
          if(*p2=='^') {
            p3=p2+1;while(*p3==' ' || *p3=='+' || *p3=='-') p3++;
            if(*p3=='(') {
              p3=find_matching(p3+1,')');
              if(p3==NULL) {unmatch=1; p3=p+strlen(p);}
              else p3++;
            }
            else p3=find_mathvar_end(p3);
            memmove(hatbuf+1,p2,p3-p2);hatbuf[p3-p2+1]=0;
            ovlstrcpy(p2,p3);
            while(*p2==' ') p2++;
            if(*p2=='*' || *p2=='/') {
              p1--;continue;
            }
            if(*p2=='(') {
              p3=find_matching(p2+1,')');
              if(p3==NULL) {unmatch=1; p3=p+strlen(p);}
              else p3++;
              string_modify(p,p3,p3,"%s",hatbuf+1);
              goto dd2;
            }
          }
          p3=p2;if(*p3=='+' || *p3=='-') p3++;
          while(isalnum(*p3) || *p3=='*' || *p3=='/' || *p3=='.')
            p3++;
          for(p4=p2; p4<p3 && !isalnum(*p4); p4++);
          if(p4>=p3) {
            if(hatbuf[1]) string_modify(p,p2,p2,"%s",hatbuf+1);
          }
          else {
            string_modify(p,p3,p3,"%s",hatbuf);
            if(p1==p2) string_modify(p,p2,p2,"(");
            else *p1='(';
          }
          dd2:
          ambiguous=1;
        }
        p1--;continue;
      }
      i=search_list(mathname,mathname_no,sizeof(mathname[0]),buf);
      if(i>=0) {
        if(mathname[i].replace[0]!=0) {
          string_modify(p,p1,p2,mathname[i].replace);
          p2=p1+strlen(mathname[i].replace);
          p3=find_word_start(p2);
        }
        switch(mathname[i].style) {
          case RM_FN:
            goto fnname1;

          case RM_VAR:
            p1=p2;p2=p3; goto add_star;

          case RM_PREFIX:
            if(*p3!='c' && *p3!='s' && *p3!='t' &&
              *p3!='C' && *p3!='S' && *p3!='T') break;
            ambiguous=1;
            ovlstrcpy(p2,p3); p1--; continue;
        }
      }
      i=search_list(hmname,hmname_no,sizeof(hmname[0]),buf);
      if(i>=0) {
        p1=p2; p2=p3; goto add_star;
      }
      for(i=0;i<rm_uservars && strcmp(buf,rm_uservar[i]);i++);
      if(i<rm_uservars) {
          p1=p2;p2=p3;goto add_star;
      }
      for(i=0;i<rm_userfns && strcmp(buf,rm_userfn[i]);i++);
      if(i<rm_userfns) goto fnname1;
      if(p2-p1>8) goto ambig;
      if(mathname_split(buf)!=NULL) {
        ambiguous=1;
        string_modify(p,p1,p2,"%s",buf);
        p1--; continue;
      }
/* unknown name */
      ambig: p1=p2;p2=p3;
      if(strlen(buf)>1) {
        for(p3=buf;*p3!=0 && !myisdigit(*p3);p3++);
        if(*p3!=0 && flatpower!=0) flatpower=1;
        else {
          unknown=1;
          force_setvar("wims_warn_rawmath_parm",buf);
        }
      }
      else {
        if(*p2=='(') ambiguous=1;
      }
      if(isalnum(*p2)) {
        if(p2>p1) *p1='*';
        else string_modify(p,p1,p1,"*");
      }
      p1--;continue;
    }
  }
  warnbuf[0]=0;
  if(ambiguous) strcat(warnbuf," ambiguous");
  if(unknown) strcat(warnbuf," unknown");
  if(flatpower>0) strcat(warnbuf," flatpower");
  if(badprec>0) strcat(warnbuf," badprec");
  if(unmatch>0) strcat(warnbuf," unmatched_parentheses");
  if(warnbuf[0]) {
    char buf[MAX_LINELEN+1],*p;
    p=getvar("wims_warn_rawmath");
    if(p!=NULL && *p!=0) {
      snprintf(buf,sizeof(buf),"%s %s",p,warnbuf);
      force_setvar("wims_warn_rawmath",buf);
    }
    else force_setvar("wims_warn_rawmath",warnbuf);
  }
}

 /* replace < and > by html code if htmlmath_gtlt=yes
  * is only used in deduc - not documented
  */
void __replace_htmlmath_gtlt (char *p)
{
  char *pp;
  char *p1=getvar("htmlmath_gtlt");
  if(p1!=NULL && strcmp(p1,"yes")==0) {
    for(pp=strchr(p,'<'); pp!=NULL; pp=strchr(pp+1,'<'))
      string_modify(p,pp,pp+1,"&lt;");
    for(pp=strchr(p,'>'); pp!=NULL; pp=strchr(pp+1,'>'))
      string_modify(p,pp,pp+1,"&gt;");
  }
}

/* exponents or indices :
 *  all digits or + or - following a ^ or _ are considered as in exponent/subscript
 *  expression with ( ) following a ^ or _ are  considered as in exponent/subscript
 *  the parenthesis are suppressed except in case of exponent and only digits.
 *  if int n != 0, use html code, else use tex code
 */
void __replace_exponent(char *p, int n)
{
  char *p1;
  char *SUPBEG, *SUPEND;
  if (n) { SUPBEG = "<sup>"; SUPEND = "</sup>";}
  else { SUPBEG = "^{"; SUPEND = "}";}

  for(p1=strchr(p,'^');p1!=NULL;p1=strchr(p1+1,'^')) {
    char *p2, *p3, *pp;
    char c;
    p3 = p2 = find_word_start(p1+1);
    if(*p2=='+' || *p2=='-') p2++;
    p2 = find_word_start(p2);
/* add '}' to recognized parenthesis in exponent
 * !mathmlmath 2 \cdot x^{3} will now produce correct exponent...
 * !mathmlmath should convert LaTeX input into correct MathML
 */
    if(*p2=='(' || *p2 == '{') { /* ^[+-]( */
      if(*p2 == '('){ p2 = find_matching(p2+1,')');}else { if(*p2 == '{'){ p2 = find_matching(p2+1,'}');}}
            /* no matching ')' : p2 = end of line; otherwise just after ')' */
      if (p2==NULL) p2=p+strlen(p); else p2++;
            /* ^( followed by any number of digits/letters, up to p2
             * [FIXME: no sign?] */
            /* do we remove parentheses containing exponent group ? */
      if (*p3=='(') for(pp=p3+1;pp<p2-1;pp++) {
        if(!isalnum(*pp)) {
                    /* not a digit/letter. Remove (). */
          p3++;*(p2-1)=0;break;
        }
                /* x^(2), parentheses not removed */
      }
/* p3: start of word after ^
 * matching parentheses from exponent group. : f^(4) 4-derivative
 * don't  ignore / remove a leading + sign in exponent group : Cu^+
 */
    }
    else { /* ^[+-] */
      char *ptt=p2;
      p2=find_word_start(find_mathvar_end(p2));
      if(*p2=='(' && isalpha(*ptt)) {
              /* ^[+-]var( */
        char *p2t;
        p2t=find_matching(p2+1,')'); if(p2t!=NULL) p2=p2t+1;
              /* FIXME: what if no matching ) ? */
      }
/* ^[+-]var(...): p2 points after closing ')'
 * FIXME: I don't think this 'else' branch is sensible. One
 * should NOT accept x^b(c+1) as meaning x^[b(c+1)]. I would
 * remove it altogether.
 */
    }
/* p1 points at ^ before exponent group
 * p2 points at end of exponent group
 * p3 = exponent group (sometimes stripped, without parentheses)
 *
 * truncate string p at p2 [ c = char deleted by truncation ]
 */
    c = *p2;if(c!=0) *p2++=0;
        /* replace ^<exponent group>. Add back missing character 'c' */
    string_modify(p,p1,p2, "%s%s%s%c",SUPBEG,p3,SUPEND,c);
  }
}

/* if int n != 0, use html code, else use tex code */
void __replace_subscript(char *p, int n)
{
   char *p1, *p2;
   char *SUBBEG, *SUBEND;
   if (n) {SUBBEG = "<sub>"; SUBEND = "</sub>";}
   else {SUBBEG = "_{"; SUBEND = "}";}
   for(p1=strchr(p,'_');p1!=NULL;p1=strchr(p1+1,'_')) {
     char buff[256];
     p2=p1+1;
     if(*p2=='(') p2=find_matching(p2+1,')');
     else p2=find_mathvar_end(p2);
     if(p2==NULL || p2>p1+64) continue;
     if(*(p1+1)=='(') p2++;
     memmove(buff,p1+1,p2-p1-1); buff[p2-p1-1]=0;
     strip_enclosing_par(buff);
     string_modify(p,p1,p2,"%s%s%s",SUBBEG,buff,SUBEND);}
}

/* get rid of 1*.. ..*1  exemple : 1 * x, x/1 */
void __replace_getrid1 (char *p)
{
  char *p1, *p2, *p3 ;
  for(p1=p;*p1;p1++) {
    if(*p1!='1') continue;
    if(myisdigit(*(p1+1)) || *(p1+1)=='.' ||
      (p1>p && (isalnum(*(p1-1)) || *(p1-1)=='.')) ) continue;
    p2=find_word_start(p1+1);
    if(p1>p+2 && (*(p1-1)=='-' || *(p1-1)=='+')) {
      for(p3=p1-2; p3>p && isspace(*p3); p3--);
      if(p3>p+1 && (*p3=='E' || *p3=='e')) { /* ??? */
        p3--; while(p3>p && isspace(*p3)) p3--;
        if(myisdigit(*p3) || *p3=='.') continue;
      }
    }
    if(p1==p) p3="+";
    else for(p3=p1-1;p3>p && isspace(*p3);p3--);
    if(*p2=='*' && *p3!='/') {/* delete 1 if 1* or /1 */
      ovlstrcpy(p1,p2+1);continue;
    }
    if(isalpha(*p2) && *p3!='/') {
      ovlstrcpy(p1,p2);continue;
    }
    if(*p3=='/' && *p2!='<') ovlstrcpy(p3,p2);

  }
}

/* get rid of '*' */
void __replace_getridstar (char *p)
{
  char *p1 ;
  for(p1=strchr(p,'*');p1!=NULL;p1=strchr(p1+1,'*')) {
    char *pq;
    pq=find_word_start(p1+1);
    if(myisdigit(*pq)) {
      string_modify(p,p1,pq,"&times;");
      continue;
    }
    if(p1>p && (isalpha(*(p1-1)) || *(p1-1)==')' || *(p1-1)=='>')
       && (isalnum(*pq) || *pq=='$')) *p1=' ';
    else {
        ovlstrcpy(p1,p1+1);p1--;
    }
  }
}

/* <=, >=, ->, =>, <=>, !=, <-> <-->, <-
 * if int n != 0, use html code, else use tex code
 */

void __replace_arrow ( char *p, int n)
{
  char *p1, *p2, *m_prefix;
  if (n) m_prefix="$m_"; else m_prefix="\\";

  for(p1=strstr(p,"&lt;="); p1!=NULL; p1=strstr(p1+1,"&lt;=")) {
    if(*(p1+5)!='&' && *(p1+5)!='=')
      string_modify(p,p1,p1+5, "%sle",m_prefix);
    else {
      for(p2=p1+5; *p2=='='; p2++);
      if(strncmp(p2,"&gt;",4)==0) {
        if(p2>p1+5) { string_modify(p,p1,p2+4,"%sLongleftrightarrow",m_prefix);}
        else { string_modify(p,p1,p2+4,"%sLeftrightarrow",m_prefix);}
      }
      else { string_modify(p,p1,p2,"%sLongleftarrow",m_prefix);}
    }
  }
  for(p1=strstr(p,"&gt;="); p1!=NULL; p1=strstr(p1+1,"&gt;=")) {
    if(*(p1+5)!='=') { string_modify(p,p1,p1+5,"%sge",m_prefix);}
  }
  for(p1=strstr(p,"-&gt;"); p1; p1=strstr(p1+1,"-&gt;")) {
    for(p2=p1; p2>p && *(p2-1)=='-'; p2--);
    if(p2>p && *(p2-1)==';') continue;
    if(p2<p1) { string_modify(p,p2,p1+5,"%slongrightarrow",m_prefix);}
    else {  string_modify(p,p1,p1+5,"%srightarrow",m_prefix);}
  }
  for(p1=strstr(p,"=&gt;"); p1; p1=strstr(p1+1,"=&gt;")) {
    for(p2=p1; p2>p && *(p2-1)=='='; p2--);
    if(p2>p && *(p2-1)==';') continue;
    if(p2<p1) { string_modify(p,p2,p1+5,"%sLongrightarrow",m_prefix);}
    else { string_modify(p,p1,p1+5,"%sRightarrow",m_prefix) ;}
  }
  /* <->, <-->, <- */
  /* <-- not implemented because -- is replaced by + elsewhere */
  for(p1=strstr(p,"&lt;-"); p1!=NULL; p1=strstr(p1+1,"&lt;-")) {
    for(p2=p1+5; *p2=='-'; p2++);
    if(strncmp(p2,"&gt;",4)==0)
      if ((p2-p1)%2)
	string_modify(p,p1,p2+4,(p2>p1+5) ? "%slongleftrightarrow" : "%sleftrightarrow",m_prefix);
      else
	string_modify(p,p1,p2+4,(p2>p1+6) ? "%sleftrightharpoons" : "%sleftrightharpoons",m_prefix);
    else
      string_modify(p,p1,p2,(p2>p1+5) ? "%slongleftarrow " : "%sleftarrow ",m_prefix);
  }

  /* Not equal */
  for(p1=strstr(p,"!="); p1; p1=strstr(p1+1,"!=")) {
    if(p1>p && !isspace(*(p1-1))) continue;
    string_modify(p,p1,p1+2,"%sneq",m_prefix);
  }
}

/* why <tt> is used sometimes ? replace single characters by italics one
 * is it useful in mathml ?
 */
void __replace_italics (char *p, int n)
{
  char *p1, *p2, *p3, pbuf[16];
  char *ITBEG, *ITEND, *ITtBEG, *ITtEND;
   if (n) {
     ITBEG = "<i>";ITtBEG = "<i><tt>";
     ITEND = "</i>";ITtEND = "</tt></i>";
  } else {;
     ITBEG = "" ; ITtBEG = "";
     ITEND = ""; ITtEND = "";
  }

  for(p1=p;*p1;p1++) {
    if(*p1=='<') {
      p1=strchr(p1,'>');
      if(p1==NULL) break; //recognize an html tag < >
      else continue;
    }
    if(*p1=='=' && *(p1+1)=='-') {
        string_modify(p,p1+1,p1+1," "); p1+=2; continue;
    }
    if(*p1=='\'') {
      for(p2=p1+1;*p2=='\'';p2++);
      memmove(pbuf,p1,p2-p1); pbuf[p2-p1]=0;
      string_modify(p,p1,p2,"%s%s%s",ITtBEG,pbuf,ITtEND);
      p1=p2+strlen(ITtBEG)+strlen(ITtEND)-1;
      continue;
    }
    if(!isalpha(*p1)) continue;
/* unique letter.*/
    for(p2=p1+1;isalpha(*p2);p2++);
    p3=find_word_start(p2);
    if(p2>p1+5 ||
       (p2>p1+1 && (*p3==';' || *p3=='(' || myisdigit(*p2))))
      {p1=p2-1; continue;}
    if(strncasecmp(p3,"</i>",strlen("</i>"))==0) continue;
    memmove(pbuf,p1,p2-p1); pbuf[p2-p1]=0;
    string_modify(p,p1,p2,"%s%s%s",ITBEG,pbuf,ITEND);
    p1=p2+strlen(ITBEG)+strlen(ITEND)-1;
  }
}

/* float (1.2 E-03) : 3E+021 -> 3 × 10^{21} - 3E-21 -> 3 × 10^{-21}
 * or replace variable name (alpha)
 * if int n != 0, use html code, else use tex code
 */
void __replace_mathvar(char *p,int n)
{
  char *p1, *p2, *p3;
  char *EXPBEG, *EXPEND, *EXPBEGMINUS, *SUBBEG, *SUBEND, *m_prefix;

  if ( n ) {
    EXPBEG = " &times; 10<sup>";
    EXPBEGMINUS = " &times; 10<sup>-";
    EXPEND = "</sup>";
    SUBBEG = "<sub>";
    SUBEND = "</sub>";
    m_prefix="$m_";
  } else {
    EXPBEG = " \\times 10^{" ;
    EXPBEGMINUS = " \\times 10^{-" ;
    EXPEND = "}";
    SUBBEG = "_{";
    SUBEND = "}";
    m_prefix="\\";
  }
  for (p1=find_mathvar_start(p);*p1!=0;p1=find_mathvar_start(p2)) {
    char buf[MAX_LINELEN+1];
/* if the variable is preceded by \ do nothing
 * in fact this should not arrive
 */
    if (p1>p && *(p1-1) == '\\' ) break ;
    p2 = find_mathvar_end(p1);
    if (p1 == p2) break;
    memmove(buf,p1,p2-p1);buf[p2-p1]=0;

    if(myisdigit(buf[0])) {
/* number : 3E+021 -> 3 x 10^{21} - 3E-21 -> 3 x 10^{-21} */
      int k = 1, minus = 0;

/* see putnumber in texmath.c*/
      if( (p3 = strpbrk(buf, "Ee")) == NULL) continue;
      p1 += p3-buf;
            //count the number of 0, +, -
      if (*(p1+k) == '-') { minus = 1; k++; }
      while(*(p1+k)=='0' || *(p1+k)=='+') k++;

      string_modify(p,p1,p1+k, minus? EXPBEGMINUS: EXPBEG);
      p2 += strlen(minus? EXPBEGMINUS: EXPBEG)-k;
      string_modify(p,p2,p2, EXPEND);
      p2 += strlen(EXPEND);
    }
    else {
/* alphabetic name, replace greek letters and symbols in hmname
 * not done in texmath.c
 */
      int i = search_list(hmname,hmname_no,sizeof(hmname[0]),buf);
      char *n, *r;
      if(i<0) { /* not in list */
        int k = strlen(buf)-1;
        if(myisdigit(buf[k])) {
          while(k>0 && myisdigit(buf[k-1])) k--;
          string_modify(buf,buf+k,buf+k,SUBBEG);
          string_modify(p,p1,p2,"%s%s",buf,SUBEND);
          p2+=strlen(SUBBEG)+strlen(SUBEND);
        }
        continue;
      }
      n = hmname[i].name;
      r = mathalign_base < 2? hmname[i].replace: hmname[i].replacem;
      if(r[0]==0) { /* replace = "" */
        string_modify(p,p1,p2,"%s%s",m_prefix, n);
        p2 = p1+strlen(n)+strlen(m_prefix);
      } else {
        string_modify(p,p1,p2,"%s",r);
        p2 = p1+strlen(r);
      }
    }
  }
}

/* translate raw math expression coming from calculators into best html way
 * if int n != 0, use html code, else use tex code
 */
void __htmlmath(char *p,int n)
{
  if(!rawmath_easy) { rawmath_easy=1; rawmath(p); rawmath_easy=0;}
  __replace_htmlmath_gtlt(p); //only used in deductio
  __replace_exponent(p,n);
  __replace_subscript(p,n);
  __replace_getrid1(p);
  __replace_mathvar(p,n);
  __replace_getridstar(p);
  __replace_arrow(p,n);
/* Now make substitutions */
  substit(p);
/* Make single names italic - done automatically by mathml */
   __replace_italics(p,n);
   strip_trailing_spaces(p);
}

void htmlmath(char *p)
{
 __htmlmath(p,1) ;
}

/* if mathml is closed, it will be just htmlmath*/

void mathmlmath(char *p)
{
    /*
     if force_mathml variable is set to "yes", do not (never) use 'htmlmath'
     in output, so command: !mathmlmath some_LaTeX_string will produce mathml
     (mathml \input inputfields may be included)
    */
  if( strcmp( getvar("force_mathml"),"yes") == 0 ){ mathalign_base = 2;mathml(p,1);return; }
  if (mathalign_base == 2) { __htmlmath(p,0) ; mathml(p,1);} else { __htmlmath(p,1) ;}
}
