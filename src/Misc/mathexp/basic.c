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

void strcompress(char *src, char *dest)
{
    char *p1, *p2;
    char lastnospc, lastspc;
    p1=find_word_start(src); p2=dest;
    lastnospc=lastspc=0;
    for(;*p1 && p2-dest<MAX_LINELEN;p1++) {
     if(isspace(*p1)) {lastspc=' '; continue;}
     if(lastspc!=0) {
         if((isalnum(lastnospc) || lastnospc=='_') &&
            (isalnum(*p1) || *p1=='_')) *p2++=' ';
     }
     lastspc=0; *p2++=*p1; lastnospc=*p1;
    }
    *p2=0;
}

/* the type of an expression, with cutpoints */
int _type(char *p, int commas[], int *commacnt)
{
    int i,l,lvl;
    char *p1, *p2, *p3, *p4;
    char buf[MAX_LINELEN+1];

    lvl=-1; *commacnt=0;
    for(p1=find_word_start(p), p2=p1; *p2; p2=find_word_start(p3)) {
     if(*p2=='.' || isdigit(*p2)) {
         if(lvl<exp_number) lvl=exp_number;
         (void)strtod(p2,&p3); continue;
     }
     if(*p2=='(') {
         p3=find_matching(p2+1,')');
         if(lvl<exp_paren) lvl=exp_paren;
         paren:
         if(p3==NULL) error("Unmatched parentheses");
         p3++; continue;
     }
     if(*p2=='[') {
         if(lvl<exp_matrix) lvl=exp_matrix;
         p3=find_matching(p2+1,']'); goto paren;
     }
     if(*p2=='{') {
         if(lvl<exp_set) lvl=exp_set;
         p3=find_matching(p2+1,'}'); goto paren;
     }
     if(isalpha(*p2)) {
         for(p3=p2; *p3=='_' || isalnum(*p3); p3++);
         if(p3-p2>=16) goto notdefined;
         memmove(buf,p2,p3-p2); buf[p3-p2]=0;
         for(i=0;i<opalphano && strcmp(buf,opalpha[i].name)!=0; i++);
         if(i<opalphano) {
          l=opalpha[i].lvl;
          if(l>lvl) {*commacnt=0; lvl=l;}
          if(l==lvl && *commacnt<MAX_COMMAS-2) {
              commas[(*commacnt)++]=p2-p;
              commas[(*commacnt)++]=p3-p;
          }
          continue;
         }
         notdefined: p4=find_word_start(p3);
         if(*p4=='(') {
          if(lvl<exp_fn) lvl=exp_fn;
          p3=find_matching(p4+1,')');
          if(p3==NULL) error("Unmatched parentheses.");
          p4++; memmove(buf,p4,p3-p2); buf[p3-p4]=0;
          p3++;
         }
         else {
          if(lvl<exp_variable) lvl=exp_variable;
         }
         continue;
     }
     for(i=0;i<oppunctno && strncmp(p2,oppunct[i].name,strlen(oppunct[i].name))!=0; i++);
     if(i>=oppunctno) error("Unknown operator.");
     p3=p2+strlen(oppunct[i].name); l=oppunct[i].lvl;
     if(l>lvl) {*commacnt=0; lvl=l;}
     if(l==lvl && *commacnt<MAX_COMMAS-2) {
         commas[(*commacnt)++]=p2-p;
         commas[(*commacnt)++]=p3-p;
     }
    }
    return lvl;
}

void getregex(char *p)
{
    char *p1, *p2, *p3;

    p1=find_word_start(p);
    for(regexcnt=0; regexcnt<MAX_REGEX && *p1!=0; p1=find_word_start(p2)) {
     p2=find_word_end(p1); if(*p2) *p2++=0;
     regexchk[regexcnt].srcreg=p1;
     for(p3=p1; *p3 && (isalnum(*p3) || *p3=='.'); p3++);
     if(*p3==0) {
         regexchk[regexcnt].isvar=1; regexcnt++;
     }
     else {
         regexchk[regexcnt].isvar=0;
         if(regcomp(&(regexchk[regexcnt].cmpreg),p1,REG_EXTENDED|REG_ICASE)==0)
           regexcnt++;
     }
    }
}

/* returns 1 if yes, 0 if no. */
int checkregex(char *p)
{
    int i;
    char buf[MAX_LINELEN+1];
    regmatch_t matchbuf[100];

    if(regexcnt<1) return 1; /* nothing to check; always true. */
    strcompress(p,buf);
    for(i=0;i<regexcnt;i++) {     /* all regex words are ANDed. */
     if(regexchk[i].isvar) {
         if(varchr(buf,regexchk[i].srcreg)==NULL) return 0;
     }
     else if(regexec(&(regexchk[i].cmpreg),buf,80,matchbuf,0)!=0) return 0;
    }
    return 1;
}
