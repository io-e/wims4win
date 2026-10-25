/* from oef2wims.c */

extern char *badpar, *inpbuf;
extern char *mdef; /* external definition */
extern int varcnt, deftag;
extern FILE *outf;

#define elsechar '\001'
#define endifchar '\002'
#define nextchar '\003'
#define whilechar '\004'
#define MAX_PARM 10
#define MAX_PROMPTLEN 40
#define MAX_PARAM   1024
#define executed_str "__EXECUTED_1692754_EXERCISE__"
extern int choicecnt, answercnt, condans, conditioncnt;
extern int embedcnt;
extern char *primitive_dir, *format;
/* from oeflines.c */

extern struct param {
    char *name;
    int serial;
    short int type, save;
} param[];

void oef_error(char *s);
void subst(char *p);
void repsubst(char *p);
void replace_newline(char *p);
char *substit(char *p);
int checkparentheses(char *p, int style);

/* from compare.c */
int _check_compare(char *p, int lvl);
int check_compare(char *p);

/* from outexec.c */
void out_exec(char *s1, char *s2);

/* from process.c */
void p_answer(char *p[MAX_PARM]);
void p_choice(char *p[MAX_PARM]);
void p_computeanswer(char *p[MAX_PARM]);
void p_precision(char *p[MAX_PARM]);
void p_css(char *p[MAX_PARM]);
void p_description(char *p[MAX_PARM]);
void p_observation(char *p[MAX_PARM]);
void p_range(char *p[MAX_PARM]);
void p_language(char *p[MAX_PARM]);
void p_statement(char *p[MAX_PARM]);
void p_int(char *p[MAX_PARM]);
void p_rational(char *p[MAX_PARM]);
void p_real(char *p[MAX_PARM]);
void p_complex(char *p[MAX_PARM]);
void p_func(char *p[MAX_PARM]);
void p_text(char *p[MAX_PARM]);
void p_matrix(char *p[MAX_PARM]);
void p_parm(char *p[MAX_PARM]);
void p_if(char *p[MAX_PARM]);
void p_ifval(char *p[MAX_PARM]);
void p_else(char *p[MAX_PARM]);
void p_endif(char *p[MAX_PARM]);
void p_endwhile(char *p[MAX_PARM]);
void p_while(char *p[MAX_PARM]);
void p_for(char *p[MAX_PARM]);
void p_condition(char *p[MAX_PARM]);
void p_conditions(char *p[MAX_PARM]);
void p_credits(char *p[MAX_PARM]);
void p_help(char *p[MAX_PARM]);
void p_feedback(char *p[MAX_PARM]);
void p_hint(char *p[MAX_PARM]);
void p_mdef(char *p[MAX_PARM]);
void p_next(char *p[MAX_PARM]);
void p_nextstep(char *p[MAX_PARM]);
void p_latex(char *p[MAX_PARM]);
void p_solution(char *p[MAX_PARM]);
void p_steps(char *p[MAX_PARM]);
void p_plot(char *p[MAX_PARM]);
void p_header(char *p[MAX_PARM]);
void p_wims(char *p[MAX_PARM]);
void empty(char *p[MAX_PARM]);

extern char vbuf_statement[MAX_LINELEN+1];
extern char vbuf_hint[MAX_LINELEN+1];
extern char vbuf_help[MAX_LINELEN+1];
extern char vbuf_solution[MAX_LINELEN+1];
extern char vbuf_latex[MAX_LINELEN+1];
/* from sp.c */

#include "sp.h"
