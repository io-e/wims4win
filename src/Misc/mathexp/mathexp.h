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

	/* Mathematical expression manipulations for WIMS, header declarations */


#include <regex.h>
#include "../../Lib/libwims.h"

	/* maximal number of object lines */
#define MAX_OBJLINES	2048
	/* maximal number of cuts */
#define MAX_COMMAS	1024
	/* maximal number of regex strings */
#define MAX_REGEX	16


enum{
    exp_number, exp_variable,
    exp_paren, exp_matrix, exp_set, exp_fn,
    exp_exp, exp_muldiv, exp_cupcap, exp_add,
    exp_eq, exp_ineq, exp_not, exp_and, exp_or, exp_imply,
    exp_comma, exp_quantifier
};

extern char *typenames[];

typedef struct op {
    char *name;
    int lvl;
} op;

typedef struct ex {
    char *name;
    int lvl1, lvl2;
} ex;

extern op oppunct[];
extern op opalpha[];
extern ex exptype[];
extern int oppunctno, opalphano, exptypeno;

typedef struct regex{
    char *srcreg;
    regex_t cmpreg;
    int isvar;
} regex;
extern regex regexchk[];

int checkregex(char *p);
void error(char *msg);
void getregex(char *p);

extern int regexcnt;
int _type(char *p, int commas[], int *commacnt);

extern int objlinecnt;
extern int thisobjline;
extern int thislinelen;
extern int expl1;
extern int expl2;

extern char *reqtype;
extern int logdir;
extern int linelogdir;
extern char *objline[MAX_OBJLINES];

extern int nocomma;
void req_cut(void);
void req_extract(void);
void req_not(void);
void req_type(void);
