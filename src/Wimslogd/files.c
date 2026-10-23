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

/* File manipulation */

struct stat ftst;

/* A simple front-end of stat(). */
int ftest(char *fname)
{
  if(strstr(fname,"..")!=NULL) return -1; /* parent directory not allowed */
  if(stat(fname,&ftst)) return -1;
  if(S_ISREG(ftst.st_mode)) {
    if((ftst.st_mode&(S_IXUSR|S_IXGRP|S_IXOTH))!=0) return is_exec;
    else return is_file;
  }
  if(S_ISDIR(ftst.st_mode)) return is_dir;
  if(S_ISFIFO(ftst.st_mode)) return is_fifo;
  if(S_ISSOCK(ftst.st_mode)) return is_socket;
  return is_unknown;
}

/* read the content of a file */
void readfile(char *fname, char buf[], long int buflen)
{
  int fd, st;
  long int l, lc;
  buf[0]=0;
  st=ftest(fname); if(st!=is_file) return;
  l=ftst.st_size; if(l<=0) return;
  if(l>=buflen) l=buflen-1;      /* silent trancation */
  fd=open(fname,O_RDONLY); if(fd==-1) return;
  lc=read(fd,buf,l); close(fd);
  if(lc!=l) {buf[0]=0; return;}
  buf[lc]=0; _tolinux(buf); return;
}

/* datafile structure: number of records.
 * tag=1 if direct access
 */
unsigned int datafile_recordnum(char *p)
{
  char *pp, buf[MAX_FILELEN+1];
  int i;

  readfile(p,buf,sizeof(buf));
  if(buf[0]!=tag_string[1]) i=0; else i=1;
  for(pp=strstr(buf,tag_string); pp!=NULL; i++, pp=strstr(pp+1,tag_string));
  return i;
}

/* datafile structure: find record n, starting from 1 */
char *datafile_fnd_record(char *p, int n, char bf[])
{
  char *pp, *p2, buf[MAX_FILELEN+1];
  int i;

  bf[0]=0;
  if(n<0) return bf;
  readfile(p,buf,sizeof(buf));
  if(buf[0]!=tag_string[1]) i=0; else i=1;
  if(i<n) {
    for(i++, pp=strstr(buf,tag_string);
        i<n && pp!=NULL;
        i++, pp=strstr(pp+1,tag_string));
  }
  else {
    if(i>n) goto end;
    pp=buf-1;
  }
  if(pp==NULL) { /* n too big */
    goto end;
  }
  if(n>0) pp+=strlen(tag_string); else pp=buf;
  p2=strstr(pp,tag_string); if(p2) *p2=0;
  mystrncpy(bf,pp,MAX_LINELEN);
  end: return bf;
}
