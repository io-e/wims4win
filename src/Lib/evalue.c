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

/* log(-1) does not make sense in real */
#ifndef NAN
#define NAN log(-1)
#endif

    /* Only two decimal points, less than 1 million.
     * No check of buffer length which should be at least 12.
     * returns the end of buffer. */
char *moneyprint(char *p, double s)
{
  char *p1, *p2, buf[16];
  int t, t1, t2;
  if(s<0) {*p++='-'; s=-s;}
  if(s>999999) s=999999;
  t=floor(s*100+0.5); if(t>99999999) t=99999999; if(t<0) t=0;
  if(t==0) {*p++='0'; *p=0; return p;}
  t1=t/100; t2=t%100; p1=buf+10;
  for(*p1--=t1%10+'0',t1/=10;t1>0;*p1--=t1%10+'0',t1/=10);
  p2=buf+11;
  if(t2) {
    *p2++='.';
    *p2++=t2/10+'0'; t2%=10;
    if(t2) *p2++=t2+'0';
  }
  p1++; *p2=0; memmove(p,p1,p2-p1+1); p+=p2-p1;
  return p;
}

/* #define RAND_BUF_SIZE 4096
static char rand_buf[RAND_BUF_SIZE];
*/
/* The trouble here is that httpd does not initialize
     * the variable RANDOM.
     * So I use time (microseconds) to get a quick solution. */
void init_random(void)
{
  int r;
  struct timeval t;
/*    initstate(1,rand_buf,RAND_BUF_SIZE); */
  gettimeofday(&t,NULL);
  r=t.tv_usec+t.tv_sec*1000;
  if(r<0) r=-r;
  if(r==0) r=1;
  srandom(r);
}

/* gives a double random number between 0 and m */
double drand(double m)
{
  double r;
  r=((double) random()+(double) random()/(double) RAND_MAX);
  return (r/(double) RAND_MAX)*m;
}

/* gives a random integer between 0 and n.
 * n maybe floating, but will be rounded */
double irand(double n)
{
  int  end,r;
  if(n==0) return 0;
  if(n>0) end=n; else end=-n;
  r=(double) random()*end/RAND_MAX;
  if(r==n) r--;
  if(n>0) return r; else return -r;
}

/* sign of d */
double sign(double d)
{
  if(d==0) return 0;
  if(d<0) return -1;
  else return 1;
}

/* rounding to integer: problem with half-way rounding */
double myround(double d)
{
  long int t;
  if(d<0) t=d-0.5; else t=d+0.5;
  return t;
}

/* log of base 2 */
double mylog2(double d)
{
  return log(d)/log(2);
}

/* sec */
double sec(double d)
{
  return 1/cos(d);
}

/* csc */
double csc(double d)
{
  return 1/sin(d);
}

/* cotangent function */
double cotan(double d)
{
  return 1/tan(d);
}

/* hyperbolic cotangent */
double cotanh(double d)
{
  return 1/tanh(d);
}

/* factorial of an integer */
double factorial(double d)
{
  int i,n; double t;
  n=d;
  if(n<0 || n!=d) return NAN;
  if(n>1000) return HUGE_VAL;
  t=1; for(i=1;i<=n;i++) t=t*i;
  return t;
}

/* binomial coefficient */
double binomial(double d1,double d2)
{
  return factorial(d1)/(factorial(d2)*factorial(d1-d2));
}

/* max and min */
double max(double d1, double d2)
{
  if(!isfinite(d1) || !isfinite(d2)) return NAN;
  if(d1<d2) return d2; else return d1;
}
double min(double d1, double d2)
{
  if(!isfinite(d1) || !isfinite(d2)) return NAN;
  if(d1<d2) return d1; else return d2;
}

/* gcd and lcm, not really checking errors. */
double gcd(double n1, double n2)
{
  unsigned long long int l1, l2, ll;
  n1=fabs(n1); n2=fabs(n2);
  if(!isfinite(n1) || !isfinite(n2) || n1<0 || n2<0 ||
     n1>1E18 || n2>1E18) return NAN;
  l1=n1; l2=n2;
  if(l1<l2) {
    ll=l1;l1=l2;l2=ll;
  }
  if(l1==0) return NAN;
  while(l2>0) {
    ll=l2;l2=l1%l2;l1=ll;
  }
  return l1;
}

