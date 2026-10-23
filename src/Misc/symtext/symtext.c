/*    Copyright (C) 2002-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
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

/* This program makes comparison between two text strings,
 * according to the symtext syntax. */

/* Input data: via environment variables.
 * wims_exec_parm: line 1 = command (comp,expand,wordlist,random,1,2,3,...)
 * line 2 = text to examine (for comp).
 * line 3 and up = symtext syntax.
 * w_symtext: dictionary style.
 * w_symtext_option: option words.
 *
 * Output: two lines.
 * Line 1: ERROR or OK
 * Line 2: result depending on command.
 */


const char *codechar="_0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

#include "symtext.h"

struct block blockbuf[MAX_BLOCKS];
int nextblock;
listtype listbuf[MAX_LISTS];
int nextlist;
listtype tagbuf[MAX_BLOCKS];
int nexttag;

struct poolstruct poolbuf[MAX_POOLS];
int nextpool;

int options;

char cmdbuf[256], stbuf[MAX_LINELEN+1], textbuf[MAX_LINELEN+1];
char wbuf[MAX_LINELEN+1];
char cmdparm[1024];
char defbuf[MAX_LINELEN+1];
char style[MAX_NAMELEN+1];
char styledir[MAX_FNAME+1];
char optionbuf[1024];
char outbuf[OUTSIZE];
char *outptr, *wptr;
int debug;

enum {
    cmd_none, cmd_comp, cmd_debug, cmd_random, cmd_1, cmd_wordlist
};
struct {
    char *name; int value;
} cmdlist[]={
    {"1",       cmd_1},
    {"comp",    cmd_comp},
    {"compare", cmd_comp},
    {"debug",   cmd_debug},
    {"match",   cmd_comp},
    {"rand",    cmd_random},
    {"random",  cmd_random},
    {"wordlist",cmd_wordlist},
    {"words",   cmd_wordlist}
};
#define cmdcnt (sizeof(cmdlist)/sizeof(cmdlist[0]))
int cmd;

void sym_error(char *msg,...)
{
    va_list vp;
    char buf[1024];

    va_start(vp,msg);
    vsnprintf(buf,sizeof(buf),msg,vp);
    va_end(vp);
    printf("ERROR\n%s\n",buf);
    exit(1);
}

/* read-in a file into buffer. Use open() and read().
 * Return buffer address which will be malloc'ed if buf=NULL.
 */
char *readfile(char *fname, char buf[], long int buflen)
{
    int fd, t;
    struct stat st;
    long int l, lc;
    char *bf;
    t=0; if(buf) buf[0]=0;
    if(stat(fname,&st)) return NULL;
    l=st.st_size; if(l<0) return NULL;
    if(l>=buflen) {
     if(buflen<MAX_LINELEN) l=buflen-1;
     else sym_error("file_too_long %s",fname);
    }
    fd=open(fname,O_RDONLY); if(fd==-1) return NULL;
    if(buf==NULL) bf=xmalloc(l+8); else {bf=buf;if(l==0) {t=1; l=buflen-1;}}
    lc=read(fd,bf,l); close(fd);
    if(lc<0 || lc>l || (lc!=l && t==0))
      {if(buf==NULL) free(bf); else buf[0]=0; return NULL;}
    bf[lc]=0; _tolinux(bf); return bf;
}

/* get option word in a string */
void _getopt(char *name, char *p)
{
    char *p1, *p2, *p3, *p4;
    char buf[MAX_LINELEN+1];

    snprintf(buf,sizeof(buf),"%s",p);
    p1=find_word_start(name);
    for(p2=buf;*p2;p2++) {
      if(myisspace(*p2)) *p2=' ';
      if(*p2=='=') *p2='	';
    }
    *p=0;
    p2=wordchr(buf,p1); if(p2==NULL) return;
    for(p3=find_word_end(p2);myisspace(*p3);p3++) {
      if(*p3=='	') {
         p3=find_word_start(p3);
         switch(*p3) {
            case '"': {
              p4=strchr(p3+1,'"');
              goto tested;
            }
            case '(': {
              p4=find_matching(p3+1,')');
              goto tested;
            }
            case '[': {
              p4=find_matching(p3+1,']');
              goto tested;
            }
            case '{': {
              p4=find_matching(p3+1,'}');
              tested:
              if(p4) {
                p3++; *p4=0; break;
              }
              else goto nomatch;
          }
          default: {
              nomatch:
              *find_word_end(p3)=0;
          }
         }
         mystrncpy(p,p3,MAX_LINELEN);
         return;
       }
    }
    *find_word_end(p2)=0;
    memmove(p,p2,strlen(p2)+1);
}

