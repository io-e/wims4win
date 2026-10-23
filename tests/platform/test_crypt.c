#include <stdio.h>
#include <string.h>
char *crypt(const char*,const char*);
#ifdef _WIN32
char *crypt_wims_new(const char*);
int crypt_wims_check(const char*,const char*);
#endif
int main(void){
  int fail=0;
  #define T(n,c) do{ if(c) printf("PASS %s\n",n); else {printf("FAIL %s\n",n);fail++;}}while(0)
  T("des password/ab", strcmp(crypt("password","ab"),"abJnggxhB/yWI")==0);
  T("des hello/xy", strcmp(crypt("hello","xy"),"xyJ5nqog.skwc")==0);
  T("des verify", strcmp(crypt("password","abJnggxhB/yWI"),"abJnggxhB/yWI")==0);
  T("des wrong", strcmp(crypt("wrong","abJnggxhB/yWI"),"abJnggxhB/yWI")!=0);
#ifdef _WIN32
  { char *h=crypt_wims_new("secret");
    T("bcrypt new format prefix", h && strncmp(h,"$wims$v1$",9)==0);
    T("bcrypt check ok", crypt_wims_check("secret",h));
    T("bcrypt check bad", !crypt_wims_check("wrong",h));
    T("des via check helper", crypt_wims_check("password","abJnggxhB/yWI"));
  }
#endif
  printf("== %s ==\n", fail?"FAILED":"OK");
  return fail?1:0;
}
