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

char cpbuf[MAX_LINELEN+1];
char *cpnext;
int noaddw=0;

char Mbuf[MAX_LINELEN+1];
char *Mnext;
struct {
    char *Mptr;
    int blkptr;
} Mind[MAX_BLOCKS];
int Mcnt;

struct liststruct {
    listtype lcnt;
    listtype *listlen;
    listtype *list;
} clist;

void cp_oneblock(char *p, struct block *blk, int next);

/* debugger: show the detail of blocks */
void showblocks(void)
{
    int i, j, k;
    struct block *blk;
    if(!debug) return;
    for(i=0;i<nextblock;i++) {
      blk=blockbuf+i;
      fprintf(stderr,"Block %2d: next=%2d.	",i,blk->nextblock);
      if(blk->fn==mt_string) {
          fprintf(stderr,"String %s.\n",blk->string);
          continue;
      }
      if(blk->fn==mt_permpick) {
          fprintf(stderr,"Permpick %d items %d lists starting %d\n",
               blk->len, blk->lcnt, blk->sublock);
          for(j=0;j<blk->lcnt;j++) {
            fprintf(stderr,"	list %d: ", j);
            for(k=0;k<blk->listlen[j];k++)
              fprintf(stderr,"%d ",blk->lists[j][k]);
            fprintf(stderr,"\n");
          }
          continue;
      }
      if(blk->fn==mt_m) {
          fprintf(stderr,"Macro starting %d ending %d\n", blk->sublock,
                blockbuf[blk->sublock].mend);
          continue;
      }
      if(blk->fn==mt_neg) {
          fprintf(stderr,"Neg starting %d\n", blk->sublock);
          continue;
      }
      if(blk->fn==mt_dic) {
          fprintf(stderr,"Dic %s.\n", blk->string);
          continue;
      }
      if(blk->fn==mt_w) {
          fprintf(stderr,"Word list.\n");
          continue;
      }
      if(blk->fn==mt_wild) {
          fprintf(stderr,"Wild min=%d, max=%d.\n", blk->lstart, blk->len+blk->lstart);
          continue;
      }
      if(blk->fn==mt_out) {
          fprintf(stderr,"Output for \"%s\", sub=%d.\n", blk->string,blk->sublock);
          continue;
      }
      fprintf(stderr,"Unknown type!\n");
    }

}

/* remove punctuations */
void depunct(char *p,char *str)
{
    char *pp;
    for(pp=p; *pp; pp++) {
      if(strchr(str,*pp)==NULL) continue;
      if(*pp=='.' && pp>p && isdigit(pp[-1]) && isdigit(pp[1])) continue;
      *pp=' ';
    }
}

/* p must have MAX_LINELEN */
void isolate_punct(char *p)
{
    char buf[MAX_LINELEN+1];
    char *p1, *p2;
    for(p1=p, p2=buf; *p1 && p2<buf+MAX_LINELEN; p1++) {
      if(myisalnum(*p1) || (*p1&128) || *p1==' ' || *p1=='_') {
          *p2++=*p1; continue;
      }
      if((*p1=='.' || *p1==',') && p1>p && isdigit(p1[-1]) && isdigit(p1[1])) {
          *p2++=*p1; continue;
      }
      if(p2>buf && !myisspace(p2[-1])) *p2++=' ';
      *p2++=*p1;
      if(p1[1] && !myisspace(p1[1])) *p2++=' ';
    }
    if(p2>=buf+MAX_LINELEN) sym_error("string_too_long");
    *p2=0;
    snprintf(p,MAX_LINELEN+1,"%s",buf);
}

void alphaonly(char *p)
{
    char *pp;
    for(pp=p; *pp; pp++) if(!myisalpha(*pp)) *pp=' ';
}

void alnumonly(char *p)
{
    char *pp;
    for(pp=p; *pp; pp++) if(!myisalnum(*pp)) *pp=' ';
}

/* buffer must have MAX_LINELEN */
void strfold(char *p)
{
    char *pp;
    singlespace(p);
    if(noaddw&2) goto fend;
    if(options&op_nocase) for(pp=p; *pp; pp++) *pp=tolower(*pp);
    if(options&op_reaccent) reaccent(p);
    if(options&op_deaccent) deaccent(p);
    if(options&op_alphaonly) alphaonly(p);
    if(options&op_alnumonly) alnumonly(p);
    isolate_punct(p);
    if(options&op_nopunct) depunct(p,char_punct);
    if(options&op_nomath) depunct(p,char_math);
    if(options&op_noparenth) depunct(p,char_parenth);
    if(options&op_nocs) depunct(p,char_cs);
    if(options&op_noquote) depunct(p,char_quote);
    if(suffixcnt>0) suffix_translate(p);
    _translate(p,transdic);

fend:
    singlespace(p);strip_trailing_spaces(p);
    if(myisspace(*p)) ovlstrcpy(p,find_word_start(p));
}

