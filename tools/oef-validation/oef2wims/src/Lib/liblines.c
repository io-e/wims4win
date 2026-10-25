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
/* line input / output routines */

#include <stdarg.h>
#include "libwims.h"

/* copy of possibly overlapping strings, to replace strcpy which is not guaranteed in this case
   and indeed produces errors particularly on 64 bits computers */

void ovlstrcpy(char *dest, char *src)
{
  memmove(dest, src, strlen(src)+1);
}

#define int_buf_size 40

/* this is rapid. Output string will be erased at the next call. */
char *int2str(int i)
{
  int neg,t;
  static char int_buf[int_buf_size];
  int_buf[int_buf_size-1]=0;
  neg=0; if(i<0) {neg=1;i=-i;}
  for(t=int_buf_size-2;t>=0;) {
    int_buf[t--]=i%10+'0'; i/=10;
    if(i==0) break;
  }
  if(t<=0) {int_buf[0]=0; return int_buf;} /* should never occur. */
  if(neg) int_buf[t--]='-';
  t++; return int_buf+t;
}

void *xmalloc(size_t n)
{
    void *p;
    p=malloc(n);
    if(p==NULL) {
      fprintf(stderr,"Malloc failure."); exit(1);
    }
    return p;
}

int msleep(int ms)
{
    struct timespec req, rem;
    if(ms<=0) return 0;
    req.tv_sec=ms/1000; req.tv_nsec=(ms%1000)*1000*1000;
    return nanosleep(&req,&rem);
}

  /* dos/mac to unix/linux translation */
void _tolinux(char *p)
{
    char *pp,*p1;
    pp=strchr(p,13); if(pp==NULL) return;
    for(p1=pp; *pp; pp++) {
      if(*pp==13) {
          if(*(pp+1)=='\n' || (pp>p && *(pp-1)=='\n') ||
             (*(pp+1)=='\\' && *(pp+2)=='\n')) continue;
          else *pp='\n';
      }
      *p1++=*pp;
    }
    *p1=0;
}


/* optimized and secure strcpy */

/* copies src to dest, at most lim bytes. Error if more than
   MAX_LINELEN chars would be copied, including final \0. */

void mystrncpy(char *dest, const char *src, size_t lim)
{
  if (lim){
    size_t i = strlen(src);
    if (i >= lim) i = lim-1;
    if (i >= MAX_LINELEN) error("cmd_output_too_long");
    memmove(dest,src,i); dest[i]=0;
  }
}

/* find matching parenthesis.
 * The entrance point should be after the opening
 * parenthesis.
 * Returns NULL if unmatched.
 */
char *find_matching(char *p, char c)
{
  char *pp;
  int parenth, brak, brace;
  if(c=='|') {
    for(pp=p;*pp!=0;pp++) {
      switch(*pp) {
        case '|': return pp;
        case '(': {
          pp=find_matching(pp+1,')');
          if(pp==NULL) return NULL;
          break;
        }
        case '[': {
          pp=find_matching(pp+1,']');
          if(pp==NULL) return NULL;
          break;
        }
        case '{': {
          pp=find_matching(pp+1,'}');
          if(pp==NULL) return NULL;
          break;
        }
        case ')':
        case ']':
        case '}': return NULL;

        default: break;
      }
    }
    return NULL;
  }
  parenth=brak=brace=0;
  for(pp=p; *pp!=0; pp++) {
    switch(*pp) {
      case '[': brak++; break;
      case ']': brak--; break;
      case '(': parenth++; break;
      case ')': parenth--; break;
      case '{': brace++; break;
      case '}': brace--; break;
      default: continue;
    }
    if(parenth<0 || brak<0 || brace<0) {
      if(*pp!=c || parenth>0 || brak>0 || brace>0) return NULL;
      else break;
    }
  }
  if(*pp!=c) return NULL;
  return pp;
}

/* Points to the end of the word */
char *find_word_end(char *p)
{
  while(!myisspace(*p) && *p!=0) p++;
  return p;
}

/* Strips leading spaces */
char *find_word_start(char *p)
{
  while(myisspace(*p)) p++;
  return p;
}

