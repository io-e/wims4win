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

/* automatic selection of math rendering method */

void exec_instex(char *p);
void calc_instexst(char *p);

struct {
  char src[124], name[128];
  int size;
} oldinstex[100];
int oldtexcnt=0;

    /* check whether the same tex source has already been produced */
int instex_ready(char *p, char *n)
{
  int i;
  char *cl, buf[MAX_LINELEN+1];

  if(strlen(p)>=124) return 0;
  cl=getvar("instex_color"); if(cl!=NULL && *cl!=0) return 0;
  mystrncpy(buf,p,sizeof(buf)); tex_nospace(buf);
  for(i=0;i<oldtexcnt;i++) {
    if(oldinstex[i].size==current_tex_size &&
     strcmp(oldinstex[i].src,buf)==0) {
      ovlstrcpy(n,oldinstex[i].name); return 1;
    }
  }
  if(strlen(n)>=128 || oldtexcnt>=100) return 0;
  ovlstrcpy(oldinstex[oldtexcnt].src,buf);
  ovlstrcpy(oldinstex[oldtexcnt].name,n);
  oldinstex[oldtexcnt].size=current_tex_size;
  oldtexcnt++; return 0;
}

    /* returns NULL if instex can use static */
char *instex_check_static(char *p)
{
    char *f;
    if(instex_usedynamic) return p;
    for(f=strchr(p,'$');
    f!=NULL && *(f+1)!='(' && *(f+1)!='[' && *(f+1)!='_' && !isalnum(*(f+1));
    f=strchr(f+2,'$'));
    if(f==NULL) f=strstr(m_file.name,"sessions/");
    return f;
}

char tnames[]="sqrt int integrate sum prod product \
Int Sum Prod conj abs";

int __gototex (char *p, char *f, int ts)
{
  char alignbak[2048];
  char *pp, buf[MAX_LINELEN+1];
  ovlstrcpy(buf,p);
  instex_style="$$";
  if(!ts) texmath(buf);
     /* ts=0 but there is some computer matrix to transform
      * done by texmath, but it does much more as replacing strings in tmathfn
      * OK if buf contains " math computer-syntax" ; if not, the result may be bad
    */
  else {// seems tex : need to interpret names of variables as \x or \calB
   //if (mathalign_base < 2) { //to check
    char *p1;
    p1=find_word_start(buf);
    if(*p1=='\\') {
      int i;
      char *pt;
      for(i=1;isalnum(p1[i]);i++); /* find an alphanumeric string beginning by \\ */
      if(p1[i]==0 && (pt=mathfont(p1))!=NULL) {
        _output_(pt); *p=0; return 1;
      }
    }
      // }
  }
      /* send to mathml */
  if( strcmp( getvar("force_mathml") , "yes" ) == 0 ){/* override math-with-gifs */
    mathalign_base = 2;
  }
  if (mathalign_base == 2 && mathml(buf,0)) { *p=0 ;return 1; }
/* end if mathml option in case ts=1 or "computer matrix" */

/* creating images*/
  pp=getvar("ins_align");
  if(pp!=NULL) mystrncpy(alignbak,pp,sizeof(alignbak));
  setvar("ins_align","middle");
  mystrncpy(ins_alt,buf,sizeof(ins_alt));
  if(f==NULL) {
    calc_instexst(buf); output("%s",buf);
  }
  else {
    instex_usedynamic=1; exec_instex(buf); instex_usedynamic=0;
  }
  instex_style="";
  if(alignbak[0]) setvar("ins_align",alignbak);
  *p=0; return 0;
}

    /* Intelligent insertion of math formulas, kernel */
