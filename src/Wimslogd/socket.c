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

/* WIMS log daemon, socket routines */
#include "wimslogd.h"

void errorquit(char *p)
{
    wimslogd_error(p); exit(1);
}

void opensock(void)
{
    struct stat st;
    struct sockaddr_un sun;
    if(stat(sockfile,&st)==0) unlink(sockfile);
    commsock=socket(PF_UNIX,SOCK_STREAM,0);
    if(commsock<0) errorquit("Unable to open socket.");
    sun.sun_family=PF_UNIX;
    snprintf(sun.sun_path,sizeof(sun.sun_path),"%s",sockfile);
    if(bind(commsock,(struct sockaddr *)&sun, sizeof(sun))==-1)
      errorquit("Unable to bind to socket file.");
    if(listen(commsock,SOCKET_QUEUE)==-1) errorquit("listen() error.");
}

void output(int fh)
{
  int l, *ip;
  if(answerlen<0) l=strlen(textbuf); else l=answerlen;
  ip=(int *) commbuf; *ip=l;
  (void)write(fh,commbuf,l+sizeof(int));
  close(fh);
  if(debugging) {
    if(textbuf[0]=='O') debug("%.2s %d bytes.",textbuf,l);
    else debug("%s",textbuf);
  }
}

void sockerror(int type, char *p,...)
{
  char *estr, buf[MAX_LINELEN+1];
  va_list vp;
  va_start(vp,p);
  vsnprintf(buf,sizeof(buf),p,vp);
  va_end(vp);
  if(errno==0) estr=""; else estr=strerror(errno);
  snprintf(textbuf,BUFFERLEN-16,"ERROR %d\n%s\n%s",type,buf,estr);
}

void sockok(char *p)
{
  snprintf(textbuf,BUFFERLEN-16,"OK\n%s",p);
}

void answer(int fh)
{
  int t,l, *ip;

  t=read(fh,commbuf,BUFFERLEN-sizeof(int)); if(t<sizeof(int)) {
    bad: sockerror(3,"Daemon socket read error.");
    goto end;
  }
  ip=(int *) commbuf; l=*ip;
  if(l<=0 || l>=BUFFERLEN-sizeof(int)) goto bad;
  while(t<l+sizeof(int)) {
    struct timeval tv;
    fd_set rset;
    int t2;
    tv.tv_sec=0; tv.tv_usec=20*1000;
    FD_ZERO(&rset); FD_SET(fh,&rset);
    if(select(fh+1,&rset,NULL,NULL,&tv)<=0) goto bad;
    t2=read(fh,commbuf+t,l+sizeof(int)-t);
    if(t2<=0 || t2>l+sizeof(int)-t) goto bad;
    t+=t2;
  }
  textbuf[l]=0; textptr=textbuf;
  answerlen=-1;
  if(debugging) debug("> %s",textbuf);
  if(options()>=0) cmd();
  end: (void)chdir(cwd); cwdtype=dir_home; output(fh);
}
