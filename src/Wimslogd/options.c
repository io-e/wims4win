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

/* daemon option processing */
#include "wimslogd.h"

char opt_class[MAX_CLASSLEN+1];
char opt_module[MAX_FNAME+1];
char opt_session[MAX_CLASSLEN+1];
char opt_user[MAX_FNAME+1];

/* returns -1 if error */
int options(void)
{
  int i;
  char c, *p1, *p2, *p3;
  opt_class[0]=opt_module[0]=opt_session[0]=opt_user[0]=0;
  for(i=0, p1=find_word_start(textptr); *p1=='-'; p1=find_word_start(p2),i++) {
    p1++; c=*p1++; p1=find_word_start(p1);
    p2=find_word_end(p1); if(*p2) *p2++=0;
    switch(c) {
      case 'c': {                  /* class */
        if(cwdtype!=dir_home) {
          sockerror(2,"option_conflict"); return -1;
        }
        if(strlen(p1)>=MAX_CLASSLEN || !myisdigit(*p1) || strstr(p1,"..")!=NULL) {
          sockerror(2,"illegal_fname %s",p1);
          return -1;
        }
        (void)chdir(classd); if(chdir(p1)<0) {
          sockerror(2,"bad_class %s",p1);
          return -1;
        }
        mystrncpy(opt_class,p1,sizeof(opt_class));
        cwdtype=dir_class; break;
      }
      case 'm': {                  /* module */
        if(cwdtype!=dir_home) {
          sockerror(2,"option_conflict"); return -1;
        }
        if(strlen(p1)>MAX_FNAME || *p1=='/' || strstr(p1,"..")!=NULL) {
          sockerror(2,"illegal_fname %s",p1);
          return -1;
        }
        (void)chdir(modd); if(chdir(p1)<0) {
          sockerror(2,"bad_module:%s",p1);
          return -1;
        }
        mystrncpy(opt_module,p1,sizeof(opt_module));
        cwdtype=dir_module; break;
      }
      case 's': {                  /* session */
        if(cwdtype!=dir_home) {
          sockerror(2,"option_conflict"); return -1;
        }
        if(strlen(p1)>=MAX_CLASSLEN || *p1=='/' || strstr(p1,"..")!=NULL) {
          sockerror(2,"illegal_fname:%s",p1);
          return -1;
        }
        (void)chdir(sesd); if(chdir(p1)<0) {
          sockerror(2,"bad_session:%s",p1);
          return -1;
        }
        mystrncpy(opt_session,p1,sizeof(opt_session));
        cwdtype=dir_session; break;
      }
      case 'u': {                  /* user definition */
        if(strchr(p1,'/')!=NULL) {
          sockerror(2,"illegal_fname %s",p1);
          return -1;
        }
        mystrncpy(opt_user,p1,sizeof(opt_user));
        for(p3=strchr(opt_user,'.'); p3!=NULL; p3=strchr(p3,'.')) *p3++='@';
        break;
      }
      default: {
        sockerror(2,"bad_option"); return -1;
      }
    }
  }
  textptr=p1; return i;
}
