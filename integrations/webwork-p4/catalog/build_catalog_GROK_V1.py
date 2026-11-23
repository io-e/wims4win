#!/usr/bin/env python3
# GROK_V1 - Build the exercise catalogue (SQLite + FTS5) for the Lumaplus programming pages (8097).
# Python standard library only (sqlite3, re, argparse, pathlib).
#
# Reads the folders of programming_folders_CLAUDE_V1_4.yaml (the same small YAML subset as
# programming_api_GROK_V1_2.php) and indexes, per file:
#   PG   (lang: pg)            OPL tags: ## DBsubject / DBchapter / DBsection / KEYWORDS / Level / Author /
#                              Institution / TitleText1, and TITLE(...) or the first ## line as title
#   OEF  (extension .oef)      \title{} \language{} \author{} \keywords{}; module INDEX (title, keywords, level)
#   Eleda JSON (blockly-json)  "name"
#   XML  (blockly-xml, xml)    <name>, or the name of <meta_folder>/<stem>.json
#   Python (python)            "# CLAUDE_Vx - <title>" header line, as pr_head_name()
# Output: one table "items" + an external-content FTS5 table "items_fts" + table "meta".
# Incremental: a file whose (size, mtime) did not change is not read again; removed files are deleted.
#
# Usage (Windows, from poc_obfuscation):
#   python -X utf8 -I build_catalog_GROK_V1.py --yaml programming_folders_CLAUDE_V1_4.yaml --db logs\catalog_GROK_V1.sqlite
# Extra folders not in the YAML (for example a WIMS modules tree):
#   ... --extra wims_oef=D:/ALF_ZGit/43-wims-paris-saclay_fr/.../public_html/modules:.oef
# Path remapping (tests on another machine):
#   ... --map D:/ALF_ZGit/LW-WeBWorK/src_pg=/data/src_pg
import argparse
import html
import os
import re
import sqlite3
import sys
import time
from pathlib import Path

SCHEMA = """
CREATE TABLE IF NOT EXISTS items (
  id INTEGER PRIMARY KEY,
  key TEXT NOT NULL UNIQUE,          -- "<folder>:<path in folder>"
  folder TEXT NOT NULL,
  source TEXT NOT NULL,              -- pg | oef | eleda-json | blockly-xml | xml | python | text
  path TEXT NOT NULL,                -- path inside the folder, forward slashes
  play_id INTEGER,                   -- id_offset + number, for numbered folders
  title TEXT, module TEXT, language TEXT, level TEXT,
  subject TEXT, chapter TEXT, section TEXT, keywords TEXT,
  author TEXT, institution TEXT,
  size INTEGER, mtime INTEGER
);
CREATE INDEX IF NOT EXISTS items_folder ON items(folder, path);
CREATE INDEX IF NOT EXISTS items_source ON items(source);
CREATE VIRTUAL TABLE IF NOT EXISTS items_fts USING fts5(
  title, keywords, subject, chapter, section, module, path, author,
  content='items', content_rowid='id', tokenize='unicode61 remove_diacritics 2'
);
CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT);
"""
COLS = ("key", "folder", "source", "path", "play_id", "title", "module", "language", "level", "subject",
        "chapter", "section", "keywords", "author", "institution", "size", "mtime")
HEAD_PG = 12000
HEAD_OEF = 24000


# ---------------------------------------------------------------- YAML subset (same rules as the PHP reader)
def strip_comment(line):
    out, q = [], None
    for i, ch in enumerate(line):
        if q:
            if ch == q:
                q = None
        elif ch in "'\"":
            q = ch
        elif ch == "#" and (i == 0 or line[i - 1] in " \t"):
            break
        out.append(ch)
    return "".join(out).rstrip()


def scalar(v):
    v = v.strip()
    if len(v) >= 2 and v[0] == v[-1] and v[0] in "'\"":
        return v[1:-1]
    if v in ("true", "false"):
        return v == "true"
    if re.fullmatch(r"-?\d+", v):
        return int(v)
    return v


