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

/* Compare two curves */
/* Input parameters: environment.
 *
 * w_curvecomp_1 and w_curvecomp_2: curves to compare, as lists of points.
 * w_curvecomp_xrange and w_curvecomp_yrange: list of 2 integers each.
 * w_curvecomp_tolerance: Maximal tolerance of distances.
 *
 * Output: 10 double numbers separated by white spaces.
 *
 * Number 1: Average distance of curve 1 with respect to curve 2.
 * Number 2: Average distance of curve 2 with respect to curve 1.
 * Number 3: Maximal distance of curve 1 with respect to curve 2.
 * Number 4: Maximal distance of curve 2 with respect to curve 1.
 * Number 5: Proportion of curve 1 close to curve 2.
 * Number 6: Proportion of curve 2 close to curve 1.
 * Number 7: Maximal jump of curve 1.
 * Number 8: Maximal jump of curve 2.
 * Number 9: Ratio of repetitions found in curve 1.
 * Number 10: Ratio of repetitions found in curve 2.
 * Furthermore, words "fnofx" and/or "fnofy" will appear if curve 2
 *   represents the graph of a function of x (and/or y).
 *
 * Returns empty if one of the curves is degenerated.
*/

/*************** Customization: change values hereafter ****************/

/* limit of point buffers */
#define pointlim 4096
#define default_tolerance 10

/***************** Nothing should need change hereafter *****************/

#include "../Lib/libwims.h"

char curve1[2*MAX_LINELEN+2], curve2[2*MAX_LINELEN+2];
int bx[2], by[2];
int tol=default_tolerance;
int xfn=0, yfn=0;
int discrete;
double dist1, dist2, max1, max2, ratio1, ratio2, rep1, rep2;
double djump1, djump2;
struct cv {
  int x,y, closest, rep;
  double dist;
} cv1[pointlim], cv2[pointlim];
int points1,points2;

int Abs(int t) {if(t>=0) return t; else return -t;}
int Min(int x,int y) {if(x>y) return y; else return x;}
int Max(int x,int y) {if(x<y) return y; else return x;}

int listbuf[pointlim*2], listcnt;

void reverse(struct cv *cvbuf, int cnt)
{
  int i;
  struct cv cvt;
  for(i=0;i<cnt/2;i++) {
    memmove(&cvt,cvbuf+i,sizeof(cvt));
    memmove(cvbuf+i,cvbuf+(cnt-i-1),sizeof(cvt));
    memmove(cvbuf+(cnt-i-1),&cvt,sizeof(cvt));
  }
}

int str2list(char *p, int lim)
{
  char *p2;
  for(p2=strchr(p,';'); p2; p2=strchr(p2+1,';')) *p2=',';
  for(listcnt=0; p && listcnt<lim; p=p2) {
    p2=strchr(p,','); if(p2!=NULL) *p2++=0;
    p=find_word_start(p); if(*p==0) continue;
    listbuf[listcnt++]=atoi(p);
  }
  return listcnt;
}

int list2curve(struct cv *cvbuf)
{
  int i, j, m, t, st, xx, yy, x2, y2, x3, y3, ll;

  ll=listcnt/2; if(ll<2) return 0;
  xx=listbuf[0]; yy=listbuf[1];
  j=0; if(xx>=bx[0] && xx<=bx[1] && yy>=by[0] && yy<=by[1]) {
    cvbuf[0].x=xx; cvbuf[0].y=yy; j++;
  }
  for(i=1; i<ll && j<pointlim; i++) {
    x2=listbuf[2*i]; y2=listbuf[2*i+1];
    m=Max(Abs(x2-xx),Abs(y2-yy)); if(m<=0) continue;
    if(discrete==1) st=m; else st=1;
    for(t=st; t<=m && j<pointlim; t++) {
      x3=(double) (x2*t+xx*(m-t))/m+0.5;
      y3=(double) (y2*t+yy*(m-t))/m+0.5;
      if(x3>=bx[0] && x3<=bx[1] && y3>=by[0] && y3<=by[1]) {
        cvbuf[j].x=x3; cvbuf[j].y=y3; cvbuf[j].dist=-1;
        cvbuf[j].rep=0; j++;
      }
    }
    xx=x2; yy=y2;
  }
  return j;
}

