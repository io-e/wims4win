# Complément à l'audit : migration de WIMS vers Windows natif

*23 octobre 2026. Complète la section 7 (« Non vérifié ») de `Audit_Migration_WIMS_Windows.md`.*

## Source utilisée
Le serveur Renater reste injoignable depuis la box (`svn ls` coupé après 90 s). Les sous-dossiers manquants ont donc été lus dans les sources complètes de **WIMS 4.31b** (`wims_4.31b+dfsg1.orig.tar.gz`, déjà présent sur la box).

Proximité avec la 4.32 : sur les 24 fichiers `.c` récupérés du clone partiel 4.32, 14 sont identiques en 4.31b et 10 diffèrent. Les constats ci-dessous portent sur l'architecture et les appels système, qui changent peu d'une version à l'autre, mais ils restent à confirmer sur la 4.32.

## Sous-dossiers désormais vérifiés (4.31b)
| Composant | Fichier(s) | Appels Unix relevés | Gravité | Piste |
|---|---|---|---|---|
| Daemon `wimslogd` | `Wimslogd/wimslogd.c`, `fork.c`, `socket.c`, `wimslogdlines.c`, `cleaning.c` | `fork`, `waitpid`, `kill(SIGKILL)`, `execve`/`execvp`, `system`, `socket(PF_UNIX)` + `sockaddr_un`, `mmap(MAP_SHARED)` pour la mémoire partagée, `chroot`, `chmod` | Bloquant | Confirmé : service Windows + TCP localhost ou named pipe + `CreateFileMapping` ; ou Cygwin |
| Interfaces logiciels (pari, maxima, octave, gap, m2, scilab, yacas) | `Interfaces/common.c` (cœur commun), `octave.c`, `m2.c`, `maxima.c` | `fork`, `pipe` x3, `dup2`, `execv`/`execvp`, `waitpid`, `kill(SIGKILL)`, `chroot`, `popen`, `umask` | Bloquant | Une seule réécriture de `common.c` (CreateProcess + CreatePipe + Job Object) couvre la plupart des interfaces |
| Wrapper sandbox | `Misc/chroot.c` (programme « Change root and exec », installé en `ch..root`) | `chroot`, `setrlimit`/`getrlimit`, `execv`, `kill`, uid à partir de 10000 | Bloquant pour la sécurité | Pas d'équivalent direct : Job Object + compte restreint, ou abandon de la sandbox en usage local |
| Wrapper uid | `Misc/wrap.c` (« Wrap uid and exec ») | `execv`/`execvp`, changement d'uid/gid | Moyen | À supprimer sous Windows |
| Serveur HTTP intégré | `Wimsd/wimsd.c` (« http daemon for WIMS ») | `fork`, `waitpid`, `alarm`/`SIGALRM`, `dup2`, `execl`, sockets BSD | Moyen | Optionnel si Apache est utilisé ; sinon à réécrire (Winsock + CreateProcess) |
| Texgif | `Texgif/basic.c` | `fork`, `execv`/`execvp`, `waitpid`, `system` (appel de TeX) | Moyen | CreateProcess vers MiKTeX/TeX Live |
| Flydraw | `Flydraw/*.c`, `flydraw.h` | Dépend de **libgd** (`gd.h`). Les « kill » trouvés sont des commandes de dessin (`killbrush`, `killtile`), pas des appels système. Un seul `system` | Faible | Compilation MinGW-w64 avec libgd (paquet MSYS2) |
| Canvasdraw et utilitaires | `Misc/canvasdraw/canvasdraw.c`, `canvasreply.c`, `checkmol`, `crossword`, `scienceprint`, `webget.c` | canvasdraw : `fork` x4, `pipe` x4, `dup2`, `execv`, beaucoup de `system` (35) ; les autres surtout `system` | Moyen | Revoir les appels `system` (commandes sh) au cas par cas |
| `Module` | `src/Module/` | Aucun appel Unix relevé dans le C | Faible | Rien de particulier |
| Scripts de `public_html/bin` | 42 entrées, dont `auth..ldap`, `convert..switch`, `insdraw..processor`, `insplot..processor` | Scripts sh appelés par WIMS | Moyen | bash MSYS2 ou réécriture |
| Scripts de `public_html/scripts` | 44 fichiers exécutables ou `.sh` | Shell | Moyen | Idem |

## Ce que ça change à la synthèse
- Le tableau principal est confirmé : les points bloquants sont `fork`/`exec`, les sockets Unix, la mémoire partagée et le daemon, et ils se retrouvent aussi dans `wimslogd` et `Interfaces/common.c`.
- Bonne nouvelle pour un portage natif : les interfaces vers les logiciels de calcul passent presque toutes par `Interfaces/common.c`, donc une seule couche à réécrire.
- La sandbox (`ch..root`) n'a pas d'équivalent Windows simple. Pour un usage local ou de développement, la désactiver est le choix le plus réaliste.
- La recommandation reste la même : Cygwin pour un WIMS qui tourne vite sous Windows sans WSL, MinGW natif seulement pour un portage durable.

## Toujours non vérifié
- Ces constats sur la 4.32 exacte (faire `svn export` de `trunk/wims-4.32` depuis la machine locale).
- La compilation réelle.
- Le contenu de `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims`.
