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

/* Common routines in interfaces */
#include "common.h"
#include "../Platform/wims_platform.h"
#define ch_root "bin/ch..root"

int mypid;
int must_chroot=0;
char inputfname[256], outputfname[256];
char pidfname[256];
char parmbuf[parmlim+16];
char *inpf, *outpf, *errorf;
char *parm;
char *tmp_dir;
char cmdbuf[1024];
char inputbuf[inputlim];
char *inputptr, *inputend;
char stdinbuf[MAX_LINELEN+1];
char *cmdparm;
int isabout=0;
char *multiexec_random;
int multiexec=0, mxpid, notfirst;
wp_chan chan;      /* parent<->CAS channel (was pipe_in/pipe_out) */
wp_pid mxproc;     /* running CAS process */
int debug=0;
FILE *goin;
unsigned int seed; /* random seed value */
char *wseed;
char aboutbuf[aboutlen];
/* this is only a default. It should usually be reset. */
char *tmpdir="/tmp";
char startstring[256], endstring[256];
char *obuf;

void check_parm(char *p);
void output(char *p);
void about(void);
char *dynsetup(char *p, char *end);

/* Find first occurrence of word */
char *wordchr2(char *p, char *w)
{
    char *r;

    if(*w==0) return NULL;
    for(r=strstr(p,w);r!=NULL &&
      ( (r>p && !isspace(*(r-1))) || (!isspace(*(r+strlen(w))) && *(r+strlen(w))!=0) );
      r=strstr(r+1,w));
    return r;
}

/* find matching parenthesis */
char *find_matching2(char *p, char c)
{
    char *pp;
    int parenth, brak, brace;
    parenth=brak=brace=0;
    for(pp=p; *pp!=0; pp++) {
      switch(*pp) {
          case '[': brak++; break;
          case ']': brak--; break;
          case '(': parenth++; break;
          case ')': parenth--; break;
          case '{': brace++; break;
          case '}': brace--; break;
          default: continue;
      }
      if(parenth<0 || brak<0 || brace<0) {
          if(*pp!=c || parenth>0 || brak>0 || brace>0) return NULL;
          else break;
      }
    }
    if(*pp!=c) return NULL;
    return pp;
}

#ifndef _WIN32 /* only used by the exec.pid bookkeeping */
/* Read/write to a file with variable parms to print filename */
/* same in Misc/ccsum.c and wlogdaccessfile */
static
void accessfile(char *content, char *type, char *s,...)
{
    va_list vp;
    char buf[MAX_LINELEN+1];
    FILE *f;
    int l;

    va_start(vp,s);
    vsnprintf(buf,sizeof(buf),s,vp);
    va_end(vp);
    f=fopen(buf,type); if(f==NULL) {
      if(*type=='r') content[0]=0;
      return;
    }
    switch(*type) {
      case 'a':
      case 'w': {
          l=strlen(content); fwrite(content,1,l,f); break;
      }
      case 'r': {
          l=fread(content,1,MAX_LINELEN-1,f);
          if(l>0 && l<MAX_LINELEN) content[l]=0;
          else content[0]=0;
          break;
      }
      default: {
          content[0]=0; break;
      }
    }
    fclose(f);
}
#endif

void getstdin(void)
{
    char *p;
    int i,j,k;
    i=0; k=MAX_LINELEN; stdinbuf[0]=0;
    do {
      j=read(0,stdinbuf+i,k);
      if(j<0 || j>k) exit(1);
      i+=j; k-=j; stdinbuf[i]=0;
      p=strstr(stdinbuf,multiexec_random);
      if(p) {*p=0; break;}
    }
    while(k>0);
}

#ifdef _WIN32
/* Win32: no exec.pid. Every CAS runs in the Job Object named after tmp_dir
 * (wp_set_job in prepare1); wims kills a session with wp_job_kill(). */
void addpid(int pid) { (void)pid; }
void rmpid(int pid) { (void)pid; }
#else
/* add a pid to the list of running childs */
void addpid(int pid)
{
    char buf[MAX_LINELEN+1], pidbuf[32];
    int l;
    snprintf(pidbuf,sizeof(pidbuf),"%u",pid);
    accessfile(buf,"r",pidfname); l=strlen(buf);
    if(l>=MAX_LINELEN-64) return;
    if(wordchr2(buf,pidbuf)==NULL) {
      snprintf(buf+l,sizeof(buf)-l," %s",pidbuf);
      accessfile(buf,"w",pidfname);
    }
}

