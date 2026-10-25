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
/* line input / output / translation routines
 * and error routines
 */

#include "../Lib/libwims.h"
#include "oef2wims.h"

/* critical error situation */
void oef_error(char *s)
{
  /* fputs(s,stderr); */
  if(badpar!=NULL) {
   char *p; int i,t,v;
   char buf[128];
   for(p=inpbuf,t=1;p<badpar;p++) if(*p=='\n') t++;
   for(p=badpar-1;p>=inpbuf && *p!='\n'; p--);
   p++; v=badpar-p+1;
   snprintf(buf,sizeof(buf),"%s",p);
   for(i=0,p=buf;i<3 && *p;p++) if(*p=='\n') i++;
   *p=0;
   printf("\nERROR %s '%c' at line %d col %d.\n",
         s,*badpar,t,v);
   for(i=1;i<v;i++) putchar(' ');
   printf("!\n%s\n",buf);
  }
  else printf("\nERROR %s\n",s);
  exit(1);
}

char *substit(char *p)
{
  char *pp;
  while((pp=strchr(p,'$'))!=NULL) *pp=' ';
  return p;
}

/* replace newline by tab */
void replace_newline(char *p)
{
  for(;*p;p++) {
    if(*p=='	') *p=' ';
    if(*p=='\n') *p='	';
  }
}
/* non existent strings should not compare */
int my_strcmp(char *a, char *b) {return (a && b)? strcmp(a,b):1; }

/* variable substitution. buffer p must have MAX_LINELEN */
void subst(char *p)
{
  char *pp, *pe, nbuf[MAX_NAMELEN];
  int i;

  for(pp=p;pp-p<MAX_LINELEN && *pp; pp++) {
    if(*pp=='$') {
      string_modify(p,pp,pp+1,"&#36;");
      pp+=3;
      continue;
    }
    if(*pp=='!' && isalnum(*(pp+1))) {
      string_modify(p,pp,pp+1,"&#33;");
      pp+=3; continue;
    }
    if(*pp!='\\') continue;
    if(*(pp+1)=='\\') {
      pp++; continue;
    }
    if(!isalpha(*(pp+1))) continue;
    for(pe=pp+1;isalnum(*pe) || *pe=='_'; pe++);
    if(pe-pp>=MAX_NAMELEN) continue;
    memmove(nbuf,pp+1,pe-pp-1);nbuf[pe-pp-1]=0;
    for(i=varcnt-1;i>=1 && my_strcmp(nbuf,param[i].name)!=0;i--);
    if(i>=1) {
      if(deftag) param[i].save=1;
      if(*pe=='[') {
        char *pt, tbuf[MAX_LINELEN+1], vbuf[MAX_LINELEN+20];
        int l;
        pt=find_matching(pe+1,']'); if(pt!=NULL && pt-pe<MAX_LINELEN) {
          pe++; memmove(tbuf,pe,pt-pe); tbuf[pt-pe]=0; pe=pt+1;
          subst(tbuf);
          for(pt=strchr(tbuf,'\\');pt;pt=strchr(pt,'\\'))
            string_modify(tbuf,pt,pt+1,"$m_");
          snprintf(vbuf,sizeof(vbuf),"$(val%d[%s])",i,tbuf);
          l=strlen(vbuf);
          string_modify(p,pp,pe,"%s",vbuf);
          pp+=l-1;continue;
        }
      }
      string_modify(p,pp,pe,"$val%d",i);
    }
    else if(wordchr(mdef,nbuf)!=NULL) string_modify(p,pp,pp+1,"$m_");
  }
}

