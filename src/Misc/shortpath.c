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

 /* Finds the shortest paths linking given points. */

/* fast algorithm is not ready. */

#define MAX_POINTS 32 /* cannot exceed number of bits in long */
#define MAX_DIM 10 /* dimension limit */
#define MAX_SHORT 20 /* limit of registered shortest paths */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <ctype.h>
#include "../config.h"

int  pointcnt,dimension,first,last,count,shortcnt;
/* style = 0: loop to the start
 *    1: arbitrary open path
 *    2: open path with fixed start
 *    3: open path with fixed end
 *    4: open path with fixed start and end */
int style=0;
int fast=0; /* fast algo if 1, no rigor */
int optimize=0;
char *input;
double dist[MAX_POINTS][MAX_POINTS];
double ds[MAX_POINTS];
double coord[MAX_POINTS][MAX_DIM];
double shortest;
double tolerance; /* to compensate floating rounding error in comparison */
double distmin; /* minimum of distance between two points */
double firstlast; /* distance between first and last */
int shpath[MAX_SHORT][MAX_POINTS];
int chain[MAX_POINTS];

/* reverse a subchain */
void reverse(int i, int j)
{
  int k,t;
  for(k=0;k<(j-i+1)/2;k++) {
    t=chain[i+k]; chain[i+k]=chain[j-k];
    chain[j-k]=t;
  }
}

/* move point i to position j */
void reinsert(int i, int j)
{
  int t;
  t=chain[i];
  if(j>i) {
    j--;
    memmove(chain+i,chain+i+1,(j-i)*sizeof(chain[0]));
    chain[j]=t;
  }
  else {
    memmove(chain+j+1,chain+j,(i-j)*sizeof(chain[0]));
    chain[j]=t;
  }
}

int _fcalc(void)
{
  int modif;
  int i,j;

  modif=0;
/* four-configuration optimize */
  for(i=1;i<pointcnt-2;i++) {
    for(j=i+1;j<pointcnt-1;j++) {
        if(dist[chain[i-1]][chain[i]]+dist[chain[j]][chain[j+1]]
           >dist[chain[i-1]][chain[j]]+dist[chain[i]][chain[j+1]]
           +tolerance) {
          reverse(i,j); modif=1;
        }
    }
  }
/* Point relink */
  for(i=1;i<pointcnt-1;i++) {
    double dd;
    int th;
    th=chain[i];
    dd=dist[chain[i-1]][th]+dist[th][chain[i+1]]
      -dist[chain[i-1]][chain[i+1]];
    for(j=0;j<pointcnt-1;j++) {
      if(j==i || j==i-1) continue;
      if(dd>dist[chain[j]][th]+dist[chain[j+1]][th]
           -dist[chain[j]][chain[j+1]]+tolerance) {
        reinsert(i,j+1); modif=1;
      }
    }
  }
  for(i=0;i<pointcnt;i++) shpath[shortcnt][i]=chain[i];
  shortcnt+=modif; return modif;
}

void fastcalc(void)
{
  int i,j;

  for(i=0;i<pointcnt;i++) chain[i]=i;
  shortcnt=1;
  for(i=0,j=1;i<10 && j!=0;i++) j=_fcalc();

  for(shortest=0,i=0;i<pointcnt-1;i++) shortest+=dist[chain[i]][chain[i+1]];
  if(style==0) shortest+=dist[chain[0]][chain[pointcnt-1]];
  for(i=0;i<pointcnt;i++) shpath[0][i]=chain[i];
}

void _calc(int level, double dis, long used, int utab[])
{
  int current[MAX_POINTS];
  double dd,d1,d3,d2,dthis;
  int i;
  long l;

  dd=dis; d1=d2=d3=0;
  if(level<pointcnt-2) {
    int j,b,prec;
    double btab[MAX_POINTS];
    if(level>0) prec=utab[level-1]; else prec=0;
    if(level>2) {
      for(i=0;i<level-2;i++)
        btab[i]=dist[utab[i]][prec];
      if(level>3) {
        d1=ds[level-4]+ds[level-3]+ds[level-2];
        d3=dist[utab[level-4]][utab[level-2]]
          +ds[level-2]
          +dist[prec][utab[level-3]];
        d2=dist[utab[level-4]][prec]
          +dist[prec][utab[level-3]]
          +ds[level-3];
      }
  }
  if(level==0 || (level==1 && style==0)) b=last+1; else b=0;
  for(i=b,l=(long) 1<<b;i<pointcnt;i++,l<<=1) {
    if(used&l) continue;
    dthis=dist[prec][i];
    if(optimize) {
      if(style==0) {
        if(level>1 &&
             firstlast+dthis>dist[prec][last]+dist[first][i])
          continue;
        }
        else if(level>0) {
/* cut differently */
          if(style==1 && dthis>firstlast) continue;
/* switch with first */
          if((style&1) && level>1 && dist[first][i]<dthis) continue;
/* switch with last */
          if(style<3 && dist[prec][last]<dthis) continue;
        }
/* already too long */
        if(level>0 && shortcnt>0 &&
             dthis+dis+distmin*(pointcnt-level-1)>shortest)
          continue;
/* four-config optimize */
        for(j=0;j<=level-3;j++) {
          if(ds[j]+dthis>btab[j]+dist[utab[j+1]][i])
            goto next;
        }
/* five-config optimize */
        if(level>3 &&
             (d1+dthis>d2+dist[utab[level-2]][i] ||
            d1+dthis>d3+dist[utab[level-3]][i]))
          continue;
/* try prec elsewhere in the chain */
        if(level>3) {
          double dt;
          dt=dist[prec][i]+ds[level-2]-dist[utab[level-2]][i];
          for(j=0;j<level-3;j++) {
              if(dt>dist[prec][utab[j]]+dist[prec][utab[j+1]]-ds[j])
                goto next;
          }
        }
      }
      memmove(current,utab,level*sizeof(utab[0]));
      current[level]=i;
      if(level>0) {dd=dis+dthis; ds[level-1]=dthis;}
      else {
        first=i; firstlast=dist[first][last];
      }
      _calc(level+1,dd,used|l,current);
      next: ;
    }

  }
  else {
    count++;
    for(i=0,l=(long) 1;i<pointcnt && used&l; i++,l<<=1);
    if(i>=pointcnt) {
      printf("ERROR: Internal error.\n"); exit(1);
    }
    utab[pointcnt-2]=i; utab[pointcnt-1]=last;
    dis+=dist[utab[pointcnt-3]][i]+dist[i][last];
    if(shortest==0 || dis<shortest-tolerance) {
      shortest=dis; shortcnt=0;
      memmove(shpath[shortcnt++],utab,MAX_POINTS*sizeof(utab[0]));
    }
    else if(dis<shortest+tolerance && shortcnt<MAX_SHORT)
      memmove(shpath[shortcnt++],utab,MAX_POINTS*sizeof(utab[0]));
  }
}

