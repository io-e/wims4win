/* oef_trace.c : traceur d'appels pour oef2wims-trace.exe (WIMS 4.31b, build MinGW).
 * Compile SANS -finstrument-functions. Le code WIMS est compile avec -finstrument-functions :
 * chaque entree/sortie de fonction appelle __cyg_profile_func_enter/exit ci-dessous.
 * Table des symboles + table des lignes C : ajoutees en fin d'exe (overlay "OEFSYM01") au build.
 * Variables : OEF2WIMS_TRACE=chemin|off (defaut <entree>.trace), OEF2WIMS_TRACE_MAX (lignes, defaut 200000),
 *             OEF2WIMS_TRACE_RING (derniers appels, defaut 64). Rien n'est ecrit sur stdout/stderr. */
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#define NI __attribute__((no_instrument_function))
extern IMAGE_DOS_HEADER __ImageBase;
extern char *badpar, *inpbuf;


typedef struct { unsigned rva, size, line; const char *name, *file; } fsym_t;
typedef struct { unsigned rva, line; const char *file; } lsym_t;
static fsym_t *F; static int nF; static lsym_t *Lt; static int nL;
static FILE *tf; static int st=0; /* 0 init pas faite, 1 actif, -1 desactive */
static LARGE_INTEGER fq,t0;
static long long nlines, maxlines=200000, ncalls; static int depth, maxdepth;
typedef struct { void *fn,*site; double t; int oef; } fr_t;
static fr_t stk[1024];
typedef struct { void *fn,*site; double t; int depth, oef, ev; } ring_t;
static ring_t *ring; static int ringn=64; static long long ringi;
typedef struct { void *fn; long long n; } cnt_t; static cnt_t cnt[8192];
static char *src; static int srcn; static int *nl; static int nnl;
static long cur_off=-1; static const char *cur_dir; static long dir_off=-1;
static const char *errmsg; static const char *status="en cours (pas de fin propre observee)";
static void *last_fn,*last_site; static unsigned crash_rva; static int dumped;
static char tpath[MAX_PATH*2];

static NI double now(void){ LARGE_INTEGER c; QueryPerformanceCounter(&c); return (c.QuadPart-t0.QuadPart)*1000.0/fq.QuadPart; }
static NI unsigned rva(void *a){ return (unsigned)((char*)a-(char*)&__ImageBase); }
static NI const fsym_t *fsym(void *a){ unsigned r=rva(a); int lo=0,hi=nF-1,b=-1; while(lo<=hi){int m=(lo+hi)/2; if(F[m].rva<=r){b=m;lo=m+1;} else hi=m-1;} return b>=0?&F[b]:0; }
static NI const lsym_t *lsym(unsigned r){ int lo=0,hi=nL-1,b=-1; while(lo<=hi){int m=(lo+hi)/2; if(Lt[m].rva<=r){b=m;lo=m+1;} else hi=m-1;} return b>=0?&Lt[b]:0; }
static NI const char *fname(void *a,char *b,size_t n){ const fsym_t *s=fsym(a); if(s&&s->rva==rva(a)) return s->name; snprintf(b,n,"fn@0x%x",rva(a)); return b; }
static NI const char *fdecl(void *a,char *b,size_t n){ const fsym_t *s=fsym(a); if(s&&s->file&&*s->file) snprintf(b,n,"%s:%u",s->file,s->line); else snprintf(b,n,"?"); return b; }
static NI const char *cline(unsigned r,char *b,size_t n){ const lsym_t *l=r?lsym(r):0; const fsym_t *s; if(l){ s=fsym((char*)&__ImageBase+r); snprintf(b,n,"%s:%u (dans %s)",l->file,l->line,s?s->name:"?"); } else snprintf(b,n,"?"); return b; }
static NI int oefline(long off,int *col){ if(off<0||!nl){ if(col)*col=0; return 0;} int lo=0,hi=nnl-1,b=-1; while(lo<=hi){int m=(lo+hi)/2; if(nl[m]<off){b=m;lo=m+1;} else hi=m-1;} if(col)*col=(int)(off-(b>=0?nl[b]:-1)); return b+2; }
static NI long curoff(void){ return cur_off; }

