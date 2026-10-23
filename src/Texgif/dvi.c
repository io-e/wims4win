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
#include "texgif.h"

/* dvi 2 gif driver, dvi interpreter */
#define dvi_set1	128
#define dvi_set2	129
#define dvi_set3	130
#define dvi_set4	131
#define dvi_set_rule	132
#define dvi_put1	133
#define dvi_put2	134
#define dvi_put3	135
#define dvi_put4	136
#define dvi_put_rule	137
#define dvi_nop		138
#define dvi_bop		139
#define dvi_eop		140
#define dvi_push	141
#define dvi_pop		142
#define dvi_right1	143
#define dvi_right2	144
#define dvi_right3	145
#define dvi_right4	146
#define dvi_w0		147
#define dvi_w1		148
#define dvi_w2		149
#define dvi_w3		150
#define dvi_w4		151
#define dvi_x0		152
#define dvi_x1		153
#define dvi_x2		154
#define dvi_x3		155
#define dvi_x4		156
#define dvi_down1	157
#define dvi_down2	158
#define dvi_down3	159
#define dvi_down4	160
#define dvi_y0		161
#define dvi_y1		162
#define dvi_y2		163
#define dvi_y3		164
#define dvi_y4		165
#define dvi_z0		166
#define dvi_z1		167
#define dvi_z2		168
#define dvi_z3		169
#define dvi_z4		170
#define dvi_fnt1	235
#define dvi_fnt2	236
#define dvi_fnt3	237
#define dvi_fnt4	238
#define dvi_xxx1	239
#define dvi_xxx2	240
#define dvi_xxx3	241
#define dvi_xxx4	242
#define dvi_fnt_def1	243
#define dvi_fnt_def2	244
#define dvi_fnt_def3	245
#define dvi_fnt_def4	246
#define dvi_pre		247
#define dvi_post	248
#define dvi_post_post	249

unsigned char *dvibuf, *dviptr;
int dvilen;
int pass, passstop;
int num, den, mag;
int minx, maxx,miny, maxy;
double dviratio;
int dvix, dviy;
int d_h,d_v,d_w,d_x,d_y,d_z;
int d_f;
int pageptr;
struct {
    int minx, maxx, miny, maxy;
} pagedata[MAX_PAGES];

struct {
    int h,v,w,x,y,z;
} dvistack[DVI_STACK_LIMIT];
int dvistackptr;

#define DVI_post_post	DVI_post
void DVI_post(void)
{
    passstop=-1; dviptr=dvibuf+dvilen;
}

void DVI_eop(void)
{
    if(pass==1) {
	pagedata[pageptr].minx=minx;
	pagedata[pageptr].maxx=maxx;
	pagedata[pageptr].miny=miny;
	pagedata[pageptr].maxy=maxy;
    }
    else saveimage();
    pageptr++;
    if(*outfile==0 || pageptr>=MAX_PAGES) {
	passstop=-1; dviptr=dvibuf+dvilen;
    }
    else passstop=0;
}

void DVI_bop(void)
{
    passstop=1; dvistackptr=0;
    d_h=d_v=d_w=d_x=d_y=d_z=0; d_f=-1;
    dviptr+=44;
    if(pass==1) {
	minx=miny=65536;
	maxx=maxy=-1;
    }
    else {
	char *p;
	maxx=pagedata[pageptr].maxx;
	minx=pagedata[pageptr].minx;
	maxy=pagedata[pageptr].maxy;
	miny=pagedata[pageptr].miny;
	if(maxx<=minx || maxy<=miny) texgif_error("Empty page.");
	createimage(maxx-minx,maxy-miny);
	currentcolor=color_black;
	p=getenv("w_instex_color");
	if(p!=NULL && *p!=0) makecolor(p);
    }
}

void DVI_nop(void) {}

int DVI_put(int p)
{
    int cc, ct, x1, x2, y1, y2;
    if(d_f<0) texgif_error("Bad dvi: trying to put non-existing font.");
    if(p==0) cc=*dviptr; else cc=texint(dviptr+1,p);
    dviptr+=p;
    if(cc<wfont[d_f].bc || cc>wfont[d_f].ec) return -1;
    dvix=rint(dviratio*d_h); dviy=rint(dviratio*d_v);
    if(pass==1) {
	ct=cc-wfont[d_f].bc;
	x1=dvix+wfont[d_f].fh[ct].xstart; y1=dviy+wfont[d_f].fh[ct].ystart;
	x2=x1+wfont[d_f].fh[ct].xmax; y2=y1+wfont[d_f].fh[ct].ymax;
	if(minx>x1) minx=x1;
	if(miny>y1) miny=y1;
	if(maxx<x2) maxx=x2;
	if(maxy<y2) maxy=y2;
    }
    else {
	paintfont(wfont+d_f,cc,dvix-minx,dviy-miny,currentcolor);
    }
    return cc;
}

