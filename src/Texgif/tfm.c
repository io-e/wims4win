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

/* dvi 2 gif driver, tfm loader */
#include "texgif.h"

struct tfm tfm;

short int tfmhd[12];

#define tfmlf tfmhd[0]
#define tfmlh tfmhd[1]
#define tfmbc tfmhd[2]
#define tfmec tfmhd[3]
#define tfmnw tfmhd[4]
#define tfmnh tfmhd[5]
#define tfmnd tfmhd[6]
#define tfmni tfmhd[7]
#define tfmnl tfmhd[8]
#define tfmnk tfmhd[9]
#define tfmne tfmhd[10]
#define tfmnp tfmhd[11]

void loadtfm(char *fname)
{
    int i, k, t, len, charcnt;
    char namebuf[1024];
    int32_t *wpbase;
    int32_t *headp, *fp, *fip, *wp, *hp, *dp;
    unsigned char *tfmbuf, *ftp;

    memset(&tfm,0,sizeof(tfm));
    snprintf(namebuf,sizeof(namebuf),"%s/%s.tfm",tmpdir,fname);
    call_sh("cp `kpsewhich %s.tfm` %s",fname,tmpdir);
    len=getfile(namebuf,&tfmbuf); unlink(namebuf);
    if(len<0) {
	texgif_error("Error reading tfm file.");
    }
    for(i=0;i<12;i++) tfmhd[i]=texint(tfmbuf+2*i,2);
    tfm.bc=tfmbc; tfm.ec=tfmec;
    charcnt=tfmec-tfmbc+1;
    if(charcnt<=0) return;
    wpbase=(int32_t *) tfmbuf;
    headp=wpbase+6; 		/* header */
    fp=headp+tfmlh; 		/* font info */
    wp=fp+charcnt;		/* width table */
    hp=wp+tfmnw;		/* height table */
    dp=hp+tfmnh;		/* depth table */
    tfm.checksum=headp[0];
    tfm.designsize=texint(headp+1,4);
    for(i=0, t=tfmbc;i<charcnt; i++,t++) {
	fip=fp+i; ftp=(unsigned char *) fip;
	k=*ftp; tfm.f[t].w=texint(wp+k,4); ftp++;
	k=((*ftp)>>4)&15; tfm.f[t].h=texint(hp+k,4);
	k=(*ftp)&15; tfm.f[t].d=texint(dp+k,4);
    }
    free(tfmbuf);
}

