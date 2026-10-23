/* from dicsort.c */

char *strip_trailing_spaces2(char *p);
void singlespace2(char *p);
void string_modify3(char *start, char *bad_beg, char *bad_end, char *good,...);

/* from suffix.c */

void suffix_dic(char *sdicname);
void suffix_translate(char *p);
extern char *sufbuf;
void suffix(char *p, char *sdicname);

/* from translator_.c */
extern int suffixcnt;

