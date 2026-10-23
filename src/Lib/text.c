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
#include "libwims.h"

char t_buf[4][MAX_LINELEN+1];
char maskbuf[MAX_LINELEN+1];

/* internal routine. */
void _text_cut(char *p, char *w)
{
  char *p1, *p2;
  p1=wordchr(p,w); if(p1==NULL) error("syntax_error");
  *p1=0; p2=find_word_start(p1+strlen(w));
  ovlstrcpy(t_buf[0],p); ovlstrcpy(t_buf[1],p2);
  strip_trailing_spaces(t_buf[0]);
  substitute(t_buf[0]); substitute(t_buf[1]);
}

/* Extract characters in buf[0] which are identical to
 * corresponding characters in buf[1]. */
void text_common(char *p)
{
  int i,j,n1,n2;
  _text_cut(p,"and");
  n1=strlen(t_buf[0]);n2=strlen(t_buf[1]);
  if(n2<n1) n1=n2;
  for(i=j=0;i<n1;i++) {
    if(t_buf[0][i]==t_buf[1][i] && maskbuf[i]!='0') p[j++]=t_buf[0][i];
  }
  p[j]=0;
}

/* Returns a mask string composed of '0's and '1's, where
 * '0' means corresponding positions of buf[0] and buf[1] are
 * equal.
 */
void text_compare(char *p)
{
  int min,max, i;
  _text_cut(p,"and");
  min=strlen(t_buf[0]); max=strlen(t_buf[1]);
  if(min>max) {
    i=min; min=max; max=i;
  }
  for(i=0; i<min; i++) {
    if(t_buf[0][i]==t_buf[1][i]) p[i]='0'; else p[i]='1';
  }
  for(; i<max; i++) p[i]='1';
  p[max]=0;
}

/* copy text according to mask. */
void text_copy(char *p)
{
  int i, j, n;

  snprintf(t_buf[0],MAX_LINELEN,"%s",p);
  strip_trailing_spaces(t_buf[0]); substitute(t_buf[0]);
  n=strlen(t_buf[0]);
  for(i=j=0;i<n;i++) {
    if(maskbuf[i]!='0') p[j++]=t_buf[0][i];
  }
  p[j]=0;
}

/* returns count of characters in buf[1] which appear in buf[0]. */
void text_count(char *p)
{
  int i, n, c;
  _text_cut(p,"in");
  n=strlen(t_buf[1]);
  for(i=c=0;i<n;i++) {
    if(strchr(t_buf[0],t_buf[1][i])!=NULL && maskbuf[i]!='0') c++;
  }
  snprintf(p,MAX_LINELEN,"%d",c);
}

/* Extract characters in buf[0] which are different than
 * corresponding characters in buf[1].
 */
void text_diff(char *p)
{
  int i,j,n1,n2;
  _text_cut(p,"from");
  n1=strlen(t_buf[0]);n2=strlen(t_buf[1]);
  if(n2<n1) n1=n2;
  for(i=j=0;i<n1;i++) {
    if(t_buf[0][i]!=t_buf[1][i] && maskbuf[i]!='0') p[j++]=t_buf[0][i];
  }
  p[j]=0;
}

/* put chars in buf[0] in a new string, into positions
 * corresponding to '1's in the mask buf[1].
 * Positions corresponding to '0's are filled by space.
 * Fill stops at the end of buf[0]. If buf[1] is
 * too short, it is reused from the start.
 * FIXME: using 0 as a mask gives a bug
 */
/*
   @@ !text expand abcdef mask 01
   @@ a b c d e f
   @@ !text expand abcdef mask 011
   @@ ab cd e
   @@ !text expand abcdefg using 101110
   @@ a bcd e fg
 */
void text_expand(char *p)
{
  int i,j,k,n1,n2;
  _text_cut(p,"using");
  n1=strlen(t_buf[0]);n2=strlen(t_buf[1]);
  if(n2==0) {p[0]=0; return;}
  for(i=j=k=0;i<n1 && j<MAX_LINELEN;j++,k=j%n2) {
    if(t_buf[1][k]=='0') p[j]=' ';
    else p[j]=t_buf[0][i++];
  }
  p[j]=0;
}

/* character by character replacement of buf[1] by buf[0],
 * replacing only mask-effective chars.
 * The resulting string is as long as buf[1], and the replacement
 * stops when chars buf[0] has run out.
 */
/*
  @@ !text insert abcefg into hijkl mask 10100
  @@ aibkl
 */
void text_insert(char *p)
{
  int i,j,n1,n2;
  _text_cut(p,"into");
  n1=strlen(t_buf[0]);n2=strlen(t_buf[1]);
  for(i=j=0; i<n2 && j<n1; i++) {
    if(maskbuf[i]!='0') t_buf[1][i]=t_buf[0][j++];
  }
  snprintf(p,MAX_LINELEN,"%s",t_buf[1]);
}

#define MAX_TLEN 96

/* interact of two strings according to rules
 * defined a table.
 */
