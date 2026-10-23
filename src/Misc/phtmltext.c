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

/* Extract text from phtml file */

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

void processbuf(void)
{
  char *p;
  deaccent(filebuf);
  for(p=filebuf; *p; p++) *p=tolower(*p);
  for(p=strpbrk(filebuf,"'-"); p!=NULL; p=strpbrk(p+1,"'-")) *p=' ';
  for(p=strstr(filebuf,"&nbsp;"); p!=NULL; p=strstr(p+1,"&nbsp;")) {
    *p=' '; ovlstrcpy(p+1,p+6);
  }
}

void output(void)
{
  char *p, *pp, lastc;
  p=find_word_start(filebuf); lastc=0;
  if(*p=='!' || *p==':') goto cont1;
  for(;*p;p++) {
    if(*p=='\n') {
      if(!isspace(lastc)) {printf(" "); lastc=' ';}
      cont2: p=find_word_start(p);
      if(*p=='!' || *p==':') {
        if(lastc!='	') printf(". ");
        lastc='	';
        cont1: p=strchr(p,'\n');
        if(p==NULL) return;
        if(*(p-1)=='\\') {p++; goto cont1;}
        goto cont2;
      }
      for(pp=p; isalnum(*pp) || *pp=='_' || *pp=='$'; pp++);
      pp=find_word_start(pp);
      if(*pp=='=') goto cont1;
    }
    if(*p=='\\' && *(p+1)=='\n') {
      printf("\n"); p++; continue;
    }
    if(*p=='<' && (isalpha(*(p+1)) || *(p+1)=='/')) {
        p=strchr(p,'>'); goto nextp;
    }
    if(*p=='$') {
      if(lastc != '	') {
        if(!isspace(lastc)) printf(" ");
        printf(". "); lastc='	';
      }
      p++;
      if(*p=='(') {p=find_matching(p+1,')'); goto nextp;}
      if(*p=='[') {p=find_matching(p+1,']'); goto nextp;}
      while(isalnum(*p) || *p=='_') p++;
      p--; continue;
    }
    if(*p=='&') {
      char *p2;
      for(p2=p+1; isalnum(*p2) || *p2=='#'; p2++);
      if(*p2==';') {
        p++; if(isalpha(*p)) {printf("%c",*p); lastc=*p;}
        p=p2; continue;
      }
    }
    if(!isspace(*p) && strchr(":!?.;,\"()[]{}=/\\+*^%@~`<>|",*p)==NULL)
      {printf("%c",*p); lastc=*p;}
    else {
      if(isspace(*p) && !isspace(lastc)) {
        printf(" "); lastc=' ';
      }
      if(!isspace(*p)) {
        switch(lastc) {
          case ' ': printf(". "); lastc='	'; break;
          case '	': break;
          default: printf(" . "); lastc='	'; break;
        }
      }
    }
    nextp: if(p==NULL || *p==0) break;
  }
}

int main(int argc, char *argv[])
{
  if(argc<=1) return 0;
  snprintf(filename,sizeof(filename)-128,"%s",argv[1]);
  prepare_file();
  processbuf();
  output();
  return 0;
}

