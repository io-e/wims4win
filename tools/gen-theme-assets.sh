#!/bin/bash
# Generate the theme assets that an SVN export lacks (a release tarball ships
# them: mkdistsvn/Makefile run themes/mkcss.pl and gifs/themes/mkindex, which
# need java+yuicompressor and ImageMagick). Without them a fresh r19040 tree
# has no themes/*/css.css, no gifs/themes/*/icon-min.css and empty
# wimsedu1/wimsedu2 icon sets (pages render unstyled, 404 on icon-min.css).
# The upstream scripts are run UNCHANGED with three small shims on PATH:
#   java -jar yuicompressor IN -o OUT   -> copy (no minification)
#   magick -resize W IN OUT             -> Pillow resize to width W
#   identify -format %w|%h FILE         -> Pillow size
# Usage: tools/gen-theme-assets.sh <wims tree>   (Linux box: perl, python3-pil)
set -e
T=$(cd "${1:?usage: gen-theme-assets.sh <wims tree>}" && pwd)
S=$(mktemp -d); trap 'rm -rf "$S"' EXIT
cat > "$S/java" <<'J'
#!/bin/sh
# java -jar <jar> IN -o OUT [-v]
in=$3; out=$5; [ -n "$in" ] && [ -n "$out" ] && cp "$in" "$out"
J
cat > "$S/magick" <<'J'
#!/usr/bin/env python3
import sys
from PIL import Image
a=sys.argv[1:]; w=int(a[a.index('-resize')+1]); i,o=[x for x in a if x not in ('-resize',str(w))][-2:]
im=Image.open(i); h=max(1,round(im.height*w/im.width)); im.resize((w,h),Image.LANCZOS).save(o)
J
cat > "$S/identify" <<'J'
#!/usr/bin/env python3
import sys
from PIL import Image
a=sys.argv[1:]; f=a[a.index('-format')+1]; im=Image.open(a[-1])
sys.stdout.write(f.replace('%w',str(im.width)).replace('%h',str(im.height)))
J
chmod +x "$S"/*
export PATH="$S:$PATH"
# only generate what is missing, so a release tarball is left untouched
if ! ls "$T"/public_html/themes/*/css.css >/dev/null 2>&1; then
  (cd "$T/public_html/themes" && perl ./mkcss.pl >/dev/null)
fi
if ! ls "$T"/public_html/gifs/themes/default/icon-min.css >/dev/null 2>&1; then
  sh "$T/public_html/gifs/themes/mkindex" >/dev/null
fi
for f in "$T"/public_html/themes/*/css.css "$T"/public_html/gifs/themes/*/icon-min.css; do
  [ -s "$f" ] || { echo "gen-theme-assets: missing $f" >&2; exit 1; }
done
echo "theme assets: $(ls "$T"/public_html/themes/*/css.css | wc -l) css.css, $(ls "$T"/public_html/gifs/themes/*/icon-min.css | wc -l) icon-min.css, wimsedu1=$(ls "$T"/public_html/gifs/themes/wimsedu1 | wc -l) files"
