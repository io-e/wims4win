#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""oef2html.py - genere une page HTML (enonce + correction) d'un exercice OEF
en executant le VRAI moteur WIMS (wims.cgi 4.31b, non modifie).

Principe : l'exercice (.oef compile par oef2wims, ou .def) est depose dans un
module OEF local d'un arbre WIMS hors ligne ; wims.cgi est appele en mode CGI
(variables QUERY_STRING...) : cmd=new (enonce, graine fixee par !setseed dans
var.proc, mecanisme prevu par le gabarit WIMS), puis cmd=reply avec une reponse
factice pour que le moteur affiche lui-meme la bonne reponse, la solution et
les feedbacks. Le script ne fait qu'extraire ces fragments et remplir un gabarit.
Python 3, bibliotheque standard uniquement.
"""
import argparse, html, json, os, re, shutil, subprocess, sys, time
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODULE = "H6/test/oef2html.fr"

# ---------------------------------------------------------------- utilitaires
def find_div(text, cls, start=0):
    """Renvoie (debut, fin, contenu_interne) du premier <div|fieldset class=...cls...>."""
    m = re.compile(r'<(div|fieldset)\b[^>]*class="[^"]*\b%s\b[^"]*"[^>]*>' % re.escape(cls), re.I).search(text, start)
    if not m:
        return None
    tag = m.group(1)
    depth, pos = 1, m.end()
    pat = re.compile(r'<(/?)%s\b[^>]*>' % tag, re.I)
    while depth:
        n = pat.search(text, pos)
        if not n:
            return m.start(), len(text), text[m.end():]
        depth += -1 if n.group(1) else 1
        pos = n.end()
    return m.start(), pos, text[m.end():n.start()]

def strip_tags(s):
    s = re.sub(r'<(math)\b.*?</\1>', lambda m: m.group(0), s, flags=re.S)
    return html.unescape(re.sub(r'<[^>]+>', '', s)).strip()

def clean(fragment):
    """Nettoie un fragment produit par WIMS pour une page autonome
    (les scripts canvasdraw, deja inlines, sont conserves)."""
    f = re.sub(r'<script\b.*?</script>', lambda m: m.group(0) if 'canvas' in m.group(0) else '',
               fragment, flags=re.S | re.I)
    f = re.sub(r'<!--.*?-->', '', f, flags=re.S)
    f = re.sub(r'\s+on\w+="[^"]*"', '', f)
    f = re.sub(r'\n\s*\n+', '\n', f)
    return f.strip()

def neutralize_inputs(f):
    """Remplace les champs de saisie par des zones vides imprimables."""
    f = re.sub(r'<datalist\b.*?</datalist>', '', f, flags=re.S)
    f = re.sub(r'<input[^>]*type="(radio)"[^>]*>', '<span class="box">&#9675;</span>', f)
    f = re.sub(r'<input[^>]*type="(checkbox)"[^>]*>', '<span class="box">&#9633;</span>', f)
    f = re.sub(r'<input[^>]*type="(hidden|submit|button)"[^>]*>', '', f)
    f = re.sub(r'<input\b[^>]*>', '<span class="blank">&nbsp;</span>', f)
    f = re.sub(r'<textarea\b.*?</textarea>', '<span class="blank large">&nbsp;</span>', f, flags=re.S)
    f = re.sub(r'<select\b[^>]*>(.*?)</select>',
               lambda m: '<span class="menu">[ ' + ' | '.join(
                   strip_tags(o) for o in re.findall(r'<option[^>]*>(.*?)</option>', m.group(1), re.S)
                   if strip_tags(o) not in ('', '---')) + ' ]</span>', f, flags=re.S)
    f = re.sub(r'</?form\b[^>]*>', '', f)
    f = re.sub(r'<button\b.*?</button>', '', f, flags=re.S)
    return f

# ---------------------------------------------------------------- moteur WIMS
class Wims:
    def __init__(self, home, lang="fr", cgi=None, cgi_prefix=None):
        self.home = Path(home).resolve()
        self.ph = self.home / "public_html"
        # [wims4win] --cgi : moteur explicite (ex. wims.cgi.exe Windows) ;
        # --cgi-prefix : lanceur (ex. "wine" pour tester l'exe Windows sur la box)
        self.cgi = Path(cgi).resolve() if cgi else self.ph / ("wims.exe" if os.name == "nt" else "wims.cgi")
        self.prefix = cgi_prefix.split() if cgi_prefix else []
        if not self.cgi.exists():
            sys.exit("wims.cgi introuvable : %s (option --wims-home)" % self.cgi)
        self.lang = lang
        self.mod = self.ph / "modules" / MODULE
        self.trace = []
        self.setup_module()

    def setup_module(self):
        tpl = self.ph / "modules/template/oef.fr"
        if not self.mod.exists():
            shutil.copytree(tpl, self.mod)
        for d in ("def", "src"):
            p = self.mod / d
            if p.exists():
                shutil.rmtree(p)
            p.mkdir()

    def install(self, name, deftext):
        (self.mod / "def" / (name + ".def")).write_bytes(deftext)
        # index minimal du module (format de scripts/oef/mkindex)
        (self.mod / "Exindex").write_text(
            "!set exolist  =!char 1 to -2 of %s,\n!set titlelist=!char 1 to -2 of %s,\n"
            "!set exototal =!itemcnt $exolist\n" % (name, name), encoding="latin-1")

    def set_seed(self, seed):
        vp = self.mod / "var.proc"
        t = vp.read_text(encoding="latin-1")
        t = re.sub(r'^!setseed .*\n', '', t, flags=re.M)
        t = t.replace("!changeto oef/var.proc", "!setseed %d\n!changeto oef/var.proc" % seed, 1)
        vp.write_text(t, encoding="latin-1")

    def inline_resources(self, frag):
        """Images (getins/getfile) et scripts canvasdraw du moteur -> integres a la page."""
        import base64, mimetypes
        def locate(url):
            u = html.unescape(url)
            sm = re.search(r'session=([A-Za-z0-9]+)', u)
            pm = re.search(r'special_parm=([^&+"]+)', u)
            if sm and pm:
                for d in (self.home / "s2" / sm.group(1), self.home / "sessions" / sm.group(1),
                          self.home / "sessions" / sm.group(1) / "getfile",
                          self.home / "s2" / sm.group(1) / "getfile"):
                    f = d / pm.group(1)
                    if f.is_file():
                        return f
            if not re.match(r'\w+:', u):
                f = self.ph / u.split("?")[0]
                if f.is_file():
                    return f
            return None
        def img(m):
            f = locate(m.group(2))
            if not f:
                return m.group(0)
            mt = mimetypes.guess_type(f.name)[0] or "image/png"
            return m.group(1) + "data:%s;base64,%s" % (mt, base64.b64encode(f.read_bytes()).decode()) + m.group(3)
        frag = re.sub(r'(<img\b[^>]*\ssrc=")([^"]+)(")', img, frag)
        def scr(m):
            f = locate(m.group(1))
            return "<script>/*canvas*/\n%s\n</script>" % f.read_text("utf-8", "replace") if f else m.group(0)
        frag = re.sub(r'<script\b[^>]*\ssrc="([^"]*getfile[^"]*)"[^>]*>\s*</script>', scr, frag)
        return frag

    def run(self, query):
        env = dict(os.environ, REQUEST_METHOD="GET", QUERY_STRING=query,
                   REMOTE_ADDR="127.0.0.1", SERVER_NAME="localhost", SERVER_PORT="80",
                   SCRIPT_NAME="/wims/wims.cgi", HTTP_HOST="localhost",
                   HTTP_ACCEPT_LANGUAGE=self.lang, HTTP_USER_AGENT="oef2html")
        t0 = time.time()
        p = subprocess.run(self.prefix + [str(self.cgi)], cwd=str(self.ph), env=env,
                           stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
        raw = p.stdout
        m = re.search(rb'charset=([\w-]+)', raw[:600])
        enc = m.group(1).decode() if m else "utf-8"
        txt = raw.decode(enc, "replace")
        self.trace.append({"query": query, "rc": p.returncode, "bytes": len(raw),
                           "s": round(time.time() - t0, 3),
                           "stderr": p.stderr.decode("utf-8", "replace")[-500:]})
        dd = os.environ.get("OEF2HTML_DUMP")  # [wims4win] debug : pages brutes
        if dd:
            os.makedirs(dd, exist_ok=True)
            open(os.path.join(dd, "%03d.html" % len(self.trace)), "w", encoding="utf-8").write(query + "\n" + txt)
        return txt

# ---------------------------------------------------------------- un exercice
def compile_oef(oef, oef2wims, tmpdir):
    out = Path(tmpdir) / (oef.stem + ".def")
    if out.exists():
        out.unlink()
    p = subprocess.run([str(oef2wims), str(oef), str(out)], stdout=subprocess.PIPE,
                       stderr=subprocess.STDOUT)
    msg = p.stdout.decode("utf-8", "replace")
    if p.returncode != 0 or not out.exists():
        raise RuntimeError("oef2wims : " + msg.strip())
    return out.read_bytes()

def form_fields(page):
    """Champs replyN / choiceN du formulaire et une valeur factice pour chacun."""
    fields = {}
    for m in re.finditer(r'<(input|select|textarea)\b([^>]*)>', page):
        attrs = m.group(2)
        nm = re.search(r'name="((?:reply|choice)\d+)"', attrs)
        if not nm:
            continue
        name = nm.group(1)
        if m.group(1) == "select":
            seg = page[m.end():page.find("</select>", m.end())]
            vals = [v for v in re.findall(r'<option[^>]*value="([^"]*)"', seg) if v not in ("", "---") and not v.startswith("?")]
            fields.setdefault(name, vals[:1] or ["1"])
            OPTIONS.setdefault(name, vals)
        else:
            typ = re.search(r'type="(\w+)"', attrs)
            typ = typ.group(1) if typ else "text"
            val = re.search(r'value="([^"]*)"', attrs)
            if typ in ("radio", "checkbox"):
                fields.setdefault(name, [val.group(1) if val else "1"])
                if val and typ == "radio":
                    OPTIONS.setdefault(name, [])
                    if val.group(1) not in OPTIONS[name]:
                        OPTIONS[name].append(val.group(1))
            elif typ == "hidden":
                fields.setdefault(name, [val.group(1) if val else ""])
            else:
                fields.setdefault(name, None)  # texte libre
    return fields

TEXT_DUMMIES = ["0", "1", "x", "a"]
OPTIONS = {}

def build_query(session, fields, k):
    from urllib.parse import quote
    q = "session=%s&cmd=reply&module=%s" % (session, MODULE)
    for n, v in fields.items():
        if v is None:
            v = [TEXT_DUMMIES[k]]
        for x in v:
            q += "&%s=%s" % (n, quote(x if x != "" else TEXT_DUMMIES[k]))
    return q

def parse_rows(analysis):
    """Lignes de l'analyse : (nom, bonne_reponse|None, juste?, reponse_affichee)."""
    rows = []
    for tr in re.findall(r'<tr>(.*?)</tr>', analysis, re.S):
        tds = re.findall(r'<td[^>]*>(.*?)(?=<td|</tr>|$)', tr, re.S)
        if len(tds) < 2:
            continue
        name = strip_tags(tds[0]).rstrip(" =:")
        given = clean(re.sub(r'</?td[^>]*>', '', tds[1]))
        rest = "".join(tds[1:])
        m = re.search(r'bonne r\S+ponse est(.*)$|good (?:reply|answer) is(.*)$', rest, re.S | re.I)
        good = None
        if m:
            good = clean(re.sub(r'</?td[^>]*>', '', re.sub(r'\.\s*(</td>)?\s*$', '', (m.group(1) or m.group(2)).strip())))
        rows.append([name, good, 'oef_indgood' in rest, given])
    return rows