double lcm(double n1, double n2)
{
  return n1*n2/gcd(n1,n2);
}

struct {
  char *name;
  int type;
  double val;
  double (*f1) (double parm);
  double (*f2) (double parm1, double parm2);
} evalname[]={
  {"Argch",	1,	0,	acosh,	NULL},
  {"Argsh",	1,	0,	asinh,	NULL},
  {"Argth",	1,	0,	atanh,	NULL},
  {"E",	0,	M_E,	NULL,	NULL},
  {"EULER",	0,	0.57721566490153286,	NULL,	NULL},
  {EV_S,	0,	0,	NULL,	NULL},
  {EV_T,	0,	0,	NULL,	NULL},
  {EV_X,	0,	0,	NULL,	NULL},
  {EV_Y,	0,	0,	NULL,	NULL},
  {"Euler",	0,	0.57721566490153286,	NULL,	NULL},
  {"Inf",	0,	1,	log,	NULL},
  {"NaN",	0,	0,	log,	NULL},
  {"PI",	0,	M_PI,	NULL,	NULL},
  {"Pi",	0,	M_PI,	NULL,	NULL},
  {"abs",	1,	0,	fabs,	NULL},
  {"acos",	1,	0,	acos,	NULL},
  {"acosh",	1,	0,	acosh,	NULL},
  {"arccos",1,	0,	acos,	NULL},
  {"arcsin",1,	0,	asin,	NULL},
  {"arctan",1,	0,	atan,	NULL},
  {"arctg",	1,	0,	atan,	NULL},
  {"argch",	1,	0,	acosh,	NULL},
  {"argsh",	1,	0,	asinh,	NULL},
  {"argth",	1,	0,	atanh,	NULL},
  {"asin",	1,	0,	asin,	NULL},
  {"asinh",	1,	0,	asinh,	NULL},
  {"atan",	1,	0,	atan,	NULL},
  {"atanh",	1,	0,	atanh,	NULL},
  {"binomial",2,	0,	NULL,	binomial},
  {"ceil",	1,	0,	ceil,	NULL}, /* round-up integer */
  {"ch",	1,	0,	cosh,	NULL},
  {"cos",	1,	0,	cos,	NULL},
  {"cosh",	1,	0,	cosh,	NULL},
  {"cot",	1,	0,	cotan,	NULL},
  {"cotan",	1,	0,	cotan,	NULL},
  {"cotanh",1,	0,	cotanh,	NULL},
  {"coth",	1,	0,	cotanh,	NULL},
  {"csc",	1,	0,	csc,	NULL},
  {"ctg",	1,	0,	cotan,	NULL},
  {"cth",	1,	0,	cotanh,	NULL},
  {"drand",	1,	0,	drand,	NULL},
  {"e",	0,	M_E,	NULL,	NULL},
  {"erf",	1,	0,	erf,	NULL},
  {"erfc",	1,	0,	erfc,	NULL},
  {"euler",	0,	0.57721566490153286,	NULL,	NULL},
  {"exp",	1,	0,	exp,	NULL},
  {"factorial",1,	0,	factorial,	NULL},
  {"floor",	1,	0,	floor,	NULL},
  {"gcd",	2,	0,	NULL,	gcd},
  {"irand",	1,	0,	irand,	NULL},
/*      {"j0",	1,	0,	j0,	NULL}, */ /* Bessel functions */
/*      {"j1",	1,	0,	j1,	NULL}, */
  {"lcm",	2,	0,	NULL,	lcm},
  {"lg",	1,	0,	log10,	NULL},
  {"lgamma",1,	0,	lgamma,	NULL}, /* log of Gamma function */
  {"ln",	1,	0,	log,	NULL},
  {"log",	1,	0,	log,	NULL},
  {"log10",	1,	0,	log10,	NULL},
  {"log2",	1,	0,	mylog2,	NULL},
  {"max",	2,	0,	NULL,	max},
  {"min",	2,	0,	NULL,	min},
  {"pi",	0,	M_PI,	NULL,	NULL},
  {"pow",	2,	0,	NULL,	pow},
  {"rand",	1,	0,	drand,	NULL},
  {"randdouble",1,	0,	drand,	NULL},
  {"randfloat",1,	0,	drand,	NULL},
  {"randint",1,	0,	irand,	NULL},
  {"random",1,	0,	drand,	NULL},
  {"randreal",1,	0,	drand,	NULL},
  {"rint",	1,	0,	myround,	NULL}, /* closest integer */
  {"round",	1,	0,	myround,	NULL}, /* closest integer */
  {"sec",	1,	0,	sec,	NULL},
  {"sgn",	1,	0,	sign,	NULL}, /* sign of the value */
  {"sh",	1,	0,	sinh,	NULL},
  {"sign",	1,	0,	sign,	NULL}, /* sign of the value */
  {"sin",	1,	0,	 sin,	NULL},
  {"sinh",	1,	0,	sinh,	NULL},
  {"sqrt",	1,	0,	sqrt,	NULL},
  {"tan",	1,	0,	tan,	NULL},
  {"tanh",	1,	0,	tanh,	NULL},
  {"tg",	1,	0,	tan,	NULL},
  {"th",	1,	0,	tanh,	NULL},
/*      {"y0",	1,	0,	y0,	NULL}, */
/*      {"y1",	1,	0,	y1,	NULL},  */
};
#define evalname_no (sizeof(evalname)/sizeof(evalname[0]))