void DVI_set(int p)
{
    int cc;
    cc=DVI_put(p);
    if(cc<0) return;
    d_h+=wfont[d_f].fh[cc-wfont[d_f].bc].w;
}

int DVI_put_rule(void)
{
    int xxx, xx, yy, x1, y1, x2, y2, xx1, xx2, yy1, yy2;
    yy=texint(dviptr+1,4); xx=texint(dviptr+5,4); dviptr+=8;
    if(xx<=0 || yy<=0) return xx;
    xxx=xx;
    if(xx>=0) {xx=rint(dviratio*xx+blacker); if(xx>0) xx--;}
    else {xx=rint(dviratio*xx-blacker); if(xx<0) xx--;}
    if(yy>=0) {yy=rint(dviratio*yy+blacker); if(yy>0) yy--;}
    else {yy=rint(dviratio*yy-blacker); if(yy<0) yy--;}
    if(pass==1) {
	x1=rint(dviratio*d_h); y2=rint(dviratio*d_v);
	x2=x1+xx; y1=y2-yy;
	if(x1<x2) {xx1=x1; xx2=x2;} else {xx1=x2; xx2=x1;}
	if(y1<y2) {yy1=y1; yy2=y2;} else {yy1=y2; yy2=y1;}
	if(minx>x1) minx=x1;
	if(miny>y1) miny=y1;
	if(maxx<x2) maxx=x2;
	if(maxy<y2) maxy=y2;
    }
    else {
	x1=rint(dviratio*d_h)-minx; y1=rint(dviratio*d_v)-miny;
	x2=x1+xx; y2=y1-yy;
	if(x1<x2) {xx1=x1; xx2=x2;} else {xx1=x2; xx2=x1;}
	if(y1<y2) {yy1=y1; yy2=y2;} else {yy1=y2; yy2=y1;}
	gdImageFilledRectangle(image,xx1,yy1,xx2,yy2,currentcolor);
    }
    return xxx;
}

void DVI_set_rule(void)
{
    d_h+=DVI_put_rule();
}

void DVI_fnt_num(int p)
{
    int fnum;
    if(p==0) fnum=*dviptr-171; else fnum=texint(dviptr+1,p);
    dviptr+=p;
    for(d_f=0; d_f<fontcnt && wfont[d_f].num!=fnum; d_f++);
    if(d_f>=fontcnt) texgif_error("Bad dvi: using font before defining it.");
}

void DVI_fnt_def(int p)
{
    int i, fnum, fdensity;
    char fname[1024];
    unsigned long int c,s,d,a,l;
    double fratio;
    union { unsigned long c; char C[4]; } U;

    if(fontcnt>=FONT_NUMBER_LIMIT) texgif_error("Font number exceeded design capacity.");
    fnum=texint(++dviptr,p); dviptr+=p;
    for (i = 0; i <= 3; i++) U.C[i] = dviptr[i];
    c=U.c; dviptr+=4;
    s=texint(dviptr,4); dviptr+=4;
    d=texint(dviptr,4); dviptr+=4;
    a=*dviptr++; l=*dviptr++;
    if(a+l>1000 || a+l<1) texgif_error("Bad dvi: bad font name.");
    memmove(fname,dviptr,a+l); fname[a+l]=0; dviptr+=a+l-1;
    if(pass==1) {
	fdensity=(double) density*((double) s/d);
	if(loadfont(fname, c, fdensity, wfont+fontcnt)==NULL) {
	    if(loadfont("cmr10",0,fdensity,wfont+fontcnt)==NULL)
	      texgif_error("Font panick: even cmr10 cannot be found.");
	    else fprintf(stderr,"Font %s not found; replace by cmr10.\n",fname);
	}
	wfont[fontcnt].num=fnum;
	fratio=(double) s/wfont[fontcnt].designsize*d/65536;
	for(i=0;i<wfont[fontcnt].cnt; i++)
	  wfont[fontcnt].fh[i].w=fratio*wfont[fontcnt].fh[i].w;
	fontcnt++;
    }
}

void DVI_push(void)
{
    if(dvistackptr>=DVI_STACK_LIMIT) texgif_error("dvi stack overflow.");
    dvistack[dvistackptr].h=d_h;
    dvistack[dvistackptr].v=d_v;
    dvistack[dvistackptr].w=d_w;
    dvistack[dvistackptr].x=d_x;
    dvistack[dvistackptr].y=d_y;
    dvistack[dvistackptr].z=d_z;
    dvistackptr++;
}

void DVI_pop(void)
{
    if(dvistackptr<=0) texgif_error("Bad dvi file: dvi stack underflow.");
    dvistackptr--;
    d_h=dvistack[dvistackptr].h;
    d_v=dvistack[dvistackptr].v;
    d_w=dvistack[dvistackptr].w;
    d_x=dvistack[dvistackptr].x;
    d_y=dvistack[dvistackptr].y;
    d_z=dvistack[dvistackptr].z;
}

