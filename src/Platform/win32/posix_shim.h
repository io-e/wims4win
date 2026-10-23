/* Minimal POSIX shims for compiling wims.cgi under mingw-w64. GPL v2+. */
#ifndef WP_POSIX_SHIM_H
#define WP_POSIX_SHIM_H
#ifdef _WIN32
#ifndef NOMINMAX
# define NOMINMAX 1
#endif
#ifndef WIN32_LEAN_AND_MEAN
# define WIN32_LEAN_AND_MEAN 1
#endif
#include <winsock2.h>
#include <ws2tcpip.h>
#include <io.h>
#include <process.h>
#include <direct.h>
#include <sys/utime.h>
#include <sys/stat.h>
#ifdef max
# undef max
#endif
#ifdef min
# undef min
#endif
#ifndef _WINSOCKAPI_ /* already included winsock2 */
#endif
/* sys/socket.h / sys/un.h stand-ins (AF_UNIX optional on Win10+) */
#define shutdown_socket closesocket
struct sockaddr_un { short sun_family; char sun_path[108]; };
#ifndef AF_UNIX
# define AF_UNIX 1
#endif
/* unistd-ish */
#ifndef getpid
# define getpid _getpid
#endif
#ifndef sleep
# define sleep(s) Sleep((DWORD)(s)*1000)
#endif
/* alarms: implemented in wp_alarm.c via CreateTimerQueueTimer */
typedef void (*wp_sighandler_t)(int);
#define SIGALRM 14
#define SIG_ERR ((wp_sighandler_t)-1)
wp_sighandler_t wp_signal(int sig, wp_sighandler_t h);
unsigned wp_alarm(unsigned sec);
#define signal(sig,h) wp_signal(sig,h)
#define alarm(sec)    wp_alarm(sec)
/* no fork: callers must use the platform layer */
#define fork() (wp_no_fork(), -1)
int wp_no_fork(void);
/* symlink -> CreateSymbolicLinkA (may fail without SeCreateSymbolicLink) */
int wp_symlink(const char *t, const char *l);
#define symlink wp_symlink
/* crypt: stub returning constant (password check needs OpenSSL later) */
char *crypt(const char *key, const char *salt);
/* setreuid family: no-ops that succeed (isolation handled by wp_spawn) */
#define getuid()  0
#define geteuid() 0
#define getgid()  0
#define getegid() 0
#define setreuid(a,b) 0
#define setregid(a,b) 0
#define nice(n) 0
/* waitpid: not used once execredirected is on the platform layer */
#define waitpid(p,s,o) (-1)
struct rlimit { unsigned long rlim_cur, rlim_max; };
#define RLIMIT_CPU 0
#define RLIMIT_FSIZE 1
#define RLIMIT_AS 2
#define RLIMIT_DATA 3
#define RLIMIT_STACK 4
#define RLIMIT_CORE 5
#define RLIMIT_NOFILE 6
int getrlimit(int, struct rlimit *);
int setrlimit(int, const struct rlimit *);
/* process / filesystem bits missing from mingw */
#ifndef S_ISLNK
# define S_ISLNK(m) 0
#endif
#ifndef S_ISSOCK
# define S_ISSOCK(m) 0
#endif
#ifndef SIGKILL
# define SIGKILL 9
#endif
int kill(int pid, int sig);
int getppid(void);
struct rusage {
  struct timeval ru_utime, ru_stime;
  long ru_maxrss, ru_ixrss, ru_idrss, ru_isrss, ru_minflt, ru_majflt;
  long ru_nswap, ru_inblock, ru_oublock, ru_msgsnd, ru_msgrcv;
  long ru_nsignals, ru_nvcsw, ru_nivcsw;
};
int getrusage(int who, struct rusage *u);
int pipe(int fildes[2]);
#include <time.h>
int nanosleep(const struct timespec *req, struct timespec *rem);
#endif
#endif
