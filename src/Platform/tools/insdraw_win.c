/* insdraw..processor.exe -- native Windows replacement of the sh script
 * public_html/bin/insdraw..processor (GPL v2 or later).
 *
 * Same contract as the script: argv[1]=plot number, argv[2]=session tmp dir;
 * environment: ins_source, ins_anim_frames, w_ins_format, w_insdraw_size,
 * w_wims_inssub, w_insdraw_filebase, trusted_module, module_dir, w_wims_home.
 * Builds the flydraw program exactly like the script, pipes it to
 * <wims_home>/other/bin/flydraw.exe, then renames <sub>-<n>-0.gif to
 * insert<sub>-<n>.<fmt>.
 * Not available on Windows (no ImageMagick/whirlgif): format conversion
 * (the GIF bytes are kept under the requested name) and multi-frame
 * animations (first frame only); "tex" (tikz) output is passed through.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>

static const char *env(const char *n, const char *d)
{ const char *v=getenv(n); return (v && *v) ? v : d; }

static char *cat(char *b, size_t *cap, size_t *len, const char *s)
{
  size_t l=strlen(s);
  if(*len+l+1>*cap){ *cap=(*len+l+1)*2; b=realloc(b,*cap); if(!b) exit(2); }
  memcpy(b+*len,s,l+1); *len+=l; return b;
}

static void slashes(char *s){ for(;*s;s++) if(*s=='\\') *s='/'; }

int main(int argc, char **argv)
{
  const char *fmt=env("w_ins_format","gif"), *size=env("w_insdraw_size","256,256");
  const char *src=env("ins_source",""), *sub=env("w_wims_inssub","");
  const char *home=env("w_wims_home",".."), *fb=getenv("w_insdraw_filebase");
  const char *mdir=env("module_dir","."), *trusted=env("trusted_module","");
  int frames=atoi(env("ins_anim_frames","1")), i, tex=strcmp(fmt,"tex")==0;
  const char *inter=tex?"tex":"gif";
  char line[4096], fly[MAX_PATH], base[8192], cmdf[MAX_PATH], from[MAX_PATH], to[MAX_PATH];
  char *prog=NULL; size_t cap=0, len=0; FILE *f; DWORD code=1;
  STARTUPINFOA si; PROCESS_INFORMATION pi; SECURITY_ATTRIBUTES sa; HANDLE rd, wr;

  if(argc<3 || !*argv[2]) return 0;
  if(frames<1) frames=1;
  prog=cat(prog,&cap,&len,"");
  if(tex) {
    snprintf(line,sizeof(line),"\n tikzfile %s/insert%s-%s.tex\n new %s\n wims_linecount 0\n %s\n ",
             argv[2],sub,argv[1],size,src);
    prog=cat(prog,&cap,&len,line);
  } else for(i=0;i<frames;i++) {
    snprintf(line,sizeof(line),"\n new %s\n animstep=%d\n s=%d/%d\n wims_linecount 0\n ",size,i,i,frames);
    prog=cat(prog,&cap,&len,line); prog=cat(prog,&cap,&len,src);
    snprintf(line,sizeof(line),"\n output %s/insert%s-%s-%d.%s\n",argv[2],sub,argv[1],i,inter);
    prog=cat(prog,&cap,&len,line);
  }
  /* flydraw_filebase, as in the script */
  if(fb==NULL || *fb==0) {
    if(strcmp(trusted,"yes")==0) snprintf(base,sizeof(base),"%s/insdraw gifs",mdir);
    else snprintf(base,sizeof(base),"%s gifs %s/public_html",mdir,home);
  } else {
    char tmp[4096], *t; base[0]=0;
    if(strcmp(trusted,"yes")==0) snprintf(base,sizeof(base),"%s",fb);
    snprintf(tmp,sizeof(tmp),"%s",fb);
    for(t=strtok(tmp," \t"); t; t=strtok(NULL," \t")) {
      size_t l=strlen(base);
      if(!strncmp(t,"modules/data/",13) || strstr(t,"/log/classes/") || strstr(t,"/getfile/oefimg")
         || strstr(t,"/getfile/click")) snprintf(base+l,sizeof(base)-l," %s",t);
      else if(!strncmp(t,"datamodule/",11)) snprintf(base+l,sizeof(base)-l," %s/images modules/data/%s",mdir,t+11);
      else if(!strncmp(t,"wimshome/public_html/",21)) {
        char o[8192]; snprintf(o,sizeof(o),"%s %s",t+21,base); snprintf(base,sizeof(base),"%s",o);
      }
      else snprintf(base+l,sizeof(base)-l," %s/%s",mdir,t);
    }
    { size_t l=strlen(base); snprintf(base+l,sizeof(base)-l," gifs %s/public_html",home); }
  }
  SetEnvironmentVariableA("flydraw_filebase",base);
  snprintf(fly,sizeof(fly),"%s/other/bin/flydraw.exe",home); slashes(fly);
  for(i=0;i<frames && !tex;i++){ snprintf(from,sizeof(from),"%s/insert%s-%s-%d.%s",argv[2],sub,argv[1],i,inter); DeleteFileA(from); }

  sa.nLength=sizeof(sa); sa.lpSecurityDescriptor=NULL; sa.bInheritHandle=TRUE;
  if(!CreatePipe(&rd,&wr,&sa,0)) return 1;
  SetHandleInformation(wr,HANDLE_FLAG_INHERIT,0);
  memset(&si,0,sizeof(si)); si.cb=sizeof(si); si.dwFlags=STARTF_USESTDHANDLES;
  si.hStdInput=rd; si.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE); si.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  snprintf(line,sizeof(line),"\"%s\"",fly);
  if(!CreateProcessA(fly,line,NULL,NULL,TRUE,CREATE_NO_WINDOW,NULL,NULL,&si,&pi)) {
    fprintf(stderr,"insdraw: cannot run %s (%lu)\n",fly,GetLastError()); return 1;
  }
  CloseHandle(rd);
  { DWORD w; WriteFile(wr,prog,(DWORD)len,&w,NULL); WriteFile(wr,"\n",1,&w,NULL); }
  CloseHandle(wr);
  WaitForSingleObject(pi.hProcess,INFINITE); GetExitCodeProcess(pi.hProcess,&code);
  CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
  snprintf(cmdf,sizeof(cmdf),"%s/insdraw.cmd",argv[2]);
  if((f=fopen(cmdf,"ab"))!=NULL){ fputs(prog,f); fputc('\n',f); fclose(f); }
  if(!tex) {
    snprintf(from,sizeof(from),"%s/insert%s-%s-0.gif",argv[2],sub,argv[1]);
    snprintf(to,sizeof(to),"%s/insert%s-%s.%s",argv[2],sub,argv[1],fmt);
    if(frames>1 && strcmp(fmt,"gif")) snprintf(to,sizeof(to),"%s/insert%s-%s.gif",argv[2],sub,argv[1]);
    MoveFileExA(from,to,MOVEFILE_REPLACE_EXISTING);
    { WIN32_FILE_ATTRIBUTE_DATA a; if(GetFileAttributesExA(to,GetFileExInfoStandard,&a) && a.nFileSizeLow==0 && a.nFileSizeHigh==0) DeleteFileA(to); }
  }
  free(prog); return 0;
}
