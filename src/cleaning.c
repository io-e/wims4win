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

/* Only miscellaneous cleanup routines remain.
 * The regular session cleaner has been moved to wimslogd.
 */

#include "wims.h"

void cleantmpdir(void)
{
  char buf[MAX_LINELEN+1], *p1, *p2;

  accessfile(buf,"r","%s/exec.pid",tmp_dir);
  for(p1=find_word_start(buf); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    kill(atoi(p1),SIGKILL);
  }
  if(strstr(tmp_dir,"sessions/")!=NULL &&
     (strstr(tmp_debug,"yes")==NULL || checkhost(manager_site)<1)) {
    if(remove_tree(tmp_dir)) {
      call_ssh("bin/ch..root cleantmpdir 2>/dev/null");
      remove_tree(tmp_dir);
    }
  }
  else {
    mkfname(buf,"%s/exec.pid",tmp_dir);
    unlink(buf);
    if(strstr(tmp_dir,"sessions/")!=NULL) {
      chmod(tmp_dir,S_IRUSR|S_IWUSR|S_IXUSR);
    }
  }
}

/* remove pid tag */
void delete_pid(void)
{
  char buf[MAX_FNAME+1],pbuf[32];

  flushlog(); flushoutput();
  if(robot_access || *session_prefix==0) return;
  cleantmpdir();
  mkfname(buf,"%s/.pid",s2_prefix);
  readfile(buf,pbuf,sizeof(pbuf));
  if(pbuf[0] && strcmp(pbuf,pidbuf)==0) unlink(buf);
  flushlog();
}
