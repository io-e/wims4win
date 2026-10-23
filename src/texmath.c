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
/* subroutines for texmath */
#include "wims.h"

/* Careful! increasing this number risks stack overflow. */
#define MAX_FACTORS 256

enum {
    type_integer, type_numeric, type_var,
      type_poly, type_transcend
};

typedef struct afactor{
    char *beg, *end;
    int type, side;
} afactor;

char texmathbuf[MAX_LINELEN+1];
char *find_term_end(char *p);
void t_onestring(char *p);
void t_oneterm(char *p, int num);
void t_onefactor(struct afactor *p, int num);
/* never used or defined ?? */
void n_onestring(char *p);
void n_oneterm(char *p, int num);
void n_onefactor(struct afactor *p, int num);
/* */
void texmath(char *p);

/* print to texmathbuf */
void tprint(char *s,...)
{
    va_list vp;
    char buf[MAX_LINELEN+1];

    va_start(vp,s); vsnprintf(buf,sizeof(buf),s,vp); va_end(vp);
    if(strlen(buf)+strlen(texmathbuf)>=MAX_LINELEN)
      user_error("cmd_output_too_long");
    strcat(texmathbuf,buf);
}
/* transforms sum(n,n=1..infinity) , product(n/(n+1),n=1..infinity). */
void _tex_sums(char *p, char *name, int type)
{
    char *p1,*p2,*p3;
    p1=find_item_end(p); if(*p1) *(p1++)=0;
    p2=find_item_end(p1); p3=strparstr(p1,"=");
    if(p3<p2) p2=p3;
    if(*p2) *(p2++)=0;
    p3=find_item_end(p2);
    if(*p3) *(p3++)=0;
    tprint("\\%s ",name);
    if(type) {
      if(*p2) {
        tprint("_{"); t_onestring(p2); tprint("}");
      }
    }
    else if(*p1) {
      tprint("_{%s",p1);
      if(*p2) { tprint("="); t_onestring(p2); }
      tprint("}");
    }
    if(*p3) {
      tprint("^{"); t_onestring(p3); tprint("}");
    }
    strip_trailing_spaces(p); if(find_term_end(p)<p+strlen(p)) {
      tprint("\\left("); t_onestring(p); tprint("\\right)");
    }
    else t_onestring(p);
    if(type && *p1) {
      strip_trailing_spaces(p1); tprint("\\,\\textrm{d}"); // case of the integrale
      if(find_term_end(p1)<p1+strlen(p1)) {
        tprint("\\left("); t_onestring(p1); tprint("\\right)");
      }
      else t_onestring(p1);
    }
}

/* integration, sum and product */
void tex_int(char *p) {  _tex_sums(p,"int",1); }
void tex_sum(char *p) {  _tex_sums(p,"sum",0); }
void tex_prod(char *p) { _tex_sums(p,"prod",0);}

struct tmathfn tmathfn[]={
      {"Arg",      1,  "\\rm{Arg}",      "\\right)"},
      {"Int",      2,  "","",        tex_int},
      {"Prod",     2,  "","",        tex_prod},
      {"Sum",      2,  "","",        tex_sum},
      {"abs",      0,  "\\left|",      "\\right|"},
      {"acos",     1,  "\\rm{arccos}",  "\\right)"},
      {"acosh",    1,  "\\rm{Argch}",      "\\right)"},
      {"arg",      1,  "\\rm{Arg}",      "\\right)"},
      {"asin",     1,  "\\rm{arcsin}",  "\\right)"},
      {"asinh",    1,  "\\rm{Argsh}",      "\\right)"},
      {"atan",     1,  "\\rm{arctg}",      "\\right)"},
      {"atanh",    1,  "\\rm{Argth}",      "\\right)"},
      {"ch",       1,  "\\rm{ch}",      "\\right)"},
      {"conj",     0,  "\\overline{",      "}"},
      {"conjugate",0,  "\\overline{",      "}"},
      {"cos",      1,  "\\cos",      "\\right)"},
      {"cosh",     1,  "\\rm{ch}",      "\\right)"},
      {"cot",      1,  "\\rm{ctg}",      "\\right)"},
      {"cotan",    1,  "\\rm{ctg}",      "\\right)"},
      {"cotanh",   1,  "\\rm{cth}",      "\\right)"},
      {"csc",      1,  "\\rm{csc}",      "\\right)"},
      {"ctg",      1,  "\\rm{ctg}",      "\\right)"},
      {"cth",      1,  "\\rm{cth}",      "\\right)"},
      {"det",      1,  "\\rm{det}",      "\\right)"},
      {"erf",      1,  "\\rm{erf}",      "\\right)"},
      {"exp",      1,  "\\exp",      "\\right)"},
      {"int",      2,  "","",        tex_int},
      {"integrate",2,  "","",        tex_int},
      {"lg",       1,  "\\rm{lg}",      "\\right)"},
      {"ln",       1,  "\\ln",        "\\right)"},
      {"log",      1,  "\\log",      "\\right)"},
      {"prod",     2,  "","",        tex_prod},
      {"product",  2,  "","",        tex_prod},
      {"sec",      1,  "\\rm{sec}",      "\\right)"},
      {"sgn",      1,  "\\rm{sgn}",      "\\right)"},
      {"sh",       1,  "\\rm{sh}",      "\\right)"},
      {"sign",     1,  "\\rm{sign}",      "\\right)"},
      {"sin",      1,  "\\sin",      "\\right)"},
      {"sinh",     1,  "\\rm{sh}",      "\\right)"},
      {"sqrt",     0,  "\\sqrt{",      "}"},
      {"sum",      2,  "","",        tex_sum},
      {"tan",      1,  "\\tan",      "\\right)"},
      {"tanh",     1,  "\\rm{th}",      "\\right)"},
      {"tg",       1,  "\\rm{tg}",      "\\right)"},
      {"th",       1,  "\\rm{th}",      "\\right)"}
};

