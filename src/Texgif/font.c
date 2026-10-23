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

/* tex 2 gif translator, font loader */
#include "texgif.h"

void mf2font(char *fontname, int density)
{
    loadtfm(fontname);
    makegf(fontname,density);
    loadgf(fontname,density);
    gf2font(fontname,density);
}

FONT *loadfont(char *fname, int checksum, int density, FONT *ft)
{
    char namebuf[2048];
    char *fcache, *cc;
    long int l,l2;
    FILE *f;
    int newfont;

    newfont=0;
    for(cc=fname; isalnum(*cc); cc++);
    if(*cc!=0) texgif_error("Bad font name.");
    snprintf(namebuf,sizeof(namebuf),"%s/%d/%s.font",fontdir,density,fname);
    f=fopen(namebuf,"r"); if(f==NULL) {
	renewfont: mf2font(fname,density);
	f=fopen(namebuf,"r"); newfont=1;
    }
    if(f==NULL) return NULL;
    fseek(f,0,SEEK_END); l=ftell(f); fseek(f,0,SEEK_SET);
    if(l<=0 || l>FILE_LENGTH_LIMIT) return NULL;
    fcache=xmalloc(l+16);
    l2=fread(fcache,1,l,f); if(l2!=l) {
	texgif_error("Error reading font file.");
    }
    memmove(&ft->checksum,fcache,sizeof(int));
    memmove(&ft->designsize,fcache+sizeof(int),sizeof(int));
    memmove(&ft->bc,fcache+2*sizeof(int),sizeof(int));
    memmove(&ft->ec,fcache+3*sizeof(int),sizeof(int));
    ft->cnt=ft->ec-ft->bc+1;
    cc=fcache+4*sizeof(int); ft->fh=(FONTHEADER *) cc;
    ft->data=fcache+4*sizeof(int)+ft->cnt*sizeof(FONTHEADER);
    ft->cache=fcache;
    fclose(f);
    if(checksum!=ft->checksum && checksum!=0 && ft->checksum!=0) {
	if(newfont) {
	    fprintf(stderr,"%08X != %08X\n",checksum, ft->checksum);
	    texgif_error("Font checksum discord.");
	}
	free(fcache); unlink(namebuf); goto renewfont;
    }
    return ft;
}