int get_evalcnt(void) {return evalname_no;}
char *get_evalname(int i) {return evalname[i].name;}
int get_evaltype(int i) {return evalname[i].type;}
int evaltab_verify(void) {return verify_order(evalname,evalname_no,sizeof(evalname[0]));}
int search_evaltab(char *p) {
  return search_list(evalname,evalname_no,sizeof(evalname[0]),p);
}

static char *evalue_pt;
int evalue_error;

int get_evalue_error(void) { return evalue_error; }
void set_evalue_error(int e) {evalue_error=e; return;}

/* prepare pointer for evaluation */
void set_evalue_pointer(char *p)
{
  evalue_pt=p;
}

/* get position of name in nametable */
int eval_getpos(char *name)
{
  return search_list(evalname,evalname_no,sizeof(evalname[0]),name);
}

/* set value to name */
void eval_setval(int pos, double v)
{
  if(pos>=0 && pos<evalname_no) evalname[pos].val=v;
}

/* get string pointer (after evaluation) */
char *get_evalue_pointer(void)
{
  return evalue_pt;
}

double _evalue(int ord)
{
  double d,dd;
  int i,k;
  char buf[32];

  if(evalue_error) return NAN;
  d=0;
  while(*evalue_pt=='+') evalue_pt++;
  if(*evalue_pt==0) return 0; /* empty string */
  switch(*evalue_pt) {
    case '(':
      evalue_pt++; d=_evalue(')');goto vld;
    case '|':
      if(ord=='|') {
        evalue_pt++; return 0;
      }
      evalue_pt++; d=fabs(_evalue('|'));goto vld;
    case '-':
      evalue_pt++; d=-_evalue(6);goto vld;
  }
  if((128&*evalue_pt)!=0) {/* special character */
    k=(*evalue_pt)&255; evalue_pt++;
    if(k>=130 && k<140) {
      i=(k-130)*200; k=(*evalue_pt)&255; evalue_pt++;
      if(k<33 || k>=233) goto badeval;
      i+=k-33; if(i<0 || i>=evalname_no) goto badeval;
      goto ename;
    }
    if(k>=140 && k<150) {
      i=(k-140)*200; k=(*evalue_pt)&255; evalue_pt++;
      if(k<33 || k>=233) goto badeval;
      if(ev_var==NULL || ev_varcnt==NULL) goto badeval;
      i+=k-33; if(i<0 || i>=*ev_varcnt) goto badeval;
      goto vname;
    }
    evalue_pt++; goto badeval;
  }
  if(*evalue_pt=='.' || myisdigit(*evalue_pt)){
    d=strtod(evalue_pt,&evalue_pt);goto binary;
  }
  for(i=0;myisalnum(*(evalue_pt+i)) && i<16; i++)
    buf[i]=*(evalue_pt+i);
  buf[i]=0; evalue_pt+=i;
  if(i==0) goto badeval;
  if(ev_varcnt!=NULL && ev_var!=NULL && *ev_varcnt>0)
    for(i=0;i<*ev_varcnt;i++) {
      if(strcmp(buf,ev_var[i].name)==0) {
        vname: d=ev_var[i].value; goto vld;
      }
    }
  i=search_list(evalname,evalname_no,sizeof(evalname[0]),buf);
  ename: if(i>=0) switch(evalname[i].type) { /* evaluation of expressions */
    case 0: {
      d=evalname[i].val;
      if(evalname[i].f1!=NULL) {
        if(d==0) d=NAN;
        if(d==1) d=HUGE_VAL;
      }
      break;
    }
    case 1: {
      if(*evalue_pt!='(') return NAN;
      evalue_pt++;
      d=evalname[i].f1(_evalue(')')); break;/* evaluation of function */
    }
    case 2: {
      double parm1,parm2;
      if(*evalue_pt!='(') return NAN;
      evalue_pt++;
      parm1=_evalue(',');parm2=_evalue(')');
      d=evalname[i].f2(parm1,parm2); break;
    }
    default: {      /* This is impossible. */
        return NAN;
    }
  }
  else {
      badeval: evalue_error=-1; return NAN;
  }
  vld:
  if(evalue_error) return NAN;
  binary: /*evaluation des expressions */
  if(*evalue_pt=='!') {
    evalue_pt++; d=factorial(d);
  }
  if(*evalue_pt==ord) {evalue_pt++;goto ok;}/* */
  if(*evalue_pt==0 || /* chaine de caractere finie*/
     (ord<10 && (*evalue_pt==',' || *evalue_pt==';' || *evalue_pt==')'
             || *evalue_pt=='|')))
    goto ok;
  switch(*evalue_pt) {
    case '+':
      if(ord<=8) break;
      evalue_pt++; d+=_evalue(8);goto vld;
    case '-':
      if(ord<=8) break;
      evalue_pt++; d-=_evalue(8);goto vld;
    case '*':
      if(ord<=6) break;
      evalue_pt++; d*=_evalue(6);goto vld;
    case '/':
      if(ord<=6) break;
      evalue_pt++; dd=_evalue(6);
      if(dd==0) {evalue_error=10;return NAN;}
          d/=dd;goto vld;
    case '%': {
      int di, ddi;
      if(ord<=6) break;
      evalue_pt++; dd=_evalue(6);
        if(dd==0) {evalue_error=10;return NAN;}
          di=d; ddi=dd; d=di%ddi;goto vld;
      }
    case '^': {
      if(ord<5) break;
      evalue_pt++; d=pow(d,_evalue(5));goto vld;
    }
    default : {
      return NAN;
    }
  }
  ok: return d;
}

