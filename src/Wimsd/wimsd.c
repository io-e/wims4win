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

/* http daemon for WIMS */

#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include "../wims.h"

#if !HAVE_SOCKLEN_T
typedef size_t socklen_t;
#endif

#define WIMS_TIMEOUT	200
#define WIMSD_TICK	10
#define WIMSD_IDLE	500
#define PIDLIM		100

char *acceptstr="127.0.0.1";

int pidtab[PIDLIM];
int pidcnt;

int sock;
int sport;
int idle; /* limited to WIMSD_TICK * WIMSD_IDLE */

void errorquit(char *msg)
{
    fprintf(stderr,"%s: %s\n",msg,strerror(errno)); exit(1);
}

void checkpid(void)
{
    int i,k;
    for(i=0;i<pidcnt;i++) {
	waitpid(pidtab[i],&k,WNOHANG);
	if(WIFEXITED(k)) {
	    memmove(pidtab+i,pidtab+i+1,(pidcnt-i-1)*sizeof(pidtab[0]));
	    pidcnt--;
	}
    }
}

void addpid(int pid)
{
    while(pidcnt>=PIDLIM) {
	sleep(1); checkpid();
    }
    pidtab[pidcnt++]=pid;
}

void alarm1(int s)
{
    fprintf(stderr,"Time out.\n"); close(sock); exit(1);
}

