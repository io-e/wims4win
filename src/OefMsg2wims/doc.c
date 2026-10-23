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

 /* Document processing primitives. */
#include "../Lib/libwims.h"
#include "msg2wims.h"

char primbuf[MAX_LINELEN+1];
char *primitive[256];
int primcnt, prepcnt;

char *_prim_if(char *p, int type)
{
    char *p1, *p2, *p3, *p4, *p5, *p6;
    char buf[MAX_LINELEN+1];
    p1=find_word_start(p); if(*p1!='{') return p;
    p2=find_matching(p1+1,'}'); if(p2==NULL) return p;
    p3=find_word_start(p2+1); if(*p3!='{') return p;
    p4=find_matching(p3+1,'}'); if(p4==NULL) return p;
    *p2=0; snprintf(buf,sizeof(buf),"%s",p1+1); substdoc(buf);
    prepcnt=0; parmprep(buf,pt_text);
    switch(type) {
          case 0: fprintf(outf," \n!if %s \n",buf); break;
          case 1: fprintf(outf," \n!ifval %s \n",buf);
    }
    p5=find_word_start(p4+1);
    if(*p5=='{' && (p6=find_matching(p5+1,'}'))!=NULL) {
      *p4=elsechar; *p5=' '; *p6=endifchar;
    }
    else *p4=endifchar;
    return p3+1;
}

char *prim_if(char *p){ return _prim_if(p, 0); }
char *prim_ifval(char *p){ return _prim_if(p, 1);}

char *prim_while(char *p)
{
    char *p1, *p2, *p3, *p4;
    char buf[MAX_LINELEN+1];
    p1=find_word_start(p); if(*p1!='{') return p;
    p2=find_matching(p1+1,'}'); if(p2==NULL) return p;
    p3=find_word_start(p2+1); if(*p3!='{') return p;
    p4=find_matching(p3+1,'}'); if(p4==NULL) return p;
    *p2=0; snprintf(buf,sizeof(buf),"%s",p1+1); substdoc(buf);
    prepcnt=0; parmprep(buf,pt_text);
    fprintf(outf," \n!while %s \n",buf);
    *p4=whilechar;
    return p3+1;
}

char *prim_for(char *p)
{
    char *p1, *p2, *p3, *p4;
    char buf[MAX_LINELEN+1];
    p1=find_word_start(p); if(*p1!='{') return p;
    p2=find_matching(p1+1,'}'); if(p2==NULL) return p;
    p3=find_word_start(p2+1); if(*p3!='{') return p;
    p4=find_matching(p3+1,'}'); if(p4==NULL) return p;
    *p2=0; snprintf(buf,sizeof(buf),"%s",p1+1); substdoc(buf);
    fprintf(outf," \n!for m_%s \n",find_word_start(buf));
    *p4=nextchar;
    return p3+1;
}

/* check whether the name is a document primitive. */
/* for the moment, only def define if ifval for while or as in "msg2wims_primitives"
 * description of primitive must be in primitive_dir="docu/primitives"
 * msg2wims_primitives is defined in two places:
 * public_html/modules/adm/doc/var.proc and public_html/scripts/docu/mkindex
 * should be elsewhere at least for primitives defined here
 * that is "def define if ifval for while"
 */

char *doccheck(char *p)
{
    char *pe, *pl, *pv, *pp, namebuf[128], parbuf[8192];
    int i, k, t;

    for(pe=p, i=0; isalnum(*pe) && i<sizeof(namebuf)-1; pe++, i++)
      namebuf[i]=*pe;
    namebuf[i]=0; pe=find_word_start(pe);
    k=search_list(primitive,primcnt,sizeof(primitive[0]),namebuf);
    if(k<0) return p;
    if(strcmp(namebuf,"def")==0 || strcmp(namebuf,"define")==0) {
      char parmbuf[MAX_LINELEN+1];
      pl=find_word_start(pe);
      if(*pl=='{') pl=find_word_start(++pl); else return pe;
      pv=find_matching(pl,'}'); if(pv==NULL) return pe;
      if(pv-pl>=sizeof(parmbuf)-1) return pe;
      memmove(parmbuf,pl,pv-pl); parmbuf[pv-pl]=0;
      def(parmbuf);
      pe=pv+1; return pe;
    }
    if(strcmp(namebuf,"if")==0) return prim_if(pe);
    if(strcmp(namebuf,"ifval")==0) return prim_ifval(pe);
    if(strcmp(namebuf,"for")==0) return prim_for(pe);
    if(strcmp(namebuf,"while")==0) return prim_while(pe);
    fprintf(outf,"\n!read primitives.phtml %d, %s",primserial++,namebuf);
    for(t=0;t<16;t++) {
      pl=find_word_start(pe);
      if(*pl=='{') pl=find_word_start(++pl); else break;
      pv=find_matching(pl,'}'); if(pv==NULL) break;
      if(pv-pl>=sizeof(parbuf)-1) break;
      memmove(parbuf,pl,pv-pl); parbuf[pv-pl]=0;
      for(pp=parbuf; *pp; pp++) {
          if(*pp=='	' || *pp=='$') *pp=' ';
          if(*pp=='\n') *pp='	';
      }
      fprintf(outf,", %s",parbuf);
      pe=pv+1;
    }
    fprintf(outf," \n");
    return pe;
}