int tmathfn_no=(sizeof(tmathfn)/sizeof(tmathfn[0]));

struct tmathvar tmathvar[]={
      {"CC",      "\\mathbb{C}"},
      {"Delta",   "\\Delta"},
      {"Gamma",   "\\Gamma"},
      {"Inf",     "\\infty"},
      {"Lambda",  "\\Lambda"},
      {"NN",      "\\mathbb{N}"},
      {"Omega",   "\\Omega"},
      {"PI",      "\\pi"},
      {"Phi",     "\\Phi"},
      {"Pi",      "\\Pi"},
      {"Psi",     "\\Psi"},
      {"QQ",      "\\mathbb{Q}"},
      {"RR",      "\\mathbb{R}"},
      {"Sigma",   "\\Sigma"},
      {"Theta",   "\\Theta"},
      {"Upsilon", "\\Upsilon"},
      {"Xi",      "\\Xi"},
      {"ZZ",      "\\mathbb{Z}"},
      {"aleph",   "\\aleph"},
      {"alpha",   "\\alpha"},
      {"beta",   "\\beta"},
      {"chi",    "\\chi"},
      {"delta",  "\\delta"},
      {"epsilon","\\epsilon"},
      {"eta",    "\\eta"},
      {"gamma",  "\\gamma"},
      {"inf",    "\\infty"},
      {"infinity","\\infty"},
      {"infty",  "\\infty"},
      {"iota",   "\\iota"},
      {"kappa",  "\\kappa"},
      {"lambda", "\\lambda"},
      {"mu",     "\\mu"},
      {"neq",    "\\neq"},
      {"nu",     "\\nu"},
      {"omega",  "\\omega"},
      {"phi",    "\\phi"},
      {"pi",     "\\pi"},
      {"psi",    "\\psi"},
      {"rho",    "\\rho"},
      {"sigma",  "\\sigma"},
      {"tau",    "\\tau"},
      {"theta",  "\\theta"},
      {"xi",     "\\xi"},
      {"zeta",   "\\zeta"}
};

int tmathvar_no=(sizeof(tmathvar)/sizeof(tmathvar[0]));


/* find the end of an additive term. */
char *find_term_end(char *p)
{
    char *pp;
    pp=p;
    if(*pp==',' || *pp==';' || *pp=='=' || *pp=='<') pp++;
    while(*pp=='+' || *pp=='-' || *pp=='=' || *pp=='>') pp++;
    for(;*pp;pp++) {
      switch(*pp) {
        case '(': pp=find_matching(pp+1,')'); goto loopend;
        case '[': pp=find_matching(pp+1,']'); goto loopend;
/*     case '{': pp=find_matching(pp+1,'}'); goto loopend;*/

        case 0:
        case '<':
        case '>':
        case ',':
        case ';':
        case '=':
        case ')':
        case ']':
        case '}':
        case '-':
        case '+': return pp;

        case '*':
        case '/':
        case '^': {
          while(*(pp+1)=='+' || *(pp+1)=='-') pp++;
          goto loopend;
        }
      }
      if(isalnum(*pp) || *pp=='.') {
        pp=find_mathvar_end(pp); pp--; continue;
      }
      continue;
      loopend:
      if(pp==NULL) module_error("unmatched_parentheses");
    }
    return pp;
}

