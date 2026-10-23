#include "drawode.h"

/* use nfunc in the outside world, so others don't depend on this file. */

#define NFUNC 3

const int nfunc=NFUNC;

/* The famous example */
static double dx_1(double x, double y)
{
  return y;
}

static double dy_1(double x, double y)
{
  return x*(x+y)+values[0]*x+values[1]*y;
}

/* Linear stuff */
static double dx_2(double x, double y)
{
  return values[0]*x+values[1]*y;
}

static double dy_2(double x, double y)
{
  return values[1]*x+values[0]*y;
}

/* All kinds of linear things */
static double dx_3(double x, double y)
{
  return values[0]*x+values[1]*y;
}

static double dy_3(double x, double y)
{
  return values[2]*x+values[3]*y;
}



func_t funcs[NFUNC][2]={{dx_1, dy_1}, {dx_2, dy_2}, {dx_3, dy_3}};