/* find a character in a string, but skipping parentheses. */
char *strparchr(char *p, char c)
{
  char *pp;

  for(pp=p;*pp && *pp!=c && pp-p<MAX_LINELEN; pp++) {
    switch(*pp) {
      case '(': pp=find_matching(pp+1,')'); break;
      case '[': pp=find_matching(pp+1,']'); break;
      case '{': pp=find_matching(pp+1,'}'); break;
    }
    if(pp==NULL) return NULL;
  }
  if(*pp==c) return pp; else return NULL;
}

/* search for string, skipping parentheses */
char *strparstr(char *p, char *fnd)
{
  char *pp, c=*fnd;
  size_t n = strlen(fnd);
  /*if(*fnd==0) return p+strlen(p);*/
  for(pp=p; *pp; pp++) {
    if(*pp==c && (n==1 || strncmp(pp,fnd,n)==0)) return pp;
    switch(*pp) {
      case '(': pp=find_matching(pp+1,')'); break;
      case '[': pp=find_matching(pp+1,']'); break;
      case '{': pp=find_matching(pp+1,'}'); break;
    }
    if (!pp) { pp = strstr(p,fnd); if (!pp) pp = p+strlen(p); break; }
  }
  return pp;
}

/* Points to the end of an item */
char *find_item_end(char *p)
{
  return strparstr(p,",");
}

/* Points to the end of an item */
char *find_line_end(char *p)
{
  char *pp=strstr(p,"\n");
  if(pp==NULL) pp=p+strlen(p);
  return pp;
}

char *charchr(char *p,char *w)
{
  return strchr(p,w[0]);
}

/* Find first occurrence of word */
char *wordchr(char *p, char *w)
{
  char *r; int n;

  if(*w==0) return NULL;
  n=strlen(w);
  for(r=strstr(p,w);r!=NULL &&
      ( (r>p && !myisspace(*(r-1))) || (!myisspace(*(r+n)) && *(r+n)!=0) );
    r=strstr(r+1,w)){};
  return r;
}

/* Find first occurrence of item */
char *itemchr(char *p, char *w)
{
  char *r, *r1, *r2; int n;

  if(*w==0) return NULL;
  n=strlen(w);
  for(r=strstr(p,w);r!=NULL; r=strstr(r+1,w)) {
    r1=r-1;while(r1>=p && myisspace(*r1)) r1--;
    r2=find_word_start(r+n);
    if((r1<p || *r1==',') && (*r2==0 || *r2==',')) return r;
  }
  return r;
}

/* Find first occurrence of line */
char *linechr(char *p, char *w)
{
  char *r;
  int n;

  if(*w==0) return NULL;
  n=strlen(w);
  for(r=strstr(p,w);r!=NULL; r=strstr(r+1,w)) {
    if((r<=p || *(r-1)=='\n') && (*(r+n)==0 || *(r+n)=='\n'))
      break;
  }
  return r;
}

/* find a variable in a string (math expression).
 * Returns the pointer or NULL. */
char *varchr(char *p, char *v)
{
  char *pp; int n;
  if(*v==0) return NULL;
  n=strlen(v);
  for(pp=strstr(p,v); pp!=NULL; pp=strstr(pp+1,v)) {
    if((pp==p || (!myisalnum(*(pp-1)) && *(pp-1)!='_')) &&
       ((!myisalnum(*(pp+n)) && *(pp+n)!='_' && *(pp+n)!='\'')
        || *(pp+n)==0)) break;
  }
  return pp;
}

int _cutit_(char *p, char *list[], int max, char *end_finder(char *pt), int tag)
{
  int i;
  char *pp, *pe, *p0;
  if(tag&1) pp=find_word_start(p); else pp=p; /* strip head space */
  for(i=0;i<max && *pp;i++) {
    pe=end_finder(pp);
    if((tag&2) && myisspace(pe[-1])) { /* strip trailing space */
      for(p0=pe; p0>pp && myisspace(p0[-1]); p0--);
      if(p0<pe) *p0=0;
    }
    if(*pe) *pe++=0;
    list[i]=pp;
    if(tag&1) pp=find_word_start(pe); else pp=pe;
  }
  return i;
}

