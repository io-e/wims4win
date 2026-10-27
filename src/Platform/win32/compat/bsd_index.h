/* BSD index() (strings.h) used by the flex scanners of Misc/chemeq and
 * Misc/units-filter; absent from mingw-w64. Pre-included (-include). */
#ifndef WP_BSD_INDEX_H
#define WP_BSD_INDEX_H
#include <string.h>
#ifdef __cplusplus
static inline char *index(char *s, int c) { return strchr(s,c); }
static inline const char *index(const char *s, int c) { return strchr(s,c); }
#else
static inline char *index(const char *s, int c) { return strchr(s,c); }
#endif
#endif