def load_yaml(path):
    cfg, folders, cur, in_list = {}, [], None, False
    for n, raw in enumerate(Path(path).read_text(encoding="utf-8-sig").splitlines(), 1):
        line = strip_comment(raw)
        if not line.strip():
            continue
        t = line.lstrip(" ")
        ind = len(line) - len(t)
        if ind == 0:
            in_list = False
            k, _, v = t.partition(":")
            if k.strip() == "folders" and not v.strip():
                in_list = True
            else:
                cfg[k.strip()] = scalar(v)
            continue
        if not in_list:
            raise ValueError(f"{path}:{n}: indented line outside folders:")
        if t.startswith("- "):
            cur = {}
            folders.append(cur)
            t = t[2:]
        if cur is None:
            raise ValueError(f"{path}:{n}: mapping before '- '")
        k, _, v = t.partition(":")
        cur[k.strip()] = scalar(v)
    cfg["folders"] = folders
    return cfg


# ---------------------------------------------------------------- readers
def read_head(p, n):
    with open(p, "rb") as f:
        b = f.read(n)
    try:
        return b.decode("utf-8")
    except UnicodeDecodeError:
        return b.decode("cp1252", errors="replace")


def clean(s, limit=400):
    if s is None:
        return None
    s = html.unescape(str(s))
    s = re.sub(r"\s+", " ", str(s)).strip().strip("'\"").strip()
    return s[:limit] or None


PG_TAG = re.compile(r"^##\s*(DBsubject|DBchapter|DBsection|KEYWORDS|Level|Author|Institution|TitleText1)\s*\((.*?)\)\s*$",
                    re.M | re.I)


def parse_pg(text):
    d = {}
    for name, val in PG_TAG.findall(text):
        name = name.lower()
        if name in d:
            continue
        val = clean(val)
        if val and val.lower() not in ("''", '""') and not val.lower().startswith("zzz-"):
            d[name] = val
    title = None
    m = re.search(r"TITLE\s*\(\s*['\"]([^'\"]{3,200})['\"]", text)
    if m:
        title = m.group(1)
    if not title:
        # OPL convention: "## DESCRIPTION" followed by "## <one line of description>"
        m = re.search(r"^##\s*DESCRIPTION\s*$\s*^##\s*(\S.{2,160})$", text, re.M | re.I)
        if m and not re.match(r"(?i)ENDDESCRIPTION|KEYWORDS|DB", m.group(1)):
            title = m.group(1)
    kw = d.get("keywords")
    if kw:
        kw = ", ".join(x.strip(" '\"") for x in kw.split(",") if x.strip(" '\""))
    return dict(title=clean(title), subject=d.get("dbsubject"), chapter=d.get("dbchapter"),
                section=d.get("dbsection"), keywords=clean(kw), level=d.get("level"),
                author=d.get("author"), institution=d.get("institution"))


def oef_cmd(text, name):
    m = re.search(r"\\" + name + r"\s*\{", text)
    if not m:
        return None
    i, depth, out = m.end(), 1, []
    while i < len(text) and depth:
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if not depth:
                break
        out.append(c)
        i += 1
    return clean("".join(out))


_module_index_cache = {}


def module_index(oef_path, root=None):
    """WIMS module of an OEF file (<...>/modules/<area>/<cat>/<module>/src/x.oef) and its INDEX fields."""
    p = Path(oef_path)
    mod_dir = p.parent.parent if p.parent.name == "src" else p.parent
    if mod_dir in _module_index_cache:
        return _module_index_cache[mod_dir]
    parts = mod_dir.parts
    if root is not None:
        try:
            module = Path(os.path.relpath(mod_dir, root)).as_posix()
        except ValueError:
            module = mod_dir.name
    else:
        module = "/".join(parts[parts.index("modules") + 1:]) if "modules" in parts else mod_dir.name
    info = {"module": module}
    idx = mod_dir / "INDEX"
    if idx.is_file():
        for line in read_head(idx, 16000).splitlines():
            k, _, v = line.partition("=")
            k = k.strip().lower()
            if k in ("title", "keywords", "level", "language") and v.strip():
                info["m_" + k] = clean(v)
    _module_index_cache[mod_dir] = info
    return info


