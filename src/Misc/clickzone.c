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

/* Check the zone of click in an image */

/* Return value: -1 if no condition is met.
 * n>0 for the line of condition meeting the click.
 * empty if condition has error.
 */

/***************** Nothing should need change hereafter *****************/

#include <gd.h>
#include "../Lib/libwims.h"

#define VLEN 1024

int clickx, clicky;
int prec;
double v[VLEN+2];
char namebuf[2048];
char oldfile[1024]="";
char filebase[10240];
gdImagePtr image=NULL;
int testcolor;

void fbase(void)
{
  char *p, *m, *p1, *p2;
  p=getenv("w_insdraw_filebase");
  m=getenv("module_dir"); if(m==NULL) m="";
  if(strncmp(m,"modules/",strlen("modules/"))!=0) {
    snprintf(filebase,sizeof(filebase),"gifs"); return;
  }
  if(p==NULL || *p==0) {
    if(strncmp(m,"modules/adm/",strlen("modules/adm"))==0)
      snprintf(filebase,sizeof(filebase),"%s/insdraw gifs",m);
    else
      snprintf(filebase,sizeof(filebase),"%s gifs",m);
    return;
  }
  for(p1=find_word_start(p); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    if(strstr(p1,"..")!=NULL) continue;
    snprintf(filebase+strlen(filebase),
           sizeof(filebase)-strlen(filebase),
           " %s/%s",m,p1);
  }
  snprintf(filebase+strlen(filebase),
         sizeof(filebase)-strlen(filebase),
         " gifs");
}

/* File opening: with security */
FILE *open4read(char *n)
{
  char *p, *p1, *p2;
  int t;
  FILE *f;
  n=find_word_start(n);
  if(*n==0) return NULL;
  p=filebase;
  p1=n+strlen(n)-4;if(p1<n || strcasecmp(p1,".gif")!=0) t=1; else t=0;
  if(p!=NULL && *p!=0) {
    char pbuf[MAX_LINELEN+1];
    snprintf(pbuf,sizeof(pbuf),"%s",p);
    p=find_word_start(pbuf); if(strstr(p,"..")!=NULL) return NULL;
    if(*n=='/' || strstr(n,"..")!=NULL) return NULL;
/* prohibit unusual file/dir names */
    for(p1=p;*p1;p1++)
      if(!isalnum(*p1) && !isspace(*p1) && strchr("~_-/.",*p1)==NULL)
        return NULL;
    for(p1=n;*p1;p1++)
      if(!isalnum(*p1) && !isspace(*p1) && strchr("~_-/.",*p1)==NULL)
        return NULL;
    f=NULL;
    for(p1=p; *p1; p1=find_word_start(p2)) {
      if(*p1=='/') return NULL;
      p2=find_word_end(p1);
      if(*p2) *p2++=0;
      snprintf(namebuf,sizeof(namebuf),"%s/%s",p1,n);
      f=fopen(namebuf,"r"); if(f!=NULL) goto imgtype;
    }
    p1=getenv("w_wims_session");
    if(p1!=NULL && strstr(p1,"..")==NULL && strncmp(n,"insert",6)==0) {
      snprintf(namebuf,sizeof(namebuf),"../s2/%s/%s",p1,n);
      f=fopen(namebuf,"r");
    }
  }
  else {
    snprintf(namebuf,sizeof(namebuf),"%s",n);
    f=fopen(namebuf,"r");
  }
  imgtype:
  if(t && f!=NULL) {
    char tbuf[1024],sbuf[4096];
    fclose(f); f=NULL;
    p1=getenv("TMPDIR"); if(p1==NULL || *p1==0) p1=".";
    snprintf(tbuf,sizeof(tbuf),"%s/drawfile_.gif",p1);
    snprintf(sbuf,sizeof(sbuf),"magick %s %s",namebuf,tbuf);
    if (system(sbuf)) fprintf(stderr,"system failed");
      f=fopen(tbuf,"r");
  }
  if(f!=NULL) snprintf(oldfile,sizeof(oldfile),"%s",n);
  return f;
}

int getvalue(char *p, int n)
{
  int t;
  char *p1;
  if(n<0) n=VLEN;
  for(t=0; t<n; t++,p=p1) {
    p1=find_item_end(p); if(*p1) *p1++=0;
    p=find_word_start(p);
    if(*p==0) break;
    v[t]=strevalue(p);
    if(!isfinite(v[t])) exit(-1);
  }
  return t;
}

