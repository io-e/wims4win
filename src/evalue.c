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

void calc_itemof(char *p);
void calc_rowof(char *p);
void calc_columnof(char *p);

/* It is this routine which uses print_precision. */
void float2str(double d, char *p)
{
  int i;
  if(d==0) {
    ovlstrcpy(p,"0"); return;
  }
  if(!isfinite(d)) { /* isinf, isnan possibly not available */
    if (d == d) ovlstrcpy(p, (d > 0)? "Inf": "-Inf"); else ovlstrcpy(p,"NaN");
    return;
  }
  if(d<1000000 && d>-1000000 && d==floor(d)) {
    mystrncpy(p,int2str(d),MAX_LINELEN); return;
  }
  i=print_precision;
  if(i<2) i=2;
  if(i>32) i=32;  /* Simple limitation. */
  snprintf(p,MAX_LINELEN,"%.*g",i,(double) d);
  if(isspace(*p)) ovlstrcpy(p,find_word_start(p));
}

/* substitute variable names by their environment strings
 * The buffer pointed to by p must have enough space
 * (defined by MAX_LINELEN).
 */
char *substit(char *p)
{
  char *pp, *p2, *ev;
  char *oldlast, *oldnext, *oldend, *newend;
  char buf[MAX_LINELEN+1], oldbuf[MAX_LINELEN+1];
  int ln;

  if((p2=strchr(p,'$'))==NULL) return p;
  if(substnest>SUBST_LIMIT) module_error("subst_exceeded");
  ln=strlen(p); if(ln>=MAX_LINELEN) goto too_long;
  memmove(oldbuf,p,ln); oldbuf[ln]=0; oldend=oldbuf+ln;
  newend=p; oldlast=oldnext=oldbuf;
  for(pp=oldbuf+(p2-p); pp!=NULL; pp=strchr(pp,'$')) {
    if(*(pp+1)=='$') { /* escaped dollar sign */
      pp++; if(newend-p+(pp-oldlast)>=MAX_LINELEN) goto too_long;
      memmove(newend,oldlast,pp-oldlast); newend+=pp-oldlast;
      pp++; oldlast=pp; continue;
    }
    switch(*(pp+1)) {
      case 0: {
        *pp=0; oldend--; goto end;
      }
      case '(': {
        p2=find_matching(pp+2,')');
        if(p2==NULL) {
          module_error("unmatched_parentheses");
          *p=0; return p;
        }
        *p2++=0; oldnext=p2; memmove(buf,pp+2,p2-pp-2);
        substnest++; substit(buf); substnest--;
        break;
      }

      case '[': {
        double d;
        p2=find_matching(pp+2,']');
        if(p2==NULL) {
          module_error("unmatched_parentheses");
          *p=0; return p;
        }
        *p2=0; ovlstrcpy(buf,pp+2); oldnext=p2+1;
        substnest++; substit(buf); substnest--;
        d=evalue(buf); float2str(d,buf);
        goto replace;
      }

      default: {
        for(p2=pp+1; myisalnum(*p2) || *p2=='_'; p2++);
        oldnext=p2; memmove(buf,pp+1,p2-(pp+1)); buf[p2-(pp+1)]=0;
        goto noarray;
      }
    }
    if((p2=strchr(buf,'['))!=NULL) {
      char *pt1, tbuf[MAX_LINELEN+1];
      *p2++=0; pt1=find_matching(p2,']');
      if(pt1==NULL) {buf[0]=0; goto replace;}
      *pt1=0; pt1=strchr(p2,';');
      if(pt1==NULL) {
        if(*find_word_start(p2)==0) {*p2=0; goto noarray;}
         snprintf(tbuf,sizeof(tbuf),"%s of $%s",p2,buf);
         calc_itemof(tbuf); ovlstrcpy(buf,tbuf); goto replace;
      }
      else {
        *pt1++=0; p2=find_word_start(p2);
        if(*p2) {
          snprintf(tbuf,sizeof(tbuf),"%s of $%s",p2,buf);
          calc_rowof(tbuf);
        }
        else {
          snprintf(tbuf,sizeof(tbuf),"$%s",buf); substit(tbuf);
        }
        if(*find_word_start(pt1)) {
          snprintf(buf,sizeof(buf),"%s of %s",pt1,tbuf);
          calc_columnof(buf); goto replace;
        }
        else ovlstrcpy(buf,tbuf);
        goto replace;
      }
    }
    noarray: ev=getvar(buf); ln=getvar_len;
    if(ev==NULL) ev="";
    if(strchr(ev,'$')==NULL) goto rep2;
    memmove(buf,ev,ln); buf[ln]=0;
    substnest++; substit(buf); substnest--;
    replace: ev=buf; ln=strlen(ev);
    rep2:
    if(pp>oldlast) {
      if(newend-p+(pp-oldlast)>=MAX_LINELEN) goto too_long;
      memmove(newend,oldlast,pp-oldlast); newend+=pp-oldlast;
    }
    if(ln>0) {
      if((newend-p)+ln>=MAX_LINELEN) goto too_long;
      memmove(newend,ev,ln); newend+=ln;
    }
    pp=oldlast=oldnext;
    continue;
  }
  end:
  if(oldlast<oldend) {
    if(newend-p+(oldend-oldlast)>=MAX_LINELEN) goto too_long;
    memmove(newend,oldlast,oldend-oldlast); newend+=oldend-oldlast;
  }
  *newend=0; return p;
  too_long: user_error("cmd_output_too_long"); return NULL;
}

