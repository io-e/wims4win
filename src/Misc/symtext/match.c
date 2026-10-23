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

#include "symtext.h"
/* Block parsing routines
 * Return value:
 * 0 if no match.
 * 1 if match.
 * -1 if error.
 */

struct npool {
    listtype nextblock,last;
} npool[MAX_POOLS];
int nextnpool, currnpool;

void outval(char *tit, char *val)
{
    char *p, l, r;
    val=find_word_start(val);
    strip_trailing_spaces(val);
    if(*val==0) {
      snprintf(outptr,OUTSIZE-(outptr-outbuf)-1,"%s=_EMPTY_ ",tit);
      goto outend;
    }
    if(*find_word_end(val)==0) {
      snprintf(outptr,OUTSIZE-(outptr-outbuf)-1,"%s=%s ",tit,val);
    }
    else {
      l='('; r=')';
      if(strchr(val,l)!=NULL || strchr(val,r)!=NULL) {
          l='['; r=']';
          if(strchr(val,l)!=NULL || strchr(val,r)!=NULL) {
             l='{'; r='}';
             if(strchr(val,l)!=NULL || strchr(val,r)!=NULL) {
                l='"'; r='"';
                if(strchr(val,l)!=NULL || strchr(val,r)!=NULL) {
                  l=r=' ';
                  for(p=val;*p;p++) if(*p==' ') *p='_';
                }
            }
          }
      }
      snprintf(outptr,OUTSIZE-(outptr-outbuf)-1,"%s=%c%s%c ",tit,l,val,r);
    }
    outend: outptr+=strlen(outptr);
}

void cleartag(struct poolstruct *pl)
{
    int i, len, level;
    listtype *tag;
    len=pl->len; tag=pl->tag; level=pl->ind2;
    if(level==0) {
      memset(tag,0,pl->len*sizeof(listtype));
    }
    else for(i=0; i<len; i++) {
      if(tag[i]>=level) tag[i]=0;
    }
}

struct poolstruct *getpool(struct block *blk)
{
    struct poolstruct *pl;

    if(nextpool>=MAX_POOLS) sym_error("pool_overflow");
    pl=poolbuf+nextpool;
    pl->lastpool=blk->pool;
    blk->pool=nextpool; nextpool++;
    pl->block=blk-blockbuf;
    pl->string=NULL;
    pl->ind1=pl->ind2=pl->dirty=pl->len=0;
    return pl;
}

void putpool(struct poolstruct *pl)
{
    nextpool--;
    if(nextpool!=pl-poolbuf) sym_error("internal_error pool leaking");
    blockbuf[pl->block].pool=pl->lastpool;
}

int mt_this(struct block *blk, char *start, int level)
{
    int r;
    if(level > MAX_LEVELS) sym_error("level_overflow %.20s",start);
    start=find_word_start(start);
    if(debug>=2) fprintf(stderr,"lvl=%d. Checking against block %d for %.10s.\n",level,(int)(blk-blockbuf),start);
    r = blk->fn(blk,start,level);
    if(debug) fprintf(stderr,"lvl=%d. Tested %d block %d for %.10s.\n",level,r,(int)(blk-blockbuf),start);
    return r;
}

int mt_next(struct block *blk, char *start, int level)
{
    int r,cp,next;
    next=blk->nextblock;
    cp=currnpool;
    if(next==-2) {
      do {
          next=npool[currnpool].nextblock; currnpool=npool[currnpool].last;
      }
      while (next==-2 && currnpool>0);
      if(next==-2) sym_error("internal_error npool disorder");
      if(debug>=3) fprintf(stderr,"Calling macro %d: next=%d.\n",
                       (int)(blk-blockbuf),next);
    }
    if(next<0) {
      if(*start) return 0; else return 1;
    }
    r=mt_this(blockbuf+next,start,level);
    currnpool=cp;
    return r;
}

int mt_string(struct block *blk, char *start, int level)
{
    char *p;
    int r;
    if(blk->len>0) {
      if(memcmp(blk->string,start,blk->len)!=0) return 0;
      p=start+blk->len; if(*p && !myisspace(*p)) return 0;
    }
    else p=start;
    r = mt_next(blk,p,level+1);
    if(debug) fprintf(stderr,"Strcmp %d %.20s. <-> %.20s.\n",r, blk->string,start);
    return r;
}

int mt_dic(struct block *blk, char *start, int level)
{
    int i,t;
    struct entry *et;
    char *p;

    i=blk->lind1;
    t=search_dic(entry+dic[i].start,dic[i].len,sizeof(entry[0]),start);
    if(t>=0) {
      et=entry+(dic[i].start+t);
      if(itemchr((char*)et->replace,blk->string)==NULL) return 0;
      p=start+et->olen;
      return mt_next(blk,p,level+1);
    }
    switch(dic[i].unknown_type) {
      case unk_leave: {
          if(memcmp(start,blk->string,blk->len)==0 ||
             (start[blk->len]!=0 && !myisspace(start[blk->len])))
            return 0;
          return mt_next(blk,find_word_start(start+blk->len),level+1);
      }
      case unk_replace: {
          if(strcmp(dic[i].unknown,blk->string)!=0) return 0;
          return mt_next(blk,start,level+1);
      }
      case unk_delete: return 0;
    }
    return 0;
}

