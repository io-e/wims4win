#!/bin/sh
# Construit un arbre WIMS 4.31b hors ligne (sans Apache) pour oef2html.py.
# Usage : sh build_wims_runtime.sh wims_4.31b+dfsg1.orig.tar.gz [dossier_cible]
# Prerequis (Debian/Ubuntu) : apt install build-essential autoconf flex bison libgd-dev pari-gp maxima
# Aucun fichier source WIMS n'est modifie : configure + make, comme le Makefile upstream.
set -e
TAR=$(readlink -f "$1"); DEST=${2:-wims-runtime}
mkdir -p "$DEST"; tar xzf "$TAR" -C "$DEST" --strip-components=1
cd "$DEST/src"
[ -x configure ] || autoreconf -i >../autoconf.log 2>&1 || autoconf >>../autoconf.log 2>&1 || true
./configure >../configure.log 2>&1 || true
# le tarball dfsg n'a pas ce Makefile.in (applets java retirees) : on le cree vide puis on relance
for i in 1 2 3; do
  f=$(./config.status 2>&1 | sed -n "s/.*cannot find input file: '\(.*\)'.*/\1/p")
  [ -z "$f" ] && break; mkdir -p "$(dirname "$f")"; printf 'all:\n\t@true\ninstall:\n\t@true\n' > "$f"
done
make libstatic wims wimslogd oef2wims mathml interfaces misc flydraw >../make.log 2>&1 || make libstatic wims wimslogd mathml interfaces >../make.log 2>&1
./wims defaults > ../public_html/bases/sys/defaults.conf
./define.sh > ../public_html/bases/sys/define.conf
cp wims ../public_html/; ln -sf wims ../public_html/wims.cgi
cp Wimslogd/wimslogd ../bin/ 2>/dev/null || true
cd ..; sh bin/update >update.log 2>&1 || true
# rendu des formules en MathML (helper wims_mathml) au lieu d'images TeX
printf 'default_texposition=2\nthreshold1=100000\nthreshold2=200000\n' >> log/wims.conf
echo "Arbre WIMS pret : $(pwd)  (test : ./src/wims table)"
