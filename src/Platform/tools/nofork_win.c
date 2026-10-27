/* wp_no_fork() for standalone helpers not linked with the platform layer:
 * fork() is "(wp_no_fork(), -1)" in posix_shim.h. GPL v2+. */
#include <errno.h>
int wp_no_fork(void) { errno = ENOSYS; return -1; }