int _permpick1(struct block *blk, char *start, int level)
{
    int i, j, k, r, re;
    struct poolstruct *pl;

    level++;
    for(i=blk->pool+1; i<nextpool;i++) poolbuf[i].dirty++;
    pl=poolbuf+blk->pool;
    i=blk->lists[pl->ind1][pl->ind2];
    pl->ind2++; cleartag(pl);
ppstart:
    if(i>=blk->len) {            /* optional match */
      i-=blk->len;
      r=mt_next(blk,start,level);
      if(r) goto end;
    }
    if(i>=0) {
      r=mt_this(blockbuf+blk->sublock+i,start,level);
      goto end;
    }
    r=0;
    switch(i) {
      case -1: {      /* any */
          any:
          for(k=blk->lstart;k<blk->len;k++) {
            pl->tag[k]=pl->ind2;
            r=mt_this(blockbuf+blk->sublock+k,start,level);
            cleartag(pl);
            if(r) break;
          }
          goto end;
      }
      case -2: {      /* any unused */
          unused:
          for(k=blk->lstart;k<blk->len;k++) {
            if(pl->tag[k]>0) continue;
            pl->tag[k]=pl->ind2;
            r=mt_this(blockbuf+blk->sublock+k,start,level);
            cleartag(pl);
            if(r) break;
          }
          goto end;
      }
      case -3: {      /* any unused bigger */
          unused_bigger:
          for(k=blk->len-1;k>=blk->lstart && pl->tag[k]==0;k--);
          for(k++;k<blk->len;k++) {
            pl->tag[k]=pl->ind2;
            r=mt_this(blockbuf+blk->sublock+k,start,level);
            cleartag(pl);
            if(r) break;
          }
          goto end;
      }
      case -4: {      /* any unused smaller; not used */
          unused_smaller:
          for(j=0; j<blk->len && pl->tag[j]==0;j++);
          for(k=blk->lstart;k<j;k++) {
            pl->tag[k]=pl->ind2;
            r=mt_this(blockbuf+blk->sublock+k,start,level);
            cleartag(pl);
            if(r) break;
          }
          goto end;
      }
      case -5: {      /* repeat */
          re=pl->ind2-1;
          if(pl->ind2<2 || pl->string >= start) goto end;
          pl->string=start;
          r=mt_next(blk,start,level);
          if(r) goto end;
          pl->ind2=re;
          i=blk->lists[pl->ind1][pl->ind2 - 1];
          goto ppstart;
      }
      case -6: {
          r=mt_next(blk,start,level);
          if(r) goto end;
          goto any;
      }
      case -7: {
          r=mt_next(blk,start,level);
          if(r) goto end;
          goto unused;
      }
      case -8: {
          r=mt_next(blk,start,level);
          if(r) goto end;
          goto unused_bigger;
      }
      case -9: {
          r=mt_next(blk,start,level);
          if(r) goto end;
          goto unused_smaller;
      }
      case -10: {      /* unused. */
          goto end;
      }
      case -11: {      /* unused. */
          goto end;
      }
      case -12: {      /* insertion */
           if(pl->tag[0]==0) {
            pl->tag[0]=pl->ind2;
            r=mt_this(blockbuf+blk->sublock,start,level);
            cleartag(pl);
            if(r) goto end;
          }
          r=_permpick1(blk,start,level+1);
          goto end;
      }
      case -13: {      /* insertion end */
          if(pl->tag[0]) r=mt_next(blk,start,level+1);
          else r=0;
          goto end;
      }
    }
end:
    if(r==0) pl->ind2--;
    for(i=blk->pool+1; i<nextpool;i++) if(poolbuf[i].dirty) poolbuf[i].dirty--;
    return r;
}

int mt_permpick(struct block *blk, char *start, int level)
{
    int r, n, newpool;
    struct poolstruct *pl;

    newpool=n=0;
    pl=poolbuf+blk->pool;
    if(pl==poolbuf || pl->dirty>0) {
      pl=getpool(blk); newpool=1;
      n=pl->len=blk->len;
      if(nexttag + n >= MAX_BLOCKS) sym_error("tag_overflow");
      pl->tag=tagbuf+nexttag; nexttag+=n;
    }
    if(pl->ind1==0 && pl->ind2==0) {
      for(r=0;pl->ind1<blk->lcnt;pl->ind1++,pl->ind2=0){
          r=_permpick1(blk,start,level);
          if(r) break;
      }
      pl->ind1=pl->ind2=0;
      if(newpool) {
          putpool(pl); nexttag-=n;
      }
      return r;
    }
    if(pl->ind2>=blk->listlen[pl->ind1]) {
      return mt_next(blk,start,level);
    }
    return _permpick1(blk,start,level);
}