int cutitems(char *p, char *list[], int max)
{
  return _cutit_(p,list,max,find_item_end,3);
}

int cutwords(char *p, char *list[], int max)
{
  return _cutit_(find_word_start(p),list,max,find_word_end,1);
}

int cutlines(char *p, char *list[], int max)
{
  return _cutit_(p,list,max,find_line_end,0);
}

int cutchars(char *p, char *list[], int max)
{
  int i; char *pp;
  for(i=0,pp=p;*pp && i<max;list[i++]=pp++);
  return i;
}

/* strip trailing spaces; return string end. */
char *strip_trailing_spaces(char *p)
{
  char *pp;
  if(*p==0) return p;
  for(pp=p+strlen(p)-1; pp>=p && myisspace(*pp); pp--);
  if(pp[1]) pp[1]=0;
  return pp;
}

/*  strip trailing spaces; return string end. */
char *strip_trailing_spaces2(char *p)
{
  char *pp;
  if(*p==0) return p;
  for(pp=p+strlen(p)-1; pp>=p && isspace(*pp); *(pp--)=0);
  return pp;
}

/* Routines for quick search of item in a list. */

/* Verify whether a list is well-ordered. For debugging uses.
 * Returns 0 if order is OK, -1 otherwise. */
int verify_order(void *list, int items, size_t item_size){
  int i; char *old, **p;
  p=list; old=*p;
  for(i=item_size;i<items*item_size;i+=item_size) {
    p=list+i;
    if(strcmp(*p,old)<0) {
      fprintf(stderr,"\nTable disorder: %s > %s\n",old,*p);
      exit(1);
    }
    old=*p;
  }
  return 0;
}

/* searches a list. Returns index if found, (-1-index of insertion) if nomatch.
 * Uses binary search, list must be sorted. */

int search_list(void *list, int items, size_t item_size, const char *str)
{
  int i = 0;
  while (items > 0){
    int m = items / 2, j = i + m;
    int k = strcmp(*(char **)(list + j * item_size), str);
    if (k == 0) return j;
    if (k > 0) items = m; else {i = j + 1; items -= (m + 1);}
  }
  return ~i;
}

/* Returns number of lines in string p */
unsigned int linenum(char *p)
{
  int i; char *pp;

  /* Ending blank line will be thus removed. */
  i=strlen(p); if(i>1 && *(p+i-1)=='\n') *(p+i-1)=0;
  if(*p=='\n') i=1; else i=0;
  for(pp=p; pp!=NULL && *pp!=0; pp=strchr(pp+1,'\n'), i++);
  return i;
}

/* Returns number of items in the list p, comma separated */
unsigned int itemnum(char *p)
{
  int i; char *pp;

  if(*p==0) return 0;
  for(i=0,pp=p; pp==p || *(pp-1)!=0; pp=find_item_end(pp)+1, i++);
  return i;
}

/* Returns number of words in string p */
unsigned int wordnum(char *p)
{
  int i; char *pp;

  for(i=0, pp=find_word_start(p); *pp!=0; i++) {
    while(!myisspace(*pp) && *pp!=0) pp++;
    while(myisspace(*pp)) pp++;
  }
  return i;
}

/* This is just to suppress an annoying compiler warning message. */
unsigned int charnum(char *p)
{
  return strlen(p);
}

/* find n-th line in string p */
char *fnd_line(char *p, int n, char bf[])
{
  char *pp;
  int i;

    /*
for(i=1,pp=p; pp-1!=NULL && *pp!=0 && i<n; pp=strchr(pp,'\n')+1, i++);
    */
  for(i=1,pp=p; i<n; i++) {
    pp=strchr(pp,'\n');
    if (!pp++) {
      fnd_position=NULL; fnd_nextpos=""; *bf=0; return bf;
    }
  }
  fnd_position=pp;
  for(i=0; *(pp+i)!=0 && *(pp+i)!='\n'; i++) *(bf+i)=*(pp+i);
  *(bf+i)=0;
  if(pp[i]=='\n') i++;
  fnd_nextpos=pp+i;
  return bf;
}

