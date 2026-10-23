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

/* dvi 2 gif driver, gf font loader and processor */
#include "texgif.h"

#define gf_paint_0	0
#define gf_paint1	64
#define gf_paint2	65
#define gf_paint3	66
#define gf_boc		67
#define gf_boc1		68
#define gf_eoc		69
#define gf_skip0	70
#define gf_skip1	71
#define gf_skip2	72
#define gf_skip3	73
#define gf_new_row_0	74
#define gf_xxx1		239
#define gf_xxx2		240
#define gf_xxx3		241
#define gf_xxx4		242
#define gf_yyy		243
#define gf_no_op	244
#define gf_char_loc	245
#define gf_char_loc0	246
#define gf_pre		247
#define gf_post		248
#define gf_post_post	249

int g;
FONTHEADER *fh;
int brush, x,y,xmax,ymax,thischar,xsize,charsize,charcnt,charpos;
int chswitch;
long int datapos;
char *onechar;
unsigned char *gfbuf;
int gfbuflen;
FILE *fontf;

void GF_eoc(void)
{
    if(onechar==NULL) return;
    fwrite(onechar,1,charsize,fontf); datapos+=charsize;
    free(onechar); onechar=NULL;
}

void GF_boc(int p)
{
    unsigned char cc;
    int min_m, min_n, max_m, max_n, del_m, del_n;
    if(!chswitch) return;
    if(p==0) {
	cc=texint(gfbuf+(++g),4); g+=8;
	min_m=texint(gfbuf+g,4); g+=4;
	max_m=texint(gfbuf+g,4); g+=4;
	min_n=texint(gfbuf+g,4); g+=4;
	max_n=texint(gfbuf+g,4); g+=4;
	del_m=max_m-min_m; del_n=max_n-min_n;
	g--;
    }
    else {
	cc=gfbuf[++g];
	del_m=gfbuf[++g]; max_m=gfbuf[++g];
	del_n=gfbuf[++g]; max_n=gfbuf[++g];
	min_m=max_m-del_m; min_n=max_n-del_n;
    }
    if(cc>tfm.ec || cc<tfm.bc) return;
    charpos=cc-tfm.bc;
    if(del_m<0) del_m=0;
    if(del_n<0) del_n=0;
    if(del_m>=MAX_FONT_X-1) del_m=MAX_FONT_X-2;
    if(del_n>=MAX_FONT_Y-1) del_n=MAX_FONT_Y-2;
    x=y=0; xmax=del_m+1; ymax=del_n+1;
    fh[charpos].xstart=min_m; fh[charpos].ystart=-max_n;
    fh[charpos].xmax=xmax; fh[charpos].ymax=ymax;
    brush=0; thischar=cc;
    xsize=(xmax+7)/8; charsize=xsize*ymax;
    onechar=xmalloc(charsize+1024);
    memset(onechar,0,charsize+1024);
    fh[charpos].start=datapos;
    fh[charpos].w=tfm.f[cc].w;
}

void GF_paint(int p)
{
    int i;
    if(onechar==NULL) return;
    if(brush) for(i=x;i<p+x && i<xmax;i++) {
	onechar[y*xsize+(i>>3)]|=(1<<(i&7));
    }
    x+=p; if(x>xmax) x=xmax;
    brush^=1;
}

void GF_new_row(int p)
{
    y++; if(y>ymax) y=ymax;
    x=p; if(x>xmax) x=xmax;
    brush=1;
}

void GF_skip(int p)
{
    if(p>0) p=texint(gfbuf+(++g),p);
    p++; y+=p; if(y>ymax) y=ymax;
    x=0; brush=0;
}

void GF_xxx(int p)
{
    unsigned int t;
    t=texint(gfbuf+(++g),p);
    g+=p+t-1;
}

void GF_yyy(void)
{
    g+=4;
}

void GF_char_loc(int p)
{
    if(p==0) g+=1+1+4+4;
    else g+=1+4+4+4+4;
}

void GF_post(void)
{
    chswitch=0;
}

