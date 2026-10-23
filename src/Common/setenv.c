#include "../config.h"

#ifndef HAVE_SETENV
#include <stdlib.h>
#include <stdio.h>
extern void *xmalloc(size_t n);
int setenv(const char *name, const char *value, int overwrite)
{
  char *s;
  if (!overwrite && getenv(name)) return 0;
  s = xmalloc(2 + strlen(name) + strlen(value));
  sprintf(s, "%s=%s", name,value);
  return putenv(s);
}

/* putenv("FOO=") should remove FOO from environment */
void unsetenv(const char *name) { setenv(name,"",1); }
#endif
