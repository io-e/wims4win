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

/* directory manipulation routines. */

/* remove a tree */
int remove_tree(char *dirname)
{
  DIR *sdir;
  struct dirent *f;
  struct stat dst;

  sdir=opendir(dirname);
  if(sdir==NULL) {   /* Cannot open session directory. */
    return -1;
  }
  while((f=readdir(sdir))!=NULL) {
    char fname[MAX_LINELEN+1];
    if(strcmp(".",f->d_name)==0 || strcmp("..",f->d_name)==0) continue;
    snprintf(fname,sizeof(fname),"%s/%s",dirname,f->d_name);
    if(lstat(fname,&dst)) continue;
    if(S_ISDIR(dst.st_mode)) remove_tree(fname);
    else remove(fname);
  }
  closedir(sdir);
  if(rmdir(dirname)<0) { /* Cannot remove directory. */
    return -1;
  }
  return 0;
}

/* recursively generate a directory structure */
void mkdirs(char *s)
{
  struct stat st;
  if(stat(s,&st)==-1) {
    if(strrchr(s,'/')!=NULL) {
      char buf[MAX_FNAME+1];
      mystrncpy(buf,s,sizeof(buf));
      *strrchr(buf,'/')=0; mkdirs(buf);
    }
    mkdir(s,S_IRWXU|S_IRWXG|S_IRWXO);
  }
}