/* at entry p must point to an atom start! */
char *find_atom_end(char *p)
{
    char *pp;
    if(*p=='[') {
      pp=find_matching(p+1,']');
      if(pp!=NULL) return pp+1;
      else sym_error("unmatched_parentheses %.20s",p);
    }
    return find_word_end(p);
}

#define find_atom_start(x) find_word_start(x)
#define next_atom(x) find_atom_start(find_atom_end(x))

/* p must have MAX_LINELEN */
void macro_trans(char *p)
{
    char *atoms[MAX_BLOCKS], *atom2[MAX_BLOCKS];
    char tbuf[MAX_LINELEN+1], ttbuf[MAX_LINELEN+1], vbuf[8];
    char *pt, *pp, *pe, *p1, *p2, *p3;
    char *pt1, *pt2;
    int i,k,m,n,repcnt,start,min;

    if(dic[macrodic].len<=0) return;
    repcnt=start=0; pt=p;
    recalc:
    repcnt++;
    if(repcnt>=MAX_BLOCKS) sym_error("macro_level_overflow %.20s",p);
    for(i=start, pp=find_atom_start(pt); i<MAX_BLOCKS && *pp; pp=next_atom(pp), i++)
      atoms[i]=pp;
    if(i>=MAX_BLOCKS-1) sym_error("block_overflow %.20s",p);
    atoms[i]=pp+strlen(pp);
    for(k=0;k<i;k++) {
      pp=atoms[k]; switch(*pp) {
          case '[': {
            pe=find_atom_end(pp); pp++;
            if(pe[-1]!=']') break;
            if(myislower(*pp)) {
                for(p1=pp;myisalnum(*p1) || *p1=='_'; p1++);
                if(*p1!=':') break;
                *p1++=0; pe[-1]=0;
                for(m=0,p2=p1;*p2;m++,p2=p3) {
                  p3=find_item_end(p2); if(*p3) *p3++=0;
                  atom2[m]=p2;
                }
                if(m==0) m=1;
                snprintf(tbuf,sizeof(tbuf),"%s",pp);
                _translate(tbuf,macrodic);
                if(tbuf[0]==0) sym_error("bad_macro %.50s",pp);
                for(p1=strchr(tbuf,'@'); p1; p1=strchr(p1,'@')) {
                  for(p2=p1+1;isdigit(*p2) || *p2=='-';p2++);
                  if(p2==p1+1 || p2>p1+6) sym_error("syntax_error %.20s",p1);
                  memmove(vbuf,p1,p2-p1); vbuf[p2-p1]=0;
                  n=atoi(vbuf+1);
                  if(n<=0 || n>m) sym_error("wrong_parmcnt macro %.50s",pp);
                  string_modify(tbuf,p1,p2,atom2[n-1]);
                }
                n=strlen(tbuf); if(n<MAX_LINELEN) {
                  tbuf[n++]=' '; tbuf[n]=0;
                }
                string_modify(p,pp-1,pe,tbuf); pt=pp-1; start=k;
                goto recalc;
            }
            break;
          }
          case '_': {
            pe=find_word_end(pp);
            if(pe-pp>MAX_NAMELEN) sym_error("name_too_long %.20s",pp);
            memmove(tbuf,pp,pe-pp); tbuf[pe-pp]=0;
            _translate(tbuf,macrodic);
            if(tbuf[0]==0) break;
            pt1=pp; pt2=find_atom_end(pt1); min=k;
            for(p1=strchr(tbuf,'@'); p1; p1=strchr(p1,'@')) {
                for(p2=p1+1;isdigit(*p2) || *p2=='-';p2++);
                if(p2==p1+1 || p2>p1+6) sym_error("syntax_error %.20s",p1);
                memmove(vbuf,p1,p2-p1); vbuf[p2-p1]=0;
                n=atoi(vbuf+1);
                if(n<-4 || n==0 || n>4) sym_error("bad_macro %.20s",atoms[k]);
                n+=k;
                if(n<0 || n>=i) sym_error("bad_macro_position %.20s",atoms[k]);
                p3=find_atom_end(atoms[n]);
                if(p3>pt2) pt2=p3;
                if(atoms[n]<pt1) {min=n;pt1=atoms[n];}
                memmove(ttbuf,atoms[n],p3-atoms[n]); ttbuf[p3-atoms[n]]=' ';
                ttbuf[p3-atoms[n]+1]=0;
                string_modify(tbuf,p1,p2,ttbuf);
            }
            string_modify(p,pt1,pt2,tbuf); pt=pt1; start=min;
            goto recalc;
          }
          default: break;
      }
    }
}

