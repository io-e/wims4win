/*  Le compte est bon - version recursive
 *  Copyright (C) 2002 Lucas Nussbaum <lucas@lucas-nussbaum.net>
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

typedef enum {ADD, MULT, SUB, DIV, EMPTY} operation;

struct result {
	unsigned int val; /* value */
	operation op; /* operator */
	struct result * l; /* left operand */
	struct result * r; /* right operand */
};

const operation ops[4] = { ADD, MULT, SUB, DIV };
const char dispops[4] = { '+', '*', '-', '/' };

/* initializes a result struct */
struct result * resinit()
{
	struct result * p;
	p = (struct result *)malloc(sizeof(struct result));
	p->l = NULL;
	p->r = NULL;
	p->op = EMPTY;
	return p;
}

/* display the result in a readable form. called recursively. */
void dispres(struct result * p)
{
	if (p->op!=EMPTY)
	{
		dispres(p->l);
		dispres(p->r);
		printf("%u %c %u = %u\n", p->l->val, dispops[p->op],p->r->val, p->val);
	}
}

/* calculate the result with those 2 operands and the operator provided */
/* /!\ MUST HAVE p->val >= q->val */
struct result * add(struct result * p, struct result * q, int op)
{
	unsigned int res = 0;
	struct result *r;
	switch(ops[op])
	{
		case ADD:
			if (!(p->val&&q->val)) return NULL;
			res = p->val + q->val;
			break;
		case MULT:
			if ((p->val==1)||(q->val==1)) return NULL;
			res = p->val * q->val;
			break;
		case SUB:
			if (!(p->val&&q->val)) return NULL;
			res = p->val - q->val;
			break;
		case DIV:
			if (!q->val) return NULL;
			if (q->val==1) return NULL;
			if (!(p->val%q->val))
				res = p->val / q->val;
			else
				return NULL;
			break;
		case EMPTY:
			return NULL; /* prevents a compiler warning */
	}
	r = resinit();
	r->op = ops[op];
	r->l = p;
	r->r = q;
	r->val = res;
	return r;
}

struct result * best = NULL;
int min = 10000;
int goal;

/* tests if result is better */
void resultest(struct result * res)
{
	int tmp;
	if ((tmp = (int)res->val - goal) < min)
	{
		min = tmp;
		best = res;
		if (!min)
		{
			printf("FOUND\n");
			dispres(best);
			exit(0);
		}
	}
}

/* recursively try to find an appropriate result */
void compute (struct result ** base, int nb)
{
	struct result * new[6];
	struct result * backup1, * backup2;
	int i, j, k;

	/* generate a brand new array ! */
	memcpy(new, base, sizeof(struct result *) * 6);
	if (nb > 1)
	{
		for (i = 0; i < nb - 1; i++)
		{
			for (j = i + 1; j < nb; j++)
			{
				/* now try to replace the 2 values with a combined one */
				backup1 = new[i];
				backup2 = new[j];
				for (k = 0; k < 4; k++) /* for each operator */
				{
					if ((backup1->val > backup2->val) ? (new[i] = add(backup1, backup2, k)) != NULL : (new[i] = add(backup2, backup1, k)) != NULL)
					{
						resultest(new[i]);
						new[j] = new[nb - 1];
						compute(new, nb - 1);
						new[j] = backup2;
					}
					new[i] = backup1;
				}
			}
		}
	}
}

/* main routine */
int main(int argc, char * argv[])
{
	struct result * base[6];
	int i;

	if (argc!=8)
	{
		printf("There should be 7 arguments. goal, following by the 6 numbers"
				"to use\n");
		exit(-1);
	}
	/* we read the goal */
	goal = atoi(argv[1]);
	/* we read the 6 base numbers */
	for (i = 0; i < 6; i++)
	{
		base[i] = resinit();
		base[i]->val = atoi(argv[i+2]);
	}

	printf("Processing ");
	for (i = 0; i < 6; i++)
		printf("%d ", base[i]->val);
	printf(". Goal : %d\n", goal);

	for (i = 0; i < 6; i++)
		resultest(base[i]);

	compute(base, 6);

	printf("NOTFOUND %d %d\n", best->val, min);
	dispres(best);
	exit(0);
}
