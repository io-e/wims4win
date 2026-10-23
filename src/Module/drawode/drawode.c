/*
  #define _ISOC99_SOURCE

  This does not work on glibc 2.0.x --- so sorry.
*/
#include <sys/time.h>
#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <unistd.h>
#include <gd.h>
#include "drawode.h"

#define NTEST 50
#define NPARAM 4
#define MINDIV 2
#define MAXCOUNT 50
#define XDIV 32
#define YDIV 32
#define XMIN (-zoom)
#define YMIN (-zoom)
#define XMAX (zoom)
#define YMAX (zoom)
#define XSTEP ((XMAX-XMIN)/XDIV)
#define YSTEP ((YMAX-YMIN)/YDIV)
#define XDEN (1.0/XSTEP)
#define YDEN (1.0/YSTEP)
#define ARROW_LEN 8
#define ARROW_ANGLE 0.3
#define MIN_STEP 2 /* minimum steps to draw arrows */

static char pr[XDIV][YDIV]; /* footprint */
static char xr[XDIV][YDIV], nr[XDIV][YDIV]; /* used by get_point() */

static double dir_angle; /* origin is x0, y0 */
double values[NPARAM];
static double zoom=1.0;
static int width=200, height=200;
static gdImagePtr img;
static int black, white;
static const char *out_fname=NULL;
static int func_num=0;

static inline int transx(double x)
{
  return lrint((x-XMIN)*XDEN);
}

static inline int transy(double y)
{
  return lrint((y-YMIN)*YDEN);
}

/* returns 0 for success, -1 for out-of-bound or already-gone-to (x,y) */
static inline int footprint(int px, int py)
{
  if (px>=0 && px<XDIV && py>=0 && py<YDIV && !pr[px][py])
    {
      pr[px][py]=1;
      return 0;
    }
  else return -1;
}

/* returns 0 for success, -1 for fail */
static int get_point(double *x, double *y)
{
  int div, i, j;
  int mdiv, mi=0, mj=0;
  int found;

  mdiv=0;
  div=1;
  memcpy(xr, pr, sizeof(pr));
  while (1)
    {
      if (div>XDIV) break;
      found=0;
      for (i=0; i<=XDIV-div; i++)
      for (j=0; j<=YDIV-div; j++)
        if (xr[i][j]==0)
          {
            found=1;
            mdiv=div;
            mi=i;
            mj=j;
            break;
          }
      if (!found) break;
      for (i=0; i<XDIV-div; i++)
      for (j=0; j<YDIV-div; j++)
        nr[i][j]=(xr[i][j]+xr[i][j+1]+xr[i+1][j]+xr[i+1][j+1]) ? 1 : 0;
      memcpy(xr, nr, sizeof(nr));
      div++;
    }
  if (mdiv>=MINDIV)
    {
      *x=XMIN+(mi+(mdiv-1)*0.5)*XSTEP;
      *y=YMIN+(mj+(mdiv-1)*0.5)*YSTEP;
      return 0;
    }
  else return -1;
}

static void draw_init(void)
{
  int i, j;
  for (i=0; i<XDIV; i++)
    for (j=0; j<YDIV; j++)
      pr[i][j]=0;
}

static double frand(double min, double max)
{
  return min+(max-min)*(double)rand()/RAND_MAX;
}

static double get_dt(void)
{
  func_t dx, dy;
  double dt;
  double x, y, xp, yp;
  int i;

  dx=funcs[func_num][0];
  dy=funcs[func_num][1];
  dt=1.0; /* enough, eh? */

  srand(time(NULL));
  for (i=0; i<NTEST; i++)
    {
      x=frand(XMIN, XMAX);
      y=frand(YMIN, YMAX);
      xp=fabs(dx(x, y));
      yp=fabs(dy(x, y));
      if (xp*dt>XSTEP)
      dt=XSTEP/xp;
      if (yp*dt>YSTEP)
      dt=YSTEP/yp;
    }
  return dt*0.5; /* for precision */
}

static int do_loop(double x0, double y0, double dt)
{
  func_t dx, dy;
  int i, count=0, steps=0;
  int px, py;
  double x, y, lx, ly;
  double xa[5], ya[5];
  double gxden, gyden;

  dx=funcs[func_num][0];
  dy=funcs[func_num][1];
  x=x0;
  y=y0;
  gxden=width/(XMAX-XMIN);
  gyden=height/(YMAX-YMIN);
  /*  fprintf(stderr, "Start: (%0.2f, %0.2f)\n", x0, y0); */
  while (1)
    {
      steps++;
      px=transx(x);
      py=transy(y);
      if (footprint(px, py)<0 && steps>1) break;
      /* fprintf(stderr, "Step %d: footprint(%d, %d)\n", steps, px, py); */
      lx=x;
      ly=y;
      count=0;
      while (count<MAXCOUNT)
      {
        count++;
        xa[0]=x;
        ya[0]=y;
        for (i=1; i<=4; i++)
          {
            xa[i]=xa[0]+dx(xa[i-1], ya[i-1])*dt;
            ya[i]=ya[0]+dy(xa[i-1], ya[i-1])*dt;
          }
        x=(xa[1]+2.0*xa[2]+xa[3]-xa[4])*(1/3.0);
        y=(ya[1]+2.0*ya[2]+ya[3]-ya[4])*(1/3.0);
        if (transx(x)!=px) break;
        if (transy(y)!=py) break;
      }
      if (dt>0 && steps==1)
      dir_angle=atan2(y-y0, x-x0);
      gdImageLine(img,
                lrint((lx-XMIN)*gxden), height-1-lrint((ly-YMIN)*gyden),
                lrint((x-XMIN)*gxden), height-1-lrint((y-YMIN)*gyden), black);
    }
  return steps;
}

