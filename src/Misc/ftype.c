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

/* Check type of a file */

/*************** Customization: change values hereafter ****************/

/* limit of data buffers */
#define buflim 1024*1024*16

/***************** Nothing should need change hereafter *****************/

#include "../Lib/libwims.h"

char filename[1024]="";
char *filebuf;
int filelen=0;

/* get the file */
void prepare_file(void)
{
  FILE *f;
  long int flen;

  filelen=0;
  f=fopen(filename,"r"); if(f==NULL) return;
  fseek(f,0,SEEK_END);flen=ftell(f); fseek(f,0,SEEK_SET);
  if(flen>buflim) return;
  filebuf=xmalloc(2*flen+1024);flen=fread(filebuf,1,flen,f);
  fclose(f);
  if(flen>0 && flen<buflim) filebuf[flen]=0; else flen=0;
  filelen=flen;
}

int main(int argc, char *argv[])
{
  char *ftype="text";
  char *p;
  char *mod, *unt;

  unt=getenv("untrust");
  if(unt!=NULL && strcasecmp(unt,"yes")==0) return 1;
  mod=getenv("w_module");
  if(mod==NULL) p=argv[1]; else p=getenv("wims_exec_parm");
  if(p==NULL || *p==0) return 1;
  snprintf(filename,sizeof(filename)-128,"%s",p);
  prepare_file();
  for(p=filebuf;p<filebuf+filelen;p++) {
    if((*p>=0 && *p<=6) || (127&*p)<=1)
      {ftype="binary"; goto fin;}
  }
  for(p=strchr(filebuf,'<'); p!=NULL; p=strchr(p,'<')) {
    p++;
    if((strncasecmp(p,"body",4)==0 && !isalnum(*(p+4))) ||
       (strncasecmp(p,"html",4)==0 && !isalnum(*(p+4))) ||
       (strncasecmp(p,"img",3)==0 && isspace(*(p+3))) ||
       strncasecmp(p,"p>",2)==0 ||
       strncasecmp(p,"/a>",3)==0) {
      ftype="html"; goto fin;
    }
  }
  if(strstr(filebuf,"\\begin{")!=NULL ||
     strstr(filebuf,"\\end{")!=NULL ||
     (strchr(filebuf,'$')!=NULL && strchr(filebuf,'\\')!=NULL &&
    strchr(filebuf,'{')!=NULL)) {
    ftype="latex"; goto fin;
  }
  fin: printf("%s",ftype);
  return 0;
}
