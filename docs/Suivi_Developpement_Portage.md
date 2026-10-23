
## 2026-10-23 — Maxima : sbcl.exe direct (issue #2)
- `maxima_command` = `C:\maxima-5.49.0\bin\sbcl.exe --core …\binary-sbcl\maxima.core --noinform --dynamic-space-size 2000 --end-runtime-options --eval '(cl-user::run)' --end-toplevel-options` — plus de maxima.bat ni de cmd.exe.
- `src/Interfaces/maxima.c` (`#ifdef _WIN32` uniquement) : dérive `SBCL_HOME=<prefix>/bin` et `MAXIMA_PREFIX=<prefix>` du chemin de sbcl.exe s'ils ne sont pas définis ; l'enfant hérite de l'environnement Windows complet. Build Linux identique (check-linux-identical : IDENTICAL).
- tests-win (env MAXIMA_PREFIX/SBCL_HOME vidés) : 24/24 PASS, dont maxima calc/factor/large/illegal/about/timeout/absent/multiexec.
