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