void _getdef(char buf[], char *name, char value[])
{
    char *p1, *p2, *p3, *p4;

    if(*name==0) goto nothing;     /* this would create segfault. */
    for(p1=strstr(buf,name); p1!=NULL; p1=strstr(p1+1,name)) {
     p2=find_word_start(p1+strlen(name));
     if((p1>buf && !isspace(*(p1-1))) || *p2!='=') continue;
     p3=p1; while(p3>buf && *(p3-1)!='\n') p3--;
     p3=find_word_start(p3);
     if(p3<p1 && *p3!='!') continue;
     if(p3<p1) {
         p3++; p4=find_word_end(p3);
         if(find_word_start(p4)!=p1) continue;
         if(p4-p3!=3 || (strncmp(p3,"set",3)!=0 &&
            strncmp(p3,"let",3)!=0 &&
            strncmp(p3,"def",3)!=0)) {
           if(p4-p3!=6 || strncmp(p3,"define",6)!=0) continue;
         }
     }
     p2++;p3=strchr(p2,'\n'); if(p3==NULL) p3=p2+strlen(p2);
     p2=find_word_start(p2);
     if(p2>p3) goto nothing;
     if(p3-p2>=MAX_LINELEN) sym_error("string_too_long def %s",name);
     memmove(value,p2,p3-p2); value[p3-p2]=0;
     strip_trailing_spaces(value); return;
    }
nothing:
    value[0]=0; return;
}

char fnbuf[MAX_FNAME+1];

/* make a filename and check length */
char *mkfname(char buf[], char *s,...)
{
    va_list vp;
    char *p;

    if(buf==NULL) p=fnbuf; else p=buf;
    va_start(vp,s);
    vsnprintf(p,MAX_FNAME,s,vp);
    va_end(vp);
    if(strlen(p)>=MAX_FNAME-1) sym_error("name_too_long %.20s",p);
    return p;
}

void getparms(void)
{
    char *p, *p2, *p3, lbuf[8];
    char buf[MAX_LINELEN+1], pbuf[MAX_LINELEN+1];
    struct stat st;
    int i;

    cmd=0;
    p=getenv("wims_exec_parm");
    if(p==NULL) return;
    snprintf(pbuf,sizeof(pbuf),"%s",p);
    rows2lines(pbuf);
    p2=strchr(pbuf,'\n'); if(p2==NULL) return; else *p2++=0;
    p=find_word_start(pbuf);
    p3=find_word_end(p); if(p3-p>=sizeof(cmdbuf)) return;
    if(*p==0) return; else *p3++=0;
    memmove(cmdbuf,p,p3-p); cmdbuf[p3-p]=0;
    p=p2; p2=strchr(p,'\n'); if(p2==NULL) p2=p+strlen(p); else *p2++=0;
    if(p2<=find_word_start(p)) return;
    if(p2-p<sizeof(textbuf)) {
       memmove(textbuf,p,p2-p); textbuf[p2-p]=0;
    }
    p=p2; p2=p+strlen(p);
    if(p2>p && p2-p<sizeof(stbuf)) {
     memmove(stbuf,p,p2-p); stbuf[p2-p]=0;
    }
    i=search_list(cmdlist,cmdcnt,sizeof(cmdlist[0]),cmdbuf);
    if(i>=0) cmd=cmdlist[i].value;
    else sym_error("bad_command %.20s",cmdbuf);
    snprintf(cmdparm,sizeof(cmdparm),"%s",p2);

    options=0;
    p=getenv("w_module_language"); if(p==NULL) p="";
    snprintf(lbuf,sizeof(lbuf),"%2s",p);
    if(*p3) {
      snprintf(buf,sizeof(buf),"%s",p3);
      _getopt("style",buf);
      snprintf(style,sizeof(style),"%s",find_word_start(buf));
      *find_word_end(style)=0;
      snprintf(buf,sizeof(buf),"%s",p3);
      _getopt("language",buf);
      if(buf[0]) snprintf(lbuf,sizeof(lbuf),"%2s",buf);
    }
    lbuf[2]=0;
    if(!myisalpha(lbuf[0]) || !myisalpha(lbuf[1])) ovlstrcpy(lbuf,"en");
    styledir[0]=defbuf[0]=optionbuf[0]=buf[0]=0;
    if(*style) {
      p=getenv("module_dir");
      if(p==NULL) {               /* non-wims operation */
          snprintf(styledir,sizeof(styledir),"%s",style);
      }
      else {
          for(i=0;i<MAX_NAMELEN && myisalnum(style[i]);i++);
          style[i]=0;
          if(style[0]) {            /* style defined */
             if(*p!='/' && strstr(p,"..")==NULL) {      /* check module dir */
                snprintf(styledir,sizeof(styledir),"%s/symtext/%s/%s/def",p,lbuf,style);
                if(stat(styledir,&st)) styledir[0]=0;
             }
             if(styledir[0]==0) {      /* check default */
                snprintf(styledir,sizeof(styledir),"%s/symtext/%s/%s/def",defaultdir,lbuf,style);
                if(stat(styledir,&st)) sym_error("style_not_found %s",style);
             }
          }
      }
      if(styledir[0]) {            /* get def */
          readfile(styledir,defbuf,sizeof(defbuf));
          styledir[strlen(styledir)-4]=0;
          suffix_dic(mkfname(NULL,"%s/suffix",styledir));
          transdic=diccnt;
          if(prepare_dic("trans")==NULL) transdic=-1;
          dic[transdic].unknown_type=unk_leave;
          macrodic=diccnt;
          if(prepare_dic("macros")==NULL) macrodic=-1;
          dic[macrodic].unknown_type=unk_delete;
      }
    }
    _getdef(defbuf,"option",buf);
    snprintf(optionbuf,sizeof(optionbuf),"%s %s",p3,buf);
    if(wordchr(optionbuf,"nocase")!=NULL) options|=op_nocase;
    if(wordchr(optionbuf,"deaccent")!=NULL) options|=op_deaccent;
    if(wordchr(optionbuf,"reaccent")!=NULL) options|=op_reaccent;
    if(wordchr(optionbuf,"nopunct")!=NULL) options|=op_nopunct;
    if(wordchr(optionbuf,"nomath")!=NULL) options|=op_nomath;
    if(wordchr(optionbuf,"noparenthesis")!=NULL) options|=op_noparenth;
    if(wordchr(optionbuf,"noparentheses")!=NULL) options|=op_noparenth;
    if(wordchr(optionbuf,"nocs")!=NULL) options|=op_nocs;
    if(wordchr(optionbuf,"noquote")!=NULL) options|=op_noquote;
    if(wordchr(optionbuf,"matchall")!=NULL) options|=op_matchall;
    if(wordchr(optionbuf,"abconly")!=NULL) options|=op_alphaonly;
    if(wordchr(optionbuf,"onlyabc")!=NULL) options|=op_alphaonly;
    if(wordchr(optionbuf,"alnumonly")!=NULL) options|=op_alnumonly;
    if(wordchr(optionbuf,"onlyalnum")!=NULL) options|=op_alnumonly;

    if(cmd==cmd_comp || cmd==cmd_debug) {
      _getopt("debug",optionbuf);
      if(optionbuf[0]) {
          i=atoi(optionbuf);
          if(i>0 || strcmp(optionbuf,"0")==0) debug=i; else debug=1;
          if(debug>0) cmd=cmd_debug;
      }
    }
    strip_enclosing_par(textbuf);
    strfold(textbuf);
}