int mt_neg(struct block *blk, char *start, int level)
{
    int r, newpool;
    struct poolstruct *pl;

    newpool=0;
    pl=poolbuf+blk->pool;
    if(pl==poolbuf || pl->dirty>0) {
      pl=getpool(blk); newpool=1;
    }
    if(pl->ind2) return 1;
    pl->ind2=1;
    r=mt_this(blockbuf+blk->sublock,start,level+1);
    pl->ind2=0;      if(newpool) putpool(pl);
    if(r==0) return mt_next(blk,start,level+1);
    if(r==1) return 0;
    else return r;
}

int mt_nomatch(struct block *blk, char *start, int level)
{
    return 0;
}

void getnpool(int b)
{
    if(nextnpool>=MAX_POOLS) sym_error("npool_overflow");
    npool[nextnpool].nextblock=b;
    npool[nextnpool].last=currnpool;
    currnpool=nextnpool;
    nextnpool++;
}

void putnpool(void)
{
    if(nextnpool<=0) sym_error("npool_underflow");
    nextnpool--;
}

int mt_m(struct block *blk, char *start, int level)
{
    int i,r,pl,b,e,cp;
    char *bkstart;

    if(blk->string>=start) return 0;
    bkstart=blk->string; blk->string=start;
    cp=currnpool;
    getnpool(blk->nextblock);
    b=blk->sublock; pl=blockbuf[b].mpool; blockbuf[b].mpool=nextpool+1;
    e=blockbuf[b].mend;
    if(pl>0) for(i=pl-1;i<nextpool;i++) {
      if(poolbuf[i].block>=b && poolbuf[i].block<e) poolbuf[i].dirty++;
    }
    if(debug>=3) fprintf(stderr, "Entering macro %d npool=%d, next=%d.\n",
                   b, currnpool, npool[currnpool].nextblock);
    r = mt_this(blockbuf+b,start,level+1);
    if(pl>0) for(i=pl-1;i<nextpool;i++) {
      if(poolbuf[i].block>=b && poolbuf[i].block<e && poolbuf[i].dirty>0) poolbuf[i].dirty--;
    }
    blockbuf[b].mpool=pl;
    putnpool(); currnpool=cp; blk->string=bkstart;
    if(debug>=3) fprintf(stderr, "macro %d returns %d, npool=%d, next=%d.\n",
                   b, r, nextnpool, npool[nextnpool].nextblock);
    return r;
}

int mt_out(struct block *blk, char *start, int level)
{
    int r, l, newpool;
    char buf[MAX_LINELEN+1];
    struct poolstruct *pl;

    newpool=0;
    pl=poolbuf+blk->pool;
    if(pl==poolbuf || pl->dirty>0) {
      pl=getpool(blk); newpool=1;
    }
    if(pl->ind2) {
      pl->string=start;
      return mt_next(blk,start,level+1);
    }
    pl->ind2++;
    r=mt_this(blockbuf+blk->sublock,start,level+1);
    pl->ind2=0;      if(newpool) putpool(pl);
    if(r) {
      l=pl->string - start;
      if(l>=0 && l<MAX_LINELEN) {
          memmove(buf,start,l); buf[l]=0;
          outval(blk->string, buf);
      }
    }
    return r;
}

int mt_w(struct block *blk, char *start, int level)
{
    char *p1, *p2;
    char buf[MAX_NAMELEN+1];
    int l;
    p1=find_word_start(start); p2=find_word_end(p1); l=p2-p1;
    if(*p2) p2++;
    if(l>=MAX_NAMELEN) return 0;
    memmove(buf,p1,l); buf[l]=0;
    if(wordchr(wbuf,buf)==NULL) return 0;
    return mt_next(blk,p2,level+1);
}

int mt_wild(struct block *blk, char *start, int level)
{
    char *p;
    int i,l,r;

    l=blk->lstart;
    for(i=0,p=find_word_start(start);i<l;i++,p=find_word_start(find_word_end(p))) {
      if(*p==0) return 0;
    }
    l=blk->len;
    if(l==0) return mt_next(blk,p,level+1);
    for(i=0; i<=l && *p; i++, p=find_word_start(find_word_end(p))) {
      r=mt_next(blk,p,level+1);
      if(r) return r;
    }
    if(i<=l) return mt_next(blk,p,level+1);
    else return 0;
}

int match(char *p)
{
    int i;
    outptr=outbuf; *outptr=0; nextpool=nextnpool=1; nexttag=0;
    currnpool=0;
    for(i=0;i<nextblock;i++) blockbuf[i].pool=blockbuf[i].mpool=0;
    return mt_this(blockbuf,find_word_start(p),0);
}
