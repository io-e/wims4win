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

/* Sort dictionary */

/*************** Customization: change values hereafter ****************/

/* limit of dictionary entries */
#define entrylim 512*1024
/* limit of dictionary length */
#define diclim 32*1024*1024
/* separation character */
char sepchar=':', grpchar=0;

/***************** Nothing should need change hereafter *****************/

#include "../Lib/libwims.h"
#include "suffix.h"

char inpbuf[MAX_LINELEN+1], outbuf[2*MAX_LINELEN+2];
char *dicbuf;
char dicname[1024], suffixname[1024];

struct entry {
  char *original;
  char *replace;
} entry[entrylim];
int entrycount;

int nocase=0, hassuffix=0, leaveline=0;
int entrycount, ocount;

int compare(const void *s1, const void *s2)
{
  const struct entry *p1, *p2;
  p1=s1; p2=s2;
  if(nocase) return strcasecmp(p1->original,p2->original);
  else return strcmp(p1->original,p2->original);
}

void sortdic(void)
{
  qsort(entry,entrycount,sizeof(entry[0]),compare);
}

/* change all spaces into ' ', and collapse multiple occurences */
void singlespace2(char *p)
{
  char *pp, *p2;
  for(pp=p;*pp;pp++) {
    if(!isspace(*pp)) continue;
    if(leaveline) {
      if(*pp==13) ovlstrcpy(pp,pp+1);
      if(*pp=='\n') {
        pp++;
        gopt: for(p2=pp; isspace(*p2) && *p2!='\n'; p2++);
        if(p2>pp) ovlstrcpy(pp,p2);
        pp--;
      }
      else {
        pp++;
        if(!isspace(*pp) || *pp=='\n') continue;
        goto gopt;
      }
    }
    else {
      if(*pp!=' ') *pp=' ';
      pp++; if(!isspace(*pp)) continue;
      for(p2=pp;isspace(*p2);p2++);
      ovlstrcpy(pp,p2); pp--;
    }
  }
}

/* Prepare dictionary */
static
void prepare_dics(void)
{
  int i;
  FILE *dicf;
  char *p1, *p2, *pp;
  long int flen;

  entrycount=0;
  dicf=fopen(dicname,"r"); if(dicf==NULL) return;
  fseek(dicf,0,SEEK_END);flen=ftell(dicf); fseek(dicf,0,SEEK_SET);
  if(flen>diclim) return;
  dicbuf=xmalloc(2*flen+1024);flen=fread(dicbuf,1,flen,dicf);
  fclose(dicf);
  if(flen>0 && flen<diclim) dicbuf[flen]=0;
  else return;
  for(i=0,p1=dicbuf;p1!=NULL && *p1!=0 && i<entrylim;p1=p2) {
    p2=strchr(p1+1,'\n'); if(p2>p1) *p2++=0;
    pp=strchr(p1,sepchar); if(pp==NULL) continue;
    *pp++=0;
    strip_trailing_spaces2(p1); strip_trailing_spaces2(pp);
    singlespace2(p1);
    p1=find_word_start(p1); pp=find_word_start(pp);
    if(*p1==0) continue;
    entry[i].original=p1; entry[i].replace=pp; i++;
  }
  entrycount=i;
}

void output(void)
{
  int i;
  FILE *f;

  ocount=0;
  strcat(dicname,".sorted");
  f=fopen(dicname,"w"); if(f==NULL) return;
  for(i=0;i<entrycount;i++) {
    if(i>0 && strcmp(entry[i].original,entry[i-1].original)==0
       && strcmp(entry[i].replace,entry[i-1].replace)==0)
      continue;
    if(grpchar!=0) {
      if(i>0 && strcmp(entry[i].original,entry[i-1].original)==0)
        fprintf(f,"%c%s",grpchar, entry[i].replace);
      else {
        if(i>0) fprintf(f,"\n");
        fprintf(f,"%s%c%s",entry[i].original,sepchar,entry[i].replace);
        ocount++;
      }
    }
    else {
      fprintf(f,"%s%c%s\n",entry[i].original,sepchar,entry[i].replace);
      ocount++;
    }
  }
  if(grpchar!=0) fprintf(f,"\n");
  fclose(f);
}

int main(int argc, char *argv[])
{
  char *ss, *gr;
  if(argc<2) return -1;

  ss=getenv("dicsort_separator");
  if(ss!=NULL && *ss!=0) sepchar=*ss;
  gr=getenv("dicsort_grouping");
  if(gr!=NULL && *gr!=0) grpchar=*gr;
  snprintf(dicname,sizeof(dicname)-128,"%s",argv[1]); prepare_dics();
  if(argc>2) {
    snprintf(suffixname,sizeof(suffixname),"%s",argv[2]);
    suffix_dic(suffixname); hassuffix=1;
  }
  else suffixname[0]=hassuffix=0;
  sortdic(); output();
  printf("%s: sorted %d entries.\n",dicname, ocount);
  return 0;
}