void DVI_move(int *hv, int p)
{
    int t;
    t=texintsigned(dviptr+1,p); dviptr+=p;
    *hv+=t;
}

void DVI_move2(int *hv, int *xyz, int p)
{
    if(p>0) *xyz=texintsigned(dviptr+1,p);
    *hv+=*xyz;
    dviptr+=p;
}

void DVI_xxx(int p)
{
    int i, t;
    char *pp, buf[1024];

    t=texint(dviptr+1,p);
    if(pass<2 || t>1000) {dviptr+=t+p; return;}
    memmove(buf,dviptr+1+1,t); buf[t]=0;
    dviptr+=t+p;
    for(i=0;i<t;i++) buf[i]=tolower(buf[i]);
    if(strncmp(buf,"color",5)!=0) return;
    pp=find_word_start(buf+5); if(*pp!='=') return;
    makecolor(pp+1);
}

	/* load and interprete dvi file */
void dvi(void)
{
    unsigned char cc, *startpoint;
    char namebuf[1024];

    snprintf(namebuf,sizeof(namebuf),"%s/texgif.dvi",tmpdir);
    dvilen=getfile(namebuf,&dvibuf);
    if(dvilen<=0) texgif_error("dvi file not found.");
    dviptr=dvibuf; fontcnt=0;
    if(*dviptr++!=dvi_pre || *dviptr++!=2) {
	/* baddvi: */ texgif_error("Bad dvi file header.");
    }
    num=texint(dviptr,4); dviptr+=4;
    den=texint(dviptr,4); dviptr+=4;
    mag=texint(dviptr,4); dviptr+=4;
    cc=*dviptr++; dviptr+=cc; startpoint=dviptr;
    density=rint((double) compressratio*mag*basedensity/1000);
    dviratio=(double) num/den*density/254000;
printf("dvi file: num=%d, den=%d, ratio=%f, mag=%d, density=%d\n",
       num,den,dviratio, mag, density);
    for(pass=1; pass<=2; pass++) {
	passstop=0; d_f=-1; pageptr=0;
	for(dviptr=startpoint; dviptr<dvibuf+dvilen && passstop>=0; dviptr++) {
	    if(*dviptr<128) {
		DVI_set(0); continue;
	    }
	    if(*dviptr>=171 && *dviptr<=234) {
		DVI_fnt_num(0); continue;
	    }
	    switch(*dviptr) {
		case dvi_set1: case dvi_set2: case dvi_set3:
		case dvi_set4: DVI_set(*dviptr-dvi_set1+1); break;
		case dvi_set_rule: DVI_set_rule(); break;

		case dvi_put1: case dvi_put2: case dvi_put3:
		case dvi_put4: DVI_put(*dviptr-dvi_put1+1); break;
		case dvi_put_rule: DVI_put_rule(); break;

		case dvi_nop: DVI_nop(); break;
		case dvi_bop: DVI_bop(); break;
		case dvi_eop: DVI_eop(); break;
		case dvi_push: DVI_push(); break;
		case dvi_pop: DVI_pop(); break;

		case dvi_right1: case dvi_right2: case dvi_right3:
		case dvi_right4: DVI_move(&d_h, *dviptr-dvi_right1+1); break;

		case dvi_w0: case dvi_w1: case dvi_w2: case dvi_w3:
		case dvi_w4: DVI_move2(&d_h, &d_w, *dviptr-dvi_w0); break;
		case dvi_x0: case dvi_x1: case dvi_x2: case dvi_x3:
		case dvi_x4: DVI_move2(&d_h, &d_x, *dviptr-dvi_x0); break;

		case dvi_down1: case dvi_down2: case dvi_down3:
		case dvi_down4: DVI_move(&d_v, *dviptr-dvi_down1+1); break;

		case dvi_y0: case dvi_y1: case dvi_y2: case dvi_y3:
		case dvi_y4: DVI_move2(&d_v, &d_y, *dviptr-dvi_y0); break;
		case dvi_z0: case dvi_z1: case dvi_z2: case dvi_z3:
		case dvi_z4: DVI_move2(&d_v, &d_z, *dviptr-dvi_z0); break;

		case dvi_fnt1: case dvi_fnt2: case dvi_fnt3:
		case dvi_fnt4: DVI_fnt_num(*dviptr-dvi_fnt1+1); break;

		case dvi_xxx1: case dvi_xxx2: case dvi_xxx3:
		case dvi_xxx4: DVI_xxx(*dviptr-dvi_xxx1+1); break;

		case dvi_fnt_def1: case dvi_fnt_def2: case dvi_fnt_def3:
		case dvi_fnt_def4: DVI_fnt_def(*dviptr-dvi_fnt_def1+1); break;

		case dvi_pre: texgif_error("Bad dvi file: pre within file.");
		case dvi_post: DVI_post(); break;
		case dvi_post_post: DVI_post_post(); break;

		default: texgif_error("Bad dvi file: unknown command.");
	    }
	}
    }
    free(dvibuf);
}

