/* Optional new WIMS password format using Windows BCrypt PBKDF2-SHA256.
 * Traditional DES crypt remains the default (wp_descrypt.c) for compatibility
 * with existing password files. Callers still use crypt(); this file only
 * adds crypt_wims_new() for future migration and a verify helper.
 * GPL v2 or later. */
#ifdef _WIN32
#include <stdio.h>
#include <string.h>
#include <windows.h>
#include <bcrypt.h>

char *crypt(const char *key, const char *salt); /* from wp_descrypt.c */

/* Create a new-format hash: $wims$v1$<iter>$<salthex>$<dkhex> */
char *crypt_wims_new(const char *key)
{
  static char out[160];
  BCRYPT_ALG_HANDLE alg=NULL; UCHAR salt[8], dk[32]; DWORD i; NTSTATUS st;
  if(!key) key="";
  BCryptGenRandom(NULL,salt,sizeof(salt),BCRYPT_USE_SYSTEM_PREFERRED_RNG);
  st=BCryptOpenAlgorithmProvider(&alg,BCRYPT_SHA256_ALGORITHM,NULL,BCRYPT_ALG_HANDLE_HMAC_FLAG);
  if(!BCRYPT_SUCCESS(st)){ snprintf(out,sizeof(out),"*"); return out; }
  st=BCryptDeriveKeyPBKDF2(alg,(PUCHAR)key,(ULONG)strlen(key),salt,sizeof(salt),10000,dk,32,0);
  BCryptCloseAlgorithmProvider(alg,0);
  if(!BCRYPT_SUCCESS(st)){ snprintf(out,sizeof(out),"*"); return out; }
  snprintf(out,sizeof(out),"$wims$v1$10000$");
  { char *p=out+strlen(out); for(i=0;i<8;i++) sprintf(p+2*i,"%02x",salt[i]); p[16]='$'; p+=17;
    for(i=0;i<32;i++) sprintf(p+2*i,"%02x",dk[i]); }
  return out;
}

/* Verify key against either traditional crypt hash or $wims$v1$ hash. */
int crypt_wims_check(const char *key, const char *hash)
{
  if(!hash||!*hash) return 0;
  if(strncmp(hash,"$wims$v1$",9)==0){
    unsigned iter=0; unsigned char salt[8], dk[32], got[32]; char salthex[17];
    BCRYPT_ALG_HANDLE alg=NULL; NTSTATUS st; DWORD i;
    if(sscanf(hash,"$wims$v1$%u$%16s",&iter,salthex)!=2 || iter==0 || iter>1000000) return 0;
    { char *p=strchr(hash+9,'$'); if(!p) return 0; p=strchr(p+1,'$'); if(!p) return 0; p++;
      for(i=0;i<8;i++){ unsigned v; if(sscanf(salthex+2*i,"%2x",&v)!=1) return 0; salt[i]=(unsigned char)v; }
      for(i=0;i<32;i++){ unsigned v; if(sscanf(p+2*i,"%2x",&v)!=1) return 0; dk[i]=(unsigned char)v; }
    }
    st=BCryptOpenAlgorithmProvider(&alg,BCRYPT_SHA256_ALGORITHM,NULL,BCRYPT_ALG_HANDLE_HMAC_FLAG);
    if(!BCRYPT_SUCCESS(st)) return 0;
    st=BCryptDeriveKeyPBKDF2(alg,(PUCHAR)key,(ULONG)strlen(key?key:""),salt,8,iter,got,32,0);
    BCryptCloseAlgorithmProvider(alg,0);
    if(!BCRYPT_SUCCESS(st)) return 0;
    return memcmp(got,dk,32)==0;
  }
  { const char *r=crypt(key,hash); return r && strcmp(r,hash)==0; }
}
#endif