struct forstruct forstruct;

int for_getvar(char *p)
{
  char *vp, buf[MAX_LINELEN+1];
  mystrncpy(buf,p,sizeof(buf)); substit(buf);
  vp=find_word_start(buf); for(p=vp; myisalnum(*p) || *p=='_'; p++);
  *p=0; if(p-vp<=0 || p-vp>MAX_NAMELEN) return -1;
  mystrncpy(forstruct.var,vp,sizeof(forstruct.var));
  return 0;
}

/* If bufp=NULL then numeric. Else string-wise.
 * returns 0 if success,
 * -1 if syntax error, 1 if bad values
 */
int cutfor(char *p, char *bufp)
{
  char *eqp, *fromp, *top, *stepp, *inp;

  p=find_word_start(p);
  inp=find_word_start(find_word_end(p));
  if(wordchr(inp,"in")==inp) {
    char buf[MAX_LINELEN+1], *bp;
     int i;
    double d;
    *inp=0; inp+=strlen("in");
    forin: inp=find_word_start(inp); forstruct.type=for_in;
    if(for_getvar(p)) return -1;
    if(bufp!=NULL) bp=bufp; else bp=buf;
    mystrncpy(bp,inp,MAX_LINELEN); substit(bp);
    strip_trailing_spaces(bp);
    if(bp[0]==0) {
      forstruct.from=0; forstruct.to=-1; forstruct.step=1; return 0;
    }
    for(i=0, inp=bp; i<MAX_VALUE_LIST && inp!=NULL; inp=top) {
      top=strparchr(inp,','); if(top) *top++=0;
      if(bufp==NULL) {
        d=evalue(inp); if(isfinite(d)) forstruct.list[i++]=d;
        else return 1;
      }
      else {
        strip_trailing_spaces(inp);
        forstruct.pos[i++]=find_word_start(inp);
      }
    }
    forstruct.from=0; forstruct.to=i-1; forstruct.step=1; return 0;
  }
  top=wordchr(p,"to"); if(top==NULL) {
    inp=strchr(p,'='); if(inp==NULL) return -1;
    *inp++=0; goto forin;
  }
  *top=0; top+=strlen("to");
  stepp=wordchr(top,"step"); if(stepp!=NULL) {
    *stepp=0; stepp+=strlen("step"); forstruct.step=evalue(stepp);
  }
  else forstruct.step=1;
  forstruct.to=evalue(top); forstruct.type=for_from;
  eqp=strchr(p,'='); fromp=wordchr(p,"from"); inp=wordchr(p,"in");
  if(eqp!=NULL && (fromp==NULL || eqp<fromp)) {
    *eqp++=0; fromp=eqp;
  }
  else {
    if(fromp==NULL) return -1;
    *fromp=0; fromp+=strlen("from");
  }
  forstruct.from=evalue(fromp);
  if(for_getvar(p)) return -1;
  if(!isfinite(forstruct.from+forstruct.to+forstruct.step)) return 1;
  else return 0;
}