/* Remove a pidname to the list of running childs */
void rmpid(int pid)
{
    char buf[MAX_LINELEN+1], pidbuf[32], *p;
    snprintf(pidbuf,sizeof(pidbuf),"%u",pid);
    accessfile(buf,"r",pidfname);
    p=wordchr2(buf,pidbuf);
    if(p!=NULL) {
      ovlstrcpy(p,find_word_start(find_word_end(p)));
      accessfile(buf,"w",pidfname);
    }
}
#endif

int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *args)
{
    int i, r;
    struct stat st;
    char *cm, *p, *p2, abuf[MAX_LINELEN+1];
    char *arg[1024];
    wp_isolation iso;

    for(cm=cmdf; isspace(*cm); cm++){};
    if(*cmdf==0) return -1;
    snprintf(abuf,sizeof(abuf),"%s",find_word_start(args));
    if(stat("../chroot/tmp/sessions/.chroot",&st)==0 || must_chroot) {
      arg[0]=ch_root; i=1; iso=WP_ISO_WRAPPER;
    }
    else {
      i=0; iso=WP_ISO_DROP_PRIV;
    }
    arg[i++]=cmdf;
    for(p=abuf; *p && i<1000; i++, p=find_word_start(p2))
      if (*p=='\'')
       {arg[i]=p2=++p; while(*p2 && *p2!='\'') p2++; if(*p2) *p2++=0;}
      else
       { arg[i]=p; p2=find_word_end(p); if(*p2) *p2++=0; }
    arg[i]=NULL;
    r=wp_spawn(&chan,arg,inf,outf,errf,iso,progname,&mxproc);
    if(r==WP_E_NOISOLATION) {
      fprintf(stderr,"%s: isolation unavailable on this platform, refused.\n",progname);
      return -1;
    }
    if(r!=WP_OK) return -1;
    /* parent */
    addpid(mxproc.id); mxpid=mxproc.id;
    if(outf) {
      wp_chan_close_parent_write(&chan);
      if(wp_wait(&mxproc,NULL)) {
        rmpid(mxproc.id); mxpid=-1;
      }
    }
    return 0;
}

/* verify illegal strings in parms. */
void find_illegal(char *p)
{
    char *pp, *pe;
    int i, l;
    for(pp=p;*pp;pp++) {
      if((*pp<' ' && *pp!='\n' && *pp!='	') || *pp>=127) *pp=' ';
    }
    for(i=0;i<illpart_no;i++) {
      pe=illpart[i]; l=strlen(pe);
      for(pp=strstr(p,pe); pp; pp=strstr(pp+1,pe)) {
          if(!isupper(pe[0]) || !islower(*(pp+l))) {
            if(pp[1]!='j' && pp[1]!='J') pp[1]='J';
            else pp[1]='Z';
          }
      }
    }
    for(i=0;i<illegal_no;i++) {
      pe=illegal[i]; l=strlen(pe);
      for(pp=strstr(p,pe); pp; pp=strstr(pp+1,pe)) {
          if((pp==p || !isalnum(*(pp-1))) && !isalnum(*(pp+l))) {
            if(pp[1]!='j' && pp[1]!='J') pp[1]='J';
            else pp[1]='Z';
          }
      }
    }
}

/* strip trailing zeros */
void strip_zeros(char *p)
{
    char *pp, *p2, *numend, *ee;
    int i;
    for(pp=p;*pp!=0;pp++) {
      if(!isdigit(*pp)) continue;
      i=0;
      for(numend=pp;isdigit(*numend) || *numend=='.';numend++)
        if(*numend=='.') i=1;
      if(i==0) {
          pp=numend-1;continue;
      }
      for(p2=numend;p2>pp && *(p2-1)=='0';p2--);
      for(ee=numend;isspace(*ee);ee++);
      if(*(pp+1)=='.' && (*ee=='E' || *ee=='e')
         && *(ee+1)=='-') {
          int k=0;
          char *pt=ee+2;
          while(isdigit(*pt)) {
            k*=10;k+=*pt-'0';pt++;
          }
          if(precision>8 && (k>precision*2 || (k>precision && *pp=='0'))) {

            sprintf(pp,"0.0%s",pt);

            pp+=strlen("0.0")-1;
            continue;
          }
      }

      if(*(p2-1)=='.' && p2<numend) p2++;

      if(p2<numend) {
          ovlstrcpy(p2,numend);numend=p2;
      }
      pp=numend-1;
    }
}