def parse_oef(path, text, root=None):
    mi = module_index(path, root)
    title = oef_cmd(text, "title")
    kw = ", ".join(x for x in (oef_cmd(text, "keywords"), mi.get("m_keywords")) if x) or None
    return dict(title=title, language=oef_cmd(text, "language") or mi.get("m_language"),
                author=oef_cmd(text, "author"), keywords=clean(kw), level=mi.get("m_level"),
                module=mi["module"], chapter=mi.get("m_title"))


def json_name(text):
    m = re.search(r'"name"\s*:\s*"((?:[^"\\]|\\.)*)"', text)
    if not m:
        return None
    try:
        import json
        return clean(json.loads('"' + m.group(1) + '"'))
    except ValueError:
        return clean(m.group(1))


def xml_name(text):
    m = re.search(r"<name>([^<]*)</name>", text)
    if not m:
        return None
    import html
    return clean(html.unescape(m.group(1)))


# ---------------------------------------------------------------- folders
def resolve_dir(path, poc, maps):
    p = str(path).replace("\\", "/").rstrip("/")
    for src, dst in maps:
        if p.lower().startswith(src.lower()):
            p = dst + p[len(src):]
            break
    if re.match(r"^[A-Za-z]:/", p) and os.name != "nt":
        return None
    q = Path(p)
    if not q.is_absolute():
        q = Path(poc) / q
    return q if q.is_dir() else None


def iter_folder(f, d):
    ext = f["extension"].lower()
    if f.get("recursive") or f.get("lang") == "pg" or ext == ".oef":
        for dp, dns, fns in os.walk(d, followlinks=True):
            dns[:] = [x for x in dns if not x.startswith(".")]   # .area5, .OLD: translator backups
            for fn in fns:
                if fn.lower().endswith(ext):
                    full = os.path.join(dp, fn)
                    yield full, os.path.relpath(full, d).replace(os.sep, "/"), None
    else:
        rx = re.compile(f.get("stem_regex") or r"^(\d+)$")
        for fn in os.listdir(d):
            if not fn.lower().endswith(ext):
                continue
            m = rx.search(fn[: -len(ext)])
            pid = int(m.group(1)) + int(f.get("id_offset") or 0) if m and m.groups() and m.group(1).isdigit() else None
            yield os.path.join(d, fn), fn, pid


def source_of(f):
    if f["extension"].lower() == ".oef":
        return "oef"
    return {"pg": "pg", "blockly-json": "eleda-json", "blockly-xml": "blockly-xml", "xml": "xml",
            "python": "python"}.get(f.get("lang"), "text")


def parse_file(src, full, rel, f, poc_dir, root=None):
    row = {}
    if src == "pg":
        row = parse_pg(read_head(full, HEAD_PG))
    elif src == "oef":
        row = parse_oef(full, read_head(full, HEAD_OEF), root)
    elif src == "eleda-json":
        row = {"title": json_name(read_head(full, 4096))}
    elif src in ("blockly-xml", "xml"):
        name = None
        if f.get("meta_folder"):
            stem = re.sub(r"\D", "", Path(rel).stem)
            mf = Path(poc_dir) / f["meta_folder"] / (stem + ".json")
            if stem and mf.is_file():
                name = json_name(read_head(mf, 4096))
        row = {"title": name or xml_name(read_head(full, 4096))}
    elif src == "python":
        m = re.search(r"^#\s*CLAUDE_V[\d_]+\s*-\s*(.{3,120})$", read_head(full, 4096), re.M)
        row = {"title": clean(m.group(1)) if m else None}
    if not row.get("title"):
        row["title"] = Path(rel).stem
    return row


