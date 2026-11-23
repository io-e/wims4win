#!/usr/bin/env python3
"""GROK_V1_1 P4: scan the OEF exercises of the two pilot modules for images in the rendered statement (seeds 1..3),
field types, steps and oracle usability. Output scan_V1_1/scan_images_GROK_V1_1.json. Test harness only."""
import json, re, sys
from pathlib import Path
P4 = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(P4 / "gates"))
from oefc_GROK_V1 import call
import oracle_GROK_V1 as o
MODS = sys.argv[1:] or ["H3/analysis/oeflinf.fr", "U2/arithmetic/oefrational.fr"]
root = P4 / "wims/build/wims/public_html/modules"
out = []
for m in MODS:
    for d in sorted((root / m / "def").glob("*.def")):
        exo = d.stem
        src = (root / m / "src" / f"{exo}.oef")
        s = src.read_text(errors="replace") if src.exists() else ""
        rec = {"module": m, "exo": exo, "steps_src": "\\steps" in s, "types": re.findall(r"type\s*=\s*([a-z]+)", s)}
        imgs, fields, err = [], None, None
        for seed in (1, 2, 3):
            c, j, t, h = call("/oef/render", {"module": m, "exo": exo, "seed": seed, "lang": "fr"})
            if c != 200:
                err = (j or {}).get("error"); break
            imgs.append(len(re.findall(r"<img\b", j["statement_html"])))
            fields = [(f["id"], f["type"]) for f in j["fields"]]
        rec.update(imgs=imgs, fields=fields, error=err)
        try:
            e = o.expected(m, exo, 1); rec["oracle"] = {k: (len(str(v)) > 0) for k, v in e.items()}
        except Exception as ex:
            rec["oracle"] = f"ERR {str(ex)[:60]}"
        out.append(rec); print(json.dumps(rec), flush=True)
(P4 / ("scan_V1_1/scan_images_GROK_V1_1" + ("_" + MODS[0].split("/")[-1] if sys.argv[1:] else "") + ".json")).write_text(json.dumps(out, indent=1))
