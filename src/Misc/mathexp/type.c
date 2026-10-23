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

/* the type of an expression, with cutpoints */
void req_type(void)
{
    int i,l;
    char *p;
    int commas[MAX_COMMAS];
    int commacnt;
    if(objlinecnt<2) return;
    while((p=wordchr(objline[0],"nocomma"))!=NULL) {
	nocomma=1; ovlstrcpy(p,p+strlen("nocomma"));
    }
    for(i=1;i<objlinecnt;i++) {
	thisobjline=i; p=find_word_start(objline[i]);
	linelogdir=0;
	if(*p=='>') {
	    if(logdir<0) continue;
	    p=find_word_start(p+1); linelogdir=1;
	}
	else if(*p=='<') {
	    if(logdir>0) continue;
	    p=find_word_start(p+1); linelogdir=-1;
	}
	thislinelen=strlen(p); if(thislinelen<=0) continue;
	l=_type(p,commas,&commacnt); printf("%s\n",typenames[l]);
    }

}

