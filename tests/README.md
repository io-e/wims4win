# Tests du portage (phase 1)

## tests/platform : tests unitaires de `src/Platform`
- `make test` : version POSIX (15 tests : strip_cr, spawn, poll lecture/écriture, lecture bornée, timeout, kill, exit 127).
- `make test_platform.exe && wine ./test_platform.exe` : squelette Win32 cross-compilé avec mingw-w64 (refus d'isolation, CreateProcess, CRLF, wait).

## tests/interfaces : équivalence avant/après sur l'interface `pari`
`./run_equiv.sh <REF>/src/Interfaces <NEW>/src/Interfaces`
- REF = arbre r19040 d'origine compilé ; NEW = arbre refactorisé compilé.
- Compare stdout, stderr, code retour et durée (±2 s), marqueurs aléatoires normalisés.
- Scénarios : gp réel (calcul, factorisation, grosse sortie, commande interdite, about),
  faux CAS `fake_cas.py` (plain, CRLF, flood 20 Mo -> borne fsizelim), CAS muet (timeout 6 s),
  CAS absent (not_INStalled), multiexec piloté comme wims (`multiexec_driver.py`, session gp persistante).
- Exécution séquentielle (pas de parallélisme ; pas de test de charge).
