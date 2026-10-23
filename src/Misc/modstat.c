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

/* This is an internal program,
 * used to show statistics of frequentation, module by module. */

#include "../Lib/libwims.h"

#define MAX_FLEN 102400
#define MAX_LANGS MAX_LANGUAGES /*#define MAX_LANGS   16*/
char mbuf[MAX_LINELEN+1];
char mbase[MAX_LINELEN+1];
char indexfile[MAX_LINELEN+1];
char modify_time[1024];
char vbuf[MAX_LINELEN+1];
char *tlist, *mlist, *slist; /* fields, modules, sqled modules */
char *langs; /* site languages */
char language[MAX_LANGS][4];

int start,end,mstart,mend,modtype,reqs,sites;
int languagecnt=0;
int count[MAX_LANGS],tcount;

void getlangs(void)
{
  char *p;

  langs=getenv("w_wims_site_languages"); if(langs==NULL) langs="";
  for(p=langs;strlen(p)>=2 && languagecnt<MAX_LANGS;
      p=find_word_start(find_word_end(p))) {
    if(!isalpha(*p) || !isalpha(*(p+1))) continue;
    memmove(language[languagecnt],p,2);
    language[languagecnt][2]=0;
    languagecnt++;
  }
}

int onefile(char *name)
{
  FILE *f;
  char *buf, *p, *pe, *p2, *p3;
  long int len,len2;
  int s,i,j,k,t,u;

  f=fopen(name,"r"); if(f==NULL) return 0;
  fseek(f,0,SEEK_END);len=ftell(f);
  if(len<=0 || len>MAX_FLEN) return 0;
  fseek(f,0,SEEK_SET);buf=xmalloc(len+1);
  len2=fread(buf,1,len,f);
  if(len2<=0 || len2>len) {
    free(buf); return 0;
  }
  buf[len2]=0; fclose(f); s=t=u=0;
  for(p=buf;p!=NULL && p<buf+len2;p=pe) {
    p=find_word_start(p); pe=strchr(p,'\n');
    if(pe!=NULL) *pe++=0;
    p2=find_word_end(p);
    if(*p2!=0) *p2++=0;
    p2=find_word_start(p2);
    p3=find_word_start(find_word_end(p2));
    *find_word_end(p3)=0;
    if(*p!=0) i=atoi(p); else i=0;
    if(*p2!=0) j=atoi(p2); else j=0;
    if(*p3!=0) k=atoi(p3); else k=0;
    if(t==0 && i<mstart) mstart=i;
      if(i>=start && i<=end) {s+=j; u+=k;}
      t=1;
  }
  reqs+=s; sites+=u;
  if(modtype==3) return u;
  else return s;
}

void onemodule(char *mod)
{
  char ibuf[MAX_LINELEN+5];
  int i,k,sum;

  sum=reqs=sites=0;mstart=end;
  k=onefile(mod);sum+=k;
  for(i=0;i<languagecnt;i++) {
    snprintf(ibuf,sizeof(ibuf),"%s.%s",mod,language[i]);
    k=onefile(ibuf);sum+=k;
    count[i]=k;
  }
  if(modtype!=2) tcount=sum;
  else {
    if(sites>0) tcount=(double) (100*reqs/sites+0.5);
    else tcount=0;
  }
}

int main()
{
  char mmbuf[MAX_LINELEN+1];
  char *p1, *p2, *pp;
  int i;

  mlist=getenv("ll");
  if(mlist==NULL || *mlist==0) return 1;
  pp=getenv("start"); if(pp==NULL || *pp==0) return 1;
  start=atoi(pp);
  pp=getenv("end"); if(pp==NULL || *pp==0) return 1;
  end=atoi(pp);
  pp=getenv("w_module_type");
  if(pp==NULL || (*pp!='2' && *pp!='3')) modtype=1; else modtype=*pp-'0';
  getlangs();
  for(p1=find_word_start(mlist);*p1!=0 && !isspace(*p1); p1=find_word_start(p2)) {
    p2=find_word_end(p1);
    if(p2-p1>MAX_LINELEN) continue;
    memmove(mmbuf,p1,p2-p1);mmbuf[p2-p1]=0;
    onemodule(mmbuf);
    printf("%d %d %s",tcount,mstart,mmbuf);
    for(i=0;i<languagecnt;i++) printf(" %d",count[i]);
    printf("\n");
  }
  return 0;
}

