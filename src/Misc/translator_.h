#ifndef _translator_
#define _translator_

extern struct entry {
    unsigned char *original, *replace;
    int olen,earlier;
} entry[];

extern int entry_size;
extern int entrycount;
extern char* dicbuf;
extern int unknown_type;
enum { unk_delete, unk_leave, unk_replace};

void singlespace2(char *p);
void translate(char *p);
void prepare_dic(char *fname);

#endif