/* main calculation loops */
void calc(void)
{
  int i;
  long used;
  int utab[MAX_POINTS];

  if(fast) {
    fastcalc(); return;
  }
  count=0; shortest=0; shortcnt=0;
  memset(utab,0,sizeof(utab));
  switch(style) {
    case 0: { /* loop to the start */
      first=0; utab[0]=0;
      for(i=1;i<pointcnt;i++) {
        last=i; used=(long) 1<<i; used|=(long) 1;
        firstlast=dist[first][last];
        _calc(1,firstlast,used,utab);
      }
      break;
    }
    case 1: { /* arbitrary open path */
      for(i=0;i<pointcnt;i++) {
        last=i; used=(long) 1<<i;
        _calc(0,0,used,utab);
      }
      break;
    }
    case 2: { /* open path with fixed start */
      first=0; utab[0]=0;
        for(i=1;i<pointcnt;i++) {
          last=i; used=(long) 1|(1<<i);
          firstlast=dist[0][last];
          _calc(1,0,used,utab);
        }
        break;
    }
    case 3: { /* open path with fixed end */
      last=0; used=(long) 1;
      _calc(0,0,used,utab);
      break;
    }
    case 4: { /* open path with fixed start and end */
      first=0; utab[0]=0;
      last=pointcnt-1; used=(long) 1|(1<<last);
      firstlast=dist[first][last];
      _calc(1,0,used,utab);
      break;
    }
  }
}

/* Print the result */
void printresult(void)
{
  int i,j;

  printf("%f %d %d\n",shortest,shortcnt,count);
  for(j=0;j<shortcnt;j++) {
    for(i=0;i<pointcnt;i++) printf("%d ",shpath[j][i]+1);
    printf("\n");
  }
}

/* prepare distance table */
void makedist(void)
{
  int i,j,k;
  double d;

  distmin=-1;
  for(i=0;i<pointcnt-1;i++) {
    for(j=i+1;j<pointcnt;j++) {
      d=0;
      for(k=0;k<dimension;k++) {
        d+=pow(coord[i][k]-coord[j][k],2);
      }
      d=sqrt(d); dist[i][j]=dist[j][i]=d;
      if(distmin<0 || distmin>d) distmin=d;
    }
  }
  tolerance=distmin*0.000001;
}

int main(int argc, char *argv[])
{
  char *p,*p2,*p3;
  int dim;
  input=getenv("wims_exec_parm");
/* nothing to do if no problem */
  if(input==NULL || *input==0) return 0;

  for(p=input+strlen(input);p>input && isspace(*(p-1));p--);
  *p=0; dimension=0;
  for(pointcnt=0,p=input;*p!=0 && pointcnt<MAX_POINTS;p=p3) {
    p2=strchr(p,'\n');
    if(p2==NULL) p3=p+strlen(p);
    else {p3=p2+1; *p2=0;}
    dim=0;
    for(p2=strchr(p,','); p2!=NULL && dim<MAX_DIM;
        p=p2+1,dim++,p2=strchr(p,',')) {
      *p2=0; coord[pointcnt][dim]=strtod(p,NULL);
    }
    if(dim<MAX_DIM) coord[pointcnt][dim++]=strtod(p,NULL);
    if(dim<2) continue; /* bad line */
    if(dimension==0) dimension=dim;
    pointcnt++;
  }
  if(pointcnt<3) {
    printf("ERROR: Not enough points.\n"); exit(1);
  }
  p=getenv("w_shortpath_style");
  if(p!=NULL) style=atoi(p);
  if(style<0 || style>4) style=0;
  makedist();
  optimize=1; calc();printresult();
/*    optimize=0; calc();printresult();
*/
  return 0;
}