/* find the end of an multiplicative factor. */
char *find_factor_end(char *p)
{
    char *pp;
    pp=p; if(*pp==',' || *pp==';' || *pp=='=') pp++;
    while(*pp=='+' || *pp=='-' || *pp=='*' || *pp=='/') pp++;
    for(;*pp;pp++) {
      switch(*pp) {
        case '(': pp=find_matching(pp+1,')'); goto loopend;
        case '[': pp=find_matching(pp+1,']'); goto loopend;
/*    case '{': pp=find_matching(pp+1,'}'); goto loopend;*/

        case 0:
        case '<':
        case '>':
        case ',':
        case ';':
        case '=':
        case ')':
        case ']':
        case '}':
        case '+':
        case '-':
        case '*':
        case '/': return pp;

        case '^': {
          while(*(pp+1)=='+' || *(pp+1)=='-') pp++;
          goto loopend;
        }
      }
      if(isalnum(*pp) || *pp=='.') {
        pp=find_mathvar_end(pp); pp--; continue;
      }
      continue;
      loopend:
      if(pp==NULL) module_error("unmatched_parentheses");
    }
    return pp;
}

    /* returns the number of terms */
int term_cnt(char *p)
{
    char *pe, *pp;
    int i;

    pe=p+strlen(p);
    for(i=0,pp=p;pp<pe;pp=find_term_end(pp),i++);
    return i;
}

/* Print a number: transform 4E+05 in 4 \times 10^{5} and  4E-05 in 4 \times 10^{-5}
suppress multiple + or 0 ; see rawmath.c
*/
void putnumber(char *p)
 {
     char *sgn = "", *pp = strpbrk(p,"Ee");
     if (pp == NULL) { tprint("%s",p); return; }
     *pp++ = 0;
     if (*pp == '-') { sgn = "-"; pp++; }
     while (*pp == '0' || *pp == '+') pp++;
     tprint("%s \\times 10^{%s%s}", p, sgn, pp);
 }

/* Print a variable name ; transform abc475 in abc_475 */
void putvar(char *p)
{
    char vbuf[1024];
    char *pp, *p2;
    int i;

    vbuf[0]=0;
    if(*(p+1)==0) {tprint("%c",*p); return;}
    for(pp=p;isalpha(*pp);pp++);
    if(myisdigit(*pp)) {
    for(p2=pp+1;myisdigit(*p2);p2++);
    if(*p2==0) {/* subscript */
        mystrncpy(vbuf,pp,sizeof(vbuf));*pp=0;
    }
    }
    i=search_list(tmathvar, tmathvar_no, sizeof(tmathvar[0]), p);
    if(i>=0) tprint("%s ",tmathvar[i].tex);
    else tprint("%s ",p);
    if(vbuf[0]) {
    if(vbuf[1]==0) tprint("_%c ",vbuf[0]);
    else tprint("_{%s} ",vbuf);
    }
}

/* sort according to type */
int fsort(const void *p1, const void *p2)
{
  struct afactor *t1, *t2;
  int i1,i2;

  t1=*(struct afactor **) p1; t2=*(struct afactor **) p2;
  i1=t1->type; i2=t2->type;
  if(i1>type_var) i1=type_var;
  if(i2>type_var) i2=type_var;
  return i1-i2;
}

