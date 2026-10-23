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

/* Mathematical expression manipulations for WIMS */

#include "mathexp.h"

char parmbuf[MAX_LINELEN+1];
char *objline[MAX_OBJLINES];
int objlinecnt, thisobjline, thislinelen;
int expl1, expl2;
char *reqtype;
int logdir, linelogdir;

regex regexchk[MAX_REGEX];

int regexcnt=0;

struct {
    char *name;
    void (*routine)(void);
} req[]={
    {"cut",		req_cut},
    {"extract",		req_extract},
    {"not",		req_not},
    {"reverse",		req_not},
    {"type",		req_type},
};
int request;
#define reqcnt (sizeof(req)/sizeof(req[0]))

void parm()
{
    char *p, *pp;
    for(pp=parmbuf;*pp;pp++) if(*pp=='	') *pp='\n';
    for(objlinecnt=0,p=parmbuf; *p && objlinecnt<MAX_OBJLINES; p=pp) {
	pp=strchr(p,'\n'); if(pp) *pp++=0; else pp=p+strlen(p);
	strip_trailing_spaces(p);
	objline[objlinecnt]=p; objlinecnt++;
    }
    if(objlinecnt<1) error("Empty input data.");
    logdir=0;
    if((p=wordchr(objline[0],"<"))!=NULL) {
	logdir=-1; ovlstrcpy(p,p+1);
    }
    if((p=wordchr(objline[0],">"))!=NULL) {
	logdir=1; ovlstrcpy(p,p+1);
    }
    p=find_word_start(objline[0]); pp=find_word_end(p); if(*pp) *pp++=0;
    objline[0]=pp;
    for(request=0;request<reqcnt && strcasecmp(req[request].name,p)!=0; request++);
    if(request>=reqcnt) error("Bad request.");
    p=find_word_start(pp); pp=find_word_end(p); if(*pp) *pp++=0;
    objline[0]=pp; reqtype=p;
}

int main()
{
    char *p;
    int i;
    string_modify=string_modify1;
    p=getenv("wims_exec_parm");
    if(p==NULL || *p==0) error("No input data.");
    snprintf(parmbuf,sizeof(parmbuf),"%s",p);
    parm();
    req[request].routine();
    for(i=0;i<regexcnt;i++) if(regexchk[i].isvar==0) regfree(&(regexchk[i].cmpreg));
    return 0;
}

