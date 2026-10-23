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

/* tex 2 gif translator, image manipulating routines */
#include "texgif.h"

gdImagePtr image=NULL;
int color_white, color_black, color_bounder;

/* same in Flydraw/flylines.c */

int getcolor(int r, int g, int b)
{
    int col;
    if(r>255) r=255;
    if(r<0) r=0;
    if(g>255) g=255;
    if(g<0) g=0;
    if(b>255) b=255;
    if(b<0) b=0;
    col=gdImageColorExact(image, r, g, b);
    if(col==-1) col=gdImageColorAllocate(image,r,g,b);
    return col;
}

void createimage(int xsize, int ysize)
{
    if(xsize<=0 || ysize<=0) texgif_error("Bad image size.");
    if(xsize*ysize>IMAGE_SIZE_LIMIT) texgif_error("Image size too big.");
    image=gdImageCreate(xsize,ysize);
    if(image==NULL) texgif_error("Image creation failure");
    color_white=gdImageColorAllocate(image,255,255,255);
    color_black=gdImageColorAllocate(image,0,0,0);
    color_bounder=gdImageColorAllocate(image,1,2,3);
    gdImageColorTransparent(image,color_white);
    printf("Created image: %dx%d\n",xsize,ysize);
}

void saveimage()
{
    int xsize, ysize;
    int white2;
    FILE *outf;
    char *ff;
    if(image==NULL) return;
    xsize=image->sx; ysize=image->sy;
    ff=outfile; while(isspace(*ff)) ff++;
    for(; *outfile && *outfile!='\n';outfile++);
    if(*outfile) *outfile++=0;
    outf=fopen(ff,"w"); if(outf==NULL) texgif_error("unable to create gif output.");
    printf("Output to %s.\n",ff);
    if(compressratio==1) gdImageGif(image,outf);
    else {
        gdImagePtr image2;
        int x2, y2;
        x2=(xsize+1)/2; y2=(ysize+1)/2;
        image2=gdImageCreate(x2,y2);
        white2=gdImageColorAllocate(image2,255,255,255);
        gdImageColorTransparent(image2,white2);
        gdImageCopyResized(image2,image,0,0,0,0,x2,y2,xsize,ysize);
        gdImageGif(image2,outf); gdImageDestroy(image2);
    }
    fclose(outf);
    gdImageDestroy(image); image=NULL;
}

int paintfont(FONT *f, int ch, int x, int y, int color)
{
    int i,j, xlen, ylen, xsize;
    char *buf;
    ch-=f->bc; x+=f->fh[ch].xstart; y+=f->fh[ch].ystart;
    xlen=f->fh[ch].xmax; ylen=f->fh[ch].ymax;
    xsize=(xlen+7)/8; buf=f->data+f->fh[ch].start;
    for(j=0;j<ylen;j++) for(i=0;i<xlen;i++) {
        if((buf[j*xsize+(i>>3)]&(1<<(i&7)))!=0)
            gdImageSetPixel(image,x+i,y+j,color);
    }
    return f->fh[ch].w;
}

    /* Make color according to string indication */
void makecolor(char *p)
{
    int t, r,g,b;
    char *p2;

    p=find_word_start(p); r=g=b=0;
    p2=strchr(p,',');
    if(p2==NULL) {  /* color name */
        *find_word_end(p)=0;
        t=search_list(colors,colorno,sizeof(colors[0]),p);
        if(t<0) return;
        r=colors[t].r; g=colors[t].g; b=colors[t].b;
    }
    else {  /* rgb values */
        *p2++=0;
        r=atoi(p); p=find_word_start(p2);
        p2=strchr(p,','); if(p2==NULL) p2=p+strlen(p); else *p2++=0;
        g=atoi(p); b=atoi(p2);
    }
    t=getcolor(r,g,b);
    if(t!=-1) currentcolor=t;
}
