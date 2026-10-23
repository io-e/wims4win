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
#include "wimslogd.h"

/* daemon command processing */

/* testing routine. Don't touch! */
void cmd_test(char *p)
{
}

void cmd_ping(char *p)
{
  char *p1;
  int qtest;
  p1=find_word_start(p); *find_word_end(p1)=0;
  if(*p1!=0 && qbuf[0]!=0 && wordchr(qbuf,p1)!=NULL) qtest=1; else qtest=0;
  snprintf(textbuf+3,MAX_LINELEN,"%d %s\n%s",qtest,loadavg,ipbuf);
}

void cmd_forklist(char *p)
{
  snprintf(textbuf+3,MAX_LINELEN,"%d",forkcnt);
}

void cmd_readfile(char *p)
{
  char *cut[2];
  int l;
  cutwords(p,cut,2);
  if(cut[1][0]) l=evalue(cut[1]); else l=MAX_LINELEN-16;
  if(l<=0 || cut[0][0]==0) return;
  if(l>BUFFERLEN-16) l=BUFFERLEN-16;
  readfile(cut[0],textbuf+3,l);
}

void cmd_record(char *p)
{
  char *cut[2];
  cutwords(p,cut,2);
  if(cut[1][0]==0) return;
  datafile_fnd_record(cut[1],atoi(cut[0]),textbuf+3);
}

void cmd_recordcnt(char *p)
{
  char *p1;
  p1=find_word_start(p);
  if(*p1==0) return;
  *find_word_end(p1)=0;
  snprintf(textbuf+3,16,"%d",datafile_recordnum(p1));
}

struct cmdlist cmdlist[]={
  {"forkcnt",    cmd_forklist},
  {"forkcount",    cmd_forklist},
  {"forklist",    cmd_forklist},
  {"getscore",    cmd_getscore},
  {"ping",    cmd_ping},
  {"readfile",    cmd_readfile},
  {"record",    cmd_record},
  {"recordcnt",    cmd_recordcnt},
  {"recordcount",    cmd_recordcnt},
  {"recordno",    cmd_recordcnt},
  {"recordnum",    cmd_recordcnt},
  {"scorelog",    cmd_scorelog},
  {"test",    cmd_test},
};
int cmdcnt=(sizeof(cmdlist)/sizeof(cmdlist[0]));

void cmd(void)
{
  char *p1, *p2, cmdline[MAX_LINELEN+1];
  int i;

  errno=0;
  p1=find_word_start(textptr); p2=find_word_end(p1);
  if(*p2) *p2++=0;
  i=search_list(cmdlist,cmdcnt,sizeof(cmdlist[0]),p1);
  if(i<0) {sockerror(2,"bad_cmd %s",p1); return;}
  mystrncpy(cmdline,find_word_start(p2),sizeof(cmdline));
  memmove(textbuf,"OK\n",4);
  cmdlist[i].routine(cmdline);
}