void GF_post_post(void)
{
    g=gfbuflen; chswitch=0;
}

void loadgf(char *fname, int density)
{
    int len;
    char namebuf[128];

    snprintf(namebuf,sizeof(namebuf),"%s/texgf.%dgf",tmpdir,density);
    len=getfile(namebuf,&gfbuf);
    if(len<=0) {
	texgif_error("Metafont failed.");
    }
    gfbuflen=len;
}

void makegf(char *fontname, int density)
{
    call_sh("mkdir -p %s\n\
cd %s\n\
rm -f texgf.* 2>/dev/null\n\
cat >texgf.mf <<@\n\
batchmode;\n\
mode_def texgf =\n\
	mode_param (pixels_per_inch, %d);\n\
	mode_param (blacker, %f);\n\
	mode_param (fillin, 0);\n\
	mode_param (o_correction, 0);\n\
	mode_common_setup_;\n\
enddef;\n\
mode = texgf;\n\
input %s;\n\
@\n\
mf texgf </dev/null || mf-nowin texgf </dev/null\n\
", fontdir, tmpdir, density, pow((double)density/100,0.75)*blacker, fontname);
}

void gf2font(char *fontname,int density)
{
    unsigned char cc;
    char namebuf[1024];
    char tmpname[1024];

    if((gfbuf[0]&255)!=gf_pre || (gfbuf[1]&255)!=131) return;
    g=3+(gfbuf[2]&255);
    if(gfbuflen<=g) return;
    if(tfm.ec==0 && tfm.bc==0) return;
    charcnt=tfm.ec-tfm.bc+1;
    fh=xmalloc(charcnt*sizeof(FONTHEADER)+256);
    memset(fh,0,charcnt*sizeof(FONTHEADER));
    snprintf(tmpname,sizeof(tmpname),"%s/wims.fonttmp",tmpdir);
    fontf=fopen(tmpname,"w"); if(fontf==NULL) return;
    brush=0; x=y=xmax=ymax=datapos=0; onechar=NULL; chswitch=1;
    for(;g<gfbuflen;g++) {
	cc=gfbuf[g];
	if(cc<=63) {GF_paint(cc); continue;}
	if(cc>=74 && cc<=238) {GF_new_row(cc-74); continue;}
	switch(cc) {
	    case gf_boc:	GF_boc(0); break;
	    case gf_boc1:	GF_boc(1); break;
	    case gf_eoc:	GF_eoc(); break;
	    case gf_skip0: 	GF_skip(0); break;
	    case gf_skip1: 	GF_skip(1); break;
	    case gf_skip2: 	GF_skip(2); break;
	    case gf_skip3: 	GF_skip(3); break;
	    case gf_char_loc:	GF_char_loc(1); break;
	    case gf_char_loc0:	GF_char_loc(0); break;
	    case gf_xxx1:	GF_xxx(1); break;
	    case gf_xxx2:	GF_xxx(2); break;
	    case gf_xxx3:	GF_xxx(3); break;
	    case gf_xxx4:	GF_xxx(4); break;
	    case gf_yyy:	GF_yyy(); break;

	    default: break;
	}
    }
    fclose(fontf);
    snprintf(namebuf,sizeof(namebuf),"%s/%s.font",
	     tmpdir,fontname);
    fontf=fopen(namebuf,"w"); if(fontf!=NULL) {
	fwrite(&(tfm.checksum),sizeof(int),1,fontf);
	fwrite(&(tfm.designsize),sizeof(int),1,fontf);
	fwrite(&(tfm.bc),sizeof(int),1,fontf);
	fwrite(&(tfm.ec),sizeof(int),1,fontf);
	fwrite(fh,sizeof(FONTHEADER),charcnt,fontf);
	fclose(fontf);
	call_sh("cat %s >>%s; mkdir -p %s/%d; mv -f %s %s/%d",
		tmpname,namebuf,fontdir,density,namebuf,fontdir,density);
	unlink(tmpname);
    }
    free(fh);
}

