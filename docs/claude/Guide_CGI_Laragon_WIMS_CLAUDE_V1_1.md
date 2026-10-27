# CLAUDE_V1_1
# Guide : WIMS CGI sous Laragon Apache (Windows)

## Résumé

WIMS fonctionne en mode CGI sous Laragon Apache 2.4.54 sur ShadowPC
(Windows 11). Deux runtime trees testés :

| Runtime | Source | Binaire | Modules | Status |
|---------|--------|---------|---------|--------|
| Debian .deb extract | `wims_4.31b+dfsg1-5_amd64.deb` | wims.exe 522 KB (notre build) | Core only (4766 fichiers) | ✅ Home 200 |
| **Grok wims-runtime** | Compilé depuis wims-4.32.tgz sur Debian | wims.cgi.exe 929 KB (fixes inclus) | Complet (5173 fichiers) | **✅ Home + modules 200** |

**Configuration active** : Grok's `wims-runtime` (commit `d820b2d`).

## Changements V1 → V1_1

- Intégré les 4 nouveaux commits Grok (`f999023..d820b2d`) :
  - Fix NULL `SERVER_ADDR` (`log.c:521`) + CGI prepare guard (`wp_cgi_win32.c`)
  - Per-tree wimslogd pipe name (plus de collision entre instances)
  - wimslogd ne bloque plus la sortie CGI (handles non hérités)
  - Build script `build-runtime-tree.sh` + Apache config template
- VHost Laragon pointe vers `wims-runtime/public_html` (Grok)
- Modules `adm/doc`, `adm/class/classes` fonctionnent (200 OK)

## Architecture de l'arbre runtime (Grok)

```
D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime\
├── public_html\
│   ├── wims.cgi.exe      ← CGI binaire (929 KB, avec fixes)
│   ├── wims.exe           ← idem (ScriptAlias pointe sur .cgi.exe)
│   ├── html\              ← 435 fichiers (templates complets)
│   ├── msg\               ← 37 fichiers langue
│   ├── modules\           ← 5173 fichiers (core + admin)
│   ├── scripts\           ← 10018 fichiers
│   ├── gifs\              ← 5818 images
│   ├── themes\            ← 304 fichiers
│   ├── bases\             ← 2365 fichiers
│   └── bin\               ← scripts internes WIMS
├── bin\
│   └── wimslogd.exe       ← daemon IPC (257 KB)
├── log\                   ← wims.conf, .wimspass
├── sessions\              ← sessions utilisateur
├── s2\                    ← WIMS secondaire
└── tmp\                   ← tmp/log pour wimslogd
```

## Configuration Apache (Laragon)

Fichier : `C:\laragon\etc\apache2\sites-enabled\auto.wims_paris_saclay_fr.test.conf`

```apache
Define WIMS_RT "D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/wims-runtime/public_html"
ScriptAliasMatch "^/wims/wims\.cgi$" "${WIMS_RT}/wims.cgi.exe"
Alias /wims "${WIMS_RT}"
```

Voir aussi `docs/apache-laragon-wims-runtime.conf` dans le repo wims4win.

## Bugs corrigés (Grok commits f999023 + 9c3628d)

### 1. NULL dereference `log.c:521`
- `getenv("SERVER_ADDR")` peut retourner NULL → segfault 0xC0000005
- Fix : `#ifdef _WIN32` guard + `wp_cgi_prepare()` pré-remplit la variable

### 2. Collision wimslogd instances
- Le pipe name était global → tous les wims.exe du même PC partageaient un seul wimslogd
- Fix : pipe name dérivé du chemin de l'arbre runtime

### 3. wimslogd bloquait la sortie CGI
- `wp_daemon_spawn()` passait les handles au processus enfant → Apache attendait indéfiniment
- Fix : `bInheritHandles=FALSE` dans `CreateProcess`

## Tests validés

| Test | Runtime Debian | Runtime Grok |
|------|:-:|:-:|
| `wims.exe version` → 4.33a | ✅ | ✅ |
| CGI `module=home` (CLI) | ✅ 2593 chars | ✅ 26492 chars |
| CGI `module=home` (Apache) | ✅ | ✅ |
| `module=adm/doc` | ❌ wrong_module | ✅ 14474 chars |
| `module=adm/class/classes` | ❌ | ✅ 15239 chars |
| Assets CSS | ✅ | ✅ |
| Session IDs | ✅ | ✅ |

## Limitations actuelles

1. **`module=help`** → 450 (module incomplet dans cette build)
2. **`module=adm/manage`** → 550 (nécessite authentification)
3. **Modules exercices OEF** → nécessitent `modupdate` pour installer
4. **Audio Shtooka** → absent (site de téléchargement indisponible)
5. **Mot de passe webmaster** → vide (`log/.wimspass` à définir)
6. **Version mismatch** → runtime 4.32, binaire 4.33a (trunk)

## Prochaines étapes

1. Installer des modules exercices via `modupdate`
2. Tester un exercice OEF dans le navigateur
3. Définir le mot de passe webmaster
4. Tester les CAS via un exercice interactif
5. Valider la version croisée VPS ↔ Windows
