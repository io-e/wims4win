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

/* Subroutines to manipulate math expressions */
#include "libwims.h"

/* Points to the start of a mathematics variable (or number) */
char *find_mathvar_start(char *p)
{
  int i;
  for(i=0;!isalnum(*p) && *p!='.' && *p!=0 && i<MAX_LINELEN; p++,i++);
  return p;
}

/* Points to the end of a mathematics variable (or number) */
char *find_mathvar_end(char *p)
{
  int i; char *pp;
  if(!isalnum(*p) && *p!='.') return p;
  if(isalpha(*p)) {
    for(i=0; *p!=0 && (isalnum(*p) || *p=='.' || *p=='\'')
        && i<MAX_LINELEN;p++,i++);
    return p;
  }
  else {
    int t=0;
    expon:
    for(i=0; (isdigit(*p) || *p=='.') && i<MAX_LINELEN;p++,i++);
    pp=p; if(t) return pp;
    while(isspace(*p)) p++;
    if(*p=='e' || *p=='E'){
      t=1; p++; while(isspace(*p)) p++;
      if(isdigit(*p)) goto expon;
      if((*p=='-' || *p=='+') && isdigit(*(p+1))) {
        p++; goto expon;
      }
    }
    return pp;
  }
}

/* list variable (function) names in an expression.
 * buffer is modified to contain the list.
 */
void mathvarlist(char *p)
{
  char buf[MAX_LINELEN+1], *pb, *pe, *pp;
  char *l[10240];
  int i,j,len, nofn=0;

  pb=find_word_start(p);pe=find_word_end(pb);
  if(pe-pb==strlen("nofn") && memcmp(pb,"nofn",strlen("nofn"))==0) {
    nofn=1; pb=find_word_start(pe);
  }
  snprintf(buf,sizeof(buf),"%s",pb);
  len=strlen(buf);
  for(i=0,pb=buf;pb<buf+len;pb++) {
    if(!isalpha(*pb)) continue;
    if(pb>buf && isalnum(*(pb-1))) {
      pb=find_mathvar_end(pb); continue;
    }
    pe=find_mathvar_end(pb); pp=find_word_start(pe);
    if(nofn && *pp=='(') {
      pb=pe; continue;
    }
    *pe=0;
    if(i<10240) {
      for(j=0;j<i;j++) if(strcmp(pb,l[j])==0) goto nextp;
      l[i++]=pb;
    }
    nextp:
    pb=pe;
  }
  for(*p=0,j=0;j<i;j++) {
    strcat(p,l[j]);
    if(j<i-1) strcat(p,",");
  }
}