void text_interact(char *p)
{
  char *table, *dline, *tline[MAX_TLEN];
  char *p1, *p2;
  int i,j1,j2,k,l,l2,n;

  table=wordchr(p,"table");
  if(table==NULL) error("syntax_error");
  *table=0; strip_trailing_spaces(p);
  table=find_word_start(table+strlen("table"));
  snprintf(t_buf[2],MAX_LINELEN,"%s",table);
  _text_cut(p,"and");
  strip_trailing_spaces(t_buf[2]); substitute(t_buf[2]);
  n=linenum(t_buf[2])-1;
  if(n>=MAX_TLEN) error("text_bad_table");
  p2=strchr(t_buf[2],'\n'); if(p2!=NULL) *p2++=0;
  if(strlen(t_buf[2])!=n) error("text_bad_table");
  dline=t_buf[2];
  for(i=0,p1=p2;i<n;i++,p1=p2) {
    if(p1==NULL) error("text_bad_table");
    p2=strchr(p1,'\n');
    if(p2!=NULL) *p2++=0;
    if(strlen(p1)!=n) error("text_bad_table");
    tline[i]=p1;
  }
  l=strlen(t_buf[0]); l2=strlen(t_buf[1]); if(l2<l) l=l2;
  for(i=k=0;i<l;i++) {
    if(maskbuf[i]!='0') {
      p1=strchr(dline,t_buf[0][i]);
      p2=strchr(dline,t_buf[1][i]);
      if(p1==NULL || p2==NULL) continue;
      j1=p1-dline; j2=p2-dline;
      if(j1>=n || j2>=n) continue; /* should not occur */
      p[k++]=tline[j1][j2];
    }
  }
  p[k]=0;
}

/* returns a mask string composed of '0's and '1's, where
 * '0' means corresponding char in buf[1] appears in buf[0].
 */
/*
   @@ !text mark a in abcaefa
   @@ 001001
 */
void text_mark(char *p)
{
  int i, n;
  _text_cut(p,"in");
  n=strlen(t_buf[1]);
  for(i=0;i<n;i++) {
    if(strchr(t_buf[0],t_buf[1][i])!=NULL) p[i]='1';
    else p[i]='0';
  }
  p[i]=0;
}

/* Returns a string whose characters are the maximum
 * of the two corresponding chars in buf[0] and buf[1].
 * Length of the string is the longuest one.
 */
void text_max(char *p)
{
  int min,max, i, j, k;
  _text_cut(p,"and");
  min=strlen(t_buf[0]); max=strlen(t_buf[1]);
  if(min>max) {
    i=min; min=max; max=i; j=0;
  }
  else j=1;
  for(i=k=0; i<min; i++) {
    if(maskbuf[i]=='0') continue;
    if((unsigned char)t_buf[0][i]>(unsigned char)t_buf[1][i])
      p[k++]=t_buf[0][i];
    else p[k++]=t_buf[1][i];
  }
  for(;i<max;i++) {
    if(maskbuf[i]!='0') p[k++]=t_buf[j][i];
  }
  p[k]=0;
}

/* Returns a string whose characters are the minimum
 * of the two corresponding chars in buf[0] and buf[1].
 * Length of the string is the shortest one.
 */
void text_min(char *p)
{
  int min,max, i,k;
  _text_cut(p,"and");
  min=strlen(t_buf[0]); max=strlen(t_buf[1]);
  if(min>max) {
    i=min; min=max; max=i;
  }
  for(i=k=0; i<min; i++) {
    if(maskbuf[i]=='0') continue;
    if((unsigned char)t_buf[0][i]< (unsigned char)t_buf[1][i])
      p[k++]=t_buf[0][i];
    else p[k++]=t_buf[1][i];
  }
  p[k]=0;
}

/* extract chars in buf[0] which occur in buf[1]. */
void text_occur(char *p)
{
  int i,j,n;
  char buf[MAX_LINELEN+1];
  memset(buf,0,sizeof(buf));
  _text_cut(p,"in");
  n=strlen(t_buf[1]);
  for(i=0;i<n;i++) {
    char *pp;
    if(maskbuf[i]=='0') continue;
    pp=strchr(t_buf[0],t_buf[1][i]);
    if(pp!=NULL) buf[pp - t_buf[0]]=1;
  }
  n=strlen(t_buf[0]);
  for(i=j=0;i<n;i++) {
    if(buf[i]) p[j++]=t_buf[0][i];
  }
  p[j]=0;
}

/* remove characters of buf[1] in buf[0]. */
void text_remove(char *p)
{
  int i, j, n;
  _text_cut(p,"in");
  n=strlen(t_buf[1]);
  for(i=j=0;i<n;i++) {
    if(strchr(t_buf[0],t_buf[1][i])==NULL
       && maskbuf[i]!='0') p[j++]=t_buf[1][i];
  }
  p[j]=0;
}