/* find n-th item in list p, comma separated */
char *fnd_item(char *p, int n, char bf[])
{
  char *pp, *pe;
  int i;

  for(i=1,pp=p; i<n && (pp==p || *(pp-1)!=0); pp=find_item_end(pp)+1, i++);
  fnd_position=pp; if(pp>p && *(pp-1)==0) {
    fnd_position=NULL; *bf=0; return bf;
  }
  pp=find_word_start(pp); pe=find_item_end(pp);
  if(*pe) fnd_nextpos=pe+1; else fnd_nextpos=pe;
  while(pe>pp && myisspace(*(pe-1))) pe--;
  memmove(bf,pp,pe-pp); bf[pe-pp]=0;
  return bf;
}

/* find n-th word in string p */
char *fnd_word(char *p, int n, char bf[])
{
  char *pp;
  int i;

  for(i=1, pp=find_word_start(p); *pp!=0 && i<n ; i++) {
    while(!myisspace(*pp) && *pp!=0) pp++;
    pp=find_word_start(pp);
  }
  if(*pp) fnd_position=pp; else fnd_position=NULL;
  for(i=0; *(pp+i)!=0 && !myisspace(*(pp+i)); i++) *(bf+i)=*(pp+i);
  fnd_nextpos=find_word_start(pp+i);
  *(bf+i)=0;
  return bf;
}

/* find n-th char in string p */
char *fnd_char(char *p, int n, char bf[])
{
  int t;

  t=strlen(p);
  if(n>t || n<1) {*bf=0;fnd_position=NULL; fnd_nextpos="";}
  else {
    *bf=*(p+n-1); *(bf+1)=0;
    fnd_position=p+n-1;fnd_nextpos=p+n;
  }
  return bf;
}

/* Returns number of lines if semicolons changed to new lines */
int rows2lines(char *p)
{
  char *pp, *p2;
  int t;
  if(strchr(p,'\n')!=NULL) return 0;
  for(t=0, pp=p; *pp; pp++) {
    if(*pp=='(') {p2=find_matching(pp+1,')'); if(p2!=NULL) pp=p2; continue;}
    if(*pp=='[') {p2=find_matching(pp+1,']'); if(p2!=NULL) pp=p2; continue;}
    if(*pp=='{') {p2=find_matching(pp+1,'}'); if(p2!=NULL) pp=p2; continue;}
    if(*pp==';') {*pp='\n'; t++; continue;}
    if(*pp=='&' && myisalpha(*(pp+1))) {
      for(p2=pp+1; myisalpha(*p2) && p2-pp<14; p2++);
      pp=p2; continue;
    }
    if(*pp=='&' && *(pp+1)=='#') {
      for(p2=pp+2; myisdigit(*p2) && p2-pp<6; p2++);
      pp=p2; continue;
    }
  }
  return t;
}

void lines2rows(char *p)
{
  char *pp;
  strip_trailing_spaces(p);
  for(pp=strchr(find_word_start(p),'\n'); pp!=NULL; pp=strchr(pp+1,'\n'))
    *pp=';';
}

unsigned int rownum(char *p)
{
  char buf[MAX_LINELEN+1];
  snprintf(buf,sizeof(buf),"%s",p);
  rows2lines(buf);
  return linenum(buf);
}

/* find n-th row in a matrix p */
char *fnd_row(char *p, int n, char bf[])
{
  rows2lines(p); return fnd_line(p,n,bf);
}

/* strstr but may have embedded zeros.
 * Returns memory end if not found.
 * Supposes memory ends with 0.
 */
char *memstr(char *s1, char *s2, int len)
{
  char *p, *pp;
  pp=s1;
  for(p=s1; p<s1+len; p=pp) {
    pp=strstr(p,s2); if(pp!=NULL) break;
    pp=p+strlen(p);
    while(pp<s1+len && *pp==0) pp++;
  }
  return pp;
}

/* Check whether parentheses are balanced in a given string.
 * Returns 0 if OK.
 */
