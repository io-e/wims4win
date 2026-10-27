# CLAUDE_V1
# Handoff → Grok Bot — Session 03 (Phase 4)

**Date :** 2026-10-23
**De :** Claude (ShadowPC)
**Pour :** Grok Bot (VPS Debian)
**Repo :** `Reprise_Wims_Grok/wims-port/` — HEAD `4374c68`
**Audit :** `Cahier_Audit_Portage_WIMS_CLAUDE_V1_1.md`
**Recette :** `Cahier_Recette_Portage_WIMS_CLAUDE_V1_1.md`

---

## 1. Bilan Phase 3 — Validé ✅

Tout le plan G1-G6 est livré. Résumé :

| Task | Commit | Résultat |
|------|--------|----------|
| G2 — lines.c multiexec sur Platform | `1cd0534` | ✅ Linux byte-identical |
| G1 — ch..root.exe sandbox | `c13459b` | ✅ AppContainer selftest OK sur ShadowPC |
| G3 — CRLF exec.err | `f81abf1` | ✅ |
| G5 — 7 interfaces .exe | `e13ec24` | ✅ Compilation. Octave filtrage étendu. |
| G6 — wims.exe 814Ko | `4374c68` | ✅ Link OK. Panique attendue (wimslogd absent). |
| G4 — Job Object natif | tests-win | ✅ 22/22, mémoire OK. **❌ CPU limit non appliqué.** |

**Score tests : ~91 exécutés sur ~170 (53%).**

---

## 2. Décision H9 — Dépôt unique

**`wims-port/` est le dépôt unique.** Claude produit specs + audit, Grok produit le code.
Pas de fusion à faire, pas de second dépôt C.

---

## 3. Phase 4 — Actions prioritaires

### Ordre recommandé (par impact sur le démarrage de wims.exe) :

| # | Action | Bloqueur | Effort estimé | Réf recette |
|---|--------|:--------:|:-------------:|:-----------:|
| **N2** | **Port wimslogd sans fork** | **wims.exe ne démarre pas** | 1-2 semaines | P4.1-P4.4 |
| **N1** | **crypt → BCrypt (Windows) / crypt_r (Linux)** | **Auth bloquée** | 3-5 jours | P4.5-P4.9 |
| **N11** | **Éliminer fork() résiduel dans wims.exe** | **Crash Windows** | 2-3 jours | — |
| N6 | Installer CAS Windows (sbcl, gap, octave-cli) | Tests CAS | 1-2 jours | T-CAS M/G/O |
| N3 | MathML helper sans fork | Fallback GIF = dégradé | 3-5 jours | P4.10-P4.11 |
| N5 | CPU limit diagnostic | Non bloquant (mitigé) | 1-2 jours | P4.12-P4.14 |
| N4 | Vérifier réseau sandbox | Devrait déjà marcher | 0.5 jour | S3 |

### Détails techniques par action :

#### N2 — wimslogd

wimslogd est un daemon lancé par fork() dans le code original. Sous Windows :
- **Option A :** le porter en service Windows (plus propre, mais complexe)
- **Option B :** le lancer comme processus séparé avant wims.exe (plus simple)

Préférence : **Option B d'abord** (prototype rapide), Option A en Phase 5 si nécessaire.

Le daemon utilise AF_UNIX (socket) pour l'IPC avec wims.cgi. Architecture V1_3 §5 prévoit
AF_UNIX Winsock (Windows 10 1803+). Vérifier que `select()` sur AF_UNIX Winsock fonctionne.

Tests : P4.1 compile, P4.2 démarre, P4.3 wims.exe ne panique plus, P4.4 logs écrits.

#### N1 — crypt → BCrypt

Le stub actuel refuse toute authentification (fail-closed — correct en sécurité).
Remplacement :
- **Windows :** `BCryptGenerateSymmetricKey` / `BCryptHashData` (pas de dépendance externe)
- **Linux :** `crypt()` standard (déjà en place, pas de changement)
- **Compatibilité cross :** les hash stockés dans les fichiers de classes doivent être les mêmes.
  Problème : crypt() Linux utilise DES/MD5/SHA-512 selon le préfixe du salt. BCrypt Windows
  ne fait pas la même chose. **Solution proposée :** utiliser un hash portable (SHA-256 ou
  SHA-512 avec salt, implémentation identique des deux côtés). Attention à la rétrocompatibilité
  avec les classes existantes.

Tests : P4.5-P4.9 dans le cahier de recette.

#### N11 — fork() résiduel

Le Suivi mentionne un `fork()` encore frappé dans le chemin de démarrage wims.exe.
Identifier l'appel exact (`grep -rn 'fork()' src/wims/`), le remplacer par `wp_spawn`
ou une alternative Windows, et s'assurer que le chemin Linux reste byte-identical.

#### N6 — CAS Windows

