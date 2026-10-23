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

/* Interface Plouffe's Inverter to wims */

/*************** Customization: change values hereafter ****************/

/* limit of parameter string */
#define parmlim 131072
/* Address of Plouffe's Inverter */
#define WebPlouffe "<a target=wims_external href=\"http://oldweb.cecm.sfu.ca/cgi-bin/isc/lookup?lookup_type=simple&number=%s\">"

/* minimal and maximal lengths of a number to interface */
#define Plouffe_min 6
#define Plouffe_max 22

char *langs[]={"en","fr","cn"};
#define lang_no (sizeof(langs)/sizeof(langs[0]))
#pragma GCC diagnostic push
#ifdef __clang__
#pragma GCC diagnostic ignored "-Winvalid-source-encoding"
#endif
char *post_msg[lang_no]={
  " <small>(click on a value to check its meaning in another window)</small> ",
  " <small>(cliquez sur une valeur pour voir sa signification dans une autre fen&ecirc;tre)</small> ",
  " <small>(在数字上单击可供你在另一视窗内查阅其意义)</small> "
};
#pragma GCC diagnostic pop
/***************** Nothing should need change hereafter *****************/

#include "../includes.h"

char *parm;

int getlang(void)
{
  char *p;
  int i;
  p=getenv("w_module_language");
  if(p==NULL || *p==0) p=getenv("w_lang");
  if(p==NULL || *p==0) return 0; /* English is default */
  for(i=0;i<lang_no && strcmp(p,langs[i])!=0;i++);
  if(i<lang_no) return i; else return 0;
}

int main(int argc,char *argv[])
{
  char *p,*pp,*p2,*p3,*ps,*last;
  char buf[parmlim];
  int i,got;

  parm=getenv("wims_exec_parm");
/* nothing to do if no parameter */
  if(parm==NULL || *parm==0) return 0;
  i=strlen(parm); if(i<0 || i>parmlim) {
    fprintf(stderr,"Plouffe: parameter too long. \n"); exit(1);
  }
  got=0;
  for(p=last=parm;*p!=0;p++) {
    if(*p=='<') {
      for(;*p!=0 && *p!='>'; p++);
      if(*p==0) p--;
      continue;
    }
    if(!isdigit(*p)) continue;
    for(i=0,pp=p;isdigit(*pp) || *pp=='.';pp++) if(*pp=='.') i++;
    if(i>1) {  /* several decimal points. */
      p=pp-1;continue;
    }
    for(p2=pp-1;p2>=p && (*p2=='.' || *p2=='0');p2--);
    p2++;
    for(i=0,p3=p;p3<p2 && *p3!='.';p3++);
    for(i=0,ps=p;ps<p2 && (*ps=='0' || *ps=='.'); ps++);
    if(p3>=p2 || p2<ps+Plouffe_min) {
      p=pp-1;continue;
    }
    memmove(buf,last,p-last);buf[p-last]=0;
    printf("%s",buf);last=p;
    i=p2-p;if(i>Plouffe_max) i=Plouffe_max;
    memmove(buf,p,i);buf[i]=0;
    printf(WebPlouffe,buf);
    memmove(buf,p,pp-p);buf[pp-p]=0;
    printf("%s</a>",buf);last=pp;
    got=1;p=pp-1;
    continue;
  }
  if(*last!=0) printf("%s",last);
  if(got) printf("%s",post_msg[getlang()]);
  return 0;
}

