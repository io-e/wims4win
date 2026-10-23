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

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <ctype.h>
#include <math.h>
#include <gd.h>
#include <gdfontt.h>
#include <gdfonts.h>
#include <gdfontmb.h>
#include <gdfontl.h>
#include <gdfontg.h>

#include "../Lib/libwims.h"

#define MAX_PARMS   8192
#define MAX_SIZE    4096
#define MAX_VARS    1024
#define MAX_VARNAMEBUF	16384
#define BOUND	(MAX_SIZE*10)
#define exec_prefix_char '#'

extern FILE * tikz_file;
extern int tikz_brushColor;

/* from flydraw.c */
extern int linecnt, varcnt;
extern ev_variable vartab[];
extern char varnamebuf[MAX_VARNAMEBUF], *varnameptr;
extern gdImagePtr image, wimg, tileimg, brushimg;
extern int brushed, tiled, styled, savew, wcolor, transform;
extern int dashed,filled,noreset,reset;
typedef double matrice[4];
#define JC_NB_MATRICES 19
extern matrice matrices_pavage[JC_NB_MATRICES+1];
#define matrix matrices_pavage[0]
typedef double vecteur[2];
extern vecteur vecteurs_pavage[JC_NB_MATRICES+1];
#define vector vecteurs_pavage[0];
extern double scale_buf[MAX_PARMS];
extern double transx, transy; /* translation vector */
extern double mobx, moby, ctheta, stheta; /* moebius parameters */
extern double xscale, yscale, xstart, ystart;
extern int sizex, sizey, saved;
extern int color_white, color_black, color_bounder, color_frame, lstep;
extern int width, width2;
extern double tend, tstart, tstep, plotjump, animstep;
extern int vimg_enable, vimg_ready;
extern FILE *vimgf;
extern char vimgfilename[1024], imagefilename[1024], tikzfilename[1024];
extern int tranged;
extern double parallogram_fonda[6];
extern char *substit(char *p);

void output(void);
/* from wimg.c */
void vimg_init (void);
void vimg_close (void);
void vimg_arc (double x0, double y0, double rx, double ry, double a1, double a2);
void vimg_ellipse (double x0, double y0, double rx, double ry);
void vimg_line (double x1, double y1, double x2, double y2);
void vimg_polyline (double xy[], int cnt, int closed);
void vimg_rect (double x1, double y1, double x2, double y2);
void vimg_plotstart (void);
void vimg_plot1 (double x, double y);
void vimg_plotend (void);


/* from flylines.c */
char *find_name_start(char *p);
char *find_name_end(char *p);
void fly_error(char *p);
void moebius(double X, double Y, double *x, double *y);
void real_to_pixel(double x, double y, int ibuf[]);
void scale(double dbuf[], int ibuf[], int cnt);
/* scale without displacement */
void scale2(double xin, double yin, double *xout, double *yout);
int widthcolor(int w, int color);
int getcolor(int r, int g, int b);
void setvar(char *p, double v);
void collapse_item(char *p, int n);
int ggetline(char buf[]);

/* from nametab.c */
enum {t_prep,t_color,t_obj};
typedef struct objparm{ int p[MAX_PARMS]; double pd[MAX_PARMS]; int pcnt, color[2], fill;
char str[MAX_LINELEN+1], font[16]; } objparm;
enum {p_and,p_from,p_to,p_thru,p_center,p_color,p_font,
  p_diameter,p_radius,p_width,p_height };

extern struct preptab { char *name;int typ; } preptab[];
extern struct objtab {
 char *name; int required_parms, color_pos, fill_tag, subst;
 void (*routine) (objparm *pm); } objtab[];

extern struct colortab {char *name;char *def;} colortab[];
extern struct nametab {char *name;int type;int serial;} nametab[];
extern int nametab_no, prep_no, obj_no, color_no;

/* from objects.c */
int obj_main(char *p);
