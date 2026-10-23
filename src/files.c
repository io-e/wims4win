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

#include "wims.h"

void secure_exec(void);

int fwrite_calls=0; /* number of files */
int fwrite_sizes=0; /* total size */

/* write to a writable file. */
void _filewrite(char *prefix, char *fname, char *content, char *type)
{
  char *m, buf[MAX_LINELEN+1];
  FILE *f;
  int n,checklink;
  struct stat stl;
  checklink=0;
  if(strstr(fname,parent_dir_string)!=NULL){
    setvar(error_data_string,fname);
   module_error("illegal_fname"); return;
  }
  m=getvar(ro_name[ro_module]);
  if(m==NULL || *m==0) return;
  if(strncmp(fname,"TEMP_",5)==0 && strchr(fname,'/')==NULL &&
     strstr(session_prefix,"robot")==NULL) {
    mystrncpy(buf,tmp_dir,sizeof(buf));
    goto add;
  }
  if(strncmp(fname,"getfile/",strlen("getfile/"))==0) {
    if(strchr(fname+strlen("getfile/"),'/')!=NULL) {
denied:
      setvar(error_data_string,fname);
      module_error("file_access_denied"); return;
    }
    fname+=strlen("getfile/");
    snprintf(buf,sizeof(buf),"%s/getfile",session_prefix);
    mkdirs(buf);
    checklink=1; goto add;
  }
  if(trusted_module() && !is_class_module && strncmp(fname,"wimshome/",9)==0) {
    mystrncpy(buf,getvar("wims_home"),sizeof(buf));
    fname+=9; goto add;
  }
  if(strncmp(m,"adm/",4)==0 || strcmp(m,home_module)==0) {
    mystrncpy(buf,prefix,sizeof(buf));
  }
  else {
    if(!trusted_module() && strchr(fname,'/')!=NULL) return; /* silent */
      snprintf(buf,sizeof(buf),"w/%s",prefix);
      mkdirs(buf);
  }
  add: snprintf(buf+strlen(buf),sizeof(buf)-strlen(buf),"/%s",fname);
  if(!trusted_module() || is_class_module) {
    if(fwrite_calls>=MAX_FWRITE) goto denied;
    fwrite_calls++;
    n=strlen(content)+1;
    if(fwrite_sizes+n>MAX_FWRITE_SIZE) goto denied;
    fwrite_sizes+=n;
  }
  if(checklink && lstat(buf,&stl)==0 && S_ISLNK(stl.st_mode))
    goto denied;
  lastdatafile[0]=lastftest[0]=0;
  f=fopen(buf,type); if(f==NULL) return;
  fprintf(f,"%s\n",content);
  fclose(f);
}

/* write to a file in module */
void filewrite(char *p)
{
  char *p1, *p2;
  secure_exec();
  p1=find_word_start(p);
  p2=find_word_end(p1);
  if(*p1==0) {*p=0;return;}
  if(*p2!=0) *p2++=0;
  _filewrite(module_prefix,p1,p2,"w");
  *p=0;
}

/* append to a file in module */
void fileappend(char *p)
{
  char *p1, *p2;
  secure_exec();
  p1=find_word_start(p);
  p2=find_word_end(p1);
  if(*p1==0) {*p=0;return;}
  if(*p2!=0) *p2++=0;
  _filewrite(module_prefix,p1,p2,"a");
  *p=0;
}