char *hname[]={"", "_1","_2","_3","_4","_5","_6","_7","_8"};
#define hnameno (sizeof(hname)/sizeof(hname[0]))

void putheader(void)
{
    char hbuf[64];
    char *p;
    int i;

    inputptr=dynsetup(inputptr,inputend);
    snprintf(inputptr,inputend-inputptr,"%s",header);
    inputptr+=strlen(inputptr);
    for(i=0;i<hnameno;i++) {
      snprintf(hbuf,sizeof(hbuf),"w_%s_header%s",progname,hname[i]);
      p=getenv(hbuf); if(p!=NULL && *p!=0) {
          snprintf(parmbuf,parmlim,"%s",p);
          check_parm(parmbuf);
          snprintf(inputptr,inputend-inputptr,"%s\n",parmbuf);
          inputptr+=strlen(inputptr);
      }
    }
}

void putparm(void)
{
    snprintf(parmbuf,parmlim,"%s",parm); check_parm(parmbuf);
    snprintf(inputptr,inputend-inputptr,stringprinter,startstring);
    inputptr+=strlen(inputptr);
    snprintf(inputptr,inputend-inputptr,"%s",parmbuf);
    inputptr+=strlen(inputptr);
    if(inputptr<inputend && inputptr>inputbuf && inputptr[-1]!='\n')
      *inputptr++='\n';
    snprintf(inputptr,inputend-inputptr,stringprinter,endstring);
    inputptr+=strlen(inputptr);
    *inputptr=0;
}

/* general first preparation */
void prepare1(void)
{
    char *p, buf[256];
    int i;
    unsigned int r[4];
    struct timeval t;

#ifdef _WIN32
    wp_stdio_binary();
#endif
    parm=getenv("wims_exec_parm");
/* nothing to do if no calling parameter */
    if(parm==NULL || *parm==0) exit(0);
    inputptr=inputbuf; inputend=inputbuf+inputlim-1;
    multiexec_random=getenv("multiexec_random");
    if(multiexec_random==NULL) multiexec_random="";
    if(*parm && strcmp(parm,multiexec_random)==0) {
      multiexec=1;
      getstdin(); parm=stdinbuf;
      if(parm[0]==0) exit(0);
    }
    if(wp_chan_open(&chan)!=WP_OK) {
      fprintf(stderr,"%s: unable to create pipe.\n",progname);
      exit(1);
    }
/*    i=fcntl(pipe_in[1],F_GETFL); if(i>=0) fcntl(pipe_in[1],F_SETFL,i|O_NONBLOCK);
    i=fcntl(pipe_out[0],F_GETFL); if(i>=0) fcntl(pipe_out[0],F_SETFL,i|O_NONBLOCK);
*/
    tmp_dir=getenv("tmp_dir"); if(tmp_dir==NULL || *tmp_dir==0) tmp_dir=tmpdir;
#ifdef _WIN32
    { /* RLIMIT_CPU / RLIMIT_AS equivalents, exported by wims on Windows */
      char *c=getenv("wims_rlimit_cpu"), *m=getenv("wims_rlimit_mem");
      wp_set_job(tmp_dir, c?atol(c):0, m?atol(m):0);
    }
#endif
    setenv("TMPDIR",tmp_dir,1);
    wseed=getenv("tmp_seed");
    mypid=getpid();
    gettimeofday(&t,NULL); seed=t.tv_usec*t.tv_sec+mypid;
    srandom(seed);
    for(i=0;i<4;i++) r[i]=random();
    snprintf(startstring,sizeof(startstring),
           "Start line %s %u %u %u %u",progname,r[0],r[1],r[2],r[3]);
    snprintf(endstring,sizeof(endstring),
           "End line %s %u %u %u %u",progname,r[0],r[1],r[2],r[3]);
    snprintf(buf,sizeof(buf),"%s_command",progname); p=getenv(buf);
    if(p!=NULL && *find_word_start(p)!=0) nameofcmd=find_word_start(p);
    snprintf(cmdbuf,sizeof(cmdbuf),"%s",nameofcmd); nameofcmd=cmdbuf;
    cmdparm=find_word_end(nameofcmd); if(*cmdparm) *cmdparm++=0;
    cmdparm=find_word_start(cmdparm);
    snprintf(pidfname,sizeof(pidfname),"%s/exec.pid",tmp_dir); addpid(mypid);
    snprintf(inputfname,sizeof(inputfname),"%s/%s_input.%d",tmp_dir,progname,mypid);
    snprintf(outputfname,sizeof(outputfname),"%s/%s_output.%d",tmp_dir,progname,mypid);
    errorf=NULL;
    inpf=inputfname; outpf=outputfname;
    isabout=0; p=find_word_start(parm); i=strlen("about");
    if(memcmp(p,"about",i)==0 && *find_word_start(p+i)==0) isabout=1;
    p=getenv("multiexec_debug"); if(p && strcmp(p,"yes")==0) debug=1;
}

