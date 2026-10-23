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

/* Interface gp to wims */

/* This program is now limited to pari version 2.??. */

/*************** Customization: change values hereafter ****************/
#include "common.h"

/* gp prompt string */
#define gpprompt "\n? "
/* This is the good bye string of gp, signaling end of output. */
#define goodbyestring "Good bye!"
/* This is PARI home page. To be kept up to date. */
#define homepage "http://pari.math.u-bordeaux.fr/"
/* String to tell the program to quit. */
char *quitstring="\nquit\n";
/* The way to print a string in the program. */
char *stringprinter="print(\"%s\")\n";
/* limit of input/output file sizes */
int fsizelim=131072;
int precision=28; /* default */

char *nameofcmd="gp -f -q";
char *gprcenv="GPRC";
char *gprc="../.gprc";
char header[]="default(output,0)\n\
default(secure,1)\n\
alias(ln,log)\n\
alias(pi,Pi)\n\
alias(euler,Euler)\n\
alias(PI,Pi)\n\
i=I\n\
e=exp(1)\n\
E=exp(1)\n\
sec(x)=1/cos(x);\n\
csc(x)=1/sin(x);\n\
lg(x)=log(x)/log(10);\n\
log2(x)=log(x)/log(10);\n\
alias(log10,lg)\n\
alias(sgn,sign)\n\
alias(ch,cosh)\n\
alias(sh,sinh)\n\
alias(th,tanh)\n\
alias(arccos,acos)\n\
alias(arcsin,asin)\n\
alias(tg,tan)\n\
alias(arctan,atan)\n\
alias(arctg,atan)\n\
alias(Argch,acosh)\n\
alias(Argsh,asinh)\n\
alias(Argth,atanh)\n\
alias(argch,acosh)\n\
alias(argsh,asinh)\n\
alias(argth,atanh)\n\
alias(cot,cotan)\n\
alias(ctg,cotan)\n\
alias(rint,round)\n\
alias(RANDOM,random)\n\
alias(SOLVE,solve)\n\
alias(parirandom,random)\n\
alias(parisolve,solve)\n\
";

struct {
    char *wname; char *defaultval; char *gpset;
} setups[]={
      {"w_pari_precision", "20", "\\p "},
      {"w_pari_serieslength", "8", "\\ps "}
};

/* names which are not allowed */
char *illegal[]={
};

int illegal_no=(sizeof(illegal)/sizeof(illegal[0]));

/* name parts which are not allowed */
char *illpart[]={
    "plot", "write", "help"
};

int illpart_no=(sizeof(illpart)/sizeof(illpart[0]));

/***************** Nothing should need change hereafter *****************/

char *progname="pari";

int pariray=0;

/* check for security violations in command string */
void check_parm(char *pm)
{
    char *p;
    for(p=pm;*p!=0;p++) {
/* Underscore replacement */
      if(*p=='_') {*p='K'; continue;}
/* no escape commands. */
      if(*p!='\n') continue;
      while(*p!=0 && isspace(*p)) p++;
      if(*p=='\\' && *(p+1)!='\\' && *(p+1)!='v' && *(p+1)!='p') *p='.';
      if(*p=='\\') p++;
    }
    find_illegal(pm);
}

/* process and print gp output */
void output(char *p)
{
    int i,n;
    char *pp, *pe, *pt;
    if(pariray) {puts(p); return;}
    pp=strstr(p,gpprompt); if(pp==NULL) return;
    pe=strstr(pp,goodbyestring);
    if(pe>=pp) *pe=0;
    while(pp!=NULL) {
      pp++;
      pe=strstr(pp,gpprompt);
      if(pe>=pp) *pe=0;
      pp=strchr(pp,'\n');
      if(pp==NULL) {
          emptyline:
          puts(""); pp=pe; continue;
      }
      pp++; n=strlen(pp);
      if(n==0) goto emptyline;
/* make every output one-line */
      for(i=0;i<n;i++) {
          if(*(pp+i)=='\n') {
            if(*(pp+i+1)!='%') *(pp+i)=' ';
            else {*(pp+i)=0; break;}
          }
      }
/* strip leading and trailing spaces */
      while(isspace(*pp) && pp<pe) pp++;
      pt=pp+strlen(pp)-1;
      while(isspace(*pt) && pt>pp) *pt--=0;
/* remove parentheses of matrix output */
      if(memcmp(pp,"Mat(",4)==0 && *pt==')' && find_matching2(pp+4,')')==pt) {
          *(pt--)=0; pp+=4;
      }
      if(memcmp(pp,"Vecsmall(",9)==0 && *pt==')' && find_matching2(pp+9,')')==pt) {
          *(pt--)=0; pp+=9;
      }
      if(*pp=='[' && *pt==']' && find_matching2(pp+1,']')==pt) {
          *(pt--)=0; pp++;
      }
      strip_zeros(pp);
      puts(pp); pp=pe;
    }
}

void about(void)
{
    char *p;

    prepabout("\\v\nquit\n",outputfname,NULL);
    if(readabout()>0) {
      p=strchr(aboutbuf,'\n'); if(p!=NULL) *p=0;
      strip_trailing_spaces2(aboutbuf);
      printf("<a target=\"wims_external\" href=\"%s\">%s</a>",homepage,aboutbuf);
    }
}

char *dynsetup(char *ptr, char *end)
{
    int i;
    char *p, *pp;
    if(wseed!= NULL) snprintf(ptr,end-ptr,"\nsetrand(%u)\n",atoi(wseed)&(0x7FFFFFFF));
    else snprintf(ptr,end-ptr,"\nsetrand(%u);\n",seed&(0x7FFFFFFF));
    ptr+=strlen(ptr);
    for(i=0;i<SETUP_NO;i++) {
      p=getenv(setups[i].wname);
      if(p!=NULL) for(pp=p;*pp;pp++) if(!isspace(*pp) && !isalnum(*pp)) p="";
      if(p==NULL || *p==0) p=setups[i].defaultval;
      snprintf(ptr,end-ptr,"%s%s\n",setups[i].gpset,p);
      ptr+=strlen(ptr);
      if(strstr(setups[i].wname,"pari_precision")!=NULL)
        precision=atoi(p);
      if(precision<0) precision=-precision;
    }
    if(pariray) snprintf(ptr,end-ptr,"\\e0\n");
    else snprintf(ptr,end-ptr,"\\e1\n");
    ptr+=strlen(ptr);
    return ptr;
}

int main(int argc,char *argv[])
{
    char *p;

    p=getenv("pari_ray");
    if(p!=NULL && *p!=0) {pariray=1; fsizelim=4*1024*1024;}
    setenv(gprcenv,gprc,1);
    prepare1();
    setenv("GPTMPDIR",tmp_dir,1);
    run();
    return 0;
}

