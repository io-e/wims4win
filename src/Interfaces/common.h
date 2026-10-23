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


/******** Modifiable definitions ****************/

	/* buffer length for about information */
#define aboutlen 512

/******** Nothing to modify below ***************/

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <unistd.h>
#include <string.h>
#include <ctype.h>
#include <time.h>
#include <errno.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <signal.h>
#include <fcntl.h>
#include "../config.h"
#include "../wimsdef.h"
#include "../Lib/libwims.h"

#define parmlim (MAX_LINELEN+256)
#define inputlim (512*1024)
#define SETUP_NO (sizeof(setups)/sizeof(setups[0]))
extern int illegal_no;
extern int illpart_no;
extern char *stringprinter;
extern char *quitstring;

/* from common.c */
void find_illegal(char *p);
char *find_matching2(char *p, char c);
void strip_zeros(char *p);
void prepabout(char *cmd, char *outf, char *errf);
int readabout(void);
char *strip_trailing_spaces2(char *p);
extern char aboutbuf[aboutlen];
extern char outputfname[256], *tmp_dir;
extern unsigned int seed; /* random seed value */
extern char *wseed;
void prepare1(void);
void run(void);

extern char *progname;
extern char *illpart[];

/* from pari.c */
extern char *illegal[];
extern int precision, fsizelim;
extern char header[];
extern char *nameofcmd;

/* from scilab.c */
extern int must_chroot;

/* from yacas.c */
extern char *cmdparm;
