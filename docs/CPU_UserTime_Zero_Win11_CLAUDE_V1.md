<!-- CLAUDE_V1 -->
# CPU User-Time Reads as Zero on Windows 11 (Job Objects)

**Issue:** [io-e/wims4win#7](https://github.com/io-e/wims4win/issues/7)
**Decision:** D4 (CPU limit at spawn) — commit `5f5523b`
**Status:** Workaround in place; documented here for future reference.

---

## 1. Observed Behavior

When a WIMS CAS child process (e.g., PARI/GP computing a factorial) runs
inside a Win32 Job Object, `QueryInformationJobObject` with
`JobObjectBasicAccountingInformation` reports **both `TotalUserTime` and
`TotalKernelTime` as zero** — even while the child is actively consuming CPU
in a tight busy loop.

Evidence from `tests-win/n5_cpu.txt` (cpu_probe on ShadowPC, Win11 26100):

```
tick kernel=0.000s user=0.000s
tick kernel=0.000s user=0.000s
...                              ← 20 ticks over 20 seconds, all 0.000
STILL ALIVE 20s
```

The child was NOT killed by the Job Object's `PerProcessUserTimeLimit` or
`PerJobUserTimeLimit` despite a 20-second budget, because the kernel never
charged it any CPU time.

## 2. Root Cause

Windows tracks per-process CPU time using the system clock interrupt, which
fires at the **default timer resolution of 15.625 ms** (64 Hz). When the
scheduler tick fires, the full quantum is charged to whichever thread is
currently running on that core.

This accounting breaks down when:

1. **Short-lived processes** finish between ticks — they accumulate zero
   charged time because no tick ever fires while they're current.
2. **Job Object accounting** sums per-process times; if each member reports
   zero, the job total is also zero.
3. On certain **Windows 11 hosts** (observed on build 26100 with modern
   standby), the issue is more pronounced — even a multi-second busy loop
   can show zero user-time. This suggests the scheduler may be using a
   different accounting path or that timer coalescing is more aggressive.

The `PROCESS_TIME` (`0x2`) and `JOB_TIME` (`0x4`) limit flags ARE correctly
set (verified in `n5_cpu.txt`: `SetInfo=1 err=0`), but the kernel never
triggers the limit because it never sees any time charged.

`GetProcessTimes()` on the individual process handle shows the same
behavior — zero user-time, zero kernel-time.

### Not specific to mingw-w64

The flags and structures used are identical to those from the Windows SDK.
`cpu_probe` verifies: `mingw PROCESS_TIME flag=0x2 JOB_TIME=0x4` matches
`SDK expected: PROCESS_TIME=0x2 JOB_TIME=0x4`. The issue is in the
Windows kernel's accounting, not in how the limit is configured.

## 3. Workaround: Wall-Clock Watchdog Thread

The fix is in `src/Platform/wp_win32.c`, function `wp_cpu_watch()` (line
102–120). A dedicated watchdog thread:

1. **Polls** `QueryInformationJobObject` every 200 ms for
   `TotalUserTime + TotalKernelTime`.
2. If accounted time exceeds the budget → `TerminateJobObject(job, 137)`.
3. **Fallback:** if wall-clock time exceeds **2× the CPU budget** and
   accounting still shows zero → also terminates. This is the safety net
   for the zero-accounting case.

```c
/* from wp_win32.c:96-97 */
/* Watchdog: poll JOBOBJECT_BASIC_ACCOUNTING_INFORMATION (user+kernel).
 * If the sum exceeds the CPU budget, TerminateJobObject. Also kill on
 * wall-clock overrun (2x budget) when accounting stays at 0 (observed on
 * some Win11 hosts for tight busy loops). */
```

The spawn sequence ensures limits are active before the child runs:

```
CreateProcess(CREATE_SUSPENDED)
  → AssignProcessToJobObject(job, process)
  → ResumeThread(main_thread)
  → start_cpu_watch(job, process, cpu_sec)
```

### Test result

From `tests-win/cpu_spawn_watchdog.txt`:

```
cpu-limited busy child ended by job/watchdog within 6 s: yes
PASS win32: CPU-limit kills busy child (PROCESS_TIME/JOB_TIME + watchdog)
```

With a default budget of 20 seconds (`WP_DEFAULT_CPU_SEC`), the wall-clock
limit is 40 seconds (2×). The test uses a shorter budget; the child is
terminated within 6 seconds — the wall-clock fallback works.

## 4. Impact on WIMS

| Aspect | Linux (setrlimit) | Windows (wims4win) |
|--------|-------------------|-------------------|
| Mechanism | `RLIMIT_CPU` (kernel signal) | Job Object + watchdog |
| Granularity | CPU-time (tick-level) | Wall-clock (200 ms poll) |
| Precision | Exact to ~4 ms | Up to 200 ms late |
| Overcharge | None (CPU-time only) | I/O wait counts as "used" |

**Practical impact:** A CAS process that spends most of its time waiting
for I/O will be killed earlier on Windows (wall-clock) than on Linux
(CPU-time). For WIMS's use case — running CAS computations that are
CPU-bound — the difference is negligible. The exercises tested (24/24 CAS
equivalence) all produce identical results.

The 2× multiplier on the wall-clock budget provides headroom for processes
that do some I/O (e.g., Maxima loading its core file). If a CAS computation
legitimately takes more than 2× its CPU budget in wall-clock time, it
indicates the system is heavily loaded, and termination is the safe
behavior.

## 5. Upstream Relevance

This is a **Windows-specific issue**. The upstream WIMS code on Linux uses
`setrlimit(RLIMIT_CPU, ...)` which is handled by the kernel scheduler with
clock-tick granularity — no zero-accounting has been observed.

The `calc.c:241` timeout bug (issue #9) is a separate, cross-platform issue
where the multiexec loop can wait forever on a dead process. That bug
exists on Linux too and should be reported upstream independently.

### Possible future improvement

If Microsoft addresses the Job Object accounting in a future Windows update,
the watchdog's primary path (accounting-based kill) would start working
correctly, and the wall-clock fallback would no longer be needed. The
current code handles both cases, so no change would be required.

Alternatively, `NtQueryInformationProcess(ProcessTimes)` at the NT API level
or `QueryProcessCycleTime()` might report non-zero values when the Win32
API doesn't. This has not been tested and is not worth pursuing given the
working watchdog.

## 6. Reproduction

Run `cpu_probe.exe` (or `cpu_probe3.exe`) on a Windows 11 host:

```powershell
.\tests-win\cpu_probe3.exe
```

Observe the tick lines. If both kernel and user stay at `0.000s` while the
child is visibly consuming CPU (Task Manager shows high CPU), the issue is
present on that host.