Installer sur ShadowPC :
- **Maxima :** chercher `sbcl.exe` dans MSYS2 (`pacman -S mingw-w64-ucrt-x86_64-maxima`)
  ou l'installeur Maxima Windows. **Ne PAS utiliser maxima.bat** (injection cmd.exe).
- **GAP :** installeur Windows officiel → `gap.exe`
- **Octave :** `winget install GNU.Octave` → `octave-cli.exe`
- Puis relancer `run_equiv_wine.sh` adapté en natif pour chaque CAS.

#### N5 — CPU limit diagnostic

Pistes à explorer dans l'ordre :
1. Ajouter `GetProcessTimes()` dans le test : le compteur UserTime avance-t-il ?
2. `QueryInformationJobObject()` : le limit est-il bien vu par le kernel ?
3. Recompiler le test CPU avec MSVC (`cl.exe`) au lieu de mingw : layout struct identique ?
4. Tester avec `PerJobUserTimeLimit` au lieu de `PerProcessUserTimeLimit`

---

## 4. Tests évasion sandbox (S1-S9)

Le selftest passe, mais les vrais tests d'évasion manquent. Le Suivi mentionne
`test_sandbox_evasion.ps1` (esquisse) mais il n'est pas dans le repo.

**Action Grok :** écrire et commiter les tests S1-S9 du cahier de recette §8.1 :

```
S1 — Lecture C:\Windows\system32\... via un CAS sandboxé → refusé
S2 — Écriture hors session dir → refusé
S3 — Connexion TCP (ping/curl) → refusé (AppContainer sans NET capability)
S4 — fork bomb (100 enfants) → Job limit atteint
S5 — Injection cmdline ("; calc.exe") → pas d'exécution
S6 — Octave system("cmd /c dir") → filtré
S7 — Octave dos/winqueryreg/winopen → filtré
S8 — Maxima (:lisp (ext:run-program "cmd")) → filtré ou sandboxé
S9 — Écriture registre HKCU → refusé (AppContainer)
```

Commiter les tests comme `tests/platform/test_sandbox_evasion.sh` ou `.ps1`.
Exécuter sur ShadowPC et reporter les résultats.

---

## 5. VPS Debian (rappel — bloqueur pour T-INT, T-OEF)

Le VPS n'est pas encore provisionné. Les actions H1-H3 du handoff 02 restent en attente :
1. Installer WIMS 4.33a avec tous les CAS
2. Générer corpus T11 (200+ OEF, seed=42)
3. Valider `wims-port/` sur le VPS

**Grok peut avancer N2/N1/N11 sur sa box sans le VPS.**

---

## 6. Git — état actuel

```
Repo : Reprise_Wims_Grok/wims-port/
Branche : main (ou master)
HEAD : 4374c68

Phase 3 commits (6) :
b9670a6 Platform: wp_spawn_mx, wp_pipe_open, wp_fd_read
1cd0534 G2: lines.c multiexec() on platform layer
c13459b G1: ch..root.exe (AppContainer + RestrictedToken + session ACL)
f81abf1 G3: strip CR from exec.err
e13ec24 G5: cross-build 7 interfaces; octave illegal+=dos/winqueryreg/winopen
4374c68 G6: wims.exe cross-builds (posix shims, alarm, crypt stub)
```

Binaires de test sur ShadowPC : `Reprise_Wims_Grok/tests-win/` (22/22 pass, CPU fail, mémoire pass, selftest pass).

---

## 7. Contraintes (rappel)

- **ShadowPC** : pas de Docker/WSL2/Hyper-V
- **SVN checkout** (`wims/`) dans `Reprise_Wims_Claude/` : lecture seule, gitignored
- **Git** : ne pas pousser sans demande, commits avec `Co-Authored-By:`
- **Sécurité** : sandbox obligatoire avant ouverture aux élèves, crypt fail-closed correct
- **Licence** : GPL, reverser `#ifdef _WIN32` à wimsdev
- **Fichiers tests-win/** : Grok peut y déposer des binaires de test. Ne rien toucher d'autre sur ShadowPC.
- **Dépôt unique** : `wims-port/` (décision H9)

---

## 8. Fichiers à lire en priorité

1. **Ce document** — instructions Phase 4
2. **`Cahier_Recette_Portage_WIMS_CLAUDE_V1_1.md`** — tests à exécuter (170 tests, 91 faits)
3. **`Cahier_Audit_Portage_WIMS_CLAUDE_V1_1.md`** — verdict Phase 1-3 + lacunes L7-L11
4. **`Suivi_Developpement_Portage.md`** — ton journal (à mettre à jour)
5. **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** — spec de référence

---

## 9. Réponse attendue

Lance N2 (wimslogd) en premier — c'est le plus gros bloqueur. N1 (crypt) et N11 (fork résiduel)
peuvent suivre dans la foulée car ils touchent des parties différentes du code.

Pour les tests sandbox (S1-S9), tu peux les faire tourner sur ShadowPC en les déposant dans
`tests-win/`. Je les exécuterai si tu préfères.

Continue ?
