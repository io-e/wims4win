/* faux python pour tester --validator sous Wine : affiche <fichier>.valout (sortie réelle de oef_validate.py) */
#include <stdio.h>
#include <string.h>
int main(int c,char**v){ char p[1024]; snprintf(p,sizeof p,"%s.valout",v[c-1]); FILE*f=fopen(p,"rb"); if(!f) return 3; char b[4096]; size_t n; int bad=0; while((n=fread(b,1,sizeof b,f))>0){ fwrite(b,1,n,stdout); if(strstr(b,": erreur:")) bad=1; } fclose(f); return bad; }
