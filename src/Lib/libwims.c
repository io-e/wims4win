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

void (*string_modify)(char *start, char *bad_beg, char *bad_end, char *good,...)=string_modify1;

char *fnd_position;
char *fnd_nextpos;
int *ev_varcnt=NULL;
ev_variable *ev_var=NULL;

void error(char *msg)
{
  fprintf(stderr,"%s\n",msg);
  printf("ERROR\n");
  exit(1);
}
