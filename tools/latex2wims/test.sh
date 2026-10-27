#! /bin/sh
# Test : génère le document exemple.tex dans un runtime puis le lit via le CGI.
# usage: sh test.sh WIMS_HOME   (Linux : public_html/wims ; Windows : wims.cgi.exe)
set -e
wh=${1:?usage: sh test.sh WIMS_HOME}
here=`cd "\`dirname "$0"\`" && pwd`
mod=local/wims4win-l2w-test.fr
sh "$here/latex2wims-doc.sh" --wims-home "$wh" --tex "$here/exemple.tex" --module $mod
cd "$wh/public_html"
cgi=./wims; [ -f wims.cgi.exe ] && cgi=./wims.cgi.exe
fail=0
check() { # bloc motif description
  out=`REQUEST_METHOD=GET QUERY_STRING="module=$mod&cmd=new&lang=fr&doc=1&block=$1" \
    HTTP_USER_AGENT='Mozilla/5.0 (X11) Firefox/130.0' SERVER_NAME=localhost SERVER_PORT=80 \
    REMOTE_ADDR=127.0.0.1 SCRIPT_NAME=/wims/wims.cgi GATEWAY_INTERFACE=CGI/1.1 \
    "$cgi" 2>/dev/null | tr -d '\r'`
  st=`echo "$out" | head -1`
  if echo "$st" | grep -q '200' && echo "$out" | grep -q "$2" && echo "$out" | grep -q '</html>' \
     && ! echo "$out" | grep -q "existe pas"; then
    echo "PASS $1 : $3"
  else echo "FAIL $1 : $3 ($st)"; fail=1; fi
}
check main 'Exemple wims4win' "sommaire, titre"
check mainS1 'Euler' "section 1, texte"
check mainS1 '<math' "section 1, formule en MathML"
check mainS2 'mfrac' "section 2, intégrale (fraction MathML)"
rm -rf "$wh/public_html/modules/$mod"
[ $fail = 0 ] && echo "== latex2wims : OK ==" || { echo "== latex2wims : ÉCHEC =="; exit 1; }
