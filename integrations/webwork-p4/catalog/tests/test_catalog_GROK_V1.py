#!/usr/bin/env python3
# GROK_V1 - tests for build_catalog_GROK_V1.py and GET /programming/search (stdlib unittest + php -S).
# Run: python3 -m unittest -v tests/test_catalog_GROK_V1.py   (from B_catalog/)
import json, os, shutil, socket, sqlite3, subprocess, sys, tempfile, time, unittest, urllib.error, urllib.parse, urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(HERE))
import build_catalog_GROK_V1 as bc  # noqa: E402

PG = """## DESCRIPTION
## Find the derivative with the chain rule
## ENDDESCRIPTION
## DBsubject(Calculus - single variable)
## DBchapter(Differentiation)
## DBsection(Chain rule (without trigonometric functions))
## KEYWORDS('derivative', 'chain rule')
## Level(3)
## Author(Jane Doe)
## Institution(Univ X)
DOCUMENT();
ENDDOCUMENT();
"""
OEF = "\\title{Intégrale d'une fonction}\n\\language{fr}\n\\author{A. B.}\n\\keywords{intégrale, primitive}\n\\text{x}\n"
INDEX = "title=Calcul intégral\nkeywords=analyse,intégrale\nlevel=U1\nlanguage=fr\n"
YAML = """title: T
folders:
  - id: pg1
    path: pg
    extension: .pg
    lang: pg
    recursive: true
  - id: oef1
    path: modules
    extension: .oef
    lang: text
    recursive: true
  - id: cert
    path: certified
    extension: .json
    lang: blockly-json
  - id: cxml
    path: certified_xml
    extension: .xml
    lang: blockly-xml
    meta_folder: certified
  - id: off
    path: nowhere
    extension: .pg
    lang: pg
    enabled: false
"""


def make_tree(root):
    (root / "pg/Sub/.OLD").mkdir(parents=True)
    (root / "pg/Sub/chain.pg").write_text(PG, encoding="utf-8")
    (root / "pg/Sub/.OLD/backup.pg").write_text(PG, encoding="utf-8")
    m = root / "modules/U1/analysis/oefint.fr"
    (m / "src").mkdir(parents=True)
    (m / "INDEX").write_text(INDEX, encoding="cp1252")
    (m / "src/int1.oef").write_bytes(OEF.encode("cp1252"))
    (root / "certified").mkdir()
    (root / "certified/1705.json").write_text('{"id":1705,"name":"Droite par deux points"}', encoding="utf-8")
    (root / "certified_xml").mkdir()
    (root / "certified_xml/1705.xml").write_text("<program><name>ignored</name></program>", encoding="utf-8")
    (root / "programming_folders_CLAUDE_V1_4.yaml").write_text(YAML, encoding="utf-8")


def build(root, *extra):
    return bc.build(bc.argparse.Namespace(yaml=str(root / "programming_folders_CLAUDE_V1_4.yaml"),
                                          db=str(root / "logs/catalog_GROK_V1.sqlite"), poc=None, extra=[],
                                          map=[], full="--full" in extra, optimize=False))


class IndexerTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="cat_"))
        make_tree(self.tmp)

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def rows(self):
        con = sqlite3.connect(self.tmp / "logs/catalog_GROK_V1.sqlite")
        con.row_factory = sqlite3.Row
        return {r["key"]: dict(r) for r in con.execute("SELECT * FROM items")}

    def test_fields(self):
        rep = build(self.tmp)
        self.assertEqual(rep["total"], 4)            # .OLD backup skipped, disabled folder skipped
        r = self.rows()
        pg = r["pg1:Sub/chain.pg"]
        self.assertEqual(pg["source"], "pg")
        self.assertEqual(pg["subject"], "Calculus - single variable")
        self.assertEqual(pg["section"], "Chain rule (without trigonometric functions)")
        self.assertEqual(pg["keywords"], "derivative, chain rule")
        self.assertEqual(pg["title"], "Find the derivative with the chain rule")
        self.assertEqual(pg["level"], "3")
        oef = r["oef1:U1/analysis/oefint.fr/src/int1.oef"]
        self.assertEqual(oef["title"], "Intégrale d'une fonction")      # cp1252 decoded
        self.assertEqual(oef["module"], "U1/analysis/oefint.fr")
        self.assertEqual(oef["language"], "fr")
        self.assertEqual(oef["level"], "U1")
        self.assertIn("primitive", oef["keywords"])
        self.assertEqual(r["cert:1705.json"]["title"], "Droite par deux points")
        self.assertEqual(r["cert:1705.json"]["play_id"], 1705)
        self.assertEqual(r["cxml:1705.xml"]["title"], "Droite par deux points")  # meta_folder wins

    def test_incremental_and_delete(self):
        build(self.tmp)
        rep = build(self.tmp)
        self.assertEqual((rep["parsed"], rep["unchanged"]), (0, 4))
        p = self.tmp / "pg/Sub/chain.pg"
        p.write_text(PG.replace("Jane Doe", "John Roe") + "\n", encoding="utf-8")
        (self.tmp / "certified/1705.json").unlink()
        rep = build(self.tmp)
        self.assertEqual((rep["parsed"], rep["removed"]), (1, 1))
        self.assertEqual(self.rows()["pg1:Sub/chain.pg"]["author"], "John Roe")

    def test_fts_accents_and_prefix(self):
        build(self.tmp)
        con = sqlite3.connect(self.tmp / "logs/catalog_GROK_V1.sqlite")
        q = lambda m: [r[0] for r in con.execute("SELECT i.key FROM items_fts JOIN items i ON i.id=items_fts.rowid WHERE items_fts MATCH ?", (m,))]
        self.assertEqual(q('"integrale"*'), ["oef1:U1/analysis/oefint.fr/src/int1.oef"])
        self.assertEqual(q('"deriv"* AND "chain"*'), ["pg1:Sub/chain.pg"])

    def test_missing_folder_reported(self):
        y = self.tmp / "programming_folders_CLAUDE_V1_4.yaml"
        y.write_text(YAML + "  - id: gone\n    path: D:/nowhere/x\n    extension: .pg\n    lang: pg\n", encoding="utf-8")
        rep = build(self.tmp)
        self.assertTrue(any(m.startswith("gone:") for m in rep["missing_folders"]))


