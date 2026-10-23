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

/* This program computes an optimal coding of variable lengths
 * on a given distribution of probabilities,
 * using Huffman algorithm */

/* Input data: via two environment variables.
 * wims_exec_parm is a comma-separated list of probability distributions.
 * Limited to MAX_ITEMS.
 * The input data will be scaled to unit sum.
 * w_huffman_radix is the encoding radix, between 2 and MAX_RADIX.
 *
 * Output: two lines.
 * Line 1: Entropy and Average code length, comma-separated.
 * Line 2: comma-separated list of codes.
 */

#define MAX_ITEMS 2048
#define MAX_CODELEN 100

const char *codechar="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
#define MAX_RADIX strlen(codechar)

#include "../Lib/libwims.h"

struct {
  double prob;
  int ind;
  unsigned char code[MAX_CODELEN];
  int codelen;
} maintab[MAX_ITEMS*2];
int itemcnt, origcnt;

int indtab[MAX_ITEMS];
int indcnt;
int radix;
double entropy, avelen;


int indcmp(const void *p1, const void *p2)
{
  const int *i1, *i2;
  double d1, d2;

  i1=p1; i2=p2;
  d1=maintab[*i1].prob; d2=maintab[*i2].prob;
  if(d1==d2) return 0;
  if(d1>d2) return -1;
  else return 1;
}

void huffman(void)
{
  int t, i, j, l;
  double d;

  while(indcnt>radix) {
    qsort(indtab,indcnt,sizeof(indtab[0]),indcmp);
    if(radix>2) t=(indcnt+radix-3)%(radix-1)+2;
    else t=2;
    d=0;
    for(i=indcnt-t; i<indcnt; i++) {
      d+=maintab[indtab[i]].prob;
      maintab[indtab[i]].ind=itemcnt;
    }
    maintab[itemcnt].prob=d;
    maintab[itemcnt].ind=-1;
    maintab[itemcnt].codelen=-1;
    indtab[indcnt-t]=itemcnt;
    indcnt-=t-1; itemcnt++;
  }
  for(i=0;i<indcnt;i++) {
    maintab[indtab[i]].codelen=1;
    maintab[indtab[i]].code[0]=i;
    maintab[indtab[i]].ind=0;
  }
  for(i=itemcnt-1;i>=0;i--) {
    if(maintab[i].codelen>0) continue;
    j=maintab[i].ind; l=maintab[j].codelen;
    if(l>=MAX_CODELEN) error("Code too long.");
    memmove(maintab[i].code,maintab[j].code,l);
    maintab[i].code[l]=maintab[j].ind++;
    maintab[i].codelen=l+1;
    maintab[i].ind=0;
  }
}

void output(void)
{
  int i, j;
  double d;

  d=0;
  for(i=0;i<origcnt;i++) d+=maintab[i].prob*maintab[i].codelen;
  printf("%f,%f\n",entropy,d);
  for(i=0;i<origcnt;i++) {
    for(j=0;j<maintab[i].codelen;j++) printf("%c",codechar[(int) maintab[i].code[j]]);
    if(i<origcnt-1) printf(",");
    else printf("\n");
  }
}

void getparm(char *p)
{
  int i;
  char *p1, *p2;
  double d1, dt;

  origcnt=0; dt=0;
  for(p1=find_word_start(p);
      *p1; p1=find_word_start(p2)) {
    for(p2=p1; *p2 && strchr(",;",*p2)==NULL; p2++);
    if(*p2) *p2++=0;
    d1=strevalue(p1);
    if(!isfinite(d1) || d1<0) {
      char buf[256];
      snprintf(buf,sizeof(buf),"Bad data: %s",p1);
      error(buf);
    }
    maintab[origcnt++].prob=d1;
    dt+=d1;
  }
  if(dt*1000000<1) error("Empty data sum.");
  if(origcnt<2) error("Insufficient data for encoding.");
  itemcnt=origcnt;
  if(dt!=1) for(i=0; i<origcnt; i++) maintab[i].prob/=dt;

  entropy=0;
  for(i=0;i<origcnt;i++) {
    maintab[i].codelen=-1; maintab[i].ind=-1;
    indtab[i]=i;
    d1=maintab[i].prob;
    if(d1>0) entropy-=d1*log(d1);
  }
  entropy=entropy/log(radix);
  indcnt=origcnt;
}

int main()
{
  char *p;
  int r;

  p=getenv("w_huffman_radix");
  if(p==NULL || *p==0) p=getenv("huffman_radix");
  if(p==NULL || *p==0) radix=2;
  else {
    r=atoi(p); if(r!=0) radix=r;
  }
  if(radix<2 || radix>MAX_RADIX) error("Bad radix.");

  p=getenv("wims_exec_parm");
  if(p==NULL || *p==0) error("No input data.");
  getparm(p);

  huffman();
  output();

  return 0;
}