char *add2cp(char *p)
{
    char *pp, *p1, *p2, buf[MAX_LINELEN+1];
    int l;
    snprintf(buf,sizeof(buf),"%s",p); strfold(buf); l=strlen(buf);
    if((cpnext-cpbuf)+l>=MAX_LINELEN) sym_error("string_too_long");
    pp=cpnext; memmove(pp,buf,l+1); cpnext+=l+1;
    if(!noaddw) for(p1=find_word_start(buf); *p1; p1=find_word_start(p2)) {
      p2=find_word_end(p1); l=p2-p1;
      if(*p2) *p2++=0;
      if(wordchr(wbuf,p1)!=NULL) continue;
      if(wptr-wbuf>=sizeof(wbuf)-l-2) continue;
      if(wptr>wbuf) *wptr++=' ';
      memmove(wptr,p1,l); wptr+=l; *wptr=0;
    }
    return pp;
}

void cp_string(char *p, struct block *blk, int next)
{
    blk->fn=mt_string;
    blk->string=add2cp(p);
    blk->len=strlen(blk->string);
    blk->nextblock=next;
    if(blk==blockbuf+nextblock) nextblock++;
}

/* p must have MAX_LINELEN */
void cp_cutline(char *p, struct block *blk, int next)
{
    char *p1, *p2, *p3;
    char buf[MAX_LINELEN+1];
    int l, ll, n, idx, start, end;
    struct block *b;

    if(debug>=3) fprintf(stderr,"Cutline %d/%d for %.15s.\n",
                   (int)(blk-blockbuf), nextblock, p);
    for(p1=strstr(p,"[|]"); p1; p1=strstr(p1,"[|]")) memmove(p1," | ",3);
    macro_trans(p);
    singlespace(p);
    p=find_word_start(p); strip_trailing_spaces(p);
    l=0; p3=p; do {
      p1=find_word_start(p3);
      if(*p1) l++;
      p2=strparchr(p1,'[');
      if(p2!=NULL) p3=find_matching(p2+1,']');
      if(p3==NULL) sym_error("unmatched_parentheses %.20s",p);
      if(p2!=NULL && p2>p1) l++;
      p3++;
    } while(p2!=NULL);
    if(l==0) {
      buf[0]=0; cp_string(buf,blk,next);
      return;
    }
    idx=start=nextblock; nextblock+=l-1; end=nextblock;
    if(nextblock > MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    for(p1=find_word_start(p); *p1; p1=find_word_start(p3)) {
      p2=strparchr(p1,'[');
      if(p2==NULL) p2=p1+strlen(p1);
      ll=p2-p1;
      if(ll>0) {
          memmove(buf,p1,ll); buf[ll]=0;
          if(idx==start) b=blk; else b=blockbuf+idx-1;
          if(idx<end) n=idx; else n=next;
          if(debug>=3) fprintf(stderr,"String block %d/%d for %.15s.\n",
                   (int)(b-blockbuf), nextblock, buf);
          cp_string(buf,b,n);
          idx++;
      }
      if(*p2=='[') {
          p2++; p3=find_matching(p2,']');
          memmove(buf,p2,p3-p2); buf[p3-p2]=0; p3++;
          if(idx==start) b=blk; else b=blockbuf+idx-1;
          if(idx<end) n=idx; else n=next;
          cp_oneblock(buf,b,n);
          idx++;
      }
      else p3=p2;
    }
}

unsigned int objnum(char *p, char delim)
{
    int i; char *p1, *p2;

    if(*p==0) return 1;
    i=0;
    for(p1=p; p1; p1=p2, i++) {
      p2=strparchr(p1,delim); if(p2) p2++;
    }
    return i;
}

void _permpick(char *p, int n, struct block *blk, int next, char delim)
{
    int i, t, idx;
    char buf[MAX_LINELEN+1];
    char *pp, *pe;

    idx=nextblock; nextblock+=n;
    if(nextblock > MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    blk->len=n;
    blk->sublock=idx;
    blk->fn=mt_permpick;
    blk->lcnt=clist.lcnt;
    blk->nextblock=next;
    if(nextlist+n > MAX_LISTS) sym_error("list_overflow %.20s",p);
    blk->listlen=listbuf+nextlist; nextlist+=n;
    for(i=t=0; i<clist.lcnt; i++) {
      blk->listlen[i]=clist.listlen[i];
      blk->lists[i]=listbuf+nextlist+t;
      t+=clist.listlen[i];
    }
    if(nextlist+t > MAX_LISTS) sym_error("list_overflow %.20s",p);
    memmove(listbuf+nextlist,clist.list,t*sizeof(listtype));
    nextlist+=t;
    for(i=0, pp=find_word_start(p);i<n;i++,idx++,pp=find_word_start(pe)) {
      pe=strparchr(pp,delim);
      if(pe==NULL) pe=pp+strlen(pp); else *pe++=0;
      snprintf(buf,sizeof(buf),"%s",pp);
      cp_cutline(buf,blockbuf+idx,next);
    }
}

/* alt for two flavours */
void _alt(char *p, struct block *blk, int next, char delim)
{
    int n;
    listtype ltab[]={-1};
    listtype len[]={1};

    clist.lcnt=1; clist.listlen=len; clist.list=ltab;
    n=objnum(p,delim); if(n==0) n=1;
    _permpick(p,n,blk,next,delim);
}

void cp_alt(char *p, struct block *blk, int next)
{
    _alt(p,blk,next,',');
}

void cp_alt2(char *p, struct block *blk, int next)
{
    _alt(p,blk,next,'|');
}

void cp_aperm(char *p, struct block *blk, int next)
{
    int i, n;
    listtype ltab[MAX_BLOCKS];
    listtype len;

    n=objnum(p,','); if(n<4) sym_error("wrong_parmcnt ins %.20s",p);
    if(n>=MAX_BLOCKS/2) sym_error("block_overflow %.20s",p);
    clist.lcnt=1; len=2*n-5; clist.listlen=&len;
    for(i=0;i<n-2;i++) ltab[2*i]=-2;
    for(i=0;i<n-4;i++) ltab[2*i+1]=0;
    ltab[len-2]=1;
    clist.list=ltab;
    _permpick(p,n,blk,blk-blockbuf,',');
    blk->lstart=2;
}

void cp_apick(char *p, struct block *blk, int next)
{
    int i, n, t;
    char *p1;
    listtype ltab[MAX_BLOCKS];
    listtype len;

    n=objnum(p,','); if(n<4) sym_error("wrong_parmcnt ins %.20s",p);
    if(n>=MAX_BLOCKS/2) sym_error("block_overflow %.20s",p);
    p1=find_item_end(p); if(*p1) *p1++=0;
    t=atoi(p); if(t<=0 || t>n-3) sym_error("syntax_error ins %.20s",p);
    clist.lcnt=1; len=2*t-1; clist.listlen=&len;
    for(i=0;i<t;i++) ltab[2*i]=-2;
    for(i=0;i<t-1;i++) ltab[2*i+1]=0;
    ltab[len-2]=1;
    clist.list=ltab;
    _permpick(p1,n,blk,blk-blockbuf,',');
    blk->lstart=2;
}

void cp_dic(char *p, struct block *blk, int next)
{
    int i, n;
    char *p1, *p2;
    n=objnum(p,',');
    if(n!=1) sym_error("wrong_parmcnt dic %.20s",p);
    p1=find_word_start(p); p2=find_word_end(p1);
    if(*p2) *p2++=0;
    p2=find_word_start(p2);
    i=getdic(p1);
    if(i<0) sym_error("bad_dictionary %.20s",p1);
    noaddw=3;
    blk->string=add2cp(p2);
    noaddw=0;
    blk->len=strlen(blk->string);
    blk->lind1=i;
    blk->fn=mt_dic;
    blk->nextblock=next;
}

void cp_dperm(char *p, struct block *blk, int next)
{
    int n;
    listtype ltab[]={0,1,2,2,3,0};
    listtype len[]={3,3};

    clist.lcnt=2; clist.listlen=len; clist.list=ltab;
    n=objnum(p,',');
    if(n!=4) sym_error("wrong_parmcnt dperm %.20s",p);
    _permpick(p,n,blk,blk-blockbuf,',');
}

void cp_ins(char *p, struct block *blk, int next)
{
    int i, n;
    listtype ltab[MAX_BLOCKS];
    listtype len;

    n=objnum(p,','); if(n<3) sym_error("wrong_parmcnt ins %.20s",p);
    if(n>=MAX_BLOCKS/2) sym_error("block_overflow %.20s",p);
    clist.lcnt=1; len=2*n-2; clist.listlen=&len;
    for(i=1;i<n;i++) ltab[2*i-2]=i;
    for(i=1;i<len;i+=2) ltab[i]=-12;
    ltab[len-1]=-13;
    clist.list=ltab;
    _permpick(p,n,blk,blk-blockbuf,',');
    blk->lstart=1;
}

void cp_iperm(char *p, struct block *blk, int next)
{
    int n;
    listtype ltab[]={0,1,2,2,1,0};
    listtype len[]={3,3};

    clist.lcnt=2; clist.listlen=len; clist.list=ltab;
    n=objnum(p,',');
    if(n!=3) sym_error("wrong_parmcnt iperm %.20s",p);
    _permpick(p,n,blk,blk-blockbuf,',');
}

void cp_m(char *p, struct block *blk, int next)
{
    int i, idx;
    char buf[MAX_LINELEN+1];

    i=objnum(p,','); if(i!=1) sym_error("wrong_parmcnt m %.20s",p);
    blk->fn=mt_m;
    blk->string=NULL;
    blk->len=1;
    blk->nextblock=next;
    p=find_word_start(p);singlespace(p);strip_trailing_spaces(p);
    for(i=0;i<Mcnt && strcmp(p,Mind[i].Mptr)!=0;i++);
    if(nextblock >= MAX_BLOCKS-2) sym_error("block_overflow %.20s",p);
    if(i<Mcnt) blk->sublock=Mind[i].blkptr;
    else {
      i=strlen(p);
      if(Mnext-Mbuf+i >= MAX_LINELEN-1) sym_error("Mbuf_overflow %.20s",p);
      if(Mcnt >= MAX_BLOCKS) sym_error("Mind_overflow %.20s",p);
      Mind[Mcnt].Mptr=Mnext; Mind[Mcnt].blkptr=nextblock;
      Mcnt++;
      memcpy(Mnext,p,i+1); Mnext+=i+1;
      idx=nextblock; blk->sublock=idx; nextblock++;
      snprintf(buf,sizeof(buf),"%s",p);
      cp_cutline(buf,blockbuf+idx,-2);
      blockbuf[idx].mend=nextblock;
    }
}

void cp_neg(char *p, struct block *blk, int next)
{
    int n, idx;
    char buf[MAX_LINELEN+1];
    n=objnum(p,','); if(n==0) n=1;
    if(n>1) sym_error("wrong_parmcnt neg %.20s",p);
    blk->fn=mt_neg;
    blk->len=1;
    blk->nextblock=next;
    if(nextblock >= MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    idx=nextblock; blk->sublock=idx; nextblock++;
    snprintf(buf,sizeof(buf),"%s",p);
    cp_cutline(buf,blockbuf+idx,blk-blockbuf);
}

void cp_none(char *p, struct block *blk, int next)
{
    blk->fn=mt_nomatch;
    blk->string="";
    blk->nextblock=next;
}

void _pick(char *p, struct block *blk, int next, int type)
{
    int i, n, t, v;
    listtype ltab[MAX_BLOCKS];
    listtype len;
    char *p1;

    n=objnum(p,','); n--;
    if(n<2) sym_error("wrong_parmcnt pick %.20s",p);
    if(n>=MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    p1=strparchr(p,','); *p1++=0;
    p=find_word_start(p); v=0;
    if(*p=='-') {p++; type-=5;}
    else if(*p=='+') {v=2; p++;}
    t=atoi(p); if(t<1 || t>MAX_PICKS || t>n) sym_error("bad_pickcnt %.20s",p);
    clist.lcnt=1; len=t+v; clist.listlen=&len;
    for(i=0;i<t;i++) ltab[i]=type;
    if(v) {ltab[i++]=-6; ltab[i]=-5;}
    clist.list=ltab;
    _permpick(p1,n,blk,blk-blockbuf,',');
}

void cp_out(char *p, struct block *blk, int next)
{
    char buf[MAX_LINELEN+1];
    char *p1;
    int n, idx;
    n=objnum(p,','); if(n!=2) sym_error("wrong_parmcnt out %.20s",p);
    p1=strparchr(p,','); if(p1) *p1++=0; else p1=p+strlen(p);
    p=find_word_start(p); *find_word_end(p)=0;
    noaddw=3;
    blk->string=add2cp(p);
    noaddw=0;
    blk->len=strlen(blk->string);
    blk->fn=mt_out;
    blk->nextblock=next;
    if(nextblock >= MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    idx=nextblock; blk->sublock=idx; nextblock++;
    snprintf(buf,sizeof(buf),"%s",p1);
    cp_cutline(buf,blockbuf+idx,blk-blockbuf);
}

void cp_opick(char *p, struct block *blk, int next)
{
    _pick(p,blk,next,-3);
}

void cp_perm(char *p, struct block *blk, int next)
{
    int i, n;
    listtype ltab[MAX_BLOCKS];
    listtype len;

    n=objnum(p,','); if(n==0) n=1;
    if(n>=MAX_BLOCKS) sym_error("block_overflow %.20s",p);
    clist.lcnt=1; len=n; clist.listlen=&len;
    for(i=0;i<n;i++) ltab[i]=-2;
    clist.list=ltab;
    _permpick(p,n,blk,blk-blockbuf,',');
}

void cp_pick(char *p, struct block *blk, int next)
{
    _pick(p,blk,next,-2);
}

void cp_rep(char *p, struct block *blk, int next)
{
    int n;
    listtype ltab[]={-1,-5};
    listtype len[]={2};

    clist.lcnt=1; clist.listlen=len; clist.list=ltab;
    n=objnum(p,','); if(n==0) n=1;
    _permpick(p,n,blk,blk-blockbuf,',');
}

void cp_w(char *p, struct block *blk, int next)
{
    items2words(p);
    blk->string=add2cp(p);
    blk->fn=mt_w;
    blk->nextblock=next;
}

void cp_wild(char *p, struct block *blk, int next)
{
    int n, min, max;
    char *pp, *pe;
    n=objnum(p,','); if(n!=1) sym_error("wrong_parmcnt wild %.20s\n",p);
    blk->string="";
    max=min=0;
    for(pp=find_word_start(p); *pp; pp=find_word_start(pe)) {
      pe=find_word_end(pp);
      if(pp[0]!='*') sym_error("syntax_error wild %.20s\n",p);
      if(pp[1]!='*') {
          min++; continue;
      }
      if(isdigit(pp[2])) max+=atoi(pp+2);
      else max=MAX_BLOCKS;
    }
    blk->len=max;
    blk->lstart=min;
    blk->fn=mt_wild;
    blk->nextblock=next;
}

struct builtin builtin[]={
      {"Alt",		cp_alt},
	{"Aperm",	cp_aperm},
	{"Apick",	cp_apick},
	{"Dic",		cp_dic},
	{"Dperm",	cp_dperm},
	{"Ins",		cp_ins},
	{"Iperm",	cp_iperm},
	{"M",		cp_m},
	{"Neg",		cp_neg},
	{"Nomatch",	cp_none},
	{"None",	cp_none},
	{"Not",		cp_neg},
	{"Opick",	cp_opick},
	{"Out",		cp_out},
	{"Perm",	cp_perm},
	{"Pick",	cp_pick},
	{"Rep",		cp_rep},
	{"W",		cp_w},
	{"Wild",	cp_wild},
};

int builtincnt=(sizeof(builtin)/sizeof(builtin[0]));

/* p must have MAX_LINELEN */
void cp_oneblock(char *p, struct block *blk, int next)
{
    char *pp, *pe;
    int i;
    if(debug>=3) fprintf(stderr,"Oneblock %d/%d for %.15s.\n",
                   (int)(blk-blockbuf), nextblock, p);
    if(myisupper(*p)) {
      for(pe=p; pe-p < MAX_BINAME && myisalpha(*pe); pe++);
      if(*pe==':') {
          *pe++=0;
          i=search_list(builtin,builtincnt,sizeof(builtin[0]),p);
          if(i<0) sym_error("unknown_cmd %.20s",p);
          builtin[i].fn(pe,blk,next);
          blk->nextblock=next;
          return;
      }
    }
    if(*p=='*') {
      cp_wild(p,blk,next);
      return;
    }
    pp=strparchr(p,'|'); if(pp==NULL) {
      cp_cutline(p,blk,next);
    }
    else cp_alt2(p,blk,next);
}

/* p must have MAX_LINELEN */
void compile(char *p)
{
    nextblock=1; nextlist=0;
    cpnext=cpbuf;
    memset(blockbuf,0,sizeof(blockbuf));
    cp_oneblock(p,blockbuf,-1);
    showblocks();
}

