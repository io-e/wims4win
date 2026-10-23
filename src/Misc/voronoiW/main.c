/*** MAIN.C ***/

#include <stdio.h>
#include <stdlib.h>  /* realloc(), qsort() */

#include <string.h>
#include <ctype.h>

#include "vdefs.h"

Site * nextone(void) ;
void readsites(void) ;

int sorted, triangulate, plot, debug, nsites, siteidx ;
float xmin, xmax, ymin, ymax ;
Site * sites ;
Freelist sfl ;

int
main(int argc, char *argv[])
    {
    Site *(*next)() ;

    sorted = plot = debug = 0 ;
    triangulate = 0;

    freeinit(&sfl, sizeof(Site)) ;
    readsites() ;
    next = nextone ;
    siteidx = 0 ;
    geominit() ;
    if (plot)
        {
        plotinit() ;
        }
    voronoi(next) ;
    return (0) ;
    }

/*** sort sites on y, then x, coord ***/

int
scomp(const void * vs1, const void * vs2)
    {
    Point * s1 = (Point *)vs1 ;
    Point * s2 = (Point *)vs2 ;

    if (s1->y < s2->y)
        {
        return (-1) ;
        }
    if (s1->y > s2->y)
        {
        return (1) ;
        }
    if (s1->x < s2->x)
        {
        return (-1) ;
        }
    if (s1->x > s2->x)
        {
        return (1) ;
        }
    return (0) ;
    }

/*** return a single in-storage site ***/

Site *
nextone(void)
    {
    Site * s ;

    if (siteidx < nsites)
        {
        s = &sites[siteidx++];
        return (s) ;
        }
    else
        {
        return ((Site *)NULL) ;
        }
    }

/*** read all sites, sort, and compute xmin, xmax, ymin, ymax ***/

void
readsites(void)
    {
    int i ;
    char *s = getenv("wims_exec_parm");

    if (!s || !*s) exit(0); /* nothing to do */

    while (isspace(*s)) s++;
    if (*s == '-' && s[1] == 't')
    {
      s += 2; triangulate = 1;
      while (isspace(*s)) s++;
    }

    nsites = 0 ;
    sites = (Site *) myalloc(4000 * sizeof(Site));
    while (sscanf(s, "%f %f", &sites[nsites].coord.x,
&sites[nsites].coord.y) !=EOF)
        {
        sites[nsites].sitenbr = nsites ;
        sites[nsites++].refcnt = 0 ;
        if (nsites % 4000 == 0)
            sites = (Site *)
realloc(sites,(nsites+4000)*sizeof(Site));
        s = strchr(s, '\n');
        if (!s++) break;
        }

    qsort((void *)sites, nsites, sizeof(Site), scomp) ;
    xmin = sites[0].coord.x ;
    xmax = sites[0].coord.x ;
    for (i = 1 ; i < nsites ; ++i)
        {
        if(sites[i].coord.x < xmin)
            {
            xmin = sites[i].coord.x ;
            }
        if (sites[i].coord.x > xmax)
            {
            xmax = sites[i].coord.x ;
            }
        }
    ymin = sites[0].coord.y ;
    ymax = sites[nsites-1].coord.y ;
    }