def build(args):
    t0 = time.perf_counter()
    poc = Path(args.poc or Path(args.yaml).resolve().parent)
    cfg = load_yaml(args.yaml)
    folders = [f for f in cfg["folders"] if f.get("enabled", True) is not False]
    for e in args.extra:
        fid, _, rest = e.partition("=")
        path, _, ext = rest.rpartition(":")
        folders.append({"id": fid, "path": path, "extension": ext, "lang": "text", "recursive": True})
    maps = [tuple(m.split("=", 1)) for m in args.map]

    Path(args.db).parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(args.db)
    con.executescript(SCHEMA)
    con.execute("PRAGMA journal_mode=WAL")
    old = {k: (i, s, m) for i, k, s, m in con.execute("SELECT id, key, size, mtime FROM items")}
    seen, stats, missing = set(), {"parsed": 0, "unchanged": 0, "errors": 0}, []
    ins = "INSERT INTO items (%s) VALUES (%s) ON CONFLICT(key) DO UPDATE SET %s" % (
        ",".join(COLS), ",".join("?" * len(COLS)), ",".join(f"{c}=excluded.{c}" for c in COLS if c != "key"))
    per_folder = {}
    with con:
        for f in folders:
            d = resolve_dir(f.get("path", ""), poc, maps)
            if d is None:
                missing.append(f"{f.get('id')}: {f.get('path')}")
                continue
            src = source_of(f)
            n = 0
            for full, rel, pid in iter_folder(f, d):
                key = f"{f['id']}:{rel}"
                seen.add(key)
                n += 1
                try:
                    st = os.stat(full)
                except OSError:
                    continue
                prev = old.get(key)
                if prev and prev[1] == st.st_size and prev[2] == int(st.st_mtime) and not args.full:
                    stats["unchanged"] += 1
                    continue
                try:
                    row = parse_file(src, full, rel, f, poc, d)
                except Exception as exc:  # one bad file must not stop the build
                    stats["errors"] += 1
                    print(f"warning: {key}: {exc}", file=sys.stderr)
                    row = {"title": Path(rel).stem}
                row.update(key=key, folder=f["id"], source=src, path=rel, play_id=pid,
                           size=st.st_size, mtime=int(st.st_mtime))
                con.execute(ins, [row.get(c) for c in COLS])
                stats["parsed"] += 1
            per_folder[f["id"]] = n
        gone = [old[k][0] for k in old.keys() - seen]
        con.executemany("DELETE FROM items WHERE id=?", [(i,) for i in gone])
        con.execute("INSERT INTO items_fts(items_fts) VALUES('rebuild')")
        total = con.execute("SELECT COUNT(*) FROM items").fetchone()[0]
        for k, v in (("built", time.strftime("%Y-%m-%dT%H:%M:%S%z")), ("yaml", Path(args.yaml).name),
                     ("count", str(total)), ("schema", "GROK_V1")):
            con.execute("INSERT OR REPLACE INTO meta VALUES (?,?)", (k, v))
    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.execute("PRAGMA journal_mode=DELETE")
    if args.optimize:
        con.execute("INSERT INTO items_fts(items_fts) VALUES('optimize')")
        con.commit()
        con.execute("VACUUM")
    con.close()
    dt = time.perf_counter() - t0
    report = dict(db=str(args.db), total=total, removed=len(gone), seconds=round(dt, 2),
                  size_bytes=os.path.getsize(args.db), folders=per_folder, missing_folders=missing, **stats)
    return report


def main(argv=None):
    ap = argparse.ArgumentParser(description="Build the SQLite/FTS5 exercise catalogue (GROK_V1).")
    ap.add_argument("--yaml", required=True, help="programming_folders YAML")
    ap.add_argument("--db", required=True, help="output SQLite file (created or updated)")
    ap.add_argument("--poc", help="base folder for relative YAML paths (default: the YAML's folder)")
    ap.add_argument("--extra", action="append", default=[], metavar="ID=DIR:EXT", help="extra recursive folder")
    ap.add_argument("--map", action="append", default=[], metavar="FROM=TO", help="path prefix remapping")
    ap.add_argument("--full", action="store_true", help="re-read every file (ignore size/mtime)")
    ap.add_argument("--optimize", action="store_true", help="FTS optimize + VACUUM at the end")
    args = ap.parse_args(argv)
    import json
    print(json.dumps(build(args), indent=1, ensure_ascii=False))


if __name__ == "__main__":
    main()
