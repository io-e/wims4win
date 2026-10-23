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

/* Versatile translation according to a dictionary */

/*************** Customization: change values hereafter ****************/
#include "suffix.h"
#include "../Lib/libwims.h"
#include "translator_.h"
/* limit of dictionary entries */
#define entrylim 32768
/* limit of dictionary length */
#define diclim 1024*1024

/***************** Nothing should need change hereafter *****************/


char inpbuf[MAX_LINELEN+1], outbuf[MAX_LINELEN];
char *dicbuf;
struct entry entry[entrylim];
int entrycount;
int entry_size=sizeof(entry);

int has_digits=0;
int unknown_type=unk_delete;
int nocase=0,leaveline=0;
char *unknown, unkbuf[1024];

int compare(int i1, const char *s2)
{
    int k;
    if(nocase) k=strncasecmp((char*)entry[i1].original,s2,entry[i1].olen);
    else k=strncmp((char*)entry[i1].original,s2,entry[i1].olen);
    if(k==0 && (isalnum(*(s2+entry[i1].olen)) || (*(s2+entry[i1].olen)&128)!=0)) return -1;
    else return k;
}

/* searches a list. Returns index if found, -1 if nomatch.
 * Uses binary search, list must be sorted. */
int search_list2(struct entry *list, int items, size_t item_size, const char *str)
{
    int i1,i2,j,k,t,t1;
    unsigned char c;

    if(items<=0) return -1;
    j=0; c=str[0];
    k=list[0].original[0]-c; if(k==0) k=compare(0,str);
    if(k==0) goto more;
    if(k>0) return -1;
    j=items-1; k=list[j].original[0]-c; if(k==0) k=compare(j,str);
    if(k==0) return j;
    if(k>0) for(i1=0,i2=j;i2>i1+1;) {
      j=i1+(i2-i1)/2;
      k=list[j].original[0]-c; if(k==0) k=compare(j,str);
      if(k==0) goto more;
      if(k>0) {i2=j; continue;}
      if(k<0) {i1=j; continue;}
    }
    if(k>0) {j--;k=compare(j,str);}
    more:
    if((t=list[j].earlier)<0) {
      if(k==0) return j; else return -1;
    }
    if(compare(t,str)!=0) return -1;
    for(j=t1=t,k=0;j<items && list[j].earlier==t1 && (k=compare(j,str))<=0; j++) {
      if(k==0) t=j;
    }
    return t;
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
          pp++; if(!isspace(*pp) || *pp=='\n') continue;
          goto gopt;
        }
    }
    else {
        if(*pp!=' ') *pp=' ';
        if(!isspace(*(pp+1))) continue;
        for(pp++,p2=pp;isspace(*p2);p2++);
        ovlstrcpy(pp,p2); pp--;
    }
  }
}

/* Prepare dictionary */
void prepare_dic(char *fname)
{
  int i,l;
  FILE *dicf;
  char *p1, *p2, *pp;
  long int flen;

  entrycount=0;
  dicf=fopen(fname,"r"); if(dicf==NULL) return;
  fseek(dicf,0,SEEK_END);flen=ftell(dicf); fseek(dicf,0,SEEK_SET);
  if(flen>diclim) return;
  dicbuf=xmalloc(flen+16);flen=fread(dicbuf,1,flen,dicf);
  fclose(dicf);
  if(flen>0 && flen<diclim) dicbuf[flen]=0;
  else return;
  for(i=0,p1=dicbuf;p1!=NULL && *p1!=0 && i<entrylim;p1=p2) {
    p2=strchr(p1+1,'\n'); if(p2>p1) *p2++=0;
    pp=strchr(p1,':'); if(pp==NULL) continue;
    *pp++=0;
    strip_trailing_spaces2(p1); strip_trailing_spaces2(pp);
    singlespace2(p1);
    p1=find_word_start(p1); pp=find_word_start(pp);
    if(*p1==0) continue;
    if(has_digits==0) {
        char *p;
        for(p=p1;*p!=0 && p<pp && !isdigit(*p);p++);
        if(isdigit(*p)) has_digits=1;
    }
    entry[i].original=(unsigned char*)p1;
    entry[i].replace=(unsigned char*)pp;
    entry[i].olen=l=strlen(p1); entry[i].earlier=-1;
    if(i>0) {
        int l1,l2;
        l1=entry[i-1].earlier; if(l1>=0) l2=entry[l1].olen;
        else {l2=entry[i-1].olen;l1=i-1;}
        if(l>l2 && isspace(p1[l2])
           && strncmp((char*)entry[l1].original,p1,l2)==0)
          entry[i].earlier=entry[i-1].earlier=l1;
    }
    i++;
  }
  entrycount=i;
}

/* now make the translation. */
void translate(char *p)
{
  char *p1, *p2, *pp;
  int t;

  if(entrycount<=0 && suffixcnt<=0) return;
  snprintf(outbuf,sizeof(outbuf),"%s",p);
  for(p1=find_word_start(outbuf);
        p1!=NULL && p1-outbuf<MAX_LINELEN && *p1!=0;
        p1=p2) {
    p2=find_word_end(p1);
    for(pp=p1;pp<p2 &&
      ((!has_digits && isalpha(*pp)) ||
       (has_digits && isalnum(*pp)) || (*pp&128)!=0 ||
      strchr("_",*pp)!=NULL);pp++);
    p2=find_word_start(p2);
    if(pp==p1 ||
      (has_digits==0 && isdigit(*pp)) ||
      (*pp!=0 && !isspace(*pp) && strchr(",.?!/;",*pp)==NULL)) continue;
    t=search_list2(entry,entrycount,sizeof(entry[0]),p1);
    if(t<0) {
      switch(unknown_type) {
        case unk_leave: break;
        case unk_delete: {
          ovlstrcpy(p1,find_word_start(pp)); p2=p1;
          break;
        }
        case unk_replace: {
          string_modify3(outbuf,p1,pp,unkbuf);
          p2=find_word_start(p1+strlen(unkbuf));
        }
      }
      continue;
    }
    string_modify3(outbuf,p1,p1+strlen((char*)entry[t].original),
                  (char*)entry[t].replace);
    p2=find_word_start(p1+strlen((char*)entry[t].replace));
  }
  snprintf(p,MAX_LINELEN,"%s",outbuf);
}