static NI void load_syms(void){
  char exe[MAX_PATH*2]; GetModuleFileNameA(0,exe,sizeof exe); FILE *f=fopen(exe,"rb"); if(!f) return;
  char tr[12]; fseek(f,-12,SEEK_END); if(fread(tr,1,12,f)!=12||memcmp(tr+4,"OEFSYM01",8)){fclose(f);return;}
  unsigned sz; memcpy(&sz,tr,4); char *d=malloc(sz+1); fseek(f,-12-(long)sz,SEEK_END); if(fread(d,1,sz,f)!=sz){fclose(f);return;} fclose(f); d[sz]=0;
  int cf=0,cl=0; for(char *p=d;*p;p++) if(*p=='\n'){ if(p[1]=='F')cf++; else if(p[1]=='L')cl++; }
  if(d[0]=='F')cf++; else if(d[0]=='L')cl++;
  F=calloc(cf+1,sizeof*F); Lt=calloc(cl+1,sizeof*Lt);
  const char *files[4096]; int nfi=0;
  for(char *p=d,*e;*p;p=e+1){ e=strchr(p,'\n'); if(!e) break; *e=0; char *f1[6]; int k=0; for(char *q=p;k<6;){ f1[k++]=q; q=strchr(q,'\t'); if(!q)break; *q++=0; }
    if(p[0]=='S'&&k>=2&&nfi<4096) files[nfi++]=f1[1];
    else if(p[0]=='F'&&k>=6){ fsym_t *s=&F[nF++]; s->rva=strtoul(f1[1],0,16); s->size=strtoul(f1[2],0,16); s->name=f1[3]; int fi=atoi(f1[4]); s->file=fi>=0&&fi<nfi?files[fi]:""; s->line=atoi(f1[5]); }
    else if(p[0]=='L'&&k>=4){ lsym_t *l=&Lt[nL++]; l->rva=strtoul(f1[1],0,16); int fi=atoi(f1[2]); l->file=fi>=0&&fi<nfi?files[fi]:"?"; l->line=atoi(f1[3]); } }
}
static NI void dump(void);
static NI LONG WINAPI seh(EXCEPTION_POINTERS *e){ static char b[96]; snprintf(b,sizeof b,"PLANTAGE exception 0x%08lx a l'adresse rva 0x%x",e->ExceptionRecord->ExceptionCode,rva(e->ExceptionRecord->ExceptionAddress)); crash_rva=rva(e->ExceptionRecord->ExceptionAddress); status=b; dump(); return EXCEPTION_CONTINUE_SEARCH; }
static NI void sigh(int s){ static char b[64]; snprintf(b,sizeof b,"PLANTAGE signal %d",s); if(!dumped){status=b; dump();} signal(s,SIG_DFL); raise(s); }
static NI void atex(void){ if(depth>0 && !errmsg) status="exit() appele hors de main"; dump(); }

static NI void init(void){
  st=-1; const char *e=getenv("OEF2WIMS_TRACE");
  if(e&&(!_stricmp(e,"off")||!strcmp(e,"0"))) return;
  if(e&&*e) snprintf(tpath,sizeof tpath,"%s",e);
  else { if(__argc<2||!strcmp(__argv[1],"table")) return; snprintf(tpath,sizeof tpath,"%s.trace",__argv[1]); }
  tf=fopen(tpath,"w"); if(!tf) return; setvbuf(tf,0,_IOFBF,1<<20);
  if((e=getenv("OEF2WIMS_TRACE_MAX"))) maxlines=atoll(e);
  if((e=getenv("OEF2WIMS_TRACE_RING"))&&atoi(e)>0) ringn=atoi(e);
  ring=calloc(ringn,sizeof*ring);
  QueryPerformanceFrequency(&fq); QueryPerformanceCounter(&t0); load_syms();
  SYSTEMTIME t; GetLocalTime(&t);
  fprintf(tf,"# oef2wims-trace (WIMS 4.31b) %04d-%02d-%02d %02d:%02d:%02d\n# commande :",t.wYear,t.wMonth,t.wDay,t.wHour,t.wMinute,t.wSecond);
  for(int i=0;i<__argc;i++) fprintf(tf," [%s]",__argv[i]);
  fprintf(tf,"\n# symboles : %d fonctions, %d lignes C%s\n# format : temps_ms  profondeur  ->fonction (fichier.c:ligne de definition) <- appelee depuis fichier.c:ligne  [OEF ligne:col]\n#          <-fonction  duree_ms\n",nF,nL,nF?"":" (TABLE ABSENTE)");
  st=1; atexit(atex); SetUnhandledExceptionFilter(seh); signal(SIGSEGV,sigh); signal(SIGFPE,sigh); signal(SIGILL,sigh); signal(SIGABRT,sigh);
}