void compare(void)
{
  int i, j, cl;
  double d, dt;

  d=2*pointlim; cl=-1;
  for(i=0,djump1=1;i<points1-1;i++) {
    dt=sqrt(pow(cv1[i].x-cv1[i+1].x,2)+pow(cv1[i].y-cv1[i+1].y,2));
    if(dt>djump1) djump1=dt;
  }
  for(i=0,djump2=1;i<points2-1;i++) {
    dt=sqrt(pow(cv2[i].x-cv2[i+1].x,2)+pow(cv2[i].y-cv2[i+1].y,2));
    if(dt>djump2) djump2=dt;
  }
  for(i=0;i<points1;i++) {
    for(j=0;j<points2;j++) {
      dt=sqrt(pow(cv2[j].x-cv1[i].x,2)+pow(cv2[j].y-cv1[i].y,2));
      if(dt<d) {d=dt; cl=j;}
      else {dt=(dt-d)/djump2; if(dt>2) j+=dt-1;}
    }
    cv1[i].dist=d; cv1[i].closest=cl;
    if(i<points1)
      d+=sqrt(pow(cv1[i].x-cv1[i+1].x,2)+pow(cv1[i].y-cv1[i+1].y,2))+0.1;
  }
  d=2*pointlim;
  for(i=0;i<points2;i++) {
    for(j=0;j<points1;j++) {
      dt=sqrt(pow(cv1[j].x-cv2[i].x,2)+pow(cv1[j].y-cv2[i].y,2));
      if(dt<d) {d=dt; cl=j;}
      else {dt=(dt-d)/djump1; if(dt>2) j+=dt-1;}
    }
    cv2[i].dist=d; cv2[i].closest=cl;
    if(i<points2)
      d+=sqrt(pow(cv2[i].x-cv2[i+1].x,2)+pow(cv2[i].y-cv2[i+1].y,2))+0.1;
  }
  for(i=1, cl=cv1[0].closest;i<points1;i++) {
    j=cv1[i].closest; if(j!=cl) cv2[j].rep++;
    cl=j;
  }
  for(i=1, cl=cv2[0].closest;i<points2;i++) {
    j=cv2[i].closest; if(j!=cl) cv1[j].rep++;
    cl=j;
  }
  for(i=cl=0; i<points1; i++) if(cv1[i].rep>1) cl+=cv1[i].rep-1;
  rep1=(double) cl/points1;
  for(i=cl=0; i<points1; i++) if(cv2[i].rep>1) cl+=cv2[i].rep-1;
  rep2=(double) cl/points2;
}

void check(void)
{
  int i,j,xret1,yret1,xret2,yret2;
  int xx,yy,xmin,xmax,ymin,ymax;
  double d;
  max1=max2=0;
  for(i=j=0,d=0;i<points1;i++) {
    if(cv1[i].dist<=tol) j++;
    d+=pow(cv1[i].dist,4);
    if(max1<cv1[i].dist) max1=cv1[i].dist;
  }
  ratio1=(double) j/points1; dist1=pow(d/points1,0.25)*1.8;
  for(i=j=0,d=0;i<points2;i++) {
    if(cv2[i].dist<=tol) j++;
    d+=pow(cv2[i].dist,4);
    if(max2<cv2[i].dist) max2=cv2[i].dist;
  }
  ratio2=(double) j/points2; dist2=pow(d/points2,0.25)*1.8;
  xret1=xret2=yret1=yret2=0;
  xmin=xmax=cv2[0].x; ymin=ymax=cv2[0].y;
  for(i=1;i<points2;i++) {
    xx=cv2[i].x; yy=cv2[i].y;
    xret1=Max(xret1,xmax-xx); xret2=Max(xret2,xx-xmin);
    yret1=Max(yret1,ymax-yy); yret2=Max(yret2,yy-ymin);
    xmin=Min(xx,xmin);xmax=Max(xx,xmax);
    ymin=Min(yy,ymin);ymax=Max(yy,ymax);
  }
  if(Min(xret1,xret2)<=2) xfn=1;
  if(Min(yret1,yret2)<=2) yfn=1;
}

void output(void)
{
  printf("%.2f %.2f %.2f %.2f \
%.3f %.3f %.1f %.1f \
%.3f %.3f",
         dist1, dist2, max1, max2,
         ratio1, ratio2, djump1, djump2,
         rep1, rep2);
  if(xfn) printf(" fx");
  if(yfn) printf(" fy");
}

void test(void)
{
  int i;
  for(i=0;i<points1;i++) printf("%d,%d,%d,%.2f\n",
                        cv1[i].x,cv1[i].y,cv1[i].closest,cv1[i].dist);
  printf("\n");
  for(i=0;i<points2;i++) printf("%d,%d,%d,%.2f\n",
                        cv2[i].x,cv2[i].y,cv2[i].closest,cv2[i].dist);
  printf("\n");
}

int main(int argc, char *argv[])
{
  int i;
  char *c1, *c2, *op;
  c1=getenv("w_curvecomp_1"); c2=getenv("w_curvecomp_2");
/* nothing to do */
  if(c1==NULL || c2==NULL || *c1==0 || *c2==0) return 0;
  snprintf(curve1,sizeof(curve1),"%s",c1);
  snprintf(curve2,sizeof(curve2),"%s",c2);
  bx[0]=by[0]=bx[1]=by[1]=-1;
  c1=getenv("w_curvecomp_xrange"); c2=getenv("w_curvecomp_yrange");
  if(c1!=NULL && *c1!=0) {
    str2list(c1,2); for(i=0;i<listcnt;i++) bx[i]=listbuf[i];
  }
  if(c2!=NULL && *c2!=0) {
    str2list(c2,2); for(i=0;i<listcnt;i++) by[i]=listbuf[i];
  }
  op=getenv("w_curvecomp_option"); if(op==NULL) op="";
  if(bx[0]==-1) bx[0]=0;
  if(bx[1]==-1) bx[1]=pointlim;
  if(by[0]==-1) by[0]=0;
  if(by[1]==-1) by[1]=pointlim;
  c1=getenv("w_curvecomp_tolerance");
  if(c1!=NULL && *c1!=0) tol=atoi(c1);
  tol=Min(30,Max(5,tol));

  if(strstr(op,"discrete1")!=NULL) discrete=1; else discrete=0;
  str2list(curve1,pointlim*2); points1=list2curve(cv1);
  if(strstr(op,"discrete2")!=NULL) discrete=1; else discrete=0;
  str2list(curve2,pointlim*2); points2=list2curve(cv2);
  if(points1<2 || points2<2) return 0;
  compare(); check(); output();
  return 0;
}
