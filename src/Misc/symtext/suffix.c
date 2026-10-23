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
#include "symtext.h"

#define suflim    256
#define sufbuflim 102400

int suffixcnt;
struct {
    unsigned char *original;
    int olen;
    unsigned char *replace;
}
suf[suflim];
char *sufbuf;
int sufwordlen, sufminlen;

/* Suffix translation, to be used within translator. */

int sufcomp(int t, const unsigned char *s2)
{
    int k;

    for(k=0;k<suf[t].olen && k<sufwordlen
      && suf[t].original[k]==s2[sufwordlen-k-1];k++);
    if(k>=suf[t].olen) {
      if(sufwordlen>k) return -1; else return 0;
    }
    else return suf[t].original[k]-s2[sufwordlen-k-1];
}

/* searches a list. Returns index if found, -1 if nomatch.
 * This routine is faster than naive one by one comparisons,
 * and is especially suited for large lists.
 */
int suffix_list(void *list, int items, size_t item_size, const unsigned char *str)
{
    int i1,i2,j,k,t,v;
    unsigned char c,d;

    if(items<=0) return -1;
    k=sufcomp(0,str);
    if(k==0) return 0; if(k>0) return -1;
    j=items-1; k=sufcomp(j,str);
    if(k==0) return j;
    if(k>0) for(i1=0,i2=j;i2>i1+1;) {
      j=i1+(i2-i1)/2; k=sufcomp(j,str);
      if(k==0) return j;
      if(k>0) {i2=j; continue;}
      if(k<0) {i1=j; continue;}
    }
    if(k>0 && j>0) j--;
    backcheck:
    v=j;for(t=0;t<suf[j].olen && t<sufwordlen
      && suf[j].original[t]==str[sufwordlen-t-1];t++);
    if(t<sufminlen) return -1; if(t>=suf[j].olen) return j;
    for(j--,c=str[sufwordlen-1],d=str[sufwordlen-t];
      j>=0 && suf[j].original[0]==c && suf[j].olen>t
      && suf[j].original[t-1]==d;j--);
    if(j>=0 && suf[j].original[0]==c &&
       strncmp((char*)suf[j].original,(char*)suf[v].original,suf[j].olen)==0)
      return j;
    else goto backcheck;
}

/* Prepare dictionary.  */
void suffix_dic(char *sdicname)
{
    int i,k,l;
    FILE *suff;
    char *p1, *p2, *pp;
    long int flen;

    suffixcnt=0; sufminlen=100000;
    suff=fopen(sdicname,"r"); if(suff==NULL) return;
    fseek(suff,0,SEEK_END);flen=ftell(suff); fseek(suff,0,SEEK_SET);
    if(flen>sufbuflim) return;
    sufbuf=xmalloc(flen+16);flen=fread(sufbuf,1,flen,suff);
    fclose(suff);
    if(flen>0 && flen<sufbuflim) sufbuf[flen]=0;
    else return;
    for(i=0,p1=sufbuf;p1!=NULL && *p1!=0 && i<suflim;p1=p2) {
      p2=strchr(p1+1,'\n'); if(p2>p1) *p2++=0;
      pp=strchr(p1,':'); if(pp==NULL) continue;
      *pp++=0;
      strip_trailing_spaces(p1); strip_trailing_spaces(pp);
      p1=find_word_start(p1); pp=find_word_start(pp);
      if(*p1==0) continue;
      if(i>0) {
          k=strcmp((char*)suf[i-1].original,p1);
          if(k>0) {
            pp=strrchr(sdicname,'/'); if(pp==NULL) pp=sdicname; else pp++;
            sym_error("unsorted_dictionary %s: %s > %s.\n",
                  pp,suf[i-1].original,p1);
          }
          if(k==0) {
            pp=strrchr(sdicname,'/'); if(pp==NULL) pp=sdicname; else pp++;
            sym_error("duplication_in_dictionary %s: %s.\n",pp,p1);
          }
      }
      suf[i].original=(unsigned char*)p1; suf[i].olen=l=strlen(p1);
      if(l<sufminlen) sufminlen=l;
      suf[i].replace=(unsigned char*)pp; i++;
    }
    suffixcnt=i;
}

/* Suffix translation. */
void suffix_translate(char *p)
{
    char *p1, *p2;
    int t;

    for(p1=find_word_start(p);
      p1!=NULL && p1-p<MAX_LINELEN && *p1!=0;
      p1=p2) {
       if(!isalpha(*p1)) {p2=p1+1; continue;}
       for(p2=p1;isalpha(*p2);p2++);
       if(*p2!=0 && strchr(" ,.?!'\"\n`:;()[]{}<>",*p2)==NULL) continue;
       sufwordlen=p2-p1;
       t=suffix_list(suf,suffixcnt,sizeof(suf[0]),(unsigned char*)p1);
       if(t<0) continue;
       string_modify(p,p2-suf[t].olen,p2,(char*)suf[t].replace);
       p2=p2-suf[t].olen+strlen((char*)suf[t].replace);
     }
     p[MAX_LINELEN]=0;
}

void suffix(char *p, char *sdicname)
{
    suffix_dic(sdicname); if(suffixcnt>0) suffix_translate(p);
}