/* style=0: simple check. style<>0: strong check. */
static int _check_parentheses(char *p, char *q, int style)
{
  int i,j = q-p, k;
  if(style!=0) {
    char *pp, *pe, c;
    for(pp=p;pp<p+j;pp++) {
      switch (*pp) {
        case ')':
        case ']':
        case '}': return -1;
        case '(': c=')'; goto find;
        case '[': c=']'; goto find;
        case '{': c='}';
        find: {
          pe=find_matching(pp+1,c);
          if(pe==NULL||pe>q) return 1;
	      if((k=_check_parentheses(pp+1,pe-1,1))!=0) return k;
          else pp=pe;
        }
        default: break;
      }
    }
    return 0;
  }
  for(i=k=0;i<j && k>=0;i++) {
    if(*(p+i)=='(') k++;
    if(*(p+i)==')') k--;
  }
  return k;
}

int check_parentheses(char *p, int style)
{
  int j=strlen(p);
  if(j>=MAX_LINELEN) return 65535;
  return _check_parentheses(p, p+j, style);
}

/* Strip enclosing pairs of parentheses */
void strip_enclosing_par(char *p)
{
  char *p1;
  partest: strip_trailing_spaces(p);
  if(*p=='(') {
    p1=find_matching(p+1,')');
    if(p1 && *(p1+1)==0) {
      *p1=0; ovlstrcpy(p,find_word_start(p+1));
      goto partest;
    }
  }
  if(*p=='[') {
    p1=find_matching(p+1,']');
    if(p1 && *(p1+1)==0) {
      *p1=0; ovlstrcpy(p,find_word_start(p+1));
      goto partest;
    }
  }
  if(*p=='{') {
    p1=find_matching(p+1,'}');
    if(p1 && *(p1+1)==0) {
      *p1=0; ovlstrcpy(p,find_word_start(p+1));
      goto partest;
    }
  }
}

/* change all spaces into ' ', and collapse multiple occurences */
void singlespace(char *p)
{
  char *pp, *pt, *p2;
  for(pp=pt=p;*pp;pp++) {
    if(!myisspace(*pp)) {*pt++=*pp; continue; }
    *pt++=' ';
    for(p2=pp+1;myisspace(*p2);p2++){};
    pp=--p2;
  }
  *pt=0;
}

/* collapses all space characters in string. */
void nospace(char *p)
{
  char *pp, *pt;
  for(pp=pt=p;*pp;pp++) if(!myismspace(*pp)) *pt++=*pp;
  *pt=0;
}

void _spaces2_(char *p, char c)
{
  char *pp; int n;
  singlespace(p);
  n=strlen(p); if(*p==' ') {memmove(p,p+1,n);n--;}
  if(n==0) return;
  if(p[n-1]==' ') p[n-1]=0;
  for(pp=strchr(p,' '); pp; pp=strchr(pp,' ')) *pp++=c;
}
/* change words to items */
void words2items(char *p)
{
  _spaces2_(p,',');
}

/* change words to lines */
void words2lines(char *p)
{
  _spaces2_(p,'\n');
}

/* change lines to items */
void lines2items(char *p)
{
  char *pp;
  for(pp=strchr(p,'\n'); pp; pp=strchr(pp,'\n')) *pp++=',';
}

/* change lines to words */
void lines2words(char *p)
{
  char *pp;
  for(pp=strchr(p,'\n'); pp; pp=strchr(pp,'\n')) *pp++=' ';
}

/* change items to words */
void items2words(char *p)
{
  char *pp;
  for(pp=strparstr(p,",");*pp;pp=strparstr(pp+1,",")) *pp=' ';
}

/* change items to lines */
void items2lines(char *p)
{
  char *pp;
  for(pp=strparstr(p,",");*pp;pp=strparstr(pp+1,",")) *pp='\n';
}
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Winvalid-source-encoding"
char *acctab="ÁÈËÍÎ˙˘˚¸·‡‚‰„ÛÚÙˆıÌÏÔÓÒ˝ˇ«…» À⁄Ÿ€‹¡¿¬√ƒ”“‘÷’ÕÃœŒ—›",
     *deatab="ceeeeuuuuaaaaaoooooiiiinyyCEEEEUUUUAAAAAOOOOOIIIINY";

