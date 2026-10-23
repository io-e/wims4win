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
/* Routines to update home directory. */

#include "wimslogd.h"

/* The following directories will be relocatable.
 * Clusterable directories (log, backup) should be avoided.
 */
char *putlist[]={
  "public_html/modules","public_html/modules/devel",
    "public_html/modules/local",
    "public_html/bases","public_html/bases/doc",
    "other"
};
#define putcnt (sizeof(putlist)/sizeof(putlist[0]))

void homedir(void)
{
  char buf[MAX_LINELEN+1], fname[4096];
  char *p1;
  struct stat st;
  int i,t;
  for(i=0;i<putcnt;i++) {
    t=stat(putlist[i],&st); if(t || !S_ISDIR(st.st_mode)) continue;
    snprintf(fname,sizeof(fname),"%s/.wimshome",putlist[i]);
    wlogdaccessfile(buf,"r","%s",fname);
    p1=find_word_start(buf); *find_word_end(p1)=0;
    if(strcmp(p1,cwd)==0) continue;
    wlogdaccessfile(cwd,"w","%s",fname);
    chmod(fname,S_IRUSR|S_IWUSR);
  }
}

