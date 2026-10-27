#! /bin/sh
# latex2wims-doc.sh : .tex (wims.sty) -> module document WIMS (doc/1/*.def)
# installé dans un runtime existant, hors de l'interface de WIMS.
# Même script sous Linux (sh) et Windows (sh.exe MSYS2 du runtime, perl MSYS2).
# Chaîne amont utilisée telle quelle :
#   modules/adm/latex2wims/latex2wims.pl  (LaTeX -> doc/1/src/*.hd)
#   scripts/docu/mkindex                  (msg2wims + conversion.pl -> *.def, .index)
set -e
usage() {
  cat <<U
usage: $0 --wims-home DIR --tex FICHIER.tex [--module local/nom.fr] [--lang fr]
          [--title TITRE] [--author NOM] [--email ADR] [--url BASE_CGI]
Installe le document dans DIR/public_html/modules/<module> et affiche l'URL.
U
  exit 2
}
wh= ; tex= ; module= ; lang=fr ; title= ; author= ; email= ; url=
while [ $# -gt 0 ]; do
  case "$1" in
    --wims-home) wh=$2; shift ;;
    --tex) tex=$2; shift ;;
    --module) module=$2; shift ;;
    --lang) lang=$2; shift ;;
    --title) title=$2; shift ;;
    --author) author=$2; shift ;;
    --email) email=$2; shift ;;
    --url) url=$2; shift ;;
    -h|--help) usage ;;
    *) echo "option inconnue: $1" >&2; usage ;;
  esac
  shift
done
[ -n "$wh" ] && [ -n "$tex" ] || usage
[ -f "$tex" ] || { echo "fichier absent: $tex" >&2; exit 2; }
wh=`cd "$wh" && pwd`
texdir=`cd "\`dirname "$tex"\`" && pwd`
base=`basename "$tex" .tex`
[ -n "$module" ] || module="local/$base.$lang"
case "$module" in
  *..*|/*) echo "module invalide: $module" >&2; exit 2 ;;
esac
[ -n "$title" ] || title=`sed -n 's/^[ \t]*\\\\title{\(.*\)}.*/\1/p' "$tex" | head -1`
[ -n "$title" ] || title=$base
here=`cd "\`dirname "$0"\`" && pwd`
tpl="$wh/public_html/modules/template/doc.$lang"
[ -d "$tpl" ] || tpl="$wh/public_html/modules/template/doc.en"
l2w="$wh/public_html/modules/adm/latex2wims"
[ -f "$l2w/latex2wims.pl" ] || { echo "latex2wims.pl absent dans $l2w" >&2; exit 3; }
command -v perl >/dev/null || { echo "perl introuvable dans le PATH" >&2; exit 3; }

md="$wh/public_html/modules/$module"
rm -rf "$md"; mkdir -p "$md"
cp -R "$tpl"/. "$md"/
rm -rf "$md/doc/1"; mkdir -p "$md/doc/1/src" "$md/doc/srctex"
# INDEX : titre, auteur, langue
sed -e "s|^title=.*|title=$title|" \
    -e "s|^description=.*|description=document généré par latex2wims.|" \
    -e "s|^author=.*|author=$author|" -e "s|^address=.*|address=$email|" \
    -e "s|^language=.*|language=$lang|" "$tpl/INDEX" >"$md/INDEX"
# sources LaTeX + wims.sty + fichiers voisins (images, css)
for f in "$texdir"/*; do
  case "$f" in *.aux|*.log|*.pdf|*.out|*.toc) continue ;; esac
  [ -f "$f" ] && cp "$f" "$md/doc/srctex/"
done
[ -f "$md/doc/srctex/wims.sty" ] || cp "$l2w/wims.sty" "$md/doc/srctex/"
# WIMS sert en windows-1252 : convertir un source UTF-8 (perl, Encode)
if perl -MEncode -e 'local $/; $_=<STDIN>; eval { my $c=$_; decode("UTF-8",$c,Encode::FB_CROAK); 1 } or exit 1; exit(/[\x80-\xff]/ ? 0 : 1)' <"$md/doc/srctex/$base.tex"; then
  perl -MEncode -pe '$_=encode("cp1252",decode("UTF-8",$_))' "$md/doc/srctex/$base.tex" >"$md/doc/srctex/$base.tex.tmp"
  mv "$md/doc/srctex/$base.tex.tmp" "$md/doc/srctex/$base.tex"
  title=`printf '%s' "$title" | perl -MEncode -pe '$_=encode("cp1252",decode("UTF-8",$_))'`
  sed -i "s|^title=.*|title=$title|" "$md/INDEX"
fi
echo "1 $base" >"$md/doc/srctex/0index"
# en-tête du document (écrit d'habitude par adm/doc, propreg.proc)
cat >"$md/doc/1/.def" <<D
tit=$title
author=$author
email=$email
copyright=
docopen=yes
doctoc=yes
dlang=$lang
header=
desc=
D
echo "$wh" >"$md/.wimshome"

cd "$md"
# mkindex lit d'abord $wims_home : forcer le runtime choisi (une variable
# wims_home globale peut viser un autre runtime, vu sur le PC)
wims_home=$wh w_wims_home=$wh w_wims_class= \
  sh "$wh/public_html/scripts/docu/mkindex" "$wh" >"$md/doc/latex2wims.log" 2>&1 || true
rm -f "$md/.wimshome"
n=`ls "$md/doc/1"/*.def 2>/dev/null | wc -l`
if [ "$n" -eq 0 ]; then
  echo "échec : aucun .def produit, voir $md/doc/latex2wims.log" >&2
  cat "$md/doc/latex2wims.log" >&2
  exit 1
fi
echo "module : $module ($n blocs, journal doc/latex2wims.log)"
[ -n "$url" ] || url="http://localhost/wims/wims.cgi"
echo "URL : $url?module=$module&cmd=new&lang=$lang"
