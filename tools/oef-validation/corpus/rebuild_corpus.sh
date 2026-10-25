#!/bin/sh
# Reconstruit le corpus OEF de test (1438 fichiers) à partir de corpus.map.
# Usage : sh rebuild_corpus.sh <wims_4.31b+dfsg1.orig.tar.gz> [DEST=corpus_oef] [ELEDA_DIR]
#   src431:<chemin>  -> extrait du tarball upstream WIMS 4.31b (333 fichiers)
#   erreur:<nom>     -> cas d'erreur minimaux livrés dans erreurs/ (5 fichiers)
#   eleda:<nom>      -> exercices Eleda convertis par xml2oef (1100 fichiers), NON publiés :
#                       fournir le dossier (ex. contenu « oef/ » de la sortie xml2oef) en 3e argument.
# Vérifie ensuite chaque fichier contre corpus.sha256.
set -e
H=$(cd "$(dirname "$0")" && pwd)
TAR=$(readlink -f "$1"); DEST=${2:-corpus_oef}; ELEDA=${3:-}
[ -f "$TAR" ] || { echo "tarball introuvable : $1" >&2; exit 2; }
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
tar xzf "$TAR" -C "$T"; TOP=$(ls "$T" | head -1)
mkdir -p "$DEST"
miss=0
while IFS="$(printf '\t')" read -r name src; do
  case "$src" in
    src431/*) cp "$T/$TOP/${src#src431/}" "$DEST/$name" ;;
    erreur:*) cp "$H/erreurs/${src#erreur:}" "$DEST/$name" ;;
    eleda:*)  if [ -n "$ELEDA" ] && [ -f "$ELEDA/${src#eleda:}" ]; then cp "$ELEDA/${src#eleda:}" "$DEST/$name"; else miss=$((miss+1)); fi ;;
  esac
done < "$H/corpus.map"
cd "$DEST"
ok=$(sha256sum -c "$H/corpus.sha256" 2>/dev/null | grep -c ': OK$' || true)
bad=$(sha256sum -c "$H/corpus.sha256" 2>/dev/null | grep -c 'FAILED$' || true)
echo "corpus : $(ls *.oef 2>/dev/null | wc -l) fichiers dans $DEST ; sha256 identiques=$ok differents=$bad ; eleda manquants=$miss"
[ "$bad" = 0 ]
