# N6 — Install Windows CAS into tests-win

## Already validated
- PARI/GP: `gp.exe` from MSYS2 ucrt64 (phase 2), equivalence OK under wine and with `pari.exe`.

## Octave (MSYS2)
Preferred on the PC (DLL closure is large; incomplete extract on the box failed on `libgcc_s_seh-1.dll`):

```
pacman -S mingw-w64-ucrt-x86_64-octave
```

Then copy `ucrt64/bin/octave-cli.exe` + dependent DLLs into `tests-win/cas/octave/`, set `octave_command` accordingly, rerun `run_equiv_wine.sh`-style tests natively.

## Maxima
Not packaged as a single MSYS2 binary (needs SBCL + maxima sources). Use the official Windows installer from maxima.sourceforge.io, or `pacman -S mingw-w64-ucrt-x86_64-sbcl` then build maxima. Point `maxima_command` at `maxima.bat` / `maxima.exe`.

## GAP
No MSYS2 package. Use the official GAP Windows build; `gap_command=gap.exe` (already the Win32 default in `gap.c`).

## Gap in this phase
Octave `.pkg.tar.zst` downloaded on the box (~20 MB + deps); full DLL closure not completed under wine. Install on the PC via pacman as above.


## Validated on PC (23/10/2026, heure de Paris)

| CAS | Source | Interface | Smoke |
|-----|--------|-----------|-------|
| PARI/GP 2.19.0 | MSYS2 ucrt64 extract → `tests-win/cas/pari/gp.exe` | `pari.exe` | `2+3` → 5 ; `factor(10!)` OK |
| Maxima 5.49.0 | `winget install MaximaTeam.Maxima` → `C:\maxima-5.49.0\bin\maxima.bat` | `maxima.exe` | `2+3` → 5 ; `factor(x^2-1)` OK |
| Octave 11.3.0 | `winget install GNU.Octave` → `...\Octave-11.3.0\mingw64\bin\octave-cli.exe` | `octave.exe` | `1+1` → 2 |
| GAP | **not installed** (no winget package) | `gap.exe` ready | — |

Environment for interfaces: set `tmp_dir`, `{cas}_command`, `wims_exec_parm`.


## GAP 4.16.1 (PC, 23/10/2026 ~19:55 heure de Paris)

Installateur officiel Cygwin-based sous :
`C:\Users\Shadow\AppData\Local\GAP-4.16.1\`

- Exécutable direct (pas de `.bat`) :
  `...\runtime\opt\gap-4.16.1\gap.exe`
- Dépendances DLL Cygwin : ajouter `...\runtime\bin` au `PATH`
  (`cygwin1.dll`, …).
- `gap_command` sans espaces : junction
  `tests-win\cas\gap` → dossier d'install, puis
  `gap_command=<tests-win>\cas\gap\runtime\opt\gap-4.16.1\gap.exe -T -n`
- Smoke : `Print(2+3,"\n");` → `5` ; interface `gap.exe` → `5`.
- Suite `tests/interfaces/run_equiv_pc.ps1` : **24/24** (gap/maxima/octave ×
  calc, factor, large/fsizelim, illegal, about, timeout, absent, multiexec).

Maxima Windows : lanceur `C:\maxima-5.49.0\bin\maxima.bat` (SBCL) — pas
d'image `.exe` autonome ; `MAXIMA_PREFIX` / `SBCL_HOME` requis pour multiexec.