/* substitute variable names by their environment strings
 * The buffer pointed to by p must have enough space
 * (defined by MAX_LINELEN). */
char *_substit(char *p)
{
  return p;
}

char *(*substitute) (char *p)=_substit;

double checked_eval( char* p)
{
  set_evalue_error(0);
  set_evalue_pointer(p);
  return _evalue(10);
}

/* evalue a string to double */
double strevalue(char *p)
{
  char buf[MAX_LINELEN+1];

  if(p==NULL) return 0;
  mystrncpy(buf,p,sizeof(buf));
  substitute(buf); nospace(buf);
  if(check_parentheses(buf,0)) {return NAN;}
  return checked_eval(buf);
}


/* compile an expression for faster evaluation
 * returns -1 if cannot be compiled.
 * else returns the number of compilations.
 */
int evalue_compile(char *p)
{
  char *p1, *p2, *pe, name[256], buf[8];
  int i,k;

  k=0;
  for(p1=p; *p1; p1++) if((128&*p1)!=0) return -1;
  nospace(p);
  for(p1=find_mathvar_start(p); *p1; p1=find_mathvar_start(pe)) {
    pe=find_mathvar_end(p1);
    if(!myisalpha(*p1)) continue;
    p2=pe; if(p2-p1>16) continue;
    memmove(name,p1,p2-p1); name[p2-p1]=0;
/* replace the variables by a number
 * at most 2000 variables on two characters :
 * variable: 140 <= integer <150,  number between 33 and 233
 * function: 130 <= integer < 140, number between 33 and 233
 */
    if(ev_varcnt!=NULL && ev_var!=NULL && *ev_varcnt>0) {
      for(i=0;i<*ev_varcnt && strcmp(name,ev_var[i].name)!=0;i++);
      if(i<*ev_varcnt && i<2000) {
      buf[0]=i/200+140; buf[1]=i%200+33; buf[2]=0;
      string_modify(p,p1,p2,"%s",buf);
      pe=p1+2; k++; continue;
      }
    }
    i=search_list(evalname,evalname_no,sizeof(evalname[0]),name);
    if(i>=0 && i<2000) {
      buf[0]=i/200+130; buf[1]=i%200+33; buf[2]=0;
      string_modify(p,p1,p2,"%s",buf);
      pe=p1+2; k++; continue;
    }
  }
  return k;
}