void t_oneterm(char *p, int num)
{
  int sign, fcnt, s, i, dentype, rel;
  char *pp, *pe, *pt;
  struct afactor factors[MAX_FACTORS];
  struct afactor *numerator[MAX_FACTORS],
    *denominator[MAX_FACTORS],
    *neutral[MAX_FACTORS];
  int numcnt,dencnt,neucnt;
/* interpret some arrows */
  rel=0;
  switch(*p) {
    case '<': {
      rel++; p++; if(*p!='=') {tprint(" < "); break;} // <
      do p++; while(*p=='=');
      if(*p!='>') {tprint("\\le ");break;} // <= , <===
      else {tprint("\\iff ");p++; break;} // <==>
    }
    case '>': {
      rel++; p++; if(*p!='=') {tprint(" > "); rel=1; break;} // >
      while(*p=='=') p++;
        tprint("\\ge "); // >=
        break;
    }
    case '-': {
      for(pp=p;*pp=='-';pp++);
      if(*pp!='>') break;
      rel++; tprint("\\to "); p=++pp; //->
      break;
    }
    case '=': {
        rel++; for(pp=p;*pp=='=';pp++);
        if(*pp!='>') break;
        tprint("\\Rightarrow "); p=++pp; // =>
        break;
    }
  }
  if(*p==',' || *p==';' || *p=='=') {tprint("%c",*p); p++; num=0;}
    sign=1; while(*p=='+' || *p=='-') {
      if(*p=='-') sign*=-1;
      p++;
  }
  for(fcnt=0, pp=p; fcnt<MAX_FACTORS && *pp; fcnt++, pp=pe) {
    s=1;
    while(*pp=='*' || *pp=='/') {
      if(*pp=='/') s=-1;
      pp++;
    }
    factors[fcnt].side=s;
    while(*pp=='+' || *pp=='-') {
      if(*pp=='-') sign*=-1;
      pp++;
    }
    pe=find_factor_end(pp); if(pe<=pp) break;
    factors[fcnt].beg=pp; factors[fcnt].end=pe;
    if(pe-pp==1 && *pp=='1') fcnt--;
    if(*pp=='(') {
      char *pt, *pe2, buf[MAX_LINELEN+1];
      int ss;
      pp++; pt=find_matching(pp,')');
      if(pt>=pe-1) {
        memmove(buf,pp,pt-pp); buf[pt-pp]=0;
        i=term_cnt(buf);
        if(i==1) { /* remove parentheses */
          for(;pp<pt && fcnt<MAX_FACTORS;pp=pe2,fcnt++) {
            ss=s;
            while(*pp=='*' || *pp=='/') {
              if(*pp=='/') ss=-s;
              pp++;
            }
            factors[fcnt].side=ss;
            while(*pp=='+' || *pp=='-') {
              if(*pp=='-') sign*=-1;
              pp++;
            }
            pe2=find_factor_end(pp);
            if(pe2<=pp) goto bailout;
            factors[fcnt].beg=pp; factors[fcnt].end=pe2;
            if(pe2-pp==1 && *pp=='1') fcnt--;
          }
          fcnt--;
        }
      }
    }
  }
  bailout:
/* decide if the factor is of type numeric, integer, poly, transcend or variable
*  (see priorities)
*/
  for(i=0;i<fcnt;i++) {
    pp=factors[i].beg; pe=factors[i].end;
    if(myisdigit(*pp) || *pp=='.') {
      for(pt=pp;pt<pe && myisdigit(*pt);pt++);
      if(pt<pe) factors[i].type=type_numeric; // digits with a point
      else factors[i].type=type_integer;  // digits without point
      continue;
    }
    if(*pp=='(') {
      factors[i].type=type_poly; continue; //there exists a parenthesis
    }
    pt=strchr(pp,'(');
    if(pt!=NULL && pt<pe) factors[i].type=type_transcend; //??
    else factors[i].type=type_var;  // variable in other cases
  }
  dentype=-1;
  for(i=0;i<fcnt;i++) if(factors[i].side<0 && factors[i].type>dentype)
    dentype=factors[i].type; // denominator type will be compared to the type of the factors
  dencnt=numcnt=neucnt=0;
  for(i=0;i<fcnt;i++) {
    if(factors[i].type>dentype) neutral[neucnt++]=factors+i;
    else {
      if(factors[i].side>0) numerator[numcnt++]=factors+i;
      else denominator[dencnt++]=factors+i;
    }
  }
  if(dencnt>0) qsort(denominator,dencnt,sizeof(denominator[0]),fsort);
  if(numcnt>0) qsort(numerator,numcnt,sizeof(numerator[0]),fsort);
  if(neucnt>0) qsort(neutral,neucnt,sizeof(neutral[0]),fsort);
  if(sign>0 && num>0 && rel==0) tprint(" +");
  if(sign<0) tprint(" -");
  if(fcnt<1) tprint("1 "); // no factors why 1 - don't remove the 1 if [1,2;3,4], the 1 is useful?
  if(dencnt>0) {
    tprint(" {\\frac{");
    if(numcnt==0) tprint(" 1"); // no numerator ? will write {1 over denominator}
    else {/* numerator */
      if(numcnt==1 && *numerator[0]->beg=='(' &&
          find_matching(numerator[0]->beg+1,')')==(numerator[0]->end)-1) {
        *(numerator[0]->end-1)=0;
        t_onestring(numerator[0]->beg+1);
        *(numerator[0]->end-1)=')';
      }
      else for(i=0; i<numcnt; i++) {t_onefactor(numerator[i],i);
      if(i<numcnt-1) tprint(" ");} /* add space between factors */
    }
    tprint(" }{ ");      /* Now denominator */
    if(dencnt==1 && *denominator[0]->beg=='(' &&
      find_matching(denominator[0]->beg+1,')')==(denominator[0]->end)-1) {
       *(denominator[0]->end-1)=0;
       t_onestring(denominator[0]->beg+1);
       *(denominator[0]->end-1)=')';
    }
    else for(i=0;i<dencnt;i++) {t_onefactor(denominator[i],i);
    if(i<dencnt-1) tprint(" ");} /* add space between factors */
    tprint("}} ");
  }
  for(i=0;i<neucnt;i++) {t_onefactor(neutral[i],i+dencnt);
  if(i<neucnt-1) tprint(" ");} /* add space between factors */
}