static void draw_main(double dt)
{
  double x0, y0;
  int px0, py0, px1, py1, px2, py2, px3, py3;
  int step1, step2;

  x0=0;
  y0=0;
  draw_init();
  do {
      step1=do_loop(x0, y0, dt);
      step2=do_loop(x0, y0, -dt);
      if (step1>=MIN_STEP+1 && step2>=MIN_STEP) /* draw an arrow */
      {
        px0=lrint((x0-XMIN)/(XMAX-XMIN)*width);
        py0=lrint((y0-YMIN)/(YMAX-YMIN)*height);
        px1=px0+lrint(ARROW_LEN*cos(dir_angle));
        py1=py0+lrint(ARROW_LEN*sin(dir_angle));
        px2=px1-lrint(ARROW_LEN*cos(dir_angle-ARROW_ANGLE));
        py2=py1-lrint(ARROW_LEN*sin(dir_angle-ARROW_ANGLE));
        px3=px1-lrint(ARROW_LEN*cos(dir_angle+ARROW_ANGLE));
        py3=py1-lrint(ARROW_LEN*sin(dir_angle+ARROW_ANGLE));
        gdImageLine(img, px1, height-1-py1, px2, height-1-py2, black);
        gdImageLine(img, px1, height-1-py1, px3, height-1-py3, black);
      }
    }
  while (get_point(&x0, &y0)>=0);
}

void show_usage(void)
{
  fprintf(stderr,
        "Usage: drawode [arguments...]\n\n"
        "Arguments:\n"
        " -h            Help\n"
        " -o <fname>    Set output file name\n"
        " -w <width>    Set width\n"
        " -l <height>   Set height\n"
        " -s <size>     Set width and height\n"
        " -f <num>      Set function number\n"
        " -p <params>   Set parameters (comma separated)\n"
        "\nValid function numbers:\n"
        " 0             x'=y y'=x(x+y)+ax+by\n"
        " 1             x'=ax+by y'=bx+ay\n"
        " 2             x'=ax+by y'=cx+dy\n"
        );
}

void read_params(const char *args)
{
  int i;
  char *p;

  i=0;
  while (i<NPARAM && (*args))
    {
      values[i]=strtod(args, &p);
      if (*p!=',') break;
      args=p+1;
      i++;
    }
  if (! isfinite(values[i]))
    {
      fprintf(stderr, "Invalid parameter value.\n");
      exit(1);
    }
}

int main(int argc, char **argv)
{
  FILE *file;
  int c;
  int i;

  for (i=0; i<NPARAM; i++) values[i]=0.0;
  /* read arguments */
  while ((c=getopt(argc, argv, "ho:w:l:s:f:p:v")) != -1)
    switch (c)
      {
      case 'h':
        show_usage();
        return 0;
        break;
      case 'v':
        fprintf(stderr, "DrawODE version 1.0\n");
        return 0;
        break;
      case 'o':
        out_fname=optarg;
        break;
      case 'w':
        width=atoi(optarg);
        break;
      case 'l':
        height=atoi(optarg);
        break;
      case 's':
        width=height=atoi(optarg);
        break;
      case 'f':
        func_num=atoi(optarg);
        if (func_num<0 || func_num>=nfunc)
        {
          fprintf(stderr, "Invalid function number %d (max %d).\n", func_num, nfunc-1);
          return 1;
        }
        break;
      case 'p':
        read_params(optarg);
        break;
      }

  if (out_fname==NULL)
    {
      fprintf(stderr, "You need to specify a file name.\n");
      return 1;
    }
  if (width<0 || height<0)
    {
      fprintf(stderr, "Invalid size.\n");
      return 1;
    }
  file=fopen(out_fname, "wb");
  if (file==NULL)
    {
      perror(out_fname);
      return 1;
    }
  img=gdImageCreate(width, height);
  white=gdImageColorAllocate(img, 255, 255, 255);
  black=gdImageColorAllocate(img, 0, 0, 0);
  draw_main(get_dt());
  gdImageGif(img, file);
  fclose(file);
  return 0;
}