def parse_answers(analysis):
    return [(r[0], r[1] if r[1] is not None else r[3]) for r in parse_rows(analysis)
            if r[1] is not None or r[2]]

def process(w, name, deftext, seed, lang):
    w.install(name, deftext)
    w.set_seed(seed)
    page = w.run("module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes&intro_sol=1&intro_good=1&intro_feed=1&lang=%s" % (MODULE, name, lang))
    res = {"name": name, "seed": seed}
    t = re.search(r'<h1 class="oeftitle">(.*?)</h1>', page, re.S)
    res["title"] = strip_tags(t.group(1)) if t else name
    st = find_div(page, "oefstatement")
    if not st:
        body = find_div(page, "wimsbody")
        raise RuntimeError("le moteur n'a pas produit d'enonce : " +
                           strip_tags(body[2] if body else page)[:400])
    res["statement"] = clean(neutralize_inputs(w.inline_resources(st[2])))
    fs = find_div(page, "oefform", st[1])
    form_html = ""
    if fs:
        inner = re.sub(r'<legend\b.*?</legend>', '', fs[2], flags=re.S)
        inner = re.sub(r'<div class="reveal".*?</div>\s*</div>\s*</div>', '', inner, flags=re.S)
        form_html = clean(neutralize_inputs(w.inline_resources(inner)))
        form_html = re.sub(r'<div class="send_answer">.*$', '', form_html, flags=re.S)
        form_html += "</div>" * max(0, len(re.findall(r'<div\b', form_html)) - len(re.findall(r'</div>', form_html)))
    res["form"] = form_html
    sm = re.search(r'name="session" value="([^"]+)"', page)
    if not sm:   # exercice sans formulaire de reponse : enonce seul
        res.update(answers=[], feedback="", solution="", ok=False, fields=[])
        return res
    OPTIONS.clear()
    fields = form_fields(page)
    res["fields"] = list(fields)
    ans = None
    for k in range(len(TEXT_DUMMIES)):
        # nouvelle session a chaque essai : meme graine => meme tirage
        if k:
            w.set_seed(seed)
            page = w.run("module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes&intro_sol=1&intro_good=1&intro_feed=1&lang=%s" % (MODULE, name, lang))
            sm = re.search(r'name="session" value="([^"]+)"', page)
        ans = w.run(build_query(sm.group(1), fields, k))
        if 'class="answer_analysis"' in ans or 'class="oefanswer"' in ans:
            break
    an = find_div(ans, "answer_analysis")
    rows = parse_rows(an[2]) if an else []
    # Menus / choix sans bonne reponse affichee : on fait noter chaque option
    # par le moteur et on retient celle qu'il declare juste (oef_indgood).
    if rows and any(r[1] is None and not r[2] for r in rows) and OPTIONS:
        nmax = min(25, max(len(v) for v in OPTIONS.values()))
        for i in range(nmax):
            f2 = dict(fields)
            for n, v in OPTIONS.items():
                if v:
                    f2[n] = [v[min(i, len(v) - 1)]]
            w.set_seed(seed)
            pg = w.run("module=%s&cmd=new&exo=%s&qnum=1&qcmlevel=1&intro_expert=yes&intro_sol=1&intro_good=1&intro_feed=1&lang=%s" % (MODULE, name, lang))
            sm2 = re.search(r'name="session" value="([^"]+)"', pg)
            if not sm2:
                break
            a2 = w.run(build_query(sm2.group(1), f2, 0))
            an2 = find_div(a2, "answer_analysis")
            if not an2:
                continue
            for j, r2 in enumerate(parse_rows(an2[2])):
                if j < len(rows) and rows[j][1] is None and not rows[j][2] and r2[2]:
                    rows[j][1] = r2[3]
            if all(r[1] is not None or r[2] for r in rows):
                break
    res["answers"] = [(r[0], r[1] if r[1] is not None else r[3]) for r in rows if r[1] is not None or r[2]]
    fb = find_div(ans, "oef_feedbacks")
    res["feedback"] = clean(w.inline_resources(fb[2])) if fb else ""
    sol = ""
    for m in re.finditer(r'<div class="wims_widget_box clearall">', ans):
        box = find_div(ans, "wims_widget_box", m.start())
        if box and re.search(r'<h2>\s*Solution', box[2]):
            c = find_div(box[2], "box_content")
            sol = clean(w.inline_resources(c[2])) if c else ""
            break
    res["solution"] = sol
    res["ok"] = bool(an)
    return res

