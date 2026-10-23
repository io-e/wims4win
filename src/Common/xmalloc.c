#include <stdio.h>
#include <stdlib.h>
void *xmalloc(size_t n)
{
    void *p = malloc(n);
    if(p==NULL) {
        fprintf(stderr,"Malloc failure."); exit(1);
    }
    return p;
}
