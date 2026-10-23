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

/* Interface Scilab to wims */

/*************** Customization: change values hereafter ****************/
#include "common.h"

/* limit of input/output file sizes */
int fsizelim=131072;
/* This string tells scilab to exit. */
char *quitstring="\nquit\n";
/* The way to print a string in the program. */
char *stringprinter="\"%s\"\n";
/* This is scilab home page. To be kept up to date. */
#define homepage "http://www.scilab.org/"
/* String to search for answers */

// old chroot:  char ans_str[]="\n-->";
char ans_str[]="\n";

char *nameofcmd="scilab -nw -ns";
int precision=9;
char header[]="\
";

struct {
    char *wname; char *defaultval; char *setname;
} setups[]={
      {"w_scilab_precision", "9", "output_precision"}
};

/* names which are not allowed */
char *illegal[]={
};

int illegal_no=(sizeof(illegal)/sizeof(illegal[0]));

/* name parts which are not allowed */
char *illpart[]={
};

int illpart_no=(sizeof(illpart)/sizeof(illpart[0]));

/***************** Nothing should need change hereafter *****************/

char *progname="scilab";

/* check for security violations in command string */
void check_parm(char *p)
{
    char *pp, *s;
/* Underscore replacement */
    for(pp=strchr(p,'_'); pp!=NULL; pp=strchr(pp+1,'_')) {
      if(pp==p || !isalnum(*(pp-1))) *pp='K';
    }
    for(s=p;*s;s++) *s=tolower(*s);
    find_illegal(p);
}

/* process and print Scilab output */
void output(char *p)
{
    int i,n;
    char *pp, *pe, *pt;

    for(pp=strstr(p,ans_str); pp; pp=pe) {
      pp+=strlen(ans_str); pe=strstr(pp,ans_str);
      if(pe) *pe=0;
      pp=find_word_start(pp); if(memcmp(pp,"ans  =",strlen("ans  ="))==0) {
          pp+=strlen("ans  ="); pp=find_word_start(pp);
      }
      if(pe!=NULL && pp>=pe) {
          emptyline:
          puts(""); continue;
      }
      n=strlen(pp); if(n==0) goto emptyline;
/* strip leading and trailing spaces */
      while(isspace(*pp) && pp<pe) pp++;
      pt=pp+strlen(pp)-1;
      while(isspace(*pt) && pt>pp) *pt--=0;
/* make every output one-line */
      for(i=0;i<n;i++) {
          if(*(pp+i)=='\n') *(pp+i)=';';
      }
/* strip_zeros(pp); */
      puts(pp);
    }
}

void about(void)
{
    char *p;

    prepabout(quitstring,outputfname,NULL);
    if(readabout()>0) {
      p=strchr(aboutbuf,'\n'); if(p!=NULL) *p=0;
      p=strchr(aboutbuf,'('); if(p!=NULL) *p=0;
      strip_trailing_spaces2(aboutbuf);
      printf("<a target=\"wims_external\" href=\"%s\">%s</a>",homepage,aboutbuf);
    }
}

char *dynsetup(char *ptr, char *end)
{
    int i;
    char *p, *pp;
/* to adapt
    if(wseed!= NULL) {
      snprintf(ptr,end-ptr,"\nwimsseed:make_random_state(%u);\nset_random_state(wimsseed);\n",atoi(wseed)&(0x7FFFFFFF));
      ptr+=strlen(ptr);
    }
*/
    for(i=0;i<SETUP_NO;i++) {
      p=getenv(setups[i].wname);
      if(p!=NULL) for(pp=p;*pp;pp++) if(!isspace(*pp) && !isalnum(*pp)) p="";
      if(p==NULL || *p==0) p=setups[i].defaultval;
      snprintf(ptr,end-ptr,"%s=%s\n",setups[i].setname,p);
      ptr+=strlen(ptr);
      if(strstr(setups[i].wname,"scilab_precision")!=NULL)
        precision=atoi(p);
      if(precision<0) precision=-precision;
    }
    return ptr;
}

int main(int argc,char *argv[])
{
    setenv("chroot","must",1);
    setenv("sysmask","must",1);
    must_chroot=1;
    prepare1();
    run();
    return 0;
}

