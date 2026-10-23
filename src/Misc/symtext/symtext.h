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

#ifndef SYMTEXT_H
#define SYMTEXT_H 1

#include <time.h>

#include "../../Lib/libwims.h"

	/* Maximum of blocks. Limited to sizeof(short). */
#define MAX_BLOCKS	8192
	/* Maximum of pools. Limited to sizeof(short). */
#define MAX_POOLS	10240
	/* Maximum of variable names. */
#define MAX_NAMELEN	63
	/* Maximum of (complete) file names. */
#define MAX_FNAME	199
	/* Maximum of dictionaries, including reserved ones. */
#define MAX_DICS	15
	/* Maximum of lists in permdata. Very limited. */
#define MAX_PERMLIST	4
	/* Size of list buffer */
#define MAX_LISTS	65536
	/* Limit of the length of builtin command names */
#define MAX_BINAME	10
	/* Limit to nesting levels */
#define MAX_LEVELS	16384
	/* How many picks at most */
#define MAX_PICKS	64
	/* limit of entries in a dictionary */
#define MAX_DICENTRIES 512*1024
	/* limit of any single dictionary size */
#define MAX_DICSIZE	10240*1024

	/* int or short */
#define listtype	short int
	/* default dictionary directory */
#define defaultdir	"scripts"

#define char_punct	".,;:?!\""
#define char_math	"+-*/=|%<>()_"
#define char_parenth	"()[]{}"
#define char_cs		"_&$#`\\@~"
#define char_quote	"`'\""

typedef struct block {
    char *string;		/* string for compare */
    int (*fn) (struct block *blk, char *start, int level);
    listtype nextblock, sublock;	/* subblocks are always consecutive */
    listtype len, lcnt, lind1, lind2, lstart, pool, mpool, mend;
    listtype *listlen;
    listtype *lists[MAX_PERMLIST];	/* permutation lists */
} block;

typedef struct poolstruct {
    listtype block, lastpool, ind1, ind2, dirty, len;
    char *string;
    listtype *tag;		/* level tags for recursion */
} poolstruct;

/* from translate.c */
extern struct entry {
    unsigned char *original, *replace;
    int olen,earlier;
} entry[];

int search_dic(struct entry *list, int items, size_t item_size, const char *str);

extern struct dic {
    char name[MAX_FNAME+1];
    char unknown[256];
    char *buf;
    int unknown_type;
    int start;
    int len;
} dic[MAX_DICS];
enum {
    unk_delete, unk_leave, unk_replace
};
extern int transdic;
extern int macrodic;
extern int diccnt;
struct dic *prepare_dic(char *fname);
int getdic(char *dicname);

/*from symtext.c */

extern char styledir[], defbuf[];
char *mkfname(char buf[], char *s,...);
extern int debug;
extern int nextpool;
extern int nexttag;
extern poolstruct poolbuf[MAX_POOLS];
extern block blockbuf[MAX_BLOCKS];
#define OUTSIZE 4096
extern char *outptr, *wptr, outbuf[OUTSIZE];
extern listtype tagbuf[MAX_BLOCKS];
extern int nextblock, nextlist;
extern char wbuf[MAX_LINELEN+1];
extern listtype listbuf[MAX_LISTS];
extern int options;
#define op_nocase    (1<<0)
#define op_deaccent  (1<<1)
#define op_reaccent  (1<<2)
#define op_nopunct   (1<<3)
#define op_nomath    (1<<4)
#define op_noparenth (1<<5)
#define op_nocs      (1<<6)
#define op_noquote   (1<<7)
#define op_matchall  (1<<8)
#define op_alphaonly (1<<9)
#define op_alnumonly (1<<10)
void sym_error(char *msg,...);
void _getdef(char buf[], char *name, char value[]);
/* from match.c */
int mt_string(struct block *blk, char *start, int level);
int mt_permpick(struct block *blk, char *start, int level);
int mt_m(struct block *blk, char *start, int level);
int mt_neg(struct block *blk, char *start, int level);
int mt_dic(struct block *blk, char *start, int level);
int mt_w(struct block *blk, char *start, int level);
int mt_wild(struct block *blk, char *start, int level);
int mt_out(struct block *blk, char *start, int level);
int mt_nomatch(struct block *blk, char *start, int level);

/* from suffix.c */
extern int suffixcnt;
void suffix_translate(char *p);
void _translate(char *p, int i);
void suffix_dic(char *sdicname);

/* from compile.c */
void strfold(char *p);
extern struct builtin {
    char *name;
    void (*fn) (char *p, struct block *blk, int next);
} builtin[];
void compile(char *p);

extern int builtincnt, Mcnt;
extern char Mbuf[MAX_LINELEN+1];
extern char *Mnext;

/* from match.c */
int match(char *p);
#endif



