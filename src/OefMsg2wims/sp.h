void parmprep(char *p,int ptype);
extern char *setpre;
void sp_asis(char *p, int ptype);
void sp_evalue(char *p, int ptype);
void sp_solve(char *p, int ptype);
void sp_diff(char *p, int ptype);
void sp_det(char *p, int ptype);
void sp_int(char *p, int ptype);
void sp_htmlmath(char *p, int ptype);
void sp_teximg(char *p, int ptype);
void sp_texmath(char *p, int ptype);
void sp_maxima(char *p, int ptype);
void sp_yacas(char *p, int ptype);
void sp_pari(char *p, int ptype);
void sp_simplify(char *p, int ptype);
void sp_slib(char *p, int ptype);
void sp_draw(char *p, int ptype);
void sp_canvasdraw(char *p, int ptype);
void sp_shuffle(char *p, int ptype);
void sp_positionof(char *p, int ptype);
void sp_random(char *p, int ptype);
void sp_pickone(char *p, int ptype);

void sp_item(char *p, int ptype);
void sp_items(char *p, int ptype);
void sp_randitem(char *p, int ptype);
void sp_column(char *p, int ptype);
void sp_row(char *p, int ptype);
void sp_rows(char *p, int ptype);
void sp_randrow(char *p, int ptype);
void sp_mathexp_cut(char *p, int ptype);
void sp_wims(char *p, int ptype);

extern int prepcnt;
extern FILE *outf;
extern char *primitive_dir;
enum {pt_int, pt_rat, pt_real, pt_complex, pt_func, pt_text, pt_matrix};
extern struct specialfn {
    char *name;
    void (*processor)(char *p, int ptype);
} specialfn[];

extern int specialfn_no;
extern int prepcnt;
extern void (*sp_error)(char *s);
