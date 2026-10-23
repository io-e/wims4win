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

/* Wrap uid and exec */

#include <unistd.h>
#include <string.h>

int main(int argc,char *argv[])
{
  char *args[1024];
  int i, uid1, uid2, gid1, gid2;

  if(argc<2) return 0;
  for(i=0;i<1000 && i<argc; i++) args[i]=argv[i+1];
  args[i]=NULL;
  uid1=geteuid(); uid2=getuid(); gid1=getegid(); gid2=getgid();
  if(strchr(args[0],'/')) {
    if(setreuid(uid1,uid2) || setregid(gid1,gid2)) return 127;
    execv(args[0],args);
  }
  else {
    if(setreuid(uid1,uid1) || setregid(gid1,gid1)) return 127;
    execvp(args[0],args);
  }
  return 127;
}