def free_port():
    s = socket.socket(); s.bind(("127.0.0.1", 0)); p = s.getsockname()[1]; s.close(); return p


@unittest.skipUnless(shutil.which("php"), "php not installed")
class SearchEndpointTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = Path(tempfile.mkdtemp(prefix="catphp_"))
        make_tree(cls.tmp)
        build(cls.tmp)
        mock = HERE / "poc_mock"
        (cls.tmp / "lumaplus/server").mkdir(parents=True)
        for f in ("router_lumaplus_GROK_V1_1.php", "router.php"):
            shutil.copy(mock / f, cls.tmp / f)
        for f in ("programming_search_api_GROK_V1.php", "programming_api_GROK_V1_2.php"):
            shutil.copy(mock / "lumaplus/server" / f, cls.tmp / "lumaplus/server" / f)
        (cls.tmp / "htdocs").mkdir()
        (cls.tmp / "htdocs/index.html").write_text('<div id="app"></div>')
        cls.port = free_port()
        cls.srv = subprocess.Popen(["php", "-S", f"127.0.0.1:{cls.port}", "router_lumaplus_GROK_V1_1.php"],
                                   cwd=cls.tmp, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        for _ in range(50):
            try:
                urllib.request.urlopen(f"http://127.0.0.1:{cls.port}/programming/search", timeout=1); break
            except Exception:
                time.sleep(0.1)

    @classmethod
    def tearDownClass(cls):
        cls.srv.terminate(); cls.srv.wait()
        shutil.rmtree(cls.tmp)

    def get(self, **params):
        url = f"http://127.0.0.1:{self.port}/programming/search?" + urllib.parse.urlencode(params)
        try:
            with urllib.request.urlopen(url, timeout=5) as r:
                return r.status, json.loads(r.read())
        except urllib.error.HTTPError as e:
            return e.code, json.loads(e.read())

    def test_search_and_facets(self):
        code, d = self.get(q="intégrale")
        self.assertEqual(code, 200)
        self.assertEqual(d["total"], 1)
        self.assertEqual(d["items"][0]["type"], "oef")
        code, d = self.get(q="chain deriv", type="pg")
        self.assertEqual((d["total"], d["by_type"]), (1, {"pg": 1}))
        code, d = self.get(q="chain", type="oef")
        self.assertEqual(d["total"], 0)

    def test_listing_paged(self):
        code, d = self.get(per_page=2, page=2)
        self.assertEqual((d["total"], d["pages"], len(d["items"])), (4, 2, 2))
        code, d = self.get(per_page=100000)
        self.assertEqual(d["per_page"], 200)

    def test_hostile_input(self):
        for q in ['"', 'a" OR "b', "NEAR(a b)", "*", "x' ; DROP TABLE items; --", "a:b", "^"]:
            code, d = self.get(q=q)
            self.assertEqual(code, 200, q)
            self.assertTrue(d["ok"], q)
        code, d = self.get(folder="../etc")
        self.assertEqual(code, 400)
        code, d = self.get(q="x" * 300)
        self.assertEqual(code, 400)
        con = sqlite3.connect(self.tmp / "logs/catalog_GROK_V1.sqlite")
        self.assertEqual(con.execute("SELECT COUNT(*) FROM items").fetchone()[0], 4)

    def test_other_routes_untouched(self):
        with urllib.request.urlopen(f"http://127.0.0.1:{self.port}/", timeout=5) as r:
            self.assertEqual(r.status, 200)


if __name__ == "__main__":
    unittest.main()
