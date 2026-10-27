/* webget for native Windows (wims4win). Same interface as src/Misc/webget.c:
 *   wims_exec_parm = "URL ..."  (http:// or https://, first word)
 *   w_webget_output / session_dir  : optional output file
 *   w_webget_option contains "direct": raw request (parm) sent to
 *       w_webget_host:w_webget_port, reply (max 10240 bytes) copied out.
 *   w_module + untrust : host must be listed in webget.sites.
 * Response headers go to stderr, body (max MAX_WEBGETFLEN) to stdout/file.
 * URL mode uses WinHTTP (http and https, no openssl, no shell);
 * direct mode uses WinSock. Linux keeps src/Misc/webget.c unchanged. */
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <winhttp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#define MAX_LINELEN 45000
#define MAX_WEBGETFLEN (1024*1024*1024)
static void errorquit(const char *m){ fprintf(stderr,"%s: error %lu\n",m,GetLastError()); exit(1); }
static char *wend(char *p){ while(*p && !isspace((unsigned char)*p)) p++; return p; }
static char *wstart(char *p){ while(*p && isspace((unsigned char)*p)) p++; return p; }
static void secure(const char *host)
{
  char buf[MAX_LINELEN+1], *p1, *p2, *p3; size_t l; FILE *f;
  p1=getenv("w_module"); if(!p1||!*p1) return;
  p1=getenv("untrust"); if(!p1||!*p1) return;
  f=fopen("webget.sites","rb"); if(!f) return;
  l=fread(buf,1,MAX_LINELEN,f); fclose(f);
  if(l==0) return;
  buf[l]=0;
  for(p1=wstart(buf);*p1;p1=wstart(p2)){
    p2=wend(p1); if(*p2) *p2++=0;
    p3=strstr(host,p1); if(!p3) continue;
    if((p3==host||p3[-1]=='.') && p3[strlen(p1)]==0) return;
  }
  exit(1);
}
static FILE *open_out(void)
{
  char nbuf[4096]; char *p1=getenv("w_webget_output"), *p2=getenv("session_dir"); FILE *f;
  if(p1 && !strstr(p1,"..") && isalnum((unsigned char)*p1) && p2){
    snprintf(nbuf,sizeof nbuf,"%s/%s",p2,p1); f=fopen(nbuf,"wb"); if(f) return f;
  }
  return stdout;
}
static int direct(const char *parm, FILE *out)
{
  WSADATA w; struct addrinfo hints={0},*ai; SOCKET s; char c=' ', b; int n=0;
  const char *h=getenv("w_webget_host"), *pt=getenv("w_webget_port"), *p;
  if(!h||!pt) errorquit("incomplete_request");
  secure(h);
  if(WSAStartup(MAKEWORD(2,2),&w)) errorquit("WSAStartup");
  hints.ai_socktype=SOCK_STREAM;
  if(getaddrinfo(h,pt,&hints,&ai)) errorquit("unknown host.");
  s=socket(ai->ai_family,ai->ai_socktype,ai->ai_protocol);
  if(s==INVALID_SOCKET) errorquit("socket() error");
  if(connect(s,ai->ai_addr,(int)ai->ai_addrlen)) errorquit("connect() error");
  for(p=parm;*p;p++){ if(*p=='\n'&&c!='\r') send(s,"\r",1,0); send(s,p,1,0); c=*p; }
  send(s,"\r\n\r\n",4,0);
  while(n<10240 && recv(s,&b,1,0)==1){ fputc(b,out); n++; }
  closesocket(s); WSACleanup(); return 0;
}
int main(void)
{
  char pbuf[4096], *url, *host, *path, *colon; wchar_t whost[1024], wpath[4096], whdr[16384];
  int https=0, port; FILE *out; char *parm=getenv("wims_exec_parm"), *dp;
  HINTERNET ss, cn, rq; DWORD n, sz; char buf[65536]; long long total=0;
  if(!parm||!*parm) errorquit("no_parameter");
  out=open_out();
  dp=getenv("w_webget_option");
  if(dp && strstr(dp,"direct")) return direct(parm,out);
  snprintf(pbuf,sizeof pbuf,"%s",parm);
  url=wstart(pbuf); *wend(url)=0;
  if(!_strnicmp(url,"http://",7)) url+=7;
  else if(!_strnicmp(url,"https://",8)){ https=1; url+=8; }
  host=url; path=strchr(host,'/');
  if(path){ *path++=0; while(*path=='/') path++; } else path="";
  colon=strchr(host,':'); port=https?443:80;
  if(colon){ *colon++=0; port=atoi(colon); }
  secure(host);
  MultiByteToWideChar(CP_UTF8,0,host,-1,whost,1024);
  { char t[4096]; snprintf(t,sizeof t,"%s%s",(!_strnicmp(path,"http://",7)||!_strnicmp(path,"https://",8))?"":"/",path);
    MultiByteToWideChar(CP_UTF8,0,t,-1,wpath,4096); }
  ss=WinHttpOpen(L"WIMS-webget",WINHTTP_ACCESS_TYPE_AUTOMATIC_PROXY,WINHTTP_NO_PROXY_NAME,WINHTTP_NO_PROXY_BYPASS,0);
  if(!ss) errorquit("WinHttpOpen");
  cn=WinHttpConnect(ss,whost,(INTERNET_PORT)port,0); if(!cn) errorquit("unknown host.");
  rq=WinHttpOpenRequest(cn,L"GET",wpath,L"HTTP/1.0",WINHTTP_NO_REFERER,WINHTTP_DEFAULT_ACCEPT_TYPES,https?WINHTTP_FLAG_SECURE:0);
  if(!rq) errorquit("request");
  /* no redirect following and no decompression: same bytes as the Linux tool */
  { DWORD o=WINHTTP_OPTION_REDIRECT_POLICY_NEVER; WinHttpSetOption(rq,WINHTTP_OPTION_REDIRECT_POLICY,&o,sizeof o); }
  if(!WinHttpSendRequest(rq,L"Accept-Language: en, fr, it, de, es\r\n",(DWORD)-1L,WINHTTP_NO_REQUEST_DATA,0,0,0)
     || !WinHttpReceiveResponse(rq,NULL)) errorquit("connect() error");
  sz=sizeof whdr;
  if(WinHttpQueryHeaders(rq,WINHTTP_QUERY_RAW_HEADERS_CRLF,WINHTTP_HEADER_NAME_BY_INDEX,whdr,&sz,WINHTTP_NO_HEADER_INDEX)){
    char h8[16384]; int i,j=0; WideCharToMultiByte(CP_UTF8,0,whdr,-1,h8,sizeof h8,NULL,NULL);
    for(i=0;h8[i];i++) if(h8[i]!='\r') h8[j++]=h8[i]; h8[j]=0;
    fputs(h8,stderr);
  }
  while(WinHttpReadData(rq,buf,sizeof buf,&n) && n>0 && total<MAX_WEBGETFLEN){
    fwrite(buf,1,n,out); total+=n;
  }
  WinHttpCloseHandle(rq); WinHttpCloseHandle(cn); WinHttpCloseHandle(ss);
  if(out!=stdout) fclose(out);
  return 0;
}
