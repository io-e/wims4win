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

 /* This is a small program used simply to fetch web pages.
  * No fancy functionalities such as link redirection or site sucking is
  * present.
  * Page fetched can only be sent to stdout. */

#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "../includes.h"
#include "../wimsdef.h"

char *cheater1="User-Agent: WIMS-webget";
char *cheater2="Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, image/png, */*\r\n\
Accept-Encoding: gzip\r\n\
Accept-Language: en, fr, it, de, es\r\n\
Accept-Charset: iso-8859-1,*,utf-8";
char pbuf[4096], tbuf[4096];
char tfname[1024];
char *tmpdir="/tmp";
int soc, port, https;
int charcnt;
FILE *outf;

void errorquit(char *msg)
{
  fprintf(stderr,"%s: %s\n",msg,strerror(errno)); exit(1);
}

/* Points to the end of the word */
char *find_word_end(char *p)
{
  int i;
  for(i=0;!isspace(*p) && *p!=0 && i<MAX_LINELEN; p++,i++);
  return p;
}

/* Strips leading spaces */
char *find_word_start(char *p)
{
  int i;
  for(i=0; isspace(*p) && i<MAX_LINELEN; p++,i++);
  return p;
}

/* Secured execution */
void secure(char *host)
{
  char *p1, *p2, *p3, buf[MAX_LINELEN+1];
  long int l;
  FILE *f;

  p1=getenv("w_module"); if(p1==NULL || *p1==0) return;
  p1=getenv("untrust"); if(p1==NULL || *p1==0) return;
  f=fopen("webget.sites","r"); if(f==NULL) return;
  l=fread(buf,1,MAX_LINELEN,f); fclose(f);
  if(l<=0 || l>MAX_LINELEN) return;
  buf[l]=0;
  for(p1=find_word_start(buf);*p1;p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    p3=strstr(host,p1); if(p3==NULL) continue;
    if((p3==host || *(p3-1)=='.') && *(p3+strlen(p1))==0) return;
  }
  exit(1);  /* unauthorized sites refused. */
}

/* open a TCP/IP socket with host/port
 * returns the file descriptor for the socket */
int net_connect(char *host)
{
  struct hostent *hp;
  struct sockaddr_in sin;
  int soc;

  secure(host);
  if(!(hp = gethostbyname(host))) errorquit("unknown host.");
  if((soc = socket(hp->h_addrtype,SOCK_STREAM,0))<0)
    errorquit("socket() error");
  memmove(&sin.sin_addr,hp->h_addr,hp->h_length);
  sin.sin_port=htons(port);
  sin.sin_family = hp->h_addrtype;
  if(connect(soc,(struct sockaddr *)&sin,sizeof(sin))<0) {
    close(soc); errorquit("connect() error");
  }
  return soc;
}

int gethttps(char *host)
{
  char buf[65536];
  char *tp;

  tp=getenv("tmp_dir"); if(tp!=NULL && *tp!=0) tmpdir=tp;
  snprintf(tfname,sizeof(tfname),"%s/https.tmp",tmpdir);
  snprintf(buf,sizeof(buf),"\
mkdir -p %s\n\
openssl s_client -connect %s:%d -quiet 2>/dev/null >%s <<@\n\
%s\n\
@\n", tmpdir,host,port,tfname,tbuf);
  if (system(buf))
    errorquit("system() error");
  return open(tfname,O_RDONLY);
}

int main(int argc, char *argv[])
{
  char *parm, *pt, *p1, *p2, *p3, *p4, *dp, *pre;
  char nbuf[4096], *pp1, *pp2;
  char c;

  parm=getenv("wims_exec_parm");
  if(parm==NULL || *parm==0) errorquit("no_parameter");
  snprintf(pbuf,sizeof(pbuf),"%s",parm);
  p1=find_word_start(pbuf); p2=find_word_end(p1);
  if(*p2!=0) *p2++=0;
  https=0;
  outf=stdout; pp1=getenv("w_webget_output");
  pp2=getenv("session_dir");
  if(pp1!=NULL && strstr(pp1,"..")==NULL && isalnum(*pp1) && pp2!=NULL) {
    snprintf(nbuf,sizeof(nbuf),"%s/%s",pp2,pp1);
    outf=fopen(nbuf,"w"); if(outf==NULL) outf=stdout;
  }
  dp=getenv("w_webget_option");
  if(dp!=NULL && strstr(dp,"direct")!=NULL) { /* direct get */
    p1=getenv("w_webget_host");
    p2=getenv("w_webget_port");
    if(p1==NULL || p2==NULL) errorquit("incomplete_request");
    port=atoi(p2);
    soc=net_connect(p1); if(soc==-1) return 1;
    c=' ';
    for(p3=parm; *p3; p3++) {
      if(*p3=='\n' && c!='\r') (void)write(soc,"\r",1);
      (void)write(soc,p3,1); c=*p3;
    }
    (void)write(soc,"\r\n\r\n",4);
    pt=getenv("w_module");
    if(pt==NULL || *pt==0 || strncmp(pt,"adm/",4)==0 ) {  /* File to post? */
      pt=getenv("w_webget_post");
      if(pt!=NULL && *pt!=0) {
        FILE *f;
        char buf[4096];
        size_t l;
        f=fopen(pt,"r"); if(f!=NULL) {
        do {
          l=fread(buf,1,sizeof(buf),f);
          if(l>0 && l<=sizeof(buf)) (void)write(soc,buf,l);
        }
        while(l==sizeof(buf));
        fclose(f);
      }
    }
  }
  if(strstr(dp,"normalread")!=NULL) goto read;
  charcnt=0;
  while(read(soc,pbuf,1)>0 && charcnt<10240) {
    fputc(pbuf[0],outf); charcnt++;
  }
  close(soc);
  return 0;
  }
  if(strncasecmp(p1,"http://",strlen("http://"))==0) p1+=strlen("http://");
  else
    if(strncasecmp(p1,"https://",strlen("https://"))==0) {
      https=1; p1+=strlen("https://");
    }
  p3=strchr(p1,'/'); if(p3==NULL) p3="";
  else {*p3++=0; while(*p3=='/') p3++;}
  if(strncasecmp(p3,"http://",strlen("http://"))==0 ||
     strncasecmp(p3,"https://",strlen("https://"))==0) pre="";
  else pre="/";
  snprintf(tbuf,sizeof(tbuf),"GET %s%s HTTP/1.0\r\n%s\r\n\
Host: %s\r\n\
%s\r\n\r\n",
           pre,p3,cheater1,p1,cheater2);
  p4=strchr(p1,':');
  if(p4==NULL) {
    if(https) port=443; else port=80;
  }
  else {*p4++=0; port=atoi(p4);}
  if(https) {
    soc=gethttps(p1); goto read;
  }
  soc=net_connect(p1);
  (void)write(soc,tbuf,strlen(tbuf));
/* header */
  read: if(soc==-1) return 1;
  c=-1;
  while(read(soc,pbuf,1)>0) {
    if(pbuf[0]=='\r') continue;
    fputc(pbuf[0],stderr);
    if((c=='\n') && (pbuf[0]=='\n')) break; else c=pbuf[0];
  }
/* body */
  charcnt=0;
  while(read(soc,pbuf,1)>0 && charcnt<MAX_WEBGETFLEN) {
    fputc(pbuf[0],outf); charcnt++;
  }
  close(soc);
  if(outf!=stdout) fclose(outf);
  if(https) unlink(tfname);
  return 0;
}