/* fold accented letters to unaccented */
void deaccent(char *p)
{
  char *sp;
  char *v;
  for(sp=p;*sp;sp++) {
    if(*sp<0 && (v=strchr(acctab,*sp))!=NULL)
      *sp=*(deatab+(v-acctab));
  }
}

static char mytolower (char c) {
  char *v;
  if (c < 0 && (v=strchr(acctab,c))!=NULL)
    c = deatab[v-acctab];
  return tolower(c);
}

int mystrcmp (char *p1,char *p2) {
  char *q1=p1, *q2=p2;
  while(*q1 && *q2) {
    int n=mytolower(*q1++)-mytolower(*q2++);
    if(n) return n;
  };
  if (*q2 || *q1) return *q1 ? 1 : -1;
  return strcmp(p1, p2);
}

char *reaccents="'`\"^~";
char *reaccentl="aeiouycnAEIOUYCN";
char *reaccentab="\
·‡‰‚„ÈËÎÍeÌÏÔÓiÛÚˆÙı˙˘¸˚u˝yˇyyccccÁnnnnÒ\
¡¿ƒ¬√…»À EÕÃœŒI”“÷‘’⁄Ÿ‹€U›YYYYCCCC«NNNN—";
#pragma GCC diagnostic pop
/* compose accent using symbol keys */
void reaccent(char *p)
{
  char *sp, *ap, c;
  int i, k;
  if(*p==0) return;
  for(sp=p+1; *sp; sp++) {
    ap=strchr(reaccents,*sp); if(ap==NULL) continue;
    i=ap-reaccents;
    sp--; ap=strchr(reaccentl,*sp); if(ap==NULL) {sp++; continue;}
    k=ap-reaccentl;
    c=reaccentab[k*strlen(reaccents)+i];
    if(c!=*sp) {*sp=c; ovlstrcpy(sp+1,sp+2);}
    else sp++;
  }
}

/* modify a string. Bufferlen must be at least MAX_LINELEN */
void string_modify1(char *start, char *bad_beg, char *bad_end, char *good,...)
{
  char buf[MAX_LINELEN+1];
  int ln, le;
  va_list vp;

  va_start(vp,good);
  vsnprintf(buf,sizeof(buf),good,vp); va_end(vp);
  ln=strlen(buf); le=strlen(bad_end);
  if(ln+le+(bad_beg-start)>=MAX_LINELEN) {
    error("string_too_long"); return;
  }
  if(ln!=bad_end-bad_beg) memmove(bad_beg+ln,bad_end,le+1);
  memmove(bad_beg,buf,ln);
}

/*  modify a string. Bufferlen must be at least MAX_LINELEN */
void string_modify3(char *start, char *bad_beg, char *bad_end, char *good,...)
{
  char buf[MAX_LINELEN+1];
  va_list vp;

  va_start(vp,good);
  vsnprintf(buf,sizeof(buf),good,vp); va_end(vp);
  if(strlen(start)-(bad_end-bad_beg)+strlen(buf)>=MAX_LINELEN)
    return; /* this is an error situation. */
  strcat(buf,bad_end);
  ovlstrcpy(bad_beg,buf);
}

/* returns number of bytes written */
int catfile(FILE *outf, char *fn,...)
{
  char buf[4096];
  va_list vp;
  int l,tot,fd;

  tot=0;
  va_start(vp,fn);
  vsnprintf(buf,sizeof(buf),fn,vp); va_end(vp);
  fd=open(buf,O_RDONLY); if(fd==-1) return 0;
  for(l=read(fd,buf,4096);l>0 && l<=4096; l=read(fd,buf,4096)) {
    fwrite(buf,1,l,outf); tot+=l;
  }
  close(fd);
  return tot;
}

/* returns -1 if error */
long int filelength(char *fn,...)
{
  char buf[MAX_FNAME+1];
  va_list vp;
  struct stat st;
  int l;

  va_start(vp,fn);
  vsnprintf(buf,sizeof(buf),fn,vp); va_end(vp);
  l=stat(buf,&st); if(l) return -1;
  return st.st_size;
}