/* put exponential */
void t_exponential(char *pp)
{
  char *pe, *pt;
  int t=0;

  while(*pp && strchr("!'\"",*pp)!=NULL) {
    tprint("%c",*pp); pp++;
  }
  if(*pp=='^') pp++; else return;
  if(*pp=='(') {
    pe=find_matching(pp+1,')');
    if(*(pe+1)==0) {
      pp++;*pe=0;
      for(pt=pp;*pt && (isalnum(*pt) || *pt=='.');pt++);
      if(*pt==0) t=1;
    }
  }
  if(strlen(pp)==1 && t==0) tprint("^%s ",pp);
  else {
    tprint(" ^{"); if(t) tprint("(");
    t_onestring(pp);
    if(t) tprint(")");
    tprint("} ");
  }
}

void t_onefactor(struct afactor *fb, int num)
{
  char *p, *pe, lp, *rp, rp2, rpbuf[128];
  char fbuf[MAX_LINELEN+1], pbuf[MAX_LINELEN+1];
  int i;

  memmove(pbuf,fb->beg,fb->end-fb->beg);
  pbuf[fb->end-fb->beg]=0;
  if(num>0 && (myisdigit(pbuf[0]) || pbuf[0]=='.'))
    tprint("\\times ");
  rp2=')'; p=pbuf;
  if(strchr("({[",*p)!=NULL) {
  lp=*p; switch(lp) {
    case '(': rp2=')';  break;
    case '[': { /* verify for matrices */
      char *pt;
      pe=find_matching(p+1,']');
      for(pt=p+1;pt<pe;pt++) {
        switch(*pt) {
          case '(': pt=find_matching(pt+1,')'); break;
          case '[': pt=find_matching(pt+1,']'); break;
          case '{': pt=find_matching(pt+1,'}'); break;
          case '|': pt=find_matching(pt+1,'|'); break;

          case ',':
          case ';': goto out;
        }
      }
      out: if(*pt==';' || *pt==',') { /* is matrix of the form [ 1,2;5,6] */
        char mbuf[MAX_LINELEN+1];
        char *pp, *pt;

        p++; if(*pe) *pe++=0;
        tprint(" \\begin{pmatrix}");
        for(pp=p,i=0;*pp;pp=pt,i++) {
          pt=find_term_end(pp);
          memmove(mbuf,pp,pt-pp); mbuf[pt-pp]=0;
          t_oneterm(mbuf,i);
          if(*pt==',') {
            tprint(" &"); pt++; i=-1;
          }
          if(*pt==';') {
            tprint("\\cr "); pt++; i=-1;
          }
        }
        tprint(" \\end{pmatrix}"); goto expon;
      }
      rp2=']'; break;
    }
    case '{': { /* protected */
      pe=find_matching(p+1,'}');
      *pe=0;tprint(" %s} ",p);
      goto expon;
    }
  }
  tprint(" \\left%c",lp);
  snprintf(rpbuf,sizeof(rpbuf),"\\right%c ",rp2); rp=rpbuf;
  paren: p++;pe=find_matching(p,rp2); *pe=0;
  t_onestring(p); tprint(rp); pe++; goto expon;
  }
  pe=find_mathvar_end(p); while(*pe && strchr("'\"!",*pe)!=NULL) pe++;
  memmove(fbuf,p,pe-p); fbuf[pe-p]=0;
  if(myisdigit(*p) || *p=='.') putnumber(fbuf);
  if(isalpha(*p)) {
    pe=find_mathvar_end(p); while(*pe && strchr("'\"!",*pe)!=NULL) pe++;
    if(*pe=='(') {
      p=pe;
/* search in list of math functions*/
      i=search_list(tmathfn, tmathfn_no, sizeof(tmathfn[0]), fbuf);
      if(i>=0) {
        switch(tmathfn[i].expind) {
          case 0: {
            tprint(" %s",tmathfn[i].left);
            rp=tmathfn[i].right; break;
          }
          case 1: {
            tprint(" %s",tmathfn[i].left);
            pe=find_matching(pe+1,')')+1;
            if(*pe && strchr("^'\"!",*pe)!=NULL) {
              t_exponential(pe); *pe=0;
            }
            tprint(" \\left("); rp=tmathfn[i].right;
            break;
          }
          case 2: {  /* routine */
            p++;pe=find_matching(p,rp2); *pe=0;
            tmathfn[i].routine(p);
            pe++; goto expon;
          }
          default: rp=""; break;
        }
      }
      else {
        putvar(fbuf);
        rp="\\right) "; tprint(" \\left(");
      }
      rp2=')'; goto paren;
    }
    else {
      putvar(fbuf);
      if(*pe=='_') {
        char *ptt, buff[256];
        tprint("_"); pe++;
        if(*pe=='(') {
          ptt=find_matching(pe+1,')'); if(ptt) ptt++;
        }
        else {
          if(*pe=='{') {
            ptt=find_matching(pe+1,'}'); if(ptt) ptt++;
          }
          else ptt=find_mathvar_end(pe);
        }
        if(ptt==NULL || ptt-pe>128) goto expon;
        memmove(buff,pe,ptt-pe); buff[ptt-pe]=0; pe=ptt;
        strip_enclosing_par(buff);
        tprint("{%s}",buff);
      }
    }
  }
    /* exponential */
  expon: if(*pe && strchr("^'\"!",*pe)!=NULL) t_exponential(pe);
}

