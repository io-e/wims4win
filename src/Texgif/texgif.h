/*    Copyright (C) 2002-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
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

/* dvi 2 gif driver, header file */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <math.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <ctype.h>
#include <gd.h>
#include "../Lib/libwims.h"

/*#define MAX_LINELEN		16383 */
#define FILE_LENGTH_LIMIT	(1*1024*1024)
#define DVI_STACK_LIMIT		1024
#define FONT_NUMBER_LIMIT	1024
#define CHAR_NUMBER_LIMIT	256
#define IMAGE_SIZE_LIMIT	(4*1024*1024)
#define MAX_FONT_X		1024
#define MAX_FONT_Y		1024
#define MAX_PAGES		4096

typedef struct {
    short int xstart, xmax, ystart, ymax;
    long int w, start;
} FONTHEADER;

typedef struct {
    int checksum,designsize,bc,ec,cnt,num;
    FONTHEADER *fh;
    char *data;
    char *cache;
} FONT;
/* from texgif.c */
extern char *progname,*tmpdir,*fontdir,*headerfile,*texstyle,*outfile, cwd[1024];
extern double blacker;
extern int basedensity, density, compressratio, wrapexec, currentcolor, fontcnt;
extern FONT wfont[FONT_NUMBER_LIMIT];
/* from basic.c */
void texgif_error(char *s);
int call_sh(char *s,...);
int getfile(char *fname, unsigned char **buf);
unsigned long int texint(void *bp, int l);
long int texintsigned(void *bp, int l);
int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *arg[]);

/* from colors.c */
extern struct colors{
	char *name;
	int r,g,b;
} colors[];
extern int colorno;

/* from tfm.c */
extern struct tfm {
    int bc, ec;
    int checksum, designsize;
    struct f {
	int32_t w,h,d;
    } f[256];
} tfm;

extern short int tfmhd[12];
void loadtfm(char *fname);

/* from gf.c */
void loadgf(char *fname, int density);
void makegf(char *fontname, int density);
void gf2font(char *fontname,int density);

/* from font.c */
FONT *loadfont(char *fname, int checksum, int density, FONT *ft);

/* from image.c */
void createimage(int xsize, int ysize);
void saveimage();
void makecolor(char *p);
int paintfont(FONT *f, int ch, int x, int y, int color);
extern gdImagePtr image;
extern int color_white, color_black, color_bounder;

/* from dvi.c */
void dvi(void);
