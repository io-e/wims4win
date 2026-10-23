/*    Copyright (C) 2002-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
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

#include "mathexp.h"

struct {
    char *orig, *reverse;
} revtab[]={
    {"=", "!="},
    {"!=", "="},
    {"==", "!="},
    {"<>", "="},
    {"<", ">="},
    {">", "<="},
    {"<=", ">"},
    {">=", "<"},
    {"and", "or"},
    {"or", "and"},
};
#define revno (sizeof(revtab)/sizeof(revtab[0]))

/* p will contain the new exp, and must has a buffer of MAX_LINELEN+1 */
void _not(char *p)
{
    char buf1[MAX_LINELEN+1], bufr[MAX_LINELEN+1];
    char buft[16];
    int commas[MAX_COMMAS], cmcnt;
    char *pp;
    int i, l;

    snprintf(buf1,sizeof(buf1),"%s",find_word_start(p));
    retype:
    if(buf1[0]==0) {*p=0; return;} /* empty */
    strip_trailing_spaces(buf1);
    switch(_type(buf1,commas,&cmcnt)) {
     case exp_paren: {
         if(buf1[0]=='(' && find_matching(buf1+1,')')==buf1+strlen(buf1)-1) {
           buf1[strlen(buf1)-1]=0; ovlstrcpy(buf1,find_word_start(buf1+1));
           goto retype;
         }
         snprintf(p,MAX_LINELEN,"not %s",buf1); return;
     }
     case exp_not: {
         if(strncasecmp(buf1,"not",3)!=0) error("Syntax error.");
         pp=find_word_start(buf1+3);
         if(*pp=='(' && find_matching(pp+1,')')==pp+strlen(pp)-1) {
          pp++; *(pp+strlen(pp)-1)=0;
         }
         snprintf(p,MAX_LINELEN,"%s",pp); return;
     }
     case exp_ineq:
     case exp_eq: {
         if(cmcnt!=2 || commas[1]-commas[0]>4) error("Syntax error.");
         memmove(buft,buf1+commas[0],commas[1]-commas[0]);
         buft[commas[1]-commas[0]]=0;
         for(i=0;i<revno && strcmp(buft,revtab[i].orig)!=0;i++);
         if(i>=revno) error("Software bug: bad sign.");
         string_modify(buf1,buf1+commas[0],buf1+commas[1],revtab[i].reverse);
         snprintf(p,MAX_LINELEN,"%s",buf1);
         return;
     }
     case exp_and: {
         if(cmcnt<2 || (cmcnt&1)!=0) error("Syntax error.");
         commas[cmcnt]=strlen(buf1);
         memmove(bufr,buf1,commas[0]); bufr[commas[0]]=0;
         _not(bufr); snprintf(p,MAX_LINELEN+1,"%s",bufr); l=strlen(p);
         for(i=1;i<=cmcnt/2;i++) {
          memmove(bufr,buf1+commas[2*i-1],commas[2*i]-commas[2*i-1]);
          bufr[commas[2*i]-commas[2*i-1]]=0;
          _not(bufr);
          snprintf(p+l,MAX_LINELEN-l," or %s",bufr); l=strlen(p);
         }
         return;
     }
     case exp_or: {
         int commas2[MAX_COMMAS], cmcnt2;
         if(cmcnt<2 || (cmcnt&1)!=0) error("Syntax error.");
         commas[cmcnt]=strlen(buf1);
         memmove(bufr,buf1,commas[0]); bufr[commas[0]]=0;
         _not(bufr);
         if(_type(bufr,commas2,&cmcnt2)!=exp_or)
           snprintf(p,MAX_LINELEN+1,"%s",bufr);
         else snprintf(p,MAX_LINELEN+3,"(%s)",bufr);
         l=strlen(p);
         for(i=1;i<=cmcnt/2;i++) {
          memmove(bufr,buf1+commas[2*i-1],commas[2*i]-commas[2*i-1]);
          bufr[commas[2*i]-commas[2*i-1]]=0;
          _not(bufr);
          if(_type(bufr,commas2,&cmcnt2)!=exp_or)
            snprintf(p+l,MAX_LINELEN-l," and %s",bufr);
          else snprintf(p+l,MAX_LINELEN-l," and (%s)",bufr);
          l=strlen(p);
         }
         return;
     }
     case exp_imply: {


     }
     case exp_quantifier: {


     }
     default: {
         snprintf(p,MAX_LINELEN,"not(%s)",buf1); return;
     }
    }
}

/* Logic reverse an expression */
void req_not(void)
{
    int i;
    char *p, *ld;
    char nbuf[MAX_LINELEN+1];
    if(objlinecnt<2) return;
    for(i=1;i<objlinecnt;i++) {
     thisobjline=i; p=find_word_start(objline[i]);
     linelogdir=0; ld="";
     if(*p=='>') {
         if(logdir<0) continue;
         ld=">"; p=find_word_start(p+1); linelogdir=1;
     }
     else if(*p=='<') {
         if(logdir>0) continue;
         ld="<"; p=find_word_start(p+1); linelogdir=-1;
     }
     thislinelen=strlen(p); if(thislinelen<=0) continue;
     snprintf(nbuf,sizeof(nbuf),"%s",p);
     _not(nbuf); printf("%s %s\n",ld, nbuf);
    }
}
