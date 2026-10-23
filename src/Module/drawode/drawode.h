#ifndef RK_H
#define RK_H

typedef double (*func_t)(double x, double y);

extern func_t funcs[][2];
extern const int nfunc;
extern double values[];

/* For non-isoc99 people.  Not very efficient */
#ifndef lrint
 #define lrint(x) ((int)rint(x))
#endif
#ifndef isfinite
 #define isfinite(x) (!isinf(x) && !isnan(x))
#endif

#endif
