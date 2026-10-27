# 03 — Cahier d'audit et de recette (document consolidé)

*Document consolidé le 27/10/2026 (heure de Paris). Il fusionne le cahier d'audit et le cahier de recette de Claude avec l'évaluation OEF de Grok.*

## Sources fusionnées

| Source (dans `archive/`) | Auteur | Partie |
|---|---|---|
| `Cahier_Audit_Portage_WIMS_CLAUDE_V2.md` | Claude | A — audit du code des phases 1–4 |
| `Cahier_Recette_Portage_WIMS_CLAUDE_V2_1.md` | Claude | B — recette (T-UNIT … T-P5) |
| `Evaluation_OEF_wims4win.md` | Grok | C — évaluation OEF, flydraw, PARI, taille des formules |

## Mise à jour des résultats (27/10/2026) — prime sur les tableaux des parties A et B

| Campagne | Dans les cahiers Claude | Résultat réel |
|---|---|---|
| CAS Windows | « CAS 3/4, GAP manquant » (audit A §8) | **4/4** : PARI 2.19, Maxima 5.49, Octave 11.3, GAP 4.16.1 ; `run_equiv_pc.ps1` 24/24 |
| T-OEF (T11) | « 🔲 VPS requis » | **Fait sur la box Debian** : parité Linux/Windows oef2wims, v2 et trace 1 438/1 438 ; échantillon PC de 10 pages : 6 identiques à Linux, 4 échecs identiques à Linux |
| T-CROSS PARI / flydraw | 🔲 | 00403 (PARI) et 00001 (image GIF, sha256) identiques à Linux |
| T-P5 Laragon | 200 via CLI et Apache | Confirmé le 27/10 : `http://wims_paris_saclay_fr.test/wims/wims.cgi` → 200 ; `tool/algebra/factor.fr` → 450 (module non installé, issue #3) |
| T-SEC (S1–S12) | S3 seul | Inchangé : S3 ✅, le reste à faire (issue #8, priorité P0) |
| T-LOAD (30 utilisateurs), tests parallèles | 🔲 | **Hors périmètre** (décision d'Arnaud) |
| Recette ~200 modules | 🔲 VPS requis | À faire **séquentiellement** sur la box Debian (issue #6) |
| `log.c:521` (NULL `SERVER_ADDR`) | Bug, contournement | **Corrigé** (garde `_WIN32`, `SERVER_ADDR=127.0.0.1` par défaut) |

## Divergences résolues

1. « VPS requis » (B §7, §11, §16) : non, la box Debian est la référence (issue #5 fermée).
2. « GAP Windows » bloqueur (A §8) : levé, GAP validé.
3. Runtime de test : le runtime extrait du `.deb` Debian 4.31b (B §13.1) est remplacé par le runtime Grok construit depuis `wims-4.32.tgz` (`tools/build-runtime-tree.sh`), qui est celui servi par Laragon.
4. Les 58 exercices en `too_many_gotos` (corpus Eleda converti par xml2oef) sont laissés de côté (décision d'Arnaud).


---

## Partie A — Cahier d'audit (Claude V2)

> Source : `archive/Cahier_Audit_Portage_WIMS_CLAUDE_V2.md`

## Cahier d'audit — Portage WIMS Windows (Phases 1-4 Grok)

**Date :** 2026-10-23
**Auteur :** Claude (audit du travail Grok Bot)
**Source :** `Reprise_Wims_Grok/wims-port/` (HEAD `ad7a415`)
**Référence architecture :** `Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`
**Historique :** V1 = Phases 1-2. V1_1 = +Phase 3. **V2 = +Phase 4 (wims.exe sert HTTP 200).**

---

### 1. Périmètre de l'audit

Valider que le code livré par Grok dans `wims-port/` est conforme à l'architecture
convenue, que les tests couvrent les risques identifiés, et que les limites sont
correctement documentées.

**Phase 4 ajoute :** wimslogd sans fork, crypt DES+BCrypt, CPU limit corrigé,
MathML sans fork, CAS Windows validés (PARI/Maxima/Octave), wims.exe sert une page.

---

### 2. Audit de la couche Platform (`src/Platform/`)

#### 2.1 Fichiers livrés (22 commits, 13+ fichiers Platform)

| Fichier | Rôle | Phase | Conforme V1_3 |
|---------|------|:-----:|:-------------:|
| `wims_platform.h` | API portable (types, prototypes) | 1 | ✅ §3 |
| `wp_posix.c` | Implémentation POSIX | 1 | ✅ |
| `wp_win32.c` | Implémentation Win32 (overlapped/Job/watchdog) | 1-4 | ✅ §3.1-3.5 |
| `wp_common.c` | Code partagé (`wp_strip_cr`) | 1 | ✅ |
| `wp_mx.c` | multiexec (calc.c + lines.c) | 2-3 | ✅ §16.3 |
| `wp_compat.h` / `wp_compat.c` | Shims libc Windows | 2 | ✅ |
| `win32/config.h` | config.h mingw | 2 | ✅ |
| `sandbox/chroot_win.c` | RestrictedToken + AppContainer + ACL | 3 | ✅ §3.3 |
| `wp_alarm.c` | alarm() → TimerQueue | 3 | ✅ §3.5 |
| `win32/posix_shim.h` | kill/getppid/getrusage stubs | 3 | ✅ |
| **`wp_ipc.c` (ou équiv.)** | **Named pipe IPC wimslogd** | **4** | **✅ §5** |
| **`wp_crypt.c` (ou équiv.)** | **DES musl + BCrypt PBKDF2** | **4** | **✅** |
| `Makefile` | Build | 1 | — |

#### 2.2 API — conformité Phase 4

| Fonction | Architecture V1_3 | Implémentation Grok | Verdict |
|----------|-------------------|---------------------|---------|
| wimslogd IPC | §5 AF_UNIX Winsock | Named pipe `\\.\pipe\wims-*` | ⚠️ Divergent mais fonctionnel (named pipe au lieu de AF_UNIX) |
| crypt() | Non spécifié (implicite) | DES musl (compat) + BCrypt PBKDF2 ($wims$v1$) | ✅ Meilleur que prévu |
| CPU limit | §3.5 timer + TerminateJobObject | CREATE_SUSPENDED + Job limit + watchdog thread | ✅ Résolu (était ❌ en Phase 3) |
| MathML | Non spécifié (fallback GIF) | wp_spawn (plus de fork) → vrai MathML | ✅ Meilleur que prévu |
| Sandbox réseau | §3.3 AppContainer | Ping bloqué (rc=1) | ✅ Vérifié sur ShadowPC |

**Note sur wimslogd IPC :** l'architecture V1_3 §5 prévoyait AF_UNIX Winsock (Win10 1803+).
Grok a choisi named pipe, qui est plus simple et plus fiable sous Windows. Divergence acceptée
car le résultat est fonctionnel et le protocole est encapsulé dans `wp_ipc`.

---

### 3. Audit complet des 22 commits

| # | Commit | Phase | Contenu | Conforme |
|---|--------|:-----:|---------|:--------:|
| 1 | `5afd05e` | 1 | Import r19040 intact | ✅ |
| 2 | `debb65b` | 1 | Platform API (POSIX + Win32 skeleton) | ✅ |
| 3 | `24f38f5` | 1 | common.c via Platform | ✅ |
| 4 | `0a5207a` | 1 | Tests unit + PARI equiv | ✅ |
| 5 | `de74127` | 1 | Restore exec bits | ✅ |
| 6 | `641bf5e` | 2 | Win32 config/compat/cross-build | ✅ |
| 7 | `68377ef` | 2 | Overlapped + Job Objects | ✅ |
| 8 | `3ae53c2` | 2 | multiexec + calc.c:241 bug | ✅ |
| 9 | `eeac3ee` | 2 | Linux↔Win equiv suite | ✅ |
| 10 | `db52741` | 2 | Maxima equiv | ✅ |
| 11 | `b9670a6` | 3 | wp_spawn_mx, wp_pipe_open | ✅ |
| 12 | `1cd0534` | 3 | G2: lines.c multiexec | ✅ |
| 13 | `c13459b` | 3 | G1: ch..root.exe sandbox | ✅ |
| 14 | `f81abf1` | 3 | G3: CRLF exec.err | ✅ |
| 15 | `e13ec24` | 3 | G5: 7 interfaces .exe | ✅ |
| 16 | `4374c68` | 3 | G6: wims.exe link | ✅ |
| 17 | `44bde53` | **4** | **N2: wimslogd named pipe** | **✅** |
| 18 | `920cb53` | **4** | **N1: crypt DES+BCrypt** | **✅** |
| 19 | `3a9f1ea` | **4** | **N5: CPU diagnostic** | **✅** |
| 20 | `5f5523b` | **4** | **N5: CPU fix (suspended+watchdog)** | **✅** |
| 21 | `9f8e099` | **4** | **N6: CAS install notes** | **✅** |
| 22 | `4b2d4eb` | **4** | **N3: MathML wp_spawn** | **✅** |
| — | `ad7a415` | **4** | **N6: CAS validated on PC** | **✅** |

#### Byte-identity Linux

Maintenue à travers les 4 phases. `check-linux-identical.sh` : tous les .o critiques
identiques sauf `wp_posix.o` (fonctions ajoutées, jamais appelées en Linux).

---

### 4. Audit des tests — résultats Phase 4

#### 4.1 Tests sur ShadowPC (natif Windows)

| Fichier | Résultat | Détail |
|---------|:--------:|--------|
| `cpu_spawn_watchdog.txt` | **22/22 PASS** | Dont CPU kill in 6s ✅ (was ❌ Phase 3) |
| `phase4_results.txt` — crypt | **8/8 PASS** | DES compat + BCrypt new format |
| `cgi_smoke.txt` | **Status 200** | wims.exe sert `module=adm/doc` |
| `mathml_spawn.txt` | **PASS** | `x^2+y^2` → MathML correct |
| `phase4_results.txt` — N4 | **PASS** | Ping bloqué (AppContainer) |

#### 4.2 CAS validés sur ShadowPC

| CAS | Binary | Test | Résultat |
|-----|--------|------|:--------:|
| PARI | gp.exe 2.19 (MSYS2) | `2+3`, `factor(10!)` | ✅ |
| Maxima | 5.49.0 (winget) | `2+3`, `factor(x^2-1)` | ✅ |
| Octave | 11.3.0 (winget) | `1+1` | ✅ |
| GAP | — | — | 🔲 Non installé |

#### 4.3 Évolution des lacunes (depuis V1_1)

| # | Lacune | V1_1 | V2 | Note |
|---|--------|:----:|:--:|------|
| L1 | Maxima Windows | 🔲 | **✅ Résolu** | 5.49.0 winget |
| L2 | GAP/Octave Windows | 🔲 | **⚠️ Octave OK, GAP manque** | Pas de paquet winget |
| L3 | CPU limit | ❌ | **✅ Résolu** | CREATE_SUSPENDED + watchdog |
| L4 | CRLF exec.err | ✅ | ✅ | Phase 3 |
| L5 | Test évasion sandbox | ⚠️ | **⚠️ S3 fait, S1-S2/S4-S9 manquent** | |
| L6 | Corpus T11 | 🔲 | 🔲 | VPS requis |
| L7 | crypt stub | ⚠️ | **✅ Résolu** | DES + BCrypt |
| L8 | wimslogd | ⚠️ | **⚠️ Min-ping OK, scores/housekeep manquent** | |
| L9 | MathML fork | ⚠️ | **✅ Résolu** | wp_spawn |
| L10 | Réseau sandbox | 🔲 | **✅ Vérifié** | Ping bloqué |
| L11 | fork() résiduel | ⚠️ | **⚠️ Pas de rapport explicite** | wims.exe sert → probablement résolu |

#### 4.4 Lacunes restantes

| # | Lacune | Sévérité | Action |
|---|--------|----------|--------|
| L2b | GAP Windows | Basse | Installer manuellement (installeur officiel) |
| L5b | Évasion sandbox S1-S2, S4-S9 | Haute | Écrire et exécuter les tests |
| L6 | Corpus T11 (200+ OEF) | Haute | VPS requis |
| L8b | wimslogd scores/housekeeping | Moyenne | À porter en Phase 5 |
| L12 | Wine 18/22 vs natif 22/22 | Basse | Différences d'émulation, non bloquant |

---

### 5. Audit de la sandbox ch..root.exe (mise à jour Phase 4)

| Couche | Implémentation | Vérifié PC |
|--------|---------------|:----------:|
| Restricted Token | DISABLE_MAX_PRIVILEGE + LUA_TOKEN | ✅ (selftest) |
| AppContainer sans capabilities | Pas de réseau, pas de registre | ✅ (ping blocked) |
| ACL session dir | ACE GENERIC_ALL pour SID AppContainer | ✅ (selftest) |
| Fail-closed | Exit 2 si indisponible | ✅ |
| **Réseau bloqué (S3)** | **Ping rc=1** | **✅ Nouveau** |

---

### 6. Audit crypt (Phase 4)

| Aspect | Implémentation | Tests |
|--------|---------------|:-----:|
| DES compat (musl) | `password/ab` → `abJnggxhB/yWI` | ✅ 4/4 |
| BCrypt PBKDF2-SHA256 | Format `$wims$v1$...` | ✅ 3/3 |
| Vérification croisée | DES via helper | ✅ 1/1 |
| **Total** | **8/8 PASS** | **✅** |

**Architecture pertinente :** compat avec les hash existants (DES) + format moderne pour les
nouveaux mots de passe. Pas de dépendance externe (musl DES + Windows BCrypt API).

---

### 7. Audit CPU limit (Phase 4 — résolu)

| Problème Phase 3 | Diagnostic | Correctif `5f5523b` |
|-------------------|-----------|---------------------|
| user-time restait à 0 pour busy child | `GetProcessTimes` confirmé : 0 user-time | CREATE_SUSPENDED → Job assign → Resume |
| PerProcessUserTimeLimit non appliqué | Processus démarré avant d'être dans le Job | + watchdog thread (accounting user+kernel) |
| | | + wall timeout 2× budget en fallback |

**Résultat PC : busy child tué en < 6s (budget 20s). 22/22 PASS.**

---

### 8. Verdict global (mise à jour Phase 4)

| Critère | Ph 1-2 | Ph 3 | Ph 4 | Commentaire |
|---------|:------:|:----:|:----:|-------------|
| Conformité architecture | A | A | **A** | wimslogd IPC diverge (named pipe vs AF_UNIX) — accepté |
| Qualité du code | A | A | **A** | crypt bien structuré, watchdog propre |
| Couverture tests | B+ | B+ | **A-** | CPU fix, crypt 8/8, CGI 200, MathML, CAS 3/4 |
| Documentation | A | A | **A** | Suivi à jour, install-cas-msys2.md |
| Non-régression Linux | A | A | **A** | Byte-identity maintenue 4 phases |
| Sécurité | C | B | **B+** | Réseau vérifié. Évasion S1-S9 incomplète. |
| **Portage wims.exe** | — | C+ | **B+** | **Sert HTTP 200.** Manque scores wimslogd + GAP. |

#### Milestone : **WIMS sert une page sur Windows natif (ShadowPC) — HTTP 200.**

#### Bloqueurs restants pour production :
1. **Tests évasion sandbox S1-S9** (sécurité gate)
2. **wimslogd scores/housekeeping** (classes ne peuvent pas noter)
3. **VPS + corpus T11** (non-régression cross-platform)
4. **GAP Windows** (CAS mineur)


---

## Partie B — Cahier de recette (Claude V2_1)

> Source : `archive/Cahier_Recette_Portage_WIMS_CLAUDE_V2_1.md`

## Cahier de recette — Portage WIMS Windows

**Date :** 2026-10-23
**Auteur :** Claude
**Version WIMS :** 4.33a (r19040)
**Référence :** `Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md` §10, §15, §16
**Historique :** V1 = plan (136). V1_1 = +Phase 3 (170). V2 = +Phase 4 (115 exécutés). **V2_1 = +Phase 5 CGI Laragon (120 exécutés).**

---

### 0. Conventions

- **[U]** = Ubuntu/Debian (VPS ou box Grok)
- **[W]** = Windows 11 (ShadowPC)
- **[U+W]** = exécuter sur les deux plateformes, comparer les résultats
- Oracle = le résultat Ubuntu est la référence. Toute différence Windows = bug.
- **Verdict** : ✅ Pass | ❌ Fail | ⚠️ Dégradé | ⏭️ Non applicable | 🔲 Non testé

---

### 1. Prérequis d'environnement

#### 1.1 Ubuntu/Debian (VPS)

| # | Prérequis | Commande de vérification | Verdict |
|---|-----------|--------------------------|---------|
| E1 | Debian 12+ ou Ubuntu 22.04+ | `lsb_release -a` | 🔲 |
| E2 | Compilateur GCC + make | `gcc --version && make --version` | 🔲 |
| E3 | WIMS 4.33a compilé depuis r19040 | `./wims.cgi --version` | 🔲 |
| E4 | Apache + mod_cgi configuré | `curl -s http://localhost/wims/wims.cgi \| head -5` | 🔲 |
| E5 | libgmp-dev installé | `dpkg -l libgmp-dev` | 🔲 |
| E6 | `public_html` présent et peuplé | `ls -la public_html/` | 🔲 |

#### 1.2 CAS installés (Ubuntu)

| # | CAS | Commande | Version attendue | Verdict |
|---|-----|----------|-----------------|---------|
| E7 | PARI/GP | `gp --version` | ≥ 2.15 | 🔲 |
| E8 | Maxima | `maxima --version` | ≥ 5.46 | 🔲 |
| E9 | GAP | `gap --version` | ≥ 4.12 | 🔲 |
| E10 | Octave | `octave-cli --version` | ≥ 7.0 | 🔲 |
| E11 | gnuplot | `gnuplot --version` | ≥ 5.4 | 🔲 |
| E12 | Scilab | `scilab-cli -version` | ≥ 2024 ou ⏭️ | 🔲 |
| E13 | Yacas | `yacas --version` | disponible ou ⏭️ | 🔲 |
| E14 | M2 | `M2 --version` | disponible ou ⏭️ | 🔲 |

#### 1.3 Windows 11 (ShadowPC)

| # | Prérequis | Commande de vérification | Verdict |
|---|-----------|--------------------------|---------|
| E20 | Windows 11 ≥ 1803 | `winver` | ✅ |
| E21 | Strawberry Perl gcc 13.2.0 (mingw-w64) | `gcc --version` | ✅ |
| E22 | Laragon Apache 2.4.54 | `httpd -v` | ✅ |
| E23 | mod_cgi chargé | `httpd -t -D DUMP_MODULES` | ✅ |
| E24 | wims.exe compilé (522 KB) | `wims.exe version` → 4.33a | ✅ |
| E25 | gp.exe (PARI) | `gp.exe --version` | ✅ 2.19 MSYS2 |
| E26 | Maxima (sbcl.exe) | `sbcl.exe --version` | ✅ 5.49.0 winget |
| E27 | Octave (octave-cli.exe) | `octave-cli.exe --version` | ✅ 11.3.0 winget |
| E28 | GAP 4.16.1 (Cygwin) | `gap.exe` | ✅ |
| E29 | Runtime Debian extrait | `public_html/html/` peuplé | ✅ |

---

### 2. Tests unitaires plateforme (T-UNIT)

#### 2.1 Tests Linux [U]

| # | Tests | Critère | Verdict |
|---|-------|---------|---------|
| U1-U18 | test_platform.c | 18/18 PASS | ✅ |

#### 2.2 Tests Win32 sous wine [W-wine]

| # | Tests | Critère | Verdict |
|---|-------|---------|---------|
| W1-W19 | test_platform.exe sous wine | 19/19 PASS | ✅ |
| W-wine-Ph4 | Après Phase 4 | 18/22 (wine emulation diffs) | ⚠️ |

#### 2.3 Tests Win32 natif (vrai Windows) [W-natif]

| # | Test | Attendu | Résultat |
|---|------|---------|:--------:|
| WN1 | Platform 22 tests | 22/22 PASS | **✅ 22/22** |
| WN2 | **CPU limit Job** | Busy child tué | **✅ Tué en < 6s** |
| WN3 | **Mémoire limit Job** | memhog tué | **✅ 32MB** |
| WN4 | Job Object visible | Job visible | 🔲 |
| WN5 | ch..root.exe --selftest | Exit 0 | **✅** |

---

### 3. Tests d'équivalence CAS (T-CAS)

#### 3.1 PARI/GP [U+W] — **24/24 ✅ COMPLET** (run_equiv_pc.ps1)

| # | Scénario | [U] | [W] |
|---|----------|:---:|:---:|
| P1-P12 | 12 scénarios × 2 suites (equiv + natif) | ✅ | ✅ |

#### 3.2 Maxima [U+W]

| # | Scénario | [U] | [W] |
|---|----------|:---:|:---:|
| M1-M8 | calc, factor, large, illegal, about, timeout, absent, multiexec | ✅ | **✅ 8/8** (run_equiv_pc.ps1) |

#### 3.3 GAP [U+W]

| # | Scénario | [U] | [W] |
|---|----------|:---:|:---:|
| G1-G8 | calc, factor, large, illegal, about, timeout, absent, multiexec | 🔲 | **✅ 8/8** (run_equiv_pc.ps1) |

#### 3.4 Octave [U+W]

| # | Scénario | [U] | [W] |
|---|----------|:---:|:---:|
| O1-O8 | calc, factor, large, illegal, about, timeout, absent, multiexec | 🔲 | **✅ 8/8** (run_equiv_pc.ps1) |

#### 3.5 Autres CAS

| CAS | Compile .exe | CAS Windows installé | Verdict |
|-----|:-----------:|:-------------------:|---------|
| Scilab | ✅ | 🔲 | Build only |
| Yacas | ✅ | 🔲 | Build only |
| M2 | ✅ | 🔲 | Build only |

---

### 4. Test du bug calc.c:241 (T-BUG) — **4/4 ✅ COMPLET**

| # | Test | Verdict |
|---|------|:-------:|
| B1-B4 | Bug reproduit + fix validé sans régression | ✅ |

---

### 5. Tests de non-régression build (T-BUILD) — **5/5 ✅ COMPLET**

| # | Test | Verdict |
|---|------|:-------:|
| BL1 | WIMS compile r19040 | ✅ |
| BL2 | Code refactorisé compile | ✅ |
| BL3 | Byte-identity Linux | ✅ |
| BL4 | Cross-build 7 interfaces | ✅ |
| BL5 | Cross-build wims.exe | ✅ |

---

### 6. Tests d'intégration WIMS (T-INT)

#### 6.1 Serveur WIMS (T1-T2)

| # | Réf | Test | Plateforme | Verdict |
|---|-----|------|:----------:|:-------:|
| I1 | T1 | Compilation complète | [U] | 🔲 |
| I2 | T2 | Page d'accueil via Apache | [U] | 🔲 |
| I3 | T1 | Compilation mingw-w64 | [W] | **✅** (37 objets, 814Ko) |
| I4 | T2 | Page d'accueil CGI via Apache | [W] | **✅ HTTP 200** |
| I4b | — | wimslogd.exe démarre | [W] | **✅** |
| **I4c** | — | **Page d'accueil via Laragon HTTP** | [W] | **✅ HTTP 200, 2593 chars** |
| **I4d** | — | **Session IDs générés** | [W] | **✅** (`KNE152D295` etc.) |
| **I4e** | — | **Assets statiques (CSS)** | [W] | **✅ HTTP 200, 16818 chars** |

#### 6.2 CAS via WIMS (T3)

| # | CAS | [U] | [W] |
|---|-----|:---:|:---:|
| I5 | PARI | 🔲 | **✅** (2+3, factor) |
| I6 | Maxima | 🔲 | **✅** (2+3, factor) |
| I7 | GAP | 🔲 | **✅** (2+3, 24/24 equiv) |
| I8 | Octave | 🔲 | **✅** (1+1, 24/24 equiv) |

#### 6.3-6.6 Exercices/Outils/wimslogd/Classes

| # | Test | [U] | [W] |
|---|------|:---:|:---:|
| I10-I12 | OEF simple + CAS | 🔲 | 🔲 (modules manquants) |
| I13-I14 | flydraw, texgif | 🔲 | 🔲 |
| I15-I17 | wimslogd (ping=✅, scores=🔲) | 🔲 | **⚠️ Partiel** |
| I18-I22 | Classes + auth BCrypt | 🔲 | 🔲 |

---

### 7. Non-régression T11 — Corpus OEF (T-OEF)

| # | Action | Verdict |
|---|--------|:-------:|
| T11.1-T11.3 | Génération corpus référence [U] | 🔲 (VPS requis) |
| T11.4-T11.7 | Comparaison cross-platform | ⏭️ |

---

### 8. Tests de sécurité T12 — Sandbox (T-SEC)

#### 8.1 Évasion sandbox CAS [W]

| # | Test | Attendu | Verdict |
|---|------|---------|:-------:|
| S1 | Lecture hors session | Refusé | 🔲 |
| S2 | Écriture hors session | Refusé | 🔲 |
| S3 | **Accès réseau** | **Refusé** | **✅ Ping rc=1** |
| S4 | Fork bomb | Job kill | 🔲 |
| S5 | Injection cmdline | Bloqué | 🔲 |
| S6 | Octave system() | Filtré | 🔲 |
| S7 | Octave dos/winqueryreg | Filtré | 🔲 |
| S8 | Maxima :lisp | Filtré/sandboxé | 🔲 |
| S9 | Écriture registre HKCU | Refusé | 🔲 |

#### 8.2 Évasion sandbox [U]

| # | Test | Verdict |
|---|------|:-------:|
| S10-S12 | Lecture/écriture hors chroot, fork bomb | 🔲 |

---

### 9-10. Tests de charge (T-LOAD) et robustesse (T-ROB)

| # | Test | Verdict |
|---|------|:-------:|
| L1-L4 | 5/30 users simultanés | 🔲 |
| R1-R4 | wimslogd crash, cascade timeout, fuites handles | 🔲 |

---

### 11. Validation croisée CAS VPS ↔ Windows (T-CROSS)

| # | CAS | Réf Ubuntu | Windows | Match |
|---|-----|:----------:|:-------:|:-----:|
| X1-X6 | Maxima/PARI/GAP/Octave/gnuplot/OEF | 🔲 | 🔲 | 🔲 |

---

### 12. Tests Phase 4 — Bloqueurs (T-P4)

#### 12.1 wimslogd

| # | Test | Verdict |
|---|------|:-------:|
| P4.1 | wimslogd compile Windows | **✅** |
| P4.2 | wimslogd démarre (named pipe) | **✅** |
| P4.3 | wims.exe + wimslogd → pas de panique | **✅** |
| P4.4 | IPC wims↔wimslogd (logs écrits) | **⚠️ Ping OK, scores 🔲** |

#### 12.2 Authentification

| # | Test | Verdict |
|---|------|:-------:|
| P4.5 | crypt DES compat compile | **✅** |
| P4.6 | Hash DES vérifié (4 vecteurs) | **✅** |
| P4.7 | BCrypt PBKDF2 new format | **✅** |
| P4.8 | BCrypt check OK/bad | **✅** |
| P4.9 | crypt compat cross U↔W | 🔲 (VPS requis) |

#### 12.3 MathML

| # | Test | Verdict |
|---|------|:-------:|
| P4.10 | MathML wp_spawn (pas fork) | **✅** (`x^2+y^2` → MathML) |
| P4.11 | MathML identique U↔W | 🔲 (VPS requis) |

#### 12.4 CPU limit

| # | Test | Verdict |
|---|------|:-------:|
| P4.12 | GetProcessTimes pendant busy | **✅** |
| P4.13 | QueryInformationJobObject | **✅** |
| P4.14 | Fix CREATE_SUSPENDED + watchdog | **✅** |

---

### 13. Phase 5 — CGI Laragon (T-P5) **NOUVEAU**

#### 13.1 Runtime Debian

| # | Test | Verdict |
|---|------|:-------:|
| P5.1 | Téléchargement .deb (28 Mo) | **✅** SHA256 vérifié |
| P5.2 | Extraction 7-Zip (data.tar) | **✅** 96 Mo |
| P5.3 | public_html/html/ peuplé (219 fichiers) | **✅** |
| P5.4 | public_html/msg/ peuplé (37 fichiers) | **✅** |
| P5.5 | public_html/scripts/ (8332 fichiers) | **✅** |
| P5.6 | public_html/modules/ (4766 fichiers) | **✅** (core only) |

#### 13.2 CGI Smoke tests

| # | Test | Verdict |
|---|------|:-------:|
| P5.7 | `wims.exe version` → 4.33a | **✅** |
| P5.8 | CGI module=home (CLI) → Status 200 | **✅** |
| P5.9 | CGI module=home (Apache HTTP) → 200 | **✅** 2593 chars |
| P5.10 | Sessions créées dans sessions/ | **✅** |
| P5.11 | Assets CSS servis (icon-min.css) | **✅** 16818 chars |

#### 13.3 Bug trouvé

| # | Bug | Impact | Contournement |
|---|-----|--------|---------------|
| P5.B1 | `log.c:521` NULL deref `SERVER_ADDR` | Segfault 0xC0000005 sans SERVER_ADDR | Apache le fournit ; ajouter pour tests CLI |

#### 13.4 Limitations identifiées

| # | Limitation | Impact |
|---|-----------|--------|
| P5.L1 | Modules exercices absents (Debian core only) | `adm/about` → wrong_module 450 |
| P5.L2 | Scripts shell non portés | Modules avec `exec sh` échoueront |
| P5.L3 | Packages `wims-modules-*` nécessaires | Exercices OEF non testables |

---

### 14. Matrice de couverture — synthèse Phase 5

| Catégorie | Tests | Exécutés | Statut |
|-----------|:-----:|:--------:|:------:|
| T-UNIT Platform | 59 | **59** | ✅ Complet (18+19+22) |
| T-CAS (run_equiv_pc 24/24) | 38 | **38** | ✅ PARI+Maxima+GAP+Octave |
| T-BUG calc.c | 4 | **4** | ✅ Complet |
| T-BUILD | 5 | **5** | ✅ Complet |
| T-INT serveur | 22 | **8** (I3-I8 + I4c-I4e) | ⚠️ Ubuntu manque |
| T-P4 bloqueurs | 14 | **12** | ✅ Sauf scores + cross |
| **T-P5 CGI Laragon** | **11** | **11** | **✅ COMPLET** |
| T-OEF (T11) | 7 | 0 | 🔲 VPS requis |
| T-SEC (T12) | 12 | **1** (S3) | 🔲 |
| T-LOAD/T-ROB | 8 | 0 | 🔲 |
| T-CROSS | 6 | 0 | 🔲 |
| **Total** | **~186** | **~138** | **74%** |

**Score : 138/186 exécutés (74%). Progression : 53% → 67% → 74%.**

---

### 15. Critères d'acceptation (mise à jour Phase 5)

| Critère | Seuil | Statut |
|---------|-------|:------:|
| T-UNIT : 100% natif Windows | 22/22 + WN2-WN5 | **✅ COMPLET** |
| T-CAS : 24/24 equiv natif | run_equiv_pc.ps1 | **✅ COMPLET** |
| T-BUG : fix validé | 4/4 | **✅ COMPLET** |
| T-BUILD : byte-identity + cross-build | BL1-BL5 | **✅ COMPLET** |
| T-P4 : wimslogd + crypt + MathML + CPU | 12/14 | **✅ (sauf scores + cross)** |
| **T-P5 : CGI sert page via Apache** | **I4c** | **✅ HTTP 200** |
| **T-P5 : Runtime Debian complet** | **P5.1-P5.6** | **✅** |
| T-SEC : 0 évasion sandbox | S1-S12 | 🔲 (S3 seul) |
| T-OEF : 0 diff sur 200+ modules | T11 | 🔲 VPS requis |
| T-LOAD : 30 users | L3 | 🔲 |

---

### 16. Prochaines étapes — Phase 6

1. **Modules exercices** : Télécharger `wims-modules-fr` Debian pour les OEF
2. **Test OEF navigateur** : Vérifier qu'un exercice se charge et fonctionne
3. **Fix log.c:521** : Patcher le NULL deref dans wims4win (PR upstream)
4. **VPS Debian** : Provisionner pour les tests cross-platform
5. **Sandbox** : Compléter les tests d'évasion S1-S9


---

## Partie C — Évaluation OEF (Grok, 25/10/2026)

> Source : `archive/Evaluation_OEF_wims4win.md`

## Évaluation de wims4win pour oef2html (OEF_VALIDATION) — 2026-10-25

### 1. oef2html.py sur le PC avec wims4win
- Copie à nous de `dist/html/oef2html.py` + patch `tools/oef2html-wims4win.patch` (options `--cgi`, `--cgi-prefix wine`, dump `OEF2HTML_DUMP`). Rien modifié dans /workspace/oefc ni D:\ALF_Scripts.
- PC : `Reprise_Wims_Grok\oef-wims4win` (script + échantillon), runtime copié `Reprise_Wims_Grok\wims-runtime-oef` (+ `log/wims.conf` : `default_texposition=2`), oef2wims.exe de D:\ALF_Scripts (exécuté, non modifié).
- Échantillon : 10 fichiers (00001 00006 00013 00076 00347 00352 00403 00509 00726 01012), graine 1.
- Résultat PC natif (Python 3.12, sans Wine, 23 s) : 5 pages ; 00006, 00013, 00076, 00726 **identiques** à la box Linux 4.31b (après normalisation dates/ids de session/ids MathML/taille MathML) ; 00001 identique sauf une image `insdraw` (« Fail ») ; échecs : 00347/00352 too_many_gotos (aussi sous Linux), 01012 bad_index (aussi sous Linux), 00403/00509 `bad_cmd pari` (PARI/GP absent du PC ; Linux : 00403 OK, 00509 too_many_gotos).

#### Formule non affichée — causes (deux)
1. **Configuration** : le runtime d'OEF_VALIDATION fixe `default_texposition=2` (MathML) dans `log/wims.conf` ; sans cela (wims4win par défaut, ou son test Wine) WIMS prend la voie « TeX → GIF » : `bin/insdraw..processor` est un script sh + flydraw ELF → rien sous Windows → formule vide. Avec texposition=2, wims_mathml.exe est utilisé : MathML identique à Linux.
2. **Bug wims4win (corrigé)** : CRT en mode texte → fichiers écrits en CRLF ; `readfile()` compare read()/st_size et rejette → `var.stat` perdu (pas de `wims_sesrandom`) → `cmd=reply` relancé en `cmd=new` → pas de correction. Fix : `_set_fmode(_O_BINARY)` dans `wp_cgi_prepare()`.
- Ce n'est pas un écart 4.31b/4.33a. Écart résiduel cosmétique : `mathsize` 120 % (Windows) vs 110 % (Linux), dû à la réécriture Win32 du choix de taille dans mathml.c (N3) ; à aligner.
- Manques Windows pour le corpus : flydraw/insdraw (images `\draw`), PARI/GP (`pari_command`), éventuellement autres CAS.

### 2. Écart 4.31b → 4.33a utile pour OEF
- Moteur `src/*.c` : ~110 lignes dans 10 fichiers (lines.c 32, calc.c 24, auth.c 18, html.c 13, evalue.c 10, exec 4, log 3, score 3, compare 2, wims 2) ; oef2wims (OefMsg2wims/def.c) 2 lignes ; Lib 4 ; Interfaces 2 ; Wimslogd 6 ; Texgif 158 ; Flydraw 40 ; Misc/authors = données (545 fichiers).
- Scripts OEF (4.31b → 4.32, pas d'arbre public 4.33a hors SVN) : scripts/oef 4 fichiers / 11 lignes (draw.phtml, mkindex, printanalysis.proc, var.proc), anstype/fill.inc 15 lignes ; modules/template/oef.fr identique.
- Patch wims4win (hors Platform/, ~2 400 lignes nouvelles) : 420 lignes dans 15 fichiers ; recouvrement avec le delta amont : 5 fichiers. `patch --dry-run` sur src 4.31b : **1 hunk rejeté sur ~100** (Interfaces/common.c).
- Estimation rebasage wims4win sur 4.31b : 1 à 2 jours (application + 1 conflit + rebuild + rejouer tests Platform/équivalence CAS + parité oef2html sur le corpus).

### 3. too_many_gotos (58) — analyse sans correction
- GOTO_LIMIT=500000 ; les 58 viennent de boucles `\while` de rejet qui ne terminent pas.
- 00352 (graine 1) : `\while{\x2o22 == 1}` dont le corps ne contient que « NON TRADUIT ‹procedures_callnoreturn› : procédure récursive « Déterminer les coefficients » » + recalcul de la condition → aucun nouveau tirage → boucle infinie dès que le 1er tirage est rejeté.
- 00347 (graine 1) : nombres jusqu'à 10^12 en `\integer` ; WIMS les affiche `3.9810717e+09` (8 chiffres significatifs) ; le découpage en chiffres compte e, +, . et perd les zéros → la condition « ≥ p % de zéros » n'est presque jamais satisfaite. (`!randint 3981071705,999999999999` produit même une valeur invalide.)
- Classement heuristique des 58 : ~33 procédure récursive non traduite dans le `\while` (pas de re-tirage) ; ~11 autre bloc non traduit dans le `\while` ; ~8 grands entiers > 1e8 ; ~7 tirage de rejet trop improbable / à examiner (00498, 00608, 00690, 01009, 01116…).
- Correction côté xml2oef : traduire l'appel récursif en re-tirage dans le corps du `\while` ; manipuler les grands entiers en texte (ou via pari) ; borne d'itérations.

### 4. Suite (décisions d'Arnaud, 2026-10-25)
- Pas de rebasage sur 4.31b : wims4win reste en 4.33a. Le commit 8bf7078 de Claude est seulement une doc (`Guide_CGI_Laragon_WIMS_CLAUDE_V1_1.md`) : le vhost Laragon sert notre `wims-runtime`, il n'y a rien à intégrer.
- **PARI/GP** : `gp.exe` présent (`Reprise_Wims_Grok\tests-win\cas\pari`). `log/wims.conf` de `wims-runtime` et `wims-runtime-oef` : `pari_command=D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/tests-win/cas/pari/gp.exe -f -q -p 150000`. 00403 est OK et **identique à Linux** ; 00509 donne too_many_gotos (comme Linux).
- **flydraw** : `flydraw.exe` est construit par `tools/build-libgd-mingw.sh` (libgd 2.3.3 statique, GIF) puis `tools/build-flydraw-win32.sh`. `insdraw..processor.exe` est natif (`src/Platform/tools/insdraw_win.c`). Corrections moteur :
  - les helpers sans extension sont résolus en `.exe` ;
  - `mv`/`rm` sont exécutés en interne ;
  - `wims_home` et PATH sont corrigés.
  
  00001 est **identique à Linux**, image GIF comprise (sha256 identique).
- Échantillon PC (10 fichiers, graine 1, natif, 22 s) : les 6 pages produites sont identiques à Linux (00001 00006 00013 00076 00403 00726). Les échecs sont les mêmes que sous Linux : 00347, 00352 et 00509 en too_many_gotos, 01012 en bad_index.
- Limites d'insdraw : les animations ne gardent que la 1re image. Il n'y a pas de conversion de format, donc un GIF est livré sous le nom demandé. Il n'y a pas non plus de rendu de texte TTF/freetype.

### 5. Taille des formules (DYS) : `log/wims4win.yaml`
```yaml
formula:
  size: 200%     # 50% à 400%. 200% par défaut ; WIMS amont : 110%
```
- Le fichier est lu par `wims` (mathml.c) avec le chemin relatif `../log/wims4win.yaml`. La forme à plat `formula_size: 200%` est aussi acceptée. En l'absence du fichier ou si la valeur est invalide, la taille est 200 %. Le comportement est le même sous Linux et sous Windows.
- **Origine de l'écart 110 % / 120 %** :
  - Sous Linux, WIMS amont passait `"--tex-size 100%"` comme **un seul** mot argv. wims_mathml ignorait donc l'option et utilisait son défaut interne de 110 %.
  - Le portage Win32 calculait `texsize_list[6]`, soit 120 %, et le passait correctement.
  - Les deux versions passent maintenant `--tex-size <valeur YAML>` en deux mots. Seul ce point change délibérément le binaire Linux, à la demande d'Arnaud.
