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

/* tex 2 gif translator */

#include "texgif.h"

char *progname;
char *tmpdir="/tmp";
char *fontdir="/tmp";
char *headerfile="";
char *texstyle="";
char *outfile="/tmp/texgif.gif";
char cwd[1024];
double blacker=0.4;
int basedensity=100, density;
int compressratio=1;
int wrapexec=0;
int currentcolor;

FONT wfont[FONT_NUMBER_LIMIT];
int fontcnt;

enum {tt_plain, tt_latex};
int textype=tt_plain;
char *texname="tex";

void checktextype(char *p)
{
    if(strstr(p,"\\begin{")!=NULL ||
       strstr(p,"\\end{")!=NULL ||
       strstr(p,"\\underset")!=NULL ||
       strstr(p,"\\overset")!=NULL ||
       strstr(p,"\\displaystyle")!=NULL ||
       strstr(p,"\\mathcal")!=NULL) {
	textype=tt_latex; texname="latex";
    }
}

	/* pass thru tex */
void tex(void)
{
    char *src;
    char *hd;
    char fbuf[1024], nbuf[2048];
    char *parmbuf[8];
    FILE *f;
    long int l;
    src=getenv("texgif_src");
    if(src==NULL || *src==0) texgif_error("No source.");
    checktextype(src);
    snprintf(fbuf,sizeof(fbuf),"%s/texgif.tex",tmpdir);
    if(*headerfile) {
     if (strstr(headerfile,".latex")!=NULL) {textype=tt_latex; texname="latex";}
	 if(strstr(headerfile,".tex")==NULL && strstr(headerfile,".latex")==NULL)
	  snprintf(nbuf,sizeof(nbuf),"%s.%s",headerfile,texname);
	else
	  snprintf(nbuf,sizeof(nbuf),"%s",headerfile);
	f=fopen(nbuf,"r");
	if(f!=NULL) {
	    fseek(f,0,SEEK_END); l=ftell(f); fseek(f,0,SEEK_SET);
	    if(l>0 && l<FILE_LENGTH_LIMIT) {
		hd=xmalloc(l+16); (void)fread(hd,1,l,f);
	    }
	    else {l=0; hd="";}
	    fclose(f); f=fopen(fbuf,"w");
	    fwrite(hd,1,l,f); free(hd);
	}
	else f=fopen(fbuf,"w");
    }
    else f=fopen(fbuf,"w");
    if(textype==tt_plain) {
	fprintf(f,"%s %s %s\n\\end\n",texstyle, src, texstyle); fclose(f);
    }
    else {
	fprintf(f,"{\\newpage\\clearpage\n\
%s %s %s\n\
\\clearpage}\n\
\\end{document}\n",texstyle, src, texstyle); fclose(f);
    }
    if (chdir(tmpdir)) texgif_error("chdir failure");
    parmbuf[0]=texname; parmbuf[1]="texgif"; parmbuf[2]=NULL;
    wrapexec=1;
    execredirected(texname,NULL,NULL,NULL,parmbuf);
    if (cwd[0] && chdir(cwd)) texgif_error("chdir failure");
}

void parms(void)
{
    char *p;
    int t;
    p=getenv("tmp_dir"); if(p!=NULL && *p!=0) tmpdir=p;
    p=getenv("texgif_tmpdir"); if(p!=NULL && *p!=0) tmpdir=p;
    p=getenv("texgif_fontdir"); if(p!=NULL && *p!=0) fontdir=p;
    p=getenv("texgif_outfile"); if(p!=NULL && *p!=0) outfile=p;
    p=getenv("texgif_texheader"); if(p!=NULL && *p!=0) headerfile=p;
    p=getenv("texgif_style"); if(p!=NULL && *p!=0) texstyle=p;
    p=getenv("texgif_density"); if(p!=NULL && *p!=0) {
	t=atoi(p); if(t>10 && t<1000) basedensity=t;
    }
    p=getenv("texgif_compressratio"); if(p!=NULL && *p!=0) {
	t=atoi(p); if(t>1 && t<=5) compressratio=t;
    }
    if(getcwd(cwd,sizeof(cwd))==NULL) cwd[0]=0;
}

int main(int argc, char *argv[])
{
    progname=argv[0];
    parms();
    fontcnt=0;
    tex();
    if(setreuid(geteuid(),geteuid()) || setregid(getegid(),getegid()))
        texgif_error("privilege drop failed");
    dvi();
    return 0;
}