void repsubst(char *p)
{
  char *p1;
  for(p1=strstr(p,"\\reply"); p1!=NULL; p1=strstr(p1+1,"\\reply")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\reply"),"$m_reply");
  }
  for(p1=strstr(p,"\\result"); p1!=NULL; p1=strstr(p1+1,"\\result")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\result"),"$m_result");
  }
  for(p1=strstr(p,"\\choice"); p1!=NULL; p1=strstr(p1+1,"\\choice")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\choice"),"$m_choice");
  }
  for(p1=strstr(p,"\\step"); p1!=NULL; p1=strstr(p1+1,"\\step")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\step"),"$m_step");
  }
  for(p1=strstr(p,"\\sc_reply"); p1!=NULL; p1=strstr(p1+1,"\\sc_reply")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\sc_reply"),"$m_sc_reply");
  }
  for(p1=strstr(p,"\\sc_choice"); p1!=NULL; p1=strstr(p1+1,"\\sc_choice")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\sc_choice"),"$m_sc_choice");
  }
  for(p1=strstr(p,"\\reply_"); p1!=NULL; p1=strstr(p1+1,"\\reply_")) {
    if(p1>p && *(p1-1)=='\\') continue;
    string_modify(p,p1,p1+strlen("\\reply_"),"$m_reply_");
  }
}


/* find matching parenthesis.
 * The entrance point should be after the opening
 * parenthesis.
 * Returns NULL if unmatched.
 */
char *find_matching2(char *p, char c)
{
  char *pp, *pp2, *ppar,*pbrk, *pbrc;
  int parenth, brak, brace;
  if(c=='|') {
    for(pp=p;*pp!=0;pp++) {
      pp2=pp;
      switch(*pp) {
        case '|': return pp;
        case '(': {
          pp=find_matching(pp+1,')');
          if(pp==NULL) {if(badpar==NULL) badpar=pp2; return NULL;}
          break;
        }
        case '[': {
          pp=find_matching(pp+1,']');
          if(pp==NULL) {if(badpar==NULL) badpar=pp2; return NULL;}
          break;
        }
        case '{': {
          pp=find_matching(pp+1,'}');
          if(pp==NULL) {if(badpar==NULL) badpar=pp2; return NULL;}
          break;
        }
        case ')':
        case ']':
        case '}': badpar=pp; return NULL;

        default: break;
      }
    }
    return NULL;
  }
  parenth=brak=brace=0; ppar=pbrc=pbrk=NULL;
  for(pp=p; *pp!=0; pp++) {
    switch(*pp) {
      case '[': if(brak==0) pbrk=pp; brak++; break;
      case ']': brak--; break;
      case '(': if(parenth==0) ppar=pp; parenth++; break;
      case ')': parenth--; break;
      case '{': if(brace==0) pbrc=pp; brace++; break;
      case '}': brace--; break;
      default: continue;
    }
    if(parenth<0 || brak<0 || brace<0) {
      if(badpar==NULL) {
        if(parenth>0) badpar=ppar;
        if(brace>0) badpar=pbrc;
        if(brak>0) badpar=pbrk;
      }
      if(parenth>0 || brak>0 || brace>0) return NULL;
      if(*pp!=c) {
        if(badpar==NULL) badpar=pp;
        return NULL;
      }
      else break;
    }
  }
  if(*pp!=c) return NULL;
  return pp;
}

/* Check whether parentheses are balanced in a given string.
 * Returns 0 if OK.
 * style=0: simple check. style<>0: strong check.
 */
int checkparentheses(char *p, int style)
{
  int i,j,k;
  j=strlen(p);
  if(j>=MAX_FILELEN) return 65535;
  if(style!=0) {
    char *pp, *pe, c;
    for(pp=p;pp<p+j;pp++) {
      switch (*pp) {
        case ')':
        case ']':
        case '}': {badpar=pp; return -1;}
        case '(': c=')'; goto find;
        case '[': c=']'; goto find;
        case '{': c='}';
        find: {
          pe=find_matching2(pp+1,c);
          if(pe==NULL) {
            if(badpar==NULL) badpar=pp;
            return 1;
          }
          else pp=pe;
        }
        default: break;
      }
    }
    return 0;
  }
  for(i=k=0;i<j && k>=0;i++) {
    if(*(p+i)=='(') k++;
    if(*(p+i)==')') k--;
  }
  return k;
}