NI void __cyg_profile_func_enter(void *fn,void *site){
  if(st==0) init(); if(st<0) return;
  double t=now(); int oef=oefline(curoff(),0);
  ncalls++; last_fn=fn; last_site=site;
  unsigned h=((unsigned)(size_t)fn>>4)&8191; while(cnt[h].fn&&cnt[h].fn!=fn) h=(h+1)&8191; cnt[h].fn=fn; cnt[h].n++;
  ring_t *r=&ring[ringi++%ringn]; r->fn=fn; r->site=site; r->t=t; r->depth=depth; r->oef=oef; r->ev=1;
  if(depth<1024){ stk[depth].fn=fn; stk[depth].site=site; stk[depth].t=t; stk[depth].oef=oef; }
  if(nlines<maxlines){ char a[64],b[160],c[200]; fprintf(tf,"%10.3f %3d %*s->%s (%s) <- %s",t,depth,depth*2,"",fname(fn,a,sizeof a),fdecl(fn,b,sizeof b),cline(rva(site)-1,c,sizeof c)); if(oef) fprintf(tf,"  [OEF %d]",oef); fputc('\n',tf); }
  else if(nlines==maxlines) fprintf(tf,"... limite OEF2WIMS_TRACE_MAX=%lld atteinte : suite non ecrite (compteurs et derniers appels conserves)\n",maxlines);
  nlines++; depth++; if(depth>maxdepth) maxdepth=depth;
}
NI void __cyg_profile_func_exit(void *fn,void *site){
  if(st<=0) return; depth--; double t=now();
  ring_t *r=&ring[ringi++%ringn]; r->fn=fn; r->site=site; r->t=t; r->depth=depth; r->oef=oefline(curoff(),0); r->ev=0;
  if(nlines<maxlines){ char a[64]; fprintf(tf,"%10.3f %3d %*s<-%s  %.3f ms\n",t,depth,depth*2,"",fname(fn,a,sizeof a),depth>=0&&depth<1024?t-stk[depth].t:0); }
  nlines++;
  if(depth==0) status="fin normale (retour de main)";
}
void oef_trace_input(const char *buf,int n){ if(st==0) init(); if(st<0||!buf||n<=0) return; srcn=n; src=malloc(n+1); memcpy(src,buf,n); src[n]=0; nl=malloc(sizeof(int)*(n+1)); for(int i=0;i<n;i++) if(src[i]=='\n') nl[nnl++]=i; }
void oef_trace_pos(const char *p){ if(st>0&&inpbuf&&p>=inpbuf&&p<inpbuf+srcn) cur_off=p-inpbuf; }
void oef_trace_directive(const char *name,const char *p){ if(st<=0) return; cur_dir=name; if(inpbuf&&p>=inpbuf&&p<inpbuf+srcn){ cur_off=dir_off=p-inpbuf; } int c,l=oefline(cur_off,&c); if(nlines<maxlines) fprintf(tf,"%10.3f %3d %*s## directive \\%s  [OEF %d:%d]\n",now(),depth,depth*2,"",name,l,c); }
void oef_trace_error(const char *msg){ if(st<=0) return; errmsg=msg; status="oef_error (code de sortie 1)"; }

