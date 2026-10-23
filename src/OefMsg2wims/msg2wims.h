#define elsechar '\001'
#define endifchar '\002'
#define nextchar '\003'
#define whilechar '\004'
#define MAX_PARM 10
#define MAX_PARAM   1024

extern int inlen, primserial, prepcnt;
void msg_error(char *s);
void substdoc(char *p);
void bailout(int i1, int i2, char *msg);
char *substit(char *p);
int _scmp(const void *p1, const void *p2);
char *find_tag_end(char *p);

/* from compare.c */
int _check_compare(char *p, int lvl);
int check_compare(char *p);

void parmprep(char *p,int ptype);
void def(char *p);

/* from doc.c */
extern char primbuf[MAX_LINELEN+1];
extern int primcnt;
extern char *primitive[256];
char *doccheck(char *p);

#include "sp.h"
