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

/* fork management */
#include "wimslogd.h"

#define MAX_FORK 1024
#define MAX_DELAY 1500      /* At most these seconds of execution */

struct {
  pid_t pid;
  time_t t;
  int type;
  int pad;
} forklist[MAX_FORK];
int forkcnt;

void addfork(pid_t pid, int type)
{
  if(forkcnt>=MAX_FORK) {
    kill(pid,SIGKILL); return;
  }
  forklist[forkcnt].pid=pid;
  forklist[forkcnt].t=time(NULL);
  forklist[forkcnt].type=type;
  forkcnt++;
}

/* forklist management */
void forkman(int kz)
{
  int delay, i, t, st;
  time_t now;
  delay=MAX_DELAY;
  if(forkcnt>=MAX_FORK/2) delay=delay/5;
  if(forkcnt*4>=MAX_FORK*3) delay=delay/4;
  now=time(NULL);
  for(i=forkcnt-1; i>=0; i--) {
    if(now-forklist[i].t>delay) kill(forklist[i].pid,SIGKILL);
    t=waitpid(forklist[i].pid,&st,WNOHANG);
    if(t==0 || !WIFEXITED(st)) continue;
    memmove(forklist+i,forklist+i+1,forkcnt-1-i);
    forkcnt--;
  }
  if(kz) waitpid(-1,NULL,WNOHANG); /* kill zombies */
}

void wait_children(void)
{
  time_t now;
  int i;
  do {
    now=time(NULL);
    for(i=0; i<forkcnt; i++) {
      if(forklist[i].type && forklist[i].t < now-MAX_DELAY) break;
    }
    if(i>=forkcnt) return;
    sleep(1);
  }
  while(1);
}