/* test one condition. Returns 1 if met. */
int test(char *p)
{
  char *p1;
  int i,t;
  double d;
  p=find_word_start(p); p1=find_item_end(p);
  if(*p1) *p1++=0; else exit(-1);
  prec=0;
  *p=tolower(*p);
  if(strncasecmp(p,"polygon",3)==0 || *p=='g') {
    int cross;
    double x0,y0,a1,b1,c1,a2,b2,c2,s1,s2,s3,s4;
    t=getvalue(p1,-1); if(t<6) exit(-1);
    cross=0; x0=-19; y0=-19;
    a1=clicky-y0;b1=x0-clickx;c1=clickx*y0-x0*clicky;
    v[t]=v[0];v[t+1]=v[1];
    for(i=2;i<t+2;i+=2) {
      a2=v[i+1]-v[i-1];b2=v[i-2]-v[i];
      c2=v[i]*v[i-1]-v[i-2]*v[i+1];
      s1=a1*v[i-2]+b1*v[i-1]+c1;
      s2=a1*v[i]+b1*v[i+1]+c1;
      s3=a2*x0+b2*y0+c2;
      s4=a2*clickx+b2*clicky+c2;
      if(s1==0) continue;
      if(s1*s2<=0 && s3*s4<=0) cross++;
    }
    return cross&1;
  }
  if(*p=='p' || strncasecmp(p,"point",3)==0) {
    t=getvalue(p1,-1); if(t<2) exit(-1);
    for(i=0;i<t;i+=2) {
      d=sqrt(pow(v[i]-clickx,2)+pow(v[i+1]-clicky,2));
      if(d<=4) return 1;
      if(d<=7) {prec=1; return 1;}
    }
    return 0;
  }
  if(strncasecmp(p,"rectangle",1)==0) {
    double x1,x2,y1,y2;
    if(getvalue(p1,4)!=4) exit(-1);
    x1=min(v[0],v[2]); x2=max(v[0],v[2]);
    y1=min(v[1],v[3]); y2=max(v[1],v[3]);
    if(clickx<x1 || clickx>x2 || clicky<y1 || clicky>y2) return 0;
    else return 1;
  }
  if(strncasecmp(p,"circle",1)==0) {
    double dist;
    if(getvalue(p1,3)!=3 || v[2]<0) exit(-1);
    dist=sqrt(pow(v[0]-clickx,2)+pow(v[1]-clicky,2));
    if(dist>v[2]/2) return 0; else return 1;
  }
  if(strncasecmp(p,"ellipse",1)==0) {
    double dist;
    if(getvalue(p1,4)!=4) exit(-1);
    if(v[2]<=0 || v[3]<=0) exit(-1);
    dist=sqrt(pow(2*(v[0]-clickx)/v[2],2)+pow(2*(v[1]-clicky)/v[3],2));
    if(dist>1) return 0; else return 1;
    return 0;
  }
  if(strncasecmp(p,"bound",1)==0) {
    char *p2;
    FILE *f;
    int c, T;
    p2=find_item_end(p1); if(*p2) *p2++=0;
    T=getvalue(p2,2); if(T!=2 && T!=0) exit(-1);
    p1=find_word_start(p1); *find_word_end(p1)=0;
    if(*p1==0) exit(-1);
    if(strcmp(p1,oldfile)!=0) {
      if(image) gdImageDestroy(image);
      f=open4read(p1); if(f==NULL) exit(-1);
      image=gdImageCreateFromGif(f); fclose(f);
      if(T==0) {
        testcolor=gdImageGetPixel(image,1,1);
      }
      else {
        testcolor=gdImageColorAllocate(image,2,3,5);
        gdImageFill(image,clickx,clicky,testcolor);
      }
    }
    if(T==0) {
      c=gdImageGetPixel(image,clickx,clicky);
      if(c!=testcolor) return 1;
    }
    else {
      c=gdImageGetPixel(image,v[0],v[1]);
      if(c==testcolor) return 1;
    }
    return 0;
  }
  exit(-1);
}

static
int oneline(char *p)
{
  char *p1, *p2;
  int t, rev;

  if(strparchr(p,'|')!=NULL) {
    t=0; for(p1=p;p1;p1=p2) {
      p2=strparchr(p1,'|'); if(p2!=NULL) *p2++=0;
      t|=oneline(p1);
    }
    return t;
  }
  if(strparchr(p,'&')!=NULL) {
    t=1; for(p1=p;p1;p1=p2) {
      p2=strparchr(p1,'&'); if(p2!=NULL) *p2++=0;
      t&=oneline(p1);
    }
    return t;
  }
  p1=find_word_start(p); rev=0;
  if(*p1=='^') {
    rev=1; p1=find_word_start(++p1);
  }
  if(*p1==0) return rev^1;
  if(*p1=='(') {
    p1++; p2=find_matching(p1,')');
    if(p2==NULL) exit(-1);
    *p2=0; return oneline(p1)^rev;
  }
    return test(p1)^rev;
}

/* Returns the number of lines matching the click coordinates */
int main()
{
  char *p, *p2, *p3;
  int i,j;

  fbase();
  p=getenv("wims_exec_parm"); if(p==NULL || *p==0) return -1;
  p=find_word_start(p); p2=strchr(p,'\n');
  if(p2==NULL) p2=p+strlen(p); else *p2++=0;
  p3=strchr(p,','); if(p3==NULL) return -1; else *p3++=0;
  clickx=atoi(p); clicky=atoi(p3);
  for(i=1,p=find_word_start(p2);*p;i++, p=find_word_start(p2)) {
    fprintf(stderr,"Line %d.\n",i);
    p2=strchr(p,'\n'); if(p2==NULL) p2=p+strlen(p); else *p2++=0;
    if(*p==0) continue;
    j=oneline(p);
    if(j) {printf("%d %d",i,prec); return 0;}
  }
  printf("-1"); return 0;
}
