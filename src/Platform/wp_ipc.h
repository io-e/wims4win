/* Daemon IPC (wimslogd <-> wims). GPL v2+.
 * POSIX: AF_UNIX stream socket (historical path).
 * Win32: named pipe \\.\pipe\wims-logd (AF_UNIX Winsock optional later).
 * Wire format unchanged: int32 LE length + payload bytes. */
#ifndef WP_IPC_H
#define WP_IPC_H
#include <stddef.h>
#ifdef _WIN32
# include <windows.h>
typedef HANDLE wp_ipc_fd;
# define WP_IPC_INVALID INVALID_HANDLE_VALUE
#else
typedef int wp_ipc_fd;
# define WP_IPC_INVALID (-1)
#endif

/* Client: connect to daemon, send buf (payload starts at buf+sizeof(int),
 * length field filled here), read reply into same buffer. Returns total
 * bytes of reply (length+sizeof(int)) or <0 on error. */
int wp_ipc_transact(const char *path, char *buf, int bufsize);

/* Server listen end (daemon). path is the historical sockfile; on Win32 the
 * basename is mapped to a named pipe. */
typedef struct {
  wp_ipc_fd listen_fd;
  char path[280];
} wp_ipc_server;

int  wp_ipc_listen(wp_ipc_server *s, const char *path);
/* Wait up to timeout_usec for a client; returns connected fd or WP_IPC_INVALID */
wp_ipc_fd wp_ipc_accept(wp_ipc_server *s, long timeout_usec);
int  wp_ipc_read_msg(wp_ipc_fd fd, char *buf, int bufsize); /* fills length+payload */
int  wp_ipc_write_msg(wp_ipc_fd fd, const char *buf, int total); /* length already set */
void wp_ipc_close(wp_ipc_fd fd);
void wp_ipc_close_server(wp_ipc_server *s);

/* Synthetic load average string (3 floats) for Win32; on POSIX reads /proc. */
void wp_loadavg(char *out, size_t cap);

/* Start a detached daemon process (replaces double-fork+exec). argv[0]=exe. */
int wp_daemon_spawn(char *const argv[], const char *pidfile);

#endif
