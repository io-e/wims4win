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

/* This file is a temporary patch to tex eps files with extra borders.
 * It cuts the borders in the resulting gif file, so that the font
 * fits tightly in the cadre. libgd is required.
 */

#include "flydraw.h"

FILE *tikz_file;

char imagefilename[1024];
char vimgfilename[1024];
char tikzfilename[1024];
int sizex, sizey, saved;
gdImagePtr image=NULL, brushimg=NULL, tileimg=NULL, wimg=NULL;
int brushed=0, tiled=0, styled=0, width=1, savew=1, wcolor=-1, width2=4;
int dashed,filled,noreset;
int color_white, color_black, color_bounder, color_frame;
int linecnt=-100000;
int tranged=0;
double xscale=1, yscale=1, xstart=0, ystart=0;
double tstart=0, tend=1, tstep=100, plotjump=200;
double animstep=0;
int transform=0;  /* transformation indicator */
double transx=0, transy=0, rotangle=0; /* translation vector and rotation */
double mobx=0,moby=0,ctheta=1,stheta=0;
int lstep=4;
ev_variable vartab[MAX_VARS];
char varnamebuf[MAX_VARNAMEBUF], *varnameptr=varnamebuf;
int varcnt;
int vimg=0; /* 0: no vector image output.
             * 1: DXF vector output. */
int vimg_enable=0; /* 0: disable. 1: enable. */
int vimg_ready=0;
FILE *vimgf=NULL;
double scale_buf[MAX_PARMS];
/* les matrices suivantes sont initialisees par la commande setmatrix nummatrix,a11,a12,a21,a22
 * elles sont remises a l'unite par resetmatrix nummatrix
 * la matrice fixant le systeme de coordonnees "mathematiques" dans l'image
 */
matrice matrices_pavage[JC_NB_MATRICES+1];
vecteur vecteurs_pavage[JC_NB_MATRICES+1];

/* les coordonnees du parallelograme de pavage = coordonnees "mathematiques"
 * du parallelogramme contenant l'image a recopier on place ces coordonnees
 * avec setparallelogram xs,ys,xu,yu,xv,yu
 * xs,ys=coordonnees math du point 0,0,
 * xu,yu coordonnees math de l'horizontale
 * xv,yv coordonnees math de la verticale
 * ces coordonnees sont remises a leur valeur par defaut par resetparallelogram
 */
double parallogram_fonda[]={0,0,100,0,0,100};

/* Write the image */
void output(void)
{
  FILE *out;

  if(!image) return;
  if(imagefilename[0]) {
    out=fopen(imagefilename,"wb");
    if(out!=NULL) {gdImageGif(image, out); fclose(out); }
  }
  else gdImageGif(image, stdout);
  saved=1;
}

/* substitute variable names by their environment strings
 * The buffer pointed to by p must have enough space
 * (defined by MAX_LINELEN).
 */
char *substit(char *p)
{
  char *pp, *pe;
  char namebuf[MAX_NAMELEN+1];
  int i, l;

  pe=strchr(p,'"'); if(pe!=NULL) l=pe-p; else l=MAX_LINELEN;
  for(pp=find_name_start(p); *pp!=0 && pp-p<l;
    pp=find_name_start(pe)) {
    pe=find_name_end(pp);
    if((pp>p && !isspace(*(pp-1)) && *(pp-1)!=',') ||
       (*pe!=0 && !isspace(*pe) && *pe!=',')) continue;
    memmove(namebuf,pp,pe-pp); namebuf[pe-pp]=0;
    i=search_list(nametab,nametab_no,sizeof(nametab[0]),namebuf);
    if(i<0) continue;
    if(nametab[i].type==t_prep && preptab[nametab[i].serial].typ==p_font)
      break;
    if(nametab[i].type==t_color)
      string_modify(p,pp,pe,colortab[nametab[i].serial].def);
  }
  return p;
}

void fly_process(void)
{
  char buf[MAX_LINELEN+1];
  int c;
  do {
    c=ggetline(buf); obj_main(buf);
  }
  while(c!=EOF);
}

int verify_tables(void) {
  return evaltab_verify();
}

int main(int argc, char *argv[])
{
  int i; for (i = 0; i <= JC_NB_MATRICES; i++)
    matrices_pavage [i][0] = matrices_pavage[i][3] = 1;
  substitute=substit;
  ev_varcnt=&varcnt; ev_var=vartab;
  if(argc==2 && strcasecmp(argv[1],"table")==0) {
    if(verify_tables()) {
      printf("Table disorder.\n"); return 1;
    }
    printf("Table orders OK.\n"); return 0;
  }
  vartab[0].name="animstep"; vartab[0].value=0;
  varcnt=1;
  if(argc>1) snprintf(imagefilename,sizeof(imagefilename),"%s",argv[1]);
  else imagefilename[0]=0;
  fly_process();
  if(tikz_file){
    fprintf(tikz_file,"\\end{tikzpicture}\n");
    fclose(tikz_file);
  }
  else
    if(!saved || imagefilename[0]!=0) output();
  if(image) gdImageDestroy(image);
  if(vimg_ready) vimg_close();
  return 0;
}
