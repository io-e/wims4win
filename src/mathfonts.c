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

/* fonts which should have align=middle */
char *middle_fonts=",y,beta,eta,mu,phi,rho,varphi,psi,\
le,leq,ge,geq,pm,ne,neq,\
subseteq,supseteq,wp,\
int,oint,Vert,\
QQ,\
leftpar2,leftpar3,leftpar4,leftpar5,leftpar6,\
leftpar7,leftpar8,leftpar9,leftpar10,leftpar11,leftpar12,\
rightpar2,rightpar3,rightpar4,rightpar5,rightpar6,\
rightpar7,rightpar8,rightpar9,rightpar10,rightpar11,rightpar12,\
leftbrace2,leftbrace3,leftbrace4,leftbrace5,leftbrace6,\
leftbrace7,leftbrace8,leftbrace9,leftbrace10,leftbrace11,leftbrace12,\
rightbrace2,rightbrace3,rightbrace4,rightbrace5,rightbrace6,\
rightbrace7,rightbrace8,rightbrace9,rightbrace10,rightbrace11,rightbrace12,";

/* These are temporary definitions. */
char *mathfont_dir="mathfonts";

/* tex font size list */
int texsize_list[]={63,69,76,83,91,100,109,120,131,144,158,173};
int texbasesize=6; /* defaults to 109 */
int usertexsize=-1; /* non-negative if defined */
#define texsize_no (sizeof(texsize_list)/sizeof(texsize_list[0]))

int current_tex_size;

void fix_tex_size(void)
{
  char buf[64],*p; int i,ts;
  ts=texbasesize;
  if(usertexsize>=0) ts=usertexsize+1;
  if(getwimstexsize) p=getvar("wims_texsize"); else p=NULL;
  if(p!=NULL && *p!=0) {
    i=evalue(p);
    if(i>=-5 && i<=5) ts+=i;
  }
  if(ts<0) ts=0;
  if(ts>=texsize_no) ts=texsize_no-1;
  current_tex_size=texsize_list[ts];
  snprintf(buf,sizeof(buf),"%d",current_tex_size);
  setenv("texgif_density",buf,1);
}

/* takes math font image file string */
char *mathfont(char *fontname)
{
  char buf1[MAX_LINELEN+1],buf2[MAX_LINELEN+1];
  struct stat st;

  if(fontname[0]=='\\') fontname++;
  else if(strncmp(fontname,mathfont_prefix,strlen(mathfont_prefix))==0)
    fontname+=strlen(mathfont_prefix);
  if(strlen(fontname)==1 /* && strchr("xyz",fontname[0])==NULL */) {
    char c=fontname[0];
    if(!isalpha(c)) { // c is not a letter
      buf2[0]=c;buf2[1]=0;
   }
   else {
// case of variable xyz in particular
    char *pp;
    if(c=='f') pp="&nbsp;"; else pp="";
    snprintf(buf2,sizeof(buf2),"<i>%c</i>%s",c,pp);
//FIXME-> should we arrive here in mathml case ???
    }
  }
  else {/* interpret font with at least two caracters as \RR \calB - only outside of math environment
    not useful in mathml*/
    char *underscore;
    fix_tex_size(); underscore="";
    {
      char *p, *p2;
      for(p=fontname; *p && !isupper(*p); p++);
/* verify positionning of capital letter */
      if(*p && p>fontname && *(p+1)!=0) return NULL;
      if(p==fontname && (fontname[1]!=*p || strlen(fontname)!=2)) {
        for(p2=p+1; *p2 && !isupper(*p2); p2++);
        if(*p2) return NULL;
      }
      if(*p) underscore="_";
    }
    snprintf(buf1,sizeof(buf1),"%s/%d/%s%s.gif",
           mathfont_dir,current_tex_size,fontname,underscore);
    if(*underscore && stat(buf1,&st)!=0)
      snprintf(buf1,sizeof(buf1),"%s/%d/%s.gif",
             mathfont_dir,current_tex_size,fontname);

    if(stat(buf1,&st)!=0) return NULL;
    snprintf(buf2,sizeof(buf2),",%s,",fontname);
    if(strstr(middle_fonts,buf2)!=NULL)
      snprintf(buf2,sizeof(buf2),
             "%s<img src=\"%s%s\" style=\"vertical-align: middle; border:none\" \
alt=\"%s\">%s",
             mathalign_sup1,ref_base,buf1,fontname,mathalign_sup2);
    else
       snprintf(buf2,sizeof(buf2),"<img src=\"%s%s\" style=\"margin:0px; border:none\" \
alt=\"%s\">",
             ref_base,buf1,fontname);
  }
  force_setvar("wims_mathfont_",buf2); return getvar("wims_mathfont_");
}
