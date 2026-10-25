#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""oef_llm_fix.py — correction assistée par LLM de fichiers OEF (WIMS), pilotée par oef2wims-v2.exe.

Boucle : compilation (oef2wims-v2.exe --json) -> prompt (source numéroté + points « À CORRIGER »)
-> LLM (Ollama local par défaut) -> contrôles de sécurité -> nom.corrige.oef -> recompilation
-> nouvel essai avec les erreurs restantes (jusqu'à --max-iter).
Le fichier d'origine n'est JAMAIS modifié.  Python 3.12, bibliothèque standard uniquement.

Schéma JSON lu (produit par oef2wims-v2.exe --json, un fichier <nom>.json à côté du .oef) :
{
  "fichier": "chemin complet du .oef",
  "verdict": "OK" | "OK (avertissements)" | "ERREUR",
  "code_retour": 0 | 1 | ...,          # code de oef2wims.exe
  "duree_ms": float,
  "message_compilateur": "texte brut après ERROR (vide si OK)",
  "def": "chemin du .def" | null,
  "erreurs": int, "avertissements": int, "infos": int,
  "validateur": {"lance": bool, "verdict": "accepte"|"refuse", "code": int},   # seulement avec --validator
  "diff": {"lignes_retirees": int, "lignes_ajoutees": int},                     # seulement avec --diff
  "problemes": [ { "severite": "ERREUR"|"AVERTISSEMENT"|"INFO",
                   "source": "compilateur"|"pré-contrôle"|"validateur",
                   "code": "PAREN"|"NOSTMT"|"STMT2"|"STEPS2"|"PROMPT"|"EMPTY"|"OUTPUT"|"TOOLONG"|"FORMULA"|
                           "SYNTAX"|"TEXTTABLE"|"STRLONG"|"CMDLONG"|"SPPAREN"|"AUTRE"|"DIRINC"|"VARINC"|
                           "NOTITLE"|"BOM"|"ENCODAGE"|"CRLF"|"CTRL"|"VALID"|"DESACCORD",
                   "ligne": int, "colonne": int,          # 0 = pas de position
                   "ligne2": int, "colonne2": int,        # facultatif : seconde position utile
                   "message": str, "explication": str, "correction": str } ]
}
"""
from __future__ import annotations
import argparse, difflib, html, json, math, os, re, shlex, subprocess, sys, time, urllib.request, urllib.error
from dataclasses import dataclass, field
from pathlib import Path

DEFAULT_MODEL = "llama3.1:8b-instruct-q5_K_M"
DIRECTIVES = {"answer","author","choice","complex","computeanswer","condition","conditions","credits","css","description","else","email",
  "endif","endwhile","feedback","for","format","function","help","hint","if","ifval","int","integer","keywords","language","latex","matrix",
  "mdef","next","nextstep","observation","parameter","plot","precision","range","rational","real","reply","solution","statement","steps",
  "text","title","title_ca","title_cn","title_en","title_es","title_fr","title_it","title_nl","title_si","variable","while","wims"}

# ----------------------------------------------------------------------------- fournisseurs LLM
class Provider:
    """Interface : chat(messages) -> texte de la réponse. Ajoutez une sous-classe pour un autre LLM."""
    name = "abstrait"
    def __init__(self, model: str, timeout: float, temperature: float = 0.1, num_ctx: int = 8192):
        self.model, self.timeout, self.temperature, self.num_ctx = model, timeout, temperature, num_ctx
    def chat(self, messages: list[dict]) -> str:
        raise NotImplementedError
    def _post(self, url: str, payload: dict, headers: dict | None = None) -> dict:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", **(headers or {})}, method="POST")
        with urllib.request.urlopen(req, timeout=self.timeout) as r:
            return json.loads(r.read().decode("utf-8"))

class OllamaProvider(Provider):
    """Serveur Ollama local : POST {base}/api/chat, stream=false."""
    name = "ollama"
    def __init__(self, *a, base_url: str | None = None, **k):
        super().__init__(*a, **k)
        if not base_url:
            h = os.environ.get("OLLAMA_HOST", "").strip() or "localhost:11434"
            if h.startswith("0.0.0.0"): h = "127.0.0.1" + h[7:]
            if not h.startswith("http"): h = "http://" + h
            if h.count(":") < 2: h += ":11434"
            base_url = h
        self.base_url = base_url.rstrip("/")
    def chat(self, messages):
        j = self._post(f"{self.base_url}/api/chat", {"model": self.model, "messages": messages, "stream": False,
                       "options": {"temperature": self.temperature, "num_ctx": self.num_ctx}})
        if "error" in j: raise RuntimeError(f"Ollama : {j['error']}")
        return j.get("message", {}).get("content", "")

class OpenAICompatProvider(Provider):
    """API compatible OpenAI (/v1/chat/completions) : LM Studio, vLLM, llama.cpp server, OpenRouter, OpenAI...
    URL : --base-url ou variable OEF_LLM_BASE_URL (ex. http://localhost:1234/v1) ; clé : variable OEF_LLM_API_KEY (facultative en local)."""
    name = "openai"
    def __init__(self, *a, base_url: str | None = None, **k):
        super().__init__(*a, **k)
        self.base_url = (base_url or os.environ.get("OEF_LLM_BASE_URL") or "http://localhost:1234/v1").rstrip("/")
        self.key = os.environ.get("OEF_LLM_API_KEY", "")
    def chat(self, messages):
        h = {"Authorization": f"Bearer {self.key}"} if self.key else {}
        j = self._post(f"{self.base_url}/chat/completions", {"model": self.model, "messages": messages, "temperature": self.temperature}, h)
        return j["choices"][0]["message"]["content"]

PROVIDERS = {"ollama": OllamaProvider, "openai": OpenAICompatProvider}

# ----------------------------------------------------------------------------- compilation
@dataclass
class Compiler:
    exe: Path
    runner: list[str] = field(default_factory=list)   # ex. ["wine"] pour tester sous Linux
    timeout: float = 120
    def compile(self, oef: Path) -> dict:
        cmd = [*self.runner, str(self.exe), "--json", "--quiet", str(oef)]
        p = subprocess.run(cmd, capture_output=True, timeout=self.timeout)
        j = oef.with_suffix(".json")
        if not j.exists():
            raise RuntimeError(f"pas de {j.name} après compilation (code {p.returncode}) : {p.stderr.decode('utf-8','replace')[:400]}")
        return json.loads(j.read_text(encoding="utf-8"))

# ----------------------------------------------------------------------------- prompt
def read_src(p: Path) -> str:
    b = p.read_bytes()
    try: return b.decode("utf-8")
    except UnicodeDecodeError: return b.decode("cp1252", errors="replace")

def numbered(src: str) -> str:
    return "\n".join(f"{i:4d} | {l}" for i, l in enumerate(src.split("\n"), 1))

def excerpt(lines: list[str], ln: int, col: int) -> str:
    out = []
    for k in range(ln - 1, ln + 2):
        if 1 <= k <= len(lines):
            out.append(f"{'>>' if k == ln else '  '}{k:4d} | {lines[k-1].rstrip(chr(13))}")
            if k == ln and col > 0: out.append(" " * (9 + col - 1) + "^")
    return "\n".join(out)

SYSTEM = ("Tu es un expert du format OEF de WIMS (exercices en ligne). Tu corriges des fichiers .oef "
          "de façon minimale et exacte. Tu ne réponds qu'avec le fichier corrigé complet.")

def build_prompt(src: str, issues: list[dict], name: str, iteration: int) -> str:
    lines = src.split("\n"); items = []
    for n, x in enumerate(issues, 1):
        pos = f"ligne {x['ligne']}, colonne {x['colonne']}" if x.get("ligne") else "sans position"
        s = [f"[{n}] {x['severite']} ({x['code']}) — {pos}", f"    Problème : {x['message']}"]
        if x.get("ligne"): s.append(excerpt(lines, x["ligne"], x.get("colonne", 0)))
        if x.get("ligne2"): s.append(f"    Voir aussi ligne {x['ligne2']}, colonne {x.get('colonne2',0)} :\n" + excerpt(lines, x["ligne2"], x.get("colonne2", 0)))
        if x.get("explication"): s.append(f"    Explication : {x['explication']}")
        if x.get("correction"): s.append(f"    Correction proposée : {x['correction']}")
        items.append("\n".join(s))
    again = "" if iteration == 1 else f"\nATTENTION : essai n°{iteration}. La correction précédente n'a pas suffi ; voici les problèmes RESTANTS sur ta dernière version.\n"
    return f"""Fichier OEF « {name} » à corriger.{again}

RÈGLES STRICTES :
1. Corrige UNIQUEMENT les problèmes listés dans « À CORRIGER ». Rien d'autre.
2. Toutes les autres lignes doivent rester identiques octet pour octet (espaces, accents, ordre, commentaires).
3. Ne supprime aucune directive (\\title, \\statement, \\answer, ...), n'en renomme aucune sauf si la correction le demande.
4. Le compilateur compte (), [] et {{}} dans TOUT le fichier : ils doivent être équilibrés.
5. Ne recopie PAS les numéros de ligne « NNNN | » dans ta réponse.
6. Réponds avec le fichier corrigé COMPLET, entre les marqueurs ```oef et ```, sans aucun autre texte.

=== À CORRIGER ===
{chr(10).join(items)}

=== SOURCE ACTUEL (numéros de ligne pour référence seulement) ===
{numbered(src)}
"""

# ----------------------------------------------------------------------------- réponse et contrôles
def extract_oef(resp: str) -> str | None:
    resp = re.sub(r"<think>.*?</think>", "", resp, flags=re.S)        # deepseek-r1
    resp = re.sub(r"^.*?</think>", "", resp, flags=re.S) if "</think>" in resp else resp
    m = re.findall(r"```oef[ \t]*\r?\n(.*?)```", resp, flags=re.S) or re.findall(r"```[a-zA-Z]*[ \t]*\r?\n(.*?)```", resp, flags=re.S)
    if not m: return None
    out = max(m, key=len)
    if re.match(r"^\s*\d+ \| ", out):                                    # numéros recopiés par erreur
        out = "\n".join(re.sub(r"^\s*\d+ \| ?", "", l) for l in out.split("\n"))
    return out

def directive_counts(src: str) -> dict[str, int]:
    c: dict[str, int] = {}
    for m in re.finditer(r"\\([A-Za-z_]+)\s*\{", src):
        if m.group(1) in DIRECTIVES: c[m.group(1)] = c.get(m.group(1), 0) + 1
    return c

def safety(orig: str, new: str, issues: list[dict], max_change: float, min_lines: int) -> tuple[bool, str, float]:
    a, b = orig.split("\n"), new.split("\n")
    sm = difflib.SequenceMatcher(a=a, b=b, autojunk=False)
    same = sum(bl.size for bl in sm.get_matching_blocks())
    changed = max(len(a), len(b)) - same
    pct = 100.0 * changed / max(1, len(a))
    allowed = max(math.ceil(max_change / 100 * len(a)), min_lines)
    if not new.strip(): return False, "réponse vide", pct
    if changed > allowed:
        return False, f"trop de lignes modifiées : {changed} sur {len(a)} ({pct:.0f} %, limite {max_change:.0f} % / {allowed} lignes)", pct
    codes = {x["code"] for x in issues}; ca, cb = directive_counts(orig), directive_counts(new)
    lost = [f"\\{k} ({v}->{cb.get(k,0)})" for k, v in ca.items() if cb.get(k, 0) < v
            and not (k == "statement" and "STMT2" in codes) and not (k in ("steps", "nextstep") and "STEPS2" in codes)]
    if lost: return False, "directive(s) supprimée(s) : " + ", ".join(lost), pct
    return True, "ok", pct

def udiff(a: str, b: str, na: str, nb: str) -> str:
    return "".join(difflib.unified_diff(a.splitlines(True), b.splitlines(True), na, nb))

# ----------------------------------------------------------------------------- traitement d'un fichier
def select(issues: list[dict], warnings: bool) -> list[dict]:
    keep = {"ERREUR"} | ({"AVERTISSEMENT"} if warnings else set())
    return [x for x in issues if x["severite"] in keep and x["code"] not in ("OUTPUT", "DESACCORD")]

def process(oef: Path, comp: Compiler, prov: Provider | None, a) -> dict:
    t0 = time.time(); log: list[str] = []
    W = log.append
    W(f"=== oef_llm_fix : {oef} ===\nModèle : {a.provider}/{a.model}   max-iter {a.max_iter}   max-change {a.max_change} %\n")
    rep = {"fichier": str(oef), "modele": f"{a.provider}/{a.model}", "erreurs_initiales": 0, "avertissements_initiaux": 0,
           "iterations": 0, "statut": "", "alertes": [], "corrige": None, "duree_s": 0.0}
    j = comp.compile(oef); rep["erreurs_initiales"], rep["avertissements_initiaux"] = j["erreurs"], j["avertissements"]
    issues = select(j["problemes"], a.warnings)
    W(f"Compilation initiale : {j['verdict']} ({j['erreurs']} erreur(s), {j['avertissements']} avertissement(s))")
    if not issues:
        rep["statut"] = "OK (rien à corriger)"; W(rep["statut"])
        return finish(oef, rep, log, t0, a)
    orig = read_src(oef); cur = orig
    out = oef.with_name(oef.stem + ".corrige.oef")
    for it in range(1, a.max_iter + 1):
        rep["iterations"] = it
        prompt = build_prompt(cur, issues, oef.name, it)
        W(f"\n----- Itération {it} : prompt -----\n{prompt}")
        if a.dry_run:
            print(f"\n######## {oef.name} — prompt (itération 1)\n{prompt}")
            rep["statut"] = "dry-run"; return finish(oef, rep, log, t0, a)
        try:
            resp = prov.chat([{"role": "system", "content": SYSTEM}, {"role": "user", "content": prompt}])
        except (urllib.error.URLError, TimeoutError, OSError, RuntimeError, KeyError, ValueError) as e:
            W(f"\n!!! Appel LLM impossible : {e}"); rep["statut"] = "ÉCHEC (LLM injoignable)"; rep["alertes"].append(str(e)); break
        W(f"\n----- Itération {it} : réponse brute -----\n{resp}")
        new = extract_oef(resp)
        if new is None:
            msg = f"itération {it} : réponse sans bloc ```oef``` (rejetée)"; W("!!! " + msg); rep["alertes"].append(msg); continue
        if cur.endswith("\n") and not new.endswith("\n"): new += "\n"
        if "\r\n" in orig and "\r\n" not in new: new = new.replace("\n", "\r\n")
        ok, why, pct = safety(cur, new, issues, a.max_change, a.min_lines)
        W(f"\n----- Itération {it} : diff (version précédente -> proposition, {pct:.0f} % de lignes modifiées) -----\n" + udiff(cur, new, "avant", "proposition"))
        if not ok:
            msg = f"itération {it} : proposition rejetée — {why}"; W("!!! " + msg); rep["alertes"].append(msg); continue
        out.write_bytes(new.encode("utf-8")); cur = new; rep["corrige"] = str(out)
        j = comp.compile(out); issues = select(j["problemes"], a.warnings)
        W(f"Recompilation de {out.name} : {j['verdict']} ({j['erreurs']} erreur(s), {j['avertissements']} avertissement(s))")
        if not issues:
            rep["statut"] = "OK"; break
    else:
        pass
    if not rep["statut"]:
        rep["statut"] = "TOUJOURS EN ERREUR" if rep["corrige"] else "ÉCHEC (aucune proposition acceptée)"
    if rep["corrige"]:
        W("\n===== Diff final (original -> corrigé) =====\n" + udiff(orig, cur, oef.name, out.name))
    return finish(oef, rep, log, t0, a)

def finish(oef: Path, rep: dict, log: list[str], t0: float, a) -> dict:
    rep["duree_s"] = round(time.time() - t0, 2)
    log.append(f"\n===== Verdict final : {rep['statut']}  (itérations {rep['iterations']}, {rep['duree_s']} s) =====")
    for al in rep["alertes"]: log.append("ALERTE : " + al)
    if not a.dry_run: oef.with_name(oef.stem + ".llm.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    print(f"{rep['statut']:<36} {oef.name}  (it. {rep['iterations']}, {rep['duree_s']} s)" + (f"  [{len(rep['alertes'])} alerte(s)]" if rep["alertes"] else ""))
    return rep

def report(reps: list[dict], dest: Path, a) -> None:
    (dest / "llm-rapport.json").write_text(json.dumps({"modele": f"{a.provider}/{a.model}", "date": time.strftime("%Y-%m-%d %H:%M:%S"), "resultats": reps}, ensure_ascii=False, indent=2), encoding="utf-8")
    ok = sum(r["statut"].startswith("OK") for r in reps)
    rows = []
    for r in reps:
        cls = "ok" if r["statut"].startswith("OK") else "ko"
        log = Path(r["fichier"]).with_name(Path(r["fichier"]).stem + ".llm.log")
        rows.append(f"<tr class='{cls}'><td>{html.escape(Path(r['fichier']).name)}</td><td>{r['erreurs_initiales']}</td><td>{r['iterations']}</td>"
                    f"<td>{html.escape(r['statut'])}</td><td>{html.escape('; '.join(r['alertes']))}</td><td>{r['duree_s']}</td>"
                    f"<td><a href='{html.escape(log.as_uri())}'>log</a>" + (f" <a href='{html.escape(Path(r['corrige']).as_uri())}'>corrigé</a>" if r["corrige"] else "") + "</td></tr>")
    (dest / "llm-rapport.html").write_text(f"""<!DOCTYPE html><html lang="fr"><head><meta charset="utf-8"><title>Correction OEF par LLM</title>
<style>body{{font-family:Segoe UI,Arial,sans-serif;margin:24px}}table{{border-collapse:collapse;width:100%}}td,th{{border:1px solid #ccc;padding:4px 8px;font-size:14px}}
th{{background:#eee}}.ok{{background:#e5f6e5}}.ko{{background:#fde2e2}}</style></head><body>
<h1>Correction OEF par LLM</h1><p>Modèle : {html.escape(a.provider + '/' + a.model)} — {time.strftime('%Y-%m-%d %H:%M')} — {ok}/{len(reps)} fichier(s) OK.
Vérifiez TOUJOURS le diff avant d'utiliser un fichier .corrige.oef.</p>
<table><tr><th>Fichier</th><th>Erreurs initiales</th><th>Itérations</th><th>Statut final</th><th>Alertes</th><th>Durée (s)</th><th>Liens</th></tr>
{''.join(rows)}</table></body></html>""", encoding="utf-8")

# ----------------------------------------------------------------------------- CLI
def main(argv=None) -> int:
    here = Path(__file__).resolve().parent
    p = argparse.ArgumentParser(prog="oef_llm_fix.py", formatter_class=argparse.RawDescriptionHelpFormatter,
        description="Corrige des fichiers OEF avec un LLM local (Ollama), guidé par les diagnostics de oef2wims-v2.exe.\n"
                    "Le fichier d'origine n'est jamais modifié : la correction est écrite dans nom.corrige.oef.",
        epilog="Exemples :\n  python oef_llm_fix.py exo.oef\n  python oef_llm_fix.py D:\\mes_oef --model qwen2.5:14b-instruct --max-iter 2\n"
               "  python oef_llm_fix.py exo.oef --dry-run\n  python oef_llm_fix.py exo.oef --provider openai --base-url http://localhost:1234/v1 --model mon-modele")
    p.add_argument("cible", type=Path, help="fichier .oef ou dossier contenant des .oef")
    p.add_argument("--exe", type=Path, default=here.parent / "oef2wims-v2.exe", help="chemin de oef2wims-v2.exe (défaut : dossier parent du script)")
    p.add_argument("--runner", default="", help="préfixe de commande pour lancer l'exe (tests sous Linux : --runner wine)")
    p.add_argument("--provider", choices=sorted(PROVIDERS), default="ollama", help="fournisseur LLM (défaut : ollama)")
    p.add_argument("--model", default=DEFAULT_MODEL, help=f"modèle (défaut : {DEFAULT_MODEL})")
    p.add_argument("--base-url", default=None, help="URL du serveur (ollama : variable OLLAMA_HOST, sinon http://localhost:11434 ; openai : …/v1)")
    p.add_argument("--num-ctx", type=int, default=8192, help="taille de contexte Ollama (défaut 8192 ; augmentez pour les gros fichiers)")
    p.add_argument("--temperature", type=float, default=0.1, help="température (défaut 0.1)")
    p.add_argument("--max-iter", type=int, default=3, help="nombre max d'essais par fichier (défaut 3)")
    p.add_argument("--max-change", type=float, default=30.0, help="%% max de lignes modifiées accepté (défaut 30)")
    p.add_argument("--min-lines", type=int, default=3, help="nombre de lignes modifiables toléré même sur petit fichier (défaut 3)")
    p.add_argument("--warnings", action="store_true", help="corriger aussi les avertissements (pas seulement les erreurs)")
    p.add_argument("--timeout", type=float, default=600, help="délai max d'un appel LLM en secondes (défaut 600)")
    p.add_argument("--dry-run", action="store_true", help="afficher seulement les prompts, sans appeler le LLM")
    a = p.parse_args(argv)
    if not a.exe.exists(): p.error(f"oef2wims-v2.exe introuvable : {a.exe} (utilisez --exe)")
    tgt = a.cible.resolve()
    files = sorted(f for f in tgt.glob("*.oef") if not f.name.endswith(".corrige.oef")) if tgt.is_dir() else [tgt]
    if not files: p.error("aucun fichier .oef trouvé")
    comp = Compiler(a.exe, shlex.split(a.runner))
    kw = {"base_url": a.base_url} if a.base_url else {}
    prov = None if a.dry_run else PROVIDERS[a.provider](a.model, a.timeout, a.temperature, a.num_ctx, **kw)
    reps = [process(f, comp, prov, a) for f in files]
    if not a.dry_run:
        report(reps, tgt if tgt.is_dir() else tgt.parent, a)
        print(f"Rapport : {(tgt if tgt.is_dir() else tgt.parent) / 'llm-rapport.html'}")
    return 0 if all(r["statut"].startswith("OK") or r["statut"] == "dry-run" for r in reps) else 1

if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"): sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