void prepabout(char *cmd, char *outf, char *errf)
{
    (void)wp_write(&chan,cmd,strlen(cmd));
    execredirected(nameofcmd,NULL,outf,errf,cmdparm);
}

/* read to aboutbuf. Returns content length. */
int readabout(void)
{
    FILE *ff;
    long int l;
    char *p;

    aboutbuf[0]=0; ff=fopen(outputfname,"r");
    if(ff!=NULL) {
      fseek(ff,0,SEEK_END); l=ftell(ff); fseek(ff,0,SEEK_SET);
      l=fread(aboutbuf,1,aboutlen-10,ff); fclose(ff);
      if(l>0 && l<aboutlen) aboutbuf[l]=0; else aboutbuf[0]=0;
      p=find_word_start(aboutbuf); if(p>aboutbuf) ovlstrcpy(aboutbuf,p);
    }
    return strlen(aboutbuf);
}

/* read result of execution */
void readresult(void)
{
    FILE *ff;
    char *p, *pp, *pe;

    if(debug) {
      ff=fopen(outputfname,"a"); if(ff) {
          fputs(obuf,ff); fclose(ff);
      }
    }
    pe=NULL;
    p=strstr(obuf,startstring);
    if(p) {
      p+=strlen(startstring);
      pe=strstr(p,endstring);
      if(pe) {
          for(pp=pe-1; pp>=p && pp>pe-64 && *pp!='\n'; pp--);
          if(pp>=p && *pp=='\n') pe=pp;
      }
    }
    if(pe) {
      *pe=0;
      while((pe=strstr(p,startstring))!=NULL) {
          p=pe+strlen(startstring);
      }
      output(p);
    }
    else {
      if(mxpid>=0) {
          if(!wp_alive(&mxproc)) { /* this doesn't work */
            fprintf(stderr,"%s not_INStalled",progname);
          }
          else {
            fprintf(stderr,"%s: execution error or time out.\n",progname);
            wp_kill(&mxproc);
          }
          rmpid(mxpid); mxpid=-1;
      }
      else {
          fprintf(stderr,"%s: aborted on earlier error.\n",progname);
      }
    }
    if(multiexec && *multiexec_random) printf("%s\n",multiexec_random);
    fflush(stdout);
}

#ifdef linebyline1

