/*  wims_platform.h -- portable process / pipe layer for WIMS (POSIX + Win32)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Scope (phase 1): what src/Interfaces/common.c needs:
 *   - a bidirectional channel (parent -> child stdin, child stdout -> parent)
 *   - spawning a child with optional file redirections and an isolation mode
 *   - non-blocking multiplexing of read/write with a timeout (select() model)
 *   - liveness test, kill, wait
 *   - helpers: bounded read (fsizelim), CR stripping
 *
 *  Security rule: an isolation mode that cannot be honoured is REFUSED
 *  (WP_E_NOISOLATION); it is never silently ignored.
 */
#ifndef WIMS_PLATFORM_H
#define WIMS_PLATFORM_H

#include <stddef.h>

#ifdef _WIN32
#  include <windows.h>
typedef HANDLE wp_fd;
/* job: Job Object owning the child and all its descendants */
typedef struct { HANDLE h; DWORD id; HANDLE job; } wp_pid;
#  define WP_FD_INVALID INVALID_HANDLE_VALUE
#  define WP_STRIP_CR_DEFAULT 1   /* Windows CAS write CRLF */
#else
#  include <sys/types.h>
typedef int wp_fd;
typedef struct { pid_t id; } wp_pid;
#  define WP_FD_INVALID (-1)
#  define WP_STRIP_CR_DEFAULT 0   /* keep Linux behaviour byte-identical */
#endif

/* error codes (negative) */
#define WP_OK             0
#define WP_E_SYS         -1   /* system call failed */
#define WP_E_NOISOLATION -2   /* requested isolation not available: refused */
#define WP_E_ARG         -3

/* isolation modes requested by the caller */
typedef enum {
  WP_ISO_NONE = 0,     /* no isolation (only for trusted helpers) */
  WP_ISO_DROP_PRIV,    /* POSIX: setreuid/setregid to real ids before exec */
  WP_ISO_WRAPPER       /* argv[0] is the sandbox wrapper (bin/ch..root);
                          Win32: requires a working sandbox, else refused */
} wp_isolation;

/* bidirectional channel: in = parent writes, child reads; out = child writes */
typedef struct {
  wp_fd in_r, in_w;
  wp_fd out_r, out_w;
#ifdef _WIN32
  /* in_w is the server end of a named pipe opened FILE_FLAG_OVERLAPPED:
   * one write may be in flight, its bytes are owned by wbuf. */
  OVERLAPPED wov;
  char *wbuf;
  int wpending;
#endif
} wp_chan;

int  wp_chan_open(wp_chan *c);
void wp_chan_close_parent_write(wp_chan *c);   /* close in_w (EOF to child) */

/* Spawn argv[0] (searched in PATH if it contains no '/').
 * inf/outf/errf: NULL = use channel (in/out) / inherit (err).
 * On success fills *pid and returns WP_OK; the child never returns.
 * In the child, if exec fails, "<progname> not_INStalled" is written to stderr
 * and the child exits with 127 (historical WIMS behaviour). */
int wp_spawn(wp_chan *c, char *const argv[], const char *inf,
             const char *outf, const char *errf, wp_isolation iso,
             const char *progname, wp_pid *pid);

/* Wait for termination (blocking). Returns 1 if the child was reaped. */
int wp_wait(wp_pid *pid, int *status);
/* 1 = process exists, 0 = no such process */
int wp_alive(wp_pid *pid);
/* hard kill (SIGKILL / TerminateProcess). Returns 0 on success. */
int wp_kill(wp_pid *pid);

/* select() model on the channel.
 * want_read: watch out_r ; want_write: watch in_w.
 * Returns >0 if something is ready, 0 on timeout, <0 on error.
 * *can_read / *can_write are set accordingly. */
int wp_poll(wp_chan *c, int want_read, int want_write, long timeout_usec,
            int *can_read, int *can_write);

/* read at most cap bytes from out_r (cap is the remaining fsizelim room).
 * Returns bytes read, 0 on EOF, <0 on error. */
long wp_read(wp_chan *c, char *buf, long cap);
/* write len bytes to in_w; returns bytes written or <0 */
long wp_write(wp_chan *c, const char *buf, long len);

/* Remove every '\r' from buf[0..len) in place, keep a terminating 0.
 * Returns the new length. */
long wp_strip_cr(char *buf, long len);

/* Execution job (Win32). Subsequent wp_spawn() put the child in a Job Object
 * named Local\\wims-job-<name> (name sanitised; NULL = anonymous job) with
 * KILL_ON_JOB_CLOSE and, if >0, a per-process CPU limit (seconds) and a
 * per-process memory limit (MB): the Windows counterpart of the RLIMIT_CPU /
 * RLIMIT_AS inherited from wims on POSIX.
 * POSIX: no-op (limits come from setrlimit in wims, cleanup from exec.pid). */
void wp_set_job(const char *name, long cpu_sec, long mem_mb);
/* Kill every process of the named job (replaces "kill pids of exec.pid").
 * Returns number of jobs terminated (0/1), WP_E_ARG on POSIX. */
int  wp_job_kill(const char *name);

/* wims side of multiexec, see wp_mx.c (fixed=0: historical behaviour of
 * calc.c:229-258 including its timeout bug; fixed=1: corrected). */
long wp_mx_exchange(wp_chan *c, const char *parm, const char *marker,
                    char *p, long cap, int fixed);
/* 1 if fd has data (or EOF) within timeout_usec, 0 otherwise */
int  wp_fd_readable(wp_fd fd, long timeout_usec);

/* Resident (multiexec) spawn: child stdin/stdout = channel, stderr = err_w
 * (write end of a wp_pipe_open pipe). lowprio: nice(10) / BELOW_NORMAL. */
int  wp_spawn_mx(wp_chan *c, char *const argv[], wp_fd err_w, wp_isolation iso,
                 int lowprio, const char *progname, wp_pid *pid);
/* plain one-way pipe; the read end is not inherited by children */
int  wp_pipe_open(wp_fd *r, wp_fd *w);
long wp_fd_read(wp_fd fd, char *buf, long cap);
#ifdef _WIN32
/* 1 if the sandbox wrapper (ch..root.exe) is present and passes --selftest */
int  wp_sandbox_available(const char *wrapper);
#endif

/* platform name, for diagnostics */
const char *wp_platform_name(void);

#endif
