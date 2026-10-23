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
#include "libwims.h"

static int _lcomp(const void *l1, const void *l2)
{
  const double *d1, *d2;
  double d;
  d1=(const double *) l1; d2=(const double *) l2;
  d=*d1-*d2;
  if(d<0) return -1;
  if(d>0) return 1;
  return 0;
}

static double lc_scalex(leveldata *ld, int x)
{
  if(x<0) x=0;
  if(x>=ld->xsize) x=ld->xsize-1;
  return ld->xspan*((double) x - 0.40127)/ld->xsize+ld->xrange[0];
}

static double lc_scaley(leveldata *ld, int y)
{
  if(y<0) y=0;
  if(y>=ld->ysize) y=ld->ysize-1;
  return -ld->yspan*((double) y - 0.40127)/ld->ysize+ld->yrange[1];
}

static int _getlevel(leveldata *ld, int x, int y)
{
  double dd;
  int i;

  eval_setval(ld->xevpos,lc_scalex(ld,x));
  eval_setval(ld->yevpos,lc_scaley(ld,y));
  dd=checked_eval(ld->fn);
  for(i=0;i<ld->levelcnt && dd>ld->levels[i];i++);
  return i;
}

static void lc_replacexy(leveldata *ld)
{
  char *pp;
  for(pp=varchr(ld->fn,ld->xname);pp!=NULL;pp=varchr(pp,ld->xname)) {
    string_modify(ld->fn,pp,pp+strlen(ld->xname),EV_X);
    pp+=strlen(EV_X);
  }
  for(pp=varchr(ld->fn,ld->yname);pp!=NULL;pp=varchr(pp,ld->yname)) {
    string_modify(ld->fn,pp,pp+strlen(ld->yname),EV_Y);
    pp+=strlen(EV_Y);
  }
}

/* produces level curve data. Returns non-zero if error. */
int levelcurve(leveldata *ld)
{
  int xx,yy,xi,yi,xsteps,ysteps;
  int i,x,y,xt,yt;
  short int l1[LEVELSIZE_LIM+16], l2[LEVELSIZE_LIM+16];
  short int l3[LEVELGRAIN_LIM+2][LEVELGRAIN_LIM+2];
  char f[MAX_LINELEN+1];

  ld->datacnt=0;
  if(ld->fn==NULL || *(ld->fn)==0 || ld->levelcnt<1) return 1;
  if(ld->grain<1) ld->grain=4;
  if(ld->grain>LEVELGRAIN_LIM) ld->grain=LEVELGRAIN_LIM;
  if(ld->levelcnt>LEVEL_LIM) ld->levelcnt=LEVEL_LIM;
  if(ld->xsize<10 || ld->xsize>LEVELSIZE_LIM) return 2;
  if(ld->ysize<10 || ld->ysize>LEVELSIZE_LIM) return 2;
  if(check_parentheses(ld->fn,0)) return 3;
  ld->xspan=ld->xrange[1]-ld->xrange[0];
  ld->yspan=ld->yrange[1]-ld->yrange[0];
  if(ld->levelcnt>1) qsort(ld->levels,ld->levelcnt,sizeof(double),_lcomp);
  if(ld->xname==NULL || *(ld->xname)==0) ld->xname="x";
  if(ld->yname==NULL || *(ld->yname)==0) ld->yname="y";
  snprintf(f,sizeof(f),"%s",ld->fn); substitute(f); ld->fn=f;
  ld->xevpos=eval_getpos(EV_X); ld->yevpos=eval_getpos(EV_Y);
  if(ld->xevpos<0 || ld->yevpos<0) return 4;
  lc_replacexy(ld); evalue_compile(f);
  xsteps=(ld->xsize+ld->grain-1)/ld->grain+1;
  ysteps=(ld->ysize+ld->grain-1)/ld->grain+1;
  for(yy=yi=0;yi<ysteps;yy+=ld->grain,yi++) l2[yi]=_getlevel(ld,0,yy);
  l2[ysteps]=l2[ysteps-1];
  for(xi=1,xx=ld->grain;xi<xsteps && ld->datacnt<LEVELPOINT_LIM;xx+=ld->grain,xi++) {
    memmove(l1,l2,(ysteps+1)*sizeof(short int));
    for(yi=yy=0;yi<ysteps;yy+=ld->grain,yi++) l2[yi]=_getlevel(ld,xx,yy);
    l2[ysteps]=l2[ysteps-1];
    for(yi=0;yi<ysteps && ld->datacnt<LEVELPOINT_LIM;yi++)
      if(l1[yi]!=l1[yi+1] || l1[yi]!=l2[yi] || l1[yi]!=l2[yi+1]) {
        for(x=0,xt=(xi-1)*ld->grain;x<=ld->grain;x++,xt++)
          for(y=0,yt=yi*ld->grain;y<=ld->grain;y++,yt++) {
            if(x==0 && y==0) {l3[x][y]=l1[yi]; continue;}
            if(x==0 && y==ld->grain) {l3[x][y]=l1[yi+1]; continue;}
            if(x==ld->grain && y==0) {l3[x][y]=l2[yi]; continue;}
            if(x==ld->grain && y==ld->grain) {l3[x][y]=l2[yi+1]; continue;}
            l3[x][y]=_getlevel(ld,xt,yt);
          }
        for(x=0,xt=(xi-1)*ld->grain;x<ld->grain;x++,xt++)
          for(y=0,yt=yi*ld->grain;y<ld->grain;y++,yt++) {
            i=l3[x][y];
            if((i!=l3[x][y+1] || i!=l3[x+1][y]) &&
                ld->datacnt<LEVELPOINT_LIM &&
                (i!=l3[x][y+1] || ld->datacnt==0 ||
                l3[x+1][y+1]==l3[x+1][y] ||
                ld->xdata[ld->datacnt-1]!=xt ||
                ld->ydata[ld->datacnt-1]!=yt-1)) {
              ld->xdata[ld->datacnt]=xt;
              ld->ydata[ld->datacnt++]=yt;
            }
          }
      }
  }
  return 0;
}