def purge_sessions(w):
    """Supprime les sessions WIMS creees par le script (bac a sable)."""
    for d in ("sessions", "s2"):
        for p in (w.home / d).glob("*"):
            if p.is_dir() and not p.name.startswith("."):
                shutil.rmtree(p, ignore_errors=True)

# ---------------------------------------------------------------- gabarit
def render(tpl, ctx):
    def rep(m):
        return str(ctx.get(m.group(1), ""))
    return re.sub(r'\{\{\s*(\w+)\s*\}\}', rep, tpl)

MATHJAX = ('<script>window.MathJax={loader:{load:["input/mml"]}};</script>\n'
           '<script async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/mml-chtml.js"></script>')

def answers_html(rows):
    if not rows:
        return '<p class="muted">(aucune bonne r&eacute;ponse renvoy&eacute;e par le moteur)</p>'
    return "<table class=\"answers\">\n" + "\n".join(
        "<tr><th>%s</th><td>%s</td></tr>" % (html.escape(n), g) for n, g in rows) + "\n</table>"

def main():
    ap = argparse.ArgumentParser(description="OEF -> HTML (enonce + correction) via wims.cgi")
    ap.add_argument("inputs", nargs="+", help="fichiers .oef/.def ou dossiers")
    ap.add_argument("--wims-home", default=os.environ.get("WIMS_HOME", str(HERE / "wims-runtime")))
    ap.add_argument("--oef2wims", default=os.environ.get("OEF2WIMS", ""))
    ap.add_argument("--template", default=str(HERE / "template" / "modele.html"))
    ap.add_argument("--css", default=None, help="fichier CSS (sinon template/style.css)")
    ap.add_argument("--seed", type=int, default=1, help="graine du premier tirage (defaut 1)")
    ap.add_argument("--variants", type=int, default=1, help="nombre de tirages (graines seed..seed+n-1)")
    ap.add_argument("--out", default="html_out", help="dossier de sortie")
    ap.add_argument("--lang", default="fr")
    ap.add_argument("--cgi", default=None, help="[wims4win] chemin du moteur (wims.cgi.exe)")
    ap.add_argument("--cgi-prefix", default=None, help="[wims4win] lanceur, ex. wine")
    ap.add_argument("--no-solution", action="store_true", help="enonce seul (sans correction)")
    ap.add_argument("--mathjax", choices=["on", "off"], default="on",
                    help="charger MathJax (rendu du MathML pour les navigateurs anciens)")
    ap.add_argument("--json", action="store_true", help="ecrire aussi les fragments en .json")
    a = ap.parse_args()

    w = Wims(a.wims_home, a.lang, cgi=a.cgi, cgi_prefix=a.cgi_prefix)
    o2w = a.oef2wims or str(w.home / "bin" / "oef2wims")
    for cand in (w.home / "src" / "OefMsg2wims" / "oef2wims", HERE / "oef2wims",
                 Path(shutil.which("oef2wims") or "/nonexistent")):
        if not a.oef2wims and not Path(o2w).exists() and cand.exists():
            o2w = str(cand)
    tpl = Path(a.template).read_text(encoding="utf-8")
    css_path = Path(a.css) if a.css else Path(a.template).parent / "style.css"
    css = css_path.read_text(encoding="utf-8") if css_path.exists() else ""
    files = []
    for i in a.inputs:
        p = Path(i)
        files += sorted(list(p.glob("*.oef")) + list(p.glob("*.def"))) if p.is_dir() else [p]
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    tmp = out / "_def"; tmp.mkdir(exist_ok=True)
    report = []
    for f in files:
        name = re.sub(r'[^A-Za-z0-9_]', '_', f.stem)
        try:
            deftext = f.read_bytes() if f.suffix.lower() == ".def" else compile_oef(f, o2w, tmp)
        except Exception as e:
            print("ECHEC compilation %s : %s" % (f, e)); report.append({"file": str(f), "error": str(e)}); continue
        for s in range(a.seed, a.seed + a.variants):
            try:
                purge_sessions(w)
                r = process(w, name, deftext, s, a.lang)
            except Exception as e:
                print("ECHEC %s graine %d : %s" % (f.name, s, e))
                report.append({"file": str(f), "seed": s, "error": str(e)}); continue
            ctx = {"title": html.escape(r["title"]), "source": html.escape(f.name), "seed": s,
                   "statement": r["statement"], "form": r["form"],
                   "answers": "" if a.no_solution else answers_html(r["answers"]),
                   "solution": "" if a.no_solution else (r["solution"] or '<p class="muted">(pas de \\solution)</p>'),
                   "feedback": "" if a.no_solution else r["feedback"],
                   "css": css, "mathjax": MATHJAX if a.mathjax == "on" else "",
                   "date": time.strftime("%d/%m/%Y %H:%M"), "engine": "WIMS 4.31b wims.cgi",
                   "correction_class": "hidden" if a.no_solution else ""}
            fn = out / ("%s_s%d.html" % (name, s))
            fn.write_text(render(tpl, ctx), encoding="utf-8")
            if a.json:
                fn.with_suffix(".json").write_text(json.dumps(r, ensure_ascii=False, indent=1), encoding="utf-8")
            print("OK  %s graine %d -> %s (%d reponse(s))" % (f.name, s, fn, len(r["answers"])))
            report.append({"file": str(f), "seed": s, "html": str(fn), "answers": len(r["answers"]),
                           "solution": bool(r["solution"]), "analysis": r["ok"]})
    (out / "oef2html_rapport.json").write_text(json.dumps({"runs": report, "cgi_calls": w.trace},
                                               ensure_ascii=False, indent=1), encoding="utf-8")
    nok = sum(1 for r in report if "error" not in r)
    print("Termine : %d page(s), %d echec(s). Rapport : %s" % (nok, len(report) - nok, out / "oef2html_rapport.json"))
    return 0 if nok == len(report) else 1

if __name__ == "__main__":
    sys.exit(main())
