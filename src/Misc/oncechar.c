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

/* This special program selects words composed by
 * selected characters, each selected character being used
 * at most once in the word
 */

/* Selected characters are entered by the env var 'oncechar'.
 * Words entered by stdin. Output to stdout.
 */

#include "../Lib/libwims.h"

#define MAX_WORDLEN 1023
char wbuf[MAX_WORDLEN+1];
char selbuf[256];

int getword(void)
{
  int t,c;

  do c=getchar(); while(isspace(c));
  for(t=0; t<MAX_WORDLEN && isalnum(c); t++, c=getchar()) wbuf[t]=c;
  wbuf[t]=0; return c;
}

void checkword(void)
{
  char sbuf[256];
  char *p, *pt;
  memmove(sbuf,selbuf,sizeof(sbuf));
  for(p=wbuf; *p; p++) {
    pt=strchr(sbuf,*p); if(pt==NULL) return;
    else *pt=' ';
  }
  printf("%s\n",wbuf);
}

int main(int argc, char *argv[])
{
  int c;
  char *p;

  p=getenv("oncechar"); if(p==NULL || *p==0) return 0;
  snprintf(selbuf,sizeof(selbuf),"%s",p);
  for(p=selbuf; *p; p++)
    if(isspace(*p) || strchr("^?*.${}[]()\\",*p)!=NULL) ovlstrcpy(p,p+1);
  do{
    c=getword(); checkword();
  }
  while(c!=EOF);
  return 0;
}