/* Cyclic reordering of text. */
void text_reorder(char *p)
{
  int i,j,k,l,n,t;
  int list[10240];
  char buf[MAX_LINELEN+1];
  _text_cut(p,"by"); *p=0;
  n=itemnum(t_buf[1]); if(n<=0 || n>=10240) return;
  for(i=0;i<n;i++) {
    buf[0]=0; fnd_item(t_buf[1],i+1,buf);
    j=atoi(buf); if(j<=0 || j>n) return;
    list[i]=j;
  }
  t=strlen(t_buf[0]);
  for(i=l=0;l<t && i<t+n;i++) {
    j=i/n; k=j*n+list[i%n];
    if(k>t || k<=0) continue;
    p[l++]=t_buf[0][k-1];
  }
  p[l]=0;
}

/* repeat a string to a given length. */
void text_repeat(char *p)
{
  int n,i,k;
  _text_cut(p,"to");
  n=strevalue(t_buf[1]); if(n>MAX_LINELEN) n=MAX_LINELEN;
  if(n<0) n=0;
  k=strlen(t_buf[0]); if(k<=0) {*p=0; return;}
  for(i=0;i<n;i++) {
    p[i]=t_buf[0][i%k];
  }
  p[i]=0;
}

/* reverse a string */
void text_reverse(char *p)
{
  int i,n;
  char buf[MAX_LINELEN+1];
  snprintf(t_buf[0],sizeof(t_buf[0]),"%s",p);
  substitute(t_buf[0]);
  n=strlen(t_buf[0]); if(n>MAX_LINELEN) n=MAX_LINELEN;
  for(i=0;i<n;i++) buf[i]=t_buf[0][n-1-i];
  buf[n]=0;
  ovlstrcpy(p,buf);
}

/* remove characters of buf[1] not in buf[0]. */
void text_select(char *p)
{
  int i, j, n;
  _text_cut(p,"in");
  n=strlen(t_buf[1]);
  for(i=j=0;i<n;i++) {
    if(strchr(t_buf[0],t_buf[1][i])!=NULL
       && maskbuf[i]!='0') p[j++]=t_buf[1][i];
  }
  p[j]=0;
}

/* tag: bit 0 is mask. */
struct {
  char *name;
  int tag;
  void (*routine) (char *p);
} text_proc[]={
  {"appear",    1,  text_occur},
  {"common",    1,  text_common},
  {"compare",   0,  text_compare},
  {"copy",      1,  text_copy},
  {"count",     1,  text_count},
  {"delete",    1,  text_remove},
  {"diff",      1,  text_diff},
  {"differ",    1,  text_diff},
  {"drop",      1,  text_remove},
  {"expand",    0,  text_expand},
  {"extract",   1,  text_select},
  {"insert",    1,  text_insert},
  {"interact",  1,  text_interact},
  {"mark",      0,  text_mark},
  {"max",       1,  text_max},
  {"min",       1,  text_min},
  {"occur",     1,  text_occur},
  {"occurrence",1,  text_occur},
  {"pick",      1,  text_select},
  {"pickup",    1,  text_select},
  {"remove",    1,  text_remove},
  {"reorder",   0,  text_reorder},
  {"repeat",    0,  text_repeat},
  {"reverse",   0,  text_reverse},
  {"select",    1,  text_select}
};
#define TEXT_PROC_NO (sizeof(text_proc)/sizeof(text_proc[0]))

int textab_verify(void)
{
  return verify_order(text_proc,TEXT_PROC_NO,sizeof(text_proc[0]));
}

/* main entry point for text routines */
void text(char *p)
{
  int i,j,n;
  char *p1, *p2;
  char c,cc;
  char buf[MAX_LINELEN+1];
  p1=find_word_start(p); p2=find_word_end(p1);
  if(p2<=p1 || *p2==0) error("syntax_error");
  *p2=0;
  i=search_list(text_proc,TEXT_PROC_NO,sizeof(text_proc[0]),p1);
  if(i<0) error("syntax_error");
  snprintf(buf,sizeof(buf),"%s",find_word_start(p2+1));
  if((text_proc[i].tag&1)!=0 && (p1=wordchr(buf,"mask"))!=NULL) {
    *p1=0; strip_trailing_spaces(buf);
    p2=find_word_start(p1+strlen("mask"));
    strip_trailing_spaces(p2);
    snprintf(maskbuf,sizeof(maskbuf),"%s",p2);
    substitute(maskbuf);
    n=strlen(maskbuf); if(n==0) goto zeromask;
    c=maskbuf[n-1]; cc=0;
    if(c=='+') cc='1';
    if(c=='-') cc='0';
    if(cc!=0) memset(maskbuf+n-1,cc,sizeof(maskbuf)-n);
    else for(j=n;j<MAX_LINELEN;j++) maskbuf[j]=maskbuf[j%n];
    maskbuf[sizeof(maskbuf)-1]=0;
  }
  else zeromask: memset(maskbuf,0,sizeof(maskbuf));
  text_proc[i].routine(buf);
  buf[MAX_LINELEN]=0;ovlstrcpy(p,buf);
}