void __insmath(char *p)
{
  char *f, *pp, *pe, *p1, buf[MAX_LINELEN+1], nbuf[256];
  int ts, n, rawmathready;

  ovlstrcpy(buf,p); strip_trailing_spaces(buf); singlespace(buf);
  p1=getvar("insmath_slashsubst");
  if(p1!=NULL && strstr(p1,"yes")!=NULL) slashsubst(buf); // substitute backslash parameters
  f=instex_check_static(buf); //decide if image already exists
  substit(buf);//substitute the variables
  /* here replace .. by , : i=1 .. 5 -> i=1, 5 !*/
  for(pp=strstr(buf,".."); pp!=NULL; pp=strstr(pp,"..")) {
    if(*(pp+2)=='.' || *(pp+2)==',') {
      do pp++; while(*pp=='.'); continue;
    }
    *pp=','; *(pp+1)=' ';
  }
  /* decide if it should be tex */
  ts=0; if(strchr(buf,'\\') || strchr(buf,'}')) ts=1;
  /* if not and if variable insmath_rawmath is there, do rawmath */
  rawmathready=0;
  if(!ts) { /* not tex, looking if rawmath is asked */
    pp=getvar("insmath_rawmath");
    if(pp!=NULL && strstr(pp,"yes")!=NULL) {
      rawmath(buf); rawmathready=1;
    }
  }
  if(ts) {
       _replace_matrix (buf,"\\matrix{","matrix"); //could be done in any case if ts=1
       _replace_matrix (buf,"\\pmatrix{","pmatrix");
  }
/* if ts=1 (it should be a tex formula)  or if there is a [ ,  ; ] matrix */
  if(ts ||
    (strchr(buf,'[')!=NULL && (strchr(buf,',')!=NULL || strchr(buf,';')!=NULL))) {
     if(__gototex(buf, f, ts)) return;
  }

/* end creating images
 * we are now in the case where ts=0 and no need of tex for matrix */

/* find math variables */
  for(pp=find_mathvar_start(buf); *pp; pp=find_mathvar_start(pe)) {
    pe=find_mathvar_end(pp); n=pe-pp;
    /* non alpha variable or too short or too long to be interpreted as tnames */
    if(!isalpha(*pp) || n<3 || n>16) continue;
    memmove(nbuf,pp,n); nbuf[n]=0;
    if(wordchr(tnames,nbuf)!=NULL) { if(__gototex(buf, f, 0)) return;}
    /* find sqrt int integrate sum prod product Int Sum Prod conj abs,
     * so must be texmath interpretated ; after going to tex, return in any case
     */
  }
/* look for  /  to interpretate as quotients -
 * extend the version by accepting something else than (
*/
    //for(pp=strchr(buf,'/'); pp!=NULL && *find_word_start(pp+1)!='('; pp=strchr(pp+1,'/'));
  pp=strchr(buf,'/');
  if(pp!=NULL){ if( __gototex(buf,f,0)) return;} /* so a/4 can be reinterpreted as {a over 4 } ; transform also 5/(x+1) */
  if(rawmathready) rawmath_easy=1;
  for(pp=strchr(buf,'<'); pp!=NULL; pp=strchr(pp+1,'<'))
    string_modify(buf,pp,pp+1,"&lt;");
  for(pp=strchr(buf,'>'); pp!=NULL; pp=strchr(pp+1,'>'))
    string_modify(buf,pp,pp+1,"&gt;");
/* no tex has been introduced - so go to mathmlmath */
  mathmlmath(buf); output("%s",buf);
  rawmath_easy=0;
}

/* the following is not used in modules : no insmath_logic=yes somewhere */

char *andor[]={"and","or","not","is","isnot"};
#define andorcnt (sizeof(andor)/sizeof(andor[0]))
char *andorlang[andorcnt], andorlangbuf[1024];
int andorlangcnt=-1;

/* Processing logic statements in math formulas */
void _mathlogic(char *p, void _put(char *pp))
{
  char *p1, *p2, *ps;
  int i;
  if(strstr(p,"qzis")==NULL) {
    for(i=0;i<andorcnt && varchr(p,andor[i])==NULL; i++);
    if(i>=andorcnt) {
      _put(p); return;
    }
  }
  if(andorlangcnt<0) {
    char buf[MAX_LINELEN+1];
    accessfile(buf,"r","bases/sys/andor.%s",lang);
    mystrncpy(andorlangbuf,find_word_start(buf),sizeof(andorlangbuf));
    for(i=0,p1=andorlangbuf;i<andorcnt;i++,p1=find_word_start(p2)) {
      p2=strchr(p1,',');
      if(p2==NULL) p2=p1+strlen(p1); else *p2++=0;
      strip_trailing_spaces(p1);
      if(*p1) andorlang[i]=p1; else break;
  }
  andorlangcnt=i;
  }
  for(ps=p, p1=find_mathvar_start(p); *p1; p1=find_mathvar_start(p2)) {
    p2=find_mathvar_end(p1);
    if(!isalpha(*p1)) continue;
    if(strncmp(p1,"qzis",4)==0) {
      char *p3, *p4, *p5;
      /*int tt;*/
      p4=find_word_start(p2);
      if(*p4!='(') continue;
      if(strncmp(p1+4,"not",3)==0) {/*tt=4;*/ p3=p1+7;}
      else {/*tt=3; */p3=p1+4;}
      if(!isalpha(*p3)) continue;
      p4++; p5=find_matching(p4,')');
      if(*p5!=')') continue;
      *p5=0; *p2=0; p2=p5+1;


      continue;
    }
    for(i=0;i<andorlangcnt;i++) if(strncmp(p1,andor[i],p2-p1)==0) break;
    if(i<andorlangcnt) {
      *p1=0; ps=find_word_start(ps); if(*ps) _put(ps);
      output(" %s ",andorlang[i]); ps=p2;
    }
  }
  ps=find_word_start(ps); if(*ps) _put(ps);
}

/* Intelligent insertion of math formulas */
void insmath(char *p)
{
  char *pt;
  if(!outputing) goto end;
  pt=getvar("insmath_logic");
  if(pt==NULL || strstr(pt,"yes")==NULL) {
    __insmath(p);
    end: *p=0; return;
  }
  _mathlogic(p,__insmath);
}