void alarm2(int s)
{
    checkpid(); idle++; alarm(WIMSD_TICK);
    if(idle>=WIMSD_IDLE) alarm1(0);
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

	/* Read/write to a file with variable parms to print filename */
void wimsdaccessfile(char *content, char *type, char *s,...)
{
    va_list vp;
    char buf[MAX_LINELEN+1];
    FILE *f;
    int l;

    va_start(vp,s);
    vsnprintf(buf,sizeof(buf),s,vp);
    va_end(vp);
    f=fopen(buf,type); if(f==NULL) {
	if(*type=='r') content[0]=0; return;
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

void badreq(void)
{
    printf("HTTP/1.0 400 Bad Request\r\n\r\nBad Request.\r\n");
    exit(0);
}

	/* open a TCP/IP socket with host/port
	 * returns the file descriptor for the socket */
int net_connect(int port)
{
    struct sockaddr_in sin;
    int soc, vrai;

    if ((soc = socket(AF_INET, SOCK_STREAM, 0)) == -1)
      errorquit("socket() error");
    if(setsockopt(soc, SOL_SOCKET, SO_REUSEADDR, &vrai, sizeof(vrai)) == -1)
      errorquit("setsockopt() error");

    sin.sin_port=htons(port);
    sin.sin_family = AF_INET;
    inet_aton(acceptstr,&sin.sin_addr);
    if(bind(soc,(struct sockaddr *)&sin, sizeof(sin))==-1)
      errorquit("bind() error");
    if(listen(soc,10)==-1) errorquit("listen() error");
    return soc;
}

void putfile(char *fname,int soc)
{
    int l;
    long int siz;
    char *p, namebuf[4096], buf[4096];
    char cbuf[MAX_LINELEN+1];
    struct stat st;
    FILE *f;

    if(fname[strlen(fname)-1]=='/')
      snprintf(namebuf,sizeof(namebuf),"%sindex.html",fname);
    else snprintf(namebuf,sizeof(namebuf),"%s",fname);
    statit: if(stat(namebuf,&st)!=0) badreq();
    if(S_ISDIR(st.st_mode)) {
	snprintf(namebuf,sizeof(namebuf),"%s/index.html",fname);
	goto statit;
    }
    if(!S_ISREG(st.st_mode)) badreq();
    if(strncmp(namebuf,"modules/adm/",strlen("modules/adm/"))==0 ||
       strncmp(namebuf,"modules/home/",strlen("modules/home/"))==0)
      badreq();
    for(p=strchr(namebuf,'/'); p!=NULL; p=strchr(p+1,'/')) {
	*p=0; snprintf(buf,sizeof(buf),"%s/.htaccess",namebuf); *p='/';
	if(stat(cbuf,&st)==0) {
	    wimsdaccessfile(cbuf,"r","%s",buf);
	    if(strstr(cbuf,"deny from all")!=NULL) badreq();
	}
    }
    siz=st.st_size;
    printf("HTTP/1.0 200 OK\r\n\
Content-Length: %ld\r\n\r\n",
	   siz);
    f=fopen(namebuf,"r"); if(f==NULL) badreq();
    do {
	l=fread(buf,1,sizeof(buf),f);
	if(l>0 && l<=sizeof(buf)) fwrite(buf,1,l,stdout);
    } while(l==sizeof(buf));
    fclose(f); fclose(stdin); fclose(stdout); close(soc);
    exit(0);
}

struct sockaddr_in saddr;

void onereq(int soc)
{
    char buf[QUERY_STRING_LIMIT+1], buf2[MAX_LINELEN+1];
    int i, k, l;
/*     FILE *fin, *fout; */
    struct stat st;
    char *query_method, *query_url, *p, *pp, *p2, *parms;

    i=fork(); if(i!=0) {
	if(i==-1) errorquit("fork() error");
	addpid(i); return;
    }
    alarm(WIMS_TIMEOUT);
    signal(SIGALRM,alarm1);
    (void)chdir("public_html");
    i=0; do {
        l=read(soc, buf+i, 1); i++;
    }
    while(i<QUERY_STRING_LIMIT && l>0 && (i<4 || memcmp(buf+i-4,"\r\n\r\n",4)!=0));
    buf[i]=0;
/*printf("%s\n",buf);*/
    dup2(soc,0); /* fin=fdopen(0,"r"); stdin=fin; */
    dup2(soc,1); /* fout=fdopen(1,"w"); stdout=fout; */
    query_method=find_word_start(buf);
    query_url=find_word_end(query_method); if(query_url) *query_url++=0;
    query_url=find_word_start(query_url);
    p=find_word_end(query_url); if(*p) *p++=0;
    parms=strchr(p,'\n'); if(parms) parms++;
    if(strcmp(query_method,"GET")!=0 &&
       strcmp(query_method,"HEAD")!=0 &&
       strcmp(query_method,"POST")!=0) badreq();
    if(query_url[0]!='/' || query_url[1]=='/' || strstr(query_url,"..")!=NULL)
      badreq();
    setenv("REQUEST_URL",query_url,1);
    p=strchr(query_url,'?'); if(p!=NULL) *p++=0; else p="";
    k=stat(query_url+1,&st);
    if(k!=0 && strchr(query_url+1,'/')==NULL)
      query_url="/wims.cgi";
    i=strlen(query_url);
    if(i<4 || strcasecmp(query_url+i-4,".cgi")!=0)
      putfile(query_url+1,soc);
    if(strchr(query_url+1,'/')!=NULL && strncmp(query_url,"/test/",strlen("/test/"))!=0)
      badreq();
    setenv("QUERY_STRING",p,1);
    setenv("QUERY_URL",query_url,1);
    setenv("SCRIPT_NAME",query_url,1);
    setenv("REQUEST_METHOD",query_method,1);
    setenv("SERVER_SOFTWARE","WIMS",1);
    snprintf(buf2,sizeof(buf2),"%d",sport);
    setenv("SERVER_PORT",buf2,1);
    for(; *parms; parms=p) {
	p=strchr(parms,'\n');
	if(p==NULL) p=parms+strlen(parms);
	else {
	    if(*(p-1)=='\r') *(p-1)=0;
	    *p++=0;
	}
	for(pp=parms; isalnum(*pp) || *pp=='-'; pp++);
	if(*pp==':') {
	    *pp++=0; pp=find_word_start(pp);
	    for(p2=parms; *p2; p2++) {
		if(*p2=='-') *p2='_'; else *p2=toupper(*p2);
	    }
	    if(strcmp(parms,"CONTENT_LENGTH")==0 ||
	       strcmp(parms,"CONTENT_TYPE")==0) {
		snprintf(buf2,sizeof(buf2),"%s",parms);
		setenv(buf2,pp,1);
	    }
	    snprintf(buf2,sizeof(buf2),"HTTP_%s",parms);
	    setenv(buf2,pp,1);
	}
    }
    setenv("REMOTE_ADDR",inet_ntoa(saddr.sin_addr),1);
    snprintf(buf2,sizeof(buf2),"%u",ntohs(saddr.sin_port));
    setenv("REMOTE_PORT",buf2,1);

    snprintf(buf2,sizeof(buf2),".%s",query_url);
    execl(buf2,buf2,NULL);

    /* fclose(fout); fclose(fin); */ close(soc); exit(0);
}

void serve(int soc)
{
    int newsoc;
    socklen_t slen=sizeof(saddr);

    wait:
    alarm(WIMSD_TICK);
    newsoc=accept(soc,(struct sockaddr *)&saddr, &slen);
    if(newsoc==-1) errorquit("accept() error");
    checkpid(); idle=0;
    onereq(newsoc); close(newsoc); goto wait;
}

void parm(void)
{
    char *p;
    p=getenv("WIMS_HOME");
    if(p!=NULL && *p!=0) (void)chdir(p);
    p=getenv("WIMSD_PORT");
    if(p!=NULL && *p!=0) {
	int i;
	i=atoi(p);
	if(i>0 && i<65536) sport=i;
    }
    p=getenv("WIMSD_ACCEPT");
    if(p!=NULL && strstr(p,"all")!=NULL) acceptstr="0.0.0.0";
}

int main(int argc, char *argv[])
{
    struct stat st;

    signal(SIGALRM,alarm2);
    sport=8088; pidcnt=0; idle=0;
    parm();
    if(stat("public_html/wims.cgi",&st)!=0) {
	char buf[1024];
	(void)getcwd(buf,sizeof(buf));
	fprintf(stderr,"Bad home directory %s: \
wims.cgi not found.\n",buf); exit(1);
    }
/*    if(stat("public_html/bin/pari",&st)==0) {
	fprintf(stderr,"wimsd must be run as nobody, \
for security reasons!\n\n\
Run bin/wrapuid as root to set things up correctly.\n"); exit(1);
    }
*/    setreuid(geteuid(),getuid());setregid(getegid(),getgid());
    sock=net_connect(sport);

    serve(sock);
    close(sock); return 0;
}

