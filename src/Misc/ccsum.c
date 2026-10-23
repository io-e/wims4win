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

/* Computes class connection count (unit: student-minutes) */

#include "../Lib/libwims.h"

/* The maximal number of sessions within one day */
#define MAX_SESSIONS (128*1024)
/* The maximal number of classes within one day */
#define MAX_CLASSES 8192
/* At least these minutes will be counted for each session */
#define MIN_CONNECT 1
/* Add this number of minutes to each session */
#define MIN_ADD 1
/* Accounting discontinues after this number of idle minutes */
#define MAX_LAPSE 15

struct {
  char s[4], u[32], cl[20];
  int start, end, cnt;

} ses[MAX_SESSIONS];
int sescnt;

char *datestr;

struct cls {
  char cl[20];
  int cnt;
} cls[MAX_CLASSES];
int clscnt;

/* Read/write to a file with variable parms to print filename */
/* same in Interfaces/common.c and wlogdaccessfile */
static
void accessfile(char *content, char *type, char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  FILE *f;
  int l;

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  f=fopen(buf,type); if(f==NULL) {
    if(*type=='r') content[0]=0;
    return;
  }
  switch(*type) {
    case 'a':
    case 'w': {
      l=strlen(content); fwrite(content,1,l,f); break;
    }
    case 'r': {
      l=fread(content,1,MAX_LINELEN-1,f);
      if(l>0 && l<MAX_LINELEN) content[l]=0;
      else content[0]=0;
      break;
    }
   default: {
      content[0]=0; break;
   }
  }
  fclose(f);
}
/* recursively generate a directory structure */
void mkdirs2(char *s)
{
  struct stat st;
  char *buf;
  if(stat(s,&st)==-1) {
  if(strrchr(s,'/')!=NULL) {
    buf=xmalloc(strlen(s)+1);
    ovlstrcpy(buf,s); *strrchr(buf,'/')=0;
    mkdirs2(buf); free(buf);
  }
  mkdir(s,-1);
  }
}

/* line format is :
YYYYMMDD.HH:MM:SS session IP cmd module (time) login,class
time is displayed only for long request
*/
static
void oneline(char *p)
{
  char tbuf[8], sbuf[8], ubuf[32], cl[20];
  char *p1  /*, *p2*/;
  int i,t;
  memmove(tbuf,p+9,6); tbuf[2]=tbuf[5]=0;
  t=atoi(tbuf)*60+atoi(tbuf+3);
  memmove(sbuf,p+18,4); sbuf[4]=0;
  p1=strchr(p,','); if(p1==NULL) return;
  if(!isdigit(*(p1+1))) return;
  snprintf(cl,sizeof(cl),"%s",p1+1);
  *p1=0; for(p1--;p1>p && !isspace(*(p1-1)); p1--);
  snprintf(ubuf,sizeof(ubuf),"%s",p1);
  for(i=0;i<sescnt;i++) {
    if(memcmp(cl,ses[i].cl,20)==0 && memcmp(sbuf,ses[i].s,4)==0 &&
        ses[i].end>=t-MAX_LAPSE) {
      ses[i].end=t; return;
    }
  }
  if(sescnt>=MAX_SESSIONS) return;
  memmove(ses[sescnt].s,sbuf,4); 
  memmove(ses[sescnt].cl,cl,20);
  ses[sescnt].start=ses[sescnt].end=t;
  snprintf(ses[sescnt].u,sizeof(ses[sescnt].u),"%s",ubuf);
  sescnt++;
}

void onefile(char *fname)
{
  FILE *f;
  long l;
  char *fbuf, *p1, *p2, *p3;
  l=filelength(fname); if(l<=0) return;
  f=fopen(fname,"r"); if(f==NULL) return;
  fbuf=xmalloc(l+16); (void)fread(fbuf,1,l,f); fclose(f); fbuf[l]=0;
  for(p1=fbuf; *p1; p1=p2) {
  p2=strchr(p1,'\n'); if(p2==NULL) p2=p1+strlen(p1); else *p2++=0;
  p3=strchr(p1,','); if(p3==NULL) continue;
  if(strncmp(p1,datestr,8)!=0) continue;
  oneline(p1);
  }
}

void classaccount(void)
{
  int i,j;
  clscnt=0;
  for(i=0;i<sescnt;i++) {
  ses[i].cnt=ses[i].end-ses[i].start+MIN_ADD;
  if(ses[i].cnt<MIN_CONNECT) ses[i].cnt=MIN_CONNECT;
  for(j=0;j<clscnt && ses[i].cl!=cls[j].cl;j++);
  if(j<clscnt) cls[j].cnt+=ses[i].cnt;
  else
    if(clscnt<MAX_CLASSES) {
      memmove(cls[clscnt].cl,ses[i].cl,20);
      cls[clscnt].cnt=ses[i].cnt;
      clscnt++;
    }
  }
}

int clscmp(const void *c1, const void *c2)
{
  struct cls *cl1, *cl2;
  cl1=(struct cls *) c1; cl2=(struct cls *) c2;
  return cl1->cl-cl2->cl;
}

void output(void)
{
  char *p, buf[1024], dbuf[1024];
  int i,t;
  p=getenv("ccsum_outdir"); if(p==NULL || *p==0) return;
  for(i=0;i<sescnt;i++) {
    snprintf(dbuf,sizeof(dbuf),"%s/%s",p,ses[i].cl);
    mkdirs2(dbuf);
    snprintf(buf,sizeof(buf),"%s.%02d:%02d %d\n",
       datestr,ses[i].start/60,ses[i].start%60,ses[i].cnt);
    accessfile(buf,"a","%s/%s",dbuf,ses[i].u);
  }
  snprintf(dbuf,sizeof(dbuf),"%s/bydate/%.4s",p,datestr);
  mkdirs2(dbuf);
  snprintf(dbuf+strlen(dbuf),sizeof(dbuf)-strlen(dbuf),"/%.2s",datestr+4);
  t=0;
  qsort(cls,clscnt,sizeof(cls[0]),clscmp);
  for(i=0;i<clscnt;i++) {
    snprintf(buf,sizeof(buf),"%s %d\n",datestr,cls[i].cnt);
    accessfile(buf,"a","%s/%s/.total",p,cls[i].cl);
    snprintf(buf,sizeof(buf),"%s %s %d\n",datestr+4,cls[i].cl,cls[i].cnt);
    accessfile(buf,"a","%s",dbuf);
    t+=cls[i].cnt;
  }
  snprintf(buf,sizeof(buf),"%s %d %d\n",datestr,t,(t+30)/60);
  accessfile(buf,"a","%s/done",p);
}

int main(int argc, char *argv[])
{
  sescnt=0;
  if(argc<2) return 1;
  datestr=getenv("ccsum_date");
  if(datestr==NULL || strlen(datestr)!=8) return 2;
  onefile(argv[1]);
  classaccount();
  output();
  return 0;
}