/* add evaluator (Dominique Bernardi june 2014)
In addition to the general evaluation functions, there is a simple mean
to evaluate standard functions with at most four variables
named "x", "y", "s" and "t". The simplest one is

double eval_simple (char *p, double x, double y, double s, double t);

which does exactly that. In case of multiple evaluation of the same function
for different values of the variable(s), it is possible to speed up a bit
the evaluation by "precompiling" the string to be evaluated.
This precompilation is done by

eval_struct* eval_create (char *p);

which returns a newly allocated pointer to something. One can use
this pointer in functions like

eval_x, eval_t, eval_x_y

When the struct is no longer useful, one can reclaim the memory it used with

void eval_destroy (eval_struct *q);

*/

void _aux (char *q, char *varn, char *subst, int *v)
{
  char *pp;
  for(pp=varchr(q,varn); pp; pp=varchr(pp,varn)) {
    string_modify(q,pp,pp+strlen(varn),"%s",subst);
    pp+=strlen(subst);
  }
  *v = eval_getpos(subst);
}

eval_struct * eval_create (char *in_p)
{
  eval_struct *p = malloc (sizeof(eval_struct));
  char *q = malloc(MAX_LINELEN+1);
  strncpy (q, in_p, MAX_LINELEN);
  _aux (q, "x", EV_X, &p->x);
  _aux (q, "y", EV_Y, &p->y);
  _aux (q, "s", EV_S, &p->s);
  _aux (q, "t", EV_T, &p->t);
  evalue_compile(q);
  p->texte = q;
  return p;
}

double eval_multiple (eval_struct *p, double x, double y, double s, double t)
{
  eval_setval(p->x,x);
  eval_setval(p->y,y);
  eval_setval(p->s,s);
  eval_setval(p->t,t);
  return checked_eval(p->texte);
}
/* non yet useful
double eval_simple (char *p, double x, double y, double s, double t)
{
  eval_struct *q = eval_create (p);
  double r = eval (q, x, y, s, t);
  eval_destroy(q);
  return r;
}

*/

double eval_x (eval_struct *p, double x)
{
  eval_setval(p->x,x);
  return checked_eval(p->texte);
}

double eval_t (eval_struct *p, double t)
{
  eval_setval(p->t,t);
  return checked_eval(p->texte);
}

double eval_x_y (eval_struct *p, double x, double y)
{
  eval_setval(p->x,x);
  eval_setval(p->y,y);
  return checked_eval(p->texte);
}

void eval_destroy(eval_struct *q) {free (q->texte); free (q);}