static NI void oefctx(long off){
  int col,l=oefline(off,&col); if(!l||!src) return;
  for(int k=l-2;k<=l+1;k++){ if(k<1||k>nnl+1) continue; int s=k==1?0:nl[k-2]+1, e=k-1<nnl?nl[k-1]:srcn;
    fprintf(tf,"  %s %5d | %.*s\n",k==l?">>":"  ",k,e-s>200?200:e-s,src+s);
    if(k==l) fprintf(tf,"           %*s^\n",col-1,""); }
}
static NI void dump(void){
  char a[64]; if(dumped||st<=0) return; dumped=1; char b[160],c[220];
  fprintf(tf,"\n== RESUME ==\n");
  fprintf(tf,"statut: %s\n",status);
  if(errmsg) fprintf(tf,"message_erreur: %s\n",errmsg);
  if(last_fn){ fprintf(tf,"dernier_appel: %s (%s)\n",fname(last_fn,a,sizeof a),fdecl(last_fn,b,sizeof b)); fprintf(tf,"derniere_ligne_C: %s\n",cline(rva(last_site)-1,c,sizeof c)); }
  if(crash_rva) fprintf(tf,"ligne_C_du_plantage: %s\n",cline(crash_rva,c,sizeof c));
  long off=(badpar&&inpbuf&&badpar>=inpbuf&&badpar<inpbuf+srcn)?badpar-inpbuf:cur_off; int col,l=oefline(off,&col);
  if(l) fprintf(tf,"derniere_ligne_OEF: %d col %d%s\n",l,col,off==cur_off?"":" (position de l'erreur, badpar)");
  if(cur_dir){ int c2,l2=oefline(dir_off,&c2); fprintf(tf,"derniere_directive: \\%s (OEF ligne %d)\n",cur_dir,l2); }
  int nd=0; for(int i=0;i<8192;i++) if(cnt[i].fn) nd++;
  fprintf(tf,"appels_total: %lld\nfonctions_distinctes: %d\nprofondeur_max: %d\nduree_ms: %.3f\n",ncalls,nd,maxdepth,now());
  if(l){ fprintf(tf,"contexte_OEF:\n"); oefctx(off); }
  fprintf(tf,"== PILE D'APPELS A LA SORTIE (la plus recente en haut) ==\n");
  for(int i=(depth<1024?depth:1024)-1;i>=0;i--) fprintf(tf,"  #%-3d %s (%s) appelee depuis %s%s\n",i,fname(stk[i].fn,a,sizeof a),fdecl(stk[i].fn,b,sizeof b),cline(rva(stk[i].site)-1,c,sizeof c),"");
  if(depth<=0) fprintf(tf,"  (pile vide : main est terminee)\n");
  long long n=ringi<ringn?ringi:ringn; fprintf(tf,"== %lld DERNIERS EVENEMENTS ==\n",n);
  for(long long i=ringi-n;i<ringi;i++){ ring_t *r=&ring[i%ringn]; fprintf(tf,"  %10.3f %3d %*s%s%s <- %s",r->t,r->depth,r->depth*2,"",r->ev?"->":"<-",fname(r->fn,a,sizeof a),cline(rva(r->site)-1,c,sizeof c)); if(r->oef) fprintf(tf," [OEF %d]",r->oef); fputc('\n',tf); }
  fprintf(tf,"== COMPTEURS (appels par fonction) ==\n");
  { static struct { const char *n; long long c; } m[1024]; int nm=0;
    for(int i=0;i<8192;i++) if(cnt[i].fn){ const char *n=fname(cnt[i].fn,a,sizeof a); if(n==a) continue; int j; for(j=0;j<nm&&strcmp(m[j].n,n);j++); if(j==nm&&nm<1024){m[nm].n=n;m[nm].c=0;nm++;} if(j<nm) m[j].c+=cnt[i].n; }
    for(int k=0;k<nm;k++){ int bi=k; for(int j=k+1;j<nm;j++) if(m[j].c>m[bi].c) bi=j; if(bi!=k){ const char *tn=m[k].n; long long tc=m[k].c; m[k]=m[bi]; m[bi].n=tn; m[bi].c=tc; } fprintf(tf,"  %10lld  %s\n",m[k].c,m[k].n); } }
  fprintf(tf,"== FIN ==\n"); fflush(tf);
}
