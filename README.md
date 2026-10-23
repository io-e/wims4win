# wims-port — portage natif Windows de WIMS

Fork de travail de **WIMS** (WWW Interactive Multipurpose Server), trunk
**r19040 / 4.33a**, visant un déploiement **natif sous Windows** (sans WSL),
tout en conservant un comportement **identique octet pour octet** sur le
chemin Linux/Debian pour le code non conditionné à `_WIN32`.

Licence : **GNU GPL v2** (voir `LICENSE`, copie de `src/COPYING` des sources
d’origine).

## Objectif

- Conserver WIMS Linux/Debian (build et tests d’équivalence).
- Porter les interfaces CAS, le CGI (`wims.exe`), wimslogd et les helpers
  (MathML, sandbox) via une couche portable `src/Platform`.
- Remplacer `fork` / `pipe` / `setrlimit` / AF_UNIX par des équivalents Win32
  (CreateProcess + Job Objects, named pipes, BCrypt, AppContainer, …).

## État (phases 1–4) — HEAD de référence `51b54d5`

| Phase | Contenu | Statut |
|-------|---------|--------|
| 1 | API `wims_platform`, refactor `Interfaces/common.c` | ✅ |
| 2 | Job Objects, overlapped I/O, multiexec, équiv. pari/maxima (wine) | ✅ |
| 3 | `ch..root.exe` (AppContainer), `wims.exe` mingw, strip CR | ✅ partiel |
| 4 | wimslogd min (named pipe), crypt DES+BCrypt, MathML via `wp_spawn`, CAS Win (pari/maxima/octave/GAP), limite CPU au spawn + watchdog | ✅ (voir limites) |

Détail : `docs/Suivi_Developpement_Portage.md`. Architecture :
`docs/Architecture_Portage_MSVC_WIMS_V3.md`. Audits : `docs/Audit_*`.

## Structure utile

```
src/Platform/     API portable (wp_posix.c / wp_win32.c, Job, IPC, crypt…)
src/Interfaces/   interfaces CAS (pari, maxima, gap, octave, …)
src/Wimslogd/     daemon (POSIX + win minimal)
src/Mathml/       helper wims_mathml (flex/bison)
tools/            build-win32.sh, build-wims-win32.sh, check-linux-identical.sh…
tests/platform/   unitaires Platform (Linux + mingw)
tests/interfaces/ suites d’équivalence + run_equiv_pc.ps1
docs/             audits, architecture, suivi
```

## Compilation

### Linux (référence)

Configurer / builder comme WIMS classique (autoconf/`compile`). Vérifier
l’identité des objets Linux après modification :

```bash
tools/check-linux-identical.sh
```

Tests Platform :

```bash
make -C tests/platform test
```

### Windows (cross mingw-w64 sur Debian)

```bash
sudo apt-get install -y mingw-w64
tools/build-win32.sh          # interfaces *.exe
tools/build-wims-win32.sh     # wims.exe
tools/build-wimslogd-win32.sh
tools/build-wims-mathml-win32.sh
tools/build-chroot-win32.sh   # ch..root.exe
```

Sur un PC Windows réel, les binaires sont joués dans un dossier de test
dédié (hors dépôt) ; voir `tests/interfaces/run_equiv_pc.ps1` et
`tools/install-cas-msys2.md` (GAP/Maxima/Octave/PARI).

## Tests

- `tests/platform` : pipes, Job kill d’arbre, CPU/watchdog, isolation.
- `tests/interfaces/run_equiv*.sh` : équivalence Linux ↔ wine / r19040.
- `tests/interfaces/run_equiv_pc.ps1` : gap / maxima / octave sur Windows natif
  (calc, large/fsizelim, illegal, timeout, absent, multiexec) — **24/24** au
  dernier passage.

## Limites restantes

- wimslogd Windows : ping/loadavg seulement (scores / housekeeping à porter).
- Maxima Windows encore lancé via `maxima.bat` (SBCL) ; GAP via `gap.exe` +
  runtime Cygwin dans le `PATH`.
- Arborescence `public_html` / modules non livrée dans ce dépôt.
- Tag amont 4.32 vs trunk 4.33a à clarifier pour la recette.
- Corpus VPS (H1–H3), recette 200 modules et tests de charge non faits.
- Diagnostic Job CPU : user-time parfois à 0 → watchdog mural en filet.
- Durcissement sandbox AppContainer ; remontée bogue `calc.c:241` à wimsdev.

## CI

GitHub Actions (`.github/workflows/build.yml`) : build Linux Platform tests +
cross mingw-w64 (`tools/build-win32.sh`).
