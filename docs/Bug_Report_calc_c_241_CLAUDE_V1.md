<!-- CLAUDE_V1 -->
# Bug Report: Infinite wait in multiexec read loop (calc.c:241)

| Field          | Value                                                         |
|----------------|---------------------------------------------------------------|
| **File**       | `src/calc.c`, function `calc_exec()`                          |
| **Line**       | 241 (upstream trunk r19040)                                   |
| **Version**    | WIMS 4.33a, SVN trunk r19040                                  |
| **Severity**   | Medium — hangs the CGI process when a CAS child stalls        |
| **Affects**    | All platforms (Linux and Windows)                             |
| **Found by**   | wims4win porting project (io-e/wims4win)                      |

## Description

The multiexec read loop in `calc_exec()` has a timeout mechanism that can
never fire because the timeout counter is reset on every iteration.

## The bug

```c
/* calc.c, lines 239-251 (upstream r19040) */
while(k>0) {
    FD_ZERO(&rset); FD_SET(fd,&rset);
    t.tv_sec=0; t.tv_usec=100000; v1=0; v2=300; /* 3 seconds */
    if(select(fd+1,&rset,NULL,NULL,&t)<=0) {
        v1++; if(v1>=v2) break;
    else continue;
    }
    j=read(fd,p+i,k);
    if(j<0 || j>k) break;
    i+=j; k-=j; p[i]=0;
    pp=strstr(p,multiexec_random);
    if(pp) {*pp=0; break;}
}
```

**Problem:** `v1=0; v2=300;` on line 241 is **inside** the `while` loop
body. Every iteration resets `v1` to 0. The increment `v1++` on the next
line has no lasting effect — `v1` will be 0 again on the next iteration.

The condition `v1>=v2` (i.e., `1 >= 300`) is therefore **always false**.
The intended 30-second timeout (300 × 100 ms) never triggers.

## Consequence

If a CAS process (PARI, Maxima, Octave, GAP) hangs without producing
output or the `multiexec_random` sentinel, the WIMS CGI process blocks
indefinitely on `select()` / `read()`. It can only be killed by the
overall `limtimex` timeout or externally. This wastes server resources
and can cause user-visible hangs.

In practice the bug is masked because:
1. CAS processes usually reply quickly, so the sentinel is found.
2. The overall request timeout (`limtimex`) eventually kills the process.
3. If `read()` returns 0 (EOF on pipe), `j=0` passes the `j<0 || j>k`
   check, and `k -= 0` loops forever (secondary infinite loop).

## Steps to reproduce

1. Start a WIMS server with multiexec enabled for any CAS.
2. Send a command that causes the CAS to hang (e.g., a computation
   that never terminates and produces no output).
3. Observe that the multiexec loop never breaks on timeout — it waits
   until the global `limtimex` kills the request.

To verify with a debugger: set a breakpoint at line 243 (`v1++`) and
observe that `v1` is always 1 after the increment, never accumulating.

## Proposed fix

Move `v1=0; v2=300;` **before** the `while` loop:

```c
*p=0; i=0; k=MAX_LINELEN; fd=mxtab[multiexec_index].pipe_stdout[0];
v1=0; v2=300;                                /* ← moved here */
while(k>0) {
    FD_ZERO(&rset); FD_SET(fd,&rset);
    t.tv_sec=0; t.tv_usec=100000;            /* ← v1/v2 removed */
    if(select(fd+1,&rset,NULL,NULL,&t)<=0) {
        v1++; if(v1>=v2) break;
        else continue;
    }
    v1=0;                                     /* ← reset on success */
    j=read(fd,p+i,k);
    /* ... rest unchanged ... */
}
```

**Additional consideration:** resetting `v1=0` after a successful
`read()` makes the timeout count only consecutive idle polls (3 seconds
of silence), not cumulative. Whether to reset on success or not depends
on the intended semantic — "3 seconds total" vs. "3 seconds of silence."
Both are defensible; resetting on success is more forgiving for slow CAS
output.

**Secondary issue:** when `read()` returns 0 (EOF), `j=0` passes the
`j<0 || j>k` check, and the loop spins forever with `k` never
decreasing. Adding `if(j==0) break;` after the read would fix this.

## Impact assessment

- **Severity: Medium.** The bug has likely been present since multiexec
  was introduced. It is masked by `limtimex` but increases resource waste
  on a server under load.
- **Risk of fix: Low.** Moving the initialization is a one-line change
  with no side effects.
- **Platforms affected: All.** The bug is in platform-independent code.
  The wims4win Windows port works around it via `wp_mx_exchange()` in the
  platform abstraction layer.

## Workaround (wims4win)

The Windows port replaces the entire multiexec loop with
`wp_mx_exchange()` (in `src/Platform/wp_mx.c`), which uses overlapped
I/O and a proper timeout. When `WIMS_MX_FIX_TIMEOUT` is defined, the
fixed timeout logic is used; otherwise the upstream behavior is
replicated for parity testing.

See: `src/calc.c:246-262` (ported version), commit `ec8f410` in
io-e/wims4win.
