/*  Le compte est bon - recherche de la plus grande somme
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

typedef enum {ADD, MULT, SUB, DIV, EMPTY} operation;
typedef int boolean;
#define TRUE 1
#define FALSE 0

struct result {
	unsigned int val; /* value */
	operation op; /* operator */
	struct result * l; /* left operand */
	struct result * r; /* right operand */
	unsigned short used; /* digits used */
	struct result * next; /* you know what a linked list is, don't you ? */
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
	p->next = NULL;
	p->op = EMPTY;
	p->used = 0;
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

/* test if 2 operands are compatible */
boolean compatible (struct result *p, struct result *q, unsigned short *used)
{
	if (p->used&q->used) return FALSE;
	*used = p->used|q->used;
	return TRUE;
}

/* calculate the result with those 2 operands and the operator provided */
/* /!\ MUST HAVE p->val >= q->val */
struct result * add(struct result * p, struct result * q, int op)
{
	int res = 0;
	struct result *r;
	switch(ops[op])
	{
		case ADD:
			res = p->val + q->val;
			break;
		case MULT:
			res = p->val * q->val;
			break;
		case SUB:
			res = p->val - q->val;
			break;
		case DIV:
			if (!q->val) return NULL;
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

/* add all possible results using the p & q operands to the s list */
void addresults(struct result ***s, struct result *p, struct result *q)
{
	unsigned short used;
	int i;
	struct result * r;
	/* switch if needed */
	if (p->val < q->val)
	{
		struct result *tmp;
		tmp = p;
		p = q;
		q = tmp;
	}
	if (compatible(p,q,&used))
		for (i=0; i<4; i++) /* for each operator */
			if ((r = add(p,q,i)))
			{
				r->used = used;
				**s = r;
				*s = &(r->next);
			}
}

unsigned int calc_sum(struct result * r)
{
	if (r)
		return calc_sum(r->l) + r->val + calc_sum(r->r);
	else
		return 0;
}

/* main routine */
int main(int argc, char * argv[])
{
	struct result * results[6];
	struct result *p, **s, *q = NULL;
	int i;
	int argp = 1;
	unsigned int biggestsum = 0;
	if (argc!=8)
	{
		printf("There should be 7 arguments. goal, following by the 6 numbers"
				"to use\n");
		exit(-1);
	}
	/* we read the goal */
	goal = atoi(argv[argp++]);
	/* we read the 6 base numbers */
	results[0] = resinit();
	p = results[0];
	q = results[0];
	p->val = atoi(argv[argp++]);
	p->used = 1;
	for (i = 1; i < 6; i++)
	{
		p = resinit();
		p->val = atoi(argv[argp++]);
		p->used = 1 << i;
		q->next = p;
		q = p;
	}

	printf("Processing ");
	for (p = results[0]; p; p=p->next)
		printf("%d ", p->val);
	printf(". Goal : %d\n", goal);

	/* 1) results composed by 2 base numbers
	 * = res[0] X res[0] */
	s = &(results[1]);
	for (p = results[0]; p->next; p=p->next)
		for (q = p->next; q; q=q->next)
			addresults(&s, p, q);

	/* 2) results composed by 3 base numbers
	 * = res[1] X res[0] */
	s = &(results[2]);
	for (p = results[1]; p; p=p->next)
		for (q = results[0]; q; q=q->next)
			addresults(&s, p, q);

	/* 3) results composed by 4 base numbers
	 * = res[1] X res[1] + res[2] X res[0] */
	s = &(results[3]);
	for (p = results[1]; p->next; p=p->next)
		for (q = p->next; q; q=q->next)
			addresults(&s, p, q);

	for (p = results[2]; p; p=p->next)
		for (q = results[0]; q; q=q->next)
			addresults(&s, p, q);

	/* 4) results composed by 5 base numbers
	 * = res[2] X res[1] + res[3] X res[0] */
	s = &(results[4]);
	for (p = results[3]; p; p=p->next)
		for (q = results[0]; q; q=q->next)
			addresults(&s, p, q);

	for (p = results[2]; p; p=p->next)
		for (q = results[1]; q; q=q->next)
			addresults(&s, p, q);

	/* 5) results composed by 6 base numbers
	 * = res[2] X res[2] + res[3] X res[1] + res[4] X res[0] */
	s = &(results[5]);
	for (p = results[2]; p->next; p=p->next)
		for (q = p->next; q; q=q->next)
			addresults(&s, p, q);

	for (p = results[3]; p; p=p->next)
		for (q = results[1]; q; q=q->next)
			addresults(&s, p, q);

	for (p = results[4]; p; p=p->next)
		for (q = results[0]; q; q=q->next)
			addresults(&s, p, q);

	/* results analysis */
	/* here, find best result */
	for (i=0; i<6; i++)
	{
		p = results[i];
		while (p)
		{
			if (abs((int)p->val-goal)<min)
			{
				best = p;
				min = p->val-goal;
			}
			if (abs((int)p->val)==goal)
			{
				unsigned int tmp = calc_sum(p);
				if (tmp > biggestsum)
				{
					biggestsum = tmp;
					best = p;
				}
			}
			p = p->next;
		}
	}
	if (min)
		printf("NOTFOUND %d %d\n", best->val, min);
	else
		printf("FOUND\n");
	dispres(best);
	exit(0);
}