char *parend(char *p)
{
  char *pp; int t;
  t=0; for(pp=p;*pp;pp++) {
    if(*pp=='(') t++;
    if(*pp==')') {t--; if(t==0) return pp; if(t<0) return NULL;}
  }
  return NULL;
}

char *bufprep(char *p)
{
/*  singlespace(p); strip_trailing_spaces(p); return find_word_start(p); */
  nospace(p); return p;
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Winvalid-source-encoding"
char *htmlsymbs[][2]={
  {"",""},
  {"&agrave;","‡"},
  {"&Agrave;","¿"},
  {"&aacute;","·"},
  {"&Aacute;","¡"},
  {"&acirc;","‚"},
  {"&Acirc;","¬"},
  {"&atilde;","„"},
  {"&Atilde;","√"},
  {"&auml;","‰"},
  {"&Auml;","ƒ"},
  {"&aring;","Â"},
  {"&Aring;","≈"},
  {"&aelig;","Ê"},
  {"&AElig;","∆"},
  {"&egrave;","Ë"},
  {"&Egrave;","»"},
  {"&eacute;","È"},
  {"&Eacute;","…"},
  {"&ecirc;","Í"},
  {"&Ecirc;"," "},
  {"&euml;","Î"},
  {"&Euml;","À"},
  {"&igrave;","Ï"},
  {"&Igrave;","Ã"},
  {"&iacute;","Ì"},
  {"&Iacute;","Õ"},
  {"&icirc;","Ó"},
  {"&Icirc;","Œ"},
  {"&iuml;","Ô"},
  {"&Iuml;","œ"},
  {"&ograve;","Ú"},
  {"&Ograve;","“"},
  {"&oacute;","Û"},
  {"&Oacute;","”"},
  {"&ocirc;","Ù"},
  {"&Ocirc;","‘"},
  {"&otilde;","ı"},
  {"&Otilde;","’"},
  {"&ouml;","ˆ"},
  {"&Ouml;","÷"},
  {"&oslash;","¯"},
  {"&Oslash;","ÿ"},
  {"&ugrave;","˘"},
  {"&Ugrave;","Ÿ"},
  {"&uacute;","˙"},
  {"&Uacute;","⁄"},
  {"&ucirc;","˚"},
  {"&Ucirc;","€"},
  {"&uuml;","¸"},
  {"&Uuml;","‹"},
  {"&ntilde;","Ò"},
  {"&Ntilde;","—"},
  {"&ccedil;","Á"},
  {"&Ccedil;","«"},
  {"&yacute;","˝"},
  {"&Yacute;","›"},
  {"&szlig;","ﬂ"},
  {"&laquo;","´"},
  {"&raquo;","ª"},
  {"&lsaquo;","\'"},
  {"&rsaquo;","\'"},
  {"&lsquo;","\'"},
  {"&rsquo;","\'"},
  {"&ldquo;","\""},
  {"&rdquo;","\""},
  {"&amp;","&"},
  {"&lt;","<"},
  {"&gt;","<"},
  {"&quot;","\""},
  {"&para;","ß"},
  {"&copy;","©"},
  {"&nbsp;"," "},
  {"&euro;","euro(s)"},
  {"&#44;",","},
  {"&#59;",";"},
};
#pragma GCC diagnostic pop

#define thesymbs_no (sizeof(htmlsymbs)/sizeof(htmlsymbs[0]) - 1)

int htmltrans[6*thesymbs_no][256]; /* 6 is the maximum length of a html symbol stripped of & and ; */

void inithtmltrans()
{
  int i, ns, st, nbstates = thesymbs_no;
  for (i = 1; i <= thesymbs_no; i++)
    {
      st = 0;
      unsigned char *p = (unsigned char *)(htmlsymbs[i][0] + 1);
      while (*p != ';')
        {
          ns = htmltrans[st][*p];
          if (ns == 0) ns = (p[1]==';') ? i : -(++nbstates);
          htmltrans[st][*p] = ns;
          st = abs(ns);
          p++;
        }
    }
  /*  printf("%d etats.\n", nbstates); */
}