int verify_tables(void)
{
    if(verify_order(builtin,builtincnt,sizeof(builtin[0]))) return -1;
    if(verify_order(cmdlist,cmdcnt,sizeof(cmdlist[0]))) return -1;

    return 0;
}

int main(int argc, char *argv[])
{
    int i, n, mat;
    char *p1, *p2;
    char lbuf[MAX_LINELEN+1];

    if(argc>1 && strcmp(argv[1],"-t")==0) {
      if(verify_tables()==0) {
         printf("Table orders OK.\n");
         return 0;
      }
      else return 1;
    }
    debug=0;
    wptr=wbuf; wbuf[0]=0;
    getparms();
    Mnext=Mbuf; Mcnt=0;
    switch(cmd) {
      case cmd_comp: {
         comp:
         n=linenum(stbuf);
         for(mat=0,i=1,p1=stbuf;i<=n;i++,p1=p2) {
            p2=find_line_end(p1); if(*p2) *p2++=0;
            p1=find_word_start(p1);
            if(*p1==0) continue;
            snprintf(lbuf,sizeof(lbuf),"%s",p1);
            compile(lbuf);
            mat=match(textbuf);
            if(mat) {
                printf("MATCH %d %s\n",i,outbuf);
                if((options&op_matchall)==0) break;
            }
          }
          if(debug) fprintf(stderr,"word list: %s\n",wbuf);
          break;
      }
      case cmd_debug: {
          if(debug==0) debug=1;
          fprintf(stderr,"debug=%d.\n",debug);
          for(i=0;i<diccnt;i++)
            fprintf(stderr,"Dictionary %d: %s, %d entries.\n",
                  i+1,dic[i].name,dic[i].len);
          goto comp;
      }
      case cmd_random: {

          break;
      }
      case cmd_wordlist: {

          break;
      }
      case cmd_1: {

          break;
      }

      case cmd_none:
      default: return 1;
    }
    return 0;
}

