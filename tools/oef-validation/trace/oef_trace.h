/* oef_trace.h : hooks OEF_TRACE (build oef2wims-trace.exe uniquement). Inclus via -include. */
#ifndef OEF_TRACE_H
#define OEF_TRACE_H
void oef_trace_input(const char *buf, int n);
void oef_trace_pos(const char *p);
void oef_trace_directive(const char *name, const char *p);
void oef_trace_error(const char *msg);
#define OEF_TRACE_INPUT(b,n)      oef_trace_input((b),(n))
#define OEF_TRACE_POS(p)          oef_trace_pos(p)
#define OEF_TRACE_DIRECTIVE(n,p)  oef_trace_directive((n),(p))
#define OEF_TRACE_ERROR(s)        oef_trace_error(s)
#endif
