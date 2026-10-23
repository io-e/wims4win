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

#include "symtext.h"

char inpbuf[MAX_LINELEN+1], troutbuf[2*MAX_LINELEN+2];
struct entry entry[MAX_DICENTRIES];
int entrycount=0;

struct dic dic[MAX_DICS];
int diccnt;
int transdic, macrodic;

int compare(struct entry *e, const char *s2)
{
    int k;
    k=strncmp((char*)e->original, (char*)s2, e->olen);
    if(k==0 && isalnum(*(s2+e->olen))) return -1;
    else return k;
}

	/* searches a list. Returns index if found, -1 if nomatch.
	 * Uses binary search, list must be sorted. */
int search_dic(struct entry *list, int items, size_t item_size, const char *str)
{
    int i1,i2,j,k,t,t1;
    unsigned char c;

    if(items<=0) return -1;
    j=0; c=str[0];
    k=list[0].original[0]-c; if(k==0) k=compare(list,str);
    if(k==0) goto more; if(k>0) return -1;
    j=items-1; k=list[j].original[0]-c; if(k==0) k=compare(list+j,str);
    if(k==0) return j;
    if(k>0) for(i1=0,i2=j;i2>i1+1;) {
	j=i1+(i2-i1)/2;
	k=list[j].original[0]-c; if(k==0) k=compare(list+j,str);
	if(k==0) goto more;
	if(k>0) {i2=j; continue;}
	if(k<0) {i1=j; continue;}
    }
    if(k>0) {j--;k=compare(list+j,str);}
    more:
    if((t=list[j].earlier)<0) {
	if(k==0) return j; else return -1;
    }
    if(compare(entry+t,str)!=0) return -1;
    for(j=t1=t,k=0;j<items+(list-entry) && entry[j].earlier==t1 && (k=compare(entry+j,str))<=0; j++)
      if(k==0) t=j;
    return t-(list-entry);
}

/* Prepare dictionary */
struct dic *prepare_dic(char *fname)
{
    int i,l;
    struct dic *thisdic;
    FILE *dicf;
    char *p1, *p2, *pp;
    char tbuf[256], buf[MAX_LINELEN+1];
    long int flen;

    if(diccnt>=MAX_DICS) sym_error("too_many_dictionaries");
    thisdic=dic+diccnt; diccnt++;
    thisdic->len=0;
    thisdic->start=entrycount;
    snprintf(thisdic->name,sizeof(thisdic->name),"%s",fname);
    dicf=fopen(mkfname(NULL,"%s/%s",styledir,fname),"r"); if(dicf==NULL) return NULL;
    fseek(dicf,0,SEEK_END);flen=ftell(dicf); fseek(dicf,0,SEEK_SET);
    if(flen>=MAX_DICSIZE) return NULL;
    thisdic->buf=xmalloc(flen+16);flen=fread(thisdic->buf,1,flen,dicf);
    fclose(dicf);
    if(flen>0 && flen<MAX_DICSIZE) thisdic->buf[flen]=0;
    else return NULL;
    for(i=entrycount,p1=thisdic->buf;p1!=NULL && *p1!=0 && i<MAX_DICENTRIES;p1=p2) {
	p2=strchr(p1+1,'\n'); if(p2>p1) *p2++=0;
	pp=strchr(p1,':'); if(pp==NULL) continue;
	*pp++=0;
	strip_trailing_spaces(p1); strip_trailing_spaces(pp);
	singlespace(p1);
	p1=find_word_start(p1); pp=find_word_start(pp);
	if(*p1==0) continue;
	if(i>entrycount && compare(entry+i-1,p1)>0)
	  sym_error("unsorted_dictionary %s: %s > %s.\n",
		fname,entry[i-1].original,p1);
	if(i>entrycount && strcmp((char*)entry[i-1].original,p1)==0)
	  sym_error("duplication_in_dictionary %s: %s.\n",
		fname,p1);
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
    thisdic->len=i-entrycount;
    pp=strrchr(fname,'/'); if(pp==NULL) pp=fname;
    snprintf(tbuf,sizeof(tbuf),"unknown_%s",pp);
    _getdef(defbuf,tbuf,buf);
    p1=find_word_start(buf); *find_word_end(p1)=0;
    for(pp=p1; *pp; pp++) *pp=tolower(*pp);
    thisdic->unknown_type=unk_delete;
    if(strcmp(p1,"leave")==0) thisdic->unknown_type=unk_leave;
    else if(strcmp(p1,"delete")!=0) {
	thisdic->unknown_type=unk_replace;
	snprintf(thisdic->unknown,sizeof(thisdic->unknown),"%s",p1);
    }
    entrycount=i;
    if(debug) fprintf(stderr,"Dictionary %d: %s, %d entries.\n",
		      diccnt,fname,thisdic->len);
    return thisdic;
}

/* make the translation. */
void _translate(char *p, int i)
{
    char *p1, *p2, *pp;
    int t;

    if(i<0 || i>=diccnt) return;
    if(dic[i].len<=0) return;
    snprintf(troutbuf,sizeof(troutbuf),"%s",p);
    for(p1=find_word_start(troutbuf);
	p1!=NULL && p1-troutbuf<MAX_LINELEN && *p1!=0;
	p1=p2) {
	p2=find_word_end(p1);
	for(pp=p1;pp<p2 && (isalnum(*pp) || strchr("_",*pp)!=NULL);pp++);
	p2=find_word_start(p2);
	if(pp==p1 || (*pp!=0 && strchr(" ,.?!",*pp)==NULL)) continue;
	t=search_dic(entry+dic[i].start,dic[i].len,sizeof(entry[0]),p1);
	if(t<0) {
	    switch(dic[i].unknown_type) {
		case unk_leave: break;
		case unk_delete: {
		    ovlstrcpy(p1,find_word_start(pp)); p2=p1;
		    break;
		}
		case unk_replace: {
		    string_modify(troutbuf,p1,pp,dic[i].unknown);
		    p2=find_word_start(p1+strlen(dic[i].unknown));
		}
	    }
	    continue;
	}
	t+=dic[i].start;
	string_modify(troutbuf,p1,p1+strlen((char*)entry[t].original),
		      (char*)entry[t].replace);
	p2=find_word_start(p1+strlen((char*)entry[t].replace));
    }
    snprintf(p,MAX_LINELEN,"%s",troutbuf);
}

	/* make translation using file name */
void translate(char *p, char *dicname)
{
    int i;
    for(i=0;i<diccnt && strcmp(dicname,dic[i].name)!=0;i++);
    if(i<diccnt) _translate(p,i);
}

	/* Returns dictionary index, or -1 if not found */
int getdic(char *dicname)
{
    int i;
    char *p1, *p2, buf[MAX_LINELEN+1];
    for(i=0;i<diccnt && strcmp(dicname,dic[i].name)!=0;i++);
    if(i<diccnt) return i;
    _getdef(defbuf,"dictionaries",buf);
    p1=wordchr(buf,dicname); if(p1==NULL) return -1;
    for(p2=p1; myisalnum(*p2) || *p2=='.'; p2++);
    if(p2-p1 >= MAX_NAMELEN) return -1;
    *p2=0; i=diccnt;
    prepare_dic(dicname); return i;
}