void t_onestring(char *p)
{
  char termbuf[MAX_LINELEN+1];
  char *pp, *pe;
  int i;

  for(pp=p,i=0;*pp;pp=pe,i++) {
  pe=find_term_end(pp);
  memmove(termbuf,pp,pe-pp); termbuf[pe-pp]=0;
  t_oneterm(termbuf,i);
  }
}
/* replace \pmatrix{  } by latex syntax \begin{pmatrix} .. \end{pmatrix} */

void _replace_matrix ( char *p , char *s_mat1, char *s_mat2 )
{
  char pbuf[MAX_LINELEN];
  while ( (p = strstr(p,s_mat1)) ){
    char *p2 = find_matching(p+strlen(s_mat1),'}');
    long len = p2-p-strlen(s_mat1);
    if (!p2) { module_error("unmatched_parentheses"); return; }
    memcpy(pbuf, p+strlen(s_mat1), len); pbuf[len]= 0;
    p2 ++ ;
    string_modify(p, p, p2, "\\begin{%s}%s\\end{%s}",s_mat2,pbuf,s_mat2);
  }
}

/* translate raw math expression into TeX source */
void texmath(char *p)
{
  char *pp;
  _replace_matrix (p,"\\matrix{","matrix");
  _replace_matrix (p,"\\pmatrix{","pmatrix");
  if(strpbrk(p,"{}\\")!=NULL) return;
  for(pp=strstr(p,"!="); pp; pp=strstr(pp+1,"!=")) {
    if(pp>p && !isspace(*(pp-1))) continue;
    string_modify(p,pp,pp+2,"*neq*");
  }
/* remove spaces */
  for(pp=p; *pp; pp++) {
  if(isspace(*pp)) {ovlstrcpy(pp,pp+1); pp--;}
  }
/* replace ** by ^  see __replace_badchar(p,"**", "^");*/
  for(pp=strstr(p,"**"); pp!=NULL; pp=strstr(pp,"**")) {
    *pp='^'; ovlstrcpy(pp+1,pp+2);
  }
  if(check_parentheses(p,1)!=0) module_error("unmatched_parentheses");
  texmathbuf[0]=0; t_onestring(p);
  singlespace(texmathbuf);strip_trailing_spaces(texmathbuf);
  mystrncpy(p,texmathbuf,MAX_LINELEN);
}