/* this one is for maxima but not used */
void dopipes(void)
{
    long int i, l;
    char *outptr;
    char *p1, *p2, *pp, *pe;
    struct timeval t;
    fd_set rset;

    if(mxpid<0) {
      *obuf=0; return;
    }
    outptr=obuf; pp=pe=NULL;
    for(p1=ibuf; *p1; p1=p2) {
      p2=strchr(p1,'\n'); if(p2) p2++; else p2=p1+strlen(p1);
      write(chan.in_w,p1,p2-p1);

      if(strstr(p1,startstring)!=NULL) iactive=1;
      reread:
      l=read(fifofd2,lp,MAX_LINELEN-(lp-lbuf));
      if(!iactive) continue;
      if(l<0 || l>MAX_LINELEN-(lp-lbuf)) l=0;
      lp+=l; *lp=0;
      if(active==0) {
          char *pp;
          pp=strstr(lbuf,startstring);
          if(pp!=NULL) {
            active=1; ovlstrcpy(lbuf,pp); lp=lbuf+strlen(lbuf);
          }
      }
      if(active!=0 && strstr(lbuf,linebyline)!=NULL) {
          fprintf(ff,"%s",lbuf); lp=lbuf;
          if(strstr(lbuf,endstring)!=NULL) {lbuf[0]=0; active=-1; break;}
          lbuf[0]=0; continue;
      }
/* time out allowance between each silence */
      t.tv_sec=3; t.tv_usec=400000;
      i=select(fifofd2+1,&rset,NULL,NULL,&t);
      if(i>0) goto reread;
      else break; /* time out or error */
    }
}

#else

void dopipes(void)
{
    long int i, k, l;
    int interval=20000; /* in microseconds */
    int timeout =300; /* in intervals */
    int cr, cw;
    char *outptr;
    char *p1, *p2, *inp, *pw;

    *obuf=0; if(mxpid<0) return;
    outptr=obuf; inp=inputbuf; k=0;
    while(inp<inputptr) {
      i=wp_poll(&chan,1,1,interval,&cr,&cw);
      if(i<=0) {
          k++; if(k>=timeout) return; /* time out */
      }
      if(cw) {
/* Writing must be line by line, for otherwise
 * it will block when the input is large. */
          for(pw=inp; pw<inputptr && *pw!='\n'; pw++);
          if(pw<inputptr) pw++;
          l=wp_write(&chan,inp,pw-inp);
          if(l<0 || l>inputptr-inp) return;
          inp+=l;
      }
      if(cr) {
          l=wp_read(&chan,outptr,fsizelim-(outptr-obuf)-1);
          if(l<=0 || l>fsizelim-(outptr-obuf)-1) {
            *outptr=0; break;
          }
          if(WP_STRIP_CR_DEFAULT) l=wp_strip_cr(outptr,l);
          outptr+=l; *outptr=0;
      }
    }
    p2=NULL;
    p1=strstr(obuf,startstring);
    if(p1) {
      p1+=strlen(startstring);
      p2=strstr(p1,endstring);
    }
    while(!p2) {
      i=wp_poll(&chan,1,0,interval,&cr,NULL);
      if(i>0) {
          l=wp_read(&chan,outptr,fsizelim-(outptr-obuf)-1);
          if(l<=0 || l>fsizelim-(outptr-obuf)-1) {
            *outptr=0; break;
          }
          if(WP_STRIP_CR_DEFAULT) l=wp_strip_cr(outptr,l);
          outptr+=l; *outptr=0;
      }
      else {
          k++; if(k>=timeout) break;
      }
      if(!p1) {
          p1=strstr(obuf,startstring);
          if(p1) p1+=strlen(startstring);
      }
      if(p1) p2=strstr(p1,endstring);
    }
}

#endif

void run(void)
{
    FILE *ff;

    if(isabout) {about(); goto end;}
    execredirected(nameofcmd,NULL,NULL,NULL,cmdparm);
    putheader();
    obuf=xmalloc(fsizelim); if(!obuf) return;
    rep:
    putparm();
    if(!multiexec) {
      snprintf(inputptr,inputend-inputptr,"%s",quitstring);
      inputptr+=strlen(inputptr);
    }
    if(debug) {
      ff=fopen(inputfname,"a"); if(ff) {
          fputs(inputbuf,ff); fclose(ff);
      }
    }
    dopipes();
    readresult();
    if(multiexec) {
      getstdin(); inputptr=inputbuf; inputbuf[0]=0;
      goto rep;
    }
    end: if(strstr(tmp_dir,"tmp/sessions/")==NULL) {
      unlink(inputfname); unlink(outputfname);
    }
    free(obuf); rmpid(mypid);
    if(mxpid>0) {wp_kill(&mxproc); rmpid(mxpid);}
}

